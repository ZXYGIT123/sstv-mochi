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
    }

    companion object {
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
