package com.safarparmar.app.ui.ekagra

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.R
import com.safarparmar.app.data.local.TimerAlertStyle
import com.safarparmar.app.notifications.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

internal object EkagraTimerAlarms {
    fun exactAllowed(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pending(context: Context, id: String = "", generation: Long = 0): PendingIntent = PendingIntent.getBroadcast(
        context, 7010, Intent(context, EkagraTimerAlarmReceiver::class.java)
            .setAction("com.safar.ekagra.DEADLINE").putExtra("session", id).putExtra("generation", generation),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(context: Context, id: String, generation: Long, elapsedDeadline: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context, id, generation)
        val whenElapsed = elapsedDeadline.coerceAtLeast(SystemClock.elapsedRealtime() + 1)
        try {
            if (exactAllowed(context)) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenElapsed, intent)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenElapsed, intent)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenElapsed, intent)
        }
    }

    fun cancel(context: Context) { runCatching { context.getSystemService(AlarmManager::class.java).cancel(pending(context)) } }

    suspend fun showRecovered(context: Context) = runCatching {
        cancel(context)
        runCatching {
            val notifications = context.getSystemService(android.app.NotificationManager::class.java)
            listOf(1001, 1005, 1006).forEach(notifications::cancel)
        }
        val style = SafarDataStore(context).timerAlertStyle.first()
        runCatching {
            SafarNotificationChannels.createAll(context)
            SafarNotificationManager(context).show(
                title = "Your interrupted session was saved",
                body = "Recorded study time is safe on this phone and will sync when you're online.",
                channelId = if (style == TimerAlertStyle.VIBRATE) SafarNotificationChannels.EKAGRA_VIBRATE_ALERT else SafarNotificationChannels.EKAGRA_ALERT,
                deepLink = "safar://ekagra", notificationId = 1007, priority = NotificationCompat.PRIORITY_HIGH,
            )
        }
    }.let { Unit }

    /** Reboot or an explicit app stop leaves the original session available, paused. */
    suspend fun showRecoveryAvailable(context: Context) = runCatching {
        SafarNotificationChannels.createAll(context)
        SafarNotificationManager(context).show(
            title = context.getString(R.string.ekagra_resume_available_title),
            body = context.getString(R.string.ekagra_resume_available_body),
            channelId = SafarNotificationChannels.FOCUS_TIMER,
            deepLink = "safar://ekagra", notificationId = 1007,
            priority = NotificationCompat.PRIORITY_DEFAULT,
        )
    }.let { Unit }
}

class EkagraTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val lock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "safar:EkagraDeadline")
        lock.acquire(10_000)
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val service = TimerService.live
                if (service != null && intent.action == "com.safar.ekagra.DEADLINE") {
                    service.onScheduledAlarm(intent.getStringExtra("session").orEmpty(), intent.getLongExtra("generation", -1))
                    delay(1500) // allow the next display tick to commit its transition
                } else if (service != null && intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                    service.scheduleTimerAlarms()
                } else {
                    // A dead process is not an End action. Keep its durable active
                    // identity and restore through the foreground service instead of
                    // finalizing the last five-second checkpoint as "interrupted".
                    val pending = EkagraSessionJournal.get(context).peekRecovery()
                    val state = pending?.recovery
                    if (pending == null || state == null) {
                        EkagraSessionSaveWorker.enqueue(context)
                        return@launch
                    }
                    if (intent.action == "com.safar.ekagra.DEADLINE" &&
                        intent.getStringExtra("session") != pending.clientSessionId) return@launch

                    val bootCount = runCatching {
                        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
                    }.getOrNull()
                    val sameBoot = bootCount != null && bootCount == state.checkpointBootCount &&
                        SystemClock.elapsedRealtime() >= state.checkpointElapsedMs
                    val exit = EkagraDiagnostics.reportRecovery(context, pending.clientSessionId, state.checkpointAtMs)
                    val runningOrTimedUnlock = state.isRunning || state.quickUnlockUntilElapsedMs > 0L
                    if (intent.action == "com.safar.ekagra.DEADLINE" && sameBoot &&
                        runningOrTimedUnlock && exit?.isUserStopped != true) {
                        // Exact timer alarms are an allowed background FGS entry.
                        // An inexact fallback may be denied; the journal remains
                        // active for the next permitted restart or foreground open.
                        try {
                            ContextCompat.startForegroundService(context,
                                Intent(context, TimerService::class.java).setAction(TimerService.ACTION_RECOVER))
                        } catch (error: Exception) {
                            EkagraDiagnostics.recordFailure(context, "foreground_recovery_denied", error)
                            EkagraTimerAlarms.showRecoveryAvailable(context)
                        }
                    } else {
                        // BOOT_COMPLETED/package replacement never launches a
                        // foreground timer. Powered-off time earns no focus credit.
                        EkagraTimerAlarms.showRecoveryAvailable(context)
                    }
                }
            } catch (error: Exception) {
                EkagraDiagnostics.recordFailure(context, "alarm_recovery_failed", error)
            } finally {
                if (lock.isHeld) lock.release()
                result.finish()
            }
        }
    }
}
