package com.safar.app

import android.app.Application
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.NotificationApi
import com.safar.app.data.remote.dto.DeviceTokenRequest
import com.safar.app.notifications.SafarNotificationChannels
import com.safar.app.notifications.PlannerAlertsWorker
import com.safar.app.notifications.MorningNudgeWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SafarApplication : Application() {

    @Inject lateinit var dataStore: SafarDataStore
    @Inject lateinit var notificationApi: NotificationApi

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        SafarNotificationChannels.createAll(this)
        appScope.launch {
            registerStoredTokenIfNeeded()
            if (dataStore.notificationsEnabled.first() && dataStore.dailyStudyReminderEnabled.first()) {
                PlannerAlertsWorker.schedule(this@SafarApplication, dataStore.dailyReminderTime.first())
                // Morning Nudge at 6:30 AM
                MorningNudgeWorker.schedule(this@SafarApplication, 6, 30)
            }
        }
    }

    private suspend fun registerStoredTokenIfNeeded() {
        val isLoggedIn = dataStore.isLoggedIn.first()
        val authToken = dataStore.authToken.first()
        if (!isLoggedIn || authToken.isNullOrBlank()) return

        val token = dataStore.fcmToken.first() ?: return
        if (token.isBlank()) return

        val now = System.currentTimeMillis()
        val lastSync = dataStore.deviceTokenLastSyncAt.first()
        val minIntervalMs = 6 * 60 * 60 * 1000L
        if (lastSync > 0 && now - lastSync < minIntervalMs) return

        runCatching {
            notificationApi.registerDeviceToken(
                DeviceTokenRequest(
                    userId = dataStore.userId.first(),
                    deviceToken = token,
                    appVersion = BuildConfig.VERSION_NAME,
                    flavor = BuildConfig.FLAVOR,
                    language = dataStore.language.first(),
                    notificationsEnabled = dataStore.notificationsEnabled.first(),
                ),
            )
        }.onSuccess {
            dataStore.setDeviceTokenLastSyncAt(now)
        }
    }

}
