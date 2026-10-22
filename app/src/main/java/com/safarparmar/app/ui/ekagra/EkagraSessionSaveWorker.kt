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
        val allSucceeded = drainPendingSaves(applicationContext)
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
            attemptEkagraSessionSync(onFailure = { error ->
                EkagraDiagnostics.record(context, "sync_storage_failed", error.javaClass.simpleName)
                false
            }) {
                val pending = EkagraPendingSessionSaveStore.getAll(context)
                if (pending.isEmpty()) return@attemptEkagraSessionSync true

                var failed = false
                pending.forEach { session ->
                    if (session.ownerId != SafarDataStore(context).userId.first()) return@attemptEkagraSessionSync false
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
        }

        suspend fun uploadOne(context: Context, session: PendingEkagraSessionSave): Resource<com.safarparmar.app.data.remote.dto.EkagraSession> =
            drainLock.withLock {
                attemptEkagraSessionSync(onFailure = { error ->
                    EkagraDiagnostics.record(context, "sync_storage_failed", error.javaClass.simpleName)
                    Resource.Error("Study time could not be synced. Free up storage and retry from History.")
                }) { uploadPending(context, session) }
            }

        suspend fun linkLocalSession(context: Context, id: String, goalId: String, goalTitle: String?, markComplete: Boolean): Resource<Unit> = drainLock.withLock {
            attemptEkagraSessionSync(onFailure = { error ->
                EkagraDiagnostics.record(context, "sync_storage_failed", error.javaClass.simpleName)
                Resource.Error("Session could not be linked. Free up storage and retry from History.")
            }) {
                val account = SafarDataStore(context).userId.first().orEmpty()
                val journal = EkagraSessionJournal.get(context)
                val session = journal.find(id, account) ?: return@attemptEkagraSessionSync Resource.Error("Session not found")
                if (journal.setPendingGoal(id, account, goalId, goalTitle, markComplete)) {
                    // The choice is durable even when offline; the worker retries the save.
                    uploadPending(context, session)
                    Resource.Success(Unit)
                } else {
                    val serverId = session.serverId ?: return@attemptEkagraSessionSync Resource.Error("Session has not uploaded")
                    EntryPointAccessors.fromApplication(context, EkagraSessionSaveEntryPoint::class.java)
                        .getEkagraRepository().linkSessionToGoal(serverId, goalId, markComplete)
                }
            }
        }

        private suspend fun uploadPending(context: Context, requested: PendingEkagraSessionSave): Resource<com.safarparmar.app.data.remote.dto.EkagraSession> {
            val account = SafarDataStore(context).userId.first().orEmpty()
            if (requested.ownerId != account) return Resource.Error("Session belongs to another account")
            // Re-read under the upload lock: another caller may have uploaded while
            // this caller waited. Acknowledged rows remain durable deduplication records.
            val session = EkagraSessionJournal.get(context).find(requested.clientSessionId, account)
                ?: return Resource.Error("Session is not in the durable journal")
            session.serverId?.takeIf { it.isNotBlank() }?.let {
                return Resource.Success(com.safarparmar.app.data.remote.dto.EkagraSession(id = it))
            }
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
