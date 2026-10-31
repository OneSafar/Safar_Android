/* Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 */
package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalHabitDarkTheme = compositionLocalOf { false }

/** Scoped blush/plum palette shared with Profile, Mehfil and SAFAR tutorials.
 * Token names are retained for existing components; only color roles change. */
object HabitColors {
    // Light palette — blush paper and plum ink
    val LightBackground = Color(0xFFFFF5F0)
    val LightParchment = Color(0xFFFFF0F6)
    val LightSurface = Color(0xFFFFFDFC)
    val LightTextPrimary = Color(0xFF39132E)
    val LightTextSecondary = Color(0xFF75616F)
    val LightTextTertiary = Color(0xFF8C7885)
    val LightOutline = Color(0xFFE8DEE5)
    val LightOutlineStrong = Color(0xFFCEB9C9)
    val LightFocusRing = Color(0xFF542347)

    // Primary plum action and lilac selection roles
    val LightRoyalPurple = Color(0xFF542347)
    val LightRoyalPurple2 = Color(0xFF793D67)
    val LightRoyalPurpleBg = Color(0xFFEDE0F4)
    val LightOnRoyalPurple = Color(0xFFFFFFFF)

    // Secondary analytic accent: reserved for streaks, trend lines, and callouts.
    val LightDeepOrange = Color(0xFF9A4E25)
    val LightDeepOrangeBg = Color(0xFFFFCBBE)

    // Completion uses the shared action color and mint wash
    val LightCheckDone = Color(0xFF542347)
    val LightCheckDoneBg = Color(0xFFC9EFE0)
    val LightCheckDoneOn = Color(0xFFFFFFFF)

    // Destructive
    val LightError = Color(0xFFB3261E)
    val LightErrorBg = Color(0xFFFFF0EF)

    // Dark palette — warm plum surfaces
    val DarkBackground = Color(0xFF131316)
    val DarkParchment = Color(0xFF211921)
    val DarkSurface = Color(0xFF342735)
    val DarkTextPrimary = Color(0xFFFFF0F6)
    val DarkTextSecondary = Color(0xFFCFBECB)
    val DarkTextTertiary = Color(0xFFAF98A8)
    val DarkOutline = Color(0xFF654D66)
    val DarkOutlineStrong = Color(0xFF876B82)
    val DarkFocusRing = Color(0xFFF0BEDC)

    val DarkRoyalPurple = Color(0xFFF0BEDC)
    val DarkRoyalPurple2 = Color(0xFFCDBBE9)
    val DarkRoyalPurpleBg = Color(0xFF513551)
    val DarkOnRoyalPurple = Color(0xFF39132E)

    val DarkDeepOrange = Color(0xFFFFB08A)
    val DarkDeepOrangeBg = Color(0xFF684047)

    val DarkCheckDone = Color(0xFFF0BEDC)
    val DarkCheckDoneBg = Color(0xFF28574D)
    val DarkCheckDoneOn = Color(0xFF39132E)

    val DarkError = Color(0xFFFFB4AB)
    val DarkErrorBg = Color(0xFF43282E)

    // Dynamic Getters
    val Background: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkBackground else LightBackground

    val Parchment: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkParchment else LightParchment

    val Surface: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkSurface else LightSurface

    val TextPrimary: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkTextPrimary else LightTextPrimary

    val TextSecondary: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkTextSecondary else LightTextSecondary

    val TextTertiary: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkTextTertiary else LightTextTertiary

    val Outline: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkOutline else LightOutline

    val OutlineStrong: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkOutlineStrong else LightOutlineStrong

    val FocusRing: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkFocusRing else LightFocusRing

    val RoyalPurple: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkRoyalPurple else LightRoyalPurple

    val RoyalPurple2: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkRoyalPurple2 else LightRoyalPurple2

    val RoyalPurpleBg: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkRoyalPurpleBg else LightRoyalPurpleBg

