package com.safar.app.data.remote.dto

data class DeviceTokenRequest(
    val userId: String?,
    val deviceToken: String,
    val platform: String = "android",
    val appVersion: String,
    val flavor: String,
    val language: String,
    val notificationsEnabled: Boolean,
)

data class DeviceTokenRevokeRequest(
    val deviceToken: String,
)

data class NotificationPreferencesRequest(
    val focus_timer_enabled: Boolean,
    val daily_study_enabled: Boolean,
    val streak_enabled: Boolean,
    val course_updates_enabled: Boolean,
    val community_enabled: Boolean,
    val account_system_enabled: Boolean,
    val announcements_enabled: Boolean,
    val quiet_hours_start: String,
    val quiet_hours_end: String,
    val timezone: String,
)
