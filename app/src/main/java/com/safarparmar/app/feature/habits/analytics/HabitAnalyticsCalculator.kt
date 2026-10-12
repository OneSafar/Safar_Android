package com.safarparmar.app.feature.habits.analytics

import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.data.HabitPerformance
import com.safarparmar.app.feature.habits.data.HabitPeriodStats
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitAnalyticsCalculator @Inject constructor() {

    /**
     * Calculates period summary statistics across habits in [startDate, endDate].
     * Excludes future dates beyond [today] from statistics so incomplete future
     * days don't falsely drag down the completion percentage.
     */
    fun calculatePeriodStats(
        habits: List<HabitWithCompletions>,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): HabitPeriodStats {
        if (startDate.isAfter(today)) {
            return HabitPeriodStats(
                scheduledCount = 0,
                completedCount = 0,
                completionRate = 0f,
                activeDays = 0,
                perfectDays = 0
            )
        }
        val effectiveEndDate = if (endDate.isAfter(today)) today else endDate

        var totalScheduled = 0
        var totalCompleted = 0
        var activeDays = 0
        var perfectDays = 0

        var cursor = startDate
        while (!cursor.isAfter(effectiveEndDate)) {
            var dayScheduled = 0
            var dayCompleted = 0
            for (hwc in habits) {
                val isCompleted = hwc.completionByDate[cursor]
                if (isCompleted != null) {
                    dayScheduled++
                    if (isCompleted) {
                        dayCompleted++
                    }
                }
            }
            totalScheduled += dayScheduled
            totalCompleted += dayCompleted

            if (dayCompleted > 0) {
                activeDays++
            }
            if (dayScheduled > 0 && dayCompleted == dayScheduled) {
                perfectDays++
            }
            cursor = cursor.plusDays(1)
        }

        val rate = if (totalScheduled > 0) totalCompleted.toFloat() / totalScheduled.toFloat() else 0f
        return HabitPeriodStats(
            scheduledCount = totalScheduled,
            completedCount = totalCompleted,
            completionRate = rate,
            activeDays = activeDays,
            perfectDays = perfectDays
        )
    }

    /**
     * Produces daily breakdown stats for each day from [startDate] to [endDate].
     * Days in the future are marked with 0 completed and 0% completion rate.
     */
    fun calculateDailyStats(
        habits: List<HabitWithCompletions>,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): List<DailyHabitStats> {
        val result = ArrayList<DailyHabitStats>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val isFuture = cursor.isAfter(today)
            var scheduled = 0
            var completed = 0
            for (hwc in habits) {
                val done = hwc.completionByDate[cursor]
                if (done != null) {
                    scheduled++
                    if (done && !isFuture) {
                        completed++
                    }
                }
            }
            val rate = if (!isFuture && scheduled > 0) completed.toFloat() / scheduled.toFloat() else 0f
            result.add(
                DailyHabitStats(
                    date = cursor,
                    scheduledCount = scheduled,
                    completedCount = completed,
                    completionRate = rate
                )
            )
            cursor = cursor.plusDays(1)
        }
        return result
    }

    /**
     * Calculates per-habit performance over [startDate, endDate].
     * Current streak and best streak are derived from the habit's full history in [hwc.completionByDate].
     */
    fun calculateHabitPerformance(
        habits: List<HabitWithCompletions>,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): List<HabitPerformance> {
        val effectiveEndDate = if (endDate.isAfter(today)) today else endDate
        return habits.map { hwc ->
            val habit = hwc.habit
            val completionByDate = hwc.completionByDate

            var scheduledCount = 0
            var completedCount = 0
            var cursor = startDate
            while (!cursor.isAfter(effectiveEndDate)) {
                val done = completionByDate[cursor]
                if (done != null) {
                    scheduledCount++
                    if (done) completedCount++
                }
                cursor = cursor.plusDays(1)
            }
            val rate = if (scheduledCount > 0) completedCount.toFloat() / scheduledCount.toFloat() else 0f
            val (currentStreak, bestStreak) = calculateStreaks(completionByDate, today)

            HabitPerformance(
                habitId = habit.id,
                habitName = habit.name,
                scheduledCount = scheduledCount,
                completedCount = completedCount,
                completionRate = rate,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                isArchived = habit.isArchived
            )
        }
    }

    /**
     * Calculates current and best streaks for a given completion map.
     * - Current streak: consecutive completed scheduled days leading up to [today].
     *   An unfinished scheduled day on [today] does not break the streak from yesterday.
     * - Best streak: maximum consecutive completed scheduled days recorded in [completionByDate].
     */
    fun calculateStreaks(
        completionByDate: Map<LocalDate, Boolean>,
        today: LocalDate = LocalDate.now()
    ): Pair<Int, Int> {
        if (completionByDate.isEmpty()) return 0 to 0

        val pastOrTodayDates = completionByDate.keys.filter { !it.isAfter(today) }.sorted()
        if (pastOrTodayDates.isEmpty()) return 0 to 0

        // Current streak walking backwards from today
        var current = 0
        var firstScheduledSeen = false
        for (i in pastOrTodayDates.indices.reversed()) {
            val date = pastOrTodayDates[i]
            val isDone = completionByDate[date] == true
            if (!firstScheduledSeen && date == today && !isDone) {
                // Today is not yet completed, check previous scheduled day
                firstScheduledSeen = true
                continue
            }
            if (isDone) {
                current++
                firstScheduledSeen = true
            } else {
                break
            }
        }

        // Best streak walking forwards
        var best = 0
        var currentRun = 0
        for (date in pastOrTodayDates) {
            if (completionByDate[date] == true) {
                currentRun++
                if (currentRun > best) best = currentRun
            } else {
                currentRun = 0
            }
        }

        return current to maxOf(best, current)
    }

    /**
     * Returns contribution calendar heatmap level (0 to 4):
     * 0: No scheduled habits or 0% completion
     * 1: 1–25% completion
     * 2: 26–50% completion
     * 3: 51–99% completion
     * 4: 100% completion
     */
    fun heatmapLevel(completionRate: Float, scheduledCount: Int, isFuture: Boolean = false): Int {
        if (isFuture || scheduledCount == 0 || completionRate <= 0f) return 0
        return when {
            completionRate <= 0.25f -> 1
            completionRate <= 0.50f -> 2
            completionRate < 1.0f -> 3
            else -> 4
        }
    }
}
