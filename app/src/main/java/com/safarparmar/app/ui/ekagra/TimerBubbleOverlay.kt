package com.safarparmar.app.ui.ekagra

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    themeAccent: Color? = null,
    onPlayPause: () -> Unit,
    onCollapse: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = if (kavachActive) Color(0xFF4ADE80) else (themeAccent ?: Color(0xFF7C6FF7))
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
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
    themeAccent: Color? = null,
    onExpand: () -> Unit,
) {
    val accent = if (kavachActive) Color(0xFF4ADE80) else (themeAccent ?: Color(0xFF7C6FF7))
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
    var themeAccent  by mutableStateOf<Color?>(null)

    // Wired up by the manager so the view can drive window position + actions.
    var onWindowTouchStart: () -> Unit = {}
    var onWindowDrag: (Float, Float) -> Unit = { _, _ -> }
    var onWindowDragEnd: () -> Unit = {}
    var onPlayPause: () -> Unit = {}
    var onOpen: () -> Unit = {}
    var onCollapsedChanged: () -> Unit = {}

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var initialRawX = 0f
    private var initialRawY = 0f
    private var isDragging = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialRawX = ev.rawX
                initialRawY = ev.rawY
                isDragging = false
                onWindowTouchStart()
                super.dispatchTouchEvent(ev)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - initialRawX
                val dy = ev.rawY - initialRawY
                if (!isDragging && kotlin.math.hypot(dx, dy) > touchSlop) {
                    isDragging = true
                }
                if (isDragging) {
                    onWindowDrag(dx, dy)
                    // Drag-to-expand: if collapsed and user pulls inward from screen edge (> 24dp)
                    if (collapsed) {
                        val inwardDrag = if (snappedRight) -dx else dx
                        if (inwardDrag > 24 * context.resources.displayMetrics.density) {
                            collapsed = false
                            onCollapsedChanged()
                        }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    onWindowDragEnd()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @Composable
    override fun Content() {
        val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f

        Box {
            if (collapsed) {
                CollapsedTab(
                    kavachActive = kavachActive,
                    snappedRight = snappedRight,
                    themeAccent = themeAccent,
                    onExpand = {
                        collapsed = false
                        onCollapsedChanged()
                    },
                )
            } else {
                ExpandedPill(
                    secondsLeft = secondsLeft,
                    progress = progress,
                    kavachActive = kavachActive,
                    isRunning = isRunning,
                    snappedRight = snappedRight,
                    themeAccent = themeAccent,
                    onPlayPause = onPlayPause,
                    onCollapse = {
                        collapsed = true
                        onCollapsedChanged()
                    },
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
    private var snapAnimator: ValueAnimator? = null

    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = context.resources.displayMetrics
            Pair(dm.widthPixels, dm.heightPixels)
        }
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
        snapAnimator?.cancel()
        snapAnimator = null
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
        lifecycleOwner = loNull()
        params         = null
    }

    private fun loNull(): BubbleLifecycleOwner? = null

    private fun snapToNearestEdge(
        view: BubbleComposeView,
        wm: WindowManager,
        p: WindowManager.LayoutParams,
        context: Context
    ) {
        snapAnimator?.cancel()
        val (screenW, screenH) = getScreenDimensions(context)
        val maxX = (screenW - view.width).coerceAtLeast(0)
        val maxY = (screenH - view.height).coerceAtLeast(0)

        val center = p.x + view.width / 2
        val toRight = center >= screenW / 2
        val targetX = if (toRight) maxX else 0
        val targetY = p.y.coerceIn(0, maxY)

        view.snappedRight = toRight

        val startX = p.x
        if (startX == targetX) {
            p.y = targetY
            currentX = p.x
            currentY = p.y
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
            return
        }

        snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val currentP = params ?: return@addUpdateListener
                currentP.x = anim.animatedValue as Int
                currentP.y = targetY
                currentX = currentP.x
                currentY = currentP.y
                try { wm.updateViewLayout(view, currentP) } catch (_: Exception) {}
            }
        }
        snapAnimator?.start()
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
        val (screenW, screenH) = getScreenDimensions(context)

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

        var initialXOnDrag = 0
        var initialYOnDrag = 0

        view.onWindowTouchStart = {
            snapAnimator?.cancel()
            val p = params
            if (p != null) {
                initialXOnDrag = p.x
                initialYOnDrag = p.y
            }
        }

        view.onWindowDrag = fun(dx: Float, dy: Float) {
            val p = params ?: return
            val (curScreenW, curScreenH) = getScreenDimensions(context)
            val maxX = (curScreenW - view.width).coerceAtLeast(0)
            val maxY = (curScreenH - view.height).coerceAtLeast(0)

            p.x = (initialXOnDrag + dx.toInt()).coerceIn(0, maxX)
            p.y = (initialYOnDrag + dy.toInt()).coerceIn(0, maxY)
            currentX = p.x
            currentY = p.y
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
        }

        view.onWindowDragEnd = fun() {
            val p = params ?: return
            snapToNearestEdge(view, wm, p, context)
        }

        view.onCollapsedChanged = fun() {
            val p = params ?: return
            view.post {
                val (curScreenW, curScreenH) = getScreenDimensions(context)
                val maxX = (curScreenW - view.width).coerceAtLeast(0)
                val maxY = (curScreenH - view.height).coerceAtLeast(0)
                p.x = if (view.snappedRight) maxX else 0
                p.y = p.y.coerceIn(0, maxY)
                currentX = p.x
                currentY = p.y
                try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
            }
        }

        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val newW = right - left
            val newH = bottom - top
            val oldW = oldRight - oldLeft
            val oldH = oldBottom - oldTop
            if (newW > 0 && newH > 0 && (newW != oldW || newH != oldH)) {
                val p = params ?: return@addOnLayoutChangeListener
                val (curScreenW, curScreenH) = getScreenDimensions(context)
                val maxX = (curScreenW - newW).coerceAtLeast(0)
                val maxY = (curScreenH - newH).coerceAtLeast(0)
                p.x = if (view.snappedRight) maxX else 0
                p.y = p.y.coerceIn(0, maxY)
                currentX = p.x
                currentY = p.y
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

        view.post {
            val p = params ?: return@post
            val (curScreenW, curScreenH) = getScreenDimensions(context)
            val maxX = (curScreenW - view.width).coerceAtLeast(0)
            val maxY = (curScreenH - view.height).coerceAtLeast(0)
            p.x = if (view.snappedRight) maxX else 0
            p.y = p.y.coerceIn(0, maxY)
            currentX = p.x
            currentY = p.y
            try { wm.updateViewLayout(view, p) } catch (_: Exception) {}
        }
    }
}

