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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import com.safarparmar.app.data.local.SafarDataStore

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
        val allSucceeded = runCatching { drainPendingSaves(applicationContext) }.getOrDefault(false)
        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        private val drainLock = Mutex()
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
        suspend fun drainPendingSaves(context: Context): Boolean = drainLock.withLock {
            val pending = EkagraPendingSessionSaveStore.getAll(context)
            if (pending.isEmpty()) return@withLock true

            var failed = false
            pending.forEach { session ->
                if (session.ownerId != SafarDataStore(context).userId.first()) return@withLock false
                when (val result = uploadPending(context, session)) {
                    is Resource.Success -> {
                        debugLog("Saved pending Ekagra session ${session.clientSessionId}")
                    }
                    is Resource.Error,
                    is Resource.Loading -> {
                        failed = true
                        EkagraDiagnostics.record(context, "upload_failed", if (result is Resource.Error) result.message?.substringBefore(":").orEmpty() else "pending")
                        debugLog("Pending Ekagra session save failed: ${session.clientSessionId}")
                    }
                }
            }

            !failed
        }

        suspend fun uploadOne(context: Context, session: PendingEkagraSessionSave): Resource<com.safarparmar.app.data.remote.dto.EkagraSession> =
            drainLock.withLock { uploadPending(context, session) }

        private suspend fun uploadPending(context: Context, session: PendingEkagraSessionSave): Resource<com.safarparmar.app.data.remote.dto.EkagraSession> {
            if (session.ownerId != SafarDataStore(context).userId.first()) return Resource.Error("Session belongs to another account")
            val repository = EntryPointAccessors
                .fromApplication(context, EkagraSessionSaveEntryPoint::class.java)
                .getEkagraRepository()

            val result = repository.saveSession(
                        clientSessionId = session.clientSessionId,
                        mode = session.mode,
                        endReason = session.endReason,
                        ownerId = session.ownerId,
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
                        markTopicDone = session.markTopicDone,
                        markGoalComplete = session.markGoalComplete,
                        taskTitle = session.taskTitle,
                        shieldEnabled = session.shieldEnabled,
                    )
            if (result is Resource.Success) EkagraSessionJournal.get(context).acknowledge(session, result.data.id)
            return result
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
                // Always leave a follow-up drain behind. KEEP can discard this
                // request when a worker is already running after that worker took
                // its queue snapshot, stranding the newly added session indefinitely.
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}
