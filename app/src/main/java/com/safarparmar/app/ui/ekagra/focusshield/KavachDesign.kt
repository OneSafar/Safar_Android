package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Stitch Kavach palette (project 15175496978778525381). Refactored to dynamically bind to Material 3 colors. */
object KavachDesign {
    val isDark: Boolean
        @Composable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f || isSystemInDarkTheme()

    val Primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val PrimaryDark: Color @Composable get() = MaterialTheme.colorScheme.primary
    val Background: Color @Composable get() = MaterialTheme.colorScheme.background
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val SurfaceHighlight: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val TextMain: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val SuccessBg: Color @Composable get() = if (isDark) Color(0xFF0F3E2B) else Color(0xFFD1E7DD)
    val SuccessText: Color @Composable get() = if (isDark) Color(0xFF75B798) else Color(0xFF0F5132)
    val SearchFieldBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val SearchHint: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val ActiveSessionStatus: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val Border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val CardWhite: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Hub screen (Kavach settings) — matches M3 design system */
    val HubHeroBackground: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val HubHeroIconBg: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val HubGreenIconBg: Color @Composable get() = if (isDark) Color(0xFF0F3E2B) else Color(0xFFD1E7DD)
    val HubGreenIcon: Color @Composable get() = if (isDark) Color(0xFF75B798) else Color(0xFF0F5132)
    val HubText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val HubTextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val HubBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val HubSectionLabel: Color @Composable get() = MaterialTheme.colorScheme.onSurface
}
