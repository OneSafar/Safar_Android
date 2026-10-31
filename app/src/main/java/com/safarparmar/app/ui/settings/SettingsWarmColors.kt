package com.safarparmar.app.ui.settings

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme

internal object SettingsWarmColors {
    private val dark @Composable get() = LocalPlannerIsDarkTheme.current == true
    val Canvas @Composable get() = if (dark) BgDark else Color(0xFFFFF5F0)
    // Preserve component fills independently of the screen canvas.
    val ComponentBackground @Composable get() = if (dark) Color(0xFF211921) else Color(0xFFFFF5F0)
    val Paper @Composable get() = if (dark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val Ink @Composable get() = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val Muted @Composable get() = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Accent @Composable get() = if (dark) Color(0xFFF0BEDC) else Color(0xFF542347)
    val Peach @Composable get() = if (dark) Color(0xFF684047) else Color(0xFFFFCBBE)
    val Lavender @Composable get() = if (dark) Color(0xFF513551) else Color(0xFFEDE0F4)
}
