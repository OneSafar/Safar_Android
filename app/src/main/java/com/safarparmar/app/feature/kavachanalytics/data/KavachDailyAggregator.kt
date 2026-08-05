package com.safarparmar.app.feature.kavachanalytics.data

import com.safarparmar.app.feature.kavachanalytics.data.local.DailyAppAggregateEntity
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import kotlin.math.roundToInt

/** Per-package event counters rolled up for one day. */
data class DailyEventCounts(
    val blockedAttempts: Map<String, Int> = emptyMap(),
    val quickUnlocks: Map<String, Int> = emptyMap(),
)

/**
 * Folds one day's reconstructed intervals into the per-app rows that are the only
 * usage shape ever uploaded.
 *
 * Pure by design: everything time-, permission- and IO-dependent is resolved by the
 * caller so this can be tested directly.
 */
object KavachDailyAggregator {

    fun aggregate(
        localDate: String,
        intervals: List<DatedUsageInterval>,
        kavachWindows: List<LongRange>,
        eventCounts: DailyEventCounts,
        categoryOf: (String) -> AppCategory,
        labelOf: (String) -> String?,
        nowMs: Long,
    ): List<DailyAppAggregateEntity> {
        val merged = UsageIntervalReconstructor.mergeWindows(kavachWindows)

        val allDayMs = mutableMapOf<String, Long>()
        val kavachMs = mutableMapOf<String, Long>()

        intervals.filter { it.localDate == localDate }.forEach { interval ->
            allDayMs[interval.packageName] =
                (allDayMs[interval.packageName] ?: 0L) + interval.durationMs
            val overlap = UsageIntervalReconstructor.overlapMs(interval, merged)
            if (overlap > 0L) {
                kavachMs[interval.packageName] = (kavachMs[interval.packageName] ?: 0L) + overlap
            }
        }

        // Apps with zero measured foreground time can still have counters: a blocked
        // attempt is recorded the moment the block screen goes up, which is often
        // before the app ever registers a usable foreground interval.
        val packages = (allDayMs.keys + eventCounts.blockedAttempts.keys + eventCounts.quickUnlocks.keys)
            .filter { it.isNotBlank() }
            .toSortedSet()

        return packages.map { packageName ->
            DailyAppAggregateEntity(
                localDate = localDate,
                packageName = packageName,
                appLabel = labelOf(packageName),
                category = categoryOf(packageName).wire,
                allDaySeconds = msToSeconds(allDayMs[packageName] ?: 0L),
                kavachSeconds = msToSeconds(kavachMs[packageName] ?: 0L),
                blockedAttempts = eventCounts.blockedAttempts[packageName] ?: 0,
                quickUnlockCount = eventCounts.quickUnlocks[packageName] ?: 0,
                updatedAtMs = nowMs,
                synced = false,
            )
        }
    }

    /**
     * Category totals for one Kavach session window, used to fill the session
     * summary that syncs alongside the daily rows.
     */
    fun sessionCategoryTotals(
        intervals: List<DatedUsageInterval>,
        window: LongRange,
        categoryOf: (String) -> AppCategory,
    ): Map<AppCategory, Int> {
        val totals = mutableMapOf<AppCategory, Long>()
        intervals.forEach { interval ->
            val overlap = UsageIntervalReconstructor.overlapMs(interval, listOf(window))
            if (overlap > 0L) {
                val category = categoryOf(interval.packageName)
                totals[category] = (totals[category] ?: 0L) + overlap
            }
        }
        return totals.mapValues { msToSeconds(it.value) }
    }

    private fun msToSeconds(ms: Long): Int = (ms / 1000.0).roundToInt().coerceAtLeast(0)
}
