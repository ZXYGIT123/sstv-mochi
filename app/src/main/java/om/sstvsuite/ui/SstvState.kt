package om.sstvsuite.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import om.sstvsuite.AppPrefs
import om.sstvsuite.audio.AudioCapture
import om.sstvsuite.audio.AudioFileDecoder
import om.sstvsuite.audio.WavStreamWriter
import om.sstvsuite.engine.Decoder
import om.sstvsuite.engine.Encoder
import om.sstvsuite.engine.Frame
import om.sstvsuite.engine.ModeRegistry
import om.sstvsuite.engine.SstvMode
import om.sstvsuite.image.ImageProcessor
import om.sstvsuite.image.WatermarkRenderer
import om.sstvsuite.storage.FileShare
import om.sstvsuite.storage.MediaStoreSaver
import ui.feice.mochi.theme.ColorSchemeMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

/** 瀑布图历史：一行一个采样点，横轴 1000–2400 Hz */
class Waterfall(val rows: Int = 72) {
    private val xs = FloatArray(rows) { -1f }
    private val lv = FloatArray(rows) { 0f }
    private var pos = 0

    fun clear() {
        for (i in 0 until rows) {
            xs[i] = -1f
            lv[i] = 0f
        }
        pos = 0
    }

    fun addSample(freq: Float, level: Float) {
        xs[pos] = if (freq <= 0f) -1f else ((freq - 1000f) / 1400f).coerceIn(0f, 1f)
        lv[pos] = level.coerceIn(0f, 1f)
        pos = (pos + 1) % rows
    }

    fun forEachRow(block: (row: Int, x: Float, level: Float) -> Unit) {
        for (i in 0 until rows) {
            val idx = (pos + i) % rows
            block(i, xs[idx], lv[idx])
        }
    }
}

/** 三个页面共用的设置状态，同时驱动主题 */
class SettingsState(context: Context) {
    private val prefs = AppPrefs.get(context)

    var autoDetect by mutableStateOf(prefs.isAutoDetect())
    var autoSave by mutableStateOf(prefs.isAutoSave())
    var waterfallVisible by mutableStateOf(prefs.isWaterfallVisible())
    var dynamicColor by mutableStateOf(prefs.isDynamicColor())
    var nightMode by mutableIntStateOf(prefs.getNightMode())
    var sampleRate by mutableIntStateOf(prefs.getDefaultSampleRate())
    var lockedModeId by mutableStateOf(prefs.getLockedModeId())
    var watermarkEnabled by mutableStateOf(prefs.isWatermarkEnabled())
    var watermarkPosition by mutableIntStateOf(prefs.getWatermarkPosition())
    var watermarkSize by mutableIntStateOf(prefs.getWatermarkSize())
    var watermarkColor by mutableIntStateOf(prefs.getWatermarkColor())
    var watermarkStroke by mutableStateOf(prefs.isWatermarkStroke())

    val modeCount: Int get() = ModeRegistry.all().size

    val colorSchemeMode: ColorSchemeMode
        get() = when (nightMode) {
            1 -> if (dynamicColor) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
            2 -> if (dynamicColor) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
            else -> if (dynamicColor) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        }

    fun setAutoDetect(v: Boolean) { autoDetect = v; prefs.setAutoDetect(v) }
    fun setAutoSave(v: Boolean) { autoSave = v; prefs.setAutoSave(v) }
    fun setWaterfallVisible(v: Boolean) { waterfallVisible = v; prefs.setWaterfallVisible(v) }
    fun setDynamicColor(v: Boolean) { dynamicColor = v; prefs.setDynamicColor(v) }
    fun setNightMode(v: Int) { nightMode = v; prefs.setNightMode(v) }
    fun setSampleRate(v: Int) { sampleRate = v; prefs.setDefaultSampleRate(v) }
    fun setLockedModeId(v: String) { lockedModeId = v; prefs.setLockedModeId(v) }
    fun setWatermarkEnabled(v: Boolean) { watermarkEnabled = v; prefs.setWatermarkEnabled(v) }
    fun setWatermarkPosition(v: Int) { watermarkPosition = v; prefs.setWatermarkPosition(v) }
    fun setWatermarkSize(v: Int) { watermarkSize = v; prefs.setWatermarkSize(v) }
    fun setWatermarkColor(v: Int) { watermarkColor = v; prefs.setWatermarkColor(v) }
    fun setWatermarkStroke(v: Boolean) { watermarkStroke = v; prefs.setWatermarkStroke(v) }
    fun watermarkText(context: Context): String = prefs.getWatermarkText(context)
    fun setWatermarkText(text: String) = prefs.setWatermarkText(text)
    fun resetWatermarkText() = prefs.resetWatermarkText()
    fun lockedMode(): SstvMode? = if (lockedModeId.isEmpty()) null else ModeRegistry.byId(lockedModeId)
}

