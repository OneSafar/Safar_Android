package com.safarparmar.app.feature.habits

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.*
import com.google.gson.JsonParser
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitAccountRepositoryTest {
    private fun loggedIn(owner: String = "alice") = mockk<SafarDataStore>().also {
        every { it.isLoggedIn } returns MutableStateFlow(true)
        every { it.userId } returns MutableStateFlow(owner)
    }

    @Test fun `habit lists hide another account and ownerless legacy rows`() = runTest {
        val dao = mockk<HabitDao>()
        every { dao.observeActiveHabitsForOwner("alice") } returns MutableStateFlow(listOf(
            HabitEntity(id = 1, name = "Mine", ownerUserId = "alice"),
            HabitEntity(id = 2, name = "Other", ownerUserId = "bob"),
            HabitEntity(id = 3, name = "Legacy")
        ))
        val names = HabitRepository(dao, dataStore = loggedIn()).observeHabits().first().map { it.name }
        assertEquals(listOf("Mine"), names)
    }

    @Test fun `a stale habit ID cannot mutate another account`() = runTest {
        val dao = mockk<HabitDao>()
        coEvery { dao.getHabitById(2) } returns HabitEntity(id = 2, name = "Other", ownerUserId = "bob")
        val result = HabitRepository(dao, dataStore = loggedIn()).toggle(2, LocalDate.now(), false)
        assertEquals(ToggleResult.HABIT_NOT_FOUND, result)
        coVerify(exactly = 0) { dao.upsertCompletion(any()) }
        coVerify(exactly = 0) { dao.enqueueMutation(any()) }
    }

    @Test fun `creation queues a durable backend mutation for the signed-in owner`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        coEvery { dao.insertHabit(any()) } returns 12
        val repo = HabitRepository(dao, dataStore = loggedIn())
        repo.createHabit(" Read ", setOf(DayOfWeek.MONDAY), false, 0)
        val habit = slot<HabitEntity>()
        val pending = slot<HabitPendingMutationEntity>()
        coVerify { dao.insertHabit(capture(habit)) }
        coVerify { dao.enqueueMutation(capture(pending)) }
        assertEquals("alice", habit.captured.ownerUserId)
        assertNotNull(habit.captured.remoteId)
        assertEquals("alice", pending.captured.ownerUserId)
        val json = JsonParser.parseString(pending.captured.payload).asJsonObject
        assertEquals(habit.captured.remoteId, json.get("habitId").asString)
        assertEquals("Read", json.get("name").asString)
        assertEquals("create", json.get("type").asString)
    }

    @Test fun `invalid reminder and order never enter the durable mutation queue`() = runTest {
        val dao = mockk<HabitDao>(relaxed = true)
        val repo = HabitRepository(dao, dataStore = loggedIn())
        try {
            repo.createHabit("Read", setOf(DayOfWeek.MONDAY), false, 0, "25:99")
            fail("Invalid reminder was accepted")
        } catch (_: IllegalArgumentException) { }
        try {
            repo.editHabit(HabitEntity(id = 1, name = "Read", order = 100_001))
            fail("Invalid order was accepted")
        } catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { dao.insertHabit(any()) }
        coVerify(exactly = 0) { dao.updateHabit(any()) }
        coVerify(exactly = 0) { dao.enqueueMutation(any()) }
    }
}
