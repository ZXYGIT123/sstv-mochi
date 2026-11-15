package om.sstvsuite.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import om.sstvsuite.storage.FileShare
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiChip
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.MochiToastState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold
import ui.feice.mochi.theme.MochiTheme
import java.io.File

@Composable
fun EncodeScreen(state: EncodeState, settings: SettingsState, toast: MochiToastState) {
    val context = LocalContext.current
    val scheme = MochiTheme.colorScheme
    var modeExpanded by remember { mutableStateOf(false) }
    var showWatermark by rememberSaveable { mutableStateOf(false) }
    var showOutput by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) state.loadImage(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
        val uri = cameraUri
        if (ok && uri != null) state.loadImage(uri)
    }

    MochiLazyLargeTitleScaffold(
        title = "编码",
        subtitle = "把照片打成一帧 SSTV 音频",
    ) {
        item {
            MediaCard {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(state.mode.width.toFloat() / state.mode.height.toFloat(), matchHeightConstraintsFirst = false),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = state.preview
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MochiIonicon("images", tint = scheme.onSurfaceVariant, size = 34.dp)
                            Caption("尚未选择图像")
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MochiButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                        MochiIonicon("images")
                        Text("  选择图片")
                    }
                    MochiButton(onClick = {
                        try {
                            val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
                            val f = File(dir, "shot_" + System.currentTimeMillis() + ".jpg")
                            val uri = FileShare.cameraOutputUri(context, f)
                            cameraFile = f
                            cameraUri = uri
                            camera.launch(uri)
                        } catch (t: Throwable) {
                            toast.show("没有可用的相机应用")
                        }
                    }) {
                        MochiIonicon("camera")
                        Text("  拍照")
                    }
                }
            }
        }

        item {
            MochiListCard {
                MochiListRow(
                    title = "SSTV 模式",
                    subtitle = state.mode.name + " · " + state.mode.width + " × " + state.mode.height,
                    trailing = {
                        MochiDropdown(
                            expanded = modeExpanded,
                            onExpandedChange = { modeExpanded = it },
                            items = persistentListOf(*state.modes.map { MochiItem(it.name) }.toTypedArray()),
                            onItemClick = { index ->
                                state.setMode(state.modes[index])
                                modeExpanded = false
                            },
                            selectedIndex = state.modes.indexOf(state.mode),
                        ) {
                            Text(state.mode.name)
                        }
                    },
                )
                MochiListRow(title = "采样率", subtitle = "编码时的音频采样率")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(32000, 44100, 48000).forEach { rate ->
                        MochiChip(
                            label = when (rate) {
                                32000 -> "32 kHz"
                                44100 -> "44.1 kHz"
                                else -> "48 kHz"
                            },
                            selected = state.sampleRate == rate,
                            onClick = { state.setSampleRate(rate) },
                        )
                    }
                }
            }
        }

        item {
            MochiListCard {
                MochiListRow(
                    title = "文字水印",
                    subtitle = if (settings.watermarkEnabled) "已开启 · 显示在图像角落" else "关闭时不写入任何文字",
                    trailing = {
                        MochiSwitch(settings.watermarkEnabled, {
                            settings.setWatermarkEnabled(it)
                            state.refreshPreview()
                        })
                    },
                )
                MochiListRow(
                    title = "编辑水印",
                    subtitle = "呼号、备注、时间都能自己写",
                    chevron = true,
                    onClick = { showWatermark = true },
                )
            }
        }

        item {
            MochiListCard {
                MochiListRow(
                    title = "发射状态",
                    subtitle = state.status,
                    trailing = {
                        Caption(if (state.transmitting) state.remaining else "")
                    },
                )
                if (state.transmitting) {
                    MochiProgressBar(state.progress, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.transmitting) {
                        MochiButton(onClick = { state.stop() }) {
                            MochiIonicon("stop")
                            Text("  停止发射")
                        }
                    } else {
                        MochiButton(onClick = { showOutput = true }) {
                            MochiIonicon("play")
                            Text("  开始发射")
                        }
                    }
                    if (state.lastWav != null) {
                        MochiButton(onClick = { state.shareLastWav() }) {
                            MochiIonicon("share-social")
                            Text("  分享 WAV")
                        }
                    }
                }
            }
        }

        item {
            MochiListCard {
                MochiListRow(title = "发射提示", subtitle = "手机扬声器音量调到最大，贴近对讲机麦克风；实时播放期间屏幕保持常亮。")
                MochiListRow(title = "或者先存 WAV", subtitle = "生成音频文件后可以离线传输，用别的设备播放。")
            }
        }
    }

    if (showOutput) {
        MochiDialog(
            visible = showOutput,
            onDismissRequest = { showOutput = false },
            title = "输出方式",
            actions = persistentListOf(
                MochiDialogAction("取消", MochiDialogActionRole.Cancel),
                MochiDialogAction("实时播放", MochiDialogActionRole.Primary, onClick = {
                    showOutput = false
                    state.startPlayback()
                }),
                MochiDialogAction("保存 WAV", onClick = {
                    showOutput = false
                    state.startWav()
                }),
            ),
        ) {
            Text("实时播放：用扬声器直接播出去，可对讲机发射。")
            Text("保存 WAV：生成音频文件，可离线传输或分享。")
        }
    }

    if (showWatermark) {
        WatermarkDialog(settings, state, onDismiss = { showWatermark = false })
    }
}

@Composable
private fun WatermarkDialog(settings: SettingsState, state: EncodeState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(settings.watermarkText(context)) }
    val positions = listOf("左上", "上", "右上", "左", "中", "右", "左下", "下", "右下")
    val sizes = listOf("小", "中", "大")
    val colors = listOf("白", "黑", "红", "黄", "青")

    MochiDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = "编辑文字水印",
        actions = persistentListOf(
            MochiDialogAction("恢复默认", onClick = {
                settings.resetWatermarkText()
                text = settings.watermarkText(context)
            }),
            MochiDialogAction("取消", MochiDialogActionRole.Cancel),
            MochiDialogAction("保存", MochiDialogActionRole.Primary, onClick = {
                settings.setWatermarkText(text)
                state.refreshPreview()
                onDismiss()
            }),
        ),
    ) {
        MochiTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "水印文本（可多行）",
            singleLine = false,
            maxLines = 4,
        )
        Caption("位置")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            positions.forEachIndexed { index, label ->
                MochiChip(label, settings.watermarkPosition == index, {
                    settings.setWatermarkPosition(index)
                    state.refreshPreview()
                })
            }
        }
        Caption("字号")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            sizes.forEachIndexed { index, label ->
                MochiChip(label, settings.watermarkSize == index, {
                    settings.setWatermarkSize(index)
                    state.refreshPreview()
                })
            }
        }
        Caption("颜色")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEachIndexed { index, label ->
                MochiChip(label, settings.watermarkColor == index, {
                    settings.setWatermarkColor(index)
                    state.refreshPreview()
                })
            }
        }
        MochiListRow(
            title = "文字描边",
            subtitle = "深色背景上更清楚",
            trailing = {
                MochiSwitch(settings.watermarkStroke, {
                    settings.setWatermarkStroke(it)
                    state.refreshPreview()
                })
            },
        )
    }
}
