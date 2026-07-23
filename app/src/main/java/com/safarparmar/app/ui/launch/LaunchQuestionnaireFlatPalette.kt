package com.safarparmar.app.ui.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Launch questionnaire palette.
 *
 * Ink and surfaces key off [MaterialTheme] background luminance — the same
 * signal used for macOS glass tiles — so dark mode never paints light-on-white
 * (or dark-on-charcoal) text. Do **not** route through PlannerFlatColors here;
 * that local can disagree with Material on this pre-home flow.
 */
object LaunchFlatColors {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.isLightBackground()

    private val isDark: Boolean
        @Composable get() = !isLight

    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    /** Stronger than typical muted — must stay readable on cream and on glass. */
    val Muted @Composable get() = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)

    /**
     * Ink for text drawn **on** opaque Control Center glass.
     * Glass body is always #F9F9FB (light) or #2C2C2E (dark) per [isLight].
     */
    val OnGlassText @Composable get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val OnGlassMuted @Composable get() = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    /** Primary Kavach / setup accent — violet */
    val Primary @Composable get() = if (isDark) Color(0xFFC084FC) else Color(0xFF6D28D9)

    val Beast @Composable get() = if (isDark) Color(0xFFFF8A65) else Color(0xFFE64A19)
    val AlwaysOn @Composable get() = Primary
    val Normal @Composable get() = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)

    val Habit @Composable get() = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    val Journal @Composable get() = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    val Calm @Composable get() = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
}
