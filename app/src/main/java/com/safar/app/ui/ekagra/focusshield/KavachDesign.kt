package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Stitch Kavach palette (project 15175496978778525381). */
object KavachDesign {
    val isDark: Boolean
        @Composable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()

    val Primary: Color @Composable get() = if (isDark) Color(0xFF3B82F6) else Color(0xFF0A56D9)
    val PrimaryDark: Color @Composable get() = if (isDark) Color(0xFF2563EB) else Color(0xFF084BBD)
    val Background: Color @Composable get() = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val Surface: Color @Composable get() = if (isDark) Color(0xFF1E293B) else Color(0xFFF0F4F9)
    val SurfaceHighlight: Color @Composable get() = if (isDark) Color(0xFF334155) else Color(0xFFE3EAF5)
    val TextMain: Color @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1C1B1F)
    val TextMuted: Color @Composable get() = if (isDark) Color(0xFF9CA3AF) else Color(0xFF44474E)
    val SuccessBg: Color @Composable get() = if (isDark) Color(0xFF064E3B) else Color(0xFFE8F5E9)
    val SuccessText: Color @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF146C2E)
    val SearchFieldBg: Color @Composable get() = if (isDark) Color(0xFF1E293B) else Color(0xFFF0F2F5)
    val SearchHint: Color @Composable get() = if (isDark) Color(0xFF94A3B8) else Color(0xFF60708A)
    val ActiveSessionStatus: Color @Composable get() = if (isDark) Color(0xFF1E293B) else Color(0xFFE3EAF5)
    val Border: Color @Composable get() = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val CardWhite: Color @Composable get() = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)

    /** Hub screen (Kavach settings) — matches Stitch/HTML mock */
    val HubHeroBackground: Color @Composable get() = if (isDark) Color(0xFF1E293B) else Color(0xFFF0F4FA)
    val HubHeroIconBg: Color @Composable get() = if (isDark) Color(0xFF1E40AF) else Color(0xFFDBEAFE)
    val HubGreenIconBg: Color @Composable get() = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    val HubGreenIcon: Color @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
    val HubText: Color @Composable get() = if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
    val HubTextMuted: Color @Composable get() = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val HubBorder: Color @Composable get() = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
    val HubSectionLabel: Color @Composable get() = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
}
