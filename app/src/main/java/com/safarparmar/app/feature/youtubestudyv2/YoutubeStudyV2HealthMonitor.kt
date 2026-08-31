package com.safarparmar.app.feature.youtubestudyv2

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.safarparmar.app.R
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeStudyV2HealthMonitor @Inject constructor(
    private val preferences: YoutubeStudyV2Preferences,
) {
    fun checkOnAppResume(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!preferences.enabled.value) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val serviceEnabled = isAccessibilityEnabled(context)
        val heartbeatFresh = System.currentTimeMillis() - preferences.lastAccessibilityHeartbeatMs() <= HEARTBEAT_STALE_MS
        if (serviceEnabled && heartbeatFresh) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        SafarNotificationChannels.createAll(context)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            NotificationDeepLinkHandler.activityIntent(context, "safar://youtube_study_v2"),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, SafarNotificationChannels.YOUTUBE_STUDY_V2_STATUS)
                .setSmallIcon(R.drawable.ic_safar_notification_sparkle)
                .setContentTitle("YouTube Study Mode is off")
                .setContentText("Turn on SAFAR in Accessibility settings.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 2123
        private const val HEARTBEAT_STALE_MS = 90_000L

        fun isAccessibilityEnabled(context: Context): Boolean {
            val target = ComponentName(context, YoutubeStudyV2AccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            return enabled.split(':').any { it.equals(target, ignoreCase = true) }
        }
    }
}
