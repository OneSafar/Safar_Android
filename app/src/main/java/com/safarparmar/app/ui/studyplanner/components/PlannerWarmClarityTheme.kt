package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.MulishFontFamily
import com.safarparmar.app.ui.theme.isLightBackground

/** Opt-in: planner primitives also serve other features, whose appearance must stay unchanged. */
val LocalPlannerWarmClarity = staticCompositionLocalOf { false }

/** Contrast-aware foreground for legacy button slots that accept arbitrary accents. */
internal fun plannerButtonForeground(background: Color): Color {
    fun contrast(foreground: Color): Float =
        (maxOf(foreground.luminance(), background.luminance()) + 0.05f) /
            (minOf(foreground.luminance(), background.luminance()) + 0.05f)
    val plum = Color(0xFF39132E)
    return when {
        contrast(plum) >= 4.5f -> plum
        contrast(Color.White) >= 4.5f -> Color.White
        else -> Color.Black
    }
}

@Immutable
data class PlannerWarmColors(
    val canvas: Color, val card: Color, val ink: Color, val muted: Color,
    val action: Color, val onAction: Color, val peach: Color, val mint: Color,
    val border: Color, val success: Color, val attention: Color, val error: Color,
)

fun plannerWarmColors(dark: Boolean): PlannerWarmColors = if (dark) PlannerWarmColors(
    canvas = Color(0xFF211921), card = Color(0xFF342735), ink = Color(0xFFFFF0F6),
    muted = Color(0xFFCFBECB), action = Color(0xFFF0BEDC), onAction = Color(0xFF39132E),
    peach = Color(0xFF684047), mint = Color(0xFF28574D), border = Color(0xFF624B60),
    success = Color(0xFF85DFC0), attention = Color(0xFFFFCF87), error = Color(0xFFFFAAA6),
) else PlannerWarmColors(
    canvas = Color(0xFFFFF5F0), card = Color(0xFFFFFDFC), ink = Color(0xFF39132E),
    muted = Color(0xFF75616F), action = Color(0xFF542347), onAction = Color.White,
    peach = Color(0xFFFFCBBE), mint = Color(0xFFC9EFE0), border = Color(0xFFE8D6DE),
    success = Color(0xFF146A50), attention = Color(0xFF915400), error = Color(0xFFB52C43),
)

@Composable
fun PlannerWarmClarityTheme(
    dark: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
    content: @Composable () -> Unit,
) {
    val c = plannerWarmColors(dark)
    val colors = MaterialTheme.colorScheme.copy(
        primary = c.action, onPrimary = c.onAction,
        primaryContainer = c.peach, onPrimaryContainer = c.ink,
        secondary = c.success, onSecondary = c.canvas,
        secondaryContainer = c.mint, onSecondaryContainer = c.ink,
        tertiary = c.attention, onTertiary = c.canvas,
        background = c.canvas, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, surfaceTint = Color.Transparent,
        surfaceVariant = c.canvas, onSurfaceVariant = c.muted,
        surfaceDim = c.canvas, surfaceBright = c.card,
        inverseSurface = c.ink, inverseOnSurface = c.card,
        surfaceContainerLowest = c.canvas, surfaceContainerLow = c.canvas,
        surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.peach,
        outline = c.muted, outlineVariant = c.border, error = c.error,
    )
    val type = MaterialTheme.typography
    val typography = type.copy(
        headlineLarge = type.headlineLarge.copy(fontFamily = MulishFontFamily, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = type.headlineMedium.copy(fontFamily = MulishFontFamily, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall = type.headlineSmall.copy(fontFamily = MulishFontFamily, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        titleLarge = type.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = type.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodySmall = type.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelSmall = type.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
    )
    CompositionLocalProvider(LocalPlannerWarmClarity provides true, LocalPlannerIsDarkTheme provides dark) {
        MaterialTheme(colorScheme = colors, typography = typography,
            shapes = Shapes(RoundedCornerShape(12.dp), RoundedCornerShape(16.dp), RoundedCornerShape(20.dp), RoundedCornerShape(24.dp), RoundedCornerShape(28.dp)),
            content = content)
    }
}
