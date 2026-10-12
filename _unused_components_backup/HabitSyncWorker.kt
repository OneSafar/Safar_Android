package com.safarparmar.app.feature.habits.data

import android.content.Context
import android.util.Log
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class HabitSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies { fun habitSyncEngine(): HabitSyncEngine }

    override suspend fun doWork(): Result = try {
        val engine = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).habitSyncEngine()
        if (engine.sync()) Result.success() else {
            engine.noteFailure()
            Result.retry()
        }
    } catch (error: Exception) {
        Log.w("HabitSync", "Synchronization failed; retaining local mutations", error)
        runCatching {
            EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
                .habitSyncEngine().noteFailure()
        }
        Result.retry()
    }

    companion object {
        private const val IMMEDIATE = "habit_sync_immediate"
        private const val PERIODIC = "habit_sync_periodic"
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<HabitSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<HabitSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
