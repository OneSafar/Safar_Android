package com.safarparmar.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object SafarNotificationChannels {
    const val FOCUS_TIMER = "focus_timer"
    // Separate from the quiet foreground timer channel so a time-sensitive
    // Ekagra check-in can appear as a heads-up notification.
    const val EKAGRA_CHECK_INS = "ekagra_check_ins_v1"
    const val POMODORO_TRANSITIONS = "pomodoro_transitions_v1"
    // Legacy channel (kept for backward compatibility; new installs should rely on the split channels below).
    const val FOCUS_SHIELD_ALERTS = "focus_shield_alerts"

    // New split channels (Android 8+):
    // - STATUS: ongoing, low-priority indicator that Focus Shield is enabled
    // - BLOCKED: event notification when a blocked app is opened (should not be heads-up)
    const val FOCUS_SHIELD_STATUS = "focus_shield_status"
    const val FOCUS_SHIELD_BLOCKED = "focus_shield_blocked"
    const val YOUTUBE_STUDY_MODE = "youtube_study_mode"
    const val YOUTUBE_STUDY_V2_STATUS = "youtube_study_v2_status"
    const val STUDY_REMINDERS = "study_reminders_v2"
    const val COURSE_UPDATES = "course_updates"
    const val ACHIEVEMENTS = "achievements_v2"
    const val COMMUNITY = "community"
    const val ACCOUNT_SYSTEM = "account_system"
    const val ANNOUNCEMENTS = "announcements_v2"
    const val MEHFIL_CONNECT = "mehfil_connect"

    /**
     * High-importance Ekagra alert channel. Uses the phone's default notification
     * sound for check-ins, timer completion, and Pomodoro transitions.
     */
    const val EKAGRA_ALERT = "ekagra_sound_alert_v2"
    const val EKAGRA_VIBRATE_ALERT = "ekagra_vibrate_alert_v1"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                FOCUS_TIMER,
                context.getString(com.safarparmar.app.R.string.channel_ekagra_timer),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_ekagra_timer_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                EKAGRA_CHECK_INS,
                context.getString(com.safarparmar.app.R.string.channel_ekagra_check_ins),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_ekagra_check_ins_desc)
                // Legacy silent check-in channel. TimerService now selects an alert
                // channel according to the user's sound or vibration preference.
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
            NotificationChannel(
                POMODORO_TRANSITIONS,
                "Pomodoro transitions",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Focus and break changes in a Pomodoro session"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_ALERTS,
                context.getString(com.safarparmar.app.R.string.channel_kavach_alerts),
                // Lower than HIGH to avoid heads-up banners; background activity starts are not user-initiated.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_kavach_alerts_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_STATUS,
                context.getString(com.safarparmar.app.R.string.channel_kavach_status),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_kavach_status_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_BLOCKED,
                context.getString(com.safarparmar.app.R.string.channel_kavach_blocked),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_kavach_blocked_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                YOUTUBE_STUDY_MODE,
                context.getString(com.safarparmar.app.R.string.channel_youtube_focus),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_youtube_focus_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                YOUTUBE_STUDY_V2_STATUS,
                context.getString(com.safarparmar.app.R.string.channel_youtube_status),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_youtube_status_desc)
                setShowBadge(false)
            },
            NotificationChannel(
                STUDY_REMINDERS,
                context.getString(com.safarparmar.app.R.string.channel_study_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_study_reminders_desc)
            },
            NotificationChannel(
                COURSE_UPDATES,
                context.getString(com.safarparmar.app.R.string.channel_course_updates),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_course_updates_desc)
            },
            NotificationChannel(
                ACHIEVEMENTS,
                context.getString(com.safarparmar.app.R.string.channel_achievements),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_achievements_desc)
            },
            NotificationChannel(
                COMMUNITY,
                context.getString(com.safarparmar.app.R.string.channel_community),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_community_desc)
            },
            NotificationChannel(
                ACCOUNT_SYSTEM,
                context.getString(com.safarparmar.app.R.string.channel_account_system),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_account_system_desc)
            },
            NotificationChannel(
                ANNOUNCEMENTS,
                context.getString(com.safarparmar.app.R.string.channel_announcements),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_announcements_desc)
            },
            // Android handles the alert using the phone's default notification sound.
            NotificationChannel(
                EKAGRA_ALERT,
                "Ekagra sound alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Phone notification sound for Ekagra check-ins, session endings, and Pomodoro changes."
                enableVibration(false)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setShowBadge(false)
            },
            NotificationChannel(
                EKAGRA_VIBRATE_ALERT,
                "Ekagra vibration alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Vibrates without sound for Ekagra check-ins and timer transitions."
                setSound(null, null)
                enableVibration(true)
                setShowBadge(false)
            },
        )

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannels(channels)
        // Clean up legacy channels so OS does not hold old silent configurations
        runCatching { notificationManager?.deleteNotificationChannel(MEHFIL_CONNECT) }
        runCatching { notificationManager?.deleteNotificationChannel("announcements") }
        runCatching { notificationManager?.deleteNotificationChannel("achievements") }
        runCatching { notificationManager?.deleteNotificationChannel("study_reminders") }
        runCatching { notificationManager?.deleteNotificationChannel("ekagra_alert_v1") }
    }

    fun normalize(channelId: String?): String = when (channelId) {
        FOCUS_TIMER,
        EKAGRA_CHECK_INS,
        POMODORO_TRANSITIONS,
        FOCUS_SHIELD_ALERTS,
        FOCUS_SHIELD_STATUS,
        FOCUS_SHIELD_BLOCKED,
        YOUTUBE_STUDY_MODE,
        YOUTUBE_STUDY_V2_STATUS,
        COURSE_UPDATES,
        COMMUNITY,
        ACCOUNT_SYSTEM,
        EKAGRA_ALERT,
        EKAGRA_VIBRATE_ALERT,
        MEHFIL_CONNECT -> channelId
        "study_reminders",
        STUDY_REMINDERS -> STUDY_REMINDERS
        "achievements",
        ACHIEVEMENTS -> ACHIEVEMENTS
        "announcements",
        ANNOUNCEMENTS -> ANNOUNCEMENTS
        else -> ACCOUNT_SYSTEM
    }

    fun ensureFocusShieldStatusChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(FOCUS_SHIELD_STATUS) == null) {
            val channel = NotificationChannel(
                FOCUS_SHIELD_STATUS,
                context.getString(com.safarparmar.app.R.string.channel_kavach_status),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_kavach_status_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun ensureYoutubeStudyV2StatusChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(YOUTUBE_STUDY_V2_STATUS) == null) {
            val channel = NotificationChannel(
                YOUTUBE_STUDY_V2_STATUS,
                context.getString(com.safarparmar.app.R.string.channel_youtube_study_status),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(com.safarparmar.app.R.string.channel_youtube_study_status_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
