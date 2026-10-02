package com.safarparmar.app.feature.habits

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class HabitSyncInstrumentedTest {
    @Test fun twoDevicesMergeIndependentEditsAndFreshInstallRestoresSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = UUID.randomUUID().toString()
        val store = SafarDataStore(context)
        store.setUserId(owner)
        store.setLoggedIn(true)
        val first = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val second = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val fresh = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val api = FakeHabitApi()
            val schedulerA = HabitReminderScheduler(context, first.habitDao(), store)
            val schedulerB = HabitReminderScheduler(context, second.habitDao(), store)
            val engineA = HabitSyncEngine(first.habitDao(), first, api, store, schedulerA)
            val engineB = HabitSyncEngine(second.habitDao(), second, api, store, schedulerB)
            HabitRepository(first.habitDao(), first, store).createHabit("Read",
                setOf(DayOfWeek.MONDAY), false, 0)
            assertTrue(engineA.sync())
            assertTrue(engineB.sync())
            val habitA = first.habitDao().getOwnedHabits(owner).single()
            val habitB = second.habitDao().getOwnedHabits(owner).single()
            assertEquals(habitA.remoteId, habitB.remoteId)

            HabitRepository(first.habitDao(), first, store).editHabit(habitA.copy(name = "Study"))
            HabitRepository(second.habitDao(), second, store).editHabit(habitB.copy(reminderTime = "08:30"))
            assertTrue(engineA.sync())
            assertTrue(engineB.sync())
            assertTrue(engineA.sync())
            assertEquals("Study", first.habitDao().getOwnedHabits(owner).single().name)
            assertEquals("08:30", first.habitDao().getOwnedHabits(owner).single().reminderTime)
            assertEquals("Study", second.habitDao().getOwnedHabits(owner).single().name)

            val engineFresh = HabitSyncEngine(fresh.habitDao(), fresh, api, store,
                HabitReminderScheduler(context, fresh.habitDao(), store))
            assertTrue(engineFresh.sync())
            assertEquals("Study", fresh.habitDao().getOwnedHabits(owner).single().name)
            assertEquals("08:30", fresh.habitDao().getOwnedHabits(owner).single().reminderTime)
        } finally {
            first.close()
            second.close()
            fresh.close()
            store.setLoggedIn(false)
            store.setUserId(null)
        }
    }

    @Test fun accountScopedQueriesNeverReturnAnotherStudentsHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val dao = db.habitDao()
            val day = LocalDate.of(2026, 9, 21)
            val aliceId = dao.insertHabit(HabitEntity(name = "Alice", ownerUserId = "alice",
                remoteId = UUID.randomUUID().toString(), scheduledSince = day))
            val bobId = dao.insertHabit(HabitEntity(name = "Bob", ownerUserId = "bob",
                remoteId = UUID.randomUUID().toString(), scheduledSince = day))
            dao.upsertCompletion(HabitCompletionEntity(aliceId, day, true))
            dao.upsertCompletion(HabitCompletionEntity(bobId, day, true))
            dao.upsertScheduleRevision(HabitScheduleRevisionEntity(habitId = aliceId,
                effectiveFrom = day, targetDays = DayOfWeek.entries.toSet(), isEveryDay = true))
            dao.upsertScheduleRevision(HabitScheduleRevisionEntity(habitId = bobId,
                effectiveFrom = day, targetDays = DayOfWeek.entries.toSet(), isEveryDay = true))
            assertEquals(listOf("Alice"), dao.observeActiveHabitsForOwner("alice").first().map { it.name })
            assertEquals(listOf(aliceId), dao.observeCompletionsForDateForOwner("alice", day).first().map { it.habitId })
            assertEquals(listOf(aliceId), dao.observeActiveScheduleRevisionsThroughForOwner("alice", day).first().map { it.habitId })
            assertEquals(listOf("Bob"), dao.observeActiveHabitsForOwner("bob").first().map { it.name })
        } finally {
            db.close()
        }
    }

    @Test fun legacyHistoryUploadsRetriesAndRestoresWithoutChangingLocalIdentity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = UUID.randomUUID().toString()
        val store = SafarDataStore(context)
        store.setUserId(owner)
        store.setLoggedIn(true)
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val dao = db.habitDao()
            val monday = LocalDate.of(2026, 9, 21)
            val localId = dao.insertHabit(HabitEntity(name = "Read", scheduledSince = monday,
                targetDays = setOf(DayOfWeek.MONDAY), isEveryDay = false))
            dao.upsertScheduleRevision(HabitScheduleRevisionEntity(habitId = localId,
                effectiveFrom = monday, targetDays = setOf(DayOfWeek.MONDAY), isEveryDay = false))
            dao.upsertCompletion(HabitCompletionEntity(localId, monday, true))
            val api = FakeHabitApi()
            val engine = HabitSyncEngine(dao, db, api, store, HabitReminderScheduler(context, dao, store))

            engine.activateCurrentAccount()
            assertEquals(3, dao.pendingMutationCount(owner))
            api.failNext = true
            try {
                engine.sync()
                fail("Expected simulated offline failure")
            } catch (_: IOException) { }
            assertEquals(3, dao.pendingMutationCount(owner))

            assertTrue(engine.sync())
            assertEquals(0, dao.pendingMutationCount(owner))
            val restored = dao.getOwnedHabits(owner).single()
            assertEquals(localId, restored.id)
            assertEquals("Read", restored.name)
            assertEquals(1, dao.getAllCompletionsForHabit(localId).size)

            api.record = api.record!!.copy(name = "Study")
            api.version++
            assertTrue(engine.sync())
            assertEquals(localId, dao.getOwnedHabits(owner).single().id)
            assertEquals("Study", dao.getOwnedHabits(owner).single().name)
        } finally {
            db.close()
            store.setLoggedIn(false)
            store.setUserId(null)
        }
    }

    private class FakeHabitApi : HabitApi {
        var failNext = false
        var version = 0L
        var record: HabitRemoteRecord? = null

        override suspend fun mutate(owner: String, request: HabitBatchRequest): HabitBatchResponse {
            if (failNext) {
                failNext = false
                throw IOException("offline")
            }
            for (mutation in request.mutations) {
                when (mutation.get("type").asString) {
                    "create" -> record = HabitRemoteRecord(
                        id = mutation.get("habitId").asString,
                        name = mutation.get("name").asString,
                        targetDays = mutation.getAsJsonArray("targetDays").map { it.asInt },
                        isEveryDay = mutation.get("isEveryDay").asBoolean,
                        isActive = mutation.get("isActive").asBoolean,
                        scheduledSince = mutation.get("scheduledSince").asString,
                        order = mutation.get("order").asInt,
                        reminderTime = null,
                        archivedAt = null,
                        deleted = false,
                        revisions = emptyList(),
                        completions = emptyList()
                    )
                    "schedule" -> record = record!!.copy(revisions = record!!.revisions + HabitRemoteRevision(
                        mutation.get("effectiveFrom").asString,
                        mutation.getAsJsonArray("targetDays").map { it.asInt },
                        mutation.get("isEveryDay").asBoolean
                    ))
                    "completion" -> record = record!!.copy(completions = record!!.completions + HabitRemoteCompletion(
                        mutation.get("date").asString, mutation.get("completed").asBoolean))
                    "patch" -> {
                        val fields = mutation.getAsJsonObject("fields")
                        record = record!!.copy(
                            name = fields.get("name")?.asString ?: record!!.name,
                            reminderTime = if (fields.has("reminderTime")) fields.get("reminderTime").takeUnless { it.isJsonNull }?.asString
                                else record!!.reminderTime
                        )
                    }
                }
            }
            version++
            return HabitBatchResponse(version, request.mutations.map { it.get("mutationId").asString })
        }

        override suspend fun snapshot(owner: String, knownVersion: Long?, cursor: String?, limit: Int): HabitSnapshotResponse =
            if (knownVersion == version) HabitSnapshotResponse(version, emptyList(), null, true)
            else HabitSnapshotResponse(version, listOfNotNull(record), null, false)
    }
}
