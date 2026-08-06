package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
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
import kotlinx.coroutines.flow.first
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
    private val analyticsRecorder: KavachAnalyticsRecorder,
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

    /**
     * All-day blocking, independent of the Ekagra timer. While this is on, the
     * chosen apps stay blocked and [KavachAlwaysOnService] keeps an ongoing
     * notification up so the student always knows blocking is running.
     */
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

    /**
     * @param isFocusPeriod false while a break is running. Beast Mode keeps blocking
     *   through breaks, but a break is not a Kavach session of its own — opening one
     *   there would split a single study session into several, and the break's own
     *   "session" would then be reported as ended early when the timer is reset.
     */
    fun activateForSession(plannedSeconds: Int = 0, isFocusPeriod: Boolean = true) {
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
        // Always On owns blocking outright, and must keep owning it for the whole
        // session. Handing over to timer-bound blocking here is what downgraded the
        // student to Normal Mode mid-session: they got the bottom sheet with a quick
        // unlock button, in the one mode whose entire point is that there is no way
        // out. The timed session is still recorded, so study time is still measured —
        // only the blocking stays where the student put it.
        if (isAlwaysOnMode.value) {
            debugLog("activateForSession deferred: Always On owns blocking")
            _activationBlockedReason.value = null
            if (isFocusPeriod) {
                analyticsRecorder.sessionStarted(strictMode = settings.strict, plannedSeconds = plannedSeconds)
            }
            return
        }

        if (!hasRequiredPermissions()) {
            debugLog("activateForSession skipped: required permission missing")
            _activationBlockedReason.value = "A permission KAVACH needs was turned off, so blocking isn't active this session."
            // Keep the flag on the summary rather than quietly reporting a clean
            // session in which nothing was ever actually blocked.
            analyticsRecorder.permissionLost()
            return
        }
        activateBlocking(settings, resetUnlocks = true)
        QuickUnlockNotification.cancel(appContext)
        _activationBlockedReason.value = null
        // Idempotent: a Normal-Mode break deactivates and re-activates blocking, but
        // that is still one Kavach session from the student's point of view.
        if (isFocusPeriod) {
            analyticsRecorder.sessionStarted(strictMode = settings.strict, plannedSeconds = plannedSeconds)
        }
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
        // Called once per foreground visit by TimerService's debounce, which is
        // exactly the counting rule Kavach analytics reports.
        analyticsRecorder.blockedAttempt(packageName)
    }

    /**
     * Records a quick unlock for analytics. The selected window is a ceiling — the
     * recorder closes the unlock with the duration actually elapsed, whether that is
     * the window expiring or the Kavach session ending first.
     */
    fun recordQuickUnlock(packageName: String, selectedMinutes: Int) {
        analyticsRecorder.quickUnlockStarted(
            packageName = packageName,
            selectedSeconds = selectedMinutes.coerceAtLeast(0) * 60,
        )
    }

    /** Settles any quick unlock whose window has expired. Cheap; safe to call often. */
    fun settleExpiredQuickUnlocks() = analyticsRecorder.settleExpiredUnlocks()

    fun clearSessionStats() {
        _blockedHitCount.value = 0
        _blockedHitsByPackage.value = emptyMap()
    }

    fun setKavachProfile(mode: String) {
        scope.launch {
            dataStore.setAppUsageMode(mode)
            when (mode) {
                com.safarparmar.app.ui.launch.AppUsageMode.ALWAYS_ON -> {
                    dataStore.setFocusShieldEnabled(true)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(true)
                    startAlwaysOnService()
                }
                com.safarparmar.app.ui.launch.AppUsageMode.BEAST -> {
                    dataStore.setFocusShieldEnabled(true)
                    dataStore.setFocusShieldStrictMode(true)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    KavachAlwaysOnService.stop(appContext)
                }
                com.safarparmar.app.ui.launch.AppUsageMode.FOCUSED,
                com.safarparmar.app.ui.launch.AppUsageMode.STANDARD -> {
                    dataStore.setFocusShieldEnabled(true)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    KavachAlwaysOnService.stop(appContext)
                }
                else -> {
                    dataStore.setFocusShieldEnabled(false)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    KavachAlwaysOnService.stop(appContext)
                    deactivateSession()
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEnabled(enabled)
            if (!enabled) {
                // Turning Kavach off has to take Always On with it, or blocking would
                // carry on from a screen that says it is switched off.
                dataStore.setFocusShieldAlwaysOnMode(false)
                dataStore.setFocusShieldStrictMode(false)
                NotificationShieldPrefs.clear(appContext)
                KavachAlwaysOnService.stop(appContext)
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

    /**
     * Turns all-day blocking on or off.
     *
     * Enabling implies Kavach itself is on — a student who asks for Always On has
     * unambiguously asked for blocking.
     */
    fun setAlwaysOnMode(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldAlwaysOnMode(enabled)
            if (enabled) {
                dataStore.setFocusShieldEnabled(true)
                startAlwaysOnService()
            } else {
                KavachAlwaysOnService.stop(appContext)
            }
        }
    }

    private fun startAlwaysOnService(packages: Set<String> = blockedPackages.value) {
        if (packages.isEmpty()) {
            debugLog("Always On not started: no blocked apps chosen")
            return
        }
        // Drop any quick-unlock window still running from a previous Normal-Mode
        // session. Carrying one into Always On would leave an app openable for
        // minutes after the student switched to the mode that exists to prevent it.
        ShieldPrefs.applyEmergencyUnlock(appContext, graceUntilMs = 0L)
        QuickUnlockNotification.cancel(appContext)
        if (!hasRequiredPermissions()) {
            _activationBlockedReason.value =
                "A permission KAVACH needs was turned off, so Always On isn't blocking."
            return
        }
        KavachAlwaysOnService.start(appContext)
    }

    /** Restarts Always On after a reboot or app update, if the student left it on. */
    fun restoreAlwaysOnIfEnabled() {
        scope.launch {
            if (dataStore.focusShieldAlwaysOnMode.first() &&
                dataStore.focusShieldEnabled.first()
            ) {
                startAlwaysOnService(dataStore.focusShieldBlockedPackages.first())
            }
        }
    }

    fun setBlockedPackages(packages: Set<String>) {
        scope.launch {
            dataStore.setFocusShieldBlockedPackages(packages)
            // A newly chosen app must start being blocked without waiting for the
            // next time the student opens Ekagra.
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

/**
 * Notification Shield has a longer lifetime than app blocking in Normal Mode: it remains
 * active through timer pauses and breaks until that timer session is completed or ended.
 */
internal object NotificationShieldPrefs {
    private const val PREFS_NAME = "kavach_notification_shield"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PACKAGES = "packages"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun write(context: Context, packages: Set<String>) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putStringSet(KEY_PACKAGES, packages)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .putStringSet(KEY_PACKAGES, emptySet())
            .apply()
    }

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)
    fun packages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
}
