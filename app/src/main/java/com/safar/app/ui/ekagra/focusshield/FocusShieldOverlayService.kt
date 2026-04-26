package com.safar.app.ui.ekagra.focusshield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.safar.app.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that polls UsageStatsManager to detect when a blocked
 * app is in the foreground and draws a full-screen overlay to block it.
 *
 * This is the same approach used by Regain and similar focus apps.
 * Requires:
 * - PACKAGE_USAGE_STATS permission (Usage Access)
 * - SYSTEM_ALERT_WINDOW permission (Display over other apps)
 */
class FocusShieldOverlayService : Service() {

    companion object {
        private const val TAG = "FocusShieldA11y"
        private const val CHANNEL_ID = "focus_shield_overlay"
        private const val NOTIFICATION_ID = 7891
        private const val POLL_INTERVAL_MS = 500L

        fun start(context: Context) {
            val intent = Intent(context, FocusShieldOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.w(TAG, "OverlayService → start requested")
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusShieldOverlayService::class.java)
            context.stopService(intent)
            Log.w(TAG, "OverlayService → stop requested")
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var isOverlayShowing = false
    private var currentBlockedPkg: String? = null
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.w(TAG, "OverlayService → onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.w(TAG, "OverlayService → onStartCommand")
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) {
            Log.w(TAG, "OverlayService polling already active")
            return
        }

        pollingJob = serviceScope.launch {
            Log.w(TAG, "OverlayService → polling started")
            while (isActive) {
                try {
                    val active = FocusShieldRepository.ShieldPrefs.isActive(this@FocusShieldOverlayService)
                    if (!active) {
                        Log.w(TAG, "OverlayService → session ended, stopping self")
                        hideOverlay()
                        stopSelf()
                        break
                    }

                    val foregroundPkg = getForegroundPackage()
                    val blockedPkgs = FocusShieldRepository.ShieldPrefs.getPackages(this@FocusShieldOverlayService)

                    if (foregroundPkg != null && foregroundPkg in blockedPkgs) {
                        if (!isOverlayShowing || currentBlockedPkg != foregroundPkg) {
                            Log.w(TAG, "OverlayService → 🛡️ BLOCKING: $foregroundPkg")
                            showOverlay(foregroundPkg)
                        }
                    } else {
                        if (isOverlayShowing) {
                            Log.w(TAG, "OverlayService → hiding overlay (fg=$foregroundPkg)")
                            hideOverlay()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OverlayService → poll error", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 5_000 // last 5 seconds

            val usageEvents = usm.queryEvents(beginTime, endTime)
            var lastPkg: String? = null
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    lastPkg = event.packageName
                }
            }

            lastPkg ?: usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                endTime - 60_000,
                endTime,
            )
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "getForegroundPackage failed", e)
            null
        }
    }

    private fun showOverlay(blockedPackageName: String) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "OverlayService → CANNOT draw overlays! Permission missing.")
            return
        }

        hideOverlay() // Remove any existing overlay first

        currentBlockedPkg = blockedPackageName
        val appName = try {
            val ai = packageManager.getApplicationInfo(blockedPackageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            blockedPackageName
        }

        // Build overlay view programmatically
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0xF0101010.toInt()) // near-black with slight transparency
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        // App icon
        try {
            val appIcon = packageManager.getApplicationIcon(blockedPackageName)
            val iconView = ImageView(this).apply {
                setImageDrawable(appIcon)
                alpha = 0.5f
            }
            val iconParams = LinearLayout.LayoutParams(160, 160).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 32
            }
            container.addView(iconView, iconParams)
        } catch (_: Exception) {}

        // Block icon (🚫)
        val blockSymbol = TextView(this).apply {
            text = "🛡️"
            textSize = 48f
            gravity = Gravity.CENTER
        }
        val blockParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 48
        }
        container.addView(blockSymbol, blockParams)

        // Title
        val titleView = TextView(this).apply {
            text = "$appName is blocked\nduring focus"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 24
        }
        container.addView(titleView, titleParams)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "Stay focused! This app is blocked until your focus session ends."
            textSize = 14f
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val subParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 64
        }
        container.addView(subtitleView, subParams)

        // "Go back to Safar" button
        val button = TextView(this).apply {
            text = "← Go back to Safar"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(64, 32, 64, 32)
            setOnClickListener {
                val launchIntent = Intent(this@FocusShieldOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_NAVIGATE_EKAGRA, true)
                }
                startActivity(launchIntent)
                hideOverlay()
            }
        }
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(button, buttonParams)

        val containerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        overlay.addView(container, containerParams)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        // Make the button clickable by clearing NOT_FOCUSABLE for touch events
        layoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        try {
            windowManager?.addView(overlay, layoutParams)
            overlayView = overlay
            isOverlayShowing = true
            Log.w(TAG, "OverlayService → overlay SHOWN for $appName")

            // Also emit for snackbar when user returns to Safar
            BlockerEventBridge.emit(BlockerEventBridge.BlockedEvent(blockedPackageName, appName))
        } catch (e: Exception) {
            Log.e(TAG, "OverlayService → failed to show overlay", e)
        }
    }

    private fun hideOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {}
        overlayView = null
        isOverlayShowing = false
        currentBlockedPkg = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Shield",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Focus Shield is protecting your focus session"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Shield Active")
            .setContentText("Blocking distracting apps during your session")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        hideOverlay()
        serviceScope.cancel()
        Log.w(TAG, "OverlayService → onDestroy")
        super.onDestroy()
    }
}
