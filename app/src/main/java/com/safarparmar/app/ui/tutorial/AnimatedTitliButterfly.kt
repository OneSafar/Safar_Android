// Adapted from the butterfly composable supplied by the user. Vector geometry is preserved.
package com.safarparmar.app.ui.tutorial

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Color tokens matching the original butterfly illustration.
 */
internal object ButterflyColors {
    val InkBlack = Color(0xFF141819)
    val AccentPink = Color(0xFFF86594)
}

/**
 * Vector path definitions for the Butterfly Only (Local coordinate space: 300x280 centered at 0,0).
 * These paths can also be pasted directly into `res/drawable/ic_butterfly.xml` `<path android:pathData="..."/>`.
 */
internal object ButterflyVectorPaths {
    // Antennae
    const val ANTENNA_LEFT = "M -3 -26 Q -11 -68 -21 -108"
    const val ANTENNA_RIGHT = "M 5 -25 Q 15 -66 29 -104"

    // Left Wing Pair (Outer Black Silhouette & Inner Pink Patch)
    const val WING_LEFT_OUTER =
        "M -4 -18 C -16 -58, -48 -128, -94 -144 C -118 -152, -135 -132, -136 -104 " +
        "C -137 -68, -124 -25, -96 -2 C -122 8, -132 38, -118 68 C -104 96, -68 98, -40 76 " +
        "C -22 62, -10 44, -4 32 Z"

    const val WING_LEFT_INNER_PINK =
        "M -6 -10 C -14 -36, -34 -72, -58 -76 C -76 -79, -86 -62, -79 -40 " +
        "C -74 -22, -56 -8, -40 -1 C -60 5, -76 22, -70 42 C -64 60, -42 62, -24 46 " +
        "C -14 37, -7 26, -5 18 Z"

    // Right Wing Pair (Outer Black Silhouette & Inner Pink Patch)
    const val WING_RIGHT_OUTER =
        "M 4 -18 C 16 -58, 48 -126, 95 -140 C 119 -147, 136 -127, 136 -99 " +
        "C 136 -64, 122 -22, 95 -1 C 120 10, 129 40, 114 70 C 100 97, 65 98, 38 76 " +
        "C 21 62, 10 44, 4 32 Z"

    const val WING_RIGHT_INNER_PINK =
        "M 6 -10 C 14 -36, 35 -70, 59 -74 C 77 -77, 86 -60, 79 -38 " +
        "C 73 -20, 56 -7, 40 0 C 60 6, 75 23, 69 43 C 63 61, 41 62, 23 46 " +
        "C 14 37, 7 26, 5 18 Z"

    // Butterfly Body (Thorax & Abdomen)
    const val BODY_CENTER =
        "M 0 -25 C 6 -25, 10 -12, 9 14 C 8 38, 5 64, 0 74 " +
        "C -5 64, -8 38, -9 14 C -10 -12, -6 -25, 0 -25 Z"
}

/**
 * Pre-parsed Compose [Path] cache so paths are allocated only once per composition.
 */
private class ButterflyPathCache {
    val antennaLeft: Path = PathParser().parsePathString(ButterflyVectorPaths.ANTENNA_LEFT).toPath()
    val antennaRight: Path = PathParser().parsePathString(ButterflyVectorPaths.ANTENNA_RIGHT).toPath()
    val wingLeftOuter: Path = PathParser().parsePathString(ButterflyVectorPaths.WING_LEFT_OUTER).toPath()
    val wingLeftInner: Path = PathParser().parsePathString(ButterflyVectorPaths.WING_LEFT_INNER_PINK).toPath()
    val wingRightOuter: Path = PathParser().parsePathString(ButterflyVectorPaths.WING_RIGHT_OUTER).toPath()
    val wingRightInner: Path = PathParser().parsePathString(ButterflyVectorPaths.WING_RIGHT_INNER_PINK).toPath()
    val body: Path = PathParser().parsePathString(ButterflyVectorPaths.BODY_CENTER).toPath()

}

