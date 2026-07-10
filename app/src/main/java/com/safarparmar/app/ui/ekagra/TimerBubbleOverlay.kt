package com.safarparmar.app.ui.ekagra

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

// ─── Pill Compose content ─────────────────────────────────────────────────────

private val PillBg = Color(0xF0121417)

@Composable
private fun ExpandedPill(
    secondsLeft: Int,
    progress: Float,
    kavachActive: Boolean,
    isRunning: Boolean,
    snappedRight: Boolean,
    onPlayPause: () -> Unit,
    onCollapse: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = if (kavachActive) Color(0xFF4ADE80) else Color(0xFF7C6FF7)
    // Fully rounded capsule.
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(PillBg)
            .border(1.dp, accent.copy(alpha = 0.35f), shape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Play / Pause button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Time + label (tap → open Ekagra)
            Column(
                modifier = Modifier.clickable(onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (kavachActive) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }
                Text(
                    text = if (isRunning) "Time remaining" else "Paused",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }

            Spacer(Modifier.width(2.dp))

            // Collapse button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onCollapse),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Collapse",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Slim progress bar pinned to the bottom edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun CollapsedTab(
    kavachActive: Boolean,
    snappedRight: Boolean,
    onExpand: () -> Unit,
) {
    val accent = if (kavachActive) Color(0xFF4ADE80) else Color(0xFF7C6FF7)
    // Rounded on the inner side, flat toward the screen edge it docks against.
    val shape = if (snappedRight)
        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
    else
        RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 48.dp)
            .clip(shape)
            .background(PillBg)
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .clickable(onClick = onExpand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = "Expand timer",
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ─── Android View wrapper ─────────────────────────────────────────────────────

private class BubbleComposeView(context: Context) : AbstractComposeView(context) {

    var secondsLeft  by mutableIntStateOf(0)
    var totalSeconds by mutableIntStateOf(1)
    var kavachActive by mutableStateOf(false)
    var isRunning    by mutableStateOf(true)
    var snappedRight by mutableStateOf(true)
    var collapsed    by mutableStateOf(false)

    // Wired up by the manager so the composable can drive window position + actions.
    var onDrag:     (Float, Float) -> Unit = { _, _ -> }
    var onDragEnd:  () -> Unit = {}
    var onPlayPause: () -> Unit = {}
    var onOpen:     () -> Unit = {}
    var onCollapsedChanged: () -> Unit = {}

    @Composable
    override fun Content() {
        val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f

        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                )
            },
        ) {
            if (collapsed) {
                CollapsedTab(
                    kavachActive = kavachActive,
                    snappedRight = snappedRight,
                    onExpand = { collapsed = false; onCollapsedChanged() },
                )
            } else {
                ExpandedPill(
                    secondsLeft = secondsLeft,
                    progress = progress,
                    kavachActive = kavachActive,
                    isRunning = isRunning,
                    snappedRight = snappedRight,
                    onPlayPause = onPlayPause,
                    onCollapse = { collapsed = true; onCollapsedChanged() },
                    onOpen = onOpen,
                )
            }
        }
    }
}

// ─── Lifecycle + SavedState stubs required by AbstractComposeView ─────────────

private class BubbleLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry      = LifecycleRegistry(this)
    private val savedStateController   = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle  get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate()  { savedStateController.performRestore(null); lifecycleRegistry.currentState = Lifecycle.State.CREATED }
    fun onResume()  { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
    fun onDestroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
}

// ─── Public singleton manager ─────────────────────────────────────────────────

object TimerBubbleOverlay {

    private var windowManager: WindowManager?     = null
    private var bubbleView:    BubbleComposeView? = null
    private var lifecycleOwner: BubbleLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private var attached = false
    private var currentX = -1
    private var currentY = -1

    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Shows or refreshes the floating pill. Safe to call repeatedly each tick. */
    fun show(
        context: Context,
        secondsLeft: Int,
        totalSeconds: Int,
        kavachActive: Boolean,
        isRunning: Boolean = true,
    ) {
        if (!canDrawOverlays(context)) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (!attached) {
            attach(context, wm, secondsLeft, totalSeconds, kavachActive, isRunning)
        } else {
            update(secondsLeft, totalSeconds, kavachActive, isRunning)
        }
    }

