package com.safar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for Focus Shield runtime state.
 *
 * Combines persisted preferences from [SafarDataStore] with transient
 * session-scoped state (whether the blocker is currently active).
 *
 * The AccessibilityService / OverlayService reads from [ShieldPrefs]
 * (backed by SharedPreferences) so that session state survives process death.
 */
@Singleton
class FocusShieldRepository @Inject constructor(
    private val dataStore: SafarDataStore,
    @ApplicationContext private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val TAG = "FocusShieldA11y"
    }

    // ── Persisted settings (from DataStore) ──────────────────────────────────

    val isEnabled: StateFlow<Boolean> = dataStore.focusShieldEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isStrictMode: StateFlow<Boolean> = dataStore.focusShieldStrictMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val allowEmergencyUnlock: StateFlow<Boolean> = dataStore.focusShieldEmergencyUnlock
        .stateIn(scope, SharingStarted.Eagerly, true)

    val blockedPackages: StateFlow<Set<String>> = dataStore.focusShieldBlockedPackages
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    // ── Transient session state ──────────────────────────────────────────────

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _sessionBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val sessionBlockedPackages: StateFlow<Set<String>> = _sessionBlockedPackages.asStateFlow()

    // ── Analytics counters (in-memory, flushed on session end) ────────────────

    private val _blockedHitCount = MutableStateFlow(0)
    val blockedHitCount: StateFlow<Int> = _blockedHitCount.asStateFlow()

    private val _emergencyUnlockCount = MutableStateFlow(0)
    val emergencyUnlockCount: StateFlow<Int> = _emergencyUnlockCount.asStateFlow()

    // ── Session lifecycle ────────────────────────────────────────────────────

    fun activateForSession() {
        if (!isEnabled.value) {
            Log.w(TAG, "activateForSession() → SKIP: shield not enabled")
            return
        }
        val packages = blockedPackages.value
        if (packages.isEmpty()) {
            Log.w(TAG, "activateForSession() → SKIP: no blocked packages")
            return
        }
        _sessionBlockedPackages.value = packages
        _sessionActive.value = true
        _blockedHitCount.value = 0
        _emergencyUnlockCount.value = 0
        ShieldPrefs.write(appContext, true, packages, isStrictMode.value)
        Log.w(TAG, "activateForSession() → DONE: ${packages.size} packages blocked")
    }

    fun deactivateSession() {
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        ShieldPrefs.clear(appContext)
        Log.w(TAG, "deactivateSession() → cleared")
    }

    fun recordBlockedHit() {
        _blockedHitCount.value++
    }

    fun recordEmergencyUnlock() {
        _emergencyUnlockCount.value++
    }

    // ── Persistence helpers ──────────────────────────────────────────────────

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

    // ══════════════════════════════════════════════════════════════════════════
    // Static SharedPreferences-backed state readable from ANY component
    // (AccessibilityService, OverlayService) without DI. Survives process death.
    // ══════════════════════════════════════════════════════════════════════════

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
            Log.w(TAG, "ShieldPrefs.write(active=$active, pkgs=${packages.size}, strict=$strict)")
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
                .apply()
            Log.w(TAG, "ShieldPrefs.clear()")
        }

        fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        fun getPackages(ctx: Context): Set<String> = prefs(ctx).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
        fun isStrict(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_STRICT, false)
    }

    // Keep the old Snapshot for backward compat (volatile fields)
    // These are updated alongside ShieldPrefs for in-process fast access
    object Snapshot {
        @Volatile var active: Boolean = false
        @Volatile var packages: Set<String> = emptySet()
        @Volatile var strict: Boolean = false
    }
}
