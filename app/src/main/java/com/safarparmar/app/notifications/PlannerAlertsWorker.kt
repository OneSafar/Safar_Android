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
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
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
                    val dedupeKey = PlannerAlertDedupe.overdueKey(plan.id, today)
                    if (!dataStore.hasPlannerAlertDedupeKey(dedupeKey)) {
                        notificationManager.show(
                            title = "Overdue Tasks: ${plan.title}",
                            body = "You have ${overdueTopics.size} tasks that need your attention.",
                            channelId = SafarNotificationChannels.STUDY_REMINDERS,
                            deepLink = "safar://studyplanner",
                        )
                        dataStore.addPlannerAlertDedupeKey(dedupeKey)
                        notified = true
                    }
                }

                // 2. Exam Countdown
                if (!notified && plan.examDate != null) {
                    val examDate = runCatching { LocalDate.parse(plan.examDate.substring(0, 10)) }.getOrNull()
                    if (examDate != null) {
                        val daysUntil = ChronoUnit.DAYS.between(today, examDate)
                        if (daysUntil == 30L || daysUntil == 7L || daysUntil == 1L) {
                            val dedupeKey = PlannerAlertDedupe.examCountdownKey(plan.id, daysUntil)
                            if (!dataStore.hasPlannerAlertDedupeKey(dedupeKey)) {
                                notificationManager.show(
                                    title = "Exam approaching!",
                                    body = "Your exam for ${plan.title} is in $daysUntil days.",
                                    channelId = SafarNotificationChannels.STUDY_REMINDERS,
                                    deepLink = "safar://studyplanner",
                                )
                                dataStore.addPlannerAlertDedupeKey(dedupeKey)
                                notified = true
                            }
                        }
                    }
                }

                // 3. Pace Warning
                if (!notified && plan.examDate != null && plan.dailyGoal != null && plan.dailyGoal > 0) {
                    val remainingTopics = allTopics.count { it.status != TopicStatus.DONE }
                    val examDate = runCatching { LocalDate.parse(plan.examDate.substring(0, 10)) }.getOrNull()
                    if (examDate != null && examDate.isAfter(today) && remainingTopics > 0) {
                        val daysUntil = ChronoUnit.DAYS.between(today, examDate)
                        if (daysUntil <= 0L) continue
                        val requiredPace = remainingTopics.toDouble() / daysUntil
                        if (requiredPace > plan.dailyGoal) {
                            val dedupeKey = PlannerAlertDedupe.paceWarningKey(plan.id, today)
                            if (!dataStore.hasPlannerAlertDedupeKey(dedupeKey)) {
                                notificationManager.show(
                                    title = plan.title,
                                    body = "You're behind schedule, time to catch up!",
                                    channelId = SafarNotificationChannels.STUDY_REMINDERS,
                                    deepLink = "safar://studyplanner",
                                )
                                dataStore.addPlannerAlertDedupeKey(dedupeKey)
                                notified = true
                            }
                        }
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "planner_alerts_worker"

        fun schedule(
            context: Context,
            reminderTime: String,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        ) {
            val delay = initialDelayMinutes(reminderTime)
            val request = PeriodicWorkRequestBuilder<PlannerAlertsWorker>(24, TimeUnit.HOURS)
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
            val parsed = runCatching { LocalTime.parse(reminderTime) }.getOrDefault(LocalTime.of(8, 0))
            val now = LocalDateTime.now()
            var next = now.withHour(parsed.hour).withMinute(parsed.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes().coerceAtLeast(1)
        }
    }
}
