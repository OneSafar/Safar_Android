package com.safar.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.NotificationApi
import com.safar.app.data.remote.dto.NotificationPreferencesRequest
import com.safar.app.notifications.PlannerAlertsWorker
import com.safar.app.notifications.StudyReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SafarDataStore,
    private val notificationApi: NotificationApi,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var preferenceSyncJob: Job? = null

    init {
        observeNotificationPreferences()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleNotifications -> updateNotificationsEnabled(event.enabled)
            is SettingsEvent.ToggleFocusTimerNotifications -> updatePreference { dataStore.setFocusTimerNotificationsEnabled(event.enabled) }
            is SettingsEvent.ToggleDailyStudyReminder -> updateDailyStudyReminder(event.enabled)
            is SettingsEvent.ToggleStreakReminder -> updatePreference { dataStore.setStreakReminderEnabled(event.enabled) }
            is SettingsEvent.ToggleCourseUpdates -> updatePreference { dataStore.setCourseUpdatesEnabled(event.enabled) }
            is SettingsEvent.ToggleAchievements -> updatePreference { dataStore.setAchievementsEnabled(event.enabled) }
            is SettingsEvent.ToggleCommunityReplies -> updatePreference { dataStore.setCommunityRepliesEnabled(event.enabled) }
            is SettingsEvent.ToggleAnnouncements -> updatePreference { dataStore.setAnnouncementsEnabled(event.enabled) }
            is SettingsEvent.ToggleWeeklySummary -> updatePreference { dataStore.setWeeklySummaryEnabled(event.enabled) }
            is SettingsEvent.UpdateDailyReminderTime -> updateDailyReminderTime(event.time)
        }
    }

    private fun observeNotificationPreferences() {
        viewModelScope.launch {
            combine(
                listOf(
                    dataStore.notificationsEnabled.map { it as Any },
                    dataStore.focusTimerNotificationsEnabled.map { it as Any },
                    dataStore.dailyStudyReminderEnabled.map { it as Any },
                    dataStore.streakReminderEnabled.map { it as Any },
                    dataStore.courseUpdatesEnabled.map { it as Any },
                    dataStore.achievementsEnabled.map { it as Any },
                    dataStore.communityRepliesEnabled.map { it as Any },
                    dataStore.announcementsEnabled.map { it as Any },
                    dataStore.weeklySummaryEnabled.map { it as Any },
                    dataStore.dailyReminderTime.map { it as Any },
                    dataStore.quietHoursStart.map { it as Any },
                    dataStore.quietHoursEnd.map { it as Any },
                ),
            ) { values ->
                SettingsUiState(
                    notificationsEnabled = values[0] as Boolean,
                    focusTimerNotificationsEnabled = values[1] as Boolean,
                    dailyStudyReminderEnabled = values[2] as Boolean,
                    streakReminderEnabled = values[3] as Boolean,
                    courseUpdatesEnabled = values[4] as Boolean,
                    achievementsEnabled = values[5] as Boolean,
                    communityRepliesEnabled = values[6] as Boolean,
                    announcementsEnabled = values[7] as Boolean,
                    weeklySummaryEnabled = values[8] as Boolean,
                    dailyReminderTime = values[9] as String,
                    quietHoursStart = values[10] as String,
                    quietHoursEnd = values[11] as String,
                )
            }.collect { state -> _uiState.value = state }
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
            schedulePreferenceSync()
        }
    }

    private fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setNotificationsEnabled(enabled)
            if (!enabled) {
                StudyReminderWorker.cancel(appContext)
                PlannerAlertsWorker.cancel(appContext)
            } else if (dataStore.dailyStudyReminderEnabled.first()) {
                StudyReminderWorker.schedule(appContext, dataStore.dailyReminderTime.first())
                PlannerAlertsWorker.schedule(appContext, dataStore.dailyReminderTime.first())
            }
            schedulePreferenceSync()
        }
    }

    private fun updateDailyStudyReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setDailyStudyReminderEnabled(enabled)
            if (enabled) {
                StudyReminderWorker.schedule(appContext, dataStore.dailyReminderTime.first())
                PlannerAlertsWorker.schedule(appContext, dataStore.dailyReminderTime.first())
            } else {
                StudyReminderWorker.cancel(appContext)
                PlannerAlertsWorker.cancel(appContext)
            }
            schedulePreferenceSync()
        }
    }

    private fun updateDailyReminderTime(time: String) {
        if (!isValidReminderTime(time)) return
        viewModelScope.launch {
            dataStore.setDailyReminderTime(time)
            if (dataStore.dailyStudyReminderEnabled.first()) {
                StudyReminderWorker.schedule(appContext, time)
                PlannerAlertsWorker.schedule(appContext, time)
            }
            schedulePreferenceSync()
        }
    }

    private fun isValidReminderTime(time: String): Boolean {
        if (!Regex("^\\d{2}:\\d{2}$").matches(time)) return false
        val parts = time.split(":")
        if (parts.size != 2) return false
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false
        return hour in 0..23 && minute in 0..59
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
}
