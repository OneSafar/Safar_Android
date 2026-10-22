package com.safarparmar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.ui.theme.colors
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.safarparmar.app.performance.LocalMotionPolicy

/** Circular companion to Composables UI's theme-aware linear progress indicator. */
@Composable
fun SafarCircularProgressIndicator(
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = Theme[colors][primaryColor],
    strokeWidth: Dp = 4.dp,
    trackColor: Color = Color.Transparent,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val motionEnabled = LocalMotionPolicy.current.animationsEnabled
    val rotation = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "circular-progress")
        val animatedRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "circular-progress-rotation",
        )
        animatedRotation
    } else 0f
    ProgressArc(
        modifier = modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        startAngle = if (motionEnabled) rotation - 90f else -90f,
        sweepAngle = if (motionEnabled) 270f else 180f,
    )
}

@Composable
fun SafarCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = Theme[colors][primaryColor],
    strokeWidth: Dp = 4.dp,
    trackColor: Color = Color.Transparent,
    strokeCap: StrokeCap = StrokeCap.Round,
    gapSize: Dp = 0.dp,
) {
    val fraction = progress().coerceIn(0f, 1f)
    ProgressArc(
        modifier = modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
        startAngle = -90f,
        sweepAngle = 360f * fraction,
        gapSize = gapSize,
    )
}

@Composable
private fun ProgressArc(
    modifier: Modifier,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    strokeCap: StrokeCap,
    startAngle: Float,
    sweepAngle: Float,
    gapSize: Dp = 0.dp,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = (minOf(size.width, size.height) - stroke).coerceAtLeast(0f)
        val arcTopLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val style = Stroke(width = stroke, cap = strokeCap)
        if (trackColor != Color.Transparent) {
            drawArc(trackColor, -90f, 360f, false, arcTopLeft, arcSize, style = style)
        }
        val gapDegrees = if (diameter > 0f) gapSize.toPx() / (diameter * 3.1415927f) * 360f else 0f
        val visibleSweep = (sweepAngle - gapDegrees).coerceAtLeast(0f)
        if (visibleSweep > 0f) {
            drawArc(color, startAngle + gapDegrees / 2f, visibleSweep, false, arcTopLeft, arcSize, style = style)
        }
    }
}
