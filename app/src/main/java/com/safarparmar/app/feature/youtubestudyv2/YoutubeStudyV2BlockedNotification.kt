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
    private const val BLOCKED_CHANNEL_NOTIFICATION_ID = 31_000
    private const val ACTION_ALLOW = "com.safarparmar.app.youtubeStudyV2.ALLOW_CHANNEL"
    private const val EXTRA_CHANNEL_ID = "channel_id"
    private const val EXTRA_CLASSIFICATION = "classification"

    fun show(context: Context, channel: YoutubeV2IdentityEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.e("YTCM", "❌ POST_NOTIFICATIONS permission not granted — cannot show notification")
            return
        }

        val notificationId = BLOCKED_CHANNEL_NOTIFICATION_ID
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            NotificationDeepLinkHandler.activityIntent(context, "safar://youtube_study_v2"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, SafarNotificationChannels.YOUTUBE_STUDY_MODE)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle("Channel blocked")
            .setContentText("${channel.displayName} was blocked. Choose how SAFAR should treat it.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
        YoutubeChannelClassification.entries.forEachIndexed { index, classification ->
            val actionIntent = Intent(context, YoutubeStudyV2NotificationActionReceiver::class.java).apply {
                action = ACTION_ALLOW
                putExtra(EXTRA_CHANNEL_ID, channel.channelId)
                putExtra(EXTRA_CLASSIFICATION, classification.wire)
            }
            val action = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + index,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                0,
                classification.name.lowercase().replaceFirstChar(Char::uppercase),
                action,
            )
        }
        val notification = builder.build()
        android.util.Log.d("YTCM", "🔔 Posting notification with ID $notificationId for channel ${channel.displayName} (${channel.channelId})")
        context.getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
    }

    internal fun notificationId(): Int = BLOCKED_CHANNEL_NOTIFICATION_ID

    internal fun isAllowAction(action: String?): Boolean = action == ACTION_ALLOW
    internal fun channelId(intent: Intent): String? = intent.getStringExtra(EXTRA_CHANNEL_ID)
    internal fun classification(intent: Intent): YoutubeChannelClassification =
        YoutubeChannelClassification.fromWire(intent.getStringExtra(EXTRA_CLASSIFICATION))
}

@AndroidEntryPoint
class YoutubeStudyV2NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: YoutubeStudyV2Repository

    override fun onReceive(context: Context, intent: Intent) {
        if (!YoutubeStudyV2BlockedNotification.isAllowAction(intent.action)) return
        val channelId = YoutubeStudyV2BlockedNotification.channelId(intent)
            ?.takeIf { it.matches(Regex("^UC[A-Za-z0-9_-]{22}$")) }
            ?: return
        val classification = YoutubeStudyV2BlockedNotification.classification(intent)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.setClassification(channelId, classification)
                context.getSystemService(NotificationManager::class.java)
                    ?.cancel(YoutubeStudyV2BlockedNotification.notificationId())
            } finally {
                pending.finish()
            }
        }
    }
}
