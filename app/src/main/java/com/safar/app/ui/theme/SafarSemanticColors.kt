package com.safar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for profile and feature screens — prefers MaterialTheme, with light-mode fallbacks.
 */
object SafarSemanticColors {

    @Composable
    fun profileBackground(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background

    @Composable
    fun profilePrimaryContainer(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    @Composable
    fun profilePrimaryFixed(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer

    @Composable
    fun profileOnBackground(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground

    @Composable
    fun profileOnSurfaceVariant(isDarkTheme: Boolean): Color =
        MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun profileSurfaceVariant(isDarkTheme: Boolean): Color =
        MaterialTheme.colorScheme.surfaceVariant

    @Composable
    fun profileSuccess(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.primary else Emerald600

    @Composable
    fun profileSuccessBackground(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Emerald100

    @Composable
    fun profileSuccessLabel(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.onPrimaryContainer else Emerald600
}
