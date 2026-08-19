package com.safarparmar.app.feature.youtubeinsights

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeChannelEntity
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YoutubeStudyModeUiState(
    val enabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val consentVersion: Int = 0,
    val hasAccessibility: Boolean = false,
    val hasNotifications: Boolean = false,
    val channels: List<YoutubeChannelEntity> = emptyList(),
)

private data class YoutubePreferences(
    val enabled: Boolean,
    val onboardingDone: Boolean,
    val consentVersion: Int,
)

@HiltViewModel
class YoutubeStudyModeViewModel @Inject constructor(
    private val app: Application,
    private val dataStore: SafarDataStore,
    private val repository: YoutubeInsightsRepository,
) : ViewModel() {
    private val channels = MutableStateFlow<List<YoutubeChannelEntity>>(emptyList())
    private val permissionTick = MutableStateFlow(0)

    private val preferences = combine(
        dataStore.youtubeInsightsEnabled,
        dataStore.youtubeStudyOnboardingDone,
        dataStore.youtubeAccessibilityConsentVersion,
    ) { enabled, onboarding, consent ->
        YoutubePreferences(enabled, onboarding, consent)
    }

    val state = combine(preferences, channels, permissionTick) { prefs, rows, _ ->
        YoutubeStudyModeUiState(
            enabled = prefs.enabled,
            onboardingDone = prefs.onboardingDone,
            consentVersion = prefs.consentVersion,
            hasAccessibility = FocusShieldPermissionHelper.hasAccessibilityService(app),
            hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(app),
            channels = rows,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YoutubeStudyModeUiState())

    init {
        viewModelScope.launch {
            repository.seedStarterChannels()
            refreshChannels()
        }
    }

    fun refresh() {
        permissionTick.value += 1
        viewModelScope.launch { refreshChannels() }
    }

    fun setEnabled(value: Boolean) = viewModelScope.launch { dataStore.setYoutubeInsightsEnabled(value) }

    fun setProductive(key: String, productive: Boolean) = viewModelScope.launch {
        repository.setProductive(key, productive)
        refreshChannels()
    }

    fun recordConsentAndEnable() = viewModelScope.launch {
        dataStore.recordYoutubeAccessibilityConsent(CONSENT_VERSION)
        dataStore.setYoutubeInsightsEnabled(true)
    }

    fun finishOnboarding() = viewModelScope.launch {
        dataStore.recordYoutubeAccessibilityConsent(CONSENT_VERSION)
        dataStore.setYoutubeInsightsEnabled(true)
        dataStore.setYoutubeStudyOnboardingDone(true)
    }

    private suspend fun refreshChannels() {
        channels.value = repository.channels()
    }

    companion object { const val CONSENT_VERSION = 2 }
}
