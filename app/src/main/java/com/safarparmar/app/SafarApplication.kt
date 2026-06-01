package com.safarparmar.app

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.notifications.SafarNotificationChannels
import androidx.work.ExistingPeriodicWorkPolicy
import com.safarparmar.app.notifications.PlannerAlertsWorker
import com.safarparmar.app.notifications.MorningNudgeWorker
import com.safarparmar.app.notifications.NotificationTokenRegistrar
import com.safarparmar.app.notifications.StudyReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SafarApplication : Application() {

    @Inject lateinit var dataStore: SafarDataStore
    @Inject lateinit var notificationTokenRegistrar: NotificationTokenRegistrar
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private val appScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    override fun onCreate() {
        super.onCreate()
        SafarNotificationChannels.createAll(this)
        fetchAndStoreFcmToken()
        appScope.launch {
            notificationTokenRegistrar.registerStoredTokenIfNeeded()
            if (dataStore.notificationsEnabled.first() && dataStore.dailyStudyReminderEnabled.first()) {
                val keep = ExistingPeriodicWorkPolicy.KEEP
                StudyReminderWorker.schedule(
                    this@SafarApplication,
                    dataStore.dailyReminderTime.first(),
                    keep,
                )
                PlannerAlertsWorker.schedule(
                    this@SafarApplication,
                    dataStore.dailyReminderTime.first(),
                    keep,
                )
                MorningNudgeWorker.schedule(this@SafarApplication, 6, 30, keep)
            } else {
                StudyReminderWorker.cancel(this@SafarApplication)
                PlannerAlertsWorker.cancel(this@SafarApplication)
                MorningNudgeWorker.cancel(this@SafarApplication)
            }
        }
    }

    private fun fetchAndStoreFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (BuildConfig.DEBUG) Log.d("SAFAR_FCM", "FCM token fetched")
                appScope.launch {
                    notificationTokenRegistrar.saveAndRegister(token)
                }
            }
            .addOnFailureListener {
                if (BuildConfig.DEBUG) Log.e("SAFAR_FCM", "FCM token fetch failed", it)
            }
            .addOnCompleteListener { task ->
                if (BuildConfig.DEBUG) {
                    Log.d("SAFAR_FCM", "FCM token task complete. success=${task.isSuccessful}")
                }
            }
    }

}
