package com.safar.app.ui.settings

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val focusTimerNotificationsEnabled: Boolean = true,
    val dailyStudyReminderEnabled: Boolean = false,
    val streakReminderEnabled: Boolean = true,
    val courseUpdatesEnabled: Boolean = true,
    val achievementsEnabled: Boolean = false,
    val communityRepliesEnabled: Boolean = false,
    val announcementsEnabled: Boolean = false,
    val weeklySummaryEnabled: Boolean = false,
    val dailyReminderTime: String = "19:00",
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
)

sealed class SettingsEvent {
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent()
    data class ToggleFocusTimerNotifications(val enabled: Boolean) : SettingsEvent()
    data class ToggleDailyStudyReminder(val enabled: Boolean) : SettingsEvent()
    data class ToggleStreakReminder(val enabled: Boolean) : SettingsEvent()
    data class ToggleCourseUpdates(val enabled: Boolean) : SettingsEvent()
    data class ToggleAchievements(val enabled: Boolean) : SettingsEvent()
    data class ToggleCommunityReplies(val enabled: Boolean) : SettingsEvent()
    data class ToggleAnnouncements(val enabled: Boolean) : SettingsEvent()
    data class ToggleWeeklySummary(val enabled: Boolean) : SettingsEvent()
    data class UpdateDailyReminderTime(val time: String) : SettingsEvent()
}
