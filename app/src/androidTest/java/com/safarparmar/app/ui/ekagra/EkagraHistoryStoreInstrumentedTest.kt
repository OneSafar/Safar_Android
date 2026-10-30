package com.safarparmar.app.ui.ekagra

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class EkagraHistoryStoreInstrumentedTest {
    private lateinit var db: EkagraHistoryDatabase
    private var diskDatabaseName: String? = null
    private lateinit var owner: MutableStateFlow<String?>
    private lateinit var store: EkagraHistoryStore
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val data = EkagraAnalyticsStats(totalFocusMinutes = 123, totalSessions = 2,
        focusStreak = 9, weeklyData = listOf(0, 1, 2, 3, 4, 5, 6),
        focusSessions = listOf(EkagraAnalyticsFocusSession(id = "server-1", sourceSessionId = "local-1",
            taskText = "Physics", actualSeconds = 7381, actualMinutes = 123, associatedGoalId = "goal-1", isGoalLinked = true)))

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, EkagraHistoryDatabase::class.java).build()
        owner = MutableStateFlow("student-a")
        store = EkagraHistoryStore(db, owner)
        runBlocking { store.activate(owner.value) }
    }
    @After fun close() { db.close(); diskDatabaseName?.let { context.deleteDatabase(it) } }
    private suspend fun save(value: EkagraAnalyticsStats = data) = store.commit(requireNotNull(store.beginRefresh("student-a")), value)

    @Test fun cachedServerHistorySurvivesColdOfflineOpenWithoutRecalculatingTotals() = runBlocking {
        db.close()
        val name = "history-cache-qa-${UUID.randomUUID()}.db"
        diskDatabaseName = name
        db = Room.databaseBuilder(context, EkagraHistoryDatabase::class.java, name).build()
        store = EkagraHistoryStore(db, owner)
        store.activate("student-a")
        assertEquals(HistoryCommit.STORED, save())
        db.close()
        db = Room.databaseBuilder(context, EkagraHistoryDatabase::class.java, name).build()
        val restarted = EkagraHistoryStore(db, owner)
        restarted.activate("student-a")
        restarted.failed(requireNotNull(restarted.beginRefresh("student-a")))
        assertEquals(data, restarted.state.value.data)
        assertTrue(restarted.state.value.offline)
        assertNotNull(restarted.state.value.lastRefreshAtMs)
    }

    @Test fun newerRefreshAndConfirmedEditRejectOldNetworkResponses() = runBlocking {
        val old = requireNotNull(store.beginRefresh("student-a"))
        assertEquals(HistoryCommit.STORED, save())
        assertEquals(HistoryCommit.REJECTED, store.commit(old, EkagraAnalyticsStats()))
        val beforeEdit = requireNotNull(store.beginRefresh("student-a"))
        store.confirmedEdit("student-a", "local-1", "New title", "new-goal")
        assertEquals(HistoryCommit.REJECTED, store.commit(beforeEdit, data))
        val changed = store.state.value.data.focusSessions.single()
        assertEquals("New title", changed.taskText)
        assertEquals("new-goal", changed.associatedGoalId)
        assertEquals(7381, changed.actualSeconds)
        assertEquals(123, store.state.value.data.totalFocusMinutes)
        val restarted = EkagraHistoryStore(db, owner)
        assertEquals(changed, restarted.activate("student-a").data.focusSessions.single())
    }

    @Test fun deletedServerAndClientIdentitiesStaySuppressedAfterRestartAndDelayedServerEcho() = runBlocking {
        save()
        store.confirmedDeletion("student-a", "server-1")
        assertTrue(store.state.value.data.focusSessions.isEmpty())
        assertTrue("local-1" in store.state.value.suppressedSessionIds)
        val restarted = EkagraHistoryStore(db, owner)
        restarted.activate("student-a")
        restarted.commit(requireNotNull(restarted.beginRefresh("student-a")), data)
        assertTrue(restarted.state.value.data.focusSessions.isEmpty())
        assertTrue("local-1" in restarted.state.value.suppressedSessionIds)
    }

    @Test fun accountChangeAndLogoutNeverAcceptAnotherAccountsResponse() = runBlocking {
        save()
        val request = requireNotNull(store.beginRefresh("student-a"))
        owner.value = "student-b"
        assertEquals(HistoryCommit.REJECTED, store.commit(request, data))
        assertTrue(store.activate("student-b").data.focusSessions.isEmpty())
        owner.value = null
        assertNull(store.beginRefresh("student-b"))
        assertNull(store.activate(null).owner)
        owner.value = "student-a"
        assertEquals(data, store.activate("student-a").data)
    }

    @Test fun cacheWriteFailureRetainsPreviousSnapshotAndDoesNotClaimDurability() = runBlocking {
        save()
        val before = db.history().snapshot("student-a")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_cache BEFORE INSERT ON history_snapshots BEGIN SELECT RAISE(ABORT, 'disk full'); END")
        assertEquals(HistoryCommit.MEMORY_ONLY, save(data.copy(totalFocusMinutes = 999)))
        assertEquals(before, db.history().snapshot("student-a"))
        assertTrue(store.state.value.cacheError)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_cache")
        assertEquals(HistoryCommit.STORED, save())
    }

    @Test fun firstOfflineOpenAndValidEmptyRefreshAreDifferentStates() = runBlocking {
        store.failed(requireNotNull(store.beginRefresh("student-a")))
        assertNull(store.state.value.lastRefreshAtMs)
        assertTrue(store.state.value.offline)
        save()
        save(EkagraAnalyticsStats())
        assertEquals(EkagraAnalyticsStats(), EkagraHistoryStore(db, owner).activate("student-a").data)
    }

    @Test fun upgradingHistoryReadModelLeavesEveryJournalStateAndLegacyPreferencesByteForByteUnchanged() = runBlocking {
        val journalDb = Room.inMemoryDatabaseBuilder(context, EkagraJournalDatabase::class.java).build()
        val prefsName = "history-upgrade-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, 0)
        prefs.edit().putBoolean("done", true).putString("queue_json", "[{\"legacy\":true}]").commit()
        try {
            val rows = listOf("active", "pending", "acknowledged", "synced").mapIndexed { index, state ->
                val payload = "{\"clientSessionId\":\"local-$index\",\"actualDurationSeconds\":7381,\"goalId\":\"goal\",\"topicId\":\"topic\",\"unknownLegacyField\":true}"
                JournalSession("student-a", "local-$index", payload, state, 1234L, "old-process", if (index > 1) "server-$index" else null)
            } + JournalSession("student-a", "zero", "{\"actualDurationSeconds\":0}", "synced", 1, "old-process")
            rows.forEach { journalDb.journal().put(it) }
            val before = journalDb.journal().rows("student-a").sortedBy { it.id }
            val beforePrefs = prefs.all.toMap()
            save()
            store.confirmedEdit("student-a", "server-1", "Edited cache only")
            store.confirmedDeletion("student-a", "server-1")
            EkagraHistoryStore(db, owner).activate("student-a")
            assertEquals(before, journalDb.journal().rows("student-a").sortedBy { it.id })
            assertEquals(beforePrefs, prefs.all)
        } finally {
            journalDb.close()
            context.deleteSharedPreferences(prefsName)
        }
    }
}