/**
 * Standalone Animated Butterfly Composable.
 *
 * Features:
 * - Smooth 60/120fps 3D wing flapping (horizontal scale + vertical perspective skew)
 * - Secondary elastic antenna sway with phase lag
 * - Natural hovering body lift synchronized with the wing downstroke
 * - App-controlled reduced motion; input stays with the parent tutorial action
 *
 * @param modifier Compose modifier for positioning/sizing.
 * @param size Default bounding size of the butterfly.
 * @param baseAngleDegrees Initial tilt angle (28° matches the reference illustration).
 * @param flapSpeedMultiplier Speed multiplier for wing beats (1.0f = normal).
 * @param enableHoverBob Whether the butterfly gently bobs in place while flapping.
 * @param animate Whether to animate; toolbar icons stay still and portraits flap gently.
 * @param wingColor Primary silhouette & body color (defaults to InkBlack `#141819`).
 * @param accentColor Inner wing & spot color (defaults to AccentPink `#F86594`).
 */
@Composable
internal fun AnimatedTitliButterfly(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    baseAngleDegrees: Float = 28f,
    flapSpeedMultiplier: Float = 1.5f,
    enableHoverBob: Boolean = true,
    animate: Boolean = true,
    wingColor: Color = ButterflyColors.InkBlack,
    accentColor: Color = ButterflyColors.AccentPink
) {
    val pathCache = remember { ButterflyPathCache() }
    val motionEnabled = animate && com.safarparmar.app.performance.LocalMotionPolicy.current.animationsEnabled
    val phase = if (motionEnabled) {
        androidx.compose.animation.core.rememberInfiniteTransition(label = "titliWings").animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                tween(durationMillis = (1800f / flapSpeedMultiplier.coerceAtLeast(0.1f)).toInt(),
                    easing = androidx.compose.animation.core.LinearEasing)
            ), label = "wingPhase"
        )
    } else remember { mutableFloatStateOf((Math.PI / 2).toFloat()) }

    Canvas(modifier = modifier.size(size)) {
        // Padded viewport contains tilted wings and their full animation envelope.
        val viewportSize = 360f
        val scaleFactor = minOf(this.size.width, this.size.height) / viewportSize

        // 1. Wing flap kinematics
        val wingPhase = phase.value
        val elapsedSeconds = if (motionEnabled) wingPhase else 0f
        // A quicker downstroke and wider fold feel less like a uniform scale pulse.
        val flapWave = sin(wingPhase + 0.18f * sin(wingPhase))
        val wingScaleX = 0.58f + 0.44f * flapWave
        // Skew factor approximates tan(skewDegrees): (1 - wingScaleX) * -0.25f
        val wingSkewTanY = (1f - wingScaleX) * -0.25f
        val bodyLift = -flapWave * 6.5f

        // 2. Secondary antenna wobble (lags behind wing beat)
        val antWaveDegrees = sin(wingPhase - 0.8f) * 4f

        // 3. Hovering bob & subtle angle oscillation
        val hoverBobX = if (enableHoverBob && motionEnabled) cos(elapsedSeconds) * 4.5f + bodyLift * 0.45f else 0f
        val hoverBobY = if (enableHoverBob && motionEnabled) sin(elapsedSeconds) * 5.5f + bodyLift * 0.88f else 0f
        val dynamicAngle = if (enableHoverBob && motionEnabled) {
            baseAngleDegrees + sin(elapsedSeconds) * 3.5f
        } else {
            baseAngleDegrees
        }

        // Transform to the butterfly center and apply scale + rotation
        withTransform({
            translate(
                left = center.x + hoverBobX * scaleFactor,
                top = center.y + (hoverBobY + 20f) * scaleFactor
            )
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            rotate(degrees = dynamicAngle, pivot = Offset.Zero)
        }) {
            // Small, slow glints; static when motion is disabled, with no flashing or blur.
            val glint = if (motionEnabled) 0.65f + 0.15f * sin(wingPhase - 0.6f) else 0.65f
            drawTitliSpark(Offset(-144f, -48f), 7f, accentColor, glint)
            drawTitliSpark(Offset(138f, -66f), 9f, accentColor, 1.3f - glint)

            // --- A. ANTENNAE ---
            withTransform({
                rotate(degrees = -antWaveDegrees, pivot = Offset(-3f, -26f))
            }) {
                drawPath(
                    path = pathCache.antennaLeft,
                    color = wingColor,
                    style = Stroke(width = 6.8f, cap = StrokeCap.Round)
                )
            }

            withTransform({
                rotate(degrees = antWaveDegrees, pivot = Offset(5f, -25f))
            }) {
                drawPath(
                    path = pathCache.antennaRight,
                    color = wingColor,
                    style = Stroke(width = 6.8f, cap = StrokeCap.Round)
                )
            }

            // --- B. LEFT WING PAIR ---
            drawWingWithSkew(
                scaleX = wingScaleX,
                skewY = -wingSkewTanY
            ) {
                // Outer black wing
                drawPath(path = pathCache.wingLeftOuter, color = accentColor.copy(alpha = 0.22f), style = Stroke(5f))
                drawPath(path = pathCache.wingLeftOuter, color = wingColor)
                // Inner pink patch
                drawPath(path = pathCache.wingLeftInner, color = accentColor)

                // Forewing Upper Pink Spot: cx=-98, cy=-104, rx=6.8, ry=13.5, rot=-24
                drawRotatedOvalSpot(
                    cx = -98f, cy = -104f, rx = 6.8f, ry = 13.5f,
                    rotationDegrees = -24f, color = accentColor
                )
                // Forewing Second Pink Spot: cx=-94, cy=-77, rx=4.5, ry=8.8, rot=-28
                drawRotatedOvalSpot(
                    cx = -94f, cy = -77f, rx = 4.5f, ry = 8.8f,
                    rotationDegrees = -28f, color = accentColor
                )
                // Hindwing Small Pink Dot: cx=-92, cy=56, rx=3.8, ry=6.0, rot=25
                drawRotatedOvalSpot(
                    cx = -92f, cy = 56f, rx = 3.8f, ry = 6.0f,
                    rotationDegrees = 25f, color = accentColor
                )
            }

            // --- C. RIGHT WING PAIR ---
            drawWingWithSkew(
                scaleX = wingScaleX,
                skewY = wingSkewTanY
            ) {
                // Outer black wing
                drawPath(path = pathCache.wingRightOuter, color = accentColor.copy(alpha = 0.22f), style = Stroke(5f))
                drawPath(path = pathCache.wingRightOuter, color = wingColor)
                // Inner pink patch
                drawPath(path = pathCache.wingRightInner, color = accentColor)

                // Forewing Upper Pink Spot: cx=98, cy=-99, rx=6.8, ry=13.5, rot=24
                drawRotatedOvalSpot(
                    cx = 98f, cy = -99f, rx = 6.8f, ry = 13.5f,
                    rotationDegrees = 24f, color = accentColor
                )
                // Forewing Second Pink Spot: cx=93, cy=-73, rx=4.5, ry=8.8, rot=28
                drawRotatedOvalSpot(
                    cx = 93f, cy = -73f, rx = 4.5f, ry = 8.8f,
                    rotationDegrees = 28f, color = accentColor
                )
                // Hindwing Small Pink Dot: cx=89, cy=58, rx=3.8, ry=6.0, rot=-22
                drawRotatedOvalSpot(
                    cx = 89f, cy = 58f, rx = 3.8f, ry = 6.0f,
                    rotationDegrees = -22f, color = accentColor
                )
            }

            // --- D. BUTTERFLY BODY ---
            drawPath(
                path = pathCache.body,
                color = wingColor
            )
        }
    }
}

