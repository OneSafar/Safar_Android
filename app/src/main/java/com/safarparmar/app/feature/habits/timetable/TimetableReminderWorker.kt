package com.safarparmar.app.feature.habits.timetable

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit

/** WorkManager reminders survive process death/reboot; Android may defer their delivery. */
class TimetableReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Dependencies { fun timetableRepository(): TimetableRepository }
    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).timetableRepository()
        val owner = inputData.getString("owner") ?: return Result.success()
        if (repository.owner.first() != owner) return Result.success()
        val id = inputData.getString("id") ?: return Result.success()
        val session = repository.sessions.first().find { it.id == id } ?: return Result.success()
        val date = inputData.getString("date")?.let(LocalDate::parse) ?: return Result.success()
        if (session.reminderMinutes == null) return Result.success()
        val now = ZonedDateTime.now()
        val end = date.atTime(session.endMinute / 60, session.endMinute % 60).atZone(now.zone)
        if (!session.isDone(date) && session.occursOn(date) && now < end &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("timetable", "Timetable reminders", NotificationManager.IMPORTANCE_DEFAULT))
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(com.safarparmar.app.notifications.NotificationDeepLinkHandler.EXTRA_ROUTE, com.safarparmar.app.ui.navigation.Routes.HABIT_TRACKER)
            }
            val pending = PendingIntent.getActivity(applicationContext, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(id.hashCode(), NotificationCompat.Builder(applicationContext, "timetable")
                .setSmallIcon(R.drawable.ic_safar_notification_sparkle).setContentTitle(session.title)
                .setContentText("${session.subject} · ${sessionTimeRange(session)}")
                .setContentIntent(pending).setAutoCancel(true).build())
        }
        schedule(applicationContext, owner, session, maxOf(date.plusDays(1), now.toLocalDate()), append = true)
        return Result.success()
    }
    companion object {
        private fun name(owner: String, id: String) = "timetable:$owner:$id"
        fun cancel(context: Context, owner: String, id: String) { WorkManager.getInstance(context).cancelUniqueWork(name(owner, id)) }
        fun schedule(context: Context, owner: String, session: TimetableSession, from: LocalDate = LocalDate.now(), append: Boolean = false) {
            val now = ZonedDateTime.now()
            val next = nextTimetableReminder(session, now, from)
            if (next == null) {
                if (!append) cancel(context, owner, session.id)
                return
            }
            val request = OneTimeWorkRequestBuilder<TimetableReminderWorker>()
                .setInitialDelay(Duration.between(now, next.second).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("owner" to owner, "id" to session.id, "date" to next.first.toString())).build()
            WorkManager.getInstance(context).enqueueUniqueWork(name(owner, session.id), if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
        }
    }
}
