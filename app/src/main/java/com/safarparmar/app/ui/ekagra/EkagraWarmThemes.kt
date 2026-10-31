package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Legacy ids remain stable because the service persists the original list index. */
internal data class EkagraWarmPalette(val title: String, val accent: Color, val light: Color, val glow: Color, val base: Color)
internal fun ekagraWarmPalette(name: String) = when (name) {
    "Focus" -> EkagraWarmPalette("Midnight Blue", Color(0xFF152C4C), Color(0xFFC8DDF0), Color(0xFF296C9A), Color(0xFF091D37))
    "Journal" -> EkagraWarmPalette("Ember Glow", Color(0xFF8A3F22), Color(0xFFFFD0AA), Color(0xFFBD632C), Color(0xFF462218))
    "Habits" -> EkagraWarmPalette("Teal Stillness", Color(0xFF123D3B), Color(0xFFC1E5DB), Color(0xFF388B83), Color(0xFF092D30))
    else -> EkagraWarmPalette("Plum Dusk", Color(0xFF491438), Color(0xFFE3CAEF), Color(0xFF873361), Color(0xFF310B29))
}
internal val LocalEkagraWarmPalette = staticCompositionLocalOf { ekagraWarmPalette("Peace") }

internal fun ekagraWarmColorScheme(theme: VisualTheme, dark: Boolean): ColorScheme {
    val p = ekagraWarmPalette(theme.name)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val canvas = if (dark) Color(0xFF211D22) else Color(0xFFFFF7F1)
    val paper = if (dark) Color(0xFF302A32) else Color(0xFFFFFDFC)
    val ink = if (dark) Color(0xFFFFF3F1) else p.accent
    val muted = if (dark) Color(0xFFCEC2CE) else Color(0xFF776A76)
    val soft = if (dark) p.accent.copy(alpha = .45f) else p.light.copy(alpha = .48f)
    val line = if (dark) Color(0xFF655865) else Color(0xFFEADFE5)
    return base.copy(primary = if (dark) p.light else p.accent, onPrimary = if (dark) p.accent else Color.White,
        primaryContainer = soft, onPrimaryContainer = ink, secondary = if (dark) p.light else p.accent,
        onSecondary = if (dark) p.accent else Color.White, secondaryContainer = soft, onSecondaryContainer = ink,
        background = canvas, onBackground = ink, surface = paper, onSurface = ink, surfaceVariant = soft,
        onSurfaceVariant = muted, surfaceContainerLowest = paper, surfaceContainerLow = paper,
        surfaceContainer = paper, surfaceContainerHigh = paper, surfaceContainerHighest = soft,
        outlineVariant = line, outline = muted, tertiary = if (dark) p.light else p.accent)
}

/** Static draw-only lighting; no bitmap, blur, infinite animation, or layout work per frame. */
internal fun Modifier.ekagraTimerCard(p: EkagraWarmPalette) = clip(RoundedCornerShape(26.dp)).drawBehind {
    drawRect(Brush.linearGradient(listOf(p.glow, p.base), start = Offset.Zero, end = Offset(size.width, size.height)))
    val center = when (p.title) {
        "Ember Glow" -> Offset(0f, size.height * .8f)
        "Teal Stillness" -> Offset(size.width, size.height * .35f)
        "Midnight Blue" -> Offset(size.width, 0f)
        else -> Offset(size.width, size.height * .5f)
    }
    drawRect(Brush.radialGradient(listOf(p.glow.copy(alpha = .65f), Color.Transparent), center, size.maxDimension * .85f))
}

@Composable
internal fun EkagraPaper(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.surface, RoundedCornerShape(22.dp))
        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun EkagraSessionNameInput(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = androidx.compose.foundation.text.input.rememberTextFieldState(value)
    val current by rememberUpdatedState(value)
    val callback by rememberUpdatedState(onChange)
    LaunchedEffect(value) { if (state.text.toString() != value) state.edit { replace(0, length, value) } }
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.collect { if (it != current) callback(it) } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.composables.ui.components.Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_session_name_sentence),
            color = MaterialTheme.colorScheme.onSurface, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        com.composables.ui.components.TextField(state = state, modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_session_name_sentence),
            placeholder = { com.composables.ui.components.Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_worked_on_hint)) },
            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp))
    }
}
