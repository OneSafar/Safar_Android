package com.safarparmar.app.feature.kavachanalytics.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodic all-day usage collection + aggregation.
 *
 * Deliberately has no network constraint: collection and aggregation are local, and
 * a student with no data must still see their own analytics. Uploading is a separate,
 * network-constrained job.
 */
class KavachUsageCollectionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = repositoryFrom(applicationContext)
        return runCatching {
            repository.refresh(sync = false)
            KavachAnalyticsSyncWorker.enqueue(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "kavach_usage_collection"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<KavachUsageCollectionWorker>(
                COLLECTION_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * WorkManager's floor is 15 minutes, and Android keeps several days of usage
         * events, so a missed run is reconciled rather than lost.
         */
        private const val COLLECTION_INTERVAL_MINUTES = 30L
    }
}

/** Uploads unacknowledged aggregates, sessions and category overrides. */
class KavachAnalyticsSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = repositoryFrom(applicationContext)
        return runCatching {
            if (repository.syncNow()) Result.success() else Result.retry()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "kavach_analytics_sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<KavachAnalyticsSyncWorker>()
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

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface KavachAnalyticsWorkerEntryPoint {
    fun kavachAnalyticsRepository(): KavachAnalyticsRepository
}

internal fun repositoryFrom(context: Context): KavachAnalyticsRepository =
    EntryPointAccessors
        .fromApplication(context.applicationContext, KavachAnalyticsWorkerEntryPoint::class.java)
        .kavachAnalyticsRepository()
