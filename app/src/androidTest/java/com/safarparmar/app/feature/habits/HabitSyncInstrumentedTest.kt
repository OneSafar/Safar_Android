package com.safarparmar.app.feature.habits

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitSyncInstrumentedTest {
    @Test fun legacyImportSurvivesLostResponseRestoresFreshPhoneAndRemovesLocalHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = SafarDataStore(context)
        val owner = UUID.randomUUID().toString()
        accounts.setUserId(owner)
        accounts.setLoggedIn(true)
        val legacy = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val fresh = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val dao = legacy.habitDao()
            val start = LocalDate.now().minusDays(110)
            val id = dao.insertHabit(HabitEntity(name = "Read", scheduledSince = start,
                targetDays = DayOfWeek.entries.toSet(), isEveryDay = true))
            dao.upsertScheduleRevision(HabitScheduleRevisionEntity(habitId = id, effectiveFrom = start,
                targetDays = DayOfWeek.entries.toSet(), isEveryDay = true))
            repeat(105) { dao.upsertCompletion(HabitCompletionEntity(id, start.plusDays(it.toLong()), true)) }
            val api = Backend()
            val first = HabitRemoteStore(api, accounts, legacy)
            api.loseResponse = true
            try { first.refresh(); fail("Must retain legacy data on unconfirmed import") }
            catch (_: IOException) { }
            assertEquals(107, dao.pendingMutationCount(owner))
            assertEquals(1, dao.getOwnedHabits(owner).size)
            // Simulate restarting the app with no in-memory operation state.
            val restarted = HabitRemoteStore(api, accounts, legacy)
            restarted.refresh()
            assertEquals(0, dao.pendingMutationCount(owner))
            assertTrue(dao.getOwnedHabits(owner).isEmpty())
            assertTrue(dao.getAllCompletionsForHabit(id).isEmpty())
            assertEquals(1, api.records.size)
            val second = HabitRemoteStore(api, accounts, fresh)
            second.refresh()
            val restored = HabitRepository(second).getHabitsWithCompletionsForRange(start, LocalDate.now()).single()
            assertEquals("Read", restored.habit.name)
            assertEquals(105, restored.completionByDate.values.count { it })
            assertTrue(fresh.habitDao().getOwnedHabits(owner).isEmpty())
        } finally {
            legacy.close(); fresh.close()
            accounts.setLoggedIn(false); accounts.setUserId(null)
        }
    }

    @Test fun importedRemoteCopyCannotOverwriteNewerServerEditsOrResurrectDeletion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = SafarDataStore(context)
        val owner = UUID.randomUUID().toString()
        accounts.setUserId(owner); accounts.setLoggedIn(true)
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        try {
            val api = Backend()
            val remoteId = UUID.randomUUID().toString()
            val erasedId = UUID.randomUUID().toString()
            api.records[remoteId] = HabitRemoteRecord(id = remoteId, name = "Server name",
                scheduledSince = LocalDate.now().toString(), targetDays = listOf(1), isEveryDay = false)
            api.records[erasedId] = HabitRemoteRecord(id = erasedId, deleted = true)
            db.habitDao().insertHabit(HabitEntity(name = "Old name", ownerUserId = owner, remoteId = remoteId))
            db.habitDao().insertHabit(HabitEntity(name = "Erased", ownerUserId = owner, remoteId = erasedId))
            val source = HabitRemoteStore(api, accounts, db)
            source.refresh()
            assertEquals("Server name", source.observeActiveHabits().first().single().name)
            assertTrue(api.receipts.isEmpty())
            assertTrue(db.habitDao().getOwnedHabits(owner).isEmpty())
        } finally {
            db.close(); accounts.setLoggedIn(false); accounts.setUserId(null)
        }
    }

    private class Backend : HabitApi {
        val records = mutableMapOf<String, HabitRemoteRecord>()
        val receipts = mutableSetOf<String>()
        var version = 0L
        var loseResponse = false
        override suspend fun mutate(owner: String, request: HabitBatchRequest): HabitBatchResponse {
            for (m in request.mutations) {
                if (!receipts.add(m.get("mutationId").asString)) continue
                val id = m.get("habitId").asString
                when (m.get("type").asString) {
                    "create" -> if (records[id] == null) records[id] = HabitRemoteRecord(id = id, name = m.get("name").asString,
                        scheduledSince = m.get("scheduledSince").asString,
                        targetDays = m.getAsJsonArray("targetDays").map { it.asInt }, isEveryDay = m.get("isEveryDay").asBoolean)
                    "schedule" -> records[id] = records[id]!!.copy(revisions = records[id]!!.revisions + HabitRemoteRevision(
                        m.get("effectiveFrom").asString, m.getAsJsonArray("targetDays").map { it.asInt }, m.get("isEveryDay").asBoolean))
                    "completion" -> {
                        val c = HabitRemoteCompletion(m.get("date").asString, m.get("completed").asBoolean)
                        records[id] = records[id]!!.copy(completions = records[id]!!.completions.filter { it.date != c.date } + c)
                    }
                    "archive" -> records[id] = records[id]!!.copy(archivedAt = m.get("archivedAt").asString)
                }
                version++
            }
            if (loseResponse) { loseResponse = false; throw IOException("Response lost after commit") }
            return HabitBatchResponse(version, request.mutations.map { it.get("mutationId").asString })
        }
        override suspend fun snapshot(owner: String, knownVersion: Long?, cursor: String?, limit: Int) =
            HabitSnapshotResponse(version, records.values.toList())
    }
}
