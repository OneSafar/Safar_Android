package com.safarparmar.app.ui.ekagra.focusshield

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.safarparmar.app.MainActivity
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
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
 * Keeps KAVACH blocking apps outside Ekagra, after the student explicitly turns
 * Always On on.
 *
 * Same Usage Access + overlay path as timer-bound blocking, but it owns no timer
 * state: as long as this service is running and its notification is showing, the
 * chosen apps stay blocked whether or not a study session is in progress.
 *
 * Fully independent of Ekagra: a timer running, paused, or on a break changes
 * nothing about whether the chosen apps are blocked. The only coordination with
 * [com.safarparmar.app.ui.ekagra.TimerService] is that this service stays quiet
 * while that one is already driving blocking, so the two never launch the block
 * screen over each other. The app is blocked either way, so that hand-off is
 * invisible to the student.
 */
class KavachAlwaysOnService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1010
        private const val SETTINGS_SYNC_MS = 2_000L
        private const val FOREGROUND_LOOKBACK_MS = 2_000L
        private const val BLOCK_DEBOUNCE_MS = 750L

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

    private var monitorJob: Job? = null
    private var blockedPackages: Set<String> = emptySet()
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    /** Package already counted once for this foreground visit. */
    private var countedAttemptPackage: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildStatusNotification())
        if (monitorJob == null) startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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
                delay(waitMs)
                untilSync -= waitMs
            }
        }
    }

    /** @return false when Always On should no longer be running, which stops the service. */
    private suspend fun syncSettings(): Boolean {
        val alwaysOn = dataStore.focusShieldAlwaysOnMode.first()
        val enabled = dataStore.focusShieldEnabled.first()
        val packages = dataStore.focusShieldBlockedPackages.first()
        val ready = FocusShieldPermissionHelper.hasUsageStatsPermission(this) &&
            FocusShieldPermissionHelper.hasOverlayPermission(this)

        if (!alwaysOn || !enabled || packages.isEmpty() || !ready) {
            KavachAlwaysOnPrefs.clear(this)
            FocusShieldRepository.Snapshot.active = false
            stopSelf()
            return false
        }

        if (packages != blockedPackages) {
            // The app list changed underneath us — start checking attentively again.
            poller.reset()
            countedAttemptPackage = null
        }
        blockedPackages = packages
        KavachAlwaysOnPrefs.write(this, packages)

        // Don't clobber a live session's snapshot; TimerService owns it then.
        if (!FocusShieldRepository.ShieldPrefs.isActive(this)) {
            FocusShieldRepository.Snapshot.active = true
            FocusShieldRepository.Snapshot.packages = packages
            FocusShieldRepository.Snapshot.strict = false
        }
        return true
    }

    /** @return how long to wait before the next poll. */
    private fun monitorForegroundApp(): Long {
        // Always On is independent of Ekagra by definition: it does not care whether
        // a timer is running, paused, or on a break. The only thing checked here is
        // whether TimerService is *already* driving blocking, and that is purely so
        // the two don't both launch the block screen over each other — the app stays
        // blocked either way, so yielding is invisible to the student.
        //
        // Note this deliberately does not stand down for a BREAK. An earlier version
        // did, which meant a student on Always On could open a blocked app simply by
        // starting a break — exactly the loophole the mode exists to close.
        if (FocusShieldRepository.ShieldPrefs.isActive(this)) {
            lastBlockedPackage = null
            countedAttemptPackage = null
            return poller.onSample(null, isBlockedApp = false)
        }

        // Honour the quick-unlock window the block screen grants.
        if (FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            return poller.onSample(null, isBlockedApp = false)
        }

        val foregroundPackage = currentForegroundPackage()
            ?: return poller.onSample(null, isBlockedApp = false)

        if (foregroundPackage == packageName ||
            foregroundPackage == "com.android.settings" ||
            isHomePackage(foregroundPackage)
        ) {
            lastBlockedPackage = null
            countedAttemptPackage = null
            return poller.onSample(foregroundPackage, isBlockedApp = false)
        }

        val isBlocked = foregroundPackage in blockedPackages
        if (isBlocked) {
            launchBlockScreen(foregroundPackage)
        } else {
            countedAttemptPackage = null
        }
        return poller.onSample(foregroundPackage, isBlockedApp = isBlocked)
    }

    private fun launchBlockScreen(blockedPackage: String) {
        val now = SystemClock.elapsedRealtime()
        if (blockedPackage == lastBlockedPackage && now - lastBlockedAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedPackage = blockedPackage
        lastBlockedAt = now

        // One attempt per foreground visit, matching how timer-bound Kavach counts,
        // so Always On days and session days are directly comparable in analytics.
        if (countedAttemptPackage != blockedPackage) {
            countedAttemptPackage = blockedPackage
            runCatching {
                KavachAnalyticsRecorder.from(applicationContext).blockedAttempt(blockedPackage)
            }
        }

        val appName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(blockedPackage, 0)).toString()
        }.getOrDefault("This app")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE, blockedPackage)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME, appName)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_STRICT, false)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_ALWAYS_ON, true)
        }
        runCatching { startActivity(intent) }
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
        return NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle("KAVACH Always On")
            .setContentText("Your chosen apps stay blocked. Tap to turn Always On off in KAVACH.")
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
