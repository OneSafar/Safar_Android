package com.safarparmar.app.ui.ekagra

import android.content.Intent
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** A QA-only, unsigned-in session: no credentials and no backend uploads. Production clocks are unchanged. */
class EkagraServiceInstrumentedTest {
    @Test fun recentAppsCheckInExpiryAndNotificationEndKeepStudyTimeSafe() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val account = "instrumentation-${UUID.randomUUID()}"
        var sessionId = ""
        ActivityScenario.launch(MainActivity::class.java).use {
            var service: TimerService? = null
            repeat(100) {
                service = TimerService.live
                if (service != null && field(service!!, "recoveryReady") == true) return@repeat
                Thread.sleep(50)
            }
            val timer = requireNotNull(service)
            instrumentation.runOnMainSync {
                setField(timer, "signedInUserId", account)
                timer.setDuration(TimerMode.FOCUS, 5 * 3600)
                timer.prepareAutoSaveSession("Reliability test", null, null, forceNew = true)
                timer.start()
                assertTrue(timer.isRunning.value)
                sessionId = requireNotNull(timer.currentSessionId())
                timer.onTaskRemoved(Intent())
                assertTrue("Swiping the task must not stop the service", timer.isRunning.value)
                // Advance the existing monotonic anchors; no shortened production intervals.
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", SystemClock.elapsedRealtime())
                timer.onScheduledAlarm(sessionId, field(timer, "alarmGeneration") as Long)
                assertTrue(timer.isRunning.value)
                assertNotNull(timer.presenceCheckInDeadline.value)
                assertTrue(timer.secondsLeft.value in 3595..3600)
                setField(timer, "presenceExpiryElapsed", SystemClock.elapsedRealtime() - 1)
                timer.onScheduledAlarm(sessionId, field(timer, "alarmGeneration") as Long)
                assertTrue("Check-in expiry must not pause the personal timer", timer.isRunning.value)
                assertNull(timer.presenceCheckInDeadline.value)
                timer.onStartCommand(Intent().setAction(TimerService.ACTION_RESET).putExtra("session", "stale-session"), 0, 1)
                assertTrue("A stale notification must not end a newer session", timer.isRunning.value)
                timer.onStartCommand(Intent().setAction(TimerService.ACTION_RESET).putExtra("session", sessionId), 0, 2)
                assertFalse(timer.isRunning.value)
            }
            runBlocking { EkagraSessionJournal.get(context).recover().await() }
            val db = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
            try {
                val rows = runBlocking { db.journal().rows(account) }
                assertEquals(1, rows.size)
                assertEquals(sessionId, rows.single().id)
                assertEquals("pending", rows.single().state)
                val saved = com.google.gson.Gson().fromJson(rows.single().payload, PendingEkagraSessionSave::class.java)
                assertTrue(saved.actualDurationSeconds!! >= 14400)
                assertEquals("user-ended", saved.endReason)
            } finally {
                db.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(account))
                db.close()
            }
        }
    }
    @Test fun stopwatchReminderAndPauseResumeDoNotChangeElapsedFocus() {
        withTimer { timer, account ->
            onMain {
                timer.setDuration(TimerMode.STOPWATCH, 0)
                timer.start()
                val id = requireNotNull(timer.currentSessionId())
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", SystemClock.elapsedRealtime())
                timer.onScheduledAlarm(id, field(timer, "alarmGeneration") as Long)
                assertTrue(timer.secondsLeft.value >= 14400)
                assertNotNull(timer.presenceCheckInDeadline.value)
                timer.confirmPresenceReminder()
                assertNull(timer.presenceCheckInDeadline.value)
                assertFalse(timer.getSystemService(android.app.NotificationManager::class.java)
                    .activeNotifications.any { it.id == TimerService.PRESENCE_NOTIFICATION_ID })
                timer.pause()
                val paused = timer.secondsLeft.value
                timer.start()
                assertEquals(paused, timer.secondsLeft.value)
                timer.endAndSave()
            }
            assertEquals(1, rows(account).size)
        }
    }

    @Test fun presenceResponsesDismissBothSurfacesAndRejectStaleActions() {
        withTimer { timer, _ ->
            onMain {
                timer.setDuration(TimerMode.STOPWATCH, 0)
                timer.start()
                val id = requireNotNull(timer.currentSessionId())
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", SystemClock.elapsedRealtime())
                timer.onScheduledAlarm(id, field(timer, "alarmGeneration") as Long)
                val deadline = requireNotNull(timer.presenceCheckInDeadline.value)
                fun respond(action: String, token: Long) = timer.onStartCommand(
                    Intent().setAction(action).putExtra("session", id).putExtra("presenceDeadline", token), 0, 1,
                )
                respond(TimerService.ACTION_CONFIRM_PRESENCE, deadline - 1)
                assertEquals(deadline, timer.presenceCheckInDeadline.value)
                respond(TimerService.ACTION_DECLINE_PRESENCE, deadline)
                assertTrue(timer.presencePromptDismissed.value)
                assertEquals("No must preserve the original grace deadline", deadline, timer.presenceCheckInDeadline.value)
                respond(TimerService.ACTION_CONFIRM_PRESENCE, deadline)
                assertEquals("A second response must not confirm a dismissed check-in", deadline, timer.presenceCheckInDeadline.value)
                val manager = timer.getSystemService(android.app.NotificationManager::class.java)
                assertFalse(manager.activeNotifications.any { it.id == TimerService.PRESENCE_NOTIFICATION_ID })
                assertTrue(timer.isRunning.value)
                timer.endAndSave()
            }
        }
    }

    @Test fun bothPomodoroStylesPreserveFocusDuringManualBreakTransition() {
        withTimer { timer, account ->
            for (style in PomodoroStyle.entries) {
                onMain {
                    setField(timer, "autoStartBreak", false)
                    assertNotNull(timer.startPomodoroSession(style, 4, 60, 10, null, null, null))
                    setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - timer.totalSeconds.value * 1000L)
                }
                Thread.sleep(1500)
                onMain {
                    assertEquals(TimerMode.BREAK, timer.timerMode.value)
                    assertFalse(timer.isRunning.value)
                    assertEquals(1, timer.pomodorosCompleted.value)
                    assertEquals(if (style == PomodoroStyle.TRADITIONAL) 300 else 600, timer.totalSeconds.value)
                    timer.endAndSave()
                }
            }
            val saved = rows(account)
            assertEquals(2, saved.size)
            assertEquals(listOf(1500, 3600), saved.map { it.actualDurationSeconds }.sortedBy { it })
        }
    }

    @Test fun repeatedStartAfterSleepKeepsTheOriginalTwoHourPeriod() {
        withTimer { timer, account ->
            onMain {
                timer.setDuration(TimerMode.FOCUS, 2 * 3600)
                timer.start()
                val id = requireNotNull(timer.currentSessionId())
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 12 * 60_000L)
                timer.pause()
                assertEquals(108 * 60, timer.secondsLeft.value)
                timer.start()
                // No display tick ran during the rest of the two-hour period.
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 108 * 60_000L)
                timer.start()
                assertEquals(id, timer.currentSessionId())
                timer.endAndSave()
            }
            assertEquals(2 * 3600, rows(account).single().actualDurationSeconds)
        }
    }

    @Test fun progressSnapshotReconcilesSleepBeforeTheScreenPausesTheTimer() {
        withTimer { timer, _ ->
            onMain {
                timer.setDuration(TimerMode.FOCUS, 2 * 3600)
                timer.start()
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 12 * 60_000L)
                timer.pause()
                assertEquals(108 * 60, timer.secondsLeft.value)
                timer.start()
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 78 * 60_000L)
                val progress = timer.focusProgressSnapshot()
                assertEquals(2 * 3600, progress.plannedSeconds)
                assertEquals(90 * 60, progress.actualSeconds)
                assertEquals(30 * 60, timer.secondsLeft.value)
                assertTrue(timer.isRunning.value)
                timer.endAndSave()
            }
        }
    }

    @Test fun manualBreakAfterSleepPreservesFocusAndPomodoroTimeWithoutCreditingTheBreak() {
        withTimer { timer, account ->
            for (mode in listOf(TimerMode.FOCUS, TimerMode.POMODORO)) {
                onMain {
                    val elapsedMinutes = if (mode == TimerMode.FOCUS) 90 else 45
                    if (mode == TimerMode.FOCUS) {
                        timer.setDuration(mode, 2 * 3600)
                        timer.start()
                    } else {
                        assertNotNull(timer.startPomodoroSession(PomodoroStyle.CUSTOM, 2, 60, 5, null, null, null))
                    }
                    setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - elapsedMinutes * 60_000L)
                    assertTrue(timer.startBreak(TimerMode.BREAK, 5 * 60))
                    assertEquals(2 * 3600, timer.focusProgressSnapshot().plannedSeconds)
                    assertEquals(elapsedMinutes * 60, timer.focusProgressSnapshot().actualSeconds)
                    setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 2 * 60_000L)
                    assertEquals(elapsedMinutes * 60, timer.focusProgressSnapshot().actualSeconds)
                    timer.endAndSave()
                }
            }
            assertEquals(listOf(45 * 60, 90 * 60), rows(account).map { it.actualDurationSeconds }.sortedBy { it })
        }
    }

    @Test fun serviceDestructionAfterSleepKeepsTwoHoursResumableInsteadOfFinalizingTwelveMinutes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = "instrumentation-${UUID.randomUUID()}"
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var timer: TimerService? = null
        try {
            repeat(100) {
                if (TimerService.live != null && field(TimerService.live!!, "recoveryReady") == true) {
                    timer = TimerService.live
                }
                if (timer == null) Thread.sleep(50)
            }
            val service = requireNotNull(timer)
            onMain {
                setField(service, "signedInUserId", account)
                service.setDuration(TimerMode.FOCUS, 2 * 3600)
                service.start()
                setField(service, "periodStartElapsed", SystemClock.elapsedRealtime() - 12 * 60_000L)
                service.pause()
                assertEquals(108 * 60, service.secondsLeft.value)
                service.start()
                // Freeze the display ticker at twelve minutes, then let the live
                // monotonic period reach its deadline before Android destroys it.
                (field(service, "tickJob") as? Job)?.cancel()
                service.onTaskRemoved(Intent())
                setField(service, "periodStartElapsed", SystemClock.elapsedRealtime() - 108 * 60_000L)
            }
            // Unbind the activity before stopping the started service so Android
            // invokes the actual lifecycle callback exactly once.
            scenario.close()
            context.stopService(Intent(context, TimerService::class.java))
            repeat(100) {
                if (TimerService.live != null) Thread.sleep(50)
            }
            assertNull(TimerService.live)
            val saved = rows(account).single()
            assertEquals(2 * 3600, saved.actualDurationSeconds)
            assertEquals(120, saved.plannedDurationMinutes)
            assertNull(saved.endReason)
            assertNotNull("Service destruction must retain the same live timer clock", saved.recovery)
            assertTrue(saved.recovery!!.isRunning)
            assertEquals(0, saved.recovery!!.remainingSeconds)
        } finally {
            scenario.close()
            context.stopService(Intent(context, TimerService::class.java))
            repeat(100) {
                if (TimerService.live != null) Thread.sleep(50)
            }
            val db = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
            try {
                runBlocking { EkagraSessionJournal.get(context).recover().await() }
                db.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(account))
            } finally {
                db.close()
            }
        }
    }

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    private fun withTimer(test: (TimerService, String) -> Unit) {
        val account = "instrumentation-${UUID.randomUUID()}"
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(700)
            val timer = requireNotNull(TimerService.live)
            onMain { setField(timer, "signedInUserId", account) }
            try { test(timer, account) } finally {
                onMain { timer.endAndSave() }
                runBlocking { EkagraSessionJournal.get(InstrumentationRegistry.getInstrumentation().targetContext).recover().await() }
                val db = Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
                db.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(account))
                db.close()
            }
        }
    }
    private fun rows(account: String): List<PendingEkagraSessionSave> = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        EkagraSessionJournal.get(context).recover().await()
        val db = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
        try { db.journal().rows(account).map { com.google.gson.Gson().fromJson(it.payload, PendingEkagraSessionSave::class.java) } }
        finally { db.close() }
    }

    private fun field(instance: Any, name: String): Any? = TimerService::class.java.getDeclaredField(name).apply { isAccessible = true }.get(instance)
    private fun setField(instance: Any, name: String, value: Any) { TimerService::class.java.getDeclaredField(name).apply { isAccessible = true }.set(instance, value) }
}
