package com.safarparmar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

val ExpressiveCardShape = RoundedCornerShape(32.dp)
val ExpressiveButtonShape = RoundedCornerShape(24.dp)

// Expressive Gradients
val LightExpressiveGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE0E7FF), // Indigo100 / PrimaryContainer light
        Color(0xFFFDFDFB)  // SafarSurfaceLight
    )
)

val DarkExpressiveGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF272C35), // SafarSurfaceVariantDark / PrimaryContainer dark
        Color(0xFF181B20)  // SafarSurfaceDark
    )
)

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

/** Dim warm light scheme for low-light reading without full dark theme. */
private val NightColorScheme = lightColorScheme(
    primary              = Teal500,
    onPrimary            = SafarOnPrimaryLight,
    primaryContainer     = Color(0xFF1A2E28),
    onPrimaryContainer   = BrandTeal,
    secondary            = SafarSecondary,
    onSecondary          = SafarOnPrimaryLight,
    secondaryContainer   = Color(0xFF2A2520),
    onSecondaryContainer = BrandMint,
    tertiary             = Teal400,
    onTertiary           = NightModeBackground,
    background           = NightModeBackground,
    onBackground         = BrandMint,
    surface              = NightModeSurface,
    onSurface            = BrandMint,
    surfaceVariant       = Color(0xFF22262E),
    onSurfaceVariant     = FieldHint,
    outline              = DividerDark,
    error                = SafarError,
)

@Composable
fun SafarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    nightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        nightMode -> NightColorScheme
        else      -> LightColorScheme
    }

    val currentDensity = LocalDensity.current
    val displayMetrics = androidx.compose.ui.platform.LocalContext.current.resources.displayMetrics
    val screenHeightPx = displayMetrics.heightPixels.toFloat()
    val screenWidthPx = displayMetrics.widthPixels.toFloat()
    val targetLogicalHeightDp = 820f
    val customDensityValue = screenHeightPx / targetLogicalHeightDp
    val maxAllowedDensity = screenWidthPx / 360f
    val systemDensity = currentDensity.density
    val densityLimit = systemDensity.coerceAtMost(maxAllowedDensity)
    val finalDensityValue = customDensityValue.coerceAtMost(densityLimit) * 0.85f
    
    val customDensity = remember(finalDensityValue, currentDensity.fontScale) {
        Density(
            density = finalDensityValue,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color     = colorScheme.background,
            darkIcons = !darkTheme && !nightMode
        )
    }

    CompositionLocalProvider(LocalDensity provides customDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SafarTypography,
        ) {
            ProvideTextStyle(
                value = SafarTypography.bodyMedium,
                content = content
            )
        }
    }
}
