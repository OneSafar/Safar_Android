/*
 * Copyright 2022-2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.safarparmar.app.ui.components.popups

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * M3-compatible menu with SAFAR's density restored inside the actual popup window.
 *
 * Keep Popup at the caller's layout anchor: routing through a hosting Dialog would
 * change its anchor bounds and break placement. Adapted from AndroidMenu/Menu and
 * MenuPosition in AndroidX Material3 1.4.0.
 */
@Composable
fun SafarDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    maximumWidth: Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    // Convert physical window bounds with the SAME density used by the menu.
    val viewportHeight = with(density) {
        view.rootView.height.takeIf { it > 0 }?.toDp() ?: configuration.screenHeightDp.dp
    }
    val maxMenuWidth = minOf(maximumWidth, LocalSafarPopupMaxWidth.current).coerceAtLeast(0.dp)
    val maxMenuHeight = (viewportHeight - MenuVerticalMargin * 2).coerceAtLeast(0.dp)
    val popupBounds = LocalSafarPopupBounds.current

    if (expandedState.currentState || expandedState.targetState) {
        val transformOrigin = remember { mutableStateOf(TransformOrigin.Center) }
        val positionProvider = remember(offset, density, popupBounds) {
            SafarDropdownMenuPositionProvider(offset, density, popupBounds) { anchor, menu ->
                transformOrigin.value = safarMenuTransformOrigin(anchor, menu)
            }
        }
        @Suppress("DEPRECATION")
        val transition = updateTransition(expandedState, label = "SafarDropdownMenu")
        // Material3 1.4's standard FastSpatial/FastEffects motion token values.
        val scale by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.9f, stiffness = 1400f) },
            label = "scale",
        ) { if (it) 1f else 0.8f }
        val alpha by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 1f, stiffness = 3800f) },
            label = "alpha",
        ) { if (it) 1f else 0f }
        val inspecting = LocalInspectionMode.current

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = positionProvider,
            properties = properties,
        ) {
            CompositionLocalProvider(LocalDensity provides density) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = maxMenuWidth)
                        .heightIn(max = maxMenuHeight)
                        .graphicsLayer {
                            scaleX = if (inspecting) { if (expandedState.targetState) 1f else 0.8f } else scale
                            scaleY = scaleX
                            this.alpha = if (inspecting) { if (expandedState.targetState) 1f else 0f } else alpha
                            this.transformOrigin = transformOrigin.value
                        },
                    shape = shape,
                    color = containerColor,
                    tonalElevation = tonalElevation,
                    shadowElevation = shadowElevation,
                    border = border,
                ) {
                    Column(
                        modifier = modifier
                            .padding(vertical = 8.dp)
                            .width(IntrinsicSize.Max)
                            .verticalScroll(scrollState),
                        content = content,
                    )
                }
            }
        }
    }
}

/** Read-only exposed menus keep M3's anchor/keyboard bounds and the same app scaling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.SafarExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    matchAnchorWidth: Boolean = true,
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SafarDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.exposedDropdownSize(matchAnchorWidth),
        scrollState = scrollState,
        properties = PopupProperties(focusable = true),
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        maximumWidth = LocalSafarPopupMaxWidth.current,
        content = content,
    )
}

private val MenuVerticalMargin = 48.dp

/** Material positioning uses physical anchors; only dp offsets/margins use app density. */
internal class SafarDropdownMenuPositionProvider(
    contentOffset: DpOffset,
    density: Density,
    private val popupBounds: IntRect? = null,
    private val onPositionCalculated: (anchorBounds: IntRect, menuBounds: IntRect) -> Unit = { _, _ -> },
) : PopupPositionProvider {
    private val offsetX = with(density) { contentOffset.x.roundToPx() }
    private val offsetY = with(density) { contentOffset.y.roundToPx() }
    private val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val menuWidth = popupContentSize.width
        val menuHeight = popupContentSize.height
        val regionLeft = popupBounds?.left?.coerceIn(0, windowSize.width) ?: 0
        val regionRight = popupBounds?.right?.coerceIn(regionLeft, windowSize.width) ?: windowSize.width
        val regionWidth = regionRight - regionLeft
        val ltr = layoutDirection == LayoutDirection.Ltr
        val resolvedOffsetX = if (ltr) offsetX else -offsetX
        val start = (if (ltr) anchorBounds.left else anchorBounds.right - menuWidth) + resolvedOffsetX
        val end = (if (ltr) anchorBounds.right - menuWidth else anchorBounds.left) + resolvedOffsetX
        val windowX = if (menuWidth >= regionWidth) {
            regionLeft + ((regionWidth - menuWidth) / 2f).roundToInt()
        } else if (anchorBounds.center.x < regionLeft + regionWidth / 2) regionLeft else regionRight - menuWidth
        val x = listOf(start, end).firstOrNull { it >= regionLeft && it + menuWidth <= regionRight } ?: windowX

        val below = anchorBounds.bottom + offsetY
        val above = anchorBounds.top - menuHeight + offsetY
        val centered = anchorBounds.top - (menuHeight / 2f).roundToInt() + offsetY
        val windowY = if (menuHeight >= windowSize.height - verticalMargin * 2) {
            ((windowSize.height - menuHeight) / 2f).roundToInt()
        } else if (anchorBounds.center.y < windowSize.height / 2) {
            verticalMargin
        } else windowSize.height - verticalMargin - menuHeight
        val y = listOf(below, above, centered).firstOrNull {
            it >= verticalMargin && it + menuHeight <= windowSize.height - verticalMargin
        } ?: windowY
        return IntOffset(x, y).also {
            onPositionCalculated(anchorBounds, IntRect(offset = it, size = popupContentSize))
        }
    }
}

internal fun safarMenuTransformOrigin(anchorBounds: IntRect, menuBounds: IntRect): TransformOrigin {
    val pivotX = when {
        menuBounds.left >= anchorBounds.right -> 0f
        menuBounds.right <= anchorBounds.left -> 1f
        menuBounds.width == 0 -> 0f
        else -> ((max(anchorBounds.left, menuBounds.left) + min(anchorBounds.right, menuBounds.right)) / 2 - menuBounds.left).toFloat() / menuBounds.width
    }
    val pivotY = when {
        menuBounds.top >= anchorBounds.bottom -> 0f
        menuBounds.bottom <= anchorBounds.top -> 1f
        menuBounds.height == 0 -> 0f
        else -> ((max(anchorBounds.top, menuBounds.top) + min(anchorBounds.bottom, menuBounds.bottom)) / 2 - menuBounds.top).toFloat() / menuBounds.height
    }
    return TransformOrigin(pivotX, pivotY)
}
