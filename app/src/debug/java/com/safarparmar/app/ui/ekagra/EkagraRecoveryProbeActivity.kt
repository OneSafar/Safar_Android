package com.safarparmar.app.ui.ekagra

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/** Debug-only, offline-emulator probe. It never exists in a production APK. */
class EkagraRecoveryProbeActivity : ComponentActivity() {
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) = Unit
        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val case = intent.getStringExtra("case") ?: "focus"
            val owner = "process-death-qa-$case"
            val action = intent.getStringExtra("probe") ?: "seed"
            val resultFile = File(filesDir, "ekagra-process-death-result.json")
            try {
                check(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
                check(getSystemService(ConnectivityManager::class.java).activeNetwork == null)
                val store = SafarDataStore(this@EkagraRecoveryProbeActivity)
                check(store.authToken.first().isNullOrBlank() && store.refreshToken.first().isNullOrBlank())
                val existingOwner = store.userId.first()
                check(existingOwner.isNullOrBlank() || existingOwner.startsWith("process-death-qa-"))
                if (action == "seed") {
                    store.setUserId(owner)
                    store.setAutoStartBreak(case == "pomodoro-chain")
                }
                startService(Intent(this@EkagraRecoveryProbeActivity, TimerService::class.java))
                bound = bindService(Intent(this@EkagraRecoveryProbeActivity, TimerService::class.java), connection, Context.BIND_AUTO_CREATE)
                var timer: TimerService? = null
                repeat(200) {
                    if (timer == null) {
                        TimerService.live?.takeIf {
                            field(it, "recoveryReady") == true && field(it, "signedInUserId") == owner
                        }?.let { ready -> timer = ready }
                        if (timer == null) delay(50)
                    }
                }
                val service = requireNotNull(timer)
                val seedFile = File(filesDir, "ekagra-process-death-seed.json")
                if (action == "seed") {
                    // A prior failed QA run may have left synthetic rows. Never
                    // touch other owners or run this path with real credentials.
                    service.endAndSave()
                    EkagraSessionJournal.get(this@EkagraRecoveryProbeActivity).recover().await()
                    val cleanup = Room.databaseBuilder(this@EkagraRecoveryProbeActivity,
                        EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            cleanup.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(owner))
                        }
                    } finally { cleanup.close() }
                    if (case == "pomodoro-chain") {
                        check(service.startPomodoroSession(PomodoroStyle.CUSTOM, 2, 1, 1, "Process death QA", null, null) != null)
                    } else {
                        service.setDuration(if (case == "stopwatch") TimerMode.STOPWATCH else TimerMode.FOCUS, if (case == "stopwatch") 0 else 7200)
                        service.prepareAutoSaveSession("Process death QA",
                            goalId = "qa-goal".takeIf { case == "goal" },
                            goalTitle = "Goal QA".takeIf { case == "goal" },
                            topicId = "qa-topic".takeIf { case == "topic" },
                            planId = "qa-plan".takeIf { case == "topic" },
                            topicTitle = "Topic QA".takeIf { case == "topic" }, forceNew = true)
                        service.start()
                    }
                    check(service.isRunning.value)
                    val elapsed = when (case) { "focus-completion" -> 7194; "pomodoro-chain" -> 54; else -> 720 }
                    setField(service, "periodStartElapsed", SystemClock.elapsedRealtime() - elapsed * 1000L)
                    service.focusProgressSnapshot()
                    when (case) {
                        "paused" -> service.pause()
                        "manual-break", "manual-break-completion" -> {
                            check(service.startBreak(TimerMode.BREAK, 300))
                            if (case == "manual-break-completion") {
                                setField(service, "periodStartElapsed", SystemClock.elapsedRealtime() - 294_000L)
                                service.focusProgressSnapshot()
                            }
                        }
                    }
                    TimerService::class.java.getDeclaredMethod("persistTimerState", Boolean::class.javaPrimitiveType)
                        .apply { isAccessible = true }.invoke(service, false)
                    EkagraSessionJournal.get(this@EkagraRecoveryProbeActivity).recover().await()
                    val saved = requireNotNull(EkagraSessionJournal.get(this@EkagraRecoveryProbeActivity).peekRecovery())
                    val state = requireNotNull(saved.recovery)
                    write(seedFile, JSONObject().put("case", case).put("owner", owner).put("id", saved.clientSessionId)
                        .put("pid", Process.myPid()).put("actualSeconds", saved.actualDurationSeconds)
                        .put("checkpointElapsedMs", state.checkpointElapsedMs).put("mode", service.timerMode.value.name)
                        .put("running", service.isRunning.value))
                } else {
                    val seed = JSONObject(seedFile.readText())
                    check(seed.getInt("pid") != Process.myPid())
                    val interruption = intent.getStringExtra("interruption") ?: "kill"
                    val stopped = interruption != "kill"
                    val initial = seed.getInt("actualSeconds")
                    val elapsed = ((SystemClock.elapsedRealtime() - seed.getLong("checkpointElapsedMs")) / 1000L).toInt()
                    val progress = service.focusProgressSnapshot()
                    if (case == "goal") {
                        val draft = requireNotNull(service.activeSessionDraft())
                        check(draft.userId == owner && draft.goalId == "qa-goal" && draft.goalTitle == "Goal QA")
                    }
                    if (case == "topic") {
                        val topic = requireNotNull(service.plannerTopicMetadata())
                        check(topic.topicId == "qa-topic" && topic.planId == "qa-plan")
                    }
                    var expected = initial
                    if (stopped || case == "paused" || case.startsWith("manual-break")) {
                        check(abs(progress.actualSeconds - initial) <= 2)
                        if (stopped || case == "paused" || case == "manual-break-completion") check(!service.isRunning.value)
                    } else if (case == "focus-completion") {
                        expected = 7200
                        check(!service.isRunning.value)
                    } else {
                        check(service.isRunning.value)
                        expected = if (case == "pomodoro-chain") (60 + (initial + elapsed - 120).coerceAtLeast(0)).coerceAtMost(120) else initial + elapsed
                        check(abs(progress.actualSeconds - expected) <= 2) { "Expected $expected seconds, got ${progress.actualSeconds}" }
                    }
                    if (case != "focus-completion" || stopped) {
                        check(service.currentSessionId() == seed.getString("id"))
                        service.endAndSave()
                    }
                    EkagraSessionJournal.get(this@EkagraRecoveryProbeActivity).recover().await()
                    val database = Room.databaseBuilder(this@EkagraRecoveryProbeActivity, EkagraJournalDatabase::class.java, "ekagra-journal.db").build()
                    try {
                        val rows = database.journal().rows(owner)
                        check(rows.size == 1 && rows.single().state == "pending")
                        val saved = Gson().fromJson(rows.single().payload, PendingEkagraSessionSave::class.java)
                        check(saved.clientSessionId == seed.getString("id"))
                        check(abs(requireNotNull(saved.actualDurationSeconds) - expected) <= 2)
                        val result = JSONObject().put("case", case).put("interruption", interruption)
                            .put("seedPid", seed.getInt("pid")).put("verificationPid", Process.myPid())
                            .put("clientSessionId", saved.clientSessionId).put("initialFocusSeconds", initial)
                            .put("finalFocusSeconds", saved.actualDurationSeconds).put("rowCount", 1).put("passed", true)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            database.openHelper.writableDatabase.delete("sessions", "owner = ?", arrayOf(owner))
                        }
                        store.setUserId("")
                        write(resultFile, result)
                    } finally { database.close() }
                }
            } catch (error: Throwable) {
                android.util.Log.e("EkagraRecoveryProbe", "Probe failed", error)
                write(resultFile, JSONObject().put("passed", false).put("error", error.toString()))
            } finally {
                if (bound) { unbindService(connection); bound = false }
                finish()
            }
        }
    }
    private fun field(timer: TimerService, name: String) = TimerService::class.java.getDeclaredField(name).apply { isAccessible = true }.get(timer)
    private fun setField(timer: TimerService, name: String, value: Any) = TimerService::class.java.getDeclaredField(name).apply { isAccessible = true }.set(timer, value)
    private fun write(file: File, data: JSONObject) {
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.outputStream().use { it.write(data.toString().toByteArray()); it.fd.sync() }
        check(temp.renameTo(file))
    }
}
