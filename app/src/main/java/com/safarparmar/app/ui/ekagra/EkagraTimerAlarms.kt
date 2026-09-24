package com.safarparmar.app.ui.ekagra

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.safarparmar.app.data.local.SafarDataStore
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
                    EkagraSessionJournal.get(context).recover().await()
                    EkagraSessionSaveWorker.enqueue(context)
                }
            } catch (error: Exception) {
                EkagraDiagnostics.record(context, "alarm_recovery_failed", error.javaClass.simpleName)
            } finally {
                if (lock.isHeld) lock.release()
                result.finish()
            }
        }
    }
}
