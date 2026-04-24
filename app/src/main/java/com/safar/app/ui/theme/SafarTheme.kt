package com.safar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColorScheme = lightColorScheme(
    primary              = PrimaryLight,            // #2E3F9E indigo
    onPrimary            = SafarOnPrimaryLight,     // #FFFFFF
    primaryContainer     = Indigo100,
    onPrimaryContainer   = BrandMidnight,           // #1B212D
    secondary            = SafarSecondary,          // #7B879D
    onSecondary          = SafarOnPrimaryLight,     // #FFFFFF
    secondaryContainer   = SafarSurfaceVariantLight,// #F2EFE9 muted
    onSecondaryContainer = BrandMidnight,           // #1B212D
    tertiary             = Teal500,
    onTertiary           = SafarOnPrimaryLight,     // #FFFFFF
    background           = BgLight,                 // #F8F6F2
    onBackground         = SafarOnBackgroundLight,  // #1B212D
    surface              = SafarSurfaceLight,       // #FDFDFB
    onSurface            = SafarOnSurfaceLight,     // #1B212D
    surfaceVariant       = SafarSurfaceVariantLight,// #F2EFE9
    onSurfaceVariant     = FieldHintLight,          // #525C6F
    outline              = DividerLight,            // #D7D3CC
    error                = SafarError,              // #E11D48
)

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryDark,             // #1FE0BA teal
    onPrimary            = SafarOnPrimaryDark,      // #0F1115
    primaryContainer     = SafarSurfaceVariantDark, // #272C35 muted dark
    onPrimaryContainer   = BrandTeal,               // #1FE0BA
    secondary            = SecondaryDark,           // #C1155D rose
    onSecondary          = SafarOnPrimaryLight,     // #FFFFFF
    secondaryContainer   = SafarSurfaceVariantDark, // #272C35
    onSecondaryContainer = BrandTeal,               // #1FE0BA
    tertiary             = Teal500,
    onTertiary           = SafarOnPrimaryDark,      // #0F1115
    background           = BgDark,                  // #0F1115
    onBackground         = SafarOnBackgroundDark,   // #E7EBEF
    surface              = SafarSurfaceDark,        // #181B20
    onSurface            = SafarOnSurfaceDark,      // #E7EBEF
    surfaceVariant       = SafarSurfaceVariantDark, // #272C35
    onSurfaceVariant     = FieldHint,               // #8996A9
    outline              = DividerDark,             // #272C35
    error                = SafarError,              // #E11D48
)

@Composable
fun SafarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    nightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color     = colorScheme.background,
            darkIcons = !darkTheme && !nightMode
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SafarTypography,
        content     = content
    )
}
