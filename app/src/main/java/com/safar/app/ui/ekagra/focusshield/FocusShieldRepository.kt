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

    val emergencyUnlocksPerSession: StateFlow<Int> = dataStore.focusShieldEmergencyUnlocksPerSession
        .stateIn(scope, SharingStarted.Eagerly, 3)

    val emergencyUnlockSeconds: StateFlow<Int> = dataStore.focusShieldEmergencyUnlockSeconds
        .stateIn(scope, SharingStarted.Eagerly, 60)

    val blockedPackages: StateFlow<Set<String>> = dataStore.focusShieldBlockedPackages
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _sessionBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val sessionBlockedPackages: StateFlow<Set<String>> = _sessionBlockedPackages.asStateFlow()

    private val _blockedHitCount = MutableStateFlow(0)
    val blockedHitCount: StateFlow<Int> = _blockedHitCount.asStateFlow()

    private val _blockedHitsByPackage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val blockedHitsByPackage: StateFlow<Map<String, Int>> = _blockedHitsByPackage.asStateFlow()

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
        _blockedHitsByPackage.value = emptyMap()
        _emergencyUnlockCount.value = 0
        ShieldPrefs.write(
            appContext,
            active = true,
            packages = packages,
            strict = isStrictMode.value,
            unlockLimit = if (allowEmergencyUnlock.value) emergencyUnlocksPerSession.value else 0,
            unlockSeconds = emergencyUnlockSeconds.value,
            resetUnlocks = true,
        )
        debugLog("activateForSession enabled for ${packages.size} packages")
    }

    fun deactivateSession() {
        val totalHits = _blockedHitCount.value
        scope.launch { dataStore.setFocusShieldLastBlockCount(totalHits) }
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        ShieldPrefs.clear(appContext)
        debugLog("deactivateSession cleared (blocks=$totalHits)")
    }

    fun recordBlockedHit(packageName: String) {
        if (packageName.isBlank()) return
        _blockedHitCount.value++
        _blockedHitsByPackage.value = _blockedHitsByPackage.value.toMutableMap().apply {
            this[packageName] = (this[packageName] ?: 0) + 1
        }
    }

    fun clearSessionStats() {
        _blockedHitCount.value = 0
        _blockedHitsByPackage.value = emptyMap()
        _emergencyUnlockCount.value = 0
    }

    /**
     * Records a user-triggered emergency unlock: bumps the session counter and writes a grace
     * window into ShieldPrefs so the accessibility service suppresses blocking until it expires.
     * Returns the new remaining unlock count (>= 0), or null when no grace was issued
     * (Beast Mode active, emergency unlock disabled, or the per-session quota is exhausted).
     */
    fun recordEmergencyUnlock(graceSeconds: Int = emergencyUnlockSeconds.value): Int? {
        if (isStrictMode.value) {
            debugLog("recordEmergencyUnlock blocked: Beast Mode active")
            return null
        }
        if (!allowEmergencyUnlock.value) {
            debugLog("recordEmergencyUnlock blocked: emergency unlock disabled")
            return null
        }
        val limit = emergencyUnlocksPerSession.value
        val used = ShieldPrefs.getUnlocksUsed(appContext)
        if (used >= limit) {
            debugLog("recordEmergencyUnlock blocked: limit reached ($used/$limit)")
            return 0
        }
        val seconds = graceSeconds.coerceAtLeast(5)
        val graceUntilMs = System.currentTimeMillis() + seconds * 1000L
        val newUsed = used + 1
        ShieldPrefs.applyEmergencyUnlock(appContext, graceUntilMs = graceUntilMs, unlocksUsed = newUsed)
        _emergencyUnlockCount.value = newUsed
        debugLog("recordEmergencyUnlock granted ${seconds}s (used=$newUsed/$limit)")
        return (limit - newUsed).coerceAtLeast(0)
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
        private const val KEY_UNLOCK_LIMIT = "unlock_limit"
        private const val KEY_UNLOCK_SECONDS = "unlock_seconds"
        private const val KEY_UNLOCKS_USED = "unlocks_used"
        private const val KEY_GRACE_UNTIL_MS = "grace_until_ms"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun write(
            ctx: Context,
            active: Boolean,
            packages: Set<String>,
            strict: Boolean,
            unlockLimit: Int = 0,
            unlockSeconds: Int = 60,
            resetUnlocks: Boolean = true,
        ) {
            prefs(ctx).edit().apply {
                putBoolean(KEY_ACTIVE, active)
                putStringSet(KEY_PACKAGES, packages)
                putBoolean(KEY_STRICT, strict)
                putInt(KEY_UNLOCK_LIMIT, unlockLimit.coerceAtLeast(0))
                putInt(KEY_UNLOCK_SECONDS, unlockSeconds.coerceAtLeast(5))
                if (resetUnlocks) {
                    putInt(KEY_UNLOCKS_USED, 0)
                    putLong(KEY_GRACE_UNTIL_MS, 0L)
                }
            }.apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    TAG,
                    "ShieldPrefs.write(active=$active, count=${packages.size}, strict=$strict, " +
                        "unlockLimit=$unlockLimit, unlockSeconds=$unlockSeconds, reset=$resetUnlocks)",
                )
            }
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
                .putInt(KEY_UNLOCKS_USED, 0)
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .apply()
            if (BuildConfig.DEBUG) android.util.Log.d(TAG, "ShieldPrefs.clear()")
        }

        fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        fun getPackages(ctx: Context): Set<String> =
            prefs(ctx).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()

        fun isStrict(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_STRICT, false)
        fun getUnlockLimit(ctx: Context): Int = prefs(ctx).getInt(KEY_UNLOCK_LIMIT, 0)
        fun getUnlockSeconds(ctx: Context): Int = prefs(ctx).getInt(KEY_UNLOCK_SECONDS, 60)
        fun getUnlocksUsed(ctx: Context): Int = prefs(ctx).getInt(KEY_UNLOCKS_USED, 0)
        fun getUnlocksRemaining(ctx: Context): Int {
            val limit = getUnlockLimit(ctx)
            val used = getUnlocksUsed(ctx)
            return (limit - used).coerceAtLeast(0)
        }
        fun getGraceUntilMs(ctx: Context): Long = prefs(ctx).getLong(KEY_GRACE_UNTIL_MS, 0L)
        fun isInGracePeriod(ctx: Context): Boolean = System.currentTimeMillis() < getGraceUntilMs(ctx)

        fun applyEmergencyUnlock(ctx: Context, graceUntilMs: Long, unlocksUsed: Int) {
            prefs(ctx).edit()
                .putLong(KEY_GRACE_UNTIL_MS, graceUntilMs)
                .putInt(KEY_UNLOCKS_USED, unlocksUsed)
                .apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyEmergencyUnlock(graceUntilMs=$graceUntilMs, unlocksUsed=$unlocksUsed)")
            }
        }
    }

    object Snapshot {
        @Volatile var active: Boolean = false
        @Volatile var packages: Set<String> = emptySet()
        @Volatile var strict: Boolean = false
    }
}
