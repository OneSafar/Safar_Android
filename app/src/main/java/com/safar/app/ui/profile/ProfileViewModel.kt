package com.safar.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.NotificationApi
import com.safar.app.data.remote.dto.NotificationPreferencesRequest
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.notifications.StudyReminderWorker
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: SafarDataStore,
    private val notificationApi: NotificationApi,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var preferenceSyncJob: Job? = null

    init {
        loadProfile()
        loadNotificationPreferences()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = authRepository.getMe()) {
                is Resource.Success -> {
                    val p = r.data
                    _uiState.update { it.copy(isLoading = false, userName = p.name, userEmail = p.email, userAvatar = p.avatar, examType = p.examType ?: "", preparationStage = p.preparationStage ?: "", gender = p.gender ?: "", editName = p.name, editExamType = p.examType ?: "", editStage = p.preparationStage ?: "", editGender = p.gender ?: "") }
                }
                is Resource.Error -> {
                    val name = dataStore.userName.first() ?: ""
                    val avatar = dataStore.userAvatar.first()
                    _uiState.update { it.copy(isLoading = false, userName = name, userAvatar = avatar, editName = name) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ShowLogoutDialog  -> _uiState.update { it.copy(showLogoutDialog = true) }
            is ProfileEvent.DismissLogoutDialog -> _uiState.update { it.copy(showLogoutDialog = false) }
            is ProfileEvent.ClearError        -> _uiState.update { it.copy(error = null) }
            is ProfileEvent.Logout            -> handleLogout()
            is ProfileEvent.SaveProfile       -> saveProfile()
            is ProfileEvent.UpdateName        -> _uiState.update { it.copy(editName = event.name) }
            is ProfileEvent.UpdateExamType    -> _uiState.update { it.copy(editExamType = event.exam) }
            is ProfileEvent.UpdateStage       -> _uiState.update { it.copy(editStage = event.stage) }
            is ProfileEvent.UpdateGender      -> _uiState.update { it.copy(editGender = event.gender) }
            is ProfileEvent.ToggleNotifications -> updateNotificationsEnabled(event.enabled)
            is ProfileEvent.ToggleFocusTimerNotifications -> updatePreference { dataStore.setFocusTimerNotificationsEnabled(event.enabled) }
            is ProfileEvent.ToggleDailyStudyReminder -> updateDailyStudyReminder(event.enabled)
            is ProfileEvent.ToggleStreakReminder -> updatePreference { dataStore.setStreakReminderEnabled(event.enabled) }
            is ProfileEvent.ToggleCourseUpdates -> updatePreference { dataStore.setCourseUpdatesEnabled(event.enabled) }
            is ProfileEvent.ToggleAchievements -> updatePreference { dataStore.setAchievementsEnabled(event.enabled) }
            is ProfileEvent.ToggleCommunityReplies -> updatePreference { dataStore.setCommunityRepliesEnabled(event.enabled) }
            is ProfileEvent.ToggleAnnouncements -> updatePreference { dataStore.setAnnouncementsEnabled(event.enabled) }
            is ProfileEvent.ToggleWeeklySummary -> updatePreference { dataStore.setWeeklySummaryEnabled(event.enabled) }
            is ProfileEvent.UpdateDailyReminderTime -> updateDailyReminderTime(event.time)
        }
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    notificationsEnabled = dataStore.notificationsEnabled.first(),
                    focusTimerNotificationsEnabled = dataStore.focusTimerNotificationsEnabled.first(),
                    dailyStudyReminderEnabled = dataStore.dailyStudyReminderEnabled.first(),
                    streakReminderEnabled = dataStore.streakReminderEnabled.first(),
                    courseUpdatesEnabled = dataStore.courseUpdatesEnabled.first(),
                    achievementsEnabled = dataStore.achievementsEnabled.first(),
                    communityRepliesEnabled = dataStore.communityRepliesEnabled.first(),
                    announcementsEnabled = dataStore.announcementsEnabled.first(),
                    weeklySummaryEnabled = dataStore.weeklySummaryEnabled.first(),
                    dailyReminderTime = dataStore.dailyReminderTime.first(),
                    quietHoursStart = dataStore.quietHoursStart.first(),
                    quietHoursEnd = dataStore.quietHoursEnd.first(),
                )
            }
        }
    }

    private fun updatePreference(write: suspend () -> Unit) {
        viewModelScope.launch {
            write()
            loadNotificationPreferences()
            schedulePreferenceSync()
        }
    }

    private fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setNotificationsEnabled(enabled)
            if (!enabled) StudyReminderWorker.cancel(appContext)
            loadNotificationPreferences()
            schedulePreferenceSync()
        }
    }

    private fun updateDailyStudyReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setDailyStudyReminderEnabled(enabled)
            if (enabled) {
                StudyReminderWorker.schedule(appContext, dataStore.dailyReminderTime.first())
            } else {
                StudyReminderWorker.cancel(appContext)
            }
            loadNotificationPreferences()
            schedulePreferenceSync()
        }
    }

    private fun updateDailyReminderTime(time: String) {
        viewModelScope.launch {
            dataStore.setDailyReminderTime(time)
            if (dataStore.dailyStudyReminderEnabled.first()) {
                StudyReminderWorker.schedule(appContext, time)
            }
            loadNotificationPreferences()
        }
    }

    private fun schedulePreferenceSync() {
        preferenceSyncJob?.cancel()
        preferenceSyncJob = viewModelScope.launch {
            delay(1200)
            val isLoggedIn = dataStore.isLoggedIn.first()
            val authToken = dataStore.authToken.first()
            if (!isLoggedIn || authToken.isNullOrBlank()) return@launch
            val request = NotificationPreferencesRequest(
                focus_timer_enabled = dataStore.focusTimerNotificationsEnabled.first(),
                daily_study_enabled = dataStore.dailyStudyReminderEnabled.first(),
                streak_enabled = dataStore.streakReminderEnabled.first(),
                course_updates_enabled = dataStore.courseUpdatesEnabled.first(),
                community_enabled = dataStore.communityRepliesEnabled.first(),
                account_system_enabled = true,
                announcements_enabled = dataStore.announcementsEnabled.first(),
                quiet_hours_start = dataStore.quietHoursStart.first(),
                quiet_hours_end = dataStore.quietHoursEnd.first(),
                timezone = TimeZone.getDefault().id,
            )
            runCatching { notificationApi.updateNotificationPreferences(request) }
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val s = _uiState.value
            when (val r = authRepository.updateProfile(s.editName.ifBlank { null }, s.editExamType.ifBlank { null }, s.editStage.ifBlank { null }, s.editGender.ifBlank { null }, null)) {
                is Resource.Success -> _uiState.update { it.copy(isSaving = false, saveSuccess = true, userName = r.data.name, examType = r.data.examType ?: "", preparationStage = r.data.preparationStage ?: "", gender = r.data.gender ?: "") }
                is Resource.Error   -> _uiState.update { it.copy(isSaving = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { authRepository.logout(); onDone() }
    }
}