    fun update(secondsLeft: Int, totalSeconds: Int, kavachActive: Boolean, isRunning: Boolean = true) {
        val view = bubbleView ?: return
        view.secondsLeft  = secondsLeft
        view.totalSeconds = totalSeconds.coerceAtLeast(1)
        view.kavachActive = kavachActive
        view.isRunning    = isRunning
    }

    fun hide() {
        val wm   = windowManager ?: return
        val view = bubbleView    ?: return
        if (!attached) return
        try {
            lifecycleOwner?.onDestroy()
            wm.removeViewImmediate(view)
        } catch (_: Exception) {}
        attached       = false
        bubbleView     = null
        windowManager  = null
        lifecycleOwner = null
        params         = null
    }

    // ── Private attach ────────────────────────────────────────────────────────

    private fun attach(
        context: Context,
        wm: WindowManager,
        secondsLeft: Int,
        totalSeconds: Int,
        kavachActive: Boolean,
        isRunning: Boolean,
    ) {
        val density = context.resources.displayMetrics.density
        val screenH = context.resources.displayMetrics.heightPixels
        val screenW = context.resources.displayMetrics.widthPixels

        if (currentY < 0) currentY = screenH / 3
        if (currentX < 0) currentX = screenW  // start docked right

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX
            y = currentY
        }

        val lo = BubbleLifecycleOwner().also { it.onCreate(); it.onResume() }

        val view = BubbleComposeView(context).apply {
            this.secondsLeft  = secondsLeft
            this.totalSeconds = totalSeconds.coerceAtLeast(1)
            this.kavachActive = kavachActive
            this.isRunning    = isRunning
            this.snappedRight = currentX > screenW / 2
            setViewTreeLifecycleOwner(lo)
            setViewTreeSavedStateRegistryOwner(lo)
        }

        val maxY = (screenH - 60 * density).toInt()

        view.onDrag = fun(dx: Float, dy: Float) {
            val p = params ?: return
            val maxX = (screenW - view.width).coerceAtLeast(0)
            p.x = (p.x + dx.toInt()).coerceIn(0, maxX)
            p.y = (p.y + dy.toInt()).coerceIn(0, maxY)
            currentX = p.x
            currentY = p.y
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
        }
        view.onDragEnd = fun() {
            val p = params ?: return
            // Snap to nearest horizontal edge, keep vertical position.
            val center = p.x + view.width / 2
            val toRight = center >= screenW / 2
            p.x = if (toRight) (screenW - view.width).coerceAtLeast(0) else 0
            currentX = p.x
            view.snappedRight = toRight
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
        }
        view.onCollapsedChanged = fun() {
            // Re-snap after size change so the tab/pill stays flush against its edge.
            val p = params ?: return
            view.post {
                val maxX = (screenW - view.width).coerceAtLeast(0)
                p.x = if (view.snappedRight) maxX else 0
                p.y = p.y.coerceIn(0, maxY)
                currentX = p.x
                try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
            }
        }
        view.onPlayPause = {
            try {
                val intent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_PLAY_PAUSE
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
        view.onOpen = {
            val intent = Intent(context, com.safarparmar.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(com.safarparmar.app.MainActivity.EXTRA_NAVIGATE_EKAGRA, true)
            }
            context.startActivity(intent)
        }

        windowManager  = wm
        bubbleView     = view
        lifecycleOwner = lo
        params         = lp
        attached       = true

        try { wm.addView(view, lp) } catch (_: Exception) { attached = false }

        // The pill's width isn't known until it lays out. Snap it flush against its docked
        // edge once measured — otherwise a right-dock with x = screenW sits fully off-screen.
        view.post {
            val p = params ?: return@post
            val maxX = (screenW - view.width).coerceAtLeast(0)
            p.x = if (view.snappedRight) maxX else 0
            p.y = p.y.coerceIn(0, maxY)
            currentX = p.x
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
        }
    }
}
