package com.safarparmar.app.notifications

import android.content.Context
import android.util.Log
import com.safarparmar.app.BuildConfig
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safarparmar.app.data.local.SafarDataStore
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

        SafarNotificationManager(applicationContext).showStudyReminder(
            title = "Your study time is ready",
            body = "Start a 25-minute Ekagra session.",
            deepLink = "safar://ekagra",
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_study_reminder"

        fun schedule(
            context: Context,
            reminderTime: String,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        ) {
            val delay = initialDelayMinutes(reminderTime)
            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()

            if (BuildConfig.DEBUG) {
                Log.d("SAFAR_WORK", "schedule $WORK_NAME policy=$policy delayMin=$delay")
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
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
