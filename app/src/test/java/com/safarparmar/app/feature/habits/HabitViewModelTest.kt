package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }
    @Test fun `history calculations run off the collecting thread and preserve results`() = runTest {
        val collectingThread = Thread.currentThread()
        val days = java.time.DayOfWeek.entries.toSet()
        val checkedDays = object : Set<java.time.DayOfWeek> by days {
            override fun contains(element: java.time.DayOfWeek): Boolean {
                assertNotSame("Schedule calculation must not block the collecting thread", collectingThread, Thread.currentThread())
                return days.contains(element)
            }
        }
        val today = LocalDate.now()
        val habit = HabitEntity(id = 1L, name = "Read", targetDays = checkedDays, scheduledSince = today.minusDays(1))
        val done = listOf(HabitCompletionEntity(1L, today.minusDays(1), true), HabitCompletionEntity(1L, today, true))
        val dao = mockk<HabitDao>()
        every { dao.observeActiveHabits() } returns MutableStateFlow(listOf(habit))
        every { dao.observeCompletionsForDate(today) } returns MutableStateFlow(listOf(done.last()))
        every { dao.observeCompletionsBetween(any(), any()) } returns MutableStateFlow(done)
        every { dao.observeActiveScheduleRevisionsThrough(any()) } returns MutableStateFlow(emptyList())
        coEvery { dao.getCompletionsForHabit(1L) } returns done
        coEvery { dao.getScheduleRevisionsForHabit(1L) } returns emptyList()
        val repo = HabitRepository(dao)

        assertTrue(repo.observeForDate(today).first().single().completed)
        assertEquals(mapOf(today.minusDays(1) to true, today to true),
            repo.observeForRange(today.minusDays(730), today).first().single().completionByDate)
        assertEquals(StreakInfo(current = 2, best = 2), repo.streaks(habit))
        assertSame("Caller resumes on its original thread", collectingThread, Thread.currentThread())
    }

    @Test fun `midnight updates current periods and preserves browsed history`() = runTest {
        run {
            val dao = mockk<HabitDao>(relaxed = true)
            every { dao.observeActiveHabits() } returns MutableStateFlow(emptyList())
            every { dao.observeCompletionsForDate(any()) } returns MutableStateFlow(emptyList())
            every { dao.observeCompletionsBetween(any(), any()) } returns MutableStateFlow(emptyList())
            every { dao.observeActiveScheduleRevisionsThrough(any()) } returns MutableStateFlow(emptyList())
            val vm = HabitViewModel(HabitRepository(dao))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            val last = LocalDate.of(2026, 5, 31) // Sunday, at both week and month boundary.
            vm.refreshToday(last)
            assertEquals(last, vm.uiState.first { !it.isLoading && it.selectedDate == last }.selectedDate)
            vm.refreshToday(last.plusDays(1))
            vm.uiState.first { it.selectedDate == last.plusDays(1) && it.weekStart == LocalDate.of(2026, 6, 1) && it.monthAnchor == LocalDate.of(2026, 6, 1) }
            assertEquals(LocalDate.of(2026, 6, 1), vm.uiState.value.weekStart)
            assertEquals(LocalDate.of(2026, 6, 1), vm.uiState.value.monthAnchor)
            vm.changeWeek(false)
            vm.changeMonth(false)
            val history = vm.uiState.first { it.weekStart == LocalDate.of(2026, 5, 25) && it.monthAnchor == LocalDate.of(2026, 5, 1) }
            vm.refreshToday(last.plusDays(2))
            vm.uiState.first { it.selectedDate == last.plusDays(2) }
            assertEquals(history.weekStart, vm.uiState.value.weekStart)
            assertEquals(history.monthAnchor, vm.uiState.value.monthAnchor)
            assertEquals(last.plusDays(2), vm.uiState.value.selectedDate)
        }
    }
    @Test fun `stale today tap never writes to yesterday`() = runTest {
        run {
            val dao = mockk<HabitDao>(relaxed = true)
            val vm = HabitViewModel(HabitRepository(dao))
            vm.toggleTodayHabit(1L, LocalDate.now().minusDays(1), false)
            coVerify(exactly = 0) { dao.upsertCompletion(any()) }
        }
    }
    @Test fun `create uses all habits for ordering and propagates a save failure`() = runTest {
        run {
            val dao = mockk<HabitDao>(relaxed = true)
            every { dao.observeActiveHabits() } returns MutableStateFlow(listOf(HabitEntity(id = 1, name = "Weekend", order = 8)))
            val vm = HabitViewModel(HabitRepository(dao))
            vm.createHabit("Read", java.time.DayOfWeek.entries.toSet(), isEveryDay = true)
            coVerify { dao.insertHabit(match { it.name == "Read" && it.order == 9 }) }
            coEvery { dao.insertHabit(any()) } throws IllegalStateException("disk unavailable")
            try { vm.createHabit("Run", java.time.DayOfWeek.entries.toSet(), isEveryDay = true); fail("Failure must reach the sheet") }
            catch (_: IllegalStateException) { }
        }
    }

    @Test fun `habit with specific target days appears only on scheduled days`() = runTest {
        val sundayHabit = HabitEntity(
            id = 10L,
            name = "Sunday Only Ritual",
            targetDays = setOf(java.time.DayOfWeek.SUNDAY),
            order = 0
        )
        val dao = mockk<HabitDao>(relaxed = true)
        every { dao.observeActiveHabits() } returns MutableStateFlow(listOf(sundayHabit))
        every { dao.observeCompletionsForDate(any()) } returns MutableStateFlow(emptyList())
        every { dao.observeActiveScheduleRevisionsThrough(any()) } returns MutableStateFlow(emptyList())

        val repo = HabitRepository(dao)

        // Monday (2026-09-14)
        val monday = LocalDate.of(2026, 9, 14)
        assertEquals(java.time.DayOfWeek.MONDAY, monday.dayOfWeek)
        val mondayHabits = repo.observeForDate(monday).first()
        assertTrue("Sunday habit must not appear on Monday", mondayHabits.isEmpty())

        // Tuesday (2026-09-15)
        val tuesday = LocalDate.of(2026, 9, 15)
        val tuesdayHabits = repo.observeForDate(tuesday).first()
        assertTrue("Sunday habit must not appear on Tuesday", tuesdayHabits.isEmpty())

        // Sunday (2026-09-20)
        val sunday = LocalDate.of(2026, 9, 20)
        assertEquals(java.time.DayOfWeek.SUNDAY, sunday.dayOfWeek)
        val sundayHabits = repo.observeForDate(sunday).first()
        assertEquals("Sunday habit must appear on Sunday", 1, sundayHabits.size)
        assertEquals(10L, sundayHabits.first().habit.id)
        assertEquals("Sunday Only Ritual", sundayHabits.first().habit.name)
    }

    @Test fun `toggleTodayHabit marks today habit complete`() = runTest {
        val today = LocalDate.now()
        val habit = HabitEntity(
            id = 1L,
            name = "Read",
            targetDays = setOf(today.dayOfWeek),
            order = 0
        )
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.getHabitById(1L) } returns habit
        every { dao.observeActiveHabits() } returns MutableStateFlow(listOf(habit))
        val vm = HabitViewModel(HabitRepository(dao))
        vm.toggleTodayHabit(1L, today, false)
        coVerify { dao.upsertCompletion(HabitCompletionEntity(1L, today, completed = true)) }
    }
}

