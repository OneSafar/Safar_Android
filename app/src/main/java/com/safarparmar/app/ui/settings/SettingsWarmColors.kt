package com.safarparmar.app.ui.settings

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme

internal object SettingsWarmColors {
    private val dark @Composable get() = LocalPlannerIsDarkTheme.current == true
    val Canvas @Composable get() = if (dark) BgDark else Color(0xFFFAF8FF)
    // Preserve component fills independently of the screen canvas.
    val ComponentBackground @Composable get() = if (dark) Color(0xFF211921) else Color(0xFFFAF8FF)
    val Paper @Composable get() = if (dark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val Ink @Composable get() = if (dark) Color(0xFFFFF0F6) else Color(0xFF241544)
    val Muted @Composable get() = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Accent @Composable get() = if (dark) RoyalPurpleDark else RoyalPurple
    val Peach @Composable get() = if (dark) RoyalPurpleSoftDark else RoyalPurpleSoft
    val Lavender @Composable get() = if (dark) RoyalPurpleSoftDark else RoyalPurpleSoft
}
