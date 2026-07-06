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
        val pending = EkagraPendingSessionSaveStore.getAll(applicationContext)
        if (pending.isEmpty()) return Result.success()

        val repository = EntryPointAccessors
            .fromApplication(applicationContext, EkagraSessionSaveEntryPoint::class.java)
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
                    goalId = session.goalId,
                    goalTitle = session.goalTitle,
                    taskTitle = session.taskTitle,
                    shieldEnabled = session.shieldEnabled,
                )
            ) {
                is Resource.Success -> {
                    EkagraPendingSessionSaveStore.remove(applicationContext, session.clientSessionId)
                    debugLog("Saved pending Ekagra session ${session.clientSessionId}")
                }
                is Resource.Error,
                is Resource.Loading -> {
                    failed = true
                    debugLog("Pending Ekagra session save failed: ${session.clientSessionId}")
                }
            }
        }

        return if (failed) Result.retry() else Result.success()
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "EkagraSaveWorker"
        private const val WORK_NAME = "ekagra_pending_session_save"

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
