package com.safarparmar.app.ui.ekagra.focusshield

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safarparmar.app.R
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import kotlin.math.ceil

object QuickUnlockNotification {
    private const val NOTIFICATION_ID = 1005

    fun show(context: Context, graceUntilMs: Long, userName: String? = null) {
        val remainingMs = (graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remainingMs <= 0L) {
            cancel(context)
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val minutesLeft = ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            NotificationDeepLinkHandler.activityIntent(context, "safar://focus_shield"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle("Quick unlock active")
            .setContentText(personalizeBody("$minutesLeft min left before KAVACH blocks again", userName))
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setWhen(graceUntilMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(remainingMs)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
    }

    private fun personalizeBody(body: String, userName: String?): String {
        val name = userName?.trim().orEmpty()
        if (name.isBlank()) return body
        return "$name, $body"
    }
}
