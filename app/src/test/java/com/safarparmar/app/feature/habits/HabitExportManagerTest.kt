package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitPerformance
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import com.safarparmar.app.feature.habits.export.ExportDateRangeOption
import com.safarparmar.app.feature.habits.export.HabitExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitExportManagerTest {

    private val exportManager = HabitExportManager()

    @Test
    fun `resolveDateRange resolves correct dates`() {
        val today = LocalDate.of(2026, 9, 16)

        val (monthStart, monthEnd) = exportManager.resolveDateRange(ExportDateRangeOption.THIS_MONTH, today = today)
        assertEquals(LocalDate.of(2026, 9, 1), monthStart)
        assertEquals(LocalDate.of(2026, 9, 30), monthEnd)

        val (yearStart, yearEnd) = exportManager.resolveDateRange(ExportDateRangeOption.THIS_YEAR, today = today)
        assertEquals(LocalDate.of(2026, 1, 1), yearStart)
        assertEquals(LocalDate.of(2026, 12, 31), yearEnd)

        val earliest = LocalDate.of(2024, 5, 10)
        val (allStart, allEnd) = exportManager.resolveDateRange(
            ExportDateRangeOption.ALL_TIME,
            today = today,
            earliestHabitDate = earliest
        )
        assertEquals(earliest, allStart)
        assertEquals(today, allEnd)

        val customS = LocalDate.of(2026, 8, 1)
        val customE = LocalDate.of(2026, 8, 15)
        val (cStart, cEnd) = exportManager.resolveDateRange(
            ExportDateRangeOption.CUSTOM,
            customStart = customS,
            customEnd = customE,
            today = today
        )
        assertEquals(customS, cStart)
        assertEquals(customE, cEnd)
    }

    @Test
    fun `empty dataset generates clean headers`() {
        val csv = exportManager.generateCsv(
            habits = emptyList(),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 5)
        )
        assertEquals("Date,Habit,Scheduled,Completed\r\n", csv)
    }

    @Test
    fun `csv escaping handles commas, double-quotes and newlines`() {
        assertEquals("Normal", exportManager.escapeCsv("Normal"))
        assertEquals("\"Read, Study\"", exportManager.escapeCsv("Read, Study"))
        assertEquals("\"Read \"\"Deep Work\"\"\"", exportManager.escapeCsv("Read \"Deep Work\""))
        assertEquals("\"Line1\nLine2\"", exportManager.escapeCsv("Line1\nLine2"))
    }

    @Test
    fun `exports rows with correct dates, habit names, and completed flags`() {
        val habit1 = HabitEntity(id = 1L, name = "Reading")
        val habit2 = HabitEntity(id = 2L, name = "Workout, Cardio")

        val day1 = LocalDate.of(2026, 9, 1)
        val day2 = LocalDate.of(2026, 9, 2)

        val h1 = HabitWithCompletions(
            habit = habit1,
            completionByDate = mapOf(day1 to true, day2 to false)
        )
        val h2 = HabitWithCompletions(
            habit = habit2,
            completionByDate = mapOf(day1 to true)
        )

        val performanceList = listOf(
            HabitPerformance(
                habitId = 1L,
                habitName = "Reading",
                scheduledCount = 2,
                completedCount = 1,
                completionRate = 0.50f,
                currentStreak = 0,
                bestStreak = 1
            ),
            HabitPerformance(
                habitId = 2L,
                habitName = "Workout, Cardio",
                scheduledCount = 1,
                completedCount = 1,
                completionRate = 1.0f,
                currentStreak = 1,
                bestStreak = 1
            )
        )

        val csv = exportManager.generateCsv(
            habits = listOf(h1, h2),
            startDate = day1,
            endDate = day2,
            performanceList = performanceList
        )

        assertTrue(csv.contains("2026-09-01,Reading,Yes,Yes"))
        assertTrue(csv.contains("2026-09-01,\"Workout, Cardio\",Yes,Yes"))
        assertTrue(csv.contains("2026-09-02,Reading,Yes,No"))
        assertFalse(csv.contains("2026-09-02,\"Workout, Cardio\""))

        assertTrue(csv.contains("Habit Summary"))
        assertTrue(csv.contains("Reading,50%,0,1"))
        assertTrue(csv.contains("\"Workout, Cardio\",100%,1,1"))
    }

    @Test
    fun `selected habits filter excludes other habits from csv and summary`() {
        val habit1 = HabitEntity(id = 1L, name = "Reading")
        val habit2 = HabitEntity(id = 2L, name = "Running")

        val day1 = LocalDate.of(2026, 9, 1)
        val h1 = HabitWithCompletions(habit = habit1, completionByDate = mapOf(day1 to true))
        val h2 = HabitWithCompletions(habit = habit2, completionByDate = mapOf(day1 to true))

        val perf = listOf(
            HabitPerformance(1L, "Reading", 1, 1, 1f, 1, 1),
            HabitPerformance(2L, "Running", 1, 1, 1f, 1, 1)
        )

        val csv = exportManager.generateCsv(
            habits = listOf(h1, h2),
            startDate = day1,
            endDate = day1,
            performanceList = perf,
            selectedHabitIds = setOf(1L)
        )

        assertTrue(csv.contains("Reading"))
        assertFalse(csv.contains("Running"))
    }

    @Test
    fun `archived habits export correctly during active periods`() {
        val archivedHabit = HabitEntity(
            id = 5L,
            name = "Archived Guitar Practice",
            isArchived = true,
            archivedAt = LocalDate.of(2026, 6, 1)
        )

        val day = LocalDate.of(2026, 5, 20)
        val h = HabitWithCompletions(habit = archivedHabit, completionByDate = mapOf(day to true))

        val csv = exportManager.generateCsv(
            habits = listOf(h),
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 31)
        )

        assertTrue(csv.contains("2026-05-20,Archived Guitar Practice,Yes,Yes"))
    }
}
