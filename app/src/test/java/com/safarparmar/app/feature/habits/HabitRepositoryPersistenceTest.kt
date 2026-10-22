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

/** Repository reads a memory projection and delegates every change to the backend store. */
class HabitRepositoryPersistenceTest {
    @Test fun `active list follows the server projection`() = runTest {
        val store = mockk<HabitRemoteStore>()
        val habits = MutableStateFlow(listOf(HabitEntity(id = 1, name = "Read")))
        every { store.observeActiveHabits() } returns habits
        val repository = HabitRepository(store)
        assertEquals(listOf("Read"), repository.observeHabits().first().map { it.name })
        habits.value = listOf(HabitEntity(id = 2, name = "Run"))
        assertEquals(listOf("Run"), repository.observeHabits().first().map { it.name })
    }
    @Test fun `missing habit cannot create a completion`() = runTest {
        val store = mockk<HabitRemoteStore>(relaxed = true)
        coEvery { store.getHabitById(99) } returns null
        assertEquals(ToggleResult.HABIT_NOT_FOUND, HabitRepository(store).toggle(99, LocalDate.now(), false))
        coVerify(exactly = 0) { store.upsertCompletion(any()) }
    }
    @Test fun `create sends schedule and propagates backend failure`() = runTest {
        val store = mockk<HabitRemoteStore>(relaxed = true)
        val days = setOf(DayOfWeek.MONDAY)
        HabitRepository(store).createHabit("Read", days, false, 3)
        coVerify { store.insertHabit(match { it.name == "Read" && it.order == 3 && it.targetDays == days && !it.isEveryDay }) }
        coEvery { store.insertHabit(any()) } throws java.io.IOException("offline")
        try { HabitRepository(store).createHabit("Run", days, false, 4); fail("Must not report a local save") }
        catch (_: java.io.IOException) { }
    }
    @Test fun `edits retain ownership and reminder metadata`() = runTest {
        val store = mockk<HabitRemoteStore>(relaxed = true)
        val existing = HabitEntity(id = 1, name = "Read", ownerUserId = "alice", remoteId = "remote-id",
            reminderTime = "08:30", scheduledSince = LocalDate.of(2026, 1, 1))
        HabitRepository(store).editHabit(existing.copy(name = "Read a chapter"))
        coVerify { store.updateHabit(existing.copy(name = "Read a chapter")) }
    }
}
