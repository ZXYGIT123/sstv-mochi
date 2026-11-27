# 1.1.2 改动说明（源码侧）

本工程是从链接里的 `sstv-mochi-buildable.zip` 解出来的，已按 1.1.2 的排查结果打了四类改动。
下面每一条都对应 APK 里实际生效的改动。

## 1. 图标

- `app/src/main/AndroidManifest.xml`：`<application>` 增加
  `android:icon="@mipmap/ic_launcher"` 与 `android:roundIcon="@mipmap/ic_launcher_round"`。
- `app/src/main/res/` 增加 `mipmap-*/ic_launcher(.round).png`、
  `mipmap-anydpi-v26/ic_launcher(.round).xml`、`drawable-*/ic_launcher_foreground.png`、
  `values/ic_launcher_background.xml`。

原因：1.1.1 的清单没有 `android:icon`，没有被引用的 mipmap 在资源压缩阶段被整体剔除，
resources.arsc 里连 mipmap 类型都不存在，启动器只能显示默认图标。

## 2. 拍照

APK 里的改法是直接替换 `androidx.activity.result.contract.ActivityResultContracts$TakePicture#createIntent`，
在 Intent 上补：

```smali
addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION)
setClipData(ClipData.newRawUri("capture", uri))
```

**如果以后从源码重新编译**，更干净的做法是在 `ui/` 下加一个自定义契约并替换调用点
（`EncodeScreen.kt` 里的 `rememberLauncherForActivityResult(TakePicture()) { ... }`）：

```kotlin
package om.sstvsuite.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class TakePictureGranted : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("capture", input) }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
```

另外删掉了 `<uses-permission android:name="android.permission.CAMERA" />`：
`ACTION_IMAGE_CAPTURE` 本来不需要它，声明了却从不申请，在部分 ROM 上反而直接失败。

## 3. PD 50–290 编不出来（根因已确认）

`engine/Encoder.java` 构造函数里生成 `rowDelta` 的循环少了一个 `else`，
`iArr6[i4] = i15` 这行在 `i4` 已经被 `i4 = i17` 推进之后又执行了一次：

```java
if (lineRunArr2[i4].kind == 3) {
    if (lineRunArr2[i4].channel == 'Y') {
        iArr6[i4] = i16; i16++;
        i4 = i17; d = d2;
    }
} else { ... }
iArr6[i4] = i15;   // ← i4 已越界
```

PD 的每个扫描行是 `sync + porch + Y + V + U + Y`（6 段，最后一段又是 Y），
所以只有 PD 会在这里越界抛 `ArrayIndexOutOfBoundsException: Index 6 out of bounds for length 6`，
其他模式（Robot36/Martin/Scottie/Wraase 每行只有一段 Y）都不会踩到。

已删掉越界的那一行，行为与 `PlanGeom` 里的同名逻辑（`rowDelta` 只在 Y 段自增、其余保持 0）一致。

验证方式（JVM，非 Android）：把 `engine` 包原样编译，用 640×496 渐变图做
`Encoder.renderAll()` → `Decoder.feed()` 往返，修复前抛异常，修复后 7 个 PD 模式全部
`complete=true`：pd50 320×256、pd90、pd120 640×496、pd160 512×400、pd180、pd240、pd290 800×616，
同时 martin1/scottie1/sc2_120/robot72 回归通过。

## 4. HF Fax

- `Modes.createAll()` 本来就包含 `hfFax()`，所以 `ModeRegistry.all()`（以及锁定列表）里一直有它；
  1.1.1 里“选不到”的说法不成立。
- 真正的问题是默认引擎是 Robot36：`xdsopl.robot36.Demodulator` 只把 5/9/20 ms 三档认作同步脉冲，
  接受窗口上限 `round(0.025 × rate)`，而 HF Fax 的同步正好 25 ms，滤波后稍长就被整段丢弃，
  行首检测不到 → `getLinesDone()` 恒为 0 → 界面全黑。
- 改法：`SstvState.kt` 里
  1) `lockMode(id)` 在 `id == "hffax"` 且当前不是标准引擎时先 `selectEngine(Standard)`；
  2) 删掉 `selectEngine` 里“标准引擎没有 HF Fax，已切回自动识别”的清空分支
     （标准引擎其实有 `hfFax()` 模式）；
  3) `lockOptions()` 里 HF Fax 只追加一次。

验证方式（JVM）：`hfFax` 模式 640×1200，锁定解码往返 `complete=true`，1125 行出图；
不锁定时因为 HF Fax 没有 VIS 头，识别不到（符合预期）。

## 5. 其他

- `versionCode` 7 / `versionName` 1.1.2-robot36。
- release 签名仍用 `sstv-mochi.jks`（别名 `sstv`），证书 SHA-256
  `8B:9F:56:66:...:9A:E5:55`，与原 1.1.1 一致，可直接覆盖安装。
