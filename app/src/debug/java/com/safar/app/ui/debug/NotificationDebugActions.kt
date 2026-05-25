package com.safar.app.ui.debug

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.safar.app.notifications.MorningNudgeWorker
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.notifications.PlannerAlertsWorker
import com.safar.app.notifications.SafarNotificationChannels
import com.safar.app.notifications.SafarNotificationManager
import com.safar.app.notifications.StudyReminderWorker
import com.safar.app.ui.ekagra.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationDebugActions {
    private const val DEBUG_NOTIFICATION_ID = 42_001

    fun runCommand(context: Context, command: String) {
        when (command) {
            "show_panel" -> {
                context.startActivity(
                    Intent(context, NotificationTestPanelActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            "local" -> triggerCustomLocal(context)
            "morning_nudge" -> enqueueWorker<MorningNudgeWorker>(context)
            "study_reminder" -> enqueueWorker<StudyReminderWorker>(context)
            "planner_alerts" -> enqueueWorker<PlannerAlertsWorker>(context)
            "focus_complete" -> triggerFocusComplete(context)
            "shield_blocked" -> triggerShieldBlocked(context)
            "shield_status" -> triggerShieldStatus(context)
        }
    }

    private fun triggerCustomLocal(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            SafarNotificationManager(context).show(
                title = "SAFAR debug notification",
                body = "Custom local notification from debug tools.",
                channelId = SafarNotificationChannels.STUDY_REMINDERS,
                deepLink = "safar://studyplanner",
                notificationId = DEBUG_NOTIFICATION_ID,
            )
        }
    }

    private inline fun <reified W : androidx.work.CoroutineWorker> enqueueWorker(context: Context) {
        val request = OneTimeWorkRequestBuilder<W>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun triggerFocusComplete(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            SafarNotificationManager(context).show(
                title = "Focus session complete",
                body = "Focus session complete. Great work - take a mindful break.",
                channelId = SafarNotificationChannels.FOCUS_TIMER,
                deepLink = "safar://ekagra",
                notificationId = TimerService.COMPLETION_NOTIFICATION_ID,
            )
        }
    }

    private fun triggerShieldBlocked(context: Context) {
        val focusPendingIntent = PendingIntent.getActivity(
            context,
            4,
            NotificationDeepLinkHandler.activityIntent(context, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SafarNotificationChannels.FOCUS_SHIELD_BLOCKED)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle("Kavach is active")
            .setContentText("Debug app is blocked until your focus timer or Study Session ends.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Debug app is blocked until your focus timer or Study Session ends. Tap to return to SAFAR."),
            )
            .setContentIntent(focusPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(TimerService.FOCUS_SHIELD_BLOCKED_NOTIFICATION_ID, notification)
    }

    private fun triggerShieldStatus(context: Context) {
        val focusPendingIntent = PendingIntent.getActivity(
            context,
            4,
            NotificationDeepLinkHandler.activityIntent(context, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(context))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(context))
            .setContentTitle("Kavach is active")
            .setContentText("Selected distracting apps are blocked until your focus timer or Study Session ends.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(focusPendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(TimerService.FOCUS_SHIELD_ACTIVE_NOTIFICATION_ID, notification)
    }
}
