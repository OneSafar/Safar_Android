package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.analytics.HabitAnalyticsCalculator
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitAnalyticsCalculatorTest {

    private val calculator = HabitAnalyticsCalculator()
    private val today = LocalDate.of(2026, 9, 16)

    @Test
    fun `no habits returns zeroed stats`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 30)

        val periodStats = calculator.calculatePeriodStats(emptyList(), start, end, today)
        assertEquals(0, periodStats.scheduledCount)
        assertEquals(0, periodStats.completedCount)
        assertEquals(0f, periodStats.completionRate, 0.001f)
        assertEquals(0, periodStats.activeDays)
        assertEquals(0, periodStats.perfectDays)

        val dailyStats = calculator.calculateDailyStats(emptyList(), start, end, today)
        assertEquals(30, dailyStats.size)
        dailyStats.forEach {
            assertEquals(0, it.scheduledCount)
            assertEquals(0, it.completedCount)
            assertEquals(0f, it.completionRate, 0.001f)
        }

        val performance = calculator.calculateHabitPerformance(emptyList(), start, end, today)
        assertEquals(0, performance.size)
    }

    @Test
    fun `one habit with 100 percent completion`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 10)
        val habit = HabitEntity(id = 1L, name = "Meditation")

        val completionMap = (1..10).associate { day ->
            LocalDate.of(2026, 9, day) to true
        }
        val habitWithCompletions = HabitWithCompletions(habit, completionMap)

        val stats = calculator.calculatePeriodStats(listOf(habitWithCompletions), start, end, today)
        assertEquals(10, stats.scheduledCount)
        assertEquals(10, stats.completedCount)
        assertEquals(1.0f, stats.completionRate, 0.001f)
        assertEquals(10, stats.activeDays)
        assertEquals(10, stats.perfectDays)
    }

    @Test
    fun `one habit with 0 percent completion`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 10)
        val habit = HabitEntity(id = 1L, name = "Workout")

        val completionMap = (1..10).associate { day ->
            LocalDate.of(2026, 9, day) to false
        }
        val habitWithCompletions = HabitWithCompletions(habit, completionMap)

        val stats = calculator.calculatePeriodStats(listOf(habitWithCompletions), start, end, today)
        assertEquals(10, stats.scheduledCount)
        assertEquals(0, stats.completedCount)
        assertEquals(0.0f, stats.completionRate, 0.001f)
        assertEquals(0, stats.activeDays)
        assertEquals(0, stats.perfectDays)
    }

    @Test
    fun `multiple habits partial completion and perfect days calculation`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 3)

        // Day 1: both complete -> active, perfect
        // Day 2: habit1 complete, habit2 not complete -> active, not perfect
        // Day 3: neither complete -> not active, not perfect
        val habit1 = HabitEntity(id = 1L, name = "H1")
        val habit2 = HabitEntity(id = 2L, name = "H2")

        val h1Map = mapOf(
            LocalDate.of(2026, 9, 1) to true,
            LocalDate.of(2026, 9, 2) to true,
            LocalDate.of(2026, 9, 3) to false
        )
        val h2Map = mapOf(
            LocalDate.of(2026, 9, 1) to true,
            LocalDate.of(2026, 9, 2) to false,
            LocalDate.of(2026, 9, 3) to false
        )

        val habits = listOf(
            HabitWithCompletions(habit1, h1Map),
            HabitWithCompletions(habit2, h2Map)
        )

        val periodStats = calculator.calculatePeriodStats(habits, start, end, today)
        assertEquals(6, periodStats.scheduledCount)
        assertEquals(3, periodStats.completedCount)
        assertEquals(0.5f, periodStats.completionRate, 0.001f)
        assertEquals(2, periodStats.activeDays) // Day 1 and Day 2
        assertEquals(1, periodStats.perfectDays) // Only Day 1
    }

    @Test
    fun `future dates in current month are not counted against completion rate`() {
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 30)
        // Today is Sept 16. Days 1..16 are evaluated (16 scheduled).
        // Days 17..30 are future dates and should not be counted in periodStats.
        val habit = HabitEntity(id = 1L, name = "Reading")
        val completionMap = (1..30).associate { day ->
            val date = LocalDate.of(2026, 9, day)
            // completed all days 1..16
            date to (day <= 16)
        }
        val habits = listOf(HabitWithCompletions(habit, completionMap))

        val stats = calculator.calculatePeriodStats(habits, start, end, today)
        assertEquals(16, stats.scheduledCount)
        assertEquals(16, stats.completedCount)
        assertEquals(1.0f, stats.completionRate, 0.001f)
        assertEquals(16, stats.activeDays)
        assertEquals(16, stats.perfectDays)

        // But dailyStats includes all days in the requested range
        val daily = calculator.calculateDailyStats(habits, start, end, today)
        assertEquals(30, daily.size)
        // Day 20 is future
        val day20 = daily.first { it.date == LocalDate.of(2026, 9, 20) }
        assertEquals(1, day20.scheduledCount)
        assertEquals(0, day20.completedCount)
        assertEquals(0f, day20.completionRate, 0.001f)
    }

    @Test
    fun `habit scheduled only on specific weekdays`() {
        val start = LocalDate.of(2026, 9, 1) // Tuesday
        val end = LocalDate.of(2026, 9, 7)   // Monday
        val habit = HabitEntity(id = 1L, name = "Sunday Service", targetDays = setOf(DayOfWeek.SUNDAY))

        // In range Sept 1 to Sept 7, only Sept 6 is a Sunday.
        val completionMap = mapOf(LocalDate.of(2026, 9, 6) to true)
        val habits = listOf(HabitWithCompletions(habit, completionMap))

        val stats = calculator.calculatePeriodStats(habits, start, end, today)
        assertEquals(1, stats.scheduledCount)
        assertEquals(1, stats.completedCount)
        assertEquals(1.0f, stats.completionRate, 0.001f)
        assertEquals(1, stats.activeDays)
        assertEquals(1, stats.perfectDays)

        val performance = calculator.calculateHabitPerformance(habits, start, end, today)
        assertEquals(1, performance.size)
        assertEquals(1, performance[0].scheduledCount)
        assertEquals(1, performance[0].completedCount)
        assertEquals(1.0f, performance[0].completionRate, 0.001f)
    }

    @Test
    fun `archived habit correctly reflects past completions and stops scheduling after archive date`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 6, 30)

        // Habit active Jan 1 -> March 15, then archived March 15
        val habit = HabitEntity(
            id = 2L,
            name = "Morning Run",
            isArchived = true,
            archivedAt = LocalDate.of(2026, 3, 15)
        )

        // Only scheduled through March 15
        val completionMap = (1..74).associate { dayOfYear ->
            LocalDate.ofYearDay(2026, dayOfYear) to true
        }
        val habits = listOf(HabitWithCompletions(habit, completionMap))

        val stats = calculator.calculatePeriodStats(habits, start, end, today)
        assertEquals(74, stats.scheduledCount)
        assertEquals(74, stats.completedCount)
        assertEquals(1.0f, stats.completionRate, 0.001f)
        assertEquals(74, stats.activeDays)
        assertEquals(74, stats.perfectDays)

        val performance = calculator.calculateHabitPerformance(habits, start, end, today)
        assertEquals(true, performance[0].isArchived)
        assertEquals(74, performance[0].scheduledCount)
        assertEquals(74, performance[0].completedCount)
    }

    @Test
    fun `leap year boundary February 29 2024 vs 2026`() {
        val leapFeb = LocalDate.of(2024, 2, 1)
        val leapEnd = LocalDate.of(2024, 2, 29)
        val habit = HabitEntity(id = 1L, name = "Daily Journal")

        val leapMap = (1..29).associate { day ->
            LocalDate.of(2024, 2, day) to true
        }
        val leapHabits = listOf(HabitWithCompletions(habit, leapMap))
        val stats2024 = calculator.calculatePeriodStats(leapHabits, leapFeb, leapEnd, LocalDate.of(2024, 3, 1))
        assertEquals(29, stats2024.scheduledCount)
        assertEquals(29, stats2024.completedCount)

        val nonLeapFeb = LocalDate.of(2026, 2, 1)
        val nonLeapEnd = LocalDate.of(2026, 2, 28)
        val nonLeapMap = (1..28).associate { day ->
            LocalDate.of(2026, 2, day) to true
        }
        val nonLeapHabits = listOf(HabitWithCompletions(habit, nonLeapMap))
        val stats2026 = calculator.calculatePeriodStats(nonLeapHabits, nonLeapFeb, nonLeapEnd, LocalDate.of(2026, 3, 1))
        assertEquals(28, stats2026.scheduledCount)
        assertEquals(28, stats2026.completedCount)
    }

    @Test
    fun `current streak preserved when today is scheduled but not yet completed`() {
        // Today is Sept 16.
        // Sept 13: done
        // Sept 14: done
        // Sept 15: done
        // Sept 16: NOT done yet (user hasn't finished the day)
        val map = mapOf(
            LocalDate.of(2026, 9, 13) to true,
            LocalDate.of(2026, 9, 14) to true,
            LocalDate.of(2026, 9, 15) to true,
            LocalDate.of(2026, 9, 16) to false
        )

        val (current, best) = calculator.calculateStreaks(map, today)
        assertEquals(3, current)
        assertEquals(3, best)
    }

    @Test
    fun `current streak broken when yesterday was scheduled and missed`() {
        // Today is Sept 16 (not done).
        // Sept 15: missed!
        // Sept 14: done
        // Sept 13: done
        val map = mapOf(
            LocalDate.of(2026, 9, 13) to true,
            LocalDate.of(2026, 9, 14) to true,
            LocalDate.of(2026, 9, 15) to false,
            LocalDate.of(2026, 9, 16) to false
        )

        val (current, best) = calculator.calculateStreaks(map, today)
        assertEquals(0, current)
        assertEquals(2, best)
    }

    @Test
    fun `best historical streak identifies peak run`() {
        // Run of 5 days, then missed 2 days, then current run of 3 days.
        val map = mapOf(
            LocalDate.of(2026, 9, 1) to true,
            LocalDate.of(2026, 9, 2) to true,
            LocalDate.of(2026, 9, 3) to true,
            LocalDate.of(2026, 9, 4) to true,
            LocalDate.of(2026, 9, 5) to true,
            LocalDate.of(2026, 9, 6) to false,
            LocalDate.of(2026, 9, 7) to false,
            LocalDate.of(2026, 9, 8) to true,
            LocalDate.of(2026, 9, 9) to true,
            LocalDate.of(2026, 9, 10) to true
        )

        val (current, best) = calculator.calculateStreaks(map, LocalDate.of(2026, 9, 10))
        assertEquals(3, current)
        assertEquals(5, best)
    }

    @Test
    fun `habit with schedule gaps preserves streak across unscheduled days`() {
        // Habit scheduled only Mon and Wed:
        // Mon Sept 7: done
        // Wed Sept 9: done
        // Mon Sept 14: done
        // Wed Sept 16: done (today)
        val map = mapOf(
            LocalDate.of(2026, 9, 7) to true,
            LocalDate.of(2026, 9, 9) to true,
            LocalDate.of(2026, 9, 14) to true,
            LocalDate.of(2026, 9, 16) to true
        )

        val (current, best) = calculator.calculateStreaks(map, today)
        assertEquals(4, current)
        assertEquals(4, best)
    }

    @Test
    fun `heatmap level mapping matches thresholds`() {
        // 0% -> 0
        assertEquals(0, calculator.heatmapLevel(0f, scheduledCount = 10))
        // 0 scheduled -> 0
        assertEquals(0, calculator.heatmapLevel(1.0f, scheduledCount = 0))
        // Future -> 0
        assertEquals(0, calculator.heatmapLevel(1.0f, scheduledCount = 5, isFuture = true))
        // 1-25% -> 1
        assertEquals(1, calculator.heatmapLevel(0.20f, scheduledCount = 10))
        assertEquals(1, calculator.heatmapLevel(0.25f, scheduledCount = 10))
        // 26-50% -> 2
        assertEquals(2, calculator.heatmapLevel(0.26f, scheduledCount = 10))
        assertEquals(2, calculator.heatmapLevel(0.50f, scheduledCount = 10))
        // 51-99% -> 3
        assertEquals(3, calculator.heatmapLevel(0.51f, scheduledCount = 10))
        assertEquals(3, calculator.heatmapLevel(0.99f, scheduledCount = 10))
        // 100% -> 4
        assertEquals(4, calculator.heatmapLevel(1.0f, scheduledCount = 10))
    }
}
