package com.safarparmar.app.feature.kavachanalytics.ui

import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Shared, testable formatting for the Kavach analytics surfaces. */
object KavachAnalyticsFormat {

    private val dayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val sessionFormatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())

    fun duration(seconds: Int): String {
        if (seconds <= 0) return "0m"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun percent(part: Int, total: Int): Int =
        if (total <= 0) 0 else ((part.toDouble() / total) * 100).roundToInt().coerceIn(0, 100)

    fun dayLabel(isoDate: String): String = runCatching {
        java.time.LocalDate.parse(isoDate).format(dayFormatter)
    }.getOrDefault(isoDate)

    fun sessionLabel(epochMs: Long): String = runCatching {
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(sessionFormatter)
    }.getOrDefault("")

    /**
     * The student-facing wording. "Ended early" is deliberate: a session the user
     * chose to stop, or that the system interrupted, is never called a failure.
     */
    fun outcomeLabel(outcome: KavachSessionOutcome?): String = when (outcome) {
        KavachSessionOutcome.COMPLETED -> "Completed"
        KavachSessionOutcome.ENDED_EARLY -> "Ended early"
        KavachSessionOutcome.INTERRUPTED -> "Interrupted"
        null -> "In progress"
    }
}
