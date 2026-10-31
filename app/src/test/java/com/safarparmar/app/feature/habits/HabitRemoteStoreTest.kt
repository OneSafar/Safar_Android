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

class HabitRemoteStoreTest {
    private class Device(api: HabitApi, initialOwner: String = "alice", reminders: HabitReminderScheduler? = null, pendingStore: HabitPendingStore? = null, icons: HabitIconStore? = null) {
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
        val source = HabitRemoteStore(api, accounts, db, reminders, pendingStore, icons)
        val repository = HabitRepository(source)
    }
    private class Backend : HabitApi {
        val records = mutableMapOf<String, MutableMap<String, HabitRemoteRecord>>()
        val receipts = mutableSetOf<String>()
        val requests = mutableListOf<HabitBatchRequest>()
        var version = 0L
        var fail = false
        var loseResponse = false
        var returnChanges = false
        var hold: CompletableDeferred<Unit>? = null
        var requested: CompletableDeferred<Unit>? = null
        override suspend fun mutate(owner: String, request: HabitBatchRequest): HabitBatchResponse {
            requests += request
            val previousVersion = version
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
            return HabitBatchResponse(version, request.mutations.map { it.get("mutationId").asString },
                previousVersion = previousVersion,
                habits = if (returnChanges) request.mutations.map { values.getValue(it.get("habitId").asString) }
                    .map { it.copy(name = it.name.uppercase()) } else null)
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
        catch (e: HabitSaveException) { assertFalse(e.uncertain) }
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
    @Test fun `reminders receive confirmed creates edits completions archives and deletes`() = runTest {
        val backend = Backend()
        val reminders = mockk<HabitReminderScheduler>(relaxed = true)
        val device = Device(backend, reminders = reminders)
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0, "08:30")
        val habit = device.repository.observeHabits().first().single()
        assertEquals("08:30", habit.reminderTime)
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.reminderTime == "08:30" }) }
        device.repository.editHabit(habit.copy(reminderTime = "09:45"))
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.reminderTime == "09:45" }) }
        device.repository.toggle(habit.id, LocalDate.now(), false)
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.completions?.any { c -> c.completed } == true }) }
        device.repository.archiveHabit(habit.id)
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.archivedAt != null }) }
        device.repository.deleteHabit(habit)
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.deleted == true }) }
    }

    @Test fun `unconfirmed network mutation never replaces reminder projection`() = runTest {
        val backend = Backend()
        val reminders = mockk<HabitReminderScheduler>(relaxed = true)
        val device = Device(backend, reminders = reminders)
        device.source.refresh()
        clearMocks(reminders)
        backend.fail = true
        try { device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0, "08:30"); fail("Must report save error") }
        catch (_: IOException) { }
        coVerify(exactly = 0) { reminders.replace(any(), match { it.isNotEmpty() }) }
    }

    @Test fun `unchanged server snapshot does not rewrite reminder projection`() = runTest {
        val backend = Backend()
        val reminders = mockk<HabitReminderScheduler>(relaxed = true)
        val device = Device(backend, reminders = reminders)
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0, "08:30")
        coVerify(timeout = 1_000) { reminders.replace("alice", match { it.singleOrNull()?.reminderTime == "08:30" }) }
        clearMocks(reminders)
        device.source.refresh()
        coVerify(exactly = 0) { reminders.replace(any(), match { it.isNotEmpty() }) }
    }

    @Test fun `pending mutations survive store recreation and replay for their original account`() = runTest {
        // SharedPreferences is the durable Android boundary; recreate both stores and devices.
        val disk = mutableMapOf<String, String>()
        val prefs = mockk<android.content.SharedPreferences>()
        val editor = mockk<android.content.SharedPreferences.Editor>()
        every { prefs.all } answers { disk.toMap() }
        every { prefs.getString(any(), any()) } answers { disk[firstArg()] ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { disk[firstArg()] = secondArg(); editor }
        every { editor.remove(any()) } answers { disk.remove(firstArg<String>()); editor }
        every { editor.commit() } returns true
        val context = mockk<android.content.Context> {
            every { getSharedPreferences(any(), any()) } returns prefs
        }
        val backend = Backend()
        val first = Device(backend, pendingStore = HabitPendingStore(context))
        first.source.refresh()
        backend.loseResponse = true
        assertTrue(runCatching { first.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0) }.isFailure)
        assertEquals(1, disk.size)
        val pendingId = backend.requests.single().mutations.single().get("mutationId")
        val bob = Device(backend, initialOwner = "bob", pendingStore = HabitPendingStore(context))
        bob.source.refresh()
        assertEquals(1, backend.requests.size)
        val restarted = Device(backend, pendingStore = HabitPendingStore(context))
        restarted.source.refresh()
        assertEquals(pendingId, backend.requests.last().mutations.single().get("mutationId"))
        assertEquals(1, backend.records["alice"]!!.size)
        assertTrue(disk.isEmpty())
        assertEquals("Read", restarted.repository.observeHabits().first().single().name)
    }

    @Test fun `save proceeds while a background snapshot is blocked`() = runTest {
        val backend = Backend()
        val device = Device(backend)
        device.source.refresh()
        backend.hold = CompletableDeferred()
        backend.requested = CompletableDeferred()
        val refresh = async { device.source.refresh() }
        backend.requested!!.await()
        try {
            device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0)
            assertFalse(refresh.isCompleted)
            assertEquals("Read", device.repository.observeHabits().first().single().name)
        } finally { backend.hold!!.complete(Unit) }
        refresh.await()
    }

    @Test fun `custom emoji remains associated with the stable create identity`() = runTest {
        val backend = Backend()
        val icons = mockk<HabitIconStore>(relaxed = true)
        val device = Device(backend, icons = icons)
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0,
            emoji = "📖", operationId = "stable-form-id")
        val habit = device.repository.observeHabits().first().single()
        assertEquals("stable-form-id", habit.remoteId)
        coVerify { icons.save("alice", "stable-form-id", "📖") }
        device.repository.editHabit(habit.copy(name = "Study"), emoji = "🎯")
        coVerify { icons.save("alice", "stable-form-id", "🎯") }
        assertEquals("Study", device.repository.observeHabits().first().single().name)
    }

    @Test fun `server changed records win and do not skip concurrent phone changes`() = runTest {
        val backend = Backend().apply { returnChanges = true }
        val device = Device(backend)
        device.source.refresh()
        device.repository.createHabit("Read", DayOfWeek.entries.toSet(), true, 0)
        val habit = device.repository.observeHabits().first().single()
        assertEquals("READ", habit.name)
        // A second device's change is outside the next POST's changed-record response.
        backend.records["alice"]!!["other-phone"] = backend.records["alice"]!!.getValue(habit.remoteId!!)
            .copy(id = "other-phone", name = "Other phone", order = 1)
        backend.version++
        device.repository.editHabit(habit.copy(name = "Study"))
        assertEquals("STUDY", device.repository.observeHabits().first().single().name)
        device.source.refresh()
        assertEquals(setOf("Study", "Other phone"), device.repository.observeHabits().first().map { it.name }.toSet())
    }

}
