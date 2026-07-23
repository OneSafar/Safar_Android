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
 * Keeps KAVACH active outside Ekagra after the student explicitly enables Always On.
 * It uses the same Usage Access and overlay path as timer-bound KAVACH, but owns no timer state.
 */
class KavachAlwaysOnService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1010
        private const val FOREGROUND_POLL_MS = 300L
        private const val SETTINGS_SYNC_MS = 1_500L
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore by lazy { SafarDataStore(applicationContext) }
    private var monitorJob: Job? = null
    private var blockedPackages: Set<String> = emptySet()
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

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
                if (untilSync <= 0L && !syncSettings()) return@launch
                monitorForegroundApp()
                delay(FOREGROUND_POLL_MS)
                untilSync -= FOREGROUND_POLL_MS
                if (untilSync <= 0L) untilSync = SETTINGS_SYNC_MS
            }
        }
    }

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
        blockedPackages = packages
        KavachAlwaysOnPrefs.write(this, packages)
        if (!FocusShieldRepository.ShieldPrefs.isActive(this)) {
            FocusShieldRepository.Snapshot.active = true
            FocusShieldRepository.Snapshot.packages = packages
            FocusShieldRepository.Snapshot.strict = false
        }
        return true
    }

    private fun monitorForegroundApp() {
        // Yield to TimerService if an Ekagra focus session or break is active
        val timerPrefs = getSharedPreferences("ekagra_timer_state_prefs", Context.MODE_PRIVATE)
        if (timerPrefs.getBoolean("has_state", false)) {
            val mode = timerPrefs.getString("mode", "FOCUS") ?: "FOCUS"
            // If user is taking a scheduled break during their study session, do NOT block apps
            if (mode == "BREAK") {
                lastBlockedPackage = null
                return
            }
            // If TimerService is actively driving focus/stopwatch/pomodoro blocking, yield to TimerService
            if (FocusShieldRepository.ShieldPrefs.isActive(this)) {
                lastBlockedPackage = null
                return
            }
        }

        val foregroundPackage = currentForegroundPackage() ?: return
        if (foregroundPackage == packageName || foregroundPackage == "com.android.settings" || isHomePackage(foregroundPackage)) {
            lastBlockedPackage = null
            return
        }
        if (foregroundPackage in blockedPackages) launchBlockScreen(foregroundPackage)
    }

    private fun launchBlockScreen(blockedPackage: String) {
        val now = SystemClock.elapsedRealtime()
        if (blockedPackage == lastBlockedPackage && now - lastBlockedAt < BLOCK_DEBOUNCE_MS) return
        lastBlockedPackage = blockedPackage
        lastBlockedAt = now

        val appName = runCatching {
            val info = packageManager.getApplicationInfo(blockedPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("This app")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        val events = manager.queryEvents(now - FOREGROUND_LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
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
            .setContentText("Selected apps stay blocked until you turn Always On off in KAVACH.")
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
    fun packages(context: Context): Set<String> = prefs(context).getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
}
