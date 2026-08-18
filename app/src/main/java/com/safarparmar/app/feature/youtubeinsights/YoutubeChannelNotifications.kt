package com.safarparmar.app.feature.youtubeinsights

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

object YoutubeChannelNotifications {
    const val ACTION_MARK_PRODUCTIVE = "com.safarparmar.app.youtube.MARK_PRODUCTIVE"
    const val EXTRA_CHANNEL_KEY = "youtube_channel_key"

    fun notificationId(channelKey: String): Int = 410_000 + (channelKey.hashCode() and 0x7fff)

    suspend fun showBlocked(context: Context, channelKey: String, channelName: String) {
        val requestCode = channelKey.hashCode()
        val markIntent = Intent(context, YoutubeChannelActionReceiver::class.java).apply {
            action = ACTION_MARK_PRODUCTIVE
            data = Uri.parse("safar://youtube-channel/${Uri.encode(channelKey)}")
            putExtra(EXTRA_CHANNEL_KEY, channelKey)
        }
        val markPendingIntent = PendingIntent.getBroadcast(
            context, requestCode, markIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val managePendingIntent = PendingIntent.getActivity(
            context,
            requestCode xor 0x5a5a,
            NotificationDeepLinkHandler.activityIntent(context, "safar://youtube_study_mode?section=channels"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        SafarNotificationManager(context).show(
            title = "Channel blocked by SAFAR",
            body = "“$channelName” is currently marked Distracting.",
            channelId = SafarNotificationChannels.YOUTUBE_STUDY_MODE,
            deepLink = "safar://youtube_study_mode?section=channels",
            notificationId = notificationId(channelKey),
            onlyAlertOnce = true,
            actions = listOf(
                NotificationCompat.Action(0, "Mark Productive", markPendingIntent),
                NotificationCompat.Action(0, "Manage channels", managePendingIntent),
            ),
        )
    }
}

@AndroidEntryPoint
class YoutubeChannelActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: YoutubeInsightsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != YoutubeChannelNotifications.ACTION_MARK_PRODUCTIVE) return
        val key = intent.getStringExtra(YoutubeChannelNotifications.EXTRA_CHANNEL_KEY)
            ?.takeIf { it.isNotBlank() } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val channel = repository.channel(key) ?: return@launch
                repository.setProductive(key, true)
                NotificationManagerCompat.from(context).cancel(YoutubeChannelNotifications.notificationId(key))
                SafarNotificationManager(context).show(
                    title = "Channel marked Productive",
                    body = "${channel.displayName} will be allowed the next time you open it.",
                    channelId = SafarNotificationChannels.YOUTUBE_STUDY_MODE,
                    deepLink = "safar://youtube_study_mode?section=channels",
                    notificationId = YoutubeChannelNotifications.notificationId(key),
                    onlyAlertOnce = true,
                )
                Handler(Looper.getMainLooper()).postDelayed(
                    { NotificationManagerCompat.from(context).cancel(YoutubeChannelNotifications.notificationId(key)) },
                    4_000L,
                )
            } finally {
                pending.finish()
            }
        }
    }
}
