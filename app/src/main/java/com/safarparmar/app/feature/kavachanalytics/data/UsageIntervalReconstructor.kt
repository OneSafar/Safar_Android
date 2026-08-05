package com.safarparmar.app.feature.kavachanalytics.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A normalised foreground/background transition read out of Android `UsageEvents`. */
data class ForegroundTransition(
    val packageName: String,
    val timestampMs: Long,
    val movedToForeground: Boolean,
)

/** A reconstructed, closed foreground visit. */
data class UsageInterval(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** A [UsageInterval] already attributed to one local calendar day. */
data class DatedUsageInterval(
    val packageName: String,
    val localDate: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/**
 * Turns the raw `UsageEvents` stream into closed foreground intervals.
 *
 * Only boundaries SAFAR actually observed are trusted: an interval is emitted when a
 * foreground event opens it and a later event (or the end of the collection window)
 * closes it. Nothing is inferred from before [windowStartMs], so a gap in collection
 * never invents screen time.
 */
object UsageIntervalReconstructor {

    /**
     * Packages whose foreground time is never counted: SAFAR itself, the launcher,
     * system UI, and the permission/settings surfaces the user only visits because
     * Kavach sent them there.
     */
    val ALWAYS_EXCLUDED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.securitycenter",
        "com.samsung.android.permissioncontroller",
    )

    private val LAUNCHER_HINTS = listOf("launcher", "home")

    fun isExcluded(
        packageName: String,
        ownPackageName: String,
        homePackages: Set<String>,
    ): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == ownPackageName) return true
        if (packageName in ALWAYS_EXCLUDED_PACKAGES) return true
        if (packageName in homePackages) return true
        return LAUNCHER_HINTS.any { packageName.contains(it, ignoreCase = true) }
    }

    /**
     * @param transitions every transition observed in the window, in any order.
     * @param windowEndMs the instant collection stopped — used to close a still-open
     *   visit. This boundary is trustworthy because it is "now".
     */
    fun reconstruct(
        transitions: List<ForegroundTransition>,
        windowStartMs: Long,
        windowEndMs: Long,
        isExcluded: (String) -> Boolean = { false },
    ): List<UsageInterval> {
        if (windowEndMs <= windowStartMs) return emptyList()

        val ordered = transitions
            .filter { it.timestampMs in windowStartMs..windowEndMs }
            .sortedWith(compareBy({ it.timestampMs }, { !it.movedToForeground }))

        val intervals = mutableListOf<UsageInterval>()
        var openPackage: String? = null
        var openStartMs = 0L

        fun close(atMs: Long) {
            val pkg = openPackage ?: return
            val end = atMs.coerceAtMost(windowEndMs)
            if (end > openStartMs && !isExcluded(pkg)) {
                intervals += UsageInterval(pkg, openStartMs, end)
            }
            openPackage = null
        }

        ordered.forEach { transition ->
            if (transition.movedToForeground) {
                if (openPackage == transition.packageName) return@forEach
                close(transition.timestampMs)
                openPackage = transition.packageName
                openStartMs = transition.timestampMs
            } else if (openPackage == transition.packageName) {
                close(transition.timestampMs)
            }
        }
        close(windowEndMs)

        return intervals
    }

    /**
     * Splits intervals at local midnight so a session that runs past 00:00 credits
     * each calendar day correctly. Uses [zone] at the instant of the boundary, so a
     * DST shift moves the split with the wall clock rather than by a fixed 24h.
     */
    fun splitByLocalDate(interval: UsageInterval, zone: ZoneId): List<DatedUsageInterval> {
        if (interval.endMs <= interval.startMs) return emptyList()

        val result = mutableListOf<DatedUsageInterval>()
        var cursor = interval.startMs
        var guard = 0
        while (cursor < interval.endMs && guard++ < 400) {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(cursor), zone)
            val nextMidnightMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = minOf(nextMidnightMs, interval.endMs)
            if (segmentEnd > cursor) {
                result += DatedUsageInterval(
                    packageName = interval.packageName,
                    localDate = date.toString(),
                    startMs = cursor,
                    endMs = segmentEnd,
                )
            }
            // A zero-length step would loop forever if the zone rules ever put the
            // next midnight at or before the cursor; nudge past it defensively.
            cursor = if (segmentEnd > cursor) segmentEnd else cursor + 1
        }
        return result
    }

    fun splitAll(intervals: List<UsageInterval>, zone: ZoneId): List<DatedUsageInterval> =
        intervals.flatMap { splitByLocalDate(it, zone) }

    /**
     * Collapses overlapping/adjacent windows so [overlapMs] can sum them without
     * double-counting a millisecond that two windows both cover.
     */
    fun mergeWindows(windows: List<LongRange>): List<LongRange> {
        val sorted = windows.filter { it.last > it.first }.sortedBy { it.first }
        val merged = mutableListOf<LongRange>()
        sorted.forEach { window ->
            val last = merged.lastOrNull()
            if (last != null && window.first <= last.last) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, window.last)
            } else {
                merged += window
            }
        }
        return merged
    }

    /** Total milliseconds of [interval] that fall inside [windows]. Windows must be merged. */
    fun overlapMs(interval: DatedUsageInterval, windows: List<LongRange>): Long =
        windows.sumOf { window ->
            val start = maxOf(interval.startMs, window.first)
            val end = minOf(interval.endMs, window.last)
            (end - start).coerceAtLeast(0L)
        }
}
