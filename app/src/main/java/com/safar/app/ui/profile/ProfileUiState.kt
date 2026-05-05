package com.safar.app.ui.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatar: String? = null,
    val examType: String = "",
    val preparationStage: String = "",
    val gender: String = "",
    val editName: String = "",
    val editExamType: String = "",
    val editStage: String = "",
    val editGender: String = "",
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
    val saveSuccess: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null
)

sealed class ProfileEvent {
    object ShowLogoutDialog : ProfileEvent()
    object DismissLogoutDialog : ProfileEvent()
    object Logout : ProfileEvent()
    object ClearError : ProfileEvent()
    object SaveProfile : ProfileEvent()
    data class UpdateName(val name: String) : ProfileEvent()
    data class UpdateExamType(val exam: String) : ProfileEvent()
    data class UpdateStage(val stage: String) : ProfileEvent()
    data class UpdateGender(val gender: String) : ProfileEvent()
    data class ToggleNotifications(val enabled: Boolean) : ProfileEvent()
    data class ToggleFocusTimerNotifications(val enabled: Boolean) : ProfileEvent()
    data class ToggleDailyStudyReminder(val enabled: Boolean) : ProfileEvent()
    data class ToggleStreakReminder(val enabled: Boolean) : ProfileEvent()
    data class ToggleCourseUpdates(val enabled: Boolean) : ProfileEvent()
    data class ToggleAchievements(val enabled: Boolean) : ProfileEvent()
    data class ToggleCommunityReplies(val enabled: Boolean) : ProfileEvent()
    data class ToggleAnnouncements(val enabled: Boolean) : ProfileEvent()
    data class ToggleWeeklySummary(val enabled: Boolean) : ProfileEvent()
    data class UpdateDailyReminderTime(val time: String) : ProfileEvent()
}
