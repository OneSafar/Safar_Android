package com.safarparmar.app.ui.calmcentre

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class CalmCompanionPose(
    val x: Float,
    val y: Float,
    val depth: Float,
    val scale: Float,
    val turn: Float,
    val tilt: Float,
)

/** A complete exchange takes 12 seconds; a full, seamless orbit takes 24. */
internal fun calmCompanionPose(elapsedMillis: Long, opposite: Boolean): CalmCompanionPose {
    val angle = (elapsedMillis.coerceAtLeast(0L) % 24_000L) / 24_000f * (2f * PI.toFloat())
    val direction = if (opposite) -1f else 1f
    val across = cos(angle) * direction
    val depth = sin(angle) * direction
    return CalmCompanionPose(
        x = 500f - 150f * across,
        y = 400f + 46f * depth,
        depth = depth,
        scale = 1f + 0.12f * depth,
        turn = 0.9f + 0.1f * abs(cos(angle)),
        tilt = -10f * across + 5f * depth,
    )
}

/** Mirrors the four wind-down cues, easing into each new height without a position jump. */
internal fun calmRestMoonY(elapsedMillis: Long, durationMillis: Long): Float {
    val duration = durationMillis.coerceAtLeast(1L)
    val elapsed = elapsedMillis.coerceIn(0L, duration - 1L)
    val position = elapsed.toDouble() * 4.0 / duration
    val step = position.toInt().coerceIn(0, 3)
    val previous = when (step) { 0 -> 260f; 1 -> 280f; 2 -> 355f; else -> 300f }
    val target = when (step) { 0 -> 280f; 1 -> 355f; 2 -> 300f; else -> 425f }
    val stepMillis = duration / 4.0
    val transitionMillis = minOf(4500.0, stepMillis * 0.35)
    val t = ((position - step) * stepMillis / transitionMillis).coerceIn(0.0, 1.0).toFloat()
    // Quintic easing has zero velocity and acceleration at both ends.
    val ease = t * t * t * (t * (6f * t - 15f) + 10f)
    return previous + (target - previous) * ease
}
