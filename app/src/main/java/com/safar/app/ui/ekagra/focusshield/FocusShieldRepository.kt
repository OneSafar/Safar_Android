package com.safar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import com.safar.app.BuildConfig
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for Focus Shield settings and session runtime state.
 */
@Singleton
class FocusShieldRepository @Inject constructor(
    private val dataStore: SafarDataStore,
    @ApplicationContext private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val TAG = "FocusShield"
    }

    val isEnabled: StateFlow<Boolean> = dataStore.focusShieldEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isStrictMode: StateFlow<Boolean> = dataStore.focusShieldStrictMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val allowEmergencyUnlock: StateFlow<Boolean> = dataStore.focusShieldEmergencyUnlock
        .stateIn(scope, SharingStarted.Eagerly, true)

    val blockedPackages: StateFlow<Set<String>> = dataStore.focusShieldBlockedPackages
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _sessionBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val sessionBlockedPackages: StateFlow<Set<String>> = _sessionBlockedPackages.asStateFlow()

    private val _blockedHitCount = MutableStateFlow(0)
    val blockedHitCount: StateFlow<Int> = _blockedHitCount.asStateFlow()

    private val _emergencyUnlockCount = MutableStateFlow(0)
    val emergencyUnlockCount: StateFlow<Int> = _emergencyUnlockCount.asStateFlow()

    fun activateForSession() {
        if (!isEnabled.value) {
            debugLog("activateForSession skipped: shield not enabled")
            return
        }

        val packages = blockedPackages.value
        if (packages.isEmpty()) {
            debugLog("activateForSession skipped: no blocked packages")
            return
        }
        if (!FocusShieldPermissionHelper.hasUsageStatsPermission(appContext)) {
            debugLog("activateForSession skipped: usage access missing")
            return
        }
        if (!FocusShieldPermissionHelper.hasAccessibilityService(appContext)) {
            debugLog("activateForSession skipped: accessibility service missing")
            return
        }

        _sessionBlockedPackages.value = packages
        _sessionActive.value = true
        _blockedHitCount.value = 0
        _emergencyUnlockCount.value = 0
        ShieldPrefs.write(appContext, active = true, packages = packages, strict = isStrictMode.value)
        debugLog("activateForSession enabled for ${packages.size} packages")
    }

    fun deactivateSession() {
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        ShieldPrefs.clear(appContext)
        debugLog("deactivateSession cleared")
    }

    fun recordBlockedHit() {
        _blockedHitCount.value++
    }

    fun recordEmergencyUnlock() {
        _emergencyUnlockCount.value++
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch { dataStore.setFocusShieldEnabled(enabled) }
    }

    fun setStrictMode(enabled: Boolean) {
        scope.launch { dataStore.setFocusShieldStrictMode(enabled) }
    }

    fun setAllowEmergencyUnlock(allow: Boolean) {
        scope.launch { dataStore.setFocusShieldEmergencyUnlock(allow) }
    }

    fun setBlockedPackages(packages: Set<String>) {
        scope.launch { dataStore.setFocusShieldBlockedPackages(packages) }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }

    object ShieldPrefs {
        private const val PREFS_NAME = "focus_shield_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_STRICT = "strict"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun write(ctx: Context, active: Boolean, packages: Set<String>, strict: Boolean) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, active)
                .putStringSet(KEY_PACKAGES, packages)
                .putBoolean(KEY_STRICT, strict)
                .apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.write(active=$active, count=${packages.size}, strict=$strict)")
            }
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
                .apply()
            if (BuildConfig.DEBUG) android.util.Log.d(TAG, "ShieldPrefs.clear()")
        }

        fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        fun getPackages(ctx: Context): Set<String> =
            prefs(ctx).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()

        fun isStrict(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_STRICT, false)
    }

    object Snapshot {
        @Volatile var active: Boolean = false
        @Volatile var packages: Set<String> = emptySet()
        @Volatile var strict: Boolean = false
    }
}
