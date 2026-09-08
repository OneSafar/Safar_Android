package com.safarparmar.app.performance

import androidx.compose.runtime.*

/** One frame callback for all visible shimmer elements; values are read only during drawing. */
internal class DecorationClock {
    var consumers by mutableIntStateOf(0)
    var frameMillis by mutableLongStateOf(0L)
}
internal val LocalDecorationClock = staticCompositionLocalOf<DecorationClock?> { null }

@Composable
internal fun ProvideDecorationClock(content: @Composable () -> Unit) {
    val clock = remember { DecorationClock() }
    val running = clock.consumers > 0
    LaunchedEffect(running) {
        if (running) {
            while (true) withFrameNanos { clock.frameMillis = it / 1_000_000L }
        }
    }
    CompositionLocalProvider(LocalDecorationClock provides clock, content = content)
}

@Composable
fun rememberDecorationPhase(durationMillis: Int): () -> Float {
    val enabled = decorativeMotionEnabled()
    val clock = LocalDecorationClock.current
    DisposableEffect(enabled, clock) {
        if (enabled && clock != null) clock.consumers++
        onDispose { if (enabled && clock != null) clock.consumers-- }
    }
    return remember(enabled, clock, durationMillis) {
        { if (!enabled || clock == null) 0f else (clock.frameMillis % durationMillis.coerceAtLeast(1)).toFloat() / durationMillis.coerceAtLeast(1) }
    }
}
