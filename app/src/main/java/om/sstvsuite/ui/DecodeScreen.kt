package om.sstvsuite.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiGauge
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiToastState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold
import ui.feice.mochi.theme.MochiTheme

@Composable
fun DecodeScreen(state: DecodeState, settings: SettingsState, toast: MochiToastState) {
    val context = LocalContext.current
    val scheme = MochiTheme.colorScheme
    var lockExpanded by remember { mutableStateOf(false) }
    var showModes by remember { mutableStateOf(false) }
    var showEngine by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) state.decodeFile(uri)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
        if (granted) state.start() else toast.show("麦克风权限被拒绝，无法接收")
    }

    fun startWithPermission() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (ok) state.start() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    MochiLazyLargeTitleScaffold(
        title = "解码",
        subtitle = "对准电台，图像会一行行长出来",
        actions = {
            MochiDropdown(
                expanded = lockExpanded,
                onExpandedChange = { lockExpanded = it },
                items = persistentListOf(*state.lockOptions().map { MochiItem(it.label) }.toTypedArray()),
                onItemClick = { index ->
                    state.lockMode(state.lockOptions()[index].id)
                    lockExpanded = false
                },
                selectedIndex = state.lockOptions().indexOfFirst { it.id == state.lockedModeId },
            ) {
                MochiIonicon("lock-closed")
            }
        },
    ) {
        item {
            MediaCard {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = state.image
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "解码结果",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MochiIonicon("image", tint = scheme.onSurfaceVariant, size = 34.dp)
                            Caption("尚无图像")
                            Caption("开始接收后，图像会在这里逐行出现")
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.receiving) {
                        MochiButton(onClick = { state.stop() }) {
                            MochiIonicon("stop")
                            Text("  停止接收")
                        }
                    } else {
                        MochiButton(onClick = { startWithPermission() }) {
                            MochiIonicon("mic")
                            Text("  开始接收")
                        }
                    }
                    MochiButton(onClick = { filePicker.launch(arrayOf("audio/*")) }) {
                        MochiIonicon("document")
                        Text("  音频文件")
                    }
                }
            }
        }

        item {
            MochiListCard {
                MochiListRow(
                    title = "状态",
                    subtitle = state.status,
                    value = if (state.linesTotal > 0) state.linesDone.toString() + " / " + state.linesTotal + " 行" else null,
                )
                if (state.linesTotal > 0) {
                    MochiProgressBar(
                        state.linesDone.toFloat() / state.linesTotal.toFloat(),
                        Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                if (state.receiving || state.fileDecoding) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MochiGauge(value = state.level * 100f, size = 74.dp, strokeWidth = 6.dp)
                        Column {
                            Text(if (state.freq > 10f) state.freq.toInt().toString() + " Hz" else "— Hz")
                            Caption("信号电平")
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MochiButton(onClick = { state.saveImage() }) {
                        MochiIonicon("save")
                        Text("  保存")
                    }
                    MochiButton(onClick = { state.shareImage() }) {
                        MochiIonicon("share-social")
                        Text("  分享")
                    }
                }
            }
        }

        if (settings.waterfallVisible) {
            item {
                MediaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Caption("瀑布图")
                        Caption("  1000–2400 Hz")
                    }
                    val tick = state.waterfallTick
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.surfaceContainerHighest),
                    ) {
                        val rows = state.waterfall.rows
                        val rowHeight = size.height / rows
                        state.waterfall.forEachRow { row, x, level ->
                            if (x >= 0f) {
                                drawRect(
                                    color = scheme.primary.copy(alpha = (0.18f + 0.82f * level).coerceIn(0.18f, 1f)),
                                    topLeft = Offset(x * size.width - 1.5f, row * rowHeight),
                                    size = Size(3f, rowHeight.coerceAtLeast(1f)),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            MochiListCard {
                MochiListRow(
                    title = "解码引擎",
                    subtitle = state.engine.label + " · " + state.engine.hint,
                    chevron = true,
                    onClick = { showEngine = true },
                )
                MochiListRow(
                    title = "锁定模式",
                    subtitle = if (state.lockedModeId.isEmpty()) "自动识别 (VIS)" else "已锁定：" + state.lockedLabel(),
                    chevron = true,
                    onClick = { showModes = true },
                )
                MochiListRow(
                    title = "自动保存",
                    subtitle = "解码完成后存入相册 Pictures/SSTV",
                    trailing = {
                        ui.feice.mochi.basic.MochiSwitch(settings.autoSave, { settings.setAutoSave(it) })
                    },
                )
            }
        }

        item {
            MochiListCard {
                MochiListRow(title = "接收提示", subtitle = "1. 电台音量中等偏上再播放 SSTV 信号")
                MochiListRow(title = "", subtitle = "2. 手机麦克风朝向电台扬声器，距离约 20–30 厘米")
                MochiListRow(title = "", subtitle = "3. 收到声音但找不到 VIS 头时，先点开始接收再让对方从头播放")
            }
        }
    }

    if (showEngine) {
        MochiDialog(
            visible = showEngine,
            onDismissRequest = { showEngine = false },
            title = "解码引擎",
            actions = persistentListOf(
                MochiDialogAction("关闭", MochiDialogActionRole.Cancel),
            ),
        ) {
            Column {
                DecodeEngine.entries.forEach { option ->
                    MochiListRow(
                        title = option.label,
                        subtitle = option.hint,
                        checked = option == state.engine,
                        onClick = {
                            state.setEngine(option)
                            showEngine = false
                        },
                    )
                }
            }
        }
    }

    if (showModes) {
        MochiDialog(
            visible = showModes,
            onDismissRequest = { showModes = false },
            title = "锁定模式",
            actions = persistentListOf(
                MochiDialogAction("自动识别", MochiDialogActionRole.Primary, onClick = {
                    state.lockMode("")
                    showModes = false
                }),
                MochiDialogAction("取消", MochiDialogActionRole.Cancel),
            ),
        ) {
            Column {
                state.lockOptions().forEach { option ->
                    MochiListRow(
                        title = option.label,
                        subtitle = option.subtitle,
                        checked = option.id == state.lockedModeId,
                        onClick = {
                            state.lockMode(option.id)
                            showModes = false
                        },
                    )
                }
            }
        }
    }
}
