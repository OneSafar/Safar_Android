package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Draws a **bottom-sheet-style** overlay on top of any blocked app using
 * [WindowManager.addView] with [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY].
 *
 * Visually identical to [FocusShieldBlockedBottomSheet] (blue card anchored to
 * the bottom of the screen, rounded top corners, dark scrim behind it), but
 * rendered via [WindowManager] instead of a Compose ModalBottomSheet inside
 * [com.safarparmar.app.MainActivity].
 *
 * This completely bypasses Android's Background Activity Launch (BAL)
 * restrictions which prevent [KavachAlwaysOnService] from calling
 * `startActivity()` to bring MainActivity to the foreground when Safar hasn't
 * been in the foreground recently.
 *
 * Because the app already holds `SYSTEM_ALERT_WINDOW` ("Display over other
 * apps"), this overlay works reliably on every Android version (8+) and every
 * OEM skin (MIUI, One UI, ColorOS, etc.).
 *
 * ### Thread-safety
 * [show] and [dismiss] may be called from any thread. All
 * [WindowManager] mutations are posted to the main-thread [Handler].
 */
class KavachBlockOverlay(
    private val context: Context,
    /** Accessibility services can use their own overlay type without Draw Over Apps. */
    private val accessibilityOverlay: Boolean = false,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** Guards against double-add / double-remove from background poll threads. */
    @Volatile
    var isShowing: Boolean = false
        private set

    private var overlayView: View? = null
    private val stateLock = Any()
    private var content = OverlayContent("This app is blocked", "KAVACH is protecting your focus.", "I'll Control Myself.", { goHome() }, emptyList())
    private var currentBlockedPackage: String? = null
    private var currentQuickUnlockOrigin: String = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH

    // ── Public API ─────────────────────────────────────────────────────

    /** Show the block bottom-sheet for [appName]. No-op if already showing. */
    fun show(
        appName: String,
        allowQuickUnlock: Boolean = false,
        blockedPackage: String? = null,
        expiredMinutes: Int = 0,
    ) {
        currentBlockedPackage = blockedPackage
        val title = if (expiredMinutes > 0) "Quick Unlock Expired" else "$appName is blocked"
        val subtitle = when {
            expiredMinutes > 0 -> "You have been distracted for over $expiredMinutes minutes."
            allowQuickUnlock -> "KAVACH is protecting your focus. Need a quick break?"
            else -> "Always On protection is active. Open KAVACH to turn it off."
        }
        showContent(
            title = title,
            subtitle = subtitle,
            buttonText = "I'll Control Myself.",
            onAction = ::goHome,
            quickUnlockMinutes = if (allowQuickUnlock) listOf(5, 10, 15, 20) else emptyList(),
            blockedPackage = blockedPackage,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH,
        )
    }

    fun showContent(
        title: String,
        subtitle: String,
        buttonText: String,
        onAction: () -> Unit,
        quickUnlockMinutes: List<Int> = emptyList(),
        blockedPackage: String? = null,
        quickUnlockOrigin: String = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH,
    ) {
        synchronized(stateLock) {
            if (isShowing) return
            if (!accessibilityOverlay && !FocusShieldPermissionHelper.hasOverlayPermission(context)) return
            content = OverlayContent(title, subtitle, buttonText, onAction, quickUnlockMinutes)
            currentBlockedPackage = blockedPackage
            currentQuickUnlockOrigin = quickUnlockOrigin
            isShowing = true
        }
        mainHandler.post { showInternal() }
    }

    /** Remove the overlay. Safe to call even if not showing. */
    fun dismiss() {
        synchronized(stateLock) {
            if (!isShowing) return
            isShowing = false
        }
        mainHandler.post { dismissInternal() }
    }

    // ── Internals (main thread only) ───────────────────────────────────

    private fun showInternal() {
        if (overlayView != null) return          // guard against race

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (accessibilityOverlay)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            // We WANT the view to receive touches (the button) so we do NOT
            // set FLAG_NOT_FOCUSABLE.  FLAG_LAYOUT_IN_SCREEN draws behind the
            // status bar for a truly full-screen scrim.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.CENTER

        val view = buildBottomSheetView()
        runCatching { windowManager.addView(view, params) }
            .onSuccess { overlayView = view }
            .onFailure { isShowing = false }
    }

    private fun dismissInternal() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
            overlayView = null
        }
    }

    // ── View builder (bottom-sheet style) ──────────────────────────────

    private fun buildBottomSheetView(): View {

        val density = context.resources.displayMetrics.density
        val dp = { value: Int -> (value * density + 0.5f).toInt() }

        // ── Root: full-screen dark scrim ──
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(153, 0, 0, 0))   // 60% black scrim
            isClickable = true      // swallow touches on the scrim
            isFocusable = true
        }

        // ── Bottom sheet card ──
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(ROYAL_PURPLE)
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),   // top-left
                    dp(24).toFloat(), dp(24).toFloat(),   // top-right
                    0f, 0f,                                // bottom-right
                    0f, 0f,                                // bottom-left
                )
            }
            setPadding(dp(24), dp(10), dp(24), dp(28))
        }

        // ── Drag handle ──
        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(184, 255, 255, 255))   // ~72 % white
                cornerRadius = dp(999).toFloat()
            }
        }
        sheet.addView(
            handle,
            LinearLayout.LayoutParams(dp(44), dp(4)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.topMargin = dp(0)
                it.bottomMargin = dp(18)
            },
        )

        // ── Shield circle ──
        val shieldCircle = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(41, 255, 255, 255))   // 16 % white
            }
        }
        val shieldEmoji = TextView(context).apply {
            text = "🛡️"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
            gravity = Gravity.CENTER
        }
        shieldCircle.addView(
            shieldEmoji,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )
        sheet.addView(
            shieldCircle,
            LinearLayout.LayoutParams(dp(76), dp(76)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        // ── Title ──
        val title = TextView(context).apply {
            text = content.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
        }
        sheet.addView(title)

        // ── Subtitle ──
        val subtitle = TextView(context).apply {
            text = content.subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.argb(209, 255, 255, 255))  // ~82 % white
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), dp(22))
        }
        sheet.addView(subtitle)

        // ── "I'll Control Myself." button ──
        val button = TextView(context).apply {
            text = content.buttonText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(48), dp(15), dp(48), dp(15))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.argb(140, 255, 255, 255))  // ~55 % white border
                cornerRadius = dp(999).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val action = content.onAction
                dismiss()
                action()
            }
        }
        sheet.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // ── Quick Unlock ──
        if (content.quickUnlockMinutes.isNotEmpty()) {
            val quickUnlockLabel = TextView(context).apply {
                text = "Quick Unlock"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(8))
            }
            sheet.addView(quickUnlockLabel)
            
            val quickUnlockRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            content.quickUnlockMinutes.forEach { minutes ->
                val minButton = TextView(context).apply {
                    text = "$minutes min"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    background = GradientDrawable().apply {
                        setColor(QUICK_UNLOCK_PURPLE)
                        cornerRadius = dp(999).toFloat()
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val graceUntilMs = System.currentTimeMillis() + (minutes * 60 * 1000L)
                        FocusShieldRepository.ShieldPrefs.applyEmergencyUnlock(
                            context,
                            graceUntilMs,
                            minutes,
                            packageName = currentBlockedPackage,
                            origin = currentQuickUnlockOrigin,
                        )
                        runCatching {
                            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                context.applicationContext, FocusShieldEntryPoint::class.java)
                            currentBlockedPackage?.let { pkg ->
                                entryPoint.focusShieldRepository().recordQuickUnlock(pkg, minutes)
                            }
                        }
                        dismiss()
                        goHome()
                    }
                }
                quickUnlockRow.addView(
                    minButton,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    }
                )
            }
            sheet.addView(
                quickUnlockRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        // ── Anchor sheet to bottom with dynamic 3-button vs gesture nav padding ──
        val baseBottomPadding = dp(28)
        val fallbackNavHeight = if (!isGestureNavigation(context)) getNavigationBarHeight(context) else 0
        sheet.setPadding(dp(24), dp(10), dp(24), baseBottomPadding + fallbackNavHeight)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = if (navBars.bottom > 0) navBars.bottom else fallbackNavHeight
            sheet.setPadding(dp(24), dp(10), dp(24), baseBottomPadding + bottomInset)
            insets
        }

        root.addView(
            sheet,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        return root
    }

    private fun isGestureNavigation(ctx: Context): Boolean = runCatching {
        Settings.Secure.getInt(ctx.contentResolver, "navigation_mode", 0) == 2
    }.getOrDefault(false)

    private fun getNavigationBarHeight(ctx: Context): Int {
        val resId = ctx.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resId > 0) ctx.resources.getDimensionPixelSize(resId) else 0
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.let { info ->
            homeIntent.component = ComponentName(info.packageName, info.name)
        }
        runCatching { context.startActivity(homeIntent) }
    }

    companion object {
        /** Rich Deep Royal Purple (Purple 900) - Hex Code: #581C87 */
        private val ROYAL_PURPLE = Color.parseColor("#581C87")
        /** Slightly lighter purple for Quick Unlock buttons (Purple 700: #7E22CE) */
        private val QUICK_UNLOCK_PURPLE = Color.parseColor("#7E22CE")
    }

    private data class OverlayContent(
        val title: String,
        val subtitle: String,
        val buttonText: String,
        val onAction: () -> Unit,
        val quickUnlockMinutes: List<Int> = emptyList(),
    )
}
