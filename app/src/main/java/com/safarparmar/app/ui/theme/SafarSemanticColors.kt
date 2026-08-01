package com.safarparmar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Semantic colors for profile and feature screens — prefers MaterialTheme, with light-mode fallbacks.
 */
object SafarSemanticColors {

    /** Rich purple for primary buttons — matches launch questionnaire Continue (#6D28D9 / #C084FC). */
    private val BrandPurpleLight = Color(0xFF6D28D9)
    private val BrandPurpleDark = Color(0xFFC084FC)

    @Composable
    fun profileBackground(isDarkTheme: Boolean): Color =
        if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background

    /** Warm off-white canvas for the Study Planner feature only — not the app-wide theme. */
    @Composable
    fun plannerBackground(): Color =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
            MaterialTheme.colorScheme.background
        } else {
            PlannerWarmBackground
        }

    /**
     * Brand purple for primary action buttons and purple CTA fills.
     * Light: #6D28D9 · Dark: #C084FC — same as launch questionnaire Continue.
     */
    @Composable
    fun brandPurple(isDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f): Color =
        if (isDarkTheme) BrandPurpleDark else BrandPurpleLight

    @Composable
    fun brandOnPurple(): Color = Color.White

    @Composable
    fun profilePrimaryContainer(isDarkTheme: Boolean): Color =
        brandPurple(isDarkTheme)

    @Composable
    fun profilePrimaryFixed(isDarkTheme: Boolean): Color =
        brandPurple(isDarkTheme).copy(alpha = if (isDarkTheme) 0.22f else 0.12f)

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
