package com.safar.app.ui.ekagra.focusshield

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.accessibility.AccessibilityEvent
import com.safar.app.BuildConfig
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.ui.ekagra.TimerService

class FocusShieldAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt: Long = 0L
    // Timestamp (elapsedRealtime) after which blocking is suppressed due to user returning to focus.
    private var returnToFocusGraceUntil: Long = 0L
    private var overlayController: FocusShieldOverlayController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val foregroundMonitor = object : Runnable {
        override fun run() {
            monitorForegroundPackage()
            handler.postDelayed(this, FOREGROUND_POLL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayController = FocusShieldOverlayController(this).also {
            it.onReturnToFocus = ::onUserReturnedToFocus
        }
        handler.post(foregroundMonitor)
        debugLog("Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (packageName == this.packageName) {
            val className = event.className?.toString().orEmpty()
            if (
                className == "com.safar.app.MainActivity" ||
                className == BlockedAppActivity::class.java.name
            ) {
                overlayController?.hide()
            }
            return
        }

        val active = FocusShieldRepository.ShieldPrefs.isActive(this)
        if (!active) {
            overlayController?.hide()
            return
        }

        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (packageName !in blockedPackages) {
            if (shouldHideForPackage(packageName)) {
                overlayController?.hide()
            }
            return
        }

        launchBlockScreen(packageName)
    }

    override fun onInterrupt() {
        overlayController?.hide()
    }

    override fun onDestroy() {
        handler.removeCallbacks(foregroundMonitor)
        overlayController?.hide()
        overlayController = null
        super.onDestroy()
    }

    private fun monitorForegroundPackage() {
        val active = FocusShieldRepository.ShieldPrefs.isActive(this)
        if (!active) {
            overlayController?.hide()
            return
        }

        // Suppress blocking during the grace period after the user tapped "Return to Focus".
        if (SystemClock.elapsedRealtime() < returnToFocusGraceUntil) return

        val foregroundPackage = currentForegroundPackage() ?: return
        if (foregroundPackage == packageName) return

        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (foregroundPackage in blockedPackages) {
            launchBlockScreen(foregroundPackage)
        } else if (shouldHideForPackage(foregroundPackage)) {
            overlayController?.hide()
        }
    }

    private fun launchBlockScreen(blockedPackage: String) {
        val now = SystemClock.elapsedRealtime()
        if (
            overlayController.isBlockingPackage(blockedPackage) &&
            lastBlockedPackage == blockedPackage &&
            now - lastBlockedAt < BLOCK_DEBOUNCE_MS
        ) return
        lastBlockedPackage = blockedPackage
        lastBlockedAt = now

        debugLog("Blocking $blockedPackage")

        // 1) Show an Accessibility overlay immediately (works even when SAFAR is backgrounded).
        val overlayShown = overlayController?.show(blockedPackage) == true
        if (!overlayShown) {
            debugLog("Overlay show failed; falling back")
        }

        // 2) Still notify TimerService so it can post a notification (and attempt Activity launch as fallback).
        if (!requestBlockOverlay(blockedPackage)) {
            // Android 13/14 may deny background activity starts; keep this as a last-resort fallback.
            if (!overlayShown) startBlockedAppActivity(blockedPackage)
        }
    }

    private fun requestBlockOverlay(blockedPackage: String): Boolean {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_FOCUS_SHIELD_BLOCKED
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }

        return runCatching {
            startService(intent)
            true
        }.getOrElse {
            debugLog("Block overlay service start failed: ${it.javaClass.simpleName}")
            false
        }
    }

    private fun startBlockedAppActivity(blockedPackage: String) {
        startActivity(
            Intent(this, BlockedAppActivity::class.java)
                .putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                ),
        )
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShieldA11y", message)
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
            packageName.contains("launcher", ignoreCase = true) ||
            packageName == "com.android.settings"
    }

    private fun FocusShieldOverlayController?.isBlockingPackage(packageName: String): Boolean {
        return this?.shownForPackage == packageName
    }

    /** Called by the overlay when the user taps "Return to Focus" — suppresses re-blocking. */
    fun onUserReturnedToFocus() {
        returnToFocusGraceUntil = SystemClock.elapsedRealtime() + RETURN_GRACE_MS
        lastBlockedPackage = null
        lastBlockedAt = 0L
        overlayController?.hide()
    }

    companion object {
        private const val FOREGROUND_POLL_MS = 250L
        private const val FOREGROUND_LOOKBACK_MS = 2_000L
        private const val BLOCK_DEBOUNCE_MS = 750L
        private const val RETURN_GRACE_MS = 2_000L
    }

    private class FocusShieldOverlayController(
        private val context: Context,
        var onReturnToFocus: (() -> Unit)? = null,
    ) {
        private var wm: WindowManager? = null
        private var root: View? = null
        var shownForPackage: String? = null
            private set
        val isShowing: Boolean
            get() = root != null

        fun show(blockedPackage: String): Boolean {
            if (root != null && shownForPackage == blockedPackage) return true
            hide()

            val windowManager = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?: return false
            wm = windowManager

            val overlayRoot = FrameLayout(context).apply {
                setBackgroundColor(Color.rgb(8, 11, 18))
                isClickable = true
                isFocusable = true
            }

            val accent = Color.rgb(245, 158, 11)
            val appName = labelForPackage(context, blockedPackage)

            val scrim = View(context).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.rgb(12, 18, 30),
                        Color.rgb(8, 11, 18),
                        Color.rgb(3, 7, 12),
                    ),
                )
            }
            overlayRoot.addView(
                scrim,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(Color.rgb(18, 24, 36), dp(22), Color.argb(95, 245, 158, 11), dp(1))
                val padH = dp(22)
                val padV = dp(24)
                setPadding(padH, padV, padH, padV)
                elevation = dp(10).toFloat()
            }

            val icon = TextView(context).apply {
                text = "!"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(accent)
                background = roundedBg(Color.argb(35, 245, 158, 11), dp(36), Color.argb(120, 245, 158, 11), dp(1))
            }

            val title = TextView(context).apply {
                text = "Blocked for Focus"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }

            val appChip = TextView(context).apply {
                text = appName
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(accent)
                background = roundedBg(Color.argb(30, 245, 158, 11), dp(999), Color.argb(80, 245, 158, 11), dp(1))
                setPadding(dp(14), dp(7), dp(14), dp(7))
            }

            val body = TextView(context).apply {
                text = "Kavach is active. This app is blocked until your current focus timer or Study Session ends."
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(203, 213, 225))
                setLineSpacing(dp(2).toFloat(), 1.0f)
            }

            val button = TextView(context).apply {
                text = "Return to Focus"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(15, 23, 42))
                background = roundedBg(accent, dp(999), Color.TRANSPARENT, 0)
                minHeight = dp(50)
                setOnClickListener {
                    // Notify the service first so grace period is set before activity starts.
                    onReturnToFocus?.invoke()
                    // User-initiated click: safe to bring SAFAR to foreground.
                    val intent = NotificationDeepLinkHandler.activityIntent(context, "safar://ekagra")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
            }

            val iconParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            container.addView(icon, iconParams)
            container.addView(space(context, 18))
            container.addView(title)
            container.addView(space(context, 10))
            container.addView(
                appChip,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { gravity = Gravity.CENTER_HORIZONTAL },
            )
            container.addView(space(context, 12))
            container.addView(body)
            container.addView(space(context, 22))
            container.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ),
            )

            overlayRoot.addView(
                container,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.CENTER
                    val margin = dp(24)
                    setMargins(margin, margin, margin, margin)
                },
            )

            val type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.OPAQUE,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            return runCatching {
                windowManager.addView(overlayRoot, params)
                root = overlayRoot
                shownForPackage = blockedPackage
                true
            }.getOrElse {
                root = null
                shownForPackage = null
                false
            }
        }

        fun hide() {
            val view = root ?: return
            root = null
            shownForPackage = null
            runCatching { wm?.removeView(view) }
        }

        private fun dp(value: Int): Int {
            return (value * context.resources.displayMetrics.density).toInt()
        }

        private fun roundedBg(
            color: Int,
            radius: Int,
            strokeColor: Int,
            strokeWidth: Int,
        ): GradientDrawable {
            return GradientDrawable().apply {
                setColor(color)
                cornerRadius = radius.toFloat()
                if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
            }
        }

        private fun space(context: Context, dp: Int): View {
            return View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (dp * context.resources.displayMetrics.density).toInt(),
                )
            }
        }

        private fun labelForPackage(context: Context, packageName: String): String {
            if (packageName.isBlank()) return "This app"
            return runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault("This app")
        }
    }
}
