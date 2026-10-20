package com.safarparmar.app.ui.ekagra.focusshield

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import com.safarparmar.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusShieldUiState(
    val isEnabled: Boolean = false,
    val isAlwaysOnMode: Boolean = false,
    val isStrictMode: Boolean = false,
    val savedAppUsageMode: String? = null,
    val blockedPackages: Set<String> = emptySet(),
    val isScheduleEnabled: Boolean = false,
    val scheduleStartMinute: Int = 540,
    val scheduleEndMinute: Int = 1320,
    val hasOverlayPermission: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasNotificationSuppressionAccess: Boolean = false,
    val hasUsageStats: Boolean = false,
    val isProtectionActive: Boolean = false,
    val isProtectionStarting: Boolean = false,
    val activationMessage: String? = null,
)

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val allApps: List<BlockedAppInfo> = emptyList(),
    val searchQuery: String = "",
)

private data class FocusShieldSettingsState(
    val enabled: Boolean,
    val alwaysOn: Boolean,
    val strict: Boolean,
    val savedMode: String?,
    val packages: Set<String>,
    val scheduleEnabled: Boolean,
    val scheduleStartMinute: Int,
    val scheduleEndMinute: Int,
)

@HiltViewModel
class FocusShieldViewModel @Inject constructor(
    private val app: Application,
    private val repo: FocusShieldRepository,
    private val appsLoader: InstalledAppsLoader,
) : ViewModel() {

    // ── Shield settings state ────────────────────────────────────────────────

    private val _permissionRefreshTick = MutableStateFlow(0)

    private val shieldSettings = combine(
        repo.isEnabled,
        repo.isAlwaysOnMode,
        repo.isStrictMode,
        repo.appUsageMode,
        repo.blockedPackages,
        repo.scheduleEnabled,
        repo.scheduleStartMinute,
        repo.scheduleEndMinute,
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        FocusShieldSettingsState(
            enabled = array[0] as Boolean,
            alwaysOn = array[1] as Boolean,
            strict = array[2] as Boolean,
            savedMode = array[3] as String?,
            packages = array[4] as Set<String>,
            scheduleEnabled = array[5] as Boolean,
            scheduleStartMinute = array[6] as Int,
            scheduleEndMinute = array[7] as Int,
        )
    }

    val shieldState: StateFlow<FocusShieldUiState> = combine(
        shieldSettings,
        repo.protectionActive,
        repo.protectionStarting,
        repo.activationBlockedReason,
        _permissionRefreshTick,
    ) { settings, protectionActive, protectionStarting, activationMessage, _ ->
        FocusShieldUiState(
            isEnabled = settings.enabled,
            isAlwaysOnMode = settings.alwaysOn,
            isStrictMode = settings.strict,
            savedAppUsageMode = settings.savedMode,
            blockedPackages = settings.packages,
            isScheduleEnabled = settings.scheduleEnabled,
            scheduleStartMinute = settings.scheduleStartMinute,
            scheduleEndMinute = settings.scheduleEndMinute,
            hasOverlayPermission = FocusShieldPermissionHelper.hasOverlayPermission(app),
            hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(app),
            hasNotificationSuppressionAccess = FocusShieldPermissionHelper.hasNotificationListenerAccess(app),
            hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(app),
            isProtectionActive = protectionActive,
            isProtectionStarting = protectionStarting,
            activationMessage = activationMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusShieldUiState())

    // ── App picker state ─────────────────────────────────────────────────────

    private val _pickerState = MutableStateFlow(AppPickerUiState())
    val pickerState: StateFlow<AppPickerUiState> = _pickerState.asStateFlow()
    val youtubeFocusEnabled: StateFlow<Boolean> = repo.youtubeFocusEnabled
    val youtubeSelectionBusy: StateFlow<Boolean> = repo.youtubeSelectionBusy
    val youtubeSelectionError: StateFlow<String?> = repo.youtubeFocusActivationError

    init {
        viewModelScope.launch {
            repo.blockedPackages.collect { packages ->
                // Focus can remove YouTube while this picker stays open. The
                // confirmed Kavach override also publishes its persisted choice here.
                _pickerState.update { state ->
                    state.copy(allApps = state.allApps.map { it.copy(isBlocked = it.packageName in packages) })
                }
            }
        }
    }

    private fun applyPickerSelection(packages: Set<String>) {
        val saved = repo.setBlockedPackages(packages)
        _pickerState.value = _pickerState.value.copy(
            allApps = _pickerState.value.allApps.map { it.copy(isBlocked = it.packageName in saved) },
        )
    }

    fun loadApps() {
        viewModelScope.launch {
            _pickerState.value = _pickerState.value.copy(isLoading = true)
            val apps = appsLoader.loadLaunchableApps()
            val blocked = repo.selectedPackages()
            _pickerState.update { state ->
                state.copy(
                    isLoading = false,
                    allApps = apps.map { it.copy(isBlocked = it.packageName in blocked) },
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _pickerState.value = _pickerState.value.copy(searchQuery = query)
    }

    fun toggleApp(packageName: String) {
        val current = repo.selectedPackages().toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        applyPickerSelection(current)
    }

    /** The repository publishes the selected row only after the explicit override is saved. */
    fun selectYoutubeForKavach() = repo.selectYoutubeForKavach()

    fun selectAllApps() {
        val allPackages = _pickerState.value.allApps.map { it.packageName }.toSet()
        applyPickerSelection(allPackages)
    }

    fun deselectAllApps() {
        applyPickerSelection(emptySet())
    }

    fun selectDistractingApps() {
        val distractingPackages = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.whatsapp",
            "com.snapchat.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "tv.twitch.android.app",
            "com.netflix.mediaclient",
            "com.reddit.frontpage"
        )
        val current = repo.selectedPackages().toMutableSet()
        val appsToBlock = _pickerState.value.allApps.filter { it.packageName in distractingPackages }.map { it.packageName }
        current.addAll(appsToBlock)
        applyPickerSelection(current)
    }

    fun setPendingEnableAfterAppSelection(pending: Boolean) {
        repo.pendingEnableAfterAppSelection = pending
    }

    fun isPendingEnableAfterAppSelection(): Boolean = repo.pendingEnableAfterAppSelection

    /**
     * App selections are saved as they change. Leaving the picker only activates
     * Kavach when the student opened it to finish an explicit enable request.
     */
    fun saveAndEnableShieldIfReady(): Boolean {
        if (!repo.pendingEnableAfterAppSelection) return false
        if (repo.isEnabled.value) {
            repo.pendingEnableAfterAppSelection = false
            return false
        }
        // The displayed rows can be loading or stale after Focus removes YouTube.
        // Read the authoritative selection instead of writing those rows back.
        if (repo.selectedPackages().isEmpty()) return false
        val hasUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(app)
        val hasOverlay = FocusShieldPermissionHelper.hasOverlayPermission(app)
        if (!KavachActivationReadiness.hasRequiredPermissions(hasUsage, hasOverlay)) return false

        repo.pendingEnableAfterAppSelection = false
        repo.setEnabled(true)
        return true
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && repo.selectedPackages().isEmpty()) {
            Toast.makeText(app, app.getString(R.string.kavach_select_app_first), Toast.LENGTH_SHORT).show()
            return
        }
        repo.setEnabled(enabled)
    }
    fun clearActivationMessage() = repo.clearActivationMessage()
    fun setKavachProfile(mode: String) = repo.setKavachProfile(mode)
    fun setStrictMode(enabled: Boolean) = repo.setStrictMode(enabled)
    fun setScheduleEnabled(enabled: Boolean) = repo.setScheduleEnabled(enabled)
    fun setScheduleRange(startMinute: Int, endMinute: Int) = repo.setScheduleRange(startMinute, endMinute)
    fun refreshPermissions() {
        // Force a re-emission by triggering one of the combine inputs
        // Since we already read permissions inside combine, we just need
        // to nudge the flow. Easiest: toggle a no-op value and back.
        // Actually, permissions are read inside combine's lambda on each
        // emission of any input. We just re-read them here for immediate UI.
        _permissionRefreshTick.value += 1
        // No-op — the permissions are re-checked on every emission
    }

    fun openOverlaySettings() = FocusShieldPermissionHelper.openOverlaySettings(app)

    val blockedHitCount: StateFlow<Int> = repo.blockedHitCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockedHitsByPackage: StateFlow<Map<String, Int>> = repo.blockedHitsByPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun clearSessionStats() = repo.clearSessionStats()
}
