package com.safarparmar.app.feature.youtubestudyv2

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object YoutubeStudyV2BlockedNotification {
    private const val ACTION_ALLOW = "com.safarparmar.app.youtubeStudyV2.ALLOW_CHANNEL"
    private const val EXTRA_CHANNEL_ID = "channel_id"
    private const val EXTRA_DISPLAY_NAME = "display_name"

    fun show(context: Context, channel: YoutubeV2IdentityEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notificationId = notificationId(channel.channelId)
        val allowIntent = Intent(context, YoutubeStudyV2NotificationActionReceiver::class.java).apply {
            action = ACTION_ALLOW
            putExtra(EXTRA_CHANNEL_ID, channel.channelId)
            putExtra(EXTRA_DISPLAY_NAME, channel.displayName)
        }
        val allowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            allowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            NotificationDeepLinkHandler.activityIntent(context, "safar://youtube_study_v2"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SafarNotificationChannels.YOUTUBE_STUDY_MODE)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle("Channel blocked")
            .setContentText("${channel.displayName} is Distracting. Make it Productive if you want to watch it.")
            .setContentIntent(contentIntent)
            .addAction(0, "Make productive", allowPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
    }

    internal fun notificationId(channelId: String): Int = 31_000 + (channelId.hashCode() and 0x0fff)

    internal fun isAllowAction(action: String?): Boolean = action == ACTION_ALLOW
    internal fun channelId(intent: Intent): String? = intent.getStringExtra(EXTRA_CHANNEL_ID)
    internal fun displayName(intent: Intent): String? = intent.getStringExtra(EXTRA_DISPLAY_NAME)
}

@AndroidEntryPoint
class YoutubeStudyV2NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: YoutubeStudyV2Repository

    override fun onReceive(context: Context, intent: Intent) {
        if (!YoutubeStudyV2BlockedNotification.isAllowAction(intent.action)) return
        val channelId = YoutubeStudyV2BlockedNotification.channelId(intent)
            ?.takeIf { it.matches(Regex("^UC[A-Za-z0-9_-]{22}$")) }
            ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.setProductive(channelId, true)
                context.getSystemService(NotificationManager::class.java)
                    ?.cancel(YoutubeStudyV2BlockedNotification.notificationId(channelId))
            } finally {
                pending.finish()
            }
        }
    }
}
