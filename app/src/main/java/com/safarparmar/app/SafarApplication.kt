package com.safarparmar.app

import android.app.Application
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import com.safarparmar.app.ui.ekagra.EkagraPendingSessionSaveStore
import com.safarparmar.app.ui.ekagra.EkagraSessionSaveWorker
import com.safarparmar.app.ui.ekagra.focusshield.KavachAlwaysOnPrefs
import com.safarparmar.app.ui.ekagra.focusshield.KavachAlwaysOnService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
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

    private val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("SAFAR_APP", "Unhandled application coroutine exception", throwable)
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    private val appScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher + appExceptionHandler) }

    override fun onCreate() {
        super.onCreate()
        configureDebugStrictMode()
        configureCrashReporting()
        SafarNotificationChannels.createAll(this)
        fetchAndStoreFcmToken()
        if (EkagraPendingSessionSaveStore.getAll(this).isNotEmpty()) {
            EkagraSessionSaveWorker.enqueue(this)
        }
        appScope.launch {
            // "Always On" is hidden for this release. A user who enabled it in a
            // previous build would otherwise keep an all-day shield running with
            // no UI left to switch it off, so retire the flag and stop the
            // service once. Restoring the mode means restoring the start call.
            if (dataStore.focusShieldAlwaysOnMode.first()) {
                dataStore.setFocusShieldAlwaysOnMode(false)
                KavachAlwaysOnPrefs.clear(this@SafarApplication)
                stopService(Intent(this@SafarApplication, KavachAlwaysOnService::class.java))
            }
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

    private fun configureDebugStrictMode() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build(),
        )
    }

    private fun configureCrashReporting() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        appScope.launch {
            dataStore.userId.collect { userId ->
                crashlytics.setUserId(userId.orEmpty())
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
