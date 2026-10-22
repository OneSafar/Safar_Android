package com.safarparmar.app.ui.ekagra

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.safarparmar.app.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** OS evidence is correlated with this checkpoint, never an unrelated previous app exit. */
internal data class EkagraProcessExitRecord(
    val timestampMs: Long,
    val reason: Int,
    val status: Int,
    val importance: Int,
    val pid: Int,
    val processName: String,
) {
    val identity: String get() = "$timestampMs:$pid:$reason"
    val isUserStopped: Boolean get() = reason == ApplicationExitInfo.REASON_USER_REQUESTED ||
        reason == ApplicationExitInfo.REASON_USER_STOPPED || reason == ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE
}

internal fun relevantEkagraProcessExit(
    records: List<EkagraProcessExitRecord>,
    processName: String,
    checkpointAtMs: Long,
    nowMs: Long,
): EkagraProcessExitRecord? = records
    .filter { it.processName == processName && it.timestampMs in checkpointAtMs..nowMs }
    // The first exit after the durable checkpoint interrupted that timer. A later
    // failed app launch must not replace the original evidence.
    .minByOrNull { it.timestampMs }

/**
 * Bounded local evidence plus one uploaded nonfatal per relevant process exit.
 * Breadcrumbs alone are not uploaded by Crashlytics until another crash occurs.
 * All preference writes and OS history queries run off the service main thread.
 */
internal object EkagraDiagnostics {
    private const val PREFS = "ekagra_reliability_diagnostics"
    private const val EVENTS = "events"
    private const val REPORTED_EXITS = "reported_exits"
    private const val MAX_EVENTS = 64
    private const val MAX_LINE_LENGTH = 1600
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Channel<() -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (command in commands) {
                // Diagnostics must never prevent the durable timer from recovering.
                runCatching(command).onFailure { Log.w("EkagraReliability", "Could not persist diagnostic", it) }
            }
        }
    }

    fun record(context: Context, event: String, detail: String = "") {
        val line = line(event, detail)
        Log.i("EkagraReliability", line)
        val appContext = context.applicationContext
        commands.trySend {
            persistBestEffort(appContext, line)
            runCatching { FirebaseCrashlytics.getInstance().log(line) }
        }
    }

    /** A caught service failure still needs an upload when the process never crashes. */
    fun recordFailure(context: Context, event: String, error: Throwable) {
        val evidence = line(event, error.javaClass.simpleName)
        Log.e("EkagraReliability", evidence, error)
        val appContext = context.applicationContext
        commands.trySend {
            persistBestEffort(appContext, evidence)
            runCatching {
                FirebaseCrashlytics.getInstance().apply {
                    log(evidence)
                    recordException(error)
                }
            }
        }
    }

    suspend fun reportRecovery(context: Context, sessionId: String, checkpointAtMs: Long): EkagraProcessExitRecord? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val exit = if (Build.VERSION.SDK_INT >= 30) runCatching {
            val records = appContext.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(null, 0, 16)
                .map { EkagraProcessExitRecord(it.timestamp, it.reason, it.status, it.importance, it.pid, it.processName) }
            relevantEkagraProcessExit(records, appContext.packageName, checkpointAtMs, now)
        }.getOrNull() else null
        val detail = buildString {
            append("session=").append(sessionId.take(120))
            append(" checkpoint_age_ms=").append((now - checkpointAtMs).coerceAtLeast(0))
            if (exit == null) append(" exit=unavailable") else {
                append(" exit=").append(exitReason(exit.reason))
                append(" exit_at_ms=").append(exit.timestampMs)
                append(" status=").append(exit.status)
                append(" importance=").append(exit.importance)
                append(" pid=").append(exit.pid)
            }
            append(" ").append(powerState(appContext))
        }
        val evidence = line("process_recovery", detail)
        val done = CompletableDeferred<Unit>()
        commands.send {
            try {
                val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val reported = jsonStrings(prefs.getString(REPORTED_EXITS, null))
                if (exit == null || exit.identity !in reported) {
                    persistBestEffort(appContext, evidence)
                    Log.i("EkagraReliability", evidence)
                    runCatching {
                        val crashlytics = FirebaseCrashlytics.getInstance()
                        crashlytics.log(evidence)
                        if (exit != null) {
                            crashlytics.recordException(EkagraProcessInterruption(exitReason(exit.reason), evidence))
                            check(prefs.edit().putString(REPORTED_EXITS,
                                JSONArray((reported + exit.identity).takeLast(MAX_EVENTS)).toString()).commit())
                        }
                    }
                }
            } finally {
                done.complete(Unit)
            }
        }
        done.await()
        exit
    }

    /** A readable export for support and device regressions; contains no account or task data. */
    suspend fun recentEvents(context: Context): List<String> {
        val appContext = context.applicationContext
        val result = CompletableDeferred<List<String>>()
        commands.send {
            result.complete(runCatching {
                jsonStrings(appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(EVENTS, null))
            }.getOrDefault(emptyList()))
        }
        return result.await()
    }

    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        commands.send { done.complete(Unit) }
        done.await()
    }

    private fun line(event: String, detail: String): String =
        "at_ms=${System.currentTimeMillis()} event=$event detail=$detail build=${BuildConfig.VERSION_CODE} device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT}"
            .replace('\n', ' ').replace('\r', ' ').take(MAX_LINE_LENGTH)

    private fun append(context: Context, line: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lines = (jsonStrings(prefs.getString(EVENTS, null)) + line).takeLast(MAX_EVENTS)
        check(prefs.edit().putString(EVENTS, JSONArray(lines).toString()).commit())
    }

    private fun persistBestEffort(context: Context, line: String) {
        runCatching { append(context, line) }
            .onFailure { Log.w("EkagraReliability", "Could not persist diagnostic", it) }
    }

    private fun jsonStrings(value: String?): List<String> = runCatching {
        val array = JSONArray(value ?: "[]")
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    private fun powerState(context: Context): String = buildString {
        val power = context.getSystemService(PowerManager::class.java)
        append("battery_exempt=").append(runCatching { power.isIgnoringBatteryOptimizations(context.packageName) }.getOrNull())
        append(" power_save=").append(runCatching { power.isPowerSaveMode }.getOrNull())
        append(" device_idle=").append(runCatching { power.isDeviceIdleMode }.getOrNull())
        append(" background_restricted=").append(if (Build.VERSION.SDK_INT >= 28) runCatching {
            context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
        }.getOrNull() else false)
        append(" exact_alarm=").append(runCatching {
            Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }.getOrNull())
    }

    private fun exitReason(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "anr"
        ApplicationExitInfo.REASON_CRASH -> "crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
        ApplicationExitInfo.REASON_SIGNALED -> "signal"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "user_requested"
        ApplicationExitInfo.REASON_USER_STOPPED -> "user_stopped"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive_resource_usage"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "package_updated"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "package_state_change"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission_change"
        ApplicationExitInfo.REASON_EXIT_SELF -> "exit_self"
        else -> "unknown_$reason"
    }

    private class EkagraProcessInterruption(reason: String, evidence: String) :
        RuntimeException("Ekagra process interruption ($reason): $evidence")
}
