package com.safarparmar.app.data.remote.dto

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

data class AdminBroadcastRequest(
    val type: String = "announcements",
    val channel: String = "announcements",
    val priority: String = "high",
    val title: String,
    val body: String,
    val deepLink: String,
    val broadcast: Boolean = true,
)

data class AdminBroadcastResult(
    val tokenPreview: String? = null,
    val success: Boolean = false,
    val messageId: String? = null,
    val error: String? = null,
)

data class AdminBroadcastResponse(
    val message: String? = null,
    val count: Int? = null,
    val results: List<AdminBroadcastResult>? = null,
)

data class CustomNotificationDto(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val channel: String? = null,
    val deepLink: String? = null,
    val priority: String? = null,
    val createdAt: String? = null,
)

data class NotificationHistoryResponse(
    val notifications: List<CustomNotificationDto>? = null,
    val page: Int? = null,
    val hasMore: Boolean? = null,
)
