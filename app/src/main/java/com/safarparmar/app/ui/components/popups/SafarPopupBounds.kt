package com.safarparmar.app.ui.components.popups

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

/** Available app column width, rather than the full tablet window behind it. */
val LocalSafarPopupMaxWidth = staticCompositionLocalOf<Dp> { 640.dp }

/** Physical pixels relative to the activity window, matching PopupPositionProvider coordinates. */
val LocalSafarPopupBounds = staticCompositionLocalOf<IntRect?> { null }

/** Publishes the actual app canvas and hosts tooltips without replacing native modal windows. */
@androidx.compose.runtime.Composable
fun SafarPopupViewport(content: @androidx.compose.runtime.Composable () -> Unit) {
    var bounds by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<IntRect?>(null)
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val rect = coordinates.boundsInWindow()
                bounds = IntRect(rect.left.roundToInt(), rect.top.roundToInt(),
                    rect.right.roundToInt(), rect.bottom.roundToInt())
            },
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalSafarPopupMaxWidth provides maxWidth,
            LocalSafarPopupBounds provides bounds,
        ) {
            com.composeunstyled.TooltipHost(androidx.compose.ui.Modifier.fillMaxSize(), content)
        }
    }
}

/** Constrains a library's floating panel while retaining its original anchor and modal handling. */
@androidx.compose.runtime.Composable
internal fun SafarAnchoredPopupContent(
    maximumWidth: Dp = LocalSafarPopupMaxWidth.current,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val viewport = LocalSafarPopupBounds.current
    var horizontalCorrection by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    SafarPopupContent {
        // Measure the unshifted outer box. Measuring the shifted child instead
        // would alternate between the correction and zero on successive frames.
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier
                .widthIn(max = minOf(maximumWidth, LocalSafarPopupMaxWidth.current))
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow()
                    horizontalCorrection = popupHorizontalCorrection(
                        position.x.roundToInt(), coordinates.size.width, viewport,
                    )
                },
        ) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.offset {
                    androidx.compose.ui.unit.IntOffset(horizontalCorrection, 0)
                },
            ) { content() }
        }
    }
}

internal fun popupHorizontalCorrection(left: Int, width: Int, viewport: IntRect?): Int {
    if (viewport == null) return 0
    val lastLeft = (viewport.right - width).coerceAtLeast(viewport.left)
    return left.coerceIn(viewport.left, lastLeft) - left
}
