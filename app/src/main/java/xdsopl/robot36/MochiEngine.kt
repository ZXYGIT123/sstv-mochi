package xdsopl.robot36

/**
 * Robot36 高质量解码器的门面（Mochi 合并版专用）。
 *
 * 之所以放在 xdsopl.robot36 包里：Decoder / PixelBuffer / ShortTimeFourierTransform
 * 的构造器与方法都是「包内可见」，跨包无法调用。
 *
 * 用法：feed() 逐块送入 16bit 单声道 PCM，返回 true 表示解出了新的扫描行，
 * 之后读 pixels/width/height 画图即可。
 */
class MochiEngine(private val sampleRate: Int) {

    private val scope = PixelBuffer(640, 2560)
    private val image = PixelBuffer(800, 616)
    private val decoder = Decoder(scope, image, "Raw", sampleRate)
    private val stft = ShortTimeFourierTransform(sampleRate / 10, 3)
    private val cx = Complex()

    /** 瀑布图用的峰值频率（Hz）与归一化电平 0..1 */
    var peakHz: Float = 0f
        private set
    var peakLevel: Float = 0f
        private set

    /** 信噪比（dB）：频带内峰值功率 / 平均功率，EMA 平滑 */
    var snrDb: Float = 0f
        private set

    val modeName: String get() = decoder.currentMode.getName()
    val width: Int get() = image.width
    val height: Int get() = image.height
    val linesDone: Int get() = if (image.line < 0) 0 else image.line
    val pixels: IntArray get() = image.pixels
    val complete: Boolean get() = image.height > 0 && image.line >= image.height

    /** name 传 null 或空串表示自动识别（VIS），否则锁定到该模式名 */
    fun lockMode(name: String?) {
        decoder.setMode(name ?: "")
    }

    fun reset() {
        image.line = -1
        peakHz = 0f
        peakLevel = 0f
    }

    fun feed(buf: ShortArray, off: Int, count: Int): Boolean {
        if (count <= 0) return false
        val pcm = FloatArray(count)
        for (i in 0 until count) pcm[i] = buf[off + i] * (1f / 32768f)
        updateSpectrum(pcm)
        return decoder.process(pcm, 0)
    }

    /** 用同一套 STFT 顺手算瀑布图数据（Robot36 原版的做法） */
    private fun updateSpectrum(pcm: FloatArray) {
        var updated = false
        for (v in pcm) {
            cx.set(v, 0f)
            if (stft.push(cx)) updated = true
        }
        if (!updated) return
        val power = stft.power
        val binHz = 10f
        val lo = (1000f / binHz).toInt()
        val hi = minOf(power.size - 1, (2400f / binHz).toInt())
        var best = 0f
        var bestBin = -1
        for (k in lo..hi) {
            val p = power[k]
            if (p > best) {
                best = p
                bestBin = k
            }
        }
        if (bestBin < 0 || best <= 0f) {
            peakHz = 0f
            peakLevel = 0f
            return
        }
        peakHz = bestBin * binHz
        val db = 10f * kotlin.math.log10(best + 1e-12f)
        peakLevel = ((db + 60f) / 60f).coerceIn(0f, 1f)

        var sum = 0f
        for (k in lo..hi) sum += power[k]
        val mean = sum / (hi - lo + 1)
        val raw = 10f * kotlin.math.log10((best + 1e-12f) / (mean + 1e-12f))
        snrDb = (snrDb * 0.85f + raw.coerceIn(0f, 60f) * 0.15f)
    }

    // ---------------- 原始扫描（Robot36 的 RawDecoder 思路） ----------------

    /** 原始扫描图像：每 0.5 s 一行，640 px，灰度 = 瞬时频率 */
    val rawPixels = IntArray(RAW_WIDTH * RAW_HEIGHT)
    private val rawDemod = Demodulator(sampleRate)
    private val rawLineSamples = sampleRate / 2
    private var rawRow = FloatArray(rawLineSamples)
    private var rawFill = 0
    private var rawRowIndex = 0
    private var rawFreq = FloatArray(0)

    val rawFilledRows: Int get() = rawRowIndex

    /** 送入 PCM，解码出一行就返回 true；图像自下往上滚，最新的行在最下面 */
    fun rawFeed(buf: ShortArray, off: Int, count: Int): Boolean {
        if (count <= 0) return false
        if (rawFreq.size < count) rawFreq = FloatArray(count)
        val pcm = FloatArray(count)
        for (i in 0 until count) pcm[i] = buf[off + i] * (1f / 32768f)
        updateSpectrum(pcm)
        rawDemod.process(pcm, 0)   // 原地写回归一化频率
        var produced = false
        for (i in 0 until count) {
            rawRow[rawFill++] = pcm[i]
            if (rawFill >= rawLineSamples) {
                emitRawRow()
                rawFill = 0
                produced = true
            }
        }
        return produced
    }

    private fun emitRawRow() {
        if (rawRowIndex < RAW_HEIGHT) {
            val base = rawRowIndex * RAW_WIDTH
            for (x in 0 until RAW_WIDTH) {
                val f = rawRow[(x * rawLineSamples) / RAW_WIDTH]
                val freq = 1900f + f * 400f
                val level = ((freq - 1500f) / 800f).coerceIn(0f, 1f)
                val v = (kotlin.math.sqrt(level) * 255f).toInt().coerceIn(0, 255)
                rawPixels[base + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
            rawRowIndex++
            return
        }
        // 滚屏：整体上移一行，新行写到最后
        System.arraycopy(rawPixels, RAW_WIDTH, rawPixels, 0, RAW_WIDTH * (RAW_HEIGHT - 1))
        val base = (RAW_HEIGHT - 1) * RAW_WIDTH
        for (x in 0 until RAW_WIDTH) {
            val f = rawRow[(x * rawLineSamples) / RAW_WIDTH]
            val freq = 1900f + f * 400f
            val level = ((freq - 1500f) / 800f).coerceIn(0f, 1f)
            val v = (kotlin.math.sqrt(level) * 255f).toInt().coerceIn(0, 255)
            rawPixels[base + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
    }

    fun resetRaw() {
        rawRowIndex = 0
        rawFill = 0
        java.util.Arrays.fill(rawPixels, 0xFF101010.toInt())
        snrDb = 0f
    }

    companion object {
        const val RAW_WIDTH = 640
        const val RAW_HEIGHT = 616
        /** Robot36 解码器支持的模式名（含 HF Fax） */
        val MODE_NAMES = listOf(
            "Martin 1", "Martin 2",
            "Scottie 1", "Scottie 2", "Scottie DX",
            "Wraase SC2\u2013180",
            "Robot 36 Color", "Robot 72 Color",
            "PD 50", "PD 90", "PD 120", "PD 160", "PD 180", "PD 240", "PD 290",
            "HF Fax",
        )
    }
}
