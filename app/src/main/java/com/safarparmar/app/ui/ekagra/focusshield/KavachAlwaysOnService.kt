package com.safarparmar.app.ui.ekagra.focusshield

import android.app.Notification
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import com.safarparmar.app.MainActivity
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
import com.safarparmar.app.feature.kavachanalytics.data.local.ProtectionSource
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Blocking service used during an Ekagra protection window or continuously under Always On.
 *
 * It monitors the foreground app using Usage Access and shows an overlay block screen
 * if a restricted app is launched. In Normal Mode, it honors the quick unlock grace
 * period, but in Always On Mode it enforces strict continuous blocking without any
 * unlock windows.
 *
 * App blocking logic has been decoupled from the timer logic.
 */
class KavachAlwaysOnService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1010
        private const val SETTINGS_SYNC_MS = 2_000L
        private const val FOREGROUND_LOOKBACK_MS = 2_000L
        private const val BLOCK_DEBOUNCE_MS = 750L
        /** Tighter poll while the overlay is up so it dismisses almost instantly. */
        private const val OVERLAY_DISMISS_POLL_MS = 250L

        private val KNOWN_HOME_PACKAGES = setOf(
            "com.miui.home", "com.mi.android.globallauncher", "com.android.launcher",
            "com.android.launcher2", "com.android.launcher3", "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher", "com.huawei.android.launcher", "com.oppo.launcher",
            "com.vivo.launcher", "com.transsion.XOSLauncher",
        )

        fun start(context: Context) {
            val intent = Intent(context, KavachAlwaysOnService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            KavachAlwaysOnPrefs.clear(context)
            runCatching { context.stopService(Intent(context, KavachAlwaysOnService::class.java)) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore by lazy { SafarDataStore(applicationContext) }
    private val poller = AdaptivePollScheduler()
    private val blockOverlay by lazy { KavachBlockOverlay(this) }

    private var monitorJob: Job? = null
    private var blockedPackages: Set<String> = emptySet()
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    /** Package already counted once for this foreground visit. */
    private var countedAttemptPackage: String? = null
    private var isAlwaysOnMode = false
    private var isStrictMode = false
    private var protectionSource: String? = null
    private var scheduleEnabled = false
    private var scheduleStartMinute = 540
    private var scheduleEndMinute = 1320

    private fun focusShieldRepository(): FocusShieldRepository =
        dagger.hilt.android.EntryPointAccessors
            .fromApplication(applicationContext, FocusShieldEntryPoint::class.java)
            .focusShieldRepository()

    override fun onCreate() {
        super.onCreate()
        // startForegroundService() gives the service only a short deadline to
        // promote itself. Do this at the earliest lifecycle callback rather than
        // waiting for onStartCommand(), which can be delayed on some OEM builds.
        SafarNotificationChannels.createAll(this)
        val notification = buildStatusNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob == null) startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        blockOverlay.dismiss()
        protectionSource?.let { source ->
            runCatching { KavachAnalyticsRecorder.from(applicationContext).endProtection(source) }
        }
        KavachAlwaysOnPrefs.clear(this)
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitorJob = scope.launch {
            var untilSync = 0L
            while (true) {
                if (untilSync <= 0L) {
                    if (!syncSettings()) return@launch
                    untilSync = SETTINGS_SYNC_MS
                }
                val waitMs = monitorForegroundApp()
                // Poll faster while the overlay is visible so it dismisses
                // almost instantly when the student leaves the blocked app.
                val actualWait = if (blockOverlay.isShowing) OVERLAY_DISMISS_POLL_MS.coerceAtMost(waitMs) else waitMs
                delay(actualWait)
                untilSync -= actualWait
            }
        }
    }

    /** @return false when Always On should no longer be running, which stops the service. */
    private suspend fun syncSettings(): Boolean {
        val alwaysOn = dataStore.focusShieldAlwaysOnMode.first()
        val strict = dataStore.focusShieldStrictMode.first()
        val enabled = dataStore.focusShieldEnabled.first()
        val packages = dataStore.focusShieldBlockedPackages.first()
        // Peak Focus scheduling is intentionally outside the first activation/protection release.
        scheduleEnabled = false
        scheduleStartMinute = dataStore.focusShieldScheduleStartMinute.first()
        scheduleEndMinute = dataStore.focusShieldScheduleEndMinute.first()
        val ready = FocusShieldPermissionHelper.hasUsageStatsPermission(this) &&
            FocusShieldPermissionHelper.hasOverlayPermission(this)

        val timerLinkedActive = FocusShieldRepository.ShieldPrefs.isActive(this)
        if (!enabled || (!alwaysOn && !timerLinkedActive) || packages.isEmpty() || !ready) {
            KavachAlwaysOnPrefs.clear(this)
            FocusShieldRepository.Snapshot.active = false
            stopSelf()
            return false
        }
        
        isAlwaysOnMode = alwaysOn
        isStrictMode = strict

        if (packages != blockedPackages) {
            // The app list changed underneath us — start checking attentively again.
            poller.reset()
            countedAttemptPackage = null
        }
        blockedPackages = packages
        KavachAlwaysOnPrefs.write(this, packages)

        // Keep exactly one correctly-labelled protection window alive.
        val nextProtectionSource = if (alwaysOn) ProtectionSource.ALWAYS_ON else ProtectionSource.SESSION
        if (protectionSource != null && protectionSource != nextProtectionSource) {
            runCatching {
                KavachAnalyticsRecorder.from(applicationContext).endProtection(protectionSource!!)
            }
        }
        protectionSource = nextProtectionSource
        runCatching {
            KavachAnalyticsRecorder.from(applicationContext)
                .heartbeatProtection(nextProtectionSource)
        }

        FocusShieldRepository.Snapshot.active = true
        FocusShieldRepository.Snapshot.packages = packages
        FocusShieldRepository.Snapshot.strict = strict
        FocusShieldRepository.Snapshot.scheduleEnabled = scheduleEnabled
        FocusShieldRepository.Snapshot.scheduleStartMinute = scheduleStartMinute
        FocusShieldRepository.Snapshot.scheduleEndMinute = scheduleEndMinute
        return true
    }

    /** @return how long to wait before the next poll. */
    private fun monitorForegroundApp(): Long {
        if (scheduleEnabled) {
            val cal = java.util.Calendar.getInstance()
            val currentMinute = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val withinSchedule = FocusShieldRepository.ShieldPrefs.isWithinSchedule(currentMinute, scheduleStartMinute, scheduleEndMinute)
            if (!withinSchedule) {
                blockOverlay.dismiss()
                lastBlockedPackage = null
                countedAttemptPackage = null
                return poller.onSample(null, isBlockedApp = false)
            }
        }

        // Check if any blocked app (e.g. YouTube) is actively playing in PiP or background
        enforceBackgroundBlockedApps()

        val foregroundPackage = currentForegroundPackage()
            ?: return poller.onSample(null, isBlockedApp = false)

        if (isStrictMode && FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            FocusShieldRepository.ShieldPrefs.clearQuickUnlock(this)
        }

        // Normal mode honors quick unlock grace periods
        if (!isStrictMode && FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            blockOverlay.dismiss()
            lastBlockedPackage = null
            return poller.onSample(null, isBlockedApp = false)
        }

        if (foregroundPackage == packageName ||
            foregroundPackage == "com.android.settings"
        ) {
            blockOverlay.dismiss()
            lastBlockedPackage = null
            countedAttemptPackage = null
            return poller.onSample(foregroundPackage, isBlockedApp = false)
        }

        if (isHomePackage(foregroundPackage)) {
            // Blocking deliberately sends the app Home. Keep the result sheet over the
            // launcher until the student dismisses it or chooses Quick Unlock. This is
            // required for both Normal and Beast under either activation option.
            // Reset visit bookkeeping only, so a later app-open is counted independently.
            lastBlockedPackage = null
            countedAttemptPackage = null
            return poller.onSample(foregroundPackage, isBlockedApp = false)
        }

        val isBlocked = foregroundPackage in blockedPackages
        if (isBlocked) {
            launchBlockScreen(foregroundPackage)
        } else {
            blockOverlay.dismiss()
            countedAttemptPackage = null
        }
        return poller.onSample(foregroundPackage, isBlockedApp = isBlocked)
    }

    private fun enforceBackgroundBlockedApps() {
        if (blockedPackages.isEmpty()) return
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager ?: return
        val runningProcesses = runCatching { am.runningAppProcesses }.getOrNull() ?: return
        for (proc in runningProcesses) {
            val pkgs = proc.pkgList ?: continue
            for (pkg in pkgs) {
                if (pkg in blockedPackages && pkg != packageName) {
                    if (proc.importance <= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ||
                        proc.importance <= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                    ) {
                        BlockedMediaEnforcer.stop(this, pkg)
                    }
                }
            }
        }
    }

    private fun launchBlockScreen(blockedPackage: String) {
        // Keep blocked video from escaping into Android's topmost PiP window.
        BlockedMediaEnforcer.stop(this, blockedPackage)

        val now = SystemClock.elapsedRealtime()
        if (blockedPackage == lastBlockedPackage && now - lastBlockedAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedPackage = blockedPackage
        lastBlockedAt = now

        // One attempt per foreground visit, matching how timer-bound Kavach counts,
        // so Always On days and session days are directly comparable in analytics.
        if (countedAttemptPackage != blockedPackage) {
            countedAttemptPackage = blockedPackage
            runCatching { focusShieldRepository().recordBlockedHit(blockedPackage) }
        }

        val appName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(blockedPackage, 0)).toString()
        }.getOrDefault("This app")

        // Force the blocked app to close / navigate to device home screen immediately for both modes.
        goHome()
        runCatching {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            am?.killBackgroundProcesses(blockedPackage)
        }

        val expiredMinutes = if (!isStrictMode) {
            FocusShieldRepository.ShieldPrefs.consumeQuickUnlockJustExpired(this)
        } else 0

        // Draw the result sheet over Home and leave it there until the student acts.
        blockOverlay.show(
            appName = appName,
            allowQuickUnlock = !isStrictMode,
            blockedPackage = blockedPackage,
            expiredMinutes = expiredMinutes,
        )
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.let { info ->
            homeIntent.component = android.content.ComponentName(info.packageName, info.name)
        }
        runCatching { startActivity(homeIntent) }
    }

    private fun currentForegroundPackage(): String? {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = runCatching { manager.queryEvents(now - FOREGROUND_LOOKBACK_MS, now) }.getOrNull()
            ?: return null
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            if (foreground && !event.packageName.isNullOrBlank()) latest = event.packageName
        }
        return latest
    }

    private fun isHomePackage(packageName: String): Boolean =
        packageName.contains("launcher", ignoreCase = true) || packageName in KNOWN_HOME_PACKAGES

    private fun buildStatusNotification(): Notification {
        val openKavach = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                data = android.net.Uri.parse("safar://${Routes.FOCUS_SHIELD}")
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cal = java.util.Calendar.getInstance()
        val currentMinute = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val isScheduleActive = !scheduleEnabled || FocusShieldRepository.ShieldPrefs.isWithinSchedule(currentMinute, scheduleStartMinute, scheduleEndMinute)

        val title = if (isAlwaysOnMode) "KAVACH Always On" else "KAVACH Active"
        val text = when {
            !isScheduleActive -> "KAVACH Scheduled (Outside active hours)"
            isAlwaysOnMode -> "Always On protection is active."
            else -> "KAVACH protection is active."
        }

        return NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openKavach)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}

internal object KavachAlwaysOnPrefs {
    private const val PREFS_NAME = "kavach_always_on"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PACKAGES = "packages"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun write(context: Context, packages: Set<String>) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, true).putStringSet(KEY_PACKAGES, packages).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).putStringSet(KEY_PACKAGES, emptySet()).apply()
    }

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun packages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
}