/** 编码页：选图、水印、实时播放或导出 WAV */
class EncodeState(val context: Context, val settings: SettingsState) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val modes: List<SstvMode> = ModeRegistry.all()

    var mode by mutableStateOf(modes[0])
    var sampleRate by mutableIntStateOf(settings.sampleRate)
    var source by mutableStateOf<Bitmap?>(null)
    var preview by mutableStateOf<Bitmap?>(null)
    var loading by mutableStateOf(false)
    var transmitting by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var status by mutableStateOf("待机")
    var remaining by mutableStateOf("")
    var lastWav by mutableStateOf<File?>(null)
    var lastWavLabel by mutableStateOf<String?>(null)

    private var worker: Thread? = null
    @Volatile private var cancel = false
    private var lastPercent = -1

    fun setSampleRate(rate: Int) {
        sampleRate = rate
        settings.setSampleRate(rate)
    }

    fun loadImage(uri: Uri) {
        loading = true
        scope.launch {
            try {
                val bmp = ImageProcessor.loadBitmap(context, uri, 2048)
                source = bmp
                refreshPreview()
            } catch (t: Throwable) {
                status = "图片加载失败"
            } finally {
                loading = false
            }
        }
    }

    fun refreshPreview() {
        val src = source
        if (src == null) {
            preview = null
            return
        }
        preview = render(src)
    }

    private fun render(src: Bitmap): Bitmap {
        val cropped = ImageProcessor.cropAndScale(src, mode.width, mode.height) ?: src
        val copy = cropped.copy(Bitmap.Config.ARGB_8888, true) ?: cropped
        if (settings.watermarkEnabled) {
            WatermarkRenderer.draw(
                copy,
                settings.watermarkText(context),
                settings.watermarkPosition,
                settings.watermarkSize,
                settings.watermarkColor,
                settings.watermarkStroke,
            )
        }
        return copy
    }

    fun setMode(next: SstvMode) {
        mode = next
        refreshPreview()
    }

    fun startPlayback() {
        val frame = buildFrame() ?: return
        cancel = false
        transmitting = true
        progress = 0f
        status = "正在播放…"
        worker = Thread {
            var track: AudioTrack? = null
            try {
                val encoder = Encoder(frame, mode, sampleRate)
                val total = encoder.totalSamples
                val min = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(maxOf(min, 16384) * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()
                val buf = ShortArray(4096)
                var pos = 0
                while (pos < total && !cancel) {
                    val n = encoder.render(pos, buf, 0, buf.size)
                    if (n <= 0) break
                    track.write(buf, 0, n)
                    pos += n
                    publishProgress(pos, total)
                }
                if (!cancel) finish("播放完成")
            } catch (t: Throwable) {
                finish("发射失败：" + (t.message ?: t.javaClass.simpleName))
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
            }
        }.also { it.start() }
    }

    fun startWav() {
        val frame = buildFrame() ?: return
        cancel = false
        transmitting = true
        progress = 0f
        status = "正在生成 WAV…"
        worker = Thread {
            try {
                val encoder = Encoder(frame, mode, sampleRate)
                val total = encoder.totalSamples
                val dir = MediaStoreSaver.audioOutputDir(context)
                val out = File(dir, "SSTV_" + mode.id + "_" + stamp() + ".wav")
                val writer = WavStreamWriter(out, sampleRate)
                val buf = ShortArray(8192)
                var pos = 0
                try {
                    while (pos < total && !cancel) {
                        val n = encoder.render(pos, buf, 0, buf.size)
                        if (n <= 0) break
                        writer.write(buf, 0, n)
                        pos += n
                        publishProgress(pos, total)
                    }
                } finally {
                    writer.close()
                }
                if (cancel) return@Thread
                val label = MediaStoreSaver.publishAudio(context, out, out.name)
                lastWav = out
                lastWavLabel = label
                finish("已保存 WAV：" + label)
            } catch (t: Throwable) {
                finish("发射失败：" + (t.message ?: t.javaClass.simpleName))
            }
        }.also { it.start() }
    }

    fun shareLastWav() {
        val f = lastWav ?: return
        try {
            context.startActivity(FileShare.audioShareIntent(context, f))
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        cancel = true
        transmitting = false
        progress = 0f
        status = "已停止发射"
    }

    private fun finish(message: String) {
        transmitting = false
        progress = 0f
        status = message
        scope.launch { remaining = "" }
    }

    private fun publishProgress(pos: Int, total: Int) {
        if (total <= 0) return
        val percent = pos * 100 / total
        if (percent == lastPercent) return
        lastPercent = percent
        val left = ((total - pos).coerceAtLeast(0)) / sampleRate.toFloat()
        scope.launch {
            progress = pos.toFloat() / total
            remaining = "剩余 " + secondsLabel(left)
        }
    }

    private fun buildFrame(): Frame? {
        val src = source
        if (src == null) {
            status = "请先选择一张图像"
            return null
        }
        return ImageProcessor.toFrame(render(src))
    }

    fun dispose() {
        cancel = true
        scope.cancel()
    }
}

/** 解码页：麦克风接收、音频文件解码、保存分享 */
class DecodeState(val context: Context, val settings: SettingsState) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val modes: List<SstvMode> = ModeRegistry.all()
    val waterfall = Waterfall()

    var receiving by mutableStateOf(false)
        private set
    var fileDecoding by mutableStateOf(false)
        private set
    var status by mutableStateOf("待机")
    var modeName by mutableStateOf<String?>(null)
    var linesDone by mutableIntStateOf(0)
    var linesTotal by mutableIntStateOf(0)
    var image by mutableStateOf<Bitmap?>(null)
    var freq by mutableFloatStateOf(0f)
    var level by mutableFloatStateOf(0f)
    var waterfallTick by mutableIntStateOf(0)
    var lockedModeId by mutableStateOf(settings.lockedModeId)

    private var capture: AudioCapture? = null
    private var decoder: Decoder? = null
    private var ticker: Job? = null
    private var lastProgressAt = 0L
    private var lastFrame: Frame? = null

    fun lockedMode(): SstvMode? = if (lockedModeId.isEmpty()) null else ModeRegistry.byId(lockedModeId)

    fun lockMode(id: String) {
        lockedModeId = id
        settings.setLockedModeId(id)
        decoder?.setLockedMode(lockedMode())
    }

    fun start() {
        if (receiving || fileDecoding) return
        val dec = Decoder(AudioCapture.SAMPLE_RATE, listener)
        dec.setLockedMode(lockedMode())
        decoder = dec
        image = null
        lastFrame = null
        modeName = null
        linesDone = 0
        linesTotal = lockedMode()?.height ?: 0
        waterfall.clear()
        waterfallTick++
        receiving = true
        status = "正在寻找 VIS 信号…"
        val cap = AudioCapture()
        capture = cap
        val started = cap.start(object : AudioCapture.Sink {
            override fun onSamples(source: AudioCapture?, pcm: ShortArray?, count: Int) {
                if (pcm == null || count <= 0) return
                try {
                    dec.feed(pcm, 0, count)
                } catch (t: Throwable) {
                    scope.launch { fail(t) }
                }
            }

            override fun onFailure(source: AudioCapture?, error: Throwable?) {
                if (error != null) scope.launch { fail(error) }
            }
        })
        if (!started) {
            receiving = false
            status = "麦克风初始化失败"
            return
        }
        ticker = scope.launch {
            while (true) {
                delay(64)
                freq = dec.signalFrequency
                level = dec.signalLevel
                if (settings.waterfallVisible) {
                    waterfall.addSample(dec.signalFrequency, dec.signalLevel)
                    waterfallTick++
                }
            }
        }
    }

    fun stop() {
        receiving = false
        ticker?.cancel()
        ticker = null
        try { capture?.stop() } catch (_: Throwable) {}
        capture = null
        decoder = null
        freq = 0f
        level = 0f
        if (status.startsWith("解码中") || status.startsWith("正在寻找") || status.startsWith("已识别")) {
            status = "已停止接收"
        }
    }

    fun decodeFile(uri: Uri) {
        if (receiving || fileDecoding) return
        fileDecoding = true
        status = "正在解码音频文件…"
        image = null
        lastFrame = null
        scope.launch {
            try {
                val pcm = withContext(Dispatchers.IO) {
                    AudioFileDecoder.decodeToMono(context, uri, AudioCapture.SAMPLE_RATE)
                }
                if (pcm == null || pcm.isEmpty()) {
                    fileDecoding = false
                    status = "无法读取该音频，请改用 WAV 文件"
                    return@launch
                }
                val dec = Decoder(AudioCapture.SAMPLE_RATE, listener)
                dec.setLockedMode(lockedMode())
                withContext(Dispatchers.Default) {
                    val chunk = 8192
                    var i = 0
                    while (i < pcm.size) {
                        val n = minOf(chunk, pcm.size - i)
                        dec.feed(pcm, i, n)
                        i += n
                        if (dec.isComplete) break
                    }
                    dec.flush()
                }
                fileDecoding = false
                if (lastFrame == null) {
                    status = "音频不完整或未识别到完整 VIS：仅预览，不自动保存"
                }
            } catch (t: Throwable) {
                fileDecoding = false
                status = "文件解码失败：" + (t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun saveImage() {
        val bmp = image
        if (bmp == null) {
            status = "还没有可保存或分享的图像"
            return
        }
        scope.launch {
            try {
                val label = MediaStoreSaver.saveImage(context, bmp, imageName())
                status = "已保存到相册：" + label
            } catch (t: Throwable) {
                status = "保存失败：" + (t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun shareImage() {
        val bmp = image
        if (bmp == null) {
            status = "还没有可保存或分享的图像"
            return
        }
        scope.launch {
            try {
                val file = MediaStoreSaver.writeImageToCache(context, bmp, imageName())
                context.startActivity(FileShare.imageShareIntent(context, file))
            } catch (t: Throwable) {
                status = "分享失败：" + (t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun imageName(): String = "SSTV_" + (modeName ?: "decode") + "_" + stamp() + ".png"

    private fun toBitmap(frame: Frame): Bitmap =
        Bitmap.createBitmap(frame.argb, frame.width, frame.height, Bitmap.Config.ARGB_8888)

    private val listener = object : Decoder.Listener {
        override fun onModeDetected(mode: SstvMode?) {
            if (mode == null) return
            scope.launch {
                modeName = mode.name
                linesTotal = mode.height
                status = "已识别：" + mode.name
            }
        }

        override fun onProgress(frame: Frame?, lines: Int, mode: SstvMode?) {
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastProgressAt < 90) return
            lastProgressAt = nowMs
            scope.launch {
                if (frame != null) {
                    image = toBitmap(frame)
                    lastFrame = frame
                }
                linesDone = lines
                if (mode != null) {
                    modeName = mode.name
                    linesTotal = mode.height
                    status = "解码中：" + mode.name
                }
            }
        }

        override fun onComplete(frame: Frame?, mode: SstvMode?) {
            scope.launch {
                if (frame != null) {
                    image = toBitmap(frame)
                    lastFrame = frame
                }
                if (mode != null) {
                    modeName = mode.name
                    linesDone = mode.height
                    linesTotal = mode.height
                    status = "解码完成：" + mode.name
                    if (settings.autoSave && frame != null) {
                        try {
                            val bmp = toBitmap(frame)
                            val label = MediaStoreSaver.saveImage(context, bmp, imageName())
                            status = "已自动保存解码结果：" + label
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }

        override fun onError(message: String?) {
            scope.launch { status = "解码出错" }
        }
    }

    private fun fail(t: Throwable) {
        receiving = false
        ticker?.cancel()
        ticker = null
        try { capture?.stop() } catch (_: Throwable) {}
        capture = null
        status = "接收失败：" + (t.message ?: t.javaClass.simpleName)
    }

    fun dispose() {
        stop()
        scope.cancel()
    }
}

internal fun secondsLabel(seconds: Float): String {
    val total = ceil(seconds).toInt().coerceAtLeast(0)
    return if (total >= 60) (total / 60).toString() + "分" + (total % 60) + "秒" else total.toString() + "秒"
}
