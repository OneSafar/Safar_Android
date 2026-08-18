package com.safarparmar.app.feature.kavachanalytics.ui

import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Shared, testable formatting for the Kavach analytics surfaces. */
object KavachAnalyticsFormat {

    /** Floor for an empty or near-empty period, so the frame is never zero-height. */
    private const val DEFAULT_AXIS_MINUTES = 20

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

    /** Short value-axis tick: "2h", "30m", "0". */
    fun axisValue(seconds: Int): String = when {
        seconds <= 0 -> "0"
        seconds >= 3600 -> {
            val hours = seconds / 3600.0
            if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "%.1fh".format(hours)
        }
        else -> "${(seconds / 60).coerceAtLeast(1)}m"
    }

    /**
     * Rounds the axis up to the next familiar interval.
     *
     * Scaling the axis to whatever the data happened to be produced ticks like
     * "41m / 20m / 0", which asks the reader to do arithmetic before a bar means
     * anything. Snapping to a ladder of round durations means the gridlines are
     * always numbers a person already thinks in — and it also stops a quiet day
     * from drawing a full-height bar that looks alarming next to a busy one.
     */
    /**
     * Scales the Y-axis to a familiar duration close to the visible data. The previous
     * fixed 10-hour floor made ordinary usage under an hour practically invisible.
     */
    fun niceAxisMax(
        seconds: Int,
        granularity: com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity = com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.DAILY,
    ): Int {
        // Week compares whole days, so preserve a stable 0h / 5h / 10h frame.
        // Changing it to 0h / 1h / 2h exaggerates an otherwise ordinary short day.
        if (granularity == com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.WEEKLY &&
            seconds <= 10 * 3600
        ) {
            return 10 * 3600
        }

        val minimum = DEFAULT_AXIS_MINUTES * 60
        if (seconds <= minimum) return minimum

        val familiarMinutes = intArrayOf(30, 60, 120, 180, 240, 360, 480, 600, 720)
        familiarMinutes.firstOrNull { seconds <= it * 60 }?.let { return it * 60 }

        // Beyond 12 hours, retain readable whole-hour ticks and round upward.
        val hours = (seconds + 3599) / 3600
        val evenHours = ((hours + 1) / 2) * 2
        return evenHours * 3600
    }

    fun percent(part: Int, total: Int): Int =
        if (total <= 0) 0 else ((part.toDouble() / total) * 100).roundToInt().coerceIn(0, 100)

    fun dayLabel(isoDate: String): String = runCatching {
        java.time.LocalDate.parse(isoDate).format(dayFormatter)
    }.getOrDefault(isoDate)

    /** "Today", "Mon 4 Aug", "3 Aug – 9 Aug" or "August 2026", depending on the step size. */
    fun periodLabel(
        granularity: com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity,
        startIso: String,
        endIso: String,
    ): String = runCatching {
        val start = java.time.LocalDate.parse(startIso)
        val end = java.time.LocalDate.parse(endIso)
        val today = java.time.LocalDate.now()
        when (granularity) {
            com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.DAILY -> when (start) {
                today -> "Today · ${start.format(dayFormatter)}"
                today.minusDays(1) -> "Yesterday · ${start.format(dayFormatter)}"
                else -> start.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
            }
            com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.WEEKLY ->
                "${start.format(dayFormatter)} – ${end.format(dayFormatter)}"
            com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.MONTHLY ->
                start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        }
    }.getOrDefault("$startIso – $endIso")

    /** Compact x-axis tick: weekday initial for short spans, day number for long ones. */
    fun axisLabel(
        isoDate: String,
        granularity: com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity,
    ): String = runCatching {
        val date = java.time.LocalDate.parse(isoDate)
        when (granularity) {
            com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity.MONTHLY ->
                date.dayOfMonth.toString()
            else -> date.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.NARROW,
                Locale.getDefault(),
            )
        }
    }.getOrDefault("")

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
