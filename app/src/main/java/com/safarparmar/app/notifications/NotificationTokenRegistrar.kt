package com.safarparmar.app.notifications

import android.util.Log
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.NotificationApi
import com.safarparmar.app.data.remote.dto.DeviceTokenRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTokenRegistrar @Inject constructor(
    private val dataStore: SafarDataStore,
    private val notificationApi: NotificationApi,
) {
    suspend fun saveAndRegister(token: String, force: Boolean = false) {
        if (token.isBlank()) return
        dataStore.setFcmToken(token)
        registerStoredTokenIfNeeded(force)
    }

    suspend fun registerStoredTokenIfNeeded(force: Boolean = false) {
        val isLoggedIn = dataStore.isLoggedIn.first()
        val authToken = dataStore.authToken.first()
        if (!isLoggedIn || authToken.isNullOrBlank()) return

        val token = dataStore.fcmToken.first()?.takeIf { it.isNotBlank() } ?: return
        val lastRegistered = dataStore.lastRegisteredFcmToken.first()
        val now = System.currentTimeMillis()
        val lastSync = dataStore.deviceTokenLastSyncAt.first()
        val minIntervalMs = 6 * 60 * 60 * 1000L
        
        val needsSync = force || token != lastRegistered || lastSync <= 0 || (now - lastSync >= minIntervalMs)
        if (!needsSync) return

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
            dataStore.setLastRegisteredFcmToken(token)
        }.onFailure {
            Log.w("SafarFCM", "Failed to register FCM token", it)
        }
    }
}
