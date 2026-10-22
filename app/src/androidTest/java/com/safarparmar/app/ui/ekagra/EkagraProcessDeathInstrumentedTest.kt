package com.safarparmar.app.ui.ekagra

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.safarparmar.app.MainActivity
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Two independent instrumentation invocations, separated by an actual OS process kill.
 * Run only through scripts/debug/ekagra-process-death.py on an offline, signed-out emulator.
 * The seed invocation intentionally stays alive until that script kills its process.
 */
class EkagraProcessDeathInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val args get() = InstrumentationRegistry.getArguments()
    private val case get() = requireNotNull(args.getString("ekagraCase"))
    private val owner get() = "process-death-qa-$case"
    private val seedFile get() = File(context.filesDir, "ekagra-process-death-seed.json")
    private val resultFile get() = File(context.filesDir, "ekagra-process-death-result.json")
    private val gson = Gson()

    @Test fun seedProcessDeath() {
        assumeTrue("Run using the external process-death harness", args.containsKey("ekagraCase"))
        requireDisposableOfflineEmulator()
        val store = SafarDataStore(context)
        runBlocking {
            require(store.userId.first().isNullOrBlank() || store.userId.first() == owner) {
                "This harness requires a signed-out disposable emulator"
            }
            store.setUserId(owner)
            store.setAutoStartBreak(case == "pomodoro-chain")
        }
        resultFile.delete()
        seedFile.delete()
        ActivityScenario.launch(MainActivity::class.java).use {
            val timer = awaitTimer()
            onMain {
                when (case) {
                    "stopwatch" -> timer.setDuration(TimerMode.STOPWATCH, 0)
                    "pomodoro-chain" -> timer.startPomodoroSession(
                        PomodoroStyle.CUSTOM, 2, 1, 1, "Process death QA", null, null,
                    )
                    else -> timer.setDuration(TimerMode.FOCUS, 120 * 60)
                }
                if (case != "pomodoro-chain") {
                    timer.prepareAutoSaveSession("Process death QA", null, null, forceNew = true)
                    timer.start()
                }
                assertTrue(timer.isRunning.value)
                if (Build.VERSION.SDK_INT >= 34) {
                    assertEquals("A silent study timer must not rely on media playback eligibility",
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, timer.foregroundServiceType)
                }
                val activeSeconds = when (case) {
                    "focus-completion" -> 7200 - 6
                    "pomodoro-chain" -> 60 - 6
                    else -> 12 * 60
                }
                setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - activeSeconds * 1000L)
                timer.focusProgressSnapshot()
                when (case) {
                    "paused" -> timer.pause()
                    "manual-break", "manual-break-completion" -> {
                        assertTrue(timer.startBreak(TimerMode.BREAK, 5 * 60))
                        if (case == "manual-break-completion") {
                            setField(timer, "periodStartElapsed", SystemClock.elapsedRealtime() - 294_000L)
                            timer.focusProgressSnapshot()
                        }
                    }
                }
                persist(timer)
            }
            runBlocking { EkagraSessionJournal.get(context).recover().await() }
            val row = readRows().single()
            val session = gson.fromJson(row.payload, PendingEkagraSessionSave::class.java)
            assertEquals("active", row.state)
            assertEquals(timer.currentSessionId(), session.clientSessionId)
            val recovery = requireNotNull(session.recovery) { "Running state must be atomic with the Room checkpoint" }
            val metadata = JSONObject()
                .put("case", case)
                .put("owner", owner)
                .put("id", session.clientSessionId)
                .put("pid", Process.myPid())
                .put("actualSeconds", session.actualDurationSeconds)
                .put("checkpointElapsedMs", recovery.checkpointElapsedMs)
                .put("mode", timer.timerMode.value.name)
                .put("running", timer.isRunning.value)
            writeAtomically(seedFile, metadata)
            // The orchestrator must kill us, without onDestroy or normal test teardown.
            Thread.sleep(180_000L)
            fail("The external harness did not kill the seed process")
        }
    }

    @Test fun verifyRecovery() {
        assumeTrue("Run using the external process-death harness", args.containsKey("ekagraCase"))
        requireDisposableOfflineEmulator()
        val seed = JSONObject(seedFile.readText())
        assertEquals(owner, seed.getString("owner"))
        assertNotEquals("Verification must run in a new process", seed.getInt("pid"), Process.myPid())
        var scenario: ActivityScenario<MainActivity>? = null
        try {
            // Exercise the real receiver while this new process has no bound timer.
            // An old PendingIntent must not restart or finalize this newer identity.
            assertNull(TimerService.live)
            val beforeStaleAlarm = readRows().single()
            context.sendBroadcast(Intent(context, EkagraTimerAlarmReceiver::class.java)
                .setAction("com.safar.ekagra.DEADLINE")
                .putExtra("session", "stale-process-death-qa-session")
                .putExtra("generation", -1L))
            instrumentation.waitForIdleSync()
            Thread.sleep(500)
            assertNull("A stale alarm must not start the timer service", TimerService.live)
            val afterStaleAlarm = readRows().single()
            assertEquals(beforeStaleAlarm.id, afterStaleAlarm.id)
            assertEquals(beforeStaleAlarm.state, afterStaleAlarm.state)
            assertEquals(beforeStaleAlarm.payload, afterStaleAlarm.payload)
            scenario = ActivityScenario.launch(MainActivity::class.java)
            val timer = awaitTimer()
            val interruption = args.getString("ekagraInterruption") ?: "kill"
            val stoppedExplicitly = interruption == "force-stop" || interruption == "reboot"
            val initialActual = seed.getInt("actualSeconds")
            var expectedActual = initialActual
            onMain {
                val progress = timer.focusProgressSnapshot()
                val elapsed = ((SystemClock.elapsedRealtime() - seed.getLong("checkpointElapsedMs")) / 1000L).toInt()
                if (stoppedExplicitly) {
                    assertFalse("An explicit stop or reboot must restore paused", timer.isRunning.value)
                    assertEquals("Do not credit unobserved stop/reboot time", initialActual, progress.actualSeconds)
                } else when (case) {
                    "focus-completion" -> {
                        assertFalse("A timer that reached its deadline must finish after recovery", timer.isRunning.value)
                        expectedActual = 7200
                    }
                    "paused" -> {
                        assertEquals(seed.getString("id"), timer.currentSessionId())
                        assertFalse(timer.isRunning.value)
                        assertEquals(initialActual, progress.actualSeconds)
                    }
                    "manual-break" -> {
                        assertEquals(seed.getString("id"), timer.currentSessionId())
                        assertEquals(TimerMode.BREAK, timer.timerMode.value)
                        assertTrue(timer.isRunning.value)
                        assertEquals("A running break must not add focus", initialActual, progress.actualSeconds)
                    }
                    "manual-break-completion" -> {
                        assertEquals(seed.getString("id"), timer.currentSessionId())
                        assertEquals(TimerMode.FOCUS, timer.timerMode.value)
                        assertFalse("A finished manual break must leave focus paused", timer.isRunning.value)
                        assertEquals(initialActual, progress.actualSeconds)
                    }
                    "pomodoro-chain" -> {
                        assertEquals(seed.getString("id"), timer.currentSessionId())
                        assertEquals(TimerMode.POMODORO, timer.timerMode.value)
                        assertTrue(timer.isRunning.value)
                        assertEquals(1, timer.pomodorosCompleted.value)
                        // First focus ends at 60s, then a 60s break, then the second focus.
                        expectedActual = (60 + (initialActual + elapsed - 120).coerceAtLeast(0)).coerceAtMost(120)
                        assertWithin(expectedActual, progress.actualSeconds, "Pomodoro must exclude the completed break")
                    }
                    else -> {
                        assertEquals(seed.getString("id"), timer.currentSessionId())
                        assertTrue("An ordinary same-boot process kill must resume", timer.isRunning.value)
                        expectedActual = initialActual + elapsed
                        assertWithin(expectedActual, progress.actualSeconds, "Recover the elapsed monotonic interval")
                        assertTrue("The stopped process interval must not disappear", progress.actualSeconds > initialActual)
                    }
                }
                if (case != "focus-completion" || stoppedExplicitly) timer.endAndSave()
            }
            runBlocking { EkagraSessionJournal.get(context).recover().await() }
            val row = readRows().single()
            val saved = gson.fromJson(row.payload, PendingEkagraSessionSave::class.java)
            assertEquals("Recovery and finalization must keep the original session identity", seed.getString("id"), row.id)
            assertEquals("pending", row.state)
            assertWithin(expectedActual, saved.actualDurationSeconds ?: -1, "Exactly one final record must retain all focus")
            if (case == "focus-completion" && !stoppedExplicitly) {
                assertEquals(7200, saved.actualDurationSeconds)
                assertEquals("completed", saved.endReason)
            }
            writeAtomically(resultFile, JSONObject()
                .put("case", case)
                .put("interruption", interruption)
                .put("seedPid", seed.getInt("pid"))
                .put("verificationPid", Process.myPid())
                .put("clientSessionId", saved.clientSessionId)
                .put("initialFocusSeconds", initialActual)
                .put("finalFocusSeconds", saved.actualDurationSeconds)
                .put("endReason", saved.endReason)
                .put("rowCount", 1)
                .put("passed", true))
        } finally {
            TimerService.live?.let { timer -> onMain { timer.endAndSave() } }
            runBlocking { EkagraSessionJournal.get(context).recover().await() }
            scenario?.close()
            context.stopService(Intent(context, TimerService::class.java))
            awaitServiceStopped()
            val db = database()
            try { db.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(owner)) }
            finally { db.close() }
            runBlocking { SafarDataStore(context).setUserId("") }
        }
    }

    private fun requireDisposableOfflineEmulator() {
        require(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")) {
            "Process-death QA is restricted to disposable emulators"
        }
        require(context.getSystemService(ConnectivityManager::class.java).activeNetwork == null) {
            "Disable emulator Wi-Fi and mobile data before running this test"
        }
        runBlocking {
            val store = SafarDataStore(context)
            require(store.authToken.first().isNullOrBlank() && store.refreshToken.first().isNullOrBlank()) {
                "This harness must never run with account credentials"
            }
        }
    }

    private fun awaitTimer(): TimerService {
        repeat(200) {
            TimerService.live?.let { timer -> if (field(timer, "recoveryReady") == true) return timer }
            Thread.sleep(50)
        }
        error("Timer recovery did not finish")
    }

    private fun awaitServiceStopped() {
        repeat(100) {
            if (TimerService.live == null) return
            Thread.sleep(50)
        }
        error("Timer service did not stop after QA cleanup")
    }

    private fun database() = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
    private fun readRows(): List<JournalSession> {
        val db = database()
        return try { runBlocking { db.journal().rows(owner) } } finally { db.close() }
    }

    private fun persist(timer: TimerService) {
        TimerService::class.java.getDeclaredMethod("persistTimerState", Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(timer, false)
    }
    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun field(instance: Any, name: String) = TimerService::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.get(instance)
    private fun setField(instance: Any, name: String, value: Any) = TimerService::class.java.getDeclaredField(name)
        .apply { isAccessible = true }.set(instance, value)
    private fun assertWithin(expected: Int, actual: Int, message: String) {
        assertTrue("$message: expected $expected +/- 2 seconds, got $actual", actual in (expected - 2)..(expected + 2))
    }
    private fun writeAtomically(file: File, value: JSONObject) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.outputStream().use { stream ->
            stream.write(value.toString().toByteArray())
            stream.fd.sync()
        }
        check(temporary.renameTo(file))
    }
}
