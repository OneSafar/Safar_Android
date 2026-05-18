package com.safar.app.ui.ekagra.focusshield

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
    val isStrictMode: Boolean = false,
    val allowEmergencyUnlock: Boolean = true,
    val blockedPackages: Set<String> = emptySet(),
    val hasAccessibilityService: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasUsageStats: Boolean = false,
)

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val allApps: List<BlockedAppInfo> = emptyList(),
    val searchQuery: String = "",
)

@HiltViewModel
class FocusShieldViewModel @Inject constructor(
    private val app: Application,
    private val repo: FocusShieldRepository,
    private val appsLoader: InstalledAppsLoader,
) : ViewModel() {

    // ── Shield settings state ────────────────────────────────────────────────

    private val _permissionRefreshTick = MutableStateFlow(0)

    val shieldState: StateFlow<FocusShieldUiState> = combine(
        repo.isEnabled,
        repo.isStrictMode,
        repo.allowEmergencyUnlock,
        repo.blockedPackages,
        _permissionRefreshTick,
    ) { enabled, strict, emergency, packages, _ ->
        FocusShieldUiState(
            isEnabled = enabled,
            isStrictMode = strict,
            allowEmergencyUnlock = emergency,
            blockedPackages = packages,
            hasAccessibilityService = FocusShieldPermissionHelper.hasAccessibilityService(app),
            hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(app),
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

    // ── Shield settings actions ──────────────────────────────────────────────

    fun setEnabled(enabled: Boolean) = repo.setEnabled(enabled)
    fun setStrictMode(enabled: Boolean) = repo.setStrictMode(enabled)
    fun setAllowEmergencyUnlock(allow: Boolean) = repo.setAllowEmergencyUnlock(allow)

    fun refreshPermissions() {
        // Force a re-emission by triggering one of the combine inputs
        // Since we already read permissions inside combine, we just need
        // to nudge the flow. Easiest: toggle a no-op value and back.
        // Actually, permissions are read inside combine's lambda on each
        // emission of any input. We just re-read them here for immediate UI.
        _permissionRefreshTick.value += 1
        // No-op — the permissions are re-checked on every emission
    }

    fun openAccessibilitySettings() = FocusShieldPermissionHelper.openAccessibilitySettings(app)

    val blockedHitCount: StateFlow<Int> = repo.blockedHitCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockedHitsByPackage: StateFlow<Map<String, Int>> = repo.blockedHitsByPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun clearSessionStats() = repo.clearSessionStats()

    fun snapshotBlockedAttempts(): List<KavachBlockedAttempt> {
        val pm = app.packageManager
        return blockedHitsByPackage.value.entries
            .sortedByDescending { it.value }
            .map { (pkg, count) ->
                val label = runCatching {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                }.getOrDefault(pkg.substringAfterLast('.'))
                val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                KavachBlockedAttempt(appName = label, attemptCount = count, icon = icon)
            }
    }
}
