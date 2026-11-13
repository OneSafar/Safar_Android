package com.safarparmar.app.ui.nishtha.analytics

import com.safarparmar.app.ui.theme.RoyalPurpleMid
import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.isLightBackground

internal fun analyticsPlum(light: Boolean) = if (light) RoyalPurple else RoyalPurpleDark
internal fun analyticsPeach(light: Boolean) = if (light) RoyalPurpleMid else RoyalPurpleSoft
internal fun analyticsLilac(light: Boolean) = if (light) RoyalPurpleMid else Color(0xFFD2BCE8)
internal fun analyticsPaper(light: Boolean) = if (light) Color(0xFFFFFDFC) else Color(0xFF342735)
internal fun analyticsCanvas(light: Boolean) = if (light) Color(0xFFFAF8FF) else Color(0xFF211921)
internal fun analyticsInk(light: Boolean) = if (light) Color(0xFF241544) else Color(0xFFFFF0F6)
internal fun analyticsMuted(light: Boolean) = if (light) Color(0xFF75616F) else Color(0xFFCFBECB)
internal fun analyticsSoft(light: Boolean) = if (light) RoyalPurpleSoft else RoyalPurpleSoftDark

@Composable
internal fun AnalyticsWarmTheme(content: @Composable () -> Unit) {
    val light = MaterialTheme.colorScheme.background.isLightBackground()
    val colors = MaterialTheme.colorScheme.copy(background = analyticsCanvas(light), surface = analyticsPaper(light),
        surfaceContainerHigh = analyticsPaper(light), onSurface = analyticsInk(light), onBackground = analyticsInk(light),
        onSurfaceVariant = analyticsMuted(light), primary = analyticsPlum(light),
        secondaryContainer = analyticsSoft(light), onSecondaryContainer = analyticsInk(light),
        outlineVariant = if (light) Color(0xFFEADFE6) else Color(0xFF654D66))
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalSafarComposablesAccent provides analyticsPlum(light)) {
            SafarComposablesTheme { content() }
        }
    }
}
