package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.theme.isLightBackground

/** Solid shapes below the measured menu; no bitmap, animation, layout reservation or touch target. */
@Composable
internal fun TutorialLessonMenu(feature: TitliFeature, content: @Composable ColumnScope.() -> Unit) {
    var contentHeight by remember { mutableIntStateOf(0) }
    val dark = !MaterialTheme.colorScheme.background.isLightBackground()
    val colors = tutorialArtPalette(dark)
    val ink = tutorialVisualColors(dark).action
    val arrangement = feature.menuArtwork()
    Box(Modifier.fillMaxSize().drawWithCache {
        val top = contentHeight.toFloat() + 20.dp.toPx()
        val room = (size.height - top).coerceAtLeast(0f)
        val w = size.width
        fun xy(x: Float, y: Float) = Offset(x * w, top + y * room)
        val archX = if (arrangement == TutorialMenuArtwork.LEAVES) 0.48f else 0.18f
        val tileX = if (arrangement == TutorialMenuArtwork.LEAVES) 0.24f else 0.60f
        val wave = Path().apply {
            moveTo(0f, top + room * if (arrangement == TutorialMenuArtwork.WAVES) 0.24f else 0.38f)
            cubicTo(w * 0.50f, top + room * 0.38f, w * 0.44f, top + room * 0.92f, w, top + room * 0.97f)
            lineTo(w, size.height); lineTo(0f, size.height); close()
        }
        fun leaf(x: Float, y: Float, right: Boolean) = Path().apply {
            val origin = xy(x, y)
            val tip = xy(x + if (right) 0.22f else -0.18f, y - 0.38f)
            moveTo(origin.x, origin.y)
            cubicTo(origin.x - w * 0.05f, tip.y + room * 0.04f, tip.x, tip.y, tip.x, tip.y)
            cubicTo(tip.x + w * 0.08f, origin.y - room * 0.10f, origin.x + w * 0.08f, origin.y, origin.x, origin.y)
            close()
        }
        val leafA = leaf(0.80f, 0.82f, true)
        val leafB = leaf(0.80f, 0.91f, false)
        val sparkle = Path().apply {
            val c = xy(0.69f, 0.23f); val r = w * 0.045f
            moveTo(c.x, c.y - r); quadraticTo(c.x + r * 0.15f, c.y - r * 0.15f, c.x + r, c.y)
            quadraticTo(c.x + r * 0.15f, c.y + r * 0.15f, c.x, c.y + r)
            quadraticTo(c.x - r * 0.15f, c.y + r * 0.15f, c.x - r, c.y)
            quadraticTo(c.x - r * 0.15f, c.y - r * 0.15f, c.x, c.y - r); close()
        }
        onDrawBehind {
            if (room > 80.dp.toPx()) clipRect(top = top) {
                drawRoundRect(colors.yellow, xy(archX, 0.10f), Size(w * 0.37f, room * 1.1f), CornerRadius(w * 0.185f))
                withTransform({ rotate(-24f, xy(tileX + 0.1f, 0.66f)) }) {
                    drawRoundRect(colors.lilac, xy(tileX - 0.10f, 0.40f), Size(w * 0.44f, room * 0.80f), CornerRadius(w * 0.09f))
                }
                if (arrangement == TutorialMenuArtwork.STEPS) {
                    drawRoundRect(colors.sage, xy(0.04f, 0.65f), Size(w * 0.22f, room * 0.5f), CornerRadius(12.dp.toPx()))
                    drawRoundRect(colors.sage, xy(0.20f, 0.49f), Size(w * 0.22f, room * 0.65f), CornerRadius(12.dp.toPx()))
                }
                drawPath(leafA, colors.sage); drawPath(leafB, colors.sage)
                drawPath(wave, colors.coral)
                drawCircle(colors.pink, w * 0.065f, xy(0.88f, 0.17f))
                drawPath(sparkle, ink)
            }
        }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().onSizeChanged { contentHeight = it.height }.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
