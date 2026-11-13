package com.safarparmar.app.ui.ekagra.focusshield

import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.SafarComposablesTheme

/** Shared across settings, permission windows, picker and analytics; enforcement is unaffected. */
@Composable
fun KavachCommandTheme(content: @Composable () -> Unit) {
    val dark = KavachDesign.isDark
    val plum = RoyalPurple
    val canvas = if (dark) BgDark else Color(0xFFFFF5F0)
    val paper = if (dark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val ink = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val muted = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val peach = if (dark) RoyalPurpleSoftDark else RoyalPurpleSoft
    val line = if (dark) Color(0xFF654D66) else Color(0xFFE8DEE5)
    val scheme = MaterialTheme.colorScheme.copy(primary = if (dark) RoyalPurpleDark else plum,
        onPrimary = if (dark) plum else Color.White, primaryContainer = peach, onPrimaryContainer = ink,
        secondary = if (dark) RoyalPurpleDark else plum, secondaryContainer = peach, onSecondaryContainer = ink,
        background = canvas, onBackground = ink, surface = paper, onSurface = ink,
        surfaceVariant = peach, onSurfaceVariant = muted, outline = line, outlineVariant = line,
        surfaceContainer = paper, surfaceContainerLow = paper, surfaceContainerHigh = peach,
        surfaceContainerHighest = peach, surfaceTint = Color.Transparent)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.copy(
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = PoppinsFontFamily),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = PoppinsFontFamily))) {
        CompositionLocalProvider(LocalSafarComposablesAccent provides scheme.primary) {
            SafarComposablesTheme { content() }
        }
    }
}
