package com.safarparmar.app.feature.habits.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safarparmar.app.MainActivity
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.HabitDao
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitScheduleRevisionEntity
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: HabitDao,
    private val dataStore: SafarDataStore
) {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll(owner: String) {
        if (!isCurrentOwner(owner)) return
        for (habit in dao.getOwnedHabits(owner)) {
            val remoteId = habit.remoteId ?: continue
            cancelForHabit(owner, remoteId)
            if (habit.isActive && !habit.isArchived && habit.reminderTime != null) scheduleNext(owner, habit)
        }
    }

    suspend fun cancelAllForOwner(owner: String) {
        for (habit in dao.getOwnedHabits(owner)) habit.remoteId?.let { cancelForHabit(owner, it) }
    }

    fun cancelForHabit(owner: String, remoteId: String) {
        val intent = alarmIntent(owner, remoteId, null)
        val pending = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    suspend fun deliver(owner: String, remoteId: String, dueDate: String?) {
        if (!isCurrentOwner(owner)) return
        val habit = dao.getOwnedHabitByRemoteId(owner, remoteId) ?: return
        val today = LocalDate.now()
        val revisions = dao.getScheduleRevisionsForHabit(habit.id)
        val due = runCatching { LocalDate.parse(dueDate) }.getOrNull()
        val scheduledToday = scheduleAt(habit, revisions, today)
        val shouldNotify = due == today && habit.isActive && !habit.isArchived &&
            habit.reminderTime != null && scheduledToday &&
            dao.getCompletion(habit.id, today)?.completed != true &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        if (shouldNotify) {
            SafarNotificationChannels.createAll(context)
            val open = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("safar://app/habit_tracker/$remoteId")
                putExtra(NotificationDeepLinkHandler.EXTRA_ROUTE, Routes.HABIT_TRACKER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentIntent = PendingIntent.getActivity(context, remoteId.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, SafarNotificationChannels.HABIT_REMINDERS)
                .setSmallIcon(R.drawable.ic_safar_notification_sparkle)
                .setContentTitle(habit.name)
                .setContentText("Time for your habit")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(remoteId, 0, notification)
        }
        scheduleNext(owner, habit, ZonedDateTime.now().plusSeconds(1))
    }

    private suspend fun scheduleNext(owner: String, habit: HabitEntity, after: ZonedDateTime = ZonedDateTime.now()) {
        val revisions = dao.getScheduleRevisionsForHabit(habit.id)
        val next = nextOccurrence(habit, revisions, after) ?: return
        val intent = alarmIntent(owner, habit.remoteId!!, next.toLocalDate().toString())
        val pending = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val millis = next.toInstant().toEpochMilli()
        val exactAllowed = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            }
        } else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    private fun alarmIntent(owner: String, remoteId: String, dueDate: String?) =
        Intent(context, HabitReminderReceiver::class.java).apply {
            data = Uri.Builder().scheme("safar-habit-alarm").authority("reminder")
                .appendPath(owner).appendPath(remoteId).build()
            putExtra("owner", owner)
            putExtra("habit_id", remoteId)
            if (dueDate != null) putExtra("due_date", dueDate)
        }

    private suspend fun isCurrentOwner(owner: String): Boolean =
        dataStore.isLoggedIn.first() && dataStore.userId.first() == owner

    companion object {
        internal fun nextOccurrence(habit: HabitEntity, revisions: List<HabitScheduleRevisionEntity>,
                                    after: ZonedDateTime): ZonedDateTime? {
            val time = habit.reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
            if (!habit.isActive || habit.isArchived) return null
            for (offset in 0..7) {
                val day = after.toLocalDate().plusDays(offset.toLong())
                if (!scheduleAt(habit, revisions, day)) continue
                val candidate = day.atTime(time).atZone(after.zone)
                if (candidate.isAfter(after)) return candidate
            }
            return null
        }

        private fun scheduleAt(habit: HabitEntity, revisions: List<HabitScheduleRevisionEntity>, date: LocalDate): Boolean {
            if (date.isBefore(habit.scheduledSince) || (habit.archivedAt != null && date.isAfter(habit.archivedAt))) return false
            val days = revisions.lastOrNull { !it.effectiveFrom.isAfter(date) }?.targetDays ?: habit.targetDays
            return date.dayOfWeek in days
        }
    }
}
