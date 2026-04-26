package com.safar.app.ui.ekagra.focusshield

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service that intercepts foreground-app changes during an
 * active Ekagra focus session and performs GLOBAL_ACTION_BACK (or HOME on
 * repeated attempts) if the opened app is in the blocked list.
 *
 * Now reads state from [FocusShieldRepository.ShieldPrefs] (SharedPreferences)
 * which survives process death, instead of volatile static fields.
 */
class SafarBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusShieldA11y"
        private const val COOLDOWN_MS = 800L
        private const val MAX_BACK_BEFORE_HOME = 2

        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.motorola.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.android.settings",
            "android",
        )
    }

    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0L
    private var consecutiveBackCount: Int = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.w(TAG, "════════════════════════════════════════════")
        Log.w(TAG, "✓ Focus Shield Accessibility Service CONNECTED")
        Log.w(TAG, "  Our package: $packageName")

        // Check ShieldPrefs state on connect
        val active = FocusShieldRepository.ShieldPrefs.isActive(this)
        val pkgs = FocusShieldRepository.ShieldPrefs.getPackages(this)
        Log.w(TAG, "  ShieldPrefs: active=$active, packages=$pkgs")
        Log.w(TAG, "════════════════════════════════════════════")

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            info.notificationTimeout = 100L
            serviceInfo = info
            Log.w(TAG, "  serviceInfo configured OK")
        } catch (e: Exception) {
            Log.e(TAG, "  Failed to configure serviceInfo", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Read session state from SharedPreferences (survives process death!)
        val isActive = FocusShieldRepository.ShieldPrefs.isActive(this)
        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)

        // Log every window change
        Log.w(TAG, "┌─ Window → $pkgName")
        Log.w(TAG, "│ active=$isActive pkgs=${blockedPackages.size} blocked=${pkgName in blockedPackages}")

        // Gate: no active session
        if (!isActive) {
            Log.w(TAG, "└─ SKIP: not active")
            return
        }

        // Gate: our own app
        if (pkgName == packageName) {
            Log.w(TAG, "└─ SKIP: our app")
            return
        }

        // Gate: system packages
        if (pkgName in IGNORED_PACKAGES) {
            Log.w(TAG, "└─ SKIP: system pkg")
            return
        }

        // Gate: popups/toasts
        if (className.contains("PopupWindow") || className.contains("Toast")) {
            Log.w(TAG, "└─ SKIP: popup")
            return
        }

        // Gate: not blocked
        if (pkgName !in blockedPackages) {
            Log.w(TAG, "└─ SKIP: not in blocked list")
            return
        }

        // === BLOCKED APP DETECTED ===
        val now = System.currentTimeMillis()

        // Cooldown
        if (now - lastInterceptTime < COOLDOWN_MS && lastInterceptedPackage == pkgName) {
            Log.w(TAG, "└─ SKIP: cooldown")
            return
        }

        // Track consecutive attempts
        if (lastInterceptedPackage == pkgName && now - lastInterceptTime < 5_000L) {
            consecutiveBackCount++
        } else {
            consecutiveBackCount = 1
            lastInterceptedPackage = pkgName
        }
        lastInterceptTime = now

        // Perform intervention
        if (consecutiveBackCount >= MAX_BACK_BEFORE_HOME) {
            Log.w(TAG, "└─ 🛡️ BLOCK $pkgName → HOME (#$consecutiveBackCount)")
            performGlobalAction(GLOBAL_ACTION_HOME)
            consecutiveBackCount = 0
        } else {
            Log.w(TAG, "└─ 🛡️ BLOCK $pkgName → BACK (#$consecutiveBackCount)")
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        // Emit event for Compose UI snackbar
        val appName = try {
            val ai = packageManager.getApplicationInfo(pkgName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkgName
        }
        BlockerEventBridge.emit(BlockerEventBridge.BlockedEvent(pkgName, appName))
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service INTERRUPTED")
    }

    override fun onDestroy() {
        Log.w(TAG, "Accessibility Service DESTROYED")
        super.onDestroy()
    }
}
