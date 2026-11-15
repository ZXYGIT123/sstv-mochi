package om.sstvsuite.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import ui.feice.mochi.effect.overscroll.ProvideMochiOverscroll
import ui.feice.mochi.icons.MochiIconStyle
import ui.feice.mochi.icons.MochiIonicon
import ui.feice.mochi.navigation.MochiTabScaffold
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController

@Composable
fun SstvRoot() {
    val context = LocalContext.current
    val settings = remember { SettingsState(context) }
    val encode = remember { EncodeState(context, settings) }
    val decode = remember { DecodeState(context, settings) }
    val controller = remember(settings.nightMode, settings.dynamicColor) {
        ThemeController(settings.colorSchemeMode)
    }

    MochiTheme(controller = controller) {
        val toast = rememberMochiToastState()
        val tabs = remember {
            persistentListOf(
                MochiTabItem(
                    label = "编码",
                    icon = { tint -> MochiIonicon("radio", tint = tint) },
                    selectedIcon = { tint -> MochiIonicon("radio", style = MochiIconStyle.Filled, tint = tint) },
                ),
                MochiTabItem(
                    label = "解码",
                    icon = { tint -> MochiIonicon("mic", tint = tint) },
                    selectedIcon = { tint -> MochiIonicon("mic", style = MochiIconStyle.Filled, tint = tint) },
                ),
                MochiTabItem(
                    label = "设置",
                    icon = { tint -> MochiIonicon("options", tint = tint) },
                    selectedIcon = { tint -> MochiIonicon("options", style = MochiIconStyle.Filled, tint = tint) },
                ),
            )
        }
        ProvideMochiOverscroll {
            MochiTabScaffold(items = tabs) { index ->
                when (index) {
                    0 -> EncodeScreen(encode, settings, toast)
                    1 -> DecodeScreen(decode, settings, toast)
                    else -> SettingsScreen(settings, toast)
                }
            }
            MochiToastHost(toast)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            encode.dispose()
            decode.dispose()
        }
    }
}
