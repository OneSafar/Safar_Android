package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
import com.safarparmar.app.ui.ekagra.TimerService
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
import kotlinx.coroutines.delay
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

    /**
     * All-day blocking, independent of the Ekagra timer. While this is on, the
     * chosen apps stay blocked and [KavachAlwaysOnService] keeps an ongoing
     * notification up so the student always knows blocking is running.
     */
    val isAlwaysOnMode: StateFlow<Boolean> = dataStore.focusShieldAlwaysOnMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isStrictMode: StateFlow<Boolean> = dataStore.focusShieldStrictMode
        .stateIn(scope, SharingStarted.Eagerly, false)

    val appUsageMode: StateFlow<String?> = dataStore.appUsageMode
        .stateIn(scope, SharingStarted.Eagerly, null)

    val blockedPackages: StateFlow<Set<String>> = dataStore.focusShieldBlockedPackages
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val scheduleEnabled: StateFlow<Boolean> = dataStore.focusShieldScheduleEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    val scheduleStartMinute: StateFlow<Int> = dataStore.focusShieldScheduleStartMinute
        .stateIn(scope, SharingStarted.Eagerly, 540) // 09:00 AM

    val scheduleEndMinute: StateFlow<Int> = dataStore.focusShieldScheduleEndMinute
        .stateIn(scope, SharingStarted.Eagerly, 1320) // 10:00 PM

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

    private val _protectionActive = MutableStateFlow(false)
    val protectionActive: StateFlow<Boolean> = _protectionActive.asStateFlow()

    private val _protectionStarting = MutableStateFlow(false)
    val protectionStarting: StateFlow<Boolean> = _protectionStarting.asStateFlow()
    private var protectionStartAttempt = 0L

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
        // Always On owns protection independently; starting Ekagra must not create a
        // second timer-linked analytics source underneath it.
        if (isAlwaysOnMode.value) return
        val settings = currentSettings()
        if (!settings.enabled) {
            return
        }
        activationWarning(settings.packages)?.let { warning ->
            reportProtectionFailure(warning)
            if (settings.packages.isNotEmpty() && !hasRequiredPermissions()) {
                analyticsRecorder.permissionLost()
            }
            return
        }
        
        _activationBlockedReason.value = null
        _protectionStarting.value = true
        _protectionActive.value = false
        activateBlocking(settings, resetUnlocks = isFocusPeriod && !_sessionActive.value)
        if (!startKavachService(settings.packages)) {
            _sessionActive.value = false
            _sessionBlockedPackages.value = emptySet()
            ShieldPrefs.clear(appContext)
            Snapshot.active = false
            Snapshot.packages = emptySet()
            return
        }

        if (isFocusPeriod) {
            analyticsRecorder.sessionStarted(plannedSeconds = plannedSeconds)
        }
    }

    fun deactivateSession() {
        val totalHits = _blockedHitCount.value
        scope.launch { dataStore.setFocusShieldLastBlockCount(totalHits) }
        _sessionActive.value = false
        _sessionBlockedPackages.value = emptySet()
        _protectionActive.value = false
        _protectionStarting.value = false
        _activationBlockedReason.value = null
        ShieldPrefs.clear(appContext)
        Snapshot.active = false
        Snapshot.packages = emptySet()
        Snapshot.strict = false
        scope.launch {
            if (!dataStore.focusShieldAlwaysOnMode.first()) {
                KavachAlwaysOnService.stop(appContext)
            }
        }
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
        analyticsRecorder.blockedAttempt(
            packageName = packageName,
            attachToEkagraSession = !isAlwaysOnMode.value,
        )
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
            attachToEkagraSession = !isAlwaysOnMode.value,
        )
        // Quick Unlock time is intentionally not study time. Pause only a timer-linked
        // Kavach session; Always On protection must remain independent of Ekagra.
        if (!isAlwaysOnMode.value && _sessionActive.value) {
            runCatching {
                appContext.startService(
                    Intent(appContext, TimerService::class.java).setAction(TimerService.ACTION_PAUSE),
                )
            }.onFailure {
                debugLog("Could not pause Ekagra for Quick Unlock: ${it.javaClass.simpleName}")
            }
        }
    }

    /** Settles any quick unlock whose window has expired. Cheap; safe to call often. */
    fun settleExpiredQuickUnlocks() = analyticsRecorder.settleExpiredUnlocks()

    /** Returning to a timer-linked focus session immediately restores protection. */
    fun endQuickUnlockForEkagraResume() {
        if (isAlwaysOnMode.value || !ShieldPrefs.isInGracePeriod(appContext)) return
        ShieldPrefs.clearQuickUnlock(appContext)
        analyticsRecorder.quickUnlockEnded()
    }

    fun clearSessionStats() {
        _blockedHitCount.value = 0
        _blockedHitsByPackage.value = emptyMap()
    }

    fun setKavachProfile(mode: String) {
        scope.launch {
            dataStore.setAppUsageMode(mode)
            dataStore.setFocusShieldScheduleEnabled(false)
            val currentlyEnabled = isEnabled.value
            when (mode) {
                com.safarparmar.app.ui.launch.AppUsageMode.ALWAYS_ON -> {
                    dataStore.setFocusShieldAlwaysOnMode(true)
                    if (currentlyEnabled && hasRequiredPermissions()) {
                        startKavachService()
                    }
                }
                com.safarparmar.app.ui.launch.AppUsageMode.FOCUSED,
                com.safarparmar.app.ui.launch.AppUsageMode.STANDARD -> {
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    if (!_sessionActive.value) KavachAlwaysOnService.stop(appContext)
                }
                else -> {
                    dataStore.setFocusShieldEnabled(false)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    ShieldPrefs.clearQuickUnlock(appContext)
                    KavachAlwaysOnService.stop(appContext)
                    deactivateSession()
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            val warning = if (enabled) activationWarning(blockedPackages.value) else null
            if (warning != null) {
                reportProtectionFailure(warning)
                return@launch
            }
            dataStore.setFocusShieldEnabled(enabled)
            if (!enabled) {
                // Turning Kavach off takes Beast Mode and active Quick Unlock with it
                dataStore.setFocusShieldAlwaysOnMode(false)
                ShieldPrefs.clearQuickUnlock(appContext)
                NotificationShieldPrefs.clear(appContext)
                KavachAlwaysOnService.stop(appContext)
                deactivateSession()
            } else {
                val mode = dataStore.appUsageMode.first()
                if (mode == com.safarparmar.app.ui.launch.AppUsageMode.ALWAYS_ON) {
                    dataStore.setFocusShieldAlwaysOnMode(true)
                }
                // Normal mode is now ready; actual protection starts with Ekagra.
                if (dataStore.focusShieldAlwaysOnMode.first()) startKavachService()
                if (blockedPackages.value.isNotEmpty()) {
                    homeRepository.trackKavachEvent("enabled", blockedPackages.value.size)
                }
            }
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
                startKavachService()
            } else {
                KavachAlwaysOnService.stop(appContext)
            }
        }
    }

    fun setStrictMode(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldStrictMode(enabled)
            if (enabled) ShieldPrefs.clearQuickUnlock(appContext)
            if (isAlwaysOnMode.value || _sessionActive.value) startKavachService()
        }
    }

    private fun startKavachService(packages: Set<String> = blockedPackages.value): Boolean {
        activationWarning(packages)?.let { warning ->
            reportProtectionFailure(warning)
            return false
        }
        _protectionStarting.value = true
        _protectionActive.value = false
        val attempt = ++protectionStartAttempt
        if (!KavachAlwaysOnService.start(appContext)) {
            reportProtectionFailure("Kavach could not start. Please try again.")
            return false
        }
        scope.launch {
            delay(5_000L)
            if (attempt == protectionStartAttempt && _protectionStarting.value && !_protectionActive.value) {
                reportProtectionFailure(
                    "Your phone stopped Kavach in the background. Allow unrestricted battery use.",
                )
            }
        }
        return true
    }

    fun reportProtectionActive() {
        _protectionStarting.value = false
        _protectionActive.value = true
        _activationBlockedReason.value = null
    }

    fun reportProtectionFailure(message: String) {
        protectionStartAttempt++
        _protectionStarting.value = false
        _protectionActive.value = false
        _activationBlockedReason.value = message
        debugLog(message)
    }

    fun reportProtectionStopped() {
        protectionStartAttempt++
        _protectionStarting.value = false
        _protectionActive.value = false
    }

    fun clearActivationMessage() {
        _activationBlockedReason.value = null
    }

    /** Restarts Always On after a reboot or app update, if the student left it on. */
    fun restoreKavachIfEnabled() {
        scope.launch {
            if (dataStore.focusShieldEnabled.first() && dataStore.focusShieldAlwaysOnMode.first()) {
                startKavachService(dataStore.focusShieldBlockedPackages.first())
            }
        }
    }

    fun setBlockedPackages(packages: Set<String>) {
        scope.launch {
            dataStore.setFocusShieldBlockedPackages(packages)
            if (isEnabled.value && packages.isNotEmpty() && (isAlwaysOnMode.value || _sessionActive.value)) {
                startKavachService(packages)
                homeRepository.trackKavachEvent("configured", packages.size)
            }
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        scope.launch { dataStore.setFocusShieldScheduleEnabled(enabled) }
    }

    fun setScheduleRange(startMinute: Int, endMinute: Int) {
        scope.launch { dataStore.setFocusShieldScheduleRange(startMinute, endMinute) }
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

    private fun activationWarning(packages: Set<String>): String? =
        KavachActivationReadiness.warning(
            blockedPackages = packages,
            hasUsageAccess = FocusShieldPermissionHelper.hasUsageStatsPermission(appContext),
            hasOverlayPermission = FocusShieldPermissionHelper.hasOverlayPermission(appContext),
        )

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }

    object ShieldPrefs {
        private const val PREFS_NAME = "focus_shield_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_GRACE_UNTIL_MS = "grace_until_ms"
        private const val KEY_GRACE_PACKAGE = "grace_package"
        private const val KEY_GRACE_ORIGIN = "grace_origin"
        private const val KEY_ONE_TIME_UNLOCK_PACKAGE = "one_time_unlock_package"
        private const val KEY_RETURN_GRACE_UNTIL_ELAPSED = "return_grace_until_elapsed"

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun write(
            ctx: Context,
            active: Boolean,
            packages: Set<String>,
            strict: Boolean = false,
            resetUnlocks: Boolean = true,
        ) {
            prefs(ctx).edit().apply {
                putBoolean(KEY_ACTIVE, active)
                putStringSet(KEY_PACKAGES, packages)
                putBoolean("strict", strict)
                if (resetUnlocks) {
                    putLong(KEY_GRACE_UNTIL_MS, 0L)
                    remove(KEY_GRACE_PACKAGE)
                    remove(KEY_GRACE_ORIGIN)
                    putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null)
                }
            }.apply()
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    TAG,
                    "ShieldPrefs.write(active=$active, count=${packages.size}, reset=$resetUnlocks)",
                )
            }
        }

        fun clear(ctx: Context) {
            prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putStringSet(KEY_PACKAGES, emptySet())
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .remove(KEY_GRACE_PACKAGE)
                .remove(KEY_GRACE_ORIGIN)
                .putString(KEY_ONE_TIME_UNLOCK_PACKAGE, null)
                .apply()
            QuickUnlockNotification.cancel(ctx)
            if (BuildConfig.DEBUG) android.util.Log.d(TAG, "ShieldPrefs.clear()")
        }

        fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)
        fun isStrict(ctx: Context): Boolean = prefs(ctx).getBoolean("strict", false)
        fun getPackages(ctx: Context): Set<String> =
            prefs(ctx).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()

        private const val KEY_LAST_QUICK_UNLOCK_MINUTES = "last_quick_unlock_minutes"
        private const val KEY_QUICK_UNLOCK_JUST_EXPIRED = "quick_unlock_just_expired"

        fun getGraceUntilMs(ctx: Context): Long = prefs(ctx).getLong(KEY_GRACE_UNTIL_MS, 0L)
        fun isInGracePeriod(ctx: Context): Boolean = System.currentTimeMillis() < getGraceUntilMs(ctx)

        fun isInGracePeriodForPackage(ctx: Context, packageName: String): Boolean {
            if (!isInGracePeriod(ctx)) return false
            val scopedPackage = prefs(ctx).getString(KEY_GRACE_PACKAGE, null)
            // A blank value preserves an unlock created by an older installed build.
            return scopedPackage.isNullOrBlank() || scopedPackage == packageName
        }

        fun quickUnlockOrigin(ctx: Context): String? =
            prefs(ctx).getString(KEY_GRACE_ORIGIN, null).takeIf { isInGracePeriod(ctx) }

        /** Grants a quick-unlock grace window (flat duration, no per-session quota). */
        fun applyEmergencyUnlock(
            ctx: Context,
            graceUntilMs: Long,
            minutes: Int = 0,
            userName: String? = null,
            packageName: String? = null,
            origin: String = QUICK_UNLOCK_ORIGIN_KAVACH,
        ) {
            prefs(ctx).edit()
                .putLong(KEY_GRACE_UNTIL_MS, graceUntilMs)
                .putInt(KEY_LAST_QUICK_UNLOCK_MINUTES, minutes)
                .putBoolean(KEY_QUICK_UNLOCK_JUST_EXPIRED, true)
                .putString(KEY_GRACE_PACKAGE, packageName)
                .putString(KEY_GRACE_ORIGIN, origin)
                .apply()
            QuickUnlockNotification.show(ctx, graceUntilMs, minutes, userName, origin)
            if (BuildConfig.DEBUG) {
                android.util.Log.d(TAG, "ShieldPrefs.applyEmergencyUnlock(graceUntilMs=$graceUntilMs, minutes=$minutes)")
            }
        }

        fun clearQuickUnlock(ctx: Context) {
            prefs(ctx).edit()
                .putLong(KEY_GRACE_UNTIL_MS, 0L)
                .putBoolean(KEY_QUICK_UNLOCK_JUST_EXPIRED, false)
                .remove(KEY_GRACE_PACKAGE)
                .remove(KEY_GRACE_ORIGIN)
                .apply()
            QuickUnlockNotification.cancel(ctx)
            if (BuildConfig.DEBUG) android.util.Log.d(TAG, "ShieldPrefs.clearQuickUnlock()")
        }

        fun consumeQuickUnlockJustExpired(ctx: Context): Int {
            val p = prefs(ctx)
            val justExpired = p.getBoolean(KEY_QUICK_UNLOCK_JUST_EXPIRED, false)
            if (justExpired && !isInGracePeriod(ctx)) {
                val mins = p.getInt(KEY_LAST_QUICK_UNLOCK_MINUTES, 0)
                p.edit().putBoolean(KEY_QUICK_UNLOCK_JUST_EXPIRED, false).apply()
                return mins
            }
            return 0
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

        const val QUICK_UNLOCK_ORIGIN_KAVACH = "kavach"
        const val QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY = "youtube_study"

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

        fun isWithinSchedule(currentMinuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
            return if (startMinute <= endMinute) {
                currentMinuteOfDay in startMinute..endMinute
            } else {
                currentMinuteOfDay >= startMinute || currentMinuteOfDay <= endMinute
            }
        }
    }

    object Snapshot {
        @Volatile var active: Boolean = false
        @Volatile var packages: Set<String> = emptySet()
        @Volatile var strict: Boolean = false
        @Volatile var scheduleEnabled: Boolean = false
        @Volatile var scheduleStartMinute: Int = 540
        @Volatile var scheduleEndMinute: Int = 1320
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
