package com.safar.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalTime
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
            .setSmallIcon(SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationStyle.brandColor(context))
            // Intentionally NOT calling setLargeIcon(...) — like Gmail/YouTube,
            // we rely on the small icon (tinted brand-blue) to brand the
            // status bar AND the shade entry, so users see exactly ONE logo.
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

    /**
     * Centralizes the brand styling for SAFAR notifications.
     *
     * Android automatically renders the small icon as a white silhouette in
     * the status bar and tints it with [brandColor] in the shade. We rely on
     * that single icon for branding (no `setLargeIcon`) so users see ONE
     * SAFAR logo in both the status bar and the expanded notification — the
     * same pattern Gmail, YouTube, and Slack use.
     */
    object SafarNotificationStyle {
        private const val SPARKLE_GOLD = 0xFFFFC83D.toInt()

        fun isNightMode(context: Context): Boolean {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }

        /** Gold sparkle tint for notification panel icon rendering. */
        fun brandColor(context: Context): Int =
            SPARKLE_GOLD

        /**
         * Small icon must be a white-on-transparent silhouette for system pipeline.
         * We use a sparkle glyph so:
         * - Status bar remains monochrome (system behavior)
         * - Notification panel gets brand tint via [brandColor]
         */
        fun smallIconRes(context: Context): Int =
            R.drawable.ic_safar_notification_sparkle
    }

    suspend fun show(
        title: String,
        body: String,
        channelId: String,
        deepLink: String? = null,
        notificationId: Int = Random.nextInt(10_000, 99_999),
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    ) {
        if (!canPostNotifications()) return
        if (shouldSuppressByQuietHours(channelId)) return
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

    private suspend fun shouldSuppressByQuietHours(channelId: String): Boolean {
        // Never suppress critical account/system alerts.
        if (channelId == SafarNotificationChannels.ACCOUNT_SYSTEM) return false

        val dataStore = SafarDataStore(context)
        val startRaw = dataStore.quietHoursStart.first()
        val endRaw = dataStore.quietHoursEnd.first()
        val start = runCatching { LocalTime.parse(startRaw) }.getOrNull() ?: return false
        val end = runCatching { LocalTime.parse(endRaw) }.getOrNull() ?: return false
        val now = LocalTime.now()

        val inQuietHours = if (start == end) {
            false
        } else if (start < end) {
            now >= start && now < end
        } else {
            // Window crosses midnight, e.g. 22:00..07:00.
            now >= start || now < end
        }
        return inQuietHours
    }
}