    val OnRoyalPurple: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkOnRoyalPurple else LightOnRoyalPurple

    val DeepOrange: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkDeepOrange else LightDeepOrange

    val DeepOrangeBg: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkDeepOrangeBg else LightDeepOrangeBg

    val CheckDone: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkCheckDone else LightCheckDone

    val CheckDoneBg: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkCheckDoneBg else LightCheckDoneBg

    val CheckDoneOn: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkCheckDoneOn else LightCheckDoneOn

    val Error: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkError else LightError

    val ErrorBg: Color
        @Composable get() = if (LocalHabitDarkTheme.current) DarkErrorBg else LightErrorBg

    // Compatibility alias for NavyBlue (maps cleanly to TextPrimary or Outline)
    val NavyBlue: Color
        @Composable get() = TextPrimary

    // Plum/lilac density ramp for progress and heatmaps
    val InkDensity100 = Color(0xFF542347)
    val InkDensity75 = Color(0xFFA97998)
    val InkDensity50 = Color(0xFFCDBBE9)
    val InkDensity25 = Color(0xFFEDE0F4)

    val DarkInkDensity100 = Color(0xFFF0BEDC)
    val DarkInkDensity75 = Color(0xFFAF7D9C)
    val DarkInkDensity50 = Color(0xFF79546E)
    val DarkInkDensity25 = Color(0xFF483149)

    @Composable
    fun getInkDensityColor(ratio: Float): Color {
        val isDark = LocalHabitDarkTheme.current
        return when {
            ratio >= 0.99f -> if (isDark) DarkInkDensity100 else InkDensity100
            ratio >= 0.70f -> if (isDark) DarkInkDensity75 else InkDensity75
            ratio >= 0.45f -> if (isDark) DarkInkDensity50 else InkDensity50
            ratio >= 0.20f -> if (isDark) DarkInkDensity25 else InkDensity25
            else -> if (isDark) DarkSurface else LightSurface
        }
    }

    @Composable
    fun getInkDensityText(ratio: Float): Color {
        val isDark = LocalHabitDarkTheme.current
        return when {
            ratio >= 0.99f -> if (isDark) DarkCheckDoneOn else LightCheckDoneOn // Contrast foreground on the strongest fill
            else -> if (isDark) DarkTextPrimary else LightTextPrimary // Shared text color on lighter washes
        }
    }
}

/** Color-only scope: typography, shapes and all component sizing remain inherited. */
@Composable
internal fun HabitAccentTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalHabitDarkTheme provides isDarkTheme) {
        val scheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
            background = HabitColors.Background, onBackground = HabitColors.TextPrimary,
            surface = HabitColors.Surface, onSurface = HabitColors.TextPrimary,
            surfaceVariant = HabitColors.Parchment, onSurfaceVariant = HabitColors.TextSecondary,
            primary = HabitColors.RoyalPurple, onPrimary = HabitColors.OnRoyalPurple,
            primaryContainer = HabitColors.RoyalPurpleBg, onPrimaryContainer = HabitColors.TextPrimary,
            secondary = HabitColors.RoyalPurple2, onSecondary = HabitColors.OnRoyalPurple,
            secondaryContainer = HabitColors.CheckDoneBg, onSecondaryContainer = HabitColors.TextPrimary,
            error = HabitColors.Error, errorContainer = HabitColors.ErrorBg,
            onError = if (isDarkTheme) HabitColors.DarkBackground else Color.White,
            onErrorContainer = HabitColors.Error,
            outline = HabitColors.OutlineStrong, outlineVariant = HabitColors.Outline,
        )
        androidx.compose.runtime.CompositionLocalProvider(
            com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides HabitColors.RoyalPurple,
        ) {
            androidx.compose.material3.MaterialTheme(colorScheme = scheme) {
                com.safarparmar.app.ui.theme.SafarComposablesTheme { content() }
            }
        }
    }
}
