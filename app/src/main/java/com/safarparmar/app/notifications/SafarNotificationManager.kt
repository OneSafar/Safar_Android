package com.safarparmar.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalTime

enum class NotificationAvailabilityReason {
    permission_not_granted,
    app_notifications_blocked,
    channel_missing,
    channel_blocked,
    allowed,
}

data class NotificationAvailability(
    val allowed: Boolean,
    val reason: NotificationAvailabilityReason,
)

class SafarNotificationManager(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun canPostNotifications(): Boolean =
        evaluateNotificationAvailability().allowed

    fun evaluateNotificationAvailability(channelId: String? = null): NotificationAvailability {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationAvailability(false, NotificationAvailabilityReason.permission_not_granted)
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return NotificationAvailability(false, NotificationAvailabilityReason.app_notifications_blocked)
        }

        val normalizedChannel = channelId?.let { SafarNotificationChannels.normalize(it) }
        if (normalizedChannel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(normalizedChannel)
                ?: return NotificationAvailability(false, NotificationAvailabilityReason.channel_missing)
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return NotificationAvailability(false, NotificationAvailabilityReason.channel_blocked)
            }
        }

        return NotificationAvailability(true, NotificationAvailabilityReason.allowed)
    }

    private suspend fun fetchBitmap(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                val drawable = result.drawable ?: return@withContext null
                if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else {
                    val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 512
                    val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 288
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }

    fun buildNotification(
        title: String,
        body: String,
        channelId: String,
        deepLink: String? = null,
        ongoing: Boolean = false,
        onlyAlertOnce: Boolean = false,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        // P2 fix: accept optional action buttons (used by study reminder "Start Now")
        actions: List<NotificationCompat.Action> = emptyList(),
        imageBitmap: Bitmap? = null,
    ): Notification {
        val normalizedChannel = SafarNotificationChannels.normalize(channelId)
        // P2 fix: tag every notification with its channel-scoped group key so that
        // Android can automatically collapse stacked notifications in the shade.
        val groupKey = groupKeyForChannel(normalizedChannel)
        val contentIntent = PendingIntent.getActivity(
            context,
            deepLink.hashCode(),
            NotificationDeepLinkHandler.activityIntent(context, deepLink),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val style = if (imageBitmap != null) {
            NotificationCompat.BigPictureStyle()
                .bigPicture(imageBitmap)
                .bigLargeIcon(null as Bitmap?)
                .setSummaryText(body)
        } else {
            NotificationCompat.BigTextStyle().bigText(body)
        }

        val builder = NotificationCompat.Builder(context, normalizedChannel)
            .setSmallIcon(SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationStyle.brandColor(context))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setPriority(priority)
            .setGroup(groupKey)

        if (imageBitmap != null) {
            builder.setLargeIcon(imageBitmap)
        }

        actions.forEach { builder.addAction(it) }

        return builder.build()
    }

    /**
     * Posts an invisible group-summary notification for the given channel.
     * This is required by Android to actually collapse multiple child notifications
     * into a single grouped bundle in the notification shade.
     *
     * Must be called AFTER posting the individual child notification.
     */
    private fun postGroupSummary(channelId: String) {
        val normalizedChannel = SafarNotificationChannels.normalize(channelId)
        val groupKey = groupKeyForChannel(normalizedChannel)
        val summaryId = groupSummaryIdForChannel(normalizedChannel)

        val summaryNotification = NotificationCompat.Builder(context, normalizedChannel)
            .setSmallIcon(SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationStyle.brandColor(context))
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(summaryId, summaryNotification)
    }

    /** Channel-scoped group key for the notification shade bundling. */
    private fun groupKeyForChannel(channelId: String): String = "safar_group_$channelId"

    /**
     * Deterministic, channel-scoped summary ID.
     * Reserved range [1_000..1_999] — well outside the [10_000..90_000] range
     * used by [stableNotificationId] for regular notifications.
     */
    private fun groupSummaryIdForChannel(channelId: String): Int =
        1_000 + (channelId.hashCode() and 0x7FFF) % 999

    object SafarNotificationStyle {
        private const val NOTIFICATION_SPARKLE_WHITE = 0xFFFFFFFF.toInt()

        fun isNightMode(context: Context): Boolean {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }

        fun brandColor(context: Context): Int = NOTIFICATION_SPARKLE_WHITE

        fun smallIconRes(context: Context): Int = R.drawable.ic_safar_notification_sparkle
    }

    fun stableNotificationId(type: String?, deepLink: String?, title: String): Int {
        val key = listOfNotNull(type?.takeIf { it.isNotBlank() }, deepLink?.takeIf { it.isNotBlank() }, title)
            .joinToString("|")
        val hash = key.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        return (hash % 80_000) + 10_000
    }

    /**
     * Well-known notification type constants.
     * The server FCM payload MUST use the same `type` string so that
     * [stableNotificationId] produces identical IDs for both local and
     * remote notifications — enabling automatic deduplication.
     */
    object DedupeType {
        const val STUDY_REMINDER = "study_reminder"
        const val MORNING_NUDGE = "morning_nudge"
        /** Matches the backend FCM type for evening quote notifications (backend-driven only). */
        const val EVENING_NUDGE = "evening_nudge"
        const val PLANNER_ALERT = "planner_alert"
        /** Matches the `type` field sent by the backend FCM scheduler for spaced revision reminders. */
        const val PLANNER_REVISION_REMINDER = "planner_revision_reminder"
    }

    suspend fun show(
        title: String,
        body: String,
        channelId: String,
        deepLink: String? = null,
        notificationId: Int? = null,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        onlyAlertOnce: Boolean = false,
        actions: List<NotificationCompat.Action> = emptyList(),
        /**
         * Prefixes the body with "Hi <name>,". Opt-in: greeting every local
         * notification made them all read the same way, and on a functional
         * message ("You unlocked: ...") the greeting only delays the payload.
         * Server-sent FCM bodies are personalized server-side and pass false too.
         */
        personalize: Boolean = false,
        /** Optional dedup type — ensures FCM and local notifications for the same
         *  logical event produce the same [stableNotificationId]. */
        dedupeType: String? = null,
        /** Optional HTTPS image URL / YouTube thumbnail for Rich Notifications */
        imageUrl: String? = null,
    ) {
        val resolvedId = notificationId ?: stableNotificationId(type = dedupeType, deepLink = deepLink, title = title)
        val normalizedChannel = SafarNotificationChannels.normalize(channelId)
        if (evaluateNotificationAvailability(normalizedChannel).reason != NotificationAvailabilityReason.allowed) {
            return
        }
        val personalizedBody = if (personalize) personalizeBody(body) else body
        val imageBitmap = fetchBitmap(imageUrl)
        // if (shouldSuppressByQuietHours(normalizedChannel)) return
        notificationManager.notify(
            resolvedId,
            buildNotification(
                title = title,
                body = personalizedBody,
                channelId = normalizedChannel,
                deepLink = deepLink,
                priority = priority,
                onlyAlertOnce = onlyAlertOnce,
                actions = actions,
                imageBitmap = imageBitmap,
            ),
        )
        // P2 fix: post group summary so Android collapses stacked channel notifications
        postGroupSummary(normalizedChannel)
    }

    private suspend fun personalizeBody(body: String): String = body

    private fun startsWithPersonalGreeting(body: String): Boolean {
        return Regex(
            pattern = "^\\s*(hi|hey|hello|good morning|good afternoon|good evening)\\b",
            option = RegexOption.IGNORE_CASE,
        ).containsMatchIn(body)
    }

    /**
     * Convenience wrapper that attaches a **"Start Now"** action button to study-reminder
     * notifications so users can jump directly into the Ekagra focus timer with one tap.
     *
     * Uses [DedupeType.STUDY_REMINDER] so that if the server also sends an FCM push
     * with `type: "study_reminder"` for the same deep link, both produce the same
     * notification ID and Android shows only one instead of duplicates.
     */
    suspend fun showStudyReminder(
        title: String,
        body: String,
        deepLink: String? = "safar://ekagra",
        notificationId: Int? = null,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        dedupeType: String = DedupeType.STUDY_REMINDER,
        /** See [show] — opt-in, and off unless this reminder earns the name. */
        personalize: Boolean = false,
    ) {
        val startNowIntent = PendingIntent.getActivity(
            context,
            "start_now_$deepLink".hashCode(),
            NotificationDeepLinkHandler.activityIntent(context, deepLink ?: "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val startNowAction = NotificationCompat.Action(
            R.drawable.ic_safar_notification_sparkle,
            context.getString(R.string.notification_action_start_now),
            startNowIntent,
        )

        show(
            title = title,
            body = body,
            channelId = SafarNotificationChannels.STUDY_REMINDERS,
            deepLink = deepLink,
            notificationId = notificationId,
            priority = priority,
            actions = listOf(startNowAction),
            dedupeType = dedupeType,
            personalize = personalize,
        )
    }

    private suspend fun shouldSuppressByQuietHours(channelId: String): Boolean {
        val dataStore = SafarDataStore(context)
        val startRaw = dataStore.quietHoursStart.first()
        val endRaw = dataStore.quietHoursEnd.first()
        val start = runCatching { LocalTime.parse(startRaw) }.getOrNull() ?: return false
        val end = runCatching { LocalTime.parse(endRaw) }.getOrNull() ?: return false
        return QuietHoursEvaluator.shouldSuppress(
            channelId = channelId,
            quietStart = start,
            quietEnd = end,
        )
    }
}
