package com.safarparmar.app.ui.nishtha

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Tabs
import com.composables.ui.components.TabList
import com.composables.ui.components.Tab
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme

internal data class NishthaWarmColors(val canvas: Color, val paper: Color, val ink: Color, val muted: Color,
    val action: Color, val onAction: Color, val peach: Color, val lilac: Color, val mint: Color, val line: Color) {
    // Calendar cells retain their original fill independently of the page canvas.
    val calendarCell: Color get() = if (canvas == BgDark) Color(0xFF211921) else canvas
}
internal fun nishthaWarmColors(light: Boolean) = if (light) NishthaWarmColors(
    Color(0xFFFFF5F0), Color(0xFFFFFDFC), Color(0xFF39132E), Color(0xFF75616F),
    Color(0xFF542347), Color.White, Color(0xFFFFCBBE), Color(0xFFEDE0F4), Color(0xFFC9EFE0), Color(0xFFE8DEE5))
else NishthaWarmColors(BgDark, Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB),
    Color(0xFFF0BEDC), Color(0xFF39132E), Color(0xFF684047), Color(0xFF513551), Color(0xFF28574D), Color(0xFF654D66))

@Composable
internal fun NishthaWarmTheme(light: Boolean, content: @Composable () -> Unit) {
    val c = nishthaWarmColors(light)
    CompositionLocalProvider(LocalSafarComposablesAccent provides c.action) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = c.canvas, surface = c.paper,
            onSurface = c.ink, onBackground = c.ink, onSurfaceVariant = c.muted, primary = c.action,
            onPrimary = c.onAction, outline = c.line, outlineVariant = c.line)) {
            SafarComposablesTheme { content() }
        }
    }
}

@Composable
internal fun WarmNishthaCard(c: NishthaWarmColors, modifier: Modifier = Modifier, spacing: androidx.compose.ui.unit.Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.paper)
        .border(1.dp, c.line, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
internal fun WarmNishthaTabs(selected: NishthaTab, onSelect: (NishthaTab) -> Unit, light: Boolean) {
    AttachedNishthaNavigation(selected, onSelect)
}
