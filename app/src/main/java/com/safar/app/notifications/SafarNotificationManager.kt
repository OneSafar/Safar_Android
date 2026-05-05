package com.safar.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safar.app.R
import kotlin.random.Random

class SafarNotificationManager(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun buildNotification(
        title: String,
        body: String,
        channelId: String,
        deepLink: String? = null,
        ongoing: Boolean = false,
        onlyAlertOnce: Boolean = false,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    ): Notification {
        val normalizedChannel = SafarNotificationChannels.normalize(channelId)
        val contentIntent = PendingIntent.getActivity(
            context,
            deepLink.hashCode(),
            NotificationDeepLinkHandler.activityIntent(context, deepLink),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, normalizedChannel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setPriority(priority)
            .build()
    }

    fun show(
        title: String,
        body: String,
        channelId: String,
        deepLink: String? = null,
        notificationId: Int = Random.nextInt(10_000, 99_999),
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    ) {
        if (!canPostNotifications()) return
        notificationManager.notify(
            notificationId,
            buildNotification(
                title = title,
                body = body,
                channelId = channelId,
                deepLink = deepLink,
                priority = priority,
            ),
        )
    }
}
