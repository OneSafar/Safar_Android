package com.safar.app.ui.butterfly

/**
 * A single stop in the butterfly guided tour.
 *
 * @param title       Bold label shown in the tooltip (e.g. "Step 1 / 4")
 * @param message     Body text describing the UI area
 * @param anchorX     0f–1f  fraction of screen width  where butterfly lands
 * @param anchorY     0f–1f  fraction of screen height where butterfly lands
 * @param tooltipSide Which side the tooltip card pops out on (auto-adjusted near edges)
 */
data class ButterflyTourStep(
    val title: String,
    val message: String,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val tooltipSide: TooltipSide = TooltipSide.AUTO,
)

enum class TooltipSide { AUTO, LEFT, RIGHT, TOP, BOTTOM }
