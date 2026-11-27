package om.sstvsuite.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/**
 * 拍照契约（MIUI 兼容版）。
 *
 * 实测结论（Redmi M2007J1SC, MIUI Phoenix 相机 OneShotImageCapture）：
 * 1. 传 EXTRA_OUTPUT（无论 FileProvider 还是 MediaStore pending 条目），相机都会
 *    先按自己的逻辑把照片存进 DCIM/Camera，然后试图写调用方给的 URI ——
 *    pending 条目因 MediaProvider 的 owner 检查被拒（"Only owner is able to
 *    interact with pending item"，grantUriPermission 不起作用）——
 *    最终相机返回 RESULT_CANCELED 且 intent=null。
 * 2. 不传 EXTRA_OUTPUT 时相机同样存 DCIM/Camera，返回缩略图。
 *
 * 所以这里统一走"无输出"路线：相机自己存相册，App 拍完扫 MediaStore 里
 * 最近新增的照片加载。图片留在系统相册是 MIUI 上的物理行为，无法绕过。
 */
class TakePictureGranted : ActivityResultContract<Unit?, TakePictureGranted.Result>() {

    data class Result(
        val ok: Boolean,
        val thumb: Bitmap? = null,
    )

    override fun createIntent(context: Context, input: Unit?): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        var thumb: Bitmap? = intent?.getParcelableExtra<Bitmap>("data")
        val ok = resultCode == android.app.Activity.RESULT_OK || thumb != null
        return Result(ok = ok, thumb = thumb)
    }

    companion object {
        /**
         * 扫 MediaStore 里 millis 毫秒内新增的照片，返回最新的一个。
         * MIUI 相机会把照片存进 DCIM/Camera，这里从系统相册把它拿回来。
         */
        fun findRecentPhoto(context: Context, millis: Long): Uri? {
            val cutoff = (System.currentTimeMillis() - millis) / 1000
            val selection = MediaStore.Images.Media.DATE_ADDED + " >= ?"
            val sort = MediaStore.Images.Media.DATE_ADDED + " DESC"
            return try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media._ID),
                    selection,
                    arrayOf(cutoff.toString()),
                    sort
                )
            } catch (_: Throwable) {
                null
            }?.use { c ->
                if (c.moveToFirst()) {
                    android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)
                    )
                } else null
            }
        }
    }
}
