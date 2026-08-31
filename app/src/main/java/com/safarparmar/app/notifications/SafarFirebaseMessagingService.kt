package com.safarparmar.app.notifications

import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafarFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var dataStore: SafarDataStore
    @Inject lateinit var notificationTokenRegistrar: NotificationTokenRegistrar
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private val scope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            notificationTokenRegistrar.saveAndRegister(token, force = true)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body ?: ""
        val channel = SafarNotificationChannels.normalize(data["channel"])
        val deepLink = data["deepLink"]

        // Mehfil Connect notifications are strictly in-app only; do NOT post system push notifications.
        val isMehfilConnect = channel == SafarNotificationChannels.MEHFIL_CONNECT ||
            data["channel"] == "mehfil_connect" ||
            data["type"] == "mehfil_connect" ||
            data["type"] == "dm_request" ||
            data["type"] == "dm_accepted" ||
            data["type"] == "dm_message" ||
            deepLink?.contains("mehfil/dm_chat") == true
        if (isMehfilConnect) return

        val imageUrl = data["imageUrl"] ?: message.notification?.imageUrl?.toString()
        val priority = when (data["priority"]) {
            "high" -> NotificationCompat.PRIORITY_HIGH
            "low" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        scope.launch {
            if (!isChannelEnabled(channel)) return@launch
            val manager = SafarNotificationManager(applicationContext)
            manager.show(
                title = title,
                body = body,
                channelId = channel,
                deepLink = deepLink,
                imageUrl = imageUrl,
                priority = priority,
                onlyAlertOnce = true,
                personalize = false,
                notificationId = manager.stableNotificationId(
                    type = data["type"],
                    deepLink = deepLink,
                    title = title,
                ),
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
            SafarNotificationChannels.MEHFIL_CONNECT -> false
            SafarNotificationChannels.ACCOUNT_SYSTEM -> true
            else -> true
        }
    }
}
