package com.safar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColorScheme = lightColorScheme(
    primary = SafarPrimary,
    onPrimary = SafarOnPrimary,
    primaryContainer = SafarSurfaceVariant,
    secondary = SafarSecondary,
    tertiary = SafarTertiary,
    background = SafarBackground,
    surface = SafarSurface,
    surfaceVariant = SafarSurfaceVariant,
    onBackground = SafarOnBackground,
    onSurface = SafarOnSurface,
    error = SafarSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = SafarPrimary,
    onPrimary = SafarOnPrimary,
    primaryContainer = SafarDarkSurfaceVariant,
    secondary = SafarSecondary,
    tertiary = SafarTertiary,
    background = SafarDarkBackground,
    surface = SafarDarkSurface,
    surfaceVariant = SafarDarkSurfaceVariant,
    onBackground = SafarDarkOnBackground,
    onSurface = SafarDarkOnSurface,
    error = SafarSecondary
)

private val NightColorScheme = darkColorScheme(
    primary = Color(0xFF9B8FFF),
    onPrimary = NightModeBackground,
    primaryContainer = NightModeSurface,
    secondary = Color(0xFFFF8FAD),
    tertiary = Color(0xFF5BCCB0),
    background = NightModeBackground,
    surface = NightModeSurface,
    surfaceVariant = Color(0xFF130F25),
    onBackground = NightModeText,
    onSurface = NightModeText,
    error = Color(0xFFFF6B6B)
)

@Composable
fun SafarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    nightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        nightMode -> NightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colorScheme.background,
            darkIcons = !darkTheme && !nightMode
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafarTypography,
        content = content
    )
}
