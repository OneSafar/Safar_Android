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

/** Current habits are local; retained sync metadata must survive local edits. */
class HabitRepositoryPersistenceTest {
    @Test fun `active list follows the local DAO stream`() = runTest {
        val dao = mockk<HabitDao>()
        val habits = MutableStateFlow(listOf(HabitEntity(id = 1, name = "Read")))
        every { dao.observeActiveHabits() } returns habits
        val repository = HabitRepository(dao)
        assertEquals(listOf("Read"), repository.observeHabits().first().map { it.name })
        habits.value = listOf(HabitEntity(id = 2, name = "Run"))
        assertEquals(listOf("Run"), repository.observeHabits().first().map { it.name })
    }

    @Test fun `missing habit cannot create a completion`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.getHabitById(99) } returns null
        assertEquals(ToggleResult.HABIT_NOT_FOUND, HabitRepository(dao).toggle(99, LocalDate.now(), false))
        coVerify(exactly = 0) { dao.upsertCompletion(any()) }
        coVerify(exactly = 0) { dao.clearCompletion(any(), any()) }
    }

    @Test fun `creation persists the initial schedule under the inserted habit ID`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.insertHabit(any()) } returns 12
        val days = setOf(DayOfWeek.MONDAY)
        HabitRepository(dao).createHabit("Read", days, false, 3)
        val habit = slot<HabitEntity>()
        val revision = slot<HabitScheduleRevisionEntity>()
        coVerify { dao.insertHabit(capture(habit)) }
        coVerify { dao.upsertScheduleRevision(capture(revision)) }
        assertEquals("Read", habit.captured.name)
        assertEquals(3, habit.captured.order)
        assertEquals(12L, revision.captured.habitId)
        assertEquals(habit.captured.scheduledSince, revision.captured.effectiveFrom)
        assertEquals(days, revision.captured.targetDays)
        assertFalse(revision.captured.isEveryDay)
    }

    @Test fun `name edit preserves retained metadata and does not rewrite schedule history`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        val existing = HabitEntity(id = 1, name = "Read", ownerUserId = "alice", remoteId = "remote-id",
            reminderTime = "08:30", scheduledSince = LocalDate.of(2026, 1, 1))
        coEvery { dao.getHabitById(1) } returns existing
        val updated = existing.copy(name = "Read a chapter")
        HabitRepository(dao).editHabit(updated)
        coVerify { dao.updateHabit(updated) }
        coVerify(exactly = 0) { dao.upsertScheduleRevision(any()) }
    }
}
