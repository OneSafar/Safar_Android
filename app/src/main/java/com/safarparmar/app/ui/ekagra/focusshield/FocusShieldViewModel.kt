package com.safarparmar.app.ui.ekagra.focusshield

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
        shieldSettings, _permissionRefreshTick,
    ) { settings, _ ->
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusShieldUiState())

    // ── App picker state ─────────────────────────────────────────────────────

    private val _pickerState = MutableStateFlow(AppPickerUiState())
    val pickerState: StateFlow<AppPickerUiState> = _pickerState.asStateFlow()

    fun loadApps() {
        viewModelScope.launch {
            _pickerState.value = _pickerState.value.copy(isLoading = true)
            val apps = appsLoader.loadLaunchableApps()
            val blocked = repo.blockedPackages.value
            _pickerState.value = AppPickerUiState(
                isLoading = false,
                allApps = apps.map { it.copy(isBlocked = it.packageName in blocked) },
            )
        }
    }

    fun setSearchQuery(query: String) {
        _pickerState.value = _pickerState.value.copy(searchQuery = query)
    }

    fun toggleApp(packageName: String) {
        val current = repo.blockedPackages.value.toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        repo.setBlockedPackages(current)
        // Update picker list to reflect change
        _pickerState.value = _pickerState.value.copy(
            allApps = _pickerState.value.allApps.map {
                if (it.packageName == packageName) it.copy(isBlocked = !it.isBlocked) else it
            },
        )
    }

    fun selectAllApps() {
        val allPackages = _pickerState.value.allApps.map { it.packageName }.toSet()
        repo.setBlockedPackages(allPackages)
        _pickerState.value = _pickerState.value.copy(
            allApps = _pickerState.value.allApps.map { it.copy(isBlocked = true) }
        )
    }

    fun deselectAllApps() {
        repo.setBlockedPackages(emptySet())
        _pickerState.value = _pickerState.value.copy(
            allApps = _pickerState.value.allApps.map { it.copy(isBlocked = false) }
        )
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
            "com.google.android.youtube",
            "com.reddit.frontpage"
        )
        val current = repo.blockedPackages.value.toMutableSet()
        val appsToBlock = _pickerState.value.allApps.filter { it.packageName in distractingPackages }.map { it.packageName }
        current.addAll(appsToBlock)
        repo.setBlockedPackages(current)
        
        _pickerState.value = _pickerState.value.copy(
            allApps = _pickerState.value.allApps.map {
                if (it.packageName in distractingPackages) it.copy(isBlocked = true) else it
            }
        )
    }

    // ── Shield settings actions ──────────────────────────────────────────────

    fun setEnabled(enabled: Boolean) = repo.setEnabled(enabled)
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
