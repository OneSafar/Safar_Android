package com.safar.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object SafarNotificationChannels {
    const val FOCUS_TIMER = "focus_timer"
    // Legacy channel (kept for backward compatibility; new installs should rely on the split channels below).
    const val FOCUS_SHIELD_ALERTS = "focus_shield_alerts"

    // New split channels (Android 8+):
    // - STATUS: ongoing, low-priority indicator that Focus Shield is enabled
    // - BLOCKED: event notification when a blocked app is opened (should not be heads-up)
    const val FOCUS_SHIELD_STATUS = "focus_shield_status"
    const val FOCUS_SHIELD_BLOCKED = "focus_shield_blocked"
    const val STUDY_REMINDERS = "study_reminders"
    const val COURSE_UPDATES = "course_updates"
    const val ACHIEVEMENTS = "achievements"
    const val COMMUNITY = "community"
    const val ACCOUNT_SYSTEM = "account_system"
    const val ANNOUNCEMENTS = "announcements"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                FOCUS_TIMER,
                "Focus timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active focus timer and break status"
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_ALERTS,
                "Kavach alerts",
                // Lower than HIGH to avoid heads-up banners; background activity starts are not user-initiated.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts when a blocked app is opened during an active focus session"
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_STATUS,
                "Kavach status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ongoing Kavach status during an active focus session or Study Session"
                setShowBadge(false)
            },
            NotificationChannel(
                FOCUS_SHIELD_BLOCKED,
                "Kavach blocked app",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Shown when a blocked app is opened during an active focus session or Study Session"
                setShowBadge(false)
            },
            NotificationChannel(
                STUDY_REMINDERS,
                "Study reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily study reminders, planned sessions, and streak protection"
            },
            NotificationChannel(
                COURSE_UPDATES,
                "Course updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "New classes, lessons, tests, and study material"
            },
            NotificationChannel(
                ACHIEVEMENTS,
                "Achievements",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Badges, streak milestones, and goal completion"
            },
            NotificationChannel(
                COMMUNITY,
                "Community",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Mehfil replies, mentions, and teacher responses"
            },
            NotificationChannel(
                ACCOUNT_SYSTEM,
                "Account and system",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Security, payment, subscription, and important account alerts"
            },
            NotificationChannel(
                ANNOUNCEMENTS,
                "Announcements",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Admin announcements and SAFAR campaigns"
            },
        )

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(channels)
    }

    fun normalize(channelId: String?): String = when (channelId) {
        FOCUS_TIMER,
        FOCUS_SHIELD_ALERTS,
        FOCUS_SHIELD_STATUS,
        FOCUS_SHIELD_BLOCKED,
        STUDY_REMINDERS,
        COURSE_UPDATES,
        ACHIEVEMENTS,
        COMMUNITY,
        ACCOUNT_SYSTEM,
        ANNOUNCEMENTS -> channelId
        else -> ACCOUNT_SYSTEM
    }
}
