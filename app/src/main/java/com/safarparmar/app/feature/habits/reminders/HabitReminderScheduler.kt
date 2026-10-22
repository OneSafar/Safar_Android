package com.safarparmar.app.feature.habits.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.HabitRemoteRecord
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cache: HabitReminderStore,
    private val accounts: SafarDataStore,
) {
    private val mutex = Mutex()
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val account = combine(accounts.userId, accounts.isLoggedIn, accounts.notificationsEnabled) { id, loggedIn, enabled ->
        id?.takeIf { loggedIn && enabled && it.isNotBlank() }
    }.distinctUntilChanged()

    suspend fun observeAccountAndSettings() {
        account.onEach { reconcile() }.retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            Log.w("HabitReminder", "Retrying reminder reconciliation", cause)
            delay(30_000)
            true
        }.collect {}
    }

    /** Called after every confirmed full server snapshot, including completion/edit/delete saves. */
    suspend fun replace(owner: String, habits: List<HabitRemoteRecord>) = mutex.withLock {
        if (accounts.userId.first() != owner || !accounts.isLoggedIn.first()) return@withLock
        val old = cache.read()
        if (old != null && old.owner != owner) cache.clear()
        val confirmed = habits.filterNot { it.deleted }
        // Persist first: a process death here leaves old alarms able to read the new projection.
        cache.replace(HabitReminderSnapshot(owner, confirmed))
        old?.habits?.filter { previous -> old.owner != owner || confirmed.none { it.id == previous.id } }
            ?.forEach { cancelForHabit(old.owner, it.id) }
        reconcileLocked()
    }

    suspend fun reconcile() = mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked() {
        val snapshot = cache.read() ?: return
        val owner = account.first()
        if (owner != snapshot.owner) {
            snapshot.habits.forEach { cancelForHabit(snapshot.owner, it.id) }
            // Turning notifications off retains the confirmed projection for re-enabling.
            if (accounts.userId.first() != snapshot.owner || !accounts.isLoggedIn.first()) cache.clear()
            return
        }
        val now = ZonedDateTime.now()
        snapshot.habits.forEach { habit ->
            if (!HabitReminderPolicy.scheduled(habit, now.toLocalDate())) notificationManager.cancel(tag(owner, habit.id), 0)
            schedule(owner, habit, now)
        }
    }

    suspend fun deliver(owner: String, id: String, date: String, token: String) = mutex.withLock {
        val snapshot = cache.read()
        val habit = snapshot?.takeIf { it.owner == owner }?.habits?.firstOrNull { it.id == id }
        if (account.first() != owner || habit == null) {
            cancelForHabit(owner, id)
            return@withLock
        }
        val now = ZonedDateTime.now()
        // Re-arm before posting: process death or a disk failure during delivery
        // must not remove the next occurrence of this recurring reminder.
        schedule(owner, habit, now)
        try {
            // Old alarms cannot deliver after a reminder, schedule or completion was edited.
            if (cache.token(habit) == token && HabitReminderPolicy.due(habit, date, now, cache.delivered(owner, id))) {
                SafarNotificationChannels.createAll(context)
                val notifications = SafarNotificationManager(context)
                if ((Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                    notifications.evaluateNotificationAvailability(SafarNotificationChannels.STUDY_REMINDERS).allowed && account.first() == owner) {
                    try {
                        notificationManager.notify(tag(owner, id), 0, notifications.buildNotification(
                            title = habit.name,
                            body = context.getString(R.string.habit_reminder_body),
                            channelId = SafarNotificationChannels.STUDY_REMINDERS,
                            deepLink = "safar://habits",
                        ))
                        cache.markDelivered(owner, id, date)
                        // An account change while posting must also remove the just-posted notification.
                        if (account.first() != owner) notificationManager.cancel(tag(owner, id), 0)
                    } catch (_: SecurityException) { /* Notification permission was revoked during delivery. */ }
                }
            }
        } finally {
            if (account.first() != owner) cancelForHabit(owner, id)
        }
    }

    private fun schedule(owner: String, habit: HabitRemoteRecord, now: ZonedDateTime) {
        val next = HabitReminderPolicy.next(habit, now, cache.delivered(owner, habit.id))
        if (next == null) {
            cancelAlarm(owner, habit.id)
            return
        }
        val intent = alarmIntent(context, owner, habit.id).apply {
            putExtra("owner", owner)
            putExtra("habit", habit.id)
            putExtra("date", next.toLocalDate().toString())
            putExtra("token", cache.token(habit))
        }
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Inexact alarms work without special exact-alarm access and survive ordinary process death.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
    }

    fun cancelForHabit(owner: String, id: String) {
        cancelAlarm(owner, id)
        notificationManager.cancel(tag(owner, id), 0)
    }

    private fun cancelAlarm(owner: String, id: String) {
        PendingIntent.getBroadcast(context, 0, alarmIntent(context, owner, id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        fun tag(owner: String, id: String) = "habit:$owner:$id"
        fun alarmIntent(context: Context, owner: String, id: String) = Intent(context, HabitReminderReceiver::class.java).apply {
            data = Uri.Builder().scheme("safar-habit-alarm").authority("reminder").appendPath(owner).appendPath(id).build()
        }
    }
}
