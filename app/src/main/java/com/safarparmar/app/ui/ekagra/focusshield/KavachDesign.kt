package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.safarparmar.app.ui.theme.SafarSemanticColors

/**
 * Kavach palette. Accent is locked to Safar purple (same violet used by glass /
 * Nishtha / Plan chrome) so Kavach doesn't drift to Material grey primary and
 * feel isolated from the rest of the app.
 */
object KavachDesign {
    val isDark: Boolean
        @Composable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    /** Safar purple for primary buttons — rich fill, macOS glass chrome unchanged elsewhere. */
    val Primary: Color
        @Composable get() = com.safarparmar.app.ui.theme.SafarSemanticColors.brandPurple(isDark)

    val PrimaryDark: Color @Composable get() = Primary
    val Background: Color @Composable get() = MaterialTheme.colorScheme.background
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val SurfaceHighlight: Color @Composable get() = Primary.copy(alpha = 0.12f)
    val TextMain: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val SuccessBg: Color @Composable get() = if (isDark) Color(0xFF0F3E2B) else Color(0xFFD1E7DD)
    val SuccessText: Color @Composable get() = if (isDark) Color(0xFF75B798) else Color(0xFF0F5132)
    val SearchFieldBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val SearchHint: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val ActiveSessionStatus: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val Border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val CardWhite: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Hub screen (Kavach settings) */
    val HubHeroBackground: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val HubHeroIconBg: Color @Composable get() = Primary.copy(alpha = 0.12f)
    val HubGreenIconBg: Color @Composable get() = if (isDark) Color(0xFF0F3E2B) else Color(0xFFD1E7DD)
    val HubGreenIcon: Color @Composable get() = if (isDark) Color(0xFF75B798) else Color(0xFF0F5132)
    val HubText: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val HubTextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val HubBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val HubSectionLabel: Color @Composable get() = MaterialTheme.colorScheme.onSurface
}
