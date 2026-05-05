package com.safar.app.notifications

import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safar.app.BuildConfig
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.NotificationApi
import com.safar.app.data.remote.dto.DeviceTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafarFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var dataStore: SafarDataStore
    @Inject lateinit var notificationApi: NotificationApi

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            dataStore.setFcmToken(token)
            registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body ?: ""
        val channel = SafarNotificationChannels.normalize(data["channel"])
        val deepLink = data["deepLink"]
        val priority = when (data["priority"]) {
            "high" -> NotificationCompat.PRIORITY_HIGH
            "low" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        scope.launch {
            if (!isChannelEnabled(channel)) return@launch
            SafarNotificationManager(applicationContext).show(
                title = title,
                body = body,
                channelId = channel,
                deepLink = deepLink,
                priority = priority,
            )
        }
    }

    private suspend fun isChannelEnabled(channel: String): Boolean {
        if (!dataStore.notificationsEnabled.first()) return false
        return when (channel) {
            SafarNotificationChannels.FOCUS_TIMER -> dataStore.focusTimerNotificationsEnabled.first()
            SafarNotificationChannels.STUDY_REMINDERS -> dataStore.dailyStudyReminderEnabled.first() ||
                dataStore.streakReminderEnabled.first()
            SafarNotificationChannels.COURSE_UPDATES -> dataStore.courseUpdatesEnabled.first()
            SafarNotificationChannels.ACHIEVEMENTS -> dataStore.achievementsEnabled.first()
            SafarNotificationChannels.COMMUNITY -> dataStore.communityRepliesEnabled.first()
            SafarNotificationChannels.ANNOUNCEMENTS -> dataStore.announcementsEnabled.first()
            SafarNotificationChannels.ACCOUNT_SYSTEM -> true
            else -> true
        }
    }

    private suspend fun registerToken(token: String) {
        val isLoggedIn = dataStore.isLoggedIn.first()
        val authToken = dataStore.authToken.first()
        if (!isLoggedIn || authToken.isNullOrBlank()) return

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
            dataStore.setDeviceTokenLastSyncAt(System.currentTimeMillis())
        }.onFailure {
            Log.w("SafarFCM", "Failed to register FCM token", it)
        }
    }
}
