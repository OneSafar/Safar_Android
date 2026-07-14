package com.safarparmar.app.ui.ekagra

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.domain.repository.EkagraRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class EkagraSessionSaveWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EkagraSessionSaveEntryPoint {
        fun getEkagraRepository(): EkagraRepository
    }

    override suspend fun doWork(): Result {
        val allSucceeded = drainPendingSaves(applicationContext)
        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "EkagraSaveWorker"
        private const val WORK_NAME = "ekagra_pending_session_save"

        private fun debugLog(message: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, message)
        }

        /**
         * Uploads every queued session, removing each one from the durable queue as
         * it succeeds. Returns true only if nothing was left behind.
         *
         * Shared by [doWork] and by TimerService's immediate post-completion flush.
         * The immediate path matters: relying solely on WorkManager means a session
         * that just finished isn't uploaded until the OS decides to run the job, and
         * aggressive OEM battery managers (Xiaomi/Oppo/Vivo) routinely defer or kill
         * that job — losing the session. Flushing inline while the foreground service
         * is still demonstrably alive gets the common case saved immediately; the
         * queue + WorkManager remain the retry path for offline/failure cases.
         */
        suspend fun drainPendingSaves(context: Context): Boolean {
            val pending = EkagraPendingSessionSaveStore.getAll(context)
            if (pending.isEmpty()) return true

            val repository = EntryPointAccessors
                .fromApplication(context, EkagraSessionSaveEntryPoint::class.java)
                .getEkagraRepository()

            var failed = false
            pending.forEach { session ->
                when (
                    repository.saveSession(
                        clientSessionId = session.clientSessionId,
                        mode = session.mode,
                        startedAt = session.startedAt,
                        endedAt = session.endedAt,
                        plannedDurationMinutes = session.plannedDurationMinutes,
                        actualDurationMinutes = session.actualDurationMinutes,
                        actualDurationSeconds = session.actualDurationSeconds,
                        goalId = session.goalId,
                        goalTitle = session.goalTitle,
                        topicId = session.topicId,
                        planId = session.planId,
                        topicTitle = session.topicTitle,
                        // Crash-recovery save: the user never confirmed the "mark topic
                        // done" checkbox, so we preserve the association (crediting the
                        // time) but never auto-complete the topic.
                        markTopicDone = false,
                        taskTitle = session.taskTitle,
                        shieldEnabled = session.shieldEnabled,
                    )
                ) {
                    is Resource.Success -> {
                        EkagraPendingSessionSaveStore.remove(context, session.clientSessionId)
                        debugLog("Saved pending Ekagra session ${session.clientSessionId}")
                    }
                    is Resource.Error,
                    is Resource.Loading -> {
                        failed = true
                        debugLog("Pending Ekagra session save failed: ${session.clientSessionId}")
                    }
                }
            }

            return !failed
        }

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<EkagraSessionSaveWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
