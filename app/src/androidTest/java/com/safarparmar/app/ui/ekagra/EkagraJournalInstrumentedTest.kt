package com.safarparmar.app.ui.ekagra

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.safarparmar.app.util.Resource
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

    @Test fun goalChoiceOnOfflineCompletionUpdatesSamePendingRow() = runBlocking {
        journal.enqueue(session().copy(goalId = null), "student-a").await()
        assertTrue(journal.setPendingGoal("local-test", "student-a", "chosen-goal", "Math", true))
        val pending = journal.pending().single()
        assertEquals("local-test", pending.clientSessionId)
        assertEquals("chosen-goal", pending.goalId)
        assertTrue(pending.markGoalComplete)
        assertEquals(10800, pending.actualDurationSeconds)
        journal.enqueue(session(), "student-a").await()
        assertEquals("chosen-goal", journal.pending().single().goalId)
    }

    @Test fun acknowledgedGoalChoiceUsesExistingServerIdentity() = runBlocking {
        journal.enqueue(session(), "student-a").await()
        journal.acknowledge(journal.pending().single(), "server-id")
        assertFalse(journal.setPendingGoal("local-test", "student-a", "new-goal", "Math", true))
        assertEquals("server-id", journal.find("local-test", "student-a")?.serverId)
        journal.reconcile(setOf("server-id")).await()
        assertEquals("server-id", journal.find("local-test", "student-a")?.serverId)
        assertTrue(journal.pending().isEmpty())
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

    @Test fun acknowledgementDiskFailureKeepsTwoHourSessionPendingForTheSameUploadRetry() = runBlocking {
        journal.enqueue(session(7200), "student-a").await()
        val pending = journal.pending().single()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_ack BEFORE INSERT ON sessions WHEN NEW.state = 'acknowledged' BEGIN SELECT RAISE(ABORT, 'simulated disk error'); END")

        val result: Resource<Unit> = attemptEkagraSessionSync(
            onFailure = { Resource.Error("Retry from History") },
        ) {
            journal.acknowledge(pending, "server-two-hours")
            Resource.Success(Unit)
        }

        assertTrue(result is Resource.Error)
        assertNotNull(journal.errors.value)
        val retry = journal.pending().single()
        assertEquals(pending.clientSessionId, retry.clientSessionId)
        assertEquals(7200, retry.actualDurationSeconds)
        assertNull(retry.serverId)

        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_ack")
        journal.acknowledge(retry, "server-two-hours")
        assertTrue(journal.pending().isEmpty())
        assertEquals("server-two-hours", journal.find(retry.clientSessionId, "student-a")?.serverId)
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

    private fun recoveryState(running: Boolean = true) = EkagraTimerRecoveryState(
        mode = TimerMode.FOCUS, totalSeconds = 7200, remainingSeconds = 6467,
        isRunning = running, checkpointElapsedMs = 1_000_000L,
        checkpointBootCount = 7, checkpointAtMs = 1_800_000_000_000L,
    )

    @Test fun resumableProcessDeathRetainsTheOriginalIdentityAndDoesNotUploadATwelveMinuteFragment() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        restarted.recover().await()
        assertTrue(restarted.pending().isEmpty())
        assertTrue(restarted.observe().first().isEmpty())
        val resumed = requireNotNull(restarted.claimRecovery("student-a"))
        assertEquals("local-test", resumed.clientSessionId)
        assertEquals("student-a", resumed.ownerId)
        assertEquals("topic", resumed.topicId)
        assertEquals(6467, resumed.recovery?.remainingSeconds)
        assertEquals("active", database.journal().rows("student-a").single().state)
        assertEquals("process-b", database.journal().rows("student-a").single().runtime)
    }

    @Test fun serviceRecreationInTheSameProcessCanClaimItsActiveDurableCheckpoint() = runBlocking {
        journal.checkpoint(session(733).copy(recovery = recoveryState()), "student-a").await()
        assertEquals("local-test", journal.claimRecovery("student-a")?.clientSessionId)
        assertTrue(journal.pending().isEmpty())
        assertNotNull(journal.claimRecovery("student-a"))
    }

    @Test fun recoveryReadsAreAccountIsolatedAndDoNotClaimOrFinalizeRows() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        assertEquals("local-test", restarted.peekRecovery()?.clientSessionId)
        assertEquals("process-a", database.journal().rows("student-a").single().runtime)
        owner.value = "student-b"
        assertNull(restarted.peekRecovery())
        assertNull(restarted.claimRecovery("student-a"))
        assertNull(restarted.claimRecovery("student-b"))
        assertEquals("active", database.journal().rows("student-a").single().state)
        owner.value = "student-a"
        assertEquals("local-test", restarted.claimRecovery("student-a")?.clientSessionId)
    }

    @Test fun recoveredCompletionAndLateCheckpointStillProduceOneFullTwoHourRecord() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        val resumed = requireNotNull(restarted.claimRecovery("student-a"))
        restarted.enqueue(resumed.copy(actualDurationSeconds = 7200, actualDurationMinutes = 120,
            endReason = "completed"), "student-a").await()
        restarted.checkpoint(session(740), "student-a", recoveryState()).await()
        restarted.enqueue(session(733), "student-a").await()
        val saved = restarted.pending().single()
        assertEquals("local-test", saved.clientSessionId)
        assertEquals(7200, saved.actualDurationSeconds)
        assertEquals("completed", saved.endReason)
        assertNull(saved.recovery)
        assertNull(restarted.peekRecovery())
        assertNull(restarted.claimRecovery("student-a"))
    }

    @Test fun claimRetainsOlderOrphanProgressWithoutStartingTwoTimers() = runBlocking {
        journal.checkpoint(session(733, "older"), "student-a", recoveryState()).await()
        journal.checkpoint(session(800, "newer"), "student-a", recoveryState()).await()
        val dao = database.journal()
        dao.put(requireNotNull(dao.row("student-a", "older")).copy(checkpointMs = 10))
        dao.put(requireNotNull(dao.row("student-a", "newer")).copy(checkpointMs = 20))
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        assertEquals("newer", restarted.claimRecovery("student-a")?.clientSessionId)
        val orphan = restarted.pending().single()
        assertEquals("older", orphan.clientSessionId)
        assertEquals(733, orphan.actualDurationSeconds)
        assertEquals("interrupted", orphan.endReason)
        assertNull(orphan.recovery)
        assertEquals(1, dao.rows("student-a").count { it.state == "active" })
    }

    @Test fun failedRecoveryCheckpointWriteRemainsRetryableWithTheSameIdentity() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_checkpoint BEFORE INSERT ON sessions BEGIN SELECT RAISE(ABORT, 'simulated disk error'); END")
        val advanced = recoveryState().copy(remainingSeconds = 6400)
        assertTrue(runCatching { journal.checkpoint(session(800), "student-a", advanced).await() }.isFailure)
        assertEquals(6467, journal.peekRecovery()?.recovery?.remainingSeconds)
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_checkpoint")
        journal.retryWrites().await()
        assertEquals(6400, journal.peekRecovery()?.recovery?.remainingSeconds)
        assertEquals("local-test", journal.peekRecovery()?.clientSessionId)
        assertNull(journal.errors.value)
    }


    @Test fun retryingAnOlderCheckpointCannotMoveAResumedTimerBackwards() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_stale BEFORE INSERT ON sessions BEGIN SELECT RAISE(ABORT, 'simulated disk error'); END")
        val failed = recoveryState().copy(remainingSeconds = 6400, checkpointElapsedMs = 1_067_000L)
        assertTrue(runCatching { journal.checkpoint(session(800), "student-a", failed).await() }.isFailure)
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_stale")
        val latest = recoveryState().copy(remainingSeconds = 6200, checkpointElapsedMs = 1_267_000L)
        journal.checkpoint(session(1000), "student-a", latest).await()
        journal.retryWrites().await()
        assertEquals(6200, journal.peekRecovery()?.recovery?.remainingSeconds)
        assertEquals(1000, journal.peekRecovery()?.actualDurationSeconds)
        journal.checkpoint(session(733), "student-a").await()
        assertEquals(6200, journal.peekRecovery()?.recovery?.remainingSeconds)
    }


    @Test fun retryWithTheSameClockMillisecondCannotUndoANewerPause() = runBlocking {
        val running = recoveryState().copy(checkpointSequence = 1)
        val paused = running.copy(isRunning = false, checkpointSequence = 2)
        journal.checkpoint(session(720), "student-a", paused).await()
        journal.checkpoint(session(720), "student-a", running).await()
        val restored = journal.claimRecovery("student-a")!!.recovery!!
        assertFalse(restored.isRunning)
        assertEquals(2L, restored.checkpointSequence)
    }

    @Test fun immediatelyEndedEmptySessionCannotResumeAfterProcessDeathOrLateCheckpoint() = runBlocking {
        val empty = session(0).copy(recovery = recoveryState().copy(remainingSeconds = 7200))
        journal.checkpoint(empty, "student-a").await()
        journal.closeEmptySession(empty.clientSessionId, "student-a").await()
        journal.checkpoint(empty, "student-a").await()
        val restarted = EkagraSessionJournal(context, database, owner, "process-b", false)
        restarted.recover().await()
        assertNull(restarted.peekRecovery())
        assertNull(restarted.claimRecovery("student-a"))
        assertTrue(restarted.pending().isEmpty())
        assertTrue(restarted.observe().first().isEmpty())
        val row = database.journal().rows("student-a").single()
        assertEquals("synced", row.state)
        assertNull(restarted.find(empty.clientSessionId, "student-a")?.recovery)
        restarted.enqueue(session(7200), "student-a").await()
        assertTrue(restarted.pending().isEmpty())
    }

    @Test fun closingBeforeTheFirstDurableCheckpointCreatesATombstoneForLateRetries() = runBlocking {
        journal.closeEmptySession("ended-before-write", "student-a").await()
        journal.checkpoint(session(0, "ended-before-write"), "student-a", recoveryState()).await()
        assertNull(journal.peekRecovery())
        assertNull(journal.claimRecovery("student-a"))
        assertEquals("synced", database.journal().rows("student-a").single().state)
        assertTrue(journal.pending().isEmpty())
    }

    @Test fun failedEmptyCloseCanRetryWithoutResurrectingTheTimer() = runBlocking {
        journal.checkpoint(session(0), "student-a", recoveryState().copy(remainingSeconds = 7200)).await()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_empty_close BEFORE INSERT ON sessions WHEN NEW.state = 'synced' BEGIN SELECT RAISE(ABORT, 'simulated disk error'); END")
        assertTrue(runCatching { journal.closeEmptySession("local-test", "student-a").await() }.isFailure)
        assertNotNull(journal.peekRecovery())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_empty_close")
        journal.retryWrites().await()
        journal.checkpoint(session(0), "student-a", recoveryState()).await()
        assertNull(journal.peekRecovery())
        assertTrue(journal.pending().isEmpty())
        assertNull(journal.errors.value)
    }

    @Test fun emptyCloseNeverDropsExistingNonzeroProgressOrRewritesAFinalizedCompletion() = runBlocking {
        journal.checkpoint(session(733), "student-a", recoveryState()).await()
        journal.closeEmptySession("local-test", "student-a").await()
        assertEquals(733, journal.pending().single().actualDurationSeconds)
        assertNull(journal.pending().single().recovery)
        journal.enqueue(session(7200, "completed").copy(endReason = "completed"), "student-a").await()
        journal.closeEmptySession("completed", "student-a").await()
        assertEquals(7200, journal.pending().single { it.clientSessionId == "completed" }.actualDurationSeconds)
        assertEquals("completed", journal.pending().single { it.clientSessionId == "completed" }.endReason)
    }

}
