package om.sstvsuite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ui.feice.mochi.theme.MochiTheme

/** 白底圆角媒体卡：图像预览、瀑布图这类非列表内容用 */
@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MochiTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        content = content,
    )
}

/** 细进度条，色调跟着主题走 */
@Composable
fun MochiProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val scheme = MochiTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(scheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.primary),
        )
    }
}

@Composable
fun RowScope.Spacer8() {
    Box(Modifier.height(1.dp))
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    ui.feice.mochi.basic.Text(
        text = text,
        modifier = modifier,
        style = MochiTheme.textStyles.footnote,
        color = MochiTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun CenteredBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center, content = content)
}

@Composable
fun RowSpacer() {
    Row(Modifier.height(8.dp)) {}
}
