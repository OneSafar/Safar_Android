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
import com.safar.app.R
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.ui.ekagra.TimerService

class FocusShieldAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt: Long = 0L
    /** Package for which we already counted one distraction this foreground visit. */
    private var countedDistractionPackage: String? = null
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
            it.onEmergencyUnlock = ::onEmergencyUnlockRequested
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

        // Honour the emergency-unlock grace window even when triggered from an accessibility event.
        if (FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
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

        scheduleBlockScreen(packageName)
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
        // Also honour the emergency-unlock grace window (wall-clock based, survives reboots).
        if (FocusShieldRepository.ShieldPrefs.isInGracePeriod(this)) {
            overlayController?.hide()
            return
        }

        val foregroundPackage = currentForegroundPackage() ?: return
        if (foregroundPackage == packageName) return

        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (foregroundPackage in blockedPackages) {
            scheduleBlockScreen(foregroundPackage)
        } else {
            if (countedDistractionPackage != null) {
                countedDistractionPackage = null
            }
            if (shouldHideForPackage(foregroundPackage)) {
                overlayController?.hide()
            }
        }
    }

    /** Accessibility callbacks can arrive off the main thread; serialize block handling. */
    private fun scheduleBlockScreen(blockedPackage: String) {
        handler.post { launchBlockScreen(blockedPackage) }
    }

    private fun launchBlockScreen(blockedPackage: String) {
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
        val unlocksRemaining = FocusShieldRepository.ShieldPrefs.getUnlocksRemaining(this)
        val unlockSeconds = FocusShieldRepository.ShieldPrefs.getUnlockSeconds(this)

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

        // 1) Show an Accessibility overlay immediately (works even when SAFAR is backgrounded).
        val overlayShown = overlayController?.show(
            blockedPackage = blockedPackage,
            strict = strict,
            unlocksRemaining = unlocksRemaining,
            unlockSeconds = unlockSeconds,
        ) == true
        if (!overlayShown) {
            debugLog("Overlay show failed; falling back")
        }

        // 2) Still notify TimerService so it can post a notification (and attempt Activity launch as fallback).
        if (!requestBlockOverlay(blockedPackage, strict, unlocksRemaining, unlockSeconds)) {
            // Android 13/14 may deny background activity starts; keep this as a last-resort fallback.
            if (!overlayShown) startBlockedAppActivity(blockedPackage, strict, unlocksRemaining, unlockSeconds)
        }
    }

    private fun requestBlockOverlay(
        blockedPackage: String,
        strict: Boolean,
        unlocksRemaining: Int,
        unlockSeconds: Int,
    ): Boolean {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_FOCUS_SHIELD_BLOCKED
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            putExtra(BlockedAppActivity.EXTRA_BEAST_MODE, strict)
            putExtra(BlockedAppActivity.EXTRA_UNLOCKS_REMAINING, unlocksRemaining)
            putExtra(BlockedAppActivity.EXTRA_UNLOCK_SECONDS, unlockSeconds)
        }

        return runCatching {
            startService(intent)
            true
        }.getOrElse {
            debugLog("Block overlay service start failed: ${it.javaClass.simpleName}")
            false
        }
    }

    private fun startBlockedAppActivity(
        blockedPackage: String,
        strict: Boolean,
        unlocksRemaining: Int,
        unlockSeconds: Int,
    ) {
        startActivity(
            Intent(this, BlockedAppActivity::class.java)
                .putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
                .putExtra(BlockedAppActivity.EXTRA_BEAST_MODE, strict)
                .putExtra(BlockedAppActivity.EXTRA_UNLOCKS_REMAINING, unlocksRemaining)
                .putExtra(BlockedAppActivity.EXTRA_UNLOCK_SECONDS, unlockSeconds)
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
        countedDistractionPackage = null
        overlayController?.hide()
    }

    /**
     * Called by the overlay when the user taps "Emergency Unlock". Writes a grace window into
     * [FocusShieldRepository.ShieldPrefs] so subsequent foreground checks suppress blocking until
     * the configured duration expires. Returns the new remaining-unlock count, or null when the
     * unlock could not be granted (Beast Mode, disabled, or quota exhausted).
     */
    fun onEmergencyUnlockRequested(): Int? {
        if (FocusShieldRepository.ShieldPrefs.isStrict(this)) return null
        val limit = FocusShieldRepository.ShieldPrefs.getUnlockLimit(this)
        val used = FocusShieldRepository.ShieldPrefs.getUnlocksUsed(this)
        if (limit <= 0 || used >= limit) return 0

        val seconds = FocusShieldRepository.ShieldPrefs.getUnlockSeconds(this).coerceAtLeast(5)
        val graceUntilMs = System.currentTimeMillis() + seconds * 1000L
        val newUsed = used + 1
        FocusShieldRepository.ShieldPrefs.applyEmergencyUnlock(this, graceUntilMs, newUsed)
        // Also clear local debounce so the user can hop straight into the blocked app.
        lastBlockedPackage = null
        lastBlockedAt = 0L
        countedDistractionPackage = null
        overlayController?.hide()
        debugLog("Emergency unlock granted for ${seconds}s ($newUsed/$limit)")
        return (limit - newUsed).coerceAtLeast(0)
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
        var onEmergencyUnlock: (() -> Int?)? = null,
    ) {
        private var wm: WindowManager? = null
        private var root: View? = null
        var shownForPackage: String? = null
            private set
        val isShowing: Boolean
            get() = root != null

        fun show(
            blockedPackage: String,
            strict: Boolean = false,
            unlocksRemaining: Int = 0,
            unlockSeconds: Int = 60,
        ): Boolean {
            if (root != null && shownForPackage == blockedPackage) return true
            hide()

            val windowManager = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?: return false
            wm = windowManager

            val primary = Color.rgb(10, 86, 217)
            val overlayRoot = FrameLayout(context).apply {
                setBackgroundColor(primary)
                isClickable = true
                isFocusable = true
            }

            val accent = Color.WHITE
            val mutedText = Color.rgb(227, 234, 245)
            val appName = labelForPackage(context, blockedPackage)
            val motivational = BLOCK_COPY.random()

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(Color.argb(28, 255, 255, 255), dp(24), Color.argb(60, 255, 255, 255), dp(1))
                val padH = dp(22)
                val padV = dp(24)
                setPadding(padH, padV, padH, padV)
                elevation = dp(10).toFloat()
            }

            val mascot = TextView(context).apply {
                text = "\uD83D\uDEE1"
                textSize = 40f
                gravity = Gravity.CENTER
                setTextColor(accent)
                background = roundedBg(Color.argb(26, 255, 255, 255), dp(48), Color.argb(51, 255, 255, 255), dp(1))
            }

            val title = TextView(context).apply {
                text = "Stay focused"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }

            val appChip = TextView(context).apply {
                text = appName
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = roundedBg(Color.argb(26, 255, 255, 255), dp(999), Color.argb(51, 255, 255, 255), dp(1))
                setPadding(dp(14), dp(7), dp(14), dp(7))
            }

            val body = TextView(context).apply {
                text = motivational
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(mutedText)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            }

            val primaryButton = TextView(context).apply {
                text = context.getString(R.string.kavach_block_return)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(primary)
                background = roundedBg(Color.WHITE, dp(999), Color.TRANSPARENT, 0)
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

            val iconParams = LinearLayout.LayoutParams(dp(84), dp(84)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            container.addView(mascot, iconParams)
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
                primaryButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ),
            )

            // Emergency unlock secondary CTA (only when unlock is enabled and Beast Mode is off).
            if (!strict && unlocksRemaining >= 0) {
                container.addView(space(context, 12))
                val unlockButton = TextView(context).apply {
                    val initialEnabled = unlocksRemaining > 0
                    text = if (initialEnabled) {
                        context.getString(R.string.kavach_block_unlock, unlocksRemaining, unlockSeconds)
                    } else {
                        context.getString(R.string.kavach_block_unlock_exhausted)
                    }
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(if (initialEnabled) Color.WHITE else mutedText)
                    background = roundedBg(
                        if (initialEnabled) Color.argb(26, 255, 255, 255) else Color.argb(20, 255, 255, 255),
                        dp(999),
                        if (initialEnabled) Color.argb(77, 255, 255, 255) else Color.argb(40, 255, 255, 255),
                        dp(1),
                    )
                    minHeight = dp(46)
                    isEnabled = initialEnabled
                    alpha = if (initialEnabled) 1f else 0.6f
                    if (initialEnabled) {
                        setOnClickListener { btn ->
                            val remaining = onEmergencyUnlock?.invoke()
                            if (remaining == null) {
                                // Beast Mode active or disabled — disable button visually.
                                (btn as TextView).text = context.getString(R.string.kavach_block_unlock_exhausted)
                                btn.setTextColor(mutedText)
                                btn.isEnabled = false
                                btn.alpha = 0.6f
                                return@setOnClickListener
                            }
                            if (remaining <= 0) {
                                (btn as TextView).text = context.getString(R.string.kavach_block_unlock_exhausted)
                                btn.setTextColor(mutedText)
                                btn.background = roundedBg(
                                    Color.argb(20, 100, 116, 139),
                                    dp(999),
                                    Color.argb(50, 100, 116, 139),
                                    dp(1),
                                )
                                btn.isEnabled = false
                                btn.alpha = 0.6f
                            }
                        }
                    }
                }
                container.addView(
                    unlockButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(46),
                    ),
                )
            }

            // Beast Mode footer chip (shown when strict mode is on).
            if (strict) {
                container.addView(space(context, 14))
                val beastFooter = TextView(context).apply {
                    text = context.getString(R.string.kavach_block_beast_footer)
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(254, 226, 226))
                    background = roundedBg(Color.argb(40, 239, 68, 68), dp(999), Color.argb(90, 239, 68, 68), dp(1))
                    setPadding(dp(14), dp(7), dp(14), dp(7))
                }
                container.addView(
                    beastFooter,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { gravity = Gravity.CENTER_HORIZONTAL },
                )
            }

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

        companion object {
            // Rotating motivational copy shown on the block overlay. Randomised per block event.
            private val BLOCK_COPY = listOf(
                "One scroll can wait. Your future can't.",
                "You set this boundary. Honour it.",
                "Five more focused minutes change everything.",
                "Distractions are loud. Discipline is louder.",
                "Right now \u2014 the only goal is your goal.",
            )
        }
    }
}
