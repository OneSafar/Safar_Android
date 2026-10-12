package com.safarparmar.app.ui.dhyan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Dhyan flat-hairline + pink accent palette.
 * Surfaces/ink key off Material background luminance (same signal as glass tiles).
 */
object DhyanFlatColors {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.isLightBackground()

    private val isDark: Boolean
        @Composable get() = !isLight

    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    val Muted @Composable get() = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)

    val OnGlassText @Composable get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val OnGlassMuted @Composable get() = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    /** Primary Dhyan accent — exact web Deep Pink (#BE185D light, #F472B6 dark) */
    val Primary @Composable get() = if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
    val PrimaryDeep = Color(0xFF9D174D)
    val PrimarySoft @Composable get() = if (isDark) Color(0xFFF472B6).copy(alpha = 0.20f) else Color(0xFFBE185D).copy(alpha = 0.12f)
    val PrimaryContainer @Composable get() = if (isDark) Color(0xFF27141E) else Color(0xFFFDF2F8)
    val BorderHairline @Composable get() = if (isDark) Color(0xFFF472B6).copy(alpha = 0.20f) else Color(0xFFBE185D).copy(alpha = 0.15f)

    /** Card surfaces & borders */
    val CardBg @Composable get() = if (isDark) Color(0xFF21171C) else Color(0xFFFFFFFF)
    val CardBorder @Composable get() = if (isDark) Color(0xFF3A2831) else Color(0xFFF1D8E3)

    /** Badges & status */
    val Emerald @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val EmeraldBg @Composable get() = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val EmeraldBorder @Composable get() = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.25f)

    val Rose @Composable get() = if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
    val Lotus @Composable get() = if (isDark) Color(0xFFE05282) else Color(0xFFFFCDE0)
    val Calm @Composable get() = if (isDark) Color(0xFF8A133B) else Color(0xFFE37A9A)
    val Sky @Composable get() = if (isDark) Color(0xFF7CB9E8) else Color(0xFF5B9BD5)

    /** Pink gradient canvas behind the session screen. */
    val CanvasBrush @Composable get() = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1A1016),
                Color(0xFF2A1520),
                Color(0xFF1A1016),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFF0F5),
                Color(0xFFFFD6E7),
                Color(0xFFF8C8DC),
            ),
        )
    }

    fun glassBody(isLight: Boolean): Color =
        if (isLight) Color(0xFFFFFFFF) else Color(0xFF21171C)

    fun onGlassText(isLight: Boolean): Color =
        if (isLight) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)

    fun onGlassMuted(isLight: Boolean): Color =
        if (isLight) Color(0xFF64748B) else Color(0xFFCBD5E1)
}

