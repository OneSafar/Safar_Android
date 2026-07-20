package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
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

        /** Shared intent-extra key for the blocked package name, used by
         * [com.safarparmar.app.ui.ekagra.TimerService]. */
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    private data class ShieldActivationSettings(
        val enabled: Boolean,
        val strict: Boolean,
        val packages: Set<String>,
    )

    val isEnabled: StateFlow<Boolean> = dataStore.focusShieldEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isStrictMode: StateFlow<Boolean> = dataStore.focusShieldStrictMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isAlwaysOnMode: StateFlow<Boolean> = dataStore.focusShieldAlwaysOnMode
        .stateIn(scope, SharingStarted.Eagerly, false)

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

    /**
     * Non-null when the most recent [activateForSession] call could not actually enable
     * blocking (e.g. a required permission was revoked mid-session). Callers should surface
     * this to the user instead of silently showing "KAVACH is active" while nothing is blocked.
     */
    private val _activationBlockedReason = MutableStateFlow<String?>(null)
    val activationBlockedReason: StateFlow<String?> = _activationBlockedReason.asStateFlow()

    init {
        scope.launch {
            combine(
                dataStore.focusShieldEnabled,
                dataStore.focusShieldStrictMode,
                dataStore.focusShieldBlockedPackages,
            ) { enabled, strict, packages ->
                ShieldActivationSettings(enabled = enabled, strict = strict, packages = packages)
            }.collect { settings ->
                if (!settings.enabled) deactivateSession()
            }
        }
    }

    fun activateForSession() {
        val settings = currentSettings()
        if (!settings.enabled) {
            debugLog("activateForSession skipped: shield not enabled")
            _activationBlockedReason.value = null
            return
        }

        if (settings.packages.isEmpty()) {
            debugLog("activateForSession skipped: no blocked packages")
            _activationBlockedReason.value = null
            return
        }
        if (!hasRequiredPermissions()) {
            debugLog("activateForSession skipped: required permission missing")
            _activationBlockedReason.value = "A permission KAVACH needs was turned off, so blocking isn't active this session."
            return
        }
        activateBlocking(settings, resetUnlocks = true)
        _activationBlockedReason.value = null
        debugLog("activateForSession enabled for ${settings.packages.size} packages")
    }

    fun deactivateSession() {
        val totalHits = _blockedHitCount.value
        scope.launch { dataStore.setFocusShieldLastBlockCount(totalHits) }
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        _activationBlockedReason.value = null
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
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEnabled(enabled)
            if (!enabled) {
                dataStore.setFocusShieldAlwaysOnMode(false)
                KavachAlwaysOnPrefs.clear(appContext)
                appContext.stopService(Intent(appContext, KavachAlwaysOnService::class.java))
            }
            val settings = currentSettings().copy(enabled = enabled)
            if (!enabled) {
                deactivateSession()
            }
            if (enabled && blockedPackages.value.isNotEmpty()) {
                homeRepository.trackKavachEvent("enabled", blockedPackages.value.size)
            }
        }
    }

    fun setStrictMode(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldStrictMode(enabled)
        }
    }

    fun setAlwaysOnMode(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldAlwaysOnMode(enabled)
            if (enabled) {
                dataStore.setFocusShieldEnabled(true)
                startAlwaysOnService()
            } else {
                KavachAlwaysOnPrefs.clear(appContext)
                appContext.stopService(Intent(appContext, KavachAlwaysOnService::class.java))
            }
        }
    }

    private fun startAlwaysOnService(packages: Set<String> = blockedPackages.value) {
        if (!hasRequiredPermissions() || packages.isEmpty()) return
        runCatching {
            KavachAlwaysOnService.start(appContext)
        }.onFailure { debugLog("Unable to start Always On service: ${it.javaClass.simpleName}") }
    }

    fun setBlockedPackages(packages: Set<String>) {
        scope.launch {
            dataStore.setFocusShieldBlockedPackages(packages)
            if (isAlwaysOnMode.value && packages.isNotEmpty()) {
                startAlwaysOnService(packages)
            }
            if (isEnabled.value && packages.isNotEmpty()) {
                homeRepository.trackKavachEvent("configured", packages.size)
            }
        }
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
        }
        ShieldPrefs.write(
            appContext,
            active = true,
            packages = settings.packages,
            strict = settings.strict,
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
            packages = blockedPackages.value,
        )

    // KAVACH needs Usage access (to see the foreground app) and Display-over-other-apps
    // (to show the block screen). Notification-listener access is optional (notification
    // suppression only) and must NOT gate activation.
    private fun hasRequiredPermissions(): Boolean =
        FocusShieldPermissionHelper.hasUsageStatsPermission(appContext) &&
            FocusShieldPermissionHelper.hasOverlayPermission(appContext)

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }

    object ShieldPrefs {
        private const val PREFS_NAME = "focus_shield_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_STRICT = "strict"
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
            resetUnlocks: Boolean = true,
        ) {
            prefs(ctx).edit().apply {
                putBoolean(KEY_ACTIVE, active)
                putStringSet(KEY_PACKAGES, packages)
                putBoolean(KEY_STRICT, strict)
                if (resetUnlocks) {
                    putLong(KEY_GRACE_UNTIL_MS, 0L)
                    putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null)
                }
            }.apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    TAG,
                    "ShieldPrefs.write(active=$active, count=${packages.size}, strict=$strict, reset=$resetUnlocks)",
                )
            }
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
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
        fun getGraceUntilMs(ctx: Context): Long = prefs(ctx).getLong(KEY_GRACE_UNTIL_MS, 0L)
        fun isInGracePeriod(ctx: Context): Boolean = System.currentTimeMillis() < getGraceUntilMs(ctx)

        /** Grants a quick-unlock grace window (flat duration, no per-session quota). */
        fun applyEmergencyUnlock(ctx: Context, graceUntilMs: Long, userName: String? = null) {
            prefs(ctx).edit()
                .putLong(KEY_GRACE_UNTIL_MS, graceUntilMs)
                .apply()
            QuickUnlockNotification.show(ctx, graceUntilMs, userName)
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyEmergencyUnlock(graceUntilMs=$graceUntilMs)")
            }
        }

        fun applyOneTimeUnlock(ctx: Context, packageName: String) {
            prefs(ctx).edit()
                .putString(KEY_ONE_TIME_UNLOCK_PACKAGE, packageName)
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyOneTimeUnlock(packageName=$packageName)")
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
