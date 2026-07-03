package com.safarparmar.app.ui.ekagra.focusshield

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
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
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
            if (
                className == "com.safarparmar.app.MainActivity" ||
                className == BlockedAppActivity::class.java.name
            ) {
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

    override fun onInterrupt() {
    }

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

        redirectToSafar(blockedPackage, strict, unlocksRemaining, unlockSeconds)
    }

    private fun requestBlockNotification(
        blockedPackage: String,
        strict: Boolean,
        unlocksRemaining: Int,
        unlockSeconds: Int,
    ): Boolean {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_FOCUS_SHIELD_BLOCKED
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            putExtra(BlockedAppActivity.EXTRA_BEAST_MODE, strict)
            putExtra(BlockedAppActivity.EXTRA_ALWAYS_ON, false)
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

    private fun redirectToSafar(
        blockedPackage: String,
        strict: Boolean,
        unlocksRemaining: Int,
        unlockSeconds: Int,
    ) {
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
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_ALWAYS_ON, false)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_UNLOCKS_REMAINING, unlocksRemaining)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_UNLOCK_SECONDS, unlockSeconds)
            putExtra(MainActivity.EXTRA_FOCUS_SHIELD_OPEN_EKAGRA, openEkagra)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            debugLog("SAFAR redirect failed: ${it.javaClass.simpleName}")
            requestBlockNotification(blockedPackage, strict, unlocksRemaining, unlockSeconds)
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
        private const val RETURN_GRACE_MS = 5_000L
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

    private class FocusShieldOverlayController(
        private val context: Context,
        var onReturnToFocus: (() -> Unit)? = null,
        var onEmergencyUnlock: (() -> Int?)? = null,
    ) {
        private var wm: WindowManager? = null
        private var backdropRoot: View? = null
        private var cardRoot: View? = null
        private var blockerRoots: List<View> = emptyList()
        var shownForPackage: String? = null
            private set
        val isShowing: Boolean
            get() = backdropRoot != null || cardRoot != null || blockerRoots.isNotEmpty()

        fun show(
            blockedPackage: String,
            strict: Boolean = false,
            unlocksRemaining: Int = 0,
            unlockSeconds: Int = 60,
        ): Boolean {
            if (isShowing && shownForPackage == blockedPackage) return true
            hide()

            val windowManager = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?: return false
            wm = windowManager

            val primary = Color.rgb(10, 86, 217)
            val backdropView = FrameLayout(context).apply {
                setBackgroundColor(primary)
                isClickable = false
                isFocusable = false
            }
            val cardWindow = FrameLayout(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = false
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
                text = context.getString(
                    if (strict) R.string.kavach_block_return_ekagra else R.string.kavach_block_return_safar,
                )
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
                    val intent = if (strict) {
                        NotificationDeepLinkHandler.activityIntent(context, "safar://ekagra")
                    } else {
                        NotificationDeepLinkHandler.activityIntent(context, "safar://home")
                    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

            // Quick unlock secondary CTA.
            if (unlocksRemaining >= 0) {
                container.addView(space(context, 12))
                val unlockButton = TextView(context).apply {
                    val initialEnabled = true
                    text = "Quick unlock"
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
                            onEmergencyUnlock?.invoke()
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
            if (false) {
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

            cardWindow.addView(
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

            val overlayBounds = overlayBounds(windowManager)
            val estimatedCardHeight = estimatedCardHeight(strict, unlocksRemaining)
            val cardTop = overlayBounds.top + ((overlayBounds.height - estimatedCardHeight) / 2)
                .coerceAtLeast(0)
            val gestureTopReserve = dp(24)
            val gestureBottomReserve = dp(72)
            val backdropParams = WindowManager.LayoutParams(
                overlayBounds.width,
                overlayBounds.height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = overlayBounds.left
                y = overlayBounds.top
            }
            val cardParams = WindowManager.LayoutParams(
                overlayBounds.width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = overlayBounds.left
                y = cardTop
            }
            val blockerWindows = buildTouchBlockers(
                type = type,
                overlayBounds = overlayBounds,
                cardTop = cardTop,
                estimatedCardHeight = estimatedCardHeight,
                topGestureReserve = gestureTopReserve,
                bottomGestureReserve = gestureBottomReserve,
            )

            return runCatching {
                windowManager.addView(backdropView, backdropParams)
                blockerWindows.forEach { (view, params) -> windowManager.addView(view, params) }
                windowManager.addView(cardWindow, cardParams)
                backdropRoot = backdropView
                cardRoot = cardWindow
                blockerRoots = blockerWindows.map { it.first }
                shownForPackage = blockedPackage
                true
            }.getOrElse {
                runCatching { windowManager.removeView(backdropView) }
                blockerWindows.forEach { (view, _) -> runCatching { windowManager.removeView(view) } }
                runCatching { windowManager.removeView(cardWindow) }
                backdropRoot = null
                cardRoot = null
                blockerRoots = emptyList()
                shownForPackage = null
                false
            }
        }

        fun hide() {
            val backdrop = backdropRoot
            val card = cardRoot
            val blockers = blockerRoots
            if (backdrop == null && card == null && blockers.isEmpty()) return
            backdropRoot = null
            cardRoot = null
            blockerRoots = emptyList()
            shownForPackage = null
            runCatching { if (card != null) wm?.removeView(card) }
            blockers.forEach { blocker -> runCatching { wm?.removeView(blocker) } }
            runCatching { if (backdrop != null) wm?.removeView(backdrop) }
        }

        private fun dp(value: Int): Int {
            return (value * context.resources.displayMetrics.density).toInt()
        }

        private fun overlayBounds(windowManager: WindowManager): OverlayBounds {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = windowManager.currentWindowMetrics
                val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
                )
                val bounds = metrics.bounds
                val width = (bounds.width() - insets.left - insets.right).coerceAtLeast(1)
                val height = (bounds.height() - insets.bottom).coerceAtLeast(1)
                return OverlayBounds(
                    left = insets.left,
                    top = 0,
                    width = width,
                    height = height,
                )
            }

            val displayMetrics = context.resources.displayMetrics
            val bottomInset = systemDimension("navigation_bar_height")
            return OverlayBounds(
                left = 0,
                top = 0,
                width = displayMetrics.widthPixels.coerceAtLeast(1),
                height = (displayMetrics.heightPixels - bottomInset).coerceAtLeast(1),
            )
        }

        private fun systemDimension(name: String): Int {
            val resourceId = context.resources.getIdentifier(name, "dimen", "android")
            return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }

        private fun estimatedCardHeight(strict: Boolean, unlocksRemaining: Int): Int {
            val base = 84 + 18 + 32 + 10 + 36 + 12 + 44 + 22 + 52 + 48
            val unlock = if (unlocksRemaining >= 0) 12 + 46 else 0
            return dp(base + unlock)
        }

        private fun buildTouchBlockers(
            type: Int,
            overlayBounds: OverlayBounds,
            cardTop: Int,
            estimatedCardHeight: Int,
            topGestureReserve: Int,
            bottomGestureReserve: Int,
        ): List<Pair<View, WindowManager.LayoutParams>> {
            val blockers = mutableListOf<Pair<View, WindowManager.LayoutParams>>()
            val topBlockerTop = overlayBounds.top + topGestureReserve
            val topBlockerHeight = (cardTop - topBlockerTop).coerceAtLeast(0)
            if (topBlockerHeight > 0) {
                blockers += touchBlocker() to blockerParams(
                    type = type,
                    bounds = overlayBounds,
                    y = topBlockerTop,
                    height = topBlockerHeight,
                )
            }

            val bottomBlockerTop = cardTop + estimatedCardHeight
            val blockerBottom = overlayBounds.top + overlayBounds.height - bottomGestureReserve
            val bottomBlockerHeight = (blockerBottom - bottomBlockerTop).coerceAtLeast(0)
            if (bottomBlockerHeight > 0) {
                blockers += touchBlocker() to blockerParams(
                    type = type,
                    bounds = overlayBounds,
                    y = bottomBlockerTop,
                    height = bottomBlockerHeight,
                )
            }
            return blockers
        }

        private fun touchBlocker(): View {
            return View(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = false
            }
        }

        private fun blockerParams(
            type: Int,
            bounds: OverlayBounds,
            y: Int,
            height: Int,
        ): WindowManager.LayoutParams {
            return WindowManager.LayoutParams(
                bounds.width,
                height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = bounds.left
                this.y = y
            }
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

        private data class OverlayBounds(
            val left: Int,
            val top: Int,
            val width: Int,
            val height: Int,
        )

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
