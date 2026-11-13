package com.safarparmar.app.ui.dhyan

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Dhyan flat-hairline + lavender accent palette.
 * Surfaces/ink key off Material background luminance (same signal as glass tiles).
 */
object DhyanFlatColors {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.isLightBackground()

    private val isDark: Boolean
        @Composable get() = !isLight

    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFAF7FF)
    val ComponentBackground @Composable get() = CardBg
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    val Muted @Composable get() = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)

    val OnGlassText @Composable get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val OnGlassMuted @Composable get() = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    /** Royal purple and lavender accents on the lavender Dhyan canvas */
    val Primary @Composable get() = if (isDark) RoyalPurpleDark else RoyalPurple
    val PrimaryDeep = RoyalPurple
    val PrimarySoft @Composable get() = if (isDark) RoyalPurpleDark.copy(alpha = 0.20f) else RoyalPurple.copy(alpha = 0.12f)
    val PrimaryContainer @Composable get() = if (isDark) RoyalPurpleSoftDark else RoyalPurpleSoft
    val BorderHairline @Composable get() = if (isDark) RoyalPurpleDark.copy(alpha = 0.20f) else RoyalPurple.copy(alpha = 0.15f)

    /** Card surfaces & borders */
    val CardBg @Composable get() = if (isDark) Color(0xFF241B33) else Color(0xFFFFFFFF)
    val CardBorder @Composable get() = if (isDark) Color(0xFF423052) else Color(0xFFE1D4F3)

    /** Badges & status */
    val Emerald @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val EmeraldBg @Composable get() = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val EmeraldBorder @Composable get() = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.25f)

    val Rose @Composable get() = if (isDark) RoyalPurpleDark else RoyalPurple
    val Lotus @Composable get() = if (isDark) Color(0xFFB39DDB) else Color(0xFFEEE5FD)
    val Calm @Composable get() = if (isDark) Color(0xFF512395) else Color(0xFF9575CD)
    val Sky @Composable get() = if (isDark) Color(0xFFD4C4FF) else Color(0xFF7954B1)

    /** Lavender gradient canvas behind the session screen. */
    val CanvasBrush @Composable get() = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF181222),
                Color(0xFF2C203E),
                Color(0xFF181222),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFAF7FF),
                Color(0xFFEEE5FD),
                Color(0xFFDDD0F1),
            ),
        )
    }

    fun glassBody(isLight: Boolean): Color =
        if (isLight) Color(0xFFFFFFFF) else Color(0xFF241B33)

    fun onGlassText(isLight: Boolean): Color =
        if (isLight) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)

    fun onGlassMuted(isLight: Boolean): Color =
        if (isLight) Color(0xFF64748B) else Color(0xFFCBD5E1)
}

