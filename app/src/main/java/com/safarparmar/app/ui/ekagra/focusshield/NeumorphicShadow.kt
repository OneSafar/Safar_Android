package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

fun Modifier.neumorphicShadow(
    isDarkTheme: Boolean,
    cornerRadius: Float = 0f
) = this.drawBehind {
    val darkShadow = if (isDarkTheme) Color(0x33000000) else Color(0x1A000000)
    val lightShadow = if (isDarkTheme) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    
    // Bottom-right shadow
    drawRoundRect(
        color = darkShadow,
        topLeft = androidx.compose.ui.geometry.Offset(10f, 10f),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )
    // Top-left highlight
    drawRoundRect(
        color = lightShadow,
        topLeft = androidx.compose.ui.geometry.Offset(-10f, -10f),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )
}
