package com.safarparmar.app.ui.drawer

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/** Bounds belong to the activity window, never the drawer or the rail column. */
internal class SidebarMenuPositionProvider(
    private val compact: Boolean,
    private val margin: Int,
    private val gap: Int,
    private val topInset: Int,
    private val bottomInset: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val desiredX = if (compact) {
            if (ltr) anchorBounds.right + gap else anchorBounds.left - gap - popupContentSize.width
        } else {
            // Overlap the trailing half of the row and extend over the scrim.
            val overlapStart = (anchorBounds.width * .42f).roundToInt()
            if (ltr) anchorBounds.left + overlapStart
            else anchorBounds.right - overlapStart - popupContentSize.width
        }
        val minX = margin
        val maxX = (windowSize.width - margin - popupContentSize.width).coerceAtLeast(minX)
        val minY = topInset + margin
        val maxY = (windowSize.height - bottomInset - margin - popupContentSize.height).coerceAtLeast(minY)
        val desiredY = if (compact) anchorBounds.top else {
            val below = anchorBounds.bottom - anchorBounds.height / 3
            if (below + popupContentSize.height <= windowSize.height - bottomInset - margin) below
            else anchorBounds.top + anchorBounds.height / 3 - popupContentSize.height
        }
        return IntOffset(desiredX.coerceIn(minX, maxX), desiredY.coerceIn(minY, maxY))
    }
}
