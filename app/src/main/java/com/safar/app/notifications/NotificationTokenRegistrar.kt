package com.safar.app.notifications

import android.util.Log
import com.safar.app.BuildConfig
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.NotificationApi
import com.safar.app.data.remote.dto.DeviceTokenRequest
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
        val now = System.currentTimeMillis()
        val lastSync = dataStore.deviceTokenLastSyncAt.first()
        val minIntervalMs = 6 * 60 * 60 * 1000L
        if (!force && lastSync > 0 && now - lastSync < minIntervalMs) return

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
        }.onFailure {
            Log.w("SafarFCM", "Failed to register FCM token", it)
        }
    }
}
