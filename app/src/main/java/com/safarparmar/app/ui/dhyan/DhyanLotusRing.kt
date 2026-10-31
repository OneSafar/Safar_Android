package com.safarparmar.app.ui.dhyan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safarparmar.app.performance.LocalMotionPolicy

/** Lotus-only breathing stage: animation changes the layer, never the layout size. */
@Composable
internal fun DhyanLotusRing(expanding: Boolean, running: Boolean, durationMillis: Int, description: String) {
    val motion = LocalMotionPolicy.current.animationsEnabled
    val scale = animateFloatAsState(if (!running || expanding) 1f else .86f,
        tween(if (motion && running) durationMillis.coerceAtLeast(1) else 0), label = "lotusBreath")
    Canvas(Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1f)
        .semantics { contentDescription = description }.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
        val radius = size.minDimension * .43f
        drawCircle(Brush.sweepGradient(listOf(Color(0xFFEDE0F4), Color(0xFFFFCBBE), Color(0xFFEDE0F4))),
            radius = radius, style = Stroke(width = size.minDimension * .035f))
        val unit = size.minDimension / 220f
        translate(center.x, center.y + 10f * unit) {
            fun petal(tipX: Float, tipY: Float, bendX: Float, color: Color) {
                val path = Path().apply {
                    moveTo(0f, 34f * unit)
                    cubicTo(bendX * unit, 25f * unit, tipX * unit, tipY * unit, tipX * unit, tipY * unit)
                    cubicTo(tipX * .2f * unit, tipY * .85f * unit, bendX * -.3f * unit, 18f * unit, 0f, 34f * unit)
                    close()
                }
                drawPath(path, color)
            }
            petal(-62f, -7f, -56f, Color(0xFFEDE0F4))
            petal(62f, -7f, 56f, Color(0xFFEDE0F4))
            petal(-35f, -40f, -48f, Color(0xFFFFCBBE))
            petal(35f, -40f, 48f, Color(0xFFFFCBBE))
            val middle = Path().apply {
                moveTo(0f, 32f * unit)
                cubicTo(-30f * unit, 5f * unit, -20f * unit, -30f * unit, 0f, -55f * unit)
                cubicTo(20f * unit, -30f * unit, 30f * unit, 5f * unit, 0f, 32f * unit)
                close()
            }
            drawPath(middle, Color(0xFFFFF5F0))
        }
    }
}
