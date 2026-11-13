package com.safarparmar.app.ui.calmcentre

import kotlin.math.PI
import kotlin.math.cos

internal enum class CalmBreathTechnique(val title: String, val pattern: String, val description: String,
    val inhale: Int, val hold: Int, val exhale: Int, val rest: Int) {
    GENTLE("Gentle breathing", "4 in · 6 out", "An easy inhale and a softer, longer exhale.", 4, 0, 6, 0),
    BELLY("Belly breathing", "4 in · 4 out", "Rest a hand on your belly and notice its gentle rise and fall.", 4, 0, 4, 0),
    BOX("Box breathing", "4 · 4 · 4 · 4", "Follow four equal sides: in, hold, out, and rest. Keep every phase comfortable.", 4, 4, 4, 4),
    LONG_EXHALE("Longer exhale", "4 in · 8 out", "Give your out-breath a little more room. Return to a natural breath whenever you need.", 4, 0, 8, 0),
}

internal data class CalmBreathFrame(val title: String, val detail: String, val expansion: Float, val progress: Float)

internal fun calmBreathFrame(time: Long, technique: CalmBreathTechnique): CalmBreathFrame {
    val lengths = intArrayOf(technique.inhale, technique.hold, technique.exhale, technique.rest)
    var position = time.coerceAtLeast(0L) % (lengths.sum() * 1000L)
    for (phase in lengths.indices) {
        val length = lengths[phase] * 1000L
        if (length > 0 && position < length) {
            val progress = position / length.toFloat()
            val eased = ((1.0 - cos(PI * progress)) * 0.5).toFloat()
            return when (phase) {
                0 -> CalmBreathFrame("Breathe in", "Let the petals open. In gently, for ${technique.inhale}.", eased, progress)
                1 -> CalmBreathFrame("Hold gently", "Let the open shape rest. Stay easy, for ${technique.hold}.", 1f, progress)
                2 -> CalmBreathFrame("Breathe out", "Let the petals soften. Out slowly, for ${technique.exhale}.", 1f - eased, progress)
                else -> CalmBreathFrame("Rest softly", "Let your breath rest comfortably, for ${technique.rest}.", 0f, progress)
            }
        }
        position -= length
    }
    error("Breathing technique must have a positive cycle")
}
