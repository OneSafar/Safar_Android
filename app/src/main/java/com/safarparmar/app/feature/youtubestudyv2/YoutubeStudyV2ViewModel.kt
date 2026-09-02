package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class YoutubeStudyV2UiState(
    val enabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val reference: String = "",
    val resolving: Boolean = false,
    val allowed: List<YoutubeV2IdentityEntity> = emptyList(),
    val classifications: Map<String, YoutubeChannelClassification> = emptyMap(),
    val available: List<ResolvedYoutubeChannelDto> = emptyList(),
    val availableExpanded: Boolean = false,
    val loadingAvailable: Boolean = false,
    val setupStep: Int = 1,
    val setupCompleted: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class YoutubeStudyV2ViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: YoutubeStudyV2Repository,
    private val preferences: YoutubeStudyV2Preferences,
) : ViewModel() {
    private val local = MutableStateFlow(YoutubeStudyV2UiState())
    private data class SetupState(
        val enabled: Boolean,
        val step: Int,
        val completed: Boolean,
    )
    private val setup = combine(
        preferences.enabled,
        preferences.setupStep,
        preferences.setupCompleted,
    ) { enabled, step, completed ->
        SetupState(enabled, step, completed)
    }
    private val starterChannels = listOf(
        ResolvedYoutubeChannelDto("starter:parmarssc", "@parmarssc", "SAFAR Parmar"),
        ResolvedYoutubeChannelDto("starter:safarparmar", "@safarparmar", "Safar"),
    )

    val state = combine(local, setup, repository.allowedChannels, repository.visitedChannels, repository.classifications) { ui, setupState, allowed, visited, classifications ->
        val localList = visited.map { entity ->
            ResolvedYoutubeChannelDto(
                channelId = entity.channelId,
                handle = entity.handle,
                displayName = entity.displayName,
                thumbnailUrl = entity.thumbnailUrl,
            )
        }
        val mergedAvailable = (localList + starterChannels).distinctBy { it.handle.lowercase() }
        ui.copy(
            enabled = setupState.enabled,
            setupStep = setupState.step,
            setupCompleted = setupState.completed,
            allowed = allowed,
            available = mergedAvailable,
            classifications = classifications.associate { it.channelId to YoutubeChannelClassification.fromWire(it.classification) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YoutubeStudyV2UiState())

    init {
        refreshPermission()
    }

    fun setReference(value: String) { local.value = local.value.copy(reference = value, message = null) }

    fun setEnabled(enabled: Boolean) {
        preferences.setEnabled(enabled)
        if (!enabled) YoutubeStudyV2GuardService.stop(context)
        else if (YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(context)) YoutubeStudyV2GuardService.start(context)
        refreshPermission()
    }

    fun acceptDisclosure() {
        preferences.acceptDisclosure()
    }

    fun goToStep2() {
        preferences.setSetupStep(2)
    }

    fun returnToStep1() {
        preferences.setSetupStep(1)
    }

    fun finishSetup() {
        preferences.completeSetup()
        setEnabled(true)
    }

    fun resolveAndAllow() {
        val reference = local.value.reference
        local.value = local.value.copy(resolving = true, message = null)
        viewModelScope.launch {
            repository.resolveAndAllow(reference)
                .onSuccess { resolution ->
                    val channel = resolution.channel
                    local.value = local.value.copy(
                        resolving = false,
                        reference = "",
                        message = "${channel.displayName} is now Productive.",
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        resolving = false,
                        message = "Could not add channel. Check the @handle.",
                        isError = true,
                    )
                }
        }
    }

    fun toggleAvailable() {
        val expanded = !local.value.availableExpanded
        local.value = local.value.copy(availableExpanded = expanded)
        if (expanded && local.value.available.isEmpty()) loadAvailable()
    }

    fun loadAvailable() {
        if (local.value.loadingAvailable) return
        local.value = local.value.copy(loadingAvailable = true, message = null)
        viewModelScope.launch {
            repository.availableChannels()
                .onSuccess { channels ->
                    local.value = local.value.copy(
                        available = channels,
                        loadingAvailable = false,
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        loadingAvailable = false,
                        message = "Could not load channels. Try again.",
                        isError = true,
                    )
                }
        }
    }

    fun setAvailableProductive(channel: ResolvedYoutubeChannelDto, productive: Boolean) {
        viewModelScope.launch {
            repository.setAvailableProductive(channel, productive)
                .onSuccess {
                    local.value = local.value.copy(
                        message = "${it.displayName} is now ${if (productive) "Productive" else "Distracting"}.",
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        message = "Could not update channel. Try again.",
                        isError = true,
                    )
                }
        }
    }

    fun setProductive(channelId: String, productive: Boolean) {
        viewModelScope.launch { repository.setProductive(channelId, productive) }
    }

    fun setClassification(channelId: String, classification: YoutubeChannelClassification) {
        viewModelScope.launch { repository.setClassification(channelId, classification) }
    }

    fun setAvailableClassification(channel: ResolvedYoutubeChannelDto, classification: YoutubeChannelClassification) {
        viewModelScope.launch {
            repository.setAvailableClassification(channel, classification)
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
        }
    }

    fun refreshPermission() {
        val accessibilityEnabled = YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(context)
        local.value = local.value.copy(accessibilityEnabled = accessibilityEnabled)
        if (!preferences.setupCompleted.value) {
            when {
                accessibilityEnabled && preferences.isDisclosureAccepted() && preferences.setupStep.value == 1 ->
                    preferences.setSetupStep(2)
                !accessibilityEnabled && preferences.setupStep.value > 1 -> preferences.setSetupStep(1)
            }
        }
    }
}
