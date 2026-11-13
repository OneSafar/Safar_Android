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
import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleMid
import com.safarparmar.app.ui.theme.RoyalPurpleDeep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Legacy ids remain stable because the service persists the original list index. */
internal data class EkagraWarmPalette(val title: String, val accent: Color, val light: Color, val glow: Color, val base: Color)
internal fun ekagraWarmPalette(name: String) = when (name) {
    "Focus" -> EkagraWarmPalette("Midnight Blue", Color(0xFF152C4C), Color(0xFFC8DDF0), Color(0xFF296C9A), Color(0xFF091D37))
    "Journal" -> EkagraWarmPalette("Ember Glow", Color(0xFF8A3F22), Color(0xFFFFD0AA), Color(0xFFBD632C), Color(0xFF462218))
    "Habits" -> EkagraWarmPalette("Teal Stillness", Color(0xFF123D3B), Color(0xFFC1E5DB), Color(0xFF388B83), Color(0xFF092D30))
    else -> EkagraWarmPalette("Royal Purple", RoyalPurple, RoyalPurpleSoft, RoyalPurpleMid, RoyalPurpleDeep)
}
internal val LocalEkagraWarmPalette = staticCompositionLocalOf { ekagraWarmPalette("Peace") }

internal fun ekagraWarmColorScheme(theme: VisualTheme, dark: Boolean): ColorScheme {
    val p = ekagraWarmPalette(theme.name)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    // All Ekagra content sits on the selected theme's dark gradient, including in light mode.
    val canvas = p.base
    val paper = androidx.compose.ui.graphics.lerp(p.base, p.accent, .55f)
    val ink = Color.White
    val muted = p.light
    val soft = p.light.copy(alpha = .18f)
    val line = p.light.copy(alpha = .28f)
    return base.copy(primary = p.light, onPrimary = p.accent,
        primaryContainer = soft, onPrimaryContainer = ink, secondary = p.light,
        onSecondary = p.accent, secondaryContainer = soft, onSecondaryContainer = ink,
        background = canvas, onBackground = ink, surface = paper, onSurface = ink, surfaceVariant = soft,
        onSurfaceVariant = muted, surfaceContainerLowest = paper, surfaceContainerLow = paper,
        surfaceContainer = paper, surfaceContainerHigh = paper, surfaceContainerHighest = soft,
        outlineVariant = line, outline = muted, tertiary = p.light)
}

/** Theme picker previews use the same lighting as the full-screen canvas. */
internal fun Modifier.ekagraTimerCard(p: EkagraWarmPalette) =
    clip(RoundedCornerShape(26.dp)).ekagraThemeBackground(p)

/** Static draw-only lighting; no bitmap, blur, infinite animation, or layout work per frame. */
internal fun Modifier.ekagraThemeBackground(p: EkagraWarmPalette) = drawBehind {
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
