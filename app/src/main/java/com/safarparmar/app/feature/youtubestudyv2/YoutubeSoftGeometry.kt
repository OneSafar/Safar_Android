package com.safarparmar.app.feature.youtubestudyv2

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleSoft

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

/** Decorative, native artwork; it carries no controls or accessibility text. */
@Composable
internal fun YoutubeSoftGeometry(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 360f, size.height / 150f, pivot = Offset.Zero) {
            drawRoundRect(RoyalPurpleSoft, Offset(225f, 0f), Size(106f, 190f), CornerRadius(53f))
            drawRoundRect(Color(0xFFED8A7D), Offset(250f, 39f), Size(57f, 150f), CornerRadius(29f))
            drawCircle(Color(0xFFED8A7D), 15f, Offset(151f, 89f))
            rotate(14f, Offset(232f, 108f)) {
                drawRoundRect(Color(0xFFCDBBE9), Offset(182f, 56f), Size(100f, 125f), CornerRadius(18f))
                drawRoundRect(RoyalPurple, Offset(205f, 83f), Size(58f, 43f), CornerRadius(12f))
                drawPath(Path().apply { moveTo(228f, 94f); lineTo(228f, 115f); lineTo(244f, 104f); close() }, Color(0xFFCDBBE9))
            }
            drawPath(Path().apply { moveTo(325f,150f); cubicTo(281f,130f,279f,102f,282f,88f); cubicTo(320f,99f,327f,128f,325f,150f); close() }, Color(0xFF8EAD7C))
            drawPath(Path().apply { moveTo(325f,150f); cubicTo(320f,102f,340f,77f,357f,68f); cubicTo(359f,112f,347f,139f,325f,150f); close() }, Color(0xFF8EAD7C))
            drawPath(Path().apply { moveTo(0f,137f); cubicTo(77f,83f,100f,95f,202f,150f); lineTo(0f,150f); close() }, Color(0xFFED8A7D))
            drawPath(Path().apply { moveTo(47f,150f); cubicTo(138f,110f,160f,119f,282f,150f); close() }, RoyalPurple)
        }
    }
}
