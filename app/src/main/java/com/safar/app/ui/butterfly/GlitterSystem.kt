package com.safar.app.ui.butterfly

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sparkle particle.
 *
 * Enhanced v2:
 * - [isTail] flag: tail particles drift backward along the flight path
 * ```
 *    with a longer life, forming a visible glitter ribbon.
 * ```
 * - [isStar] flag: star-shaped cross drawn by ButterflyOverlay canvas.
 * - [gold] flag: gold-tinted particles mixed in with the pink ones.
 * - Faster decay for ambient sparkles; slower for tail particles.
 */
@Stable
data class GlitterParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        var life: Float = 1f,
        val decay: Float,
        val isTail: Boolean = false,
        val isStar: Boolean = false,
        val isGold: Boolean = false,
)

/** Call [tick] every frame, [spawn] / [spawnTail] when butterfly moves. */
class GlitterSystem {
    val particles = mutableStateListOf<GlitterParticle>()

    /**
     * Ambient sparkles orbiting the butterfly position. [count] increased to 5 for more visible
     * glitter cloud.
     */
    fun spawn(x: Float, y: Float, count: Int = 5) {
        repeat(count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val radius = 4f + Random.nextFloat() * 20f
            val decay = 0.007f + Random.nextFloat() * 0.014f

            val spawnX = x + cos(angle) * radius
            val spawnY = y + sin(angle) * radius

            // Inward velocity — converges back toward butterfly
            val dx = x - spawnX
            val dy = y - spawnY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
            val speed = radius * decay

            particles.add(
                    GlitterParticle(
                            x = spawnX,
                            y = spawnY,
                            vx = (dx / dist) * speed,
                            vy = (dy / dist) * speed,
                            size = 0.6f + Random.nextFloat() * 2.8f,
                            decay = decay,
                            isStar = Random.nextFloat() > 0.6f,
                            isGold = Random.nextFloat() > 0.72f,
                    )
            )
        }
    }

    /**
     * Tail particles — emitted at the butterfly's *previous* position, drifting away from the
     * flight direction. These are larger, slower to fade, and always rendered as stars for a
     * comet-tail look.
     *
     * [dx] / [dy] is the motion delta this frame (current - previous pos).
     */
    fun spawnTail(x: Float, y: Float, dx: Float, dy: Float, count: Int = 4) {
        repeat(count) {
            // Drift mostly opposite to flight direction + small random spread
            val spread = (Random.nextFloat() - 0.5f) * 1.6f
            val baseVx = -dx * 0.18f + spread
            val baseVy = -dy * 0.18f + spread

            particles.add(
                    GlitterParticle(
                            x = x + (Random.nextFloat() - 0.5f) * 8f,
                            y = y + (Random.nextFloat() - 0.5f) * 8f,
                            vx = baseVx,
                            vy = baseVy,
                            size = 1.4f + Random.nextFloat() * 3.0f,
                            life = 0.9f + Random.nextFloat() * 0.1f,
                            decay = 0.004f + Random.nextFloat() * 0.007f, // slower fade
                            isTail = true,
                            isStar = true,
                            isGold = Random.nextFloat() > 0.5f,
                    )
            )
        }
    }

    fun tick() {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= p.decay
            if (p.life <= 0f) iter.remove()
        }
    }

    fun clear() = particles.clear()
}
