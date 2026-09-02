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

    fun show(
        context: Context,
        graceUntilMs: Long,
        minutes: Int = 0,
        userName: String? = null,
        origin: String = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH,
    ) {
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

        val displayMins = if (minutes > 0) minutes else ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            NotificationDeepLinkHandler.activityIntent(context, "safar://focus_shield"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isYoutubeStudyUnlock =
            origin == FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY
        val notification = NotificationCompat.Builder(context, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle(if (isYoutubeStudyUnlock) "YouTube Quick Unlock Active" else "KAVACH Quick Unlock Active")
            .setContentText(
                if (isYoutubeStudyUnlock) {
                    "YouTube is unlocked for $displayMins min. Study Mode will block it again when time ends."
                } else {
                    "Unlocked for $displayMins min. KAVACH will re-block when timer ends."
                },
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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

    private fun personalizeBody(body: String, _userName: String?): String = body
}
