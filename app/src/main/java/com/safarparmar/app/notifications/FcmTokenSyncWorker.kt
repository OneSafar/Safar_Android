package com.safarparmar.app.notifications

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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that syncs the FCM device token to the server when the network
 * becomes available.
 *
 * This fixes the P1 audit finding: previously, if [onNewToken] fired while the device
 * was offline, the server would keep a stale token, silently breaking push delivery.
 *
 * Schedule this worker via [enqueue] after any failed registration attempt. WorkManager
 * will retry with exponential back-off until a network connection is established.
 */
class FcmTokenSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FcmTokenSyncEntryPoint {
        fun getNotificationTokenRegistrar(): NotificationTokenRegistrar
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                FcmTokenSyncEntryPoint::class.java,
            )
            entryPoint.getNotificationTokenRegistrar().registerStoredTokenIfNeeded(force = true)
            if (BuildConfig.DEBUG) {
                Log.d("SafarFCM", "FcmTokenSyncWorker: token sync completed successfully")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("SafarFCM", "FcmTokenSyncWorker: token sync failed, will retry", e)
            // WorkManager will honour the BackoffPolicy set in enqueue()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "fcm_token_sync"

        /**
         * Enqueues a network-constrained one-time work request that will register the
         * stored FCM token as soon as a network connection is available.
         *
         * Uses [ExistingWorkPolicy.REPLACE] so that if a sync is already queued a new
         * one (e.g. from a second `onNewToken` call) replaces the stale entry.
         *
         * Back-off: 30 s → 60 s → 120 s … (exponential, capped by WorkManager default of 5 h).
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )

            if (BuildConfig.DEBUG) {
                Log.d("SafarFCM", "FcmTokenSyncWorker: enqueued (will fire when network is available)")
            }
        }
    }
}
