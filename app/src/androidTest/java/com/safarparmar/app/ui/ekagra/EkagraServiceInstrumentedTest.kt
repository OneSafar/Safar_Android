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
                timer.elapsedRealtime = { SystemClock.elapsedRealtime() + 86_400_000L }
                setField(timer, "signedInUserId", account)
                timer.setDuration(TimerMode.FOCUS, 5 * 3600)
                timer.prepareAutoSaveSession("Reliability test", null, null, forceNew = true)
                timer.start()
                assertTrue(timer.isRunning.value)
                sessionId = requireNotNull(timer.currentSessionId())
                timer.onTaskRemoved(Intent())
                assertTrue("Swiping the task must not stop the service", timer.isRunning.value)
                // Advance the existing monotonic anchors; no shortened production intervals.
                setField(timer, "periodStartElapsed", elapsed(timer) - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", elapsed(timer))
                timer.onScheduledAlarm(sessionId, field(timer, "alarmGeneration") as Long)
                assertTrue(timer.isRunning.value)
                assertNotNull(timer.presenceCheckInDeadline.value)
                assertTrue(timer.secondsLeft.value in 3595..3600)
                setField(timer, "presenceExpiryElapsed", elapsed(timer) - 1)
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
                setField(timer, "periodStartElapsed", elapsed(timer) - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", elapsed(timer))
                timer.onScheduledAlarm(id, field(timer, "alarmGeneration") as Long)
                assertTrue(timer.secondsLeft.value >= 14400)
                assertNotNull(timer.presenceCheckInDeadline.value)
                timer.confirmPresenceReminder()
                assertNull(timer.presenceCheckInDeadline.value)
            }
            awaitNoPresence(timer)
            onMain {
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
                setField(timer, "periodStartElapsed", elapsed(timer) - 4 * 3600_000L)
                setField(timer, "presenceDueElapsed", elapsed(timer))
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
                assertTrue(timer.isRunning.value)
            }
            awaitNoPresence(timer)
        }
    }

    @Test fun bothPomodoroStylesPreserveFocusDuringManualBreakTransition() {
        withTimer { timer, account ->
            for (style in PomodoroStyle.entries) {
                onMain {
                    setField(timer, "autoStartBreak", false)
                    assertNotNull(timer.startPomodoroSession(style, 4, 60, 10, null, null, null))
                    setField(timer, "periodStartElapsed", elapsed(timer) - timer.totalSeconds.value * 1000L)
                }
                Thread.sleep(1500)
                onMain {
                    assertEquals(TimerMode.BREAK, timer.timerMode.value)
                    assertFalse(timer.isRunning.value)
                    assertEquals(1, timer.pomodorosCompleted.value)
                    assertEquals(if (style == PomodoroStyle.TRADITIONAL) 300 else 600, timer.totalSeconds.value)
                    timer.endAndSave()
                }
                awaitEnd(timer)
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
                setField(timer, "periodStartElapsed", elapsed(timer) - 12 * 60_000L)
                timer.pause()
                assertEquals(108 * 60, timer.secondsLeft.value)
                timer.start()
                // No display tick ran during the rest of the two-hour period.
                setField(timer, "periodStartElapsed", elapsed(timer) - 108 * 60_000L)
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
                setField(timer, "periodStartElapsed", elapsed(timer) - 12 * 60_000L)
                timer.pause()
                assertEquals(108 * 60, timer.secondsLeft.value)
                timer.start()
                setField(timer, "periodStartElapsed", elapsed(timer) - 78 * 60_000L)
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
                    setField(timer, "periodStartElapsed", elapsed(timer) - elapsedMinutes * 60_000L)
                    assertTrue(timer.startBreak(TimerMode.BREAK, 5 * 60))
                    assertEquals(2 * 3600, timer.focusProgressSnapshot().plannedSeconds)
                    assertEquals(elapsedMinutes * 60, timer.focusProgressSnapshot().actualSeconds)
                    setField(timer, "periodStartElapsed", elapsed(timer) - 2 * 60_000L)
                    assertEquals(elapsedMinutes * 60, timer.focusProgressSnapshot().actualSeconds)
                    timer.endAndSave()
                }
                awaitEnd(timer)
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
                service.elapsedRealtime = { SystemClock.elapsedRealtime() + 86_400_000L }
                setField(service, "signedInUserId", account)
                service.setDuration(TimerMode.FOCUS, 2 * 3600)
                service.start()
                setField(service, "periodStartElapsed", elapsed(service) - 12 * 60_000L)
                service.pause()
                assertEquals(108 * 60, service.secondsLeft.value)
                service.start()
                // Freeze the display ticker at twelve minutes, then let the live
                // monotonic period reach its deadline before Android destroys it.
                (field(service, "tickJob") as? Job)?.cancel()
                service.onTaskRemoved(Intent())
                setField(service, "periodStartElapsed", elapsed(service) - 108 * 60_000L)
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

    @Test fun chronometerUpdatesWithoutRebuildingNotificationAndEndCannotResurrectIt() {
        withTimer(realClock = true) { timer, account ->
            onMain {
                timer.elapsedRealtime = { SystemClock.elapsedRealtime() }
                timer.setDuration(TimerMode.STOPWATCH, 0)
                timer.start()
            }
            Thread.sleep(1400)
            var builds = 0
            onMain {
                builds = field(timer, "notificationBuildCount") as Int
                val notification = TimerService::class.java.getDeclaredMethod("buildNotification")
                    .apply { isAccessible = true }.invoke(timer) as android.app.Notification
                val content = notification.contentView.apply(timer, android.widget.FrameLayout(timer))
                val clock = content.findViewById<android.widget.Chronometer>(com.safarparmar.app.R.id.ekagra_notification_clock)
                assertFalse(clock.isCountDown)
                assertTrue(kotlin.math.abs(clock.base - elapsed(timer)) < 3000)
                builds++ // The explicit inspection above builds one notification.
            }
            Thread.sleep(2600)
            onMain {
                assertEquals("Ticks must not reconstruct the notification", builds, field(timer, "notificationBuildCount"))
                timer.onStartCommand(Intent().setAction(TimerService.ACTION_END_SAVE)
                    .putExtra("session", timer.currentSessionId()), 0, 1)
            }
            awaitEnd(timer)
            Thread.sleep(1200)
            onMain {
                assertFalse(timer.isRunning.value)
                assertFalse(field(timer, "timerSessionActive") as Boolean)
                if (android.os.Build.VERSION.SDK_INT >= 29) assertEquals(0, timer.foregroundServiceType)
                assertFalse(timer.getSystemService(android.app.NotificationManager::class.java)
                    .activeNotifications.any { it.id == TimerService.NOTIFICATION_ID })
            }
            assertEquals(1, rows(account).size)
        }
    }

    @Test fun countdownChronometerUsesFutureMonotonicDeadlineAndPausedValue() {
        withTimer(realClock = true) { timer, _ ->
            onMain {
                timer.elapsedRealtime = { SystemClock.elapsedRealtime() }
                timer.setDuration(TimerMode.FOCUS, 120)
                timer.start()
                val method = TimerService::class.java.getDeclaredMethod("buildNotification").apply { isAccessible = true }
                val notification = method.invoke(timer) as android.app.Notification
                val content = notification.contentView.apply(timer, android.widget.FrameLayout(timer))
                val clock = content.findViewById<android.widget.Chronometer>(com.safarparmar.app.R.id.ekagra_notification_clock)
                assertTrue(clock.isCountDown)
                assertTrue(clock.base - elapsed(timer) in 117000L..120000L)
                timer.pause()
                val paused = method.invoke(timer) as android.app.Notification
                val pausedContent = paused.contentView.apply(timer, android.widget.FrameLayout(timer))
                assertEquals("02:00", pausedContent.findViewById<android.widget.Chronometer>(com.safarparmar.app.R.id.ekagra_notification_clock).text.toString())
            }
        }
    }

    @Test fun delayedAutomaticPomodoroCrossesBoundariesAndFinalizesOneFocusIdentity() {
        withTimer { timer, account ->
            var identity = ""
            onMain {
                setField(timer, "autoStartBreak", true)
                identity = requireNotNull(timer.startPomodoroSession(PomodoroStyle.CUSTOM, 2, 1, 1, "Physics", "goal-qa", null))
                setField(timer, "periodStartElapsed", elapsed(timer) - 140_000L)
                timer.onScheduledAlarm(identity, field(timer, "alarmGeneration") as Long)
                assertEquals(TimerMode.POMODORO, timer.timerMode.value)
                assertEquals(40, timer.secondsLeft.value)
                assertEquals(80, timer.focusProgressSnapshot().actualSeconds)
                setField(timer, "periodStartElapsed", elapsed(timer) - 140_000L)
                timer.onScheduledAlarm(identity, field(timer, "alarmGeneration") as Long)
            }
            awaitEnd(timer)
            val saved = rows(account)
            val focus = saved.single { it.clientSessionId == identity }
            assertEquals(120, focus.actualDurationSeconds)
            assertEquals("goal-qa", focus.goalId)
            assertEquals(2, saved.count { it.taskTitle == "Break" })
            assertEquals(3, saved.map { it.clientSessionId }.distinct().size)
            onMain { timer.onScheduledAlarm(identity, -1) }
            assertEquals(3, rows(account).size)
        }
    }

    @Test fun standaloneBreakNotificationEndIsSessionScopedAndStopsForegroundWork() {
        withTimer(realClock = true) { timer, _ ->
            onMain {
                timer.elapsedRealtime = { SystemClock.elapsedRealtime() }
                timer.setDuration(TimerMode.BREAK, 120)
                timer.start()
                val id = requireNotNull(timer.currentSessionId())
                timer.onStartCommand(Intent().setAction(TimerService.ACTION_END_SAVE).putExtra("session", "old-break"), 0, 1)
                assertTrue(timer.isRunning.value)
                timer.onStartCommand(Intent().setAction(TimerService.ACTION_END_SAVE).putExtra("session", id), 0, 2)
            }
            awaitEnd(timer)
            onMain {
                assertFalse(timer.isRunning.value)
                if (android.os.Build.VERSION.SDK_INT >= 29) assertEquals(0, timer.foregroundServiceType)
            }
        }
    }

    @Test fun failedTerminalWriteKeepsForegroundProtectionUntilLocalRetryCommits() {
        withTimer { timer, account ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val journal = EkagraSessionJournal.get(context)
            onMain {
                timer.setDuration(TimerMode.FOCUS, 120)
                timer.start()
                setField(timer, "periodStartElapsed", elapsed(timer) - 10_000L)
                timer.focusProgressSnapshot()
            }
            runBlocking { journal.recover().await() }
            val db = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
            try {
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER qa_fail_terminal BEFORE INSERT ON sessions WHEN NEW.owner = '$account' AND NEW.state = 'pending' BEGIN SELECT RAISE(ABORT, 'QA disk full'); END")
                onMain { timer.endAndSave() }
                awaitEnd(timer)
                onMain {
                    assertNotNull(journal.errors.value)
                    assertTrue(field(timer, "timerSessionActive") as Boolean)
                    if (android.os.Build.VERSION.SDK_INT >= 29) assertTrue(timer.foregroundServiceType != 0)
                }
            } finally {
                db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS qa_fail_terminal")
                runBlocking { journal.retryWrites().await() }
                db.close()
            }
            onMain {
                timer.start()
                assertFalse("A committed End must finish cleanup before another Start", timer.isRunning.value)
                timer.retryRecovery()
            }
            repeat(100) {
                var active = true
                onMain { active = field(timer, "timerSessionActive") as Boolean }
                if (active) Thread.sleep(50)
            }
            onMain { assertFalse("Retry must finish the original End without a second End tap", field(timer, "timerSessionActive") as Boolean) }
            awaitEnd(timer)
            assertEquals(1, rows(account).size)
            onMain { if (android.os.Build.VERSION.SDK_INT >= 29) assertEquals(0, timer.foregroundServiceType) }
        }
    }

    @Test fun staleRecoveryStartWithNoActiveTimerDoesNotLeaveForegroundWork() {
        withTimer(realClock = true) { timer, _ ->
            onMain {
                assertFalse(field(timer, "timerSessionActive") as Boolean)
                assertEquals(android.app.Service.START_NOT_STICKY,
                    timer.onStartCommand(Intent().setAction(TimerService.ACTION_RECOVER), 0, 1))
                if (android.os.Build.VERSION.SDK_INT >= 29) assertEquals(0, timer.foregroundServiceType)
            }
        }
    }

    private fun awaitNoPresence(timer: TimerService) {
        val manager = timer.getSystemService(android.app.NotificationManager::class.java)
        repeat(100) {
            if (manager.activeNotifications.none { it.id == TimerService.PRESENCE_NOTIFICATION_ID }) return
            Thread.sleep(20)
        }
        fail("Presence notification was not removed")
    }

    @org.junit.After fun awaitServiceShutdown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TimerService.live?.let { timer ->
            onMain { if (field(timer, "timerSessionActive") == true) timer.endAndSave() }
            awaitEnd(timer)
        }
        context.stopService(Intent(context, TimerService::class.java))
        repeat(100) {
            if (TimerService.live == null) return
            Thread.sleep(50)
        }
        fail("Timer service did not stop after test unbound")
    }

    private fun awaitEnd(timer: TimerService) {
        repeat(100) {
            if (TimerService.live !== timer) return
            var closing = false
            onMain { closing = field(timer, "closingSession") as Boolean }
            if (!closing) return
            Thread.sleep(50)
        }
        fail("Local finalization did not finish")
    }

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    private fun elapsed(timer: TimerService): Long = timer.elapsedRealtime()
    private fun withTimer(realClock: Boolean = false, test: (TimerService, String) -> Unit) {
        val account = "instrumentation-${UUID.randomUUID()}"
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(700)
            val timer = requireNotNull(TimerService.live)
            onMain {
                if (!realClock) timer.elapsedRealtime = { SystemClock.elapsedRealtime() + 86_400_000L }
                setField(timer, "signedInUserId", account)
            }
            try { test(timer, account) } finally {
                onMain { timer.endAndSave() }
                awaitEnd(timer)
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
