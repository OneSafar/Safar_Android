/* Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 */
package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalHabitDarkTheme = compositionLocalOf { false }

/**
 * Hallmark Locked Design System Tokens for Safar Habit Tracker.
 * Architecture: Workbench / Ledger (Editorial Desk Journal).
 * Base: Warm Cream Paper (#FFFDF6). Accent: Royal Purple (#581C87) <= 3% viewport.
 * Completion: Royal Purple Stamp (#581C87). Trend accent: Deep Orange (#9A3412).
 */
object HabitColors {
    // Light palette (Editorial Cream Paper Ledger)
    val LightBackground = Color(0xFFFAF8F2)       // warm off-white cream paper
    val LightParchment = Color(0xFFF4F0E7)        // subtle paper tint for tab bars & insets
    val LightSurface = Color(0xFFFFFEFA)          // crisp card surface
    val LightTextPrimary = Color(0xFF20221F)       // deep charcoal ink
    val LightTextSecondary = Color(0xFF70736D)     // graphite pencil muted
    val LightTextTertiary = Color(0xFF98988F)      // light graphite guide
    val LightOutline = Color(0xFFE3DED3)          // delicate hairline rule (0.75-1dp)
    val LightOutlineStrong = Color(0xFFD8D1C5)    // firm hairline rule
    val LightFocusRing = Color(0xFF682392)        // high-contrast focus indicator

    // Accent (Surgical application only: active tab indicator, primary action, focus)
    val LightRoyalPurple = Color(0xFF682392)       // Deep Royal Purple anchor
    val LightRoyalPurple2 = Color(0xFF7F36AA)
    val LightRoyalPurpleBg = Color(0xFFF2E8F7)     // Soft Purple Tint
    val LightOnRoyalPurple = Color(0xFFFFFFFF)

    // Secondary analytic accent: reserved for streaks, trend lines, and callouts.
    val LightDeepOrange = Color(0xFF9A3412)
    val LightDeepOrangeBg = Color(0xFFFFE7D6)

    // Habit Completion: Tactile Royal Purple Stamp
    val LightCheckDone = Color(0xFF581C87)         // Royal purple completion stamp
    val LightCheckDoneBg = Color(0xFFF2E8F7)       // Soft purple wash
    val LightCheckDoneOn = Color(0xFFFFFFFF)

    // Destructive
    val LightError = Color(0xFFA44343)             // Muted Crimson
    val LightErrorBg = Color(0xFFFAEEEE)

    // Dark palette (Editorial Dark Slate Ledger)
    val DarkBackground = Color(0xFF141416)        // deep slate paper
    val DarkParchment = Color(0xFF1C1D22)         // dark parchment inset
    val DarkSurface = Color(0xFF22232A)           // elevated slate card
    val DarkTextPrimary = Color(0xFFF3F4F6)        // crisp light ink
    val DarkTextSecondary = Color(0xFF9CA3AF)      // muted silver
    val DarkTextTertiary = Color(0xFF6B7280)
    val DarkOutline = Color(0xFF2E2F38)           // dark hairline rule
    val DarkOutlineStrong = Color(0xFF3E404D)
    val DarkFocusRing = Color(0xFFC084FC)

    val DarkRoyalPurple = Color(0xFFC084FC)        // lighter purple for dark mode contrast
    val DarkRoyalPurple2 = Color(0xFFA855F7)
    val DarkRoyalPurpleBg = Color(0xFF2E1065)
    val DarkOnRoyalPurple = Color(0xFF141416)

    val DarkDeepOrange = Color(0xFFEA580C)
    val DarkDeepOrangeBg = Color(0xFF431407)

    val DarkCheckDone = Color(0xFFC084FC)
    val DarkCheckDoneBg = Color(0xFF2E1065)
    val DarkCheckDoneOn = Color(0xFF141416)

    val DarkError = Color(0xFFEF4444)
    val DarkErrorBg = Color(0xFF450A0A)

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

    // Editorial Ink Density (Shade Washes per Hallmark Approach 2)
    val InkDensity100 = Color(0xFF581C87)
    val InkDensity75 = Color(0xFF7E22CE)
    val InkDensity50 = Color(0xFFC4B5FD)
    val InkDensity25 = Color(0xFFEDE9FE)

    val DarkInkDensity100 = Color(0xFFC084FC)
    val DarkInkDensity75 = Color(0xFFA855F7)
    val DarkInkDensity50 = Color(0xFF7E22CE)
    val DarkInkDensity25 = Color(0xFF3B0764)

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
            ratio >= 0.99f -> if (isDark) DarkCheckDoneOn else LightCheckDoneOn // White text on 100%
            else -> if (isDark) DarkTextPrimary else LightTextPrimary // Dark charcoal date on washes
        }
    }
}
