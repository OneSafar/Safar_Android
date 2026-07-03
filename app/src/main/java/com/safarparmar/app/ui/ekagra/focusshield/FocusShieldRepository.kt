package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.HomeRepository
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for Focus Shield settings and session runtime state.
 */
@Singleton
class FocusShieldRepository @Inject constructor(
    private val dataStore: SafarDataStore,
    private val homeRepository: HomeRepository,
    @ApplicationContext private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val TAG = "FocusShield"
    }

    private data class ShieldActivationSettings(
        val enabled: Boolean,
        val strict: Boolean,
        val allowEmergencyUnlock: Boolean,
        val packages: Set<String>,
        val unlockLimit: Int,
        val unlockSeconds: Int,
    )

    val isEnabled: StateFlow<Boolean> = dataStore.focusShieldEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _isAlwaysOn = MutableStateFlow(false)
    val isAlwaysOn: StateFlow<Boolean> = _isAlwaysOn.asStateFlow()

    val isStrictMode: StateFlow<Boolean> = dataStore.focusShieldStrictMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val allowEmergencyUnlock: StateFlow<Boolean> = dataStore.focusShieldEmergencyUnlock
        .stateIn(scope, SharingStarted.Eagerly, true)

    val emergencyUnlocksPerSession: StateFlow<Int> = dataStore.focusShieldEmergencyUnlocksPerSession
        .stateIn(scope, SharingStarted.Eagerly, 3)

    val emergencyUnlockSeconds: StateFlow<Int> = dataStore.focusShieldEmergencyUnlockSeconds
        .stateIn(scope, SharingStarted.Eagerly, 60)

    private val userName: StateFlow<String?> = dataStore.userName
        .stateIn(scope, SharingStarted.Eagerly, null)

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

    private val _alwaysOnActive = MutableStateFlow(false)

    init {
        val primarySettings = combine(
            dataStore.focusShieldEnabled,
            dataStore.focusShieldStrictMode,
            dataStore.focusShieldEmergencyUnlock,
            dataStore.focusShieldBlockedPackages,
        ) { enabled, strict, emergency, packages ->
            ShieldActivationSettings(
                enabled = enabled,
                strict = strict,
                allowEmergencyUnlock = emergency,
                packages = packages,
                unlockLimit = emergencyUnlocksPerSession.value,
                unlockSeconds = emergencyUnlockSeconds.value,
            )
        }

        scope.launch {
            dataStore.setFocusShieldAlwaysOn(false)
            combine(
                primarySettings,
                dataStore.focusShieldEmergencyUnlocksPerSession,
                dataStore.focusShieldEmergencyUnlockSeconds,
            ) { settings, unlockLimit, unlockSeconds ->
                settings.copy(unlockLimit = unlockLimit, unlockSeconds = unlockSeconds)
            }.collect { settings ->
                if (!settings.enabled) deactivateSession()
            }
        }
    }

    fun activateForSession() {
        val settings = currentSettings()
        if (!settings.enabled) {
            debugLog("activateForSession skipped: shield not enabled")
            return
        }

        if (settings.packages.isEmpty()) {
            debugLog("activateForSession skipped: no blocked packages")
            return
        }
        if (!hasRequiredPermissions()) {
            debugLog("activateForSession skipped: required permission missing")
            return
        }
        activateBlocking(settings, resetUnlocks = true)
        debugLog("activateForSession enabled for ${settings.packages.size} packages")
    }

    fun deactivateSession() {
        val totalHits = _blockedHitCount.value
        scope.launch { dataStore.setFocusShieldLastBlockCount(totalHits) }
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        ShieldPrefs.clear(appContext)
        Snapshot.active = false
        Snapshot.packages = emptySet()
        Snapshot.strict = false
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
        ShieldPrefs.applyEmergencyUnlock(
            ctx = appContext,
            graceUntilMs = graceUntilMs,
            unlocksUsed = newUsed,
            userName = userName.value,
        )
        _emergencyUnlockCount.value = newUsed
        debugLog("recordEmergencyUnlock granted ${seconds}s (used=$newUsed/$limit)")
        return (limit - newUsed).coerceAtLeast(0)
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEnabled(enabled)
            val settings = currentSettings().copy(enabled = enabled)
            if (!enabled) {
                deactivateSession()
            }
            if (enabled && blockedPackages.value.isNotEmpty()) {
                homeRepository.trackKavachEvent("enabled", blockedPackages.value.size)
            }
        }
    }

    fun setAlwaysOn(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldAlwaysOn(false)
            _isAlwaysOn.value = false
            _alwaysOnActive.value = false
        }
    }

    fun setStrictMode(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldStrictMode(enabled)
        }
    }

    fun setAllowEmergencyUnlock(allow: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEmergencyUnlock(allow)
        }
    }

    fun setBlockedPackages(packages: Set<String>) {
        scope.launch {
            dataStore.setFocusShieldBlockedPackages(packages)
            if (isEnabled.value && packages.isNotEmpty()) {
                homeRepository.trackKavachEvent("configured", packages.size)
            }
        }
    }

    fun syncAlwaysOnActivation(resetUnlocks: Boolean = false) {
        scope.launch { dataStore.setFocusShieldAlwaysOn(false) }
        _isAlwaysOn.value = false
        _alwaysOnActive.value = false
    }

    fun shouldPreserveAlwaysOnBlocking(): Boolean {
        return false
    }

    private fun syncAlwaysOnActivation(
        settings: ShieldActivationSettings,
        resetUnlocks: Boolean = false,
    ) {
        _isAlwaysOn.value = false
        _alwaysOnActive.value = false
    }

    private fun activateBlocking(
        settings: ShieldActivationSettings,
        resetUnlocks: Boolean,
    ) {
        _sessionBlockedPackages.value = settings.packages
        _sessionActive.value = true
        if (resetUnlocks) {
            _blockedHitCount.value = 0
            _blockedHitsByPackage.value = emptyMap()
            _emergencyUnlockCount.value = 0
        }
        ShieldPrefs.write(
            appContext,
            active = true,
            packages = settings.packages,
            strict = settings.strict,
            alwaysOn = false,
            unlockLimit = if (settings.allowEmergencyUnlock && !settings.strict) settings.unlockLimit else 0,
            unlockSeconds = settings.unlockSeconds,
            resetUnlocks = resetUnlocks,
        )
        Snapshot.active = true
        Snapshot.packages = settings.packages
        Snapshot.strict = settings.strict
    }

    private fun currentSettings(): ShieldActivationSettings =
        ShieldActivationSettings(
            enabled = isEnabled.value,
            strict = isStrictMode.value,
            allowEmergencyUnlock = allowEmergencyUnlock.value,
            packages = blockedPackages.value,
            unlockLimit = emergencyUnlocksPerSession.value,
            unlockSeconds = emergencyUnlockSeconds.value,
        )

    private fun hasRequiredPermissions(): Boolean =
        FocusShieldPermissionHelper.hasUsageStatsPermission(appContext) &&
            (
                !FocusShieldPermissionHelper.isAccessibilityFeatureEnabled() ||
                    FocusShieldPermissionHelper.hasAccessibilityService(appContext)
                )

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }

    object ShieldPrefs {
        private const val PREFS_NAME = "focus_shield_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_STRICT = "strict"
        private const val KEY_ALWAYS_ON = "always_on"
        private const val KEY_UNLOCK_LIMIT = "unlock_limit"
        private const val KEY_UNLOCK_SECONDS = "unlock_seconds"
        private const val KEY_UNLOCKS_USED = "unlocks_used"
        private const val KEY_GRACE_UNTIL_MS = "grace_until_ms"
        private const val KEY_ONE_TIME_UNLOCK_PACKAGE = "one_time_unlock_package"
        private const val KEY_RETURN_GRACE_UNTIL_ELAPSED = "return_grace_until_elapsed"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun write(
            ctx: Context,
            active: Boolean,
            packages: Set<String>,
            strict: Boolean,
            alwaysOn: Boolean = false,
            unlockLimit: Int = 0,
            unlockSeconds: Int = 60,
            resetUnlocks: Boolean = true,
        ) {
            prefs(ctx).edit().apply {
                putBoolean(KEY_ACTIVE, active)
                putStringSet(KEY_PACKAGES, packages)
                putBoolean(KEY_STRICT, strict)
                putBoolean(KEY_ALWAYS_ON, alwaysOn)
                putInt(KEY_UNLOCK_LIMIT, unlockLimit.coerceAtLeast(0))
                putInt(KEY_UNLOCK_SECONDS, unlockSeconds.coerceAtLeast(5))
                if (resetUnlocks) {
                    putInt(KEY_UNLOCKS_USED, 0)
                    putLong(KEY_GRACE_UNTIL_MS, 0L)
                    putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null)
                }
            }.apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    TAG,
                    "ShieldPrefs.write(active=$active, count=${packages.size}, strict=$strict, " +
                        "alwaysOn=$alwaysOn, unlockLimit=$unlockLimit, unlockSeconds=$unlockSeconds, reset=$resetUnlocks)",
                )
            }
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
                .putBoolean(KEY_ALWAYS_ON, false)
                .putInt(KEY_UNLOCKS_USED, 0)
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null)
                .apply()
            QuickUnlockNotification.cancel(ctx)
            if (BuildConfig.DEBUG) android.util.Log.d(TAG, "ShieldPrefs.clear()")
        }

        fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        fun getPackages(ctx: Context): Set<String> =
            prefs(ctx).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()

        fun isStrict(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_STRICT, false)
        fun isAlwaysOn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ALWAYS_ON, false)
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

        fun applyEmergencyUnlock(ctx: Context, graceUntilMs: Long, unlocksUsed: Int, userName: String? = null) {
            prefs(ctx).edit()
                .putLong(KEY_GRACE_UNTIL_MS, graceUntilMs)
                .putInt(KEY_UNLOCKS_USED, unlocksUsed)
                .apply()
            QuickUnlockNotification.show(ctx, graceUntilMs, userName)
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyEmergencyUnlock(graceUntilMs=$graceUntilMs, unlocksUsed=$unlocksUsed)")
            }
        }

        fun applyOneTimeUnlock(ctx: Context, packageName: String, unlocksUsed: Int) {
            prefs(ctx).edit()
                .putString(KEY_ONE_TIME_UNLOCK_PACKAGE, packageName)
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .putInt(KEY_UNLOCKS_USED, unlocksUsed)
                .apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyOneTimeUnlock(packageName=$packageName, unlocksUsed=$unlocksUsed)")
            }
        }

        fun isOneTimeUnlockedPackage(ctx: Context, packageName: String): Boolean =
            packageName.isNotBlank() &&
                prefs(ctx).getString(KEY_ONE_TIME_UNLOCK_PACKAGE, null) == packageName

        fun clearOneTimeUnlock(ctx: Context) {
            prefs(ctx).edit().putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null).apply()
        }

        /** Suppress re-blocking briefly after the user taps the return button on the block screen. */
        fun beginReturnToFocusGrace(ctx: Context, durationMs: Long) {
            prefs(ctx).edit()
                .putLong(KEY_RETURN_GRACE_UNTIL_ELAPSED, SystemClock.elapsedRealtime() + durationMs)
                .apply()
        }

        fun isInReturnToFocusGrace(ctx: Context): Boolean =
            SystemClock.elapsedRealtime() < prefs(ctx).getLong(KEY_RETURN_GRACE_UNTIL_ELAPSED, 0L)

        fun clearReturnToFocusGrace(ctx: Context) {
            prefs(ctx).edit().putLong(KEY_RETURN_GRACE_UNTIL_ELAPSED, 0L).apply()
        }
    }

    object Snapshot {
        @Volatile var active: Boolean = false
        @Volatile var packages: Set<String> = emptySet()
        @Volatile var strict: Boolean = false
    }
}
