package com.safarparmar.app.ui.mehfil

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

internal enum class MehfilCardGlyph { Heart, Comment, Connect, Bookmark, Leaf, More, Lock }

// Native vector artwork, shared by phone and tablet cards. Coordinates use a 32 × 32 viewport.
private val heart = Path().apply {
    moveTo(16f, 29f)
    cubicTo(13.5f, 26.6f, 4.1f, 20.1f, 2.4f, 14.1f)
    cubicTo(0.3f, 6.9f, 5.9f, 2.6f, 11.2f, 4.1f)
    cubicTo(13.3f, 4.7f, 15f, 6.2f, 16f, 8f)
    cubicTo(17.3f, 5.6f, 19.8f, 3.7f, 23f, 3.8f)
    cubicTo(29.8f, 3.9f, 32f, 10.2f, 29.2f, 16.1f)
    cubicTo(26.8f, 21.4f, 19.2f, 27f, 16f, 29f)
    close()
}
private val comment = Path().apply {
    moveTo(15.8f, 4.2f)
    cubicTo(7.4f, 4.2f, 3.3f, 7.5f, 3.3f, 14.5f)
    cubicTo(3.3f, 18f, 4.5f, 20.4f, 7.1f, 22.1f)
    lineTo(5.1f, 28f)
    lineTo(12.6f, 24.1f)
    cubicTo(14f, 24.4f, 15.5f, 24.5f, 17.2f, 24.4f)
    cubicTo(25.1f, 24.2f, 28.7f, 20.8f, 28.7f, 14.1f)
    cubicTo(28.7f, 7.4f, 24.5f, 4.2f, 15.8f, 4.2f)
    close()
}
private val bookmark = Path().apply {
    moveTo(8.5f, 4.2f)
    quadraticTo(8.5f, 3f, 9.7f, 3f)
    lineTo(23f, 3f)
    quadraticTo(24.2f, 3f, 24.2f, 4.2f)
    lineTo(24.2f, 28.8f)
    lineTo(16.4f, 23.2f)
    lineTo(8.5f, 28.8f)
    close()
}
private val leaf = Path().apply {
    moveTo(6.4f, 24f)
    cubicTo(3.4f, 14.5f, 9.3f, 7.2f, 18.7f, 6.2f)
    cubicTo(23f, 5.7f, 26.8f, 5f, 29.3f, 3f)
    cubicTo(27.5f, 6.8f, 27.6f, 13.7f, 24.1f, 19.5f)
    cubicTo(20.9f, 24.4f, 15.6f, 26.5f, 10.5f, 24.2f)
    lineTo(6.4f, 24f)
    close()
}
private val connectionBody = Path().apply {
    moveTo(2.5f, 28f)
    lineTo(2.5f, 24.5f)
    cubicTo(2.5f, 19f, 20.5f, 19f, 20.5f, 24.5f)
    lineTo(20.5f, 28f)
    close()
}
private val heartGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF638C), Color(0xFFFF386B), Color(0xFFEF245B)),
    start = Offset(5f, 4f), end = Offset(26f, 29f),
)

/** Filled/outline states use the same silhouette; only their paint changes on reaction or save. */
@Composable
internal fun MehfilCardArtwork(
    glyph: MehfilCardGlyph,
    modifier: Modifier = Modifier,
    color: Color = MehfilFlatColors.Muted,
    filled: Boolean = false,
    description: String? = null,
) {
    if (glyph == MehfilCardGlyph.Lock) {
        Icon(Icons.Default.Lock, description, modifier, tint = color)
        return
    }
    val vein = MehfilFlatColors.Lavender
    Canvas(modifier.semantics { if (description != null) contentDescription = description }) {
        scale(size.width / 32f, size.height / 32f, pivot = Offset.Zero) {
            val outline = Stroke(width = 2.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (glyph) {
                MehfilCardGlyph.Heart -> if (filled) drawPath(heart, heartGradient)
                    else drawPath(heart, color, style = outline)
                MehfilCardGlyph.Comment -> drawPath(comment, color, style = outline)
                MehfilCardGlyph.Bookmark -> if (filled) drawPath(bookmark, color)
                    else drawPath(bookmark, color, style = outline)
                MehfilCardGlyph.More -> listOf(6f, 16f, 26f).forEach {
                    drawCircle(color, radius = 1.8f, center = Offset(it, 16f))
                }
                MehfilCardGlyph.Leaf -> {
                    drawPath(leaf, color)
                    drawLine(vein, Offset(8f, 25f), Offset(22f, 12f), strokeWidth = 1.2f, cap = StrokeCap.Round)
                    drawLine(color, Offset(4f, 28f), Offset(11f, 20f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                }
                MehfilCardGlyph.Connect -> {
                    drawCircle(color, radius = 5.1f, center = Offset(11.5f, 10.3f))
                    drawPath(connectionBody, color)
                    drawCircle(color, radius = 5f, center = Offset(26f, 24f))
                    drawCircle(color.copy(alpha = 0.65f), radius = 3.5f, center = Offset(24.4f, 12.4f))
                }
                MehfilCardGlyph.Lock -> Unit
            }
        }
    }
}
