package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.*
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate

/** Controlled reproductions for the 27 October 2026 latency audit. No real network or accounts. */
class HabitLatencyAuditTest {
    private class Device(api: HabitApi, initialOwner: String = "alice", reminders: HabitReminderScheduler? = null) {
        val owner = MutableStateFlow<String?>(initialOwner)
        val loggedIn = MutableStateFlow(true)
        val accounts = mockk<SafarDataStore> {
            every { userId } returns owner
            every { isLoggedIn } returns loggedIn
        }
        val dao = mockk<HabitDao>(relaxed = true) {
            coEvery { getLegacyHabits(any()) } returns emptyList()
            coEvery { pendingMutationCount(any()) } returns 0
        }
        val db = mockk<HabitDatabase> { every { habitDao() } returns dao }
        val source = HabitRemoteStore(api, accounts, db, reminders)
        val repository = HabitRepository(source)
    }
    private class Backend : HabitApi {
        val records = mutableMapOf<String, MutableMap<String, HabitRemoteRecord>>()
        val receipts = mutableSetOf<String>()
        val requests = mutableListOf<HabitBatchRequest>()
        var version = 0L
        var fail = false
        var loseResponse = false
        var failSnapshot = false
        var hold: CompletableDeferred<Unit>? = null
        var requested: CompletableDeferred<Unit>? = null
        override suspend fun mutate(owner: String, request: HabitBatchRequest): HabitBatchResponse {
            requests += request
            if (fail) throw IOException("offline")
            val values = records.getOrPut(owner) { mutableMapOf() }
            for (m in request.mutations) {
                val receipt = "$owner:${m.get("mutationId").asString}"
                if (!receipts.add(receipt)) continue
                val id = m.get("habitId").asString
                val old = values[id]
                fun days() = m.getAsJsonArray("targetDays").map { it.asInt }
                when (m.get("type").asString) {
                    "create" -> if (old == null) values[id] = HabitRemoteRecord(id = id, name = m.get("name").asString,
                        targetDays = days(), isEveryDay = m.get("isEveryDay").asBoolean,
                        scheduledSince = m.get("scheduledSince").asString, order = m.get("order").asInt,
                        reminderTime = m.get("reminderTime")?.takeUnless { it.isJsonNull }?.asString,
                        revisions = listOf(HabitRemoteRevision(m.get("scheduledSince").asString, days(), m.get("isEveryDay").asBoolean)))
                    "patch" -> {
                        val f = m.getAsJsonObject("fields")
                        values[id] = old!!.copy(name = f.get("name")?.asString ?: old.name,
                            reminderTime = if (f.has("reminderTime")) f.get("reminderTime").takeUnless { it.isJsonNull }?.asString else old.reminderTime)
                    }
                    "schedule" -> {
                        val r = HabitRemoteRevision(m.get("effectiveFrom").asString, days(), m.get("isEveryDay").asBoolean)
                        values[id] = old!!.copy(revisions = old.revisions.filter { it.effectiveFrom != r.effectiveFrom } + r)
                    }
                    "completion" -> {
                        val c = HabitRemoteCompletion(m.get("date").asString, m.get("completed").asBoolean)
                        values[id] = old!!.copy(completions = old.completions.filter { it.date != c.date } + c)
                    }
                    "archive" -> values[id] = old!!.copy(archivedAt = m.get("archivedAt").asString)
                    "delete" -> values[id] = old!!.copy(deleted = true, completions = emptyList(), revisions = emptyList())
                }
                version++
            }
            if (loseResponse) { loseResponse = false; throw IOException("response lost after commit") }
            return HabitBatchResponse(version, request.mutations.map { it.get("mutationId").asString })
        }
        override suspend fun snapshot(owner: String, knownVersion: Long?, cursor: String?, limit: Int): HabitSnapshotResponse {
            if (failSnapshot) throw IOException("snapshot unavailable after accepted write")
            requested?.complete(Unit)
            hold?.await()
            if (fail) throw IOException("offline")
            return HabitSnapshotResponse(version, if (knownVersion == version) emptyList() else records[owner]?.values?.toList().orEmpty(),
                unchanged = knownVersion == version)
        }
    }

    @Test fun `accepted create completes without waiting for a full snapshot`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        backend.hold = CompletableDeferred()
        backend.requested = CompletableDeferred()
        val save = async { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }
        save.await()
        assertFalse(backend.requested!!.isCompleted)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals("Read", device.repository.observeHabits().first().single().name)
        backend.hold!!.complete(Unit)
    }

    @Test fun `accepted create succeeds when a subsequent snapshot is unavailable`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        backend.failSnapshot = true
        val result = runCatching { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }
        assertTrue(result.isSuccess)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals("Read", device.repository.observeHabits().first().single().name)
    }

    @Test fun `accepted create does not wait for reminder side effects`() = runTest {
        val backend = Backend()
        val reminders = mockk<HabitReminderScheduler>(relaxed = true)
        val device = Device(backend, reminders = reminders)
        device.source.refresh()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { reminders.replace(any(), match { it.isNotEmpty() }) } coAnswers {
            entered.complete(Unit)
            release.await()
        }
        val save = async { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }
        save.await()
        entered.await()
        try {
            assertEquals(1, backend.records["alice"]!!.size)
            assertNull(backend.records["alice"]!!.values.single().reminderTime)
            assertTrue(save.isCompleted)
            assertEquals("Read", device.repository.observeHabits().first().single().name)
        } finally { release.complete(Unit) }
        save.await()
    }

    @Test fun `reminder exception cannot turn an accepted save into failure`() = runTest {
        val backend = Backend()
        val reminders = mockk<HabitReminderScheduler>(relaxed = true)
        val device = Device(backend, reminders = reminders)
        device.source.refresh()
        coEvery { reminders.replace(any(), match { it.isNotEmpty() }) } throws IllegalStateException("disk full")
        val result = runCatching { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }
        assertTrue(result.isSuccess)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals("Read", device.repository.observeHabits().first().single().name)
    }

    @Test fun `restored form identity prevents duplicate creates after process restart`() = runTest {
        val backend = Backend()
        val firstProcess = Device(backend)
        firstProcess.source.refresh()
        backend.loseResponse = true
        val operation = "c461fbf0-4327-4687-8aaa-f4698a7b2f2d"
        assertTrue(runCatching { firstProcess.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0, operationId = operation) }.isFailure)
        val nextProcess = Device(backend)
        nextProcess.source.refresh()
        nextProcess.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 1, operationId = operation)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals(backend.requests[0].mutations[0].get("habitId"), backend.requests[1].mutations[0].get("habitId"))
    }

    @Test fun `repeated taps from same unchanged frame do not duplicate completion writes`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0)
        val id = device.repository.observeHabits().first().single().id
        device.repository.toggle(id, LocalDate.now(), false)
        device.repository.toggle(id, LocalDate.now(), false)
        assertEquals(1, backend.requests.count { it.mutations.single().get("type").asString == "completion" })
        assertTrue(device.repository.observeForDate(LocalDate.now()).first().single().completed)
    }
}
