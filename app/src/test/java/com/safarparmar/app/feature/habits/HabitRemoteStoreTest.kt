package com.safarparmar.app.feature.habits

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

class HabitRemoteStoreTest {
    private class Device(api: HabitApi, initialOwner: String = "alice") {
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
        val source = HabitRemoteStore(api, accounts, db)
        val repository = HabitRepository(source)
    }
    private class Backend : HabitApi {
        val records = mutableMapOf<String, MutableMap<String, HabitRemoteRecord>>()
        val receipts = mutableSetOf<String>()
        val requests = mutableListOf<HabitBatchRequest>()
        var version = 0L
        var fail = false
        var loseResponse = false
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
            requested?.complete(Unit)
            hold?.await()
            if (fail) throw IOException("offline")
            return HabitSnapshotResponse(version, if (knownVersion == version) emptyList() else records[owner]?.values?.toList().orEmpty(),
                unchanged = knownVersion == version)
        }
    }

    @Test fun `two phones and fresh install share all backend history without writing Room`() = runTest {
        val backend = Backend()
        val a = Device(backend)
        val b = Device(backend)
        a.source.refresh()
        b.source.refresh()
        a.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0)
        val habitA = a.repository.observeHabits().first().single()
        a.repository.toggle(habitA.id, LocalDate.now(), false)
        b.source.refresh()
        val habitB = b.repository.observeHabits().first().single()
        assertEquals(habitA.remoteId, habitB.remoteId)
        assertTrue(b.repository.observeForDate(LocalDate.now()).first().single().completed)
        a.repository.editHabit(habitA.copy(name = "Study"))
        b.repository.editHabit(habitB.copy(reminderTime = "08:30"))
        a.source.refresh()
        val merged = a.repository.observeHabits().first().single()
        assertEquals("Study", merged.name)
        assertEquals("08:30", merged.reminderTime)
        val fresh = Device(backend)
        fresh.source.refresh()
        assertEquals("Study", fresh.repository.observeHabits().first().single().name)
        assertTrue(fresh.repository.observeForDate(LocalDate.now()).first().single().completed)
        a.repository.archiveHabit(merged.id)
        b.source.refresh()
        assertTrue(b.repository.observeHabits().first().isEmpty())
        assertTrue(b.repository.getHabitsWithCompletionsForRange(LocalDate.now(), LocalDate.now()).single().completionByDate[LocalDate.now()]!!)
        a.repository.deleteHabit(merged)
        fresh.source.refresh()
        assertTrue(fresh.repository.getHabitsWithCompletionsForRange(LocalDate.now(), LocalDate.now()).isEmpty())
        for (device in listOf(a, b, fresh)) {
            coVerify(exactly = 0) { device.dao.insertHabit(any()) }
            coVerify(exactly = 0) { device.dao.upsertCompletion(any()) }
            coVerify(exactly = 0) { device.dao.enqueueMutation(any()) }
        }
    }

    @Test fun `offline creation does not pretend to save and retry cannot duplicate a committed habit`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        backend.loseResponse = true
        try { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0); fail("Must report unconfirmed save") }
        catch (_: IOException) { }
        assertTrue(device.repository.observeHabits().first().isEmpty())
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals(backend.requests[0].mutations[0].get("mutationId"), backend.requests[1].mutations[0].get("mutationId"))
        assertEquals("Read", device.repository.observeHabits().first().single().name)
        assertTrue(backend.requests[0].mutations[0].has("reminderTime"))
        assertTrue(backend.requests[0].mutations[0].get("reminderTime").isJsonNull)
    }

    @Test fun `background refresh after a lost create response cannot make the retry create a second habit`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        backend.loseResponse = true
        try { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }
        catch (_: IOException) { }
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 1)
        assertEquals(1, backend.records["alice"]!!.size)
        assertEquals(backend.requests[0].mutations[0].get("habitId"), backend.requests[1].mutations[0].get("habitId"))
    }

    @Test fun `logout and account change hide old history and reject late responses`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        device.repository.createHabit("Alice habit", DayOfWeek.entries.toSet(), true, 0)
        val alice = device.repository.observeHabits().first().single()
        backend.hold = CompletableDeferred()
        backend.requested = CompletableDeferred()
        val inFlight = async { runCatching { device.source.refresh() } }
        backend.requested!!.await()
        device.owner.value = "bob"
        assertTrue(device.repository.observeHabits().first().isEmpty())
        backend.hold!!.complete(Unit)
        assertTrue(inFlight.await().isFailure)
        backend.hold = null
        device.source.refresh()
        assertTrue(device.repository.observeHabits().first().isEmpty())
        try { device.repository.editHabit(alice.copy(name = "wrong owner")); fail("Must reject another account's habit") }
        catch (_: IllegalStateException) { }
        device.loggedIn.value = false
        assertTrue(device.repository.observeHabits().first().isEmpty())
        assertFalse(device.source.syncStatus.first().loading)
    }

    @Test fun `server snake case contract retains schedule archive and completion fields`() {
        val raw = """{"version":2,"habits":[{"id":"remote","name":"Read","target_days":[1],"is_every_day":false,"is_active":true,"scheduled_since":"2026-09-21","order":0,"reminder_time":"07:00","archived_at":"2026-09-28","deleted":false,"revisions":[{"effective_from":"2026-09-21","target_days":[1],"is_every_day":false}],"completions":[{"date":"2026-09-21","completed":true}]}],"nextCursor":null,"unchanged":false}"""
        val record = Gson().fromJson(raw, HabitSnapshotResponse::class.java).habits.single()
        assertEquals("2026-09-21", record.scheduledSince)
        assertEquals("2026-09-28", record.archivedAt)
        assertEquals("07:00", record.reminderTime)
        assertEquals(listOf(1), record.revisions.single().targetDays)
        assertTrue(record.completions.single().completed)
    }
}
