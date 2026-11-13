package com.safarparmar.app.ui.calmcentre

import kotlin.math.cos
import kotlin.math.PI

internal enum class CalmMood(val title: String, val detail: String) {
    OVERWHELMED("A little overwhelmed", "There is a lot on my mind"),
    LOW("Feeling low", "I could use a little kindness"),
    RESTLESS("Restless & distracted", "It is hard to settle into one thing"),
    TIRED("Ready to unwind", "I want to put the day down"),
    OKAY("Doing okay", "Just making a little room for myself"),
}

internal enum class CalmIntention(val title: String, val detail: String) {
    RESET("Help me reset", "A little space between me and my thoughts"),
    SUPPORT("Be kinder to myself", "Less pressure. A softer inner voice"),
    FOCUS("Feel more present", "Come back to what is around me"),
    REST("Ease into rest", "Slow down without needing to achieve"),
}

internal enum class CalmPractice(val title: String, val category: String, val description: String) {
    BREATHE("Find your rhythm", "Gentle breathing", "Follow an easy inhale and a longer exhale. No breath holds, no rush."),
    KINDNESS("A little self-kindness", "Compassion practice", "Make room for how you feel, then offer yourself a few kind words."),
    GROUND("Come back to here", "Sensory grounding", "Notice the small, ordinary things around you. One sense at a time."),
    REST("Let the day settle", "Quiet wind-down", "Soften your shoulders and let your attention rest on a quiet, drifting sky."),
}

internal data class CalmRecommendation(val practice: CalmPractice, val reason: String)

internal fun recommendCalmPractice(mood: CalmMood, intention: CalmIntention): CalmRecommendation {
    // An explicit request for kindness, presence or rest takes priority over a mood label.
    val practice = when (intention) {
        CalmIntention.SUPPORT -> CalmPractice.KINDNESS
        CalmIntention.FOCUS -> CalmPractice.GROUND
        CalmIntention.REST -> CalmPractice.REST
        CalmIntention.RESET -> when (mood) {
            CalmMood.LOW -> CalmPractice.KINDNESS
            CalmMood.RESTLESS -> CalmPractice.GROUND
            CalmMood.TIRED -> CalmPractice.REST
            else -> CalmPractice.BREATHE
        }
    }
    val reason = when (practice) {
        CalmPractice.BREATHE -> "You asked for a reset. A simple breathing rhythm gives your attention one gentle place to land."
        CalmPractice.KINDNESS -> "A softer inner voice can be a place to begin. This practice makes room for you, just as you are."
        CalmPractice.GROUND -> "You are looking for a little presence. Start with what you can notice, right here around you."
        CalmPractice.REST -> "You are ready for a slower pace. This is a quiet space to pause and let the day settle."
    }
    return CalmRecommendation(practice, reason)
}

internal data class CalmCue(val title: String, val detail: String)

internal fun calmCue(practice: CalmPractice, elapsedMillis: Long, durationMillis: Long): CalmCue {
    val elapsed = elapsedMillis.coerceIn(0L, durationMillis.coerceAtLeast(1L) - 1L)
    return when (practice) {
        CalmPractice.BREATHE -> if (elapsed % 10_000L < 4_000L) {
            CalmCue("Breathe in", "Let the petals open. In gently, for 4.")
        } else {
            CalmCue("Breathe out", "Let the petals soften. Out slowly, for 6.")
        }
        CalmPractice.KINDNESS -> listOf(
            CalmCue("Make a little room", "Notice how you feel. You do not have to change it."),
            CalmCue("You are allowed to pause", "Let your shoulders drop. There is nothing to prove here."),
            CalmCue("Offer yourself kindness", "Try saying: may I be patient with myself."),
            CalmCue("Carry a little softness", "Take one kind thought with you into the rest of your day."),
        )[((elapsed * 4L) / durationMillis.coerceAtLeast(1L)).toInt().coerceIn(0, 3)]
        CalmPractice.GROUND -> listOf(
            CalmCue("Notice five things", "Let your eyes find five things you can see."),
            CalmCue("Feel four things", "Notice four points of touch: your feet, your clothes, your seat."),
            CalmCue("Listen for three sounds", "Near or far. Let each sound arrive on its own."),
            CalmCue("Notice two scents", "If nothing stands out, imagine two familiar scents."),
            CalmCue("One small taste", "Notice a taste, or imagine a sip of something you enjoy."),
        )[((elapsed * 5L) / durationMillis.coerceAtLeast(1L)).toInt().coerceIn(0, 4)]
        CalmPractice.REST -> listOf(
            CalmCue("Arrive, just as you are", "Feel the surface beneath you. Let it support your weight."),
            CalmCue("Unclench a little", "Soften your forehead, your jaw, and then your shoulders."),
            CalmCue("Let thoughts drift", "You can notice a thought without following it."),
            CalmCue("Nothing more to do", "Rest your attention here. Let your breath find its own pace."),
        )[((elapsed * 4L) / durationMillis.coerceAtLeast(1L)).toInt().coerceIn(0, 3)]
    }
}

/** One continuous curve, shared by the blossom and breathing instructions. */
internal fun calmBreathExpansion(elapsedMillis: Long): Float {
    val position = elapsedMillis.coerceAtLeast(0L) % 10_000L
    val linear = if (position < 4_000L) position / 4_000f else 1f - (position - 4_000L) / 6_000f
    return ((1.0 - cos(PI * linear)) * 0.5).toFloat()
}

internal fun calmTimeLabel(remainingMillis: Long): String {
    val seconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
