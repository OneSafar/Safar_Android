package com.safarparmar.app.ui.ekagra.focusshield

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.MainActivity
import com.safarparmar.app.ui.ekagra.TimerService

class FocusShieldAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt: Long = 0L
    /** Package for which we already counted one distraction this foreground visit. */
    private var countedDistractionPackage: String? = null
    private val homePackages: Set<String> by lazy { resolveHomePackages() }
    private val handler = Handler(Looper.getMainLooper())
    private val foregroundMonitor = object : Runnable {
        override fun run() {
            monitorForegroundPackage()
            handler.postDelayed(this, FOREGROUND_POLL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.post(foregroundMonitor)
        debugLog("Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (packageName == this.packageName) {
            FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
            val className = event.className?.toString().orEmpty()
            if (className == "com.safarparmar.app.MainActivity") {
                lastBlockedPackage = null
                lastBlockedAt = 0L
            }
            return
        }
        if (isHomePackage(packageName)) {
            countedDistractionPackage = null
            FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
            FocusShieldRepository.ShieldPrefs.clearReturnToFocusGrace(this)
            return
        }

        val active = FocusShieldRepository.ShieldPrefs.isActive(this)
        if (!active) {
            return
        }

        // Honour the emergency-unlock grace window even when triggered from an accessibility event.
        if (FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            return
        }

        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (packageName !in blockedPackages) {
            if (shouldHideForPackage(packageName)) {
                FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
                lastBlockedPackage = null
                lastBlockedAt = 0L
            }
            return
        }

        if (FocusShieldRepository.ShieldPrefs.isInReturnToFocusGrace(this)) return
        if (FocusShieldRepository.ShieldPrefs.isOneTimeUnlockedPackage(this, packageName)) return

        scheduleBlockScreen(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(foregroundMonitor)
        super.onDestroy()
    }

    private fun monitorForegroundPackage() {
        val active = FocusShieldRepository.ShieldPrefs.isActive(this)
        if (!active) {
            return
        }

        // Suppress blocking during the grace period after the user tapped the return button.
        if (FocusShieldRepository.ShieldPrefs.isInReturnToFocusGrace(this)) return
        // Also honour the emergency-unlock grace window (wall-clock based, survives reboots).
        if (FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            return
        }

        val foregroundPackage = currentForegroundPackage() ?: return
        if (foregroundPackage == packageName) {
            FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
            return
        }
        if (isHomePackage(foregroundPackage)) {
            countedDistractionPackage = null
            FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
            FocusShieldRepository.ShieldPrefs.clearReturnToFocusGrace(this)
            return
        }

        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (foregroundPackage in blockedPackages) {
            if (FocusShieldRepository.ShieldPrefs.isOneTimeUnlockedPackage(this, foregroundPackage)) return
            scheduleBlockScreen(foregroundPackage)
        } else {
            if (countedDistractionPackage != null && shouldHideForPackage(foregroundPackage)) {
                countedDistractionPackage = null
                FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
                FocusShieldRepository.ShieldPrefs.clearReturnToFocusGrace(this)
            }
            if (shouldHideForPackage(foregroundPackage)) {
                FocusShieldRepository.ShieldPrefs.clearOneTimeUnlock(this)
                lastBlockedPackage = null
                lastBlockedAt = 0L
            }
        }
    }

    /** Accessibility callbacks can arrive off the main thread; serialize block handling. */
    private fun scheduleBlockScreen(blockedPackage: String) {
        if (isHomePackage(blockedPackage)) {
            return
        }
        handler.post { launchBlockScreen(blockedPackage) }
    }

    private fun launchBlockScreen(blockedPackage: String) {
        if (isHomePackage(blockedPackage)) {
            return
        }
        if (FocusShieldRepository.ShieldPrefs.isInReturnToFocusGrace(this)) {
            debugLog("Skipping block during return-to-ekagra grace for $blockedPackage")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (
            lastBlockedPackage == blockedPackage &&
            now - lastBlockedAt < BLOCK_DEBOUNCE_MS
        ) {
            return
        }
        lastBlockedPackage = blockedPackage
        lastBlockedAt = now

        val strict = FocusShieldRepository.ShieldPrefs.isStrict(this)

        val isNewDistractionVisit = countedDistractionPackage != blockedPackage
        if (isNewDistractionVisit) {
            countedDistractionPackage = blockedPackage
            debugLog("Distraction counted for $blockedPackage (strict=$strict)")
            runCatching {
                dagger.hilt.android.EntryPointAccessors
                    .fromApplication(applicationContext, FocusShieldEntryPoint::class.java)
                    .focusShieldRepository()
                    .recordBlockedHit(blockedPackage)
            }
        } else {
            debugLog("Re-blocking $blockedPackage (same visit, not counted again)")
        }

        showBlockUi(blockedPackage, strict)
    }

    private fun requestBlockNotification(blockedPackage: String): Boolean {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_FOCUS_SHIELD_BLOCKED
            putExtra(FocusShieldRepository.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }

        return runCatching {
            startService(intent)
            true
        }.getOrElse {
            debugLog("Block overlay service start failed: ${it.javaClass.simpleName}")
            false
        }
    }

    private fun showBlockUi(blockedPackage: String, strict: Boolean) {
        requestBlockNotification(blockedPackage)

        val appName = labelForPackage(blockedPackage)
        val openEkagra = TimerService.isFocusTimerRunning(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE, blockedPackage)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME, appName)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_STRICT, strict)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_OPEN_EKAGRA, openEkagra)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            debugLog("SAFAR redirect failed: ${it.javaClass.simpleName}")
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShieldA11y", message)
    }

    private fun labelForPackage(packageName: String): String {
        if (packageName.isBlank()) return "This app"
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault("This app")
    }

    private fun currentForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - FOREGROUND_LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent =
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            if (isForegroundEvent && !event.packageName.isNullOrBlank()) {
                latestPackage = event.packageName
            }
        }

        return latestPackage
    }

    private fun shouldHideForPackage(packageName: String): Boolean {
        return packageName == this.packageName ||
            isHomePackage(packageName) ||
            packageName == "com.android.settings"
    }

    private fun resolveHomePackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val queried = packageManager.queryIntentActivities(homeIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
        val resolved = packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        return (queried + listOfNotNull(resolved) + KNOWN_HOME_PACKAGES).toSet()
    }

    private fun isHomePackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return packageName in homePackages ||
            packageName.contains("launcher", ignoreCase = true)
    }

    companion object {
        private const val FOREGROUND_POLL_MS = 250L
        private const val FOREGROUND_LOOKBACK_MS = 2_000L
        private const val BLOCK_DEBOUNCE_MS = 750L
        private val KNOWN_HOME_PACKAGES = setOf(
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.vivo.launcher",
            "com.transsion.XOSLauncher",
        )
    }
}