/**
 * Helper to apply horizontal wing scaling + vertical perspective skew on the native canvas.
 */
private inline fun DrawScope.drawWingWithSkew(
    scaleX: Float,
    skewY: Float,
    block: DrawScope.() -> Unit
) {
    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.scale(scaleX, 1f)
    drawContext.canvas.nativeCanvas.skew(0f, skewY)
    block()
    drawContext.canvas.nativeCanvas.restore()
}

/**
 * Draws an elliptical wing spot rotated around its own center.
 */
private fun DrawScope.drawRotatedOvalSpot(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    rotationDegrees: Float,
    color: Color
) {
    withTransform({
        rotate(degrees = rotationDegrees, pivot = Offset(cx, cy))
    }) {
        drawOval(
            color = color,
            topLeft = Offset(cx - rx, cy - ry),
            size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f)
        )
    }
}


/** Tiny vector light catches keep the butterfly crisp at toolbar and portrait sizes. */
private fun DrawScope.drawTitliSpark(center: Offset, radius: Float, color: Color, brightness: Float) {
    drawCircle(color.copy(alpha = 0.10f * brightness), radius * 1.4f, center)
    val ink = color.copy(alpha = brightness.coerceIn(0f, 1f))
    drawLine(ink, center - Offset(radius, 0f), center + Offset(radius, 0f), 2f, StrokeCap.Round)
    drawLine(ink, center - Offset(0f, radius), center + Offset(0f, radius), 2f, StrokeCap.Round)
    drawCircle(color.copy(alpha = 0.85f), 1.6f, center)
}
