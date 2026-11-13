package com.safarparmar.app.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlin.math.hypot
import kotlin.math.max

/** Captures once before a user toggle, then uncovers the new theme with one expanding circle. */
@Composable
internal fun RadialThemeReveal(viewModel: ThemeViewModel, content: @Composable () -> Unit) {
    val view = LocalView.current
    var previous by remember { mutableStateOf<ImageBitmap?>(null) }
    var touch by remember { mutableStateOf<Offset?>(null) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var generation by remember { mutableIntStateOf(0) }
    var runningGeneration by remember { mutableIntStateOf(-1) }
    val radius = remember { Animatable(0f) }
    DisposableEffect(viewModel, view) {
        val capture: () -> Unit = {
            val enabled = Settings.Global.getFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            if (enabled && view.width > 0 && view.height > 0) {
                previous = runCatching {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(AndroidCanvas(bitmap))
                    bitmap.asImageBitmap()
                }.getOrNull()
                center = touch ?: Offset(view.width * 0.9f, view.height * 0.07f)
                generation++
            } else previous = null
        }
        viewModel.onBeforeThemeToggle = capture
        onDispose { if (viewModel.onBeforeThemeToggle === capture) viewModel.onBeforeThemeToggle = null }
    }
    LaunchedEffect(generation) {
        if (previous != null) {
            radius.snapTo(0f)
            runningGeneration = generation
            withFrameNanos { }
            radius.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            previous = null
        }
    }
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull()?.let { touch = it.position }
            }
        }
    }) {
        content()
        previous?.let { bitmap ->
            Canvas(Modifier.matchParentSize()) {
                val farX = max(center.x, size.width - center.x)
                val farY = max(center.y, size.height - center.y)
                val hole = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(center, hypot(farX, farY) * if (runningGeneration == generation) radius.value else 0f))
                }
                clipPath(hole, ClipOp.Difference) { drawImage(bitmap) }
            }
        }
    }
}
