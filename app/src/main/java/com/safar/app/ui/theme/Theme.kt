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
    primary            = PrimaryLight,
    onPrimary          = SafarOnPrimaryLight,
    primaryContainer   = BrandMint,
    onPrimaryContainer = BrandMidnight,
    secondary          = SafarSecondary,
    onSecondary        = BrandMidnight,
    tertiary           = BrandTeal,
    onTertiary         = BrandMidnight,
    background         = BgLight,
    onBackground       = SafarOnBackgroundLight,
    surface            = SafarSurfaceLight,
    onSurface          = SafarOnSurfaceLight,
    surfaceVariant     = SafarSurfaceVariantLight,
    onSurfaceVariant   = SafarOnSurfaceLight,
    error              = SafarError
)

private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryDark,
    onPrimary          = SafarOnPrimaryDark,
    primaryContainer   = BrandPlumDark,
    onPrimaryContainer = BrandTeal,
    secondary          = BrandTeal,
    onSecondary        = BrandMidnight,
    tertiary           = BrandMint,
    onTertiary         = BrandMidnight,
    background         = BgDark,
    onBackground       = SafarOnBackgroundDark,
    surface            = SafarSurfaceDark,
    onSurface          = SafarOnSurfaceDark,
    surfaceVariant     = SafarSurfaceVariantDark,
    onSurfaceVariant   = BrandTeal,
    error              = SafarError
)

private val NightColorScheme = darkColorScheme(
    primary            = BrandTeal,
    onPrimary          = BrandMidnight,
    primaryContainer   = BrandPurpleDeep,
    onPrimaryContainer = BrandMint,
    secondary          = PrimaryDark,
    onSecondary        = BrandMidnight,
    tertiary           = BrandMint,
    onTertiary         = BrandMidnight,
    background         = NightModeBackground,
    onBackground       = NightModeText,
    surface            = NightModeSurface,
    onSurface          = NightModeText,
    surfaceVariant     = BrandPlumDark,
    onSurfaceVariant   = BrandTeal,
    error              = SafarError
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
        else      -> LightColorScheme
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
        typography  = SafarTypography,
        content     = content
    )
}

