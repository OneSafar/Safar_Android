package com.safarparmar.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A content-sized row for buttons, text cards and detail/action pairs. If their
 * preferred widths cannot fit, it becomes a full-width column. Preferred widths
 * include the actual font scale, translated labels, icons and component padding.
 *
 * Children must support intrinsic measurement (ordinary Compose layouts and
 * Composables UI buttons do). Lazy lists and BoxWithConstraints belong outside
 * this layout. Place the group in scrolling content when height is limited.
 */
@Composable
fun SafarAdaptiveRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    equalWidth: Boolean = true,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { children, constraints ->
        if (children.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val gap = spacing.roundToPx()
            val preferred = children.map { it.maxIntrinsicWidth(Constraints.Infinity) }
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else
                constraints.constrainWidth((preferred.sumOf { it.toLong() } + gap.toLong() * (children.size - 1))
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            val horizontal = adaptiveRowFits(preferred, width, gap, equalWidth)
            val widths = if (!horizontal) List(children.size) { width } else if (equalWidth) {
                val available = width - gap * (children.size - 1)
                List(children.size) { index -> available / children.size + if (index < available % children.size) 1 else 0 }
            } else {
                val extra = width - preferred.sum() - gap * (children.size - 1)
                preferred.mapIndexed { index, value -> value + if (index == 0) extra else 0 }
            }
            val placeables = children.mapIndexed { index, child ->
                child.measure(Constraints(minWidth = widths[index], maxWidth = widths[index],
                    maxHeight = constraints.maxHeight))
            }
            val height = if (horizontal) placeables.maxOf { it.height } else
                placeables.sumOf { it.height } + gap * (placeables.size - 1)
            layout(width, constraints.constrainHeight(height)) {
                var offset = 0
                placeables.forEach { child ->
                    if (horizontal) {
                        child.placeRelative(offset, (height - child.height) / 2)
                        offset += child.width + gap
                    } else {
                        child.placeRelative(0, offset)
                        offset += child.height + gap
                    }
                }
            }
        }
    }
}

internal fun adaptiveRowFits(widths: List<Int>, available: Int, gap: Int, equalWidth: Boolean): Boolean {
    if (widths.isEmpty()) return true
    val content = if (equalWidth) widths.max().toLong() * widths.size else widths.sumOf { it.toLong() }
    return content + gap.toLong() * (widths.size - 1) <= available
}

/** Measures the bottom controls before allocating the remaining space to content. */
@Composable
fun SafarContentWithBottomBar(
    modifier: Modifier = Modifier,
    bottomBar: @Composable ColumnScope.() -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Column(modifier) {
        content(Modifier.weight(1f).fillMaxWidth())
        bottomBar()
    }
}

/** Keeps tab labels at their chosen font size and lets the strip scroll to fit. */
@Composable
fun SafarScrollableTabRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        content = content)
}
