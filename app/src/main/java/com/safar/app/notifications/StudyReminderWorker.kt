package com.safar.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class StudyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dataStore = SafarDataStore(applicationContext)
        if (!dataStore.notificationsEnabled.first() || !dataStore.dailyStudyReminderEnabled.first()) {
            return Result.success()
        }

        SafarNotificationManager(applicationContext).show(
            title = "Your study time is ready",
            body = "Start a 25-minute Ekagra session.",
            channelId = SafarNotificationChannels.STUDY_REMINDERS,
            deepLink = "safar://ekagra",
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_study_reminder"

        fun schedule(context: Context, reminderTime: String) {
            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes(reminderTime), TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun initialDelayMinutes(reminderTime: String): Long {
            val parsed = runCatching { LocalTime.parse(reminderTime) }.getOrDefault(LocalTime.of(19, 0))
            val now = LocalDateTime.now()
            var next = now.withHour(parsed.hour).withMinute(parsed.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes().coerceAtLeast(1)
        }
    }
}
