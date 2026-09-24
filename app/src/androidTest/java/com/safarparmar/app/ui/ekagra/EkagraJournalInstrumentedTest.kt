package com.safarparmar.app.ui.ekagra

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class EkagraJournalInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: EkagraJournalDatabase
    private lateinit var owner: MutableStateFlow<String?>
    private lateinit var journal: EkagraSessionJournal
    @Before fun setup() {
        val prefix = "test-${UUID.randomUUID()}-"
        context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(prefix + name, mode)
        }
        database = Room.inMemoryDatabaseBuilder(context, EkagraJournalDatabase::class.java).build()
        owner = MutableStateFlow("student-a")
        journal = EkagraSessionJournal(context, database, owner, "process-a", false)
    }
    @After fun tearDown() { database.close() }
    private fun session(seconds: Int = 10800, id: String = "local-test") = PendingEkagraSessionSave(
        clientSessionId = id, mode = "Timer", startedAt = "2026-09-23T10:00:00Z", endedAt = "2026-09-23T13:00:00Z",
        plannedDurationMinutes = 300, actualDurationMinutes = seconds / 60, actualDurationSeconds = seconds,
        goalId = "goal", goalTitle = "Study", topicId = "topic", planId = "plan", taskTitle = "Physics", shieldEnabled = false,
    )

    @Test fun processDeathSavesCheckpointNotThePlannedFiveHours() = runBlocking {
        journal.checkpoint(session(), "student-a").await()
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        restarted.recover().await()
        val saved = restarted.pending().single()
        assertEquals(10800, saved.actualDurationSeconds)
        assertEquals("interrupted", saved.endReason)
        assertEquals("topic", saved.topicId)
        assertEquals("2026-09-23T13:00:00Z", saved.endedAt)
        restarted.recover().await()
        assertEquals(1, restarted.pending().size)
    }

    @Test fun sameProcessScreenRecreationDoesNotFinalize() = runBlocking {
        journal.checkpoint(session(), "student-a").await()
        val rebound = EkagraSessionJournal(context, database, owner, "process-a", false)
        rebound.recover().await()
        assertTrue(rebound.pending().isEmpty())
    }

    @Test fun completionAndEndRaceAndLateCheckpointProduceOneFinalRecord() = runBlocking {
        journal.checkpoint(session(), "student-a").await()
        val first = journal.enqueue(session(18000).copy(endReason = "completed"), "student-a")
        val second = journal.enqueue(session().copy(endReason = "user-ended"), "student-a")
        first.await(); second.await()
        journal.checkpoint(session(12000), "student-a").await()
        assertEquals(18000, journal.pending().single().actualDurationSeconds)
        assertEquals("completed", journal.pending().single().endReason)
    }

    @Test fun uploadAcknowledgementKeepsIdentityAndCannotBeReopened() = runBlocking {
        journal.enqueue(session(), "student-a").await()
        journal.acknowledge(journal.pending().single(), "server-id")
        journal.checkpoint(session(), "student-a").await()
        journal.enqueue(session(), "student-a").await()
        assertTrue(journal.pending().isEmpty())
        assertEquals("server-id", journal.observe().first().single().serverId)
        journal.reconcile(setOf("server-id")).await()
        assertTrue(journal.observe().first().isEmpty())
    }

    @Test fun pendingHistoryAndUploadsAreAccountIsolated() = runBlocking {
        journal.enqueue(session(), "student-a").await()
        owner.value = "student-b"
        assertTrue(journal.pending().isEmpty())
        assertTrue(journal.observe().first().isEmpty())
        owner.value = "student-a"
        assertEquals(1, journal.pending().size)
    }

    @Test fun failedDiskWriteRetainsCheckpointAndCanRetry() = runBlocking {
        journal.checkpoint(session(), "student-a").await()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_save BEFORE INSERT ON sessions BEGIN SELECT RAISE(ABORT, 'simulated disk error'); END")
        assertTrue(runCatching { journal.enqueue(session(12000), "student-a").await() }.isFailure)
        assertEquals("active", database.journal().rows("student-a").single().state)
        assertNotNull(journal.errors.value)
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_save")
        journal.retryWrites().await()
        assertEquals(12000, journal.pending().single().actualDurationSeconds)
        assertNull(journal.errors.value)
    }

    @Test fun legacyRecoveryDoesNotCountOvernightGapAndMigrationIsIdempotent() = runBlocking {
        context.getSharedPreferences("ekagra_timer_state_prefs", 0).edit()
            .putBoolean("has_state", true).putBoolean("is_running", true).putString("mode", "FOCUS")
            .putInt("total_seconds", 18000).putInt("remaining_seconds", 7200)
            .putLong("saved_at_ms", 1758621600000L).putString("auto_save_client_session_id", "legacy-one").commit()
        journal.recover().await()
        assertEquals(10800, journal.pending().single().actualDurationSeconds)
        val next = EkagraSessionJournal(context, database, owner, "process-b", false)
        next.recover().await()
        assertEquals(1, next.pending().size)
        assertTrue(context.getSharedPreferences("ekagra_timer_state_prefs", 0).getBoolean("has_state", false))
    }

    @Test fun legacyPomodoroBreakOnlyCountsCompletedFocusRounds() = runBlocking {
        context.getSharedPreferences("ekagra_timer_state_prefs", 0).edit()
            .putBoolean("has_state", true).putString("mode", "BREAK")
            .putInt("total_seconds", 600).putInt("remaining_seconds", 120)
            .putInt("target_pomodoro_loops", 4).putInt("completed_pomodoro_loops", 2)
            .putInt("pomodoro_focus_seconds", 1500).commit()
        journal.recover().await()
        assertEquals(3000, journal.pending().single().actualDurationSeconds)
    }

    @Test fun legacyManualBreakPreservesSuspendedFocus() = runBlocking {
        context.getSharedPreferences("ekagra_timer_state_prefs", 0).edit()
            .putBoolean("has_state", true).putString("mode", "BREAK")
            .putInt("total_seconds", 600).putInt("remaining_seconds", 120)
            .putInt("suspended_total_seconds", 18000).putInt("suspended_remaining_seconds", 7200).commit()
        journal.recover().await()
        assertEquals(10800, journal.pending().single().actualDurationSeconds)
    }
}
