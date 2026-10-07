package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitScheduleHistoryRepositoryTest {
    private val monday = LocalDate.of(2026, 9, 21)
    private val habit = HabitEntity(id = 1, name = "Read", scheduledSince = monday,
        targetDays = setOf(DayOfWeek.MONDAY), isEveryDay = false)

    @Test fun `later weekday revision does not rewrite earlier scheduled days`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        val revision = HabitScheduleRevisionEntity(habitId = 1, effectiveFrom = monday.plusDays(1),
            targetDays = setOf(DayOfWeek.WEDNESDAY), isEveryDay = false)
        coEvery { dao.getHabitsForRange(any(), any()) } returns listOf(habit)
        coEvery { dao.getScheduleRevisionsForRange(any(), any()) } returns listOf(revision)
        coEvery { dao.getCompletionsBetween(any(), any()) } returns listOf(HabitCompletionEntity(1, monday, true))
        val result = HabitRepository(dao).getHabitsWithCompletionsForRange(monday, monday.plusWeeks(1)).single()
        assertEquals(mapOf(monday to true, monday.plusDays(2) to false), result.completionByDate)
    }

    @Test fun `days before the initial schedule have no completion slots`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.getHabitsForRange(any(), any()) } returns listOf(habit)
        val result = HabitRepository(dao).getHabitsWithCompletionsForRange(monday.minusWeeks(1), monday).single()
        assertEquals(mapOf(monday to false), result.completionByDate)
    }

    @Test fun `archival preserves earlier completion history and removes later slots`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.getHabitsForRange(any(), any()) } returns listOf(habit.copy(isArchived = true, archivedAt = monday))
        coEvery { dao.getCompletionsBetween(any(), any()) } returns listOf(HabitCompletionEntity(1, monday, true))
        val result = HabitRepository(dao).getHabitsWithCompletionsForRange(monday, monday.plusWeeks(2)).single()
        assertEquals(mapOf(monday to true), result.completionByDate)
    }

    @Test fun `date view uses the schedule revision effective on that date`() = runTest {
        val dao = mockk<HabitDao>()
        val wednesday = monday.plusDays(2)
        every { dao.observeActiveHabits() } returns MutableStateFlow(listOf(habit))
        every { dao.observeCompletionsForDate(wednesday) } returns MutableStateFlow(emptyList())
        every { dao.observeActiveScheduleRevisionsThrough(wednesday) } returns MutableStateFlow(listOf(
            HabitScheduleRevisionEntity(habitId = 1, effectiveFrom = monday.plusDays(1),
                targetDays = setOf(DayOfWeek.WEDNESDAY), isEveryDay = false)))
        val result = HabitRepository(dao).observeForDate(wednesday).first().single()
        assertEquals(habit, result.habit)
        assertEquals(wednesday, result.date)
        assertFalse(result.completed)
    }
}
