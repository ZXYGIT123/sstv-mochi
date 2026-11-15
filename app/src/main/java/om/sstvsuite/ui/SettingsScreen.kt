package om.sstvsuite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import om.sstvsuite.engine.ModeRegistry
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiToastState
import ui.feice.mochi.basic.Text
import ui.feice.mochi.list.MochiListCard
import ui.feice.mochi.list.MochiListRow
import ui.feice.mochi.list.MochiListSectionHeader
import ui.feice.mochi.navigation.MochiLazyLargeTitleScaffold

@Composable
fun SettingsScreen(settings: SettingsState, toast: MochiToastState) {
    var nightExpanded by remember { mutableStateOf(false) }
    var rateExpanded by remember { mutableStateOf(false) }
    var lockExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val modes = remember { ModeRegistry.all() }

    val nightLabels = listOf("跟随系统", "浅色", "深色")
    val rateLabels = listOf("32 kHz", "44.1 kHz", "48 kHz")
    val rates = listOf(32000, 44100, 48000)
    val nightValues = listOf(-1, 1, 2)

    MochiLazyLargeTitleScaffold(title = "设置") {
        item { MochiListSectionHeader("通用") }
        item {
            MochiListCard {
                MochiListRow(
                    title = "自动识别模式",
                    subtitle = "通过 VIS 自动判断 SSTV 模式",
                    trailing = { MochiSwitch(settings.autoDetect, { settings.setAutoDetect(it) }) },
                )
                MochiListRow(
                    title = "自动保存解码图像",
                    subtitle = "解码完成后保存到相册 Pictures/SSTV",
                    trailing = { MochiSwitch(settings.autoSave, { settings.setAutoSave(it) }) },
                )
                MochiListRow(
                    title = "显示瀑布图",
                    subtitle = "解码时显示实时频率轨迹",
                    trailing = { MochiSwitch(settings.waterfallVisible, { settings.setWaterfallVisible(it) }) },
                )
            }
        }
        item {
            MochiListCard {
                MochiListRow(
                    title = "锁定模式",
                    subtitle = "不使用 VIS 时手动指定模式",
                    trailing = {
                        MochiDropdown(
                            expanded = lockExpanded,
                            onExpandedChange = { lockExpanded = it },
                            items = persistentListOf(*modes.map { MochiItem(it.name) }.toTypedArray()),
                            onItemClick = { index ->
                                settings.setLockedModeId(modes[index].id)
                                lockExpanded = false
                            },
                            selectedIndex = modes.indexOfFirst { it.id == settings.lockedModeId },
                        ) {
                            Text(if (settings.lockedModeId.isEmpty()) "自动" else (settings.lockedMode()?.name ?: "自动"))
                        }
                    },
                )
                MochiListRow(
                    title = "默认采样率",
                    subtitle = "编码时使用的音频采样率",
                    trailing = {
                        MochiDropdown(
                            expanded = rateExpanded,
                            onExpandedChange = { rateExpanded = it },
                            items = persistentListOf(*rateLabels.map { MochiItem(it) }.toTypedArray()),
                            onItemClick = { index ->
                                settings.setSampleRate(rates[index])
                                rateExpanded = false
                            },
                            selectedIndex = rates.indexOf(settings.sampleRate),
                        ) {
                            Text(rateLabels[rates.indexOf(settings.sampleRate).coerceAtLeast(0)])
                        }
                    },
                )
                MochiListRow(
                    title = "引擎状态",
                    subtitle = "DSP 引擎就绪 · " + settings.modeCount + " 种模式",
                )
            }
        }

        item { MochiListSectionHeader("外观") }
        item {
            MochiListCard {
                MochiListRow(
                    title = "夜间模式",
                    subtitle = "跟随系统、浅色或深色主题",
                    trailing = {
                        MochiDropdown(
                            expanded = nightExpanded,
                            onExpandedChange = { nightExpanded = it },
                            items = persistentListOf(*nightLabels.map { MochiItem(it) }.toTypedArray()),
                            onItemClick = { index ->
                                settings.setNightMode(nightValues[index])
                                nightExpanded = false
                            },
                            selectedIndex = nightValues.indexOf(settings.nightMode).coerceAtLeast(0),
                        ) {
                            Text(nightLabels[nightValues.indexOf(settings.nightMode).coerceAtLeast(0)])
                        }
                    },
                )
                MochiListRow(
                    title = "动态取色",
                    subtitle = "使用系统壁纸配色（Android 12 及以上）",
                    trailing = { MochiSwitch(settings.dynamicColor, { settings.setDynamicColor(it) }) },
                )
            }
        }

        item { MochiListSectionHeader("其他") }
        item {
            MochiListCard {
                MochiListRow(
                    title = "关于 SSTV 收发站",
                    subtitle = "版本、致谢、许可证与隐私说明",
                    chevron = true,
                    onClick = { showAbout = true },
                )
            }
        }
    }

    if (showAbout) {
        MochiDialog(
            visible = showAbout,
            onDismissRequest = { showAbout = false },
            title = "关于 SSTV 收发站",
            actions = persistentListOf(MochiDialogAction("好的", MochiDialogActionRole.Primary)),
        ) {
            Text("1.0.3-mochi · 界面由 mochi-compose 重写")
            Text("DSP 引擎：纯 Java 实现的 SSTV 编解码，支持 Martin、Scottie、Robot、PD、Wraase 系列。")
            Text("致谢：Robot36（Ahmet Inan）、SSTV Encoder（om.sstvencoder）。")
            Text("隐私：不收集、不上传任何数据；麦克风、相机与图片只在设备本地处理。")
        }
    }
}
