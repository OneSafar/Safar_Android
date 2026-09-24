package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.R
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
    val disclosureAccepted: Boolean = false,
    val reference: String = "",
    val resolving: Boolean = false,
    val allowed: List<YoutubeV2IdentityEntity> = emptyList(),
    val classifications: Map<String, YoutubeChannelClassification> = emptyMap(),
    val available: List<ResolvedYoutubeChannelDto> = emptyList(),
    val availableExpanded: Boolean = false,
    val loadingAvailable: Boolean = false,
    val setupStep: Int = 1,
    val setupCompleted: Boolean = false,
    val bannerDismissed: Boolean = false,
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
        val bannerDismissed: Boolean,
    )
    private val setup = combine(
        preferences.enabled,
        preferences.setupStep,
        preferences.setupCompleted,
        preferences.bannerDismissed,
    ) { enabled, step, completed, bannerDismissed ->
        SetupState(enabled, step, completed, bannerDismissed)
    }
    private val starterChannels = listOf(
        ResolvedYoutubeChannelDto("starter:parmarssc", "@parmarssc", "SAFAR Parmar"),
        ResolvedYoutubeChannelDto("starter:safarparmar", "@safarparmar", "Safar"),
    )

    val state = combine(local, setup, repository.allowedChannels, repository.classifications) { ui, setupState, allowed, classifications ->
        val classMap = classifications.associate { it.channelId to YoutubeChannelClassification.fromWire(it.classification) }

        // Channels that were manually added via @handle (allowed table JOIN identity)
        val allowedDtos = allowed.map { entity ->
            ResolvedYoutubeChannelDto(
                channelId = entity.channelId,
                handle = entity.handle,
                displayName = entity.displayName,
                thumbnailUrl = entity.thumbnailUrl,
            )
        }

        val mergedList = allowedDtos.distinctBy { it.channelId }

        val mergedAvailable = if (!setupState.completed) {
            (mergedList + starterChannels).distinctBy { it.handle.ifBlank { it.displayName }.lowercase() }
        } else {
            mergedList.distinctBy { it.handle.ifBlank { it.displayName }.lowercase() }
        }
        ui.copy(
            enabled = setupState.enabled,
            setupStep = setupState.step,
            setupCompleted = setupState.completed,
            bannerDismissed = setupState.bannerDismissed,
            allowed = allowed,
            available = mergedAvailable,
            classifications = classMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YoutubeStudyV2UiState())

    init {
        refreshPermission()
    }

    fun dismissEkagraBanner() {
        preferences.dismissBanner()
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
        refreshPermission()
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
        if (local.value.resolving) return
        val reference = local.value.reference
        if (reference.isBlank()) return
        local.value = local.value.copy(resolving = true, message = null, isError = false)
        viewModelScope.launch {
            repository.resolveAndAllow(reference)
                .onSuccess { resolution ->
                    val channel = resolution.channel
                    local.value = local.value.copy(
                        resolving = false,
                        reference = if (local.value.reference == reference) "" else local.value.reference,
                        message = context.getString(R.string.youtube_channel_productive, channel.displayName),
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        resolving = false,
                        message = it.message ?: "Enter the channel name or @handle exactly as YouTube shows it.",
                        isError = true,
                    )
                }
        }
    }

    fun toggleAvailable() {
        local.value = local.value.copy(availableExpanded = !local.value.availableExpanded)
    }

    fun setAvailableProductive(channel: ResolvedYoutubeChannelDto, productive: Boolean) {
        viewModelScope.launch {
            repository.setAvailableProductive(channel, productive)
                .onSuccess {
                    local.value = local.value.copy(
                        message = context.getString(
                            R.string.youtube_channel_classified,
                            it.displayName,
                            context.getString(if (productive) R.string.youtube_productive else R.string.youtube_distracting),
                        ),
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        message = context.getString(R.string.youtube_channel_update_failed),
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
        // Clear any stale message immediately so the old one never lingers
        local.value = local.value.copy(message = null, isError = false)
        viewModelScope.launch {
            repository.setAvailableClassification(channel, classification)
                .onSuccess {
                    val label = when (classification) {
                        YoutubeChannelClassification.PRODUCTIVE -> context.getString(R.string.youtube_productive)
                        YoutubeChannelClassification.DISTRACTING -> context.getString(R.string.youtube_distracting)
                        else -> null
                    }
                    local.value = local.value.copy(
                        message = label?.let { context.getString(R.string.youtube_channel_classified, channel.displayName, it) },
                        isError = false,
                    )
                }
                .onFailure {
                    local.value = local.value.copy(
                        message = context.getString(R.string.youtube_channel_update_failed),
                        isError = true,
                    )
                }
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
        }
    }

    fun refreshPermission() {
        val accessibilityEnabled = YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(context)
        local.value = local.value.copy(
            accessibilityEnabled = accessibilityEnabled,
            disclosureAccepted = preferences.isDisclosureAccepted(),
        )
        if (accessibilityEnabled && preferences.isDisclosureAccepted()) {
            if (!preferences.setupCompleted.value) {
                preferences.completeSetup()
                preferences.setEnabled(true)
                // Accessibility may connect before setup flips enabled. Start
                // foreground support here too so that ordering cannot leave it off.
                YoutubeStudyV2GuardService.start(context)
            }
        } else {
            if (!preferences.setupCompleted.value && preferences.setupStep.value > 1) {
                preferences.setSetupStep(1)
            }
        }
    }
}
