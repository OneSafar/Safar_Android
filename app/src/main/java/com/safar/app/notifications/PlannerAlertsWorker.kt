package com.safar.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safar.app.data.local.SafarDataStore
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.domain.repository.StudyPlannerRepository
import com.safar.app.util.Resource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class PlannerAlertsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PlannerAlertsWorkerEntryPoint {
        fun getStudyPlannerRepository(): StudyPlannerRepository
    }

    override suspend fun doWork(): Result {
        val dataStore = SafarDataStore(applicationContext)
        // Check if notifications and study reminders are enabled
        if (!dataStore.notificationsEnabled.first() || !dataStore.dailyStudyReminderEnabled.first()) {
            return Result.success()
        }

        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, PlannerAlertsWorkerEntryPoint::class.java)
        val repository = entryPoint.getStudyPlannerRepository()

        val result = repository.listPlans()
        if (result is Resource.Success) {
            val plans = result.data
            val today = LocalDate.now()
            val notificationManager = SafarNotificationManager(applicationContext)

            for (plan in plans) {
                var notified = false
                val allTopics = plan.subjects.flatMap { it.chapters }.flatMap { it.topics }

                // 1. Overdue Tasks
                val overdueTopics = allTopics.filter { topic ->
                    val plannedDateStr = topic.plannedDate
                    if (plannedDateStr != null && topic.status != TopicStatus.DONE) {
                        val plannedDate = runCatching { LocalDate.parse(plannedDateStr.substring(0, 10)) }.getOrNull()
                        plannedDate != null && plannedDate.isBefore(today)
                    } else {
                        false
                    }
                }

                if (overdueTopics.isNotEmpty() && !notified) {
                    notificationManager.show(
                        title = "Overdue Tasks: ${plan.title}",
                        body = "You have ${overdueTopics.size} tasks that need your attention.",
                        channelId = SafarNotificationChannels.STUDY_REMINDERS,
                        deepLink = "safar://studyplanner",
                    )
                    notified = true
                }

                // 2. Exam Countdown
                if (!notified && plan.examDate != null) {
                    val examDate = runCatching { LocalDate.parse(plan.examDate.substring(0, 10)) }.getOrNull()
                    if (examDate != null) {
                        val daysUntil = ChronoUnit.DAYS.between(today, examDate)
                        if (daysUntil == 30L || daysUntil == 7L || daysUntil == 1L) {
                            notificationManager.show(
                                title = "Exam approaching!",
                                body = "Your exam for ${plan.title} is in $daysUntil days.",
                                channelId = SafarNotificationChannels.STUDY_REMINDERS,
                                deepLink = "safar://studyplanner",
                            )
                            notified = true
                        }
                    }
                }

                // 3. Pace Warning
                if (!notified && plan.examDate != null && plan.dailyGoal != null && plan.dailyGoal > 0) {
                    val remainingTopics = allTopics.count { it.status != TopicStatus.DONE }
                    val examDate = runCatching { LocalDate.parse(plan.examDate.substring(0, 10)) }.getOrNull()
                    if (examDate != null && examDate.isAfter(today) && remainingTopics > 0) {
                        val daysUntil = ChronoUnit.DAYS.between(today, examDate)
                        val requiredPace = remainingTopics.toDouble() / daysUntil
                        if (requiredPace > plan.dailyGoal) {
                            notificationManager.show(
                                title = "Study Pace Warning",
                                body = "You need to complete ${requiredPace.toInt()} topics/day to finish ${plan.title} on time.",
                                channelId = SafarNotificationChannels.STUDY_REMINDERS,
                                deepLink = "safar://studyplanner",
                            )
                            notified = true
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "planner_alerts_worker"

        fun schedule(context: Context, reminderTime: String) {
            val request = PeriodicWorkRequestBuilder<PlannerAlertsWorker>(24, TimeUnit.HOURS)
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
            val parsed = runCatching { LocalTime.parse(reminderTime) }.getOrDefault(LocalTime.of(8, 0))
            val now = LocalDateTime.now()
            var next = now.withHour(parsed.hour).withMinute(parsed.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes().coerceAtLeast(1)
        }
    }
}
