package com.safarparmar.app.ui.components.popups

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.ui.components.BottomSheetState
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuScope
import com.composables.ui.components.DropdownMenuSide
import com.composables.ui.components.TooltipAlignment
import com.composables.ui.components.TooltipScope
import com.composables.ui.components.TooltipSide
import com.composables.ui.theme.colors
import com.composables.ui.theme.dialogShape
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.overlayShadow
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.shadows
import com.composables.ui.theme.shapes
import com.composables.ui.theme.sheetShape
import com.composeunstyled.theme.Theme
import com.composables.ui.components.AlertDialog as LibraryAlertDialog
import com.composables.ui.components.BottomSheet as LibraryBottomSheet
import com.composables.ui.components.DropdownMenu as LibraryDropdownMenu
import com.composables.ui.components.Tooltip as LibraryTooltip

/** Preserves the Composables UI 0.2.0 API and theme while isolating native popup resources. */
@Composable
fun SafarComposablesAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    neutralButton: (@Composable () -> Unit)? = null,
    negativeButton: (@Composable () -> Unit)? = null,
    shape: Shape = Theme[shapes][dialogShape],
    backgroundColor: Color = Theme[colors][panelColor],
    contentColor: Color = Theme[colors][onPanelColor],
    supportingTextColor: Color = Theme[colors][mutedColor],
    shadow: Shadow = Theme[shadows][overlayShadow],
) {
    val maxWidth = LocalSafarPopupMaxWidth.current
    SafarPopupEnvironment {
        LibraryAlertDialog(
            visible = visible,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.widthIn(max = maxWidth).then(modifier),
            icon = icon?.let { slot -> { SafarPopupContent { slot() } } },
            title = title?.let { slot -> { SafarPopupContent { slot() } } },
            text = { SafarPopupContent { text() } },
            positiveButton = { SafarPopupContent { positiveButton() } },
            neutralButton = neutralButton?.let { slot -> { SafarPopupContent { slot() } } },
            negativeButton = negativeButton?.let { slot -> { SafarPopupContent { slot() } } },
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            supportingTextColor = supportingTextColor,
            shadow = shadow,
        )
    }
}

@Composable
fun SafarComposablesAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    paneTitle: String = "Alert dialog",
    shape: Shape = Theme[shapes][dialogShape],
    backgroundColor: Color = Theme[colors][panelColor],
    contentColor: Color = Theme[colors][onPanelColor],
    contentPadding: PaddingValues = PaddingValues(24.dp),
    shadow: Shadow = Theme[shadows][overlayShadow],
    content: @Composable () -> Unit,
) {
    val maxWidth = LocalSafarPopupMaxWidth.current
    SafarPopupEnvironment {
        LibraryAlertDialog(
            visible = visible,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.widthIn(max = maxWidth).then(modifier),
            paneTitle = paneTitle,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            contentPadding = contentPadding,
            shadow = shadow,
            content = { SafarPopupContent { content() } },
        )
    }
}

@Composable
fun SafarComposablesBottomSheet(
    state: BottomSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    toolbar: (@Composable () -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    shape: Shape = Theme[shapes][sheetShape],
    backgroundColor: Color = Theme[colors][panelColor],
    contentColor: Color = Theme[colors][onPanelColor],
    contentPadding: PaddingValues = PaddingValues(20.dp),
    shadow: Shadow = Theme[shadows][overlayShadow],
    body: @Composable ColumnScope.() -> Unit,
) {
    val maxWidth = minOf(640.dp, LocalSafarPopupMaxWidth.current)
    SafarPopupEnvironment {
        LibraryBottomSheet(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.widthIn(max = maxWidth).then(modifier),
            toolbar = toolbar?.let { slot -> { SafarPopupContent { slot() } } },
            footer = footer?.let { slot -> { val columnScope = this; SafarPopupContent { slot(columnScope) } } },
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            contentPadding = contentPadding,
            shadow = shadow,
            body = { val columnScope = this; SafarPopupContent { body(columnScope) } },
        )
    }
}

/** Anchors stay in their original view; only the floating panel restores popup density. */
@Composable
fun SafarComposablesDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    side: DropdownMenuSide = DropdownMenuSide.Bottom,
    alignment: DropdownMenuAlignment = DropdownMenuAlignment.Start,
    sideOffset: Dp = 4.dp,
    alignmentOffset: Dp = 0.dp,
    panel: @Composable DropdownMenuScope.() -> Unit,
    anchor: @Composable () -> Unit,
) {
    val popupDensity = LocalDensity.current
    LibraryDropdownMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        side = side,
        alignment = alignment,
        sideOffset = sideOffset,
        alignmentOffset = alignmentOffset,
        panel = {
            val panelScope = this
            ProvideSafarPopupScale(popupDensity) {
                SafarAnchoredPopupContent { panel(panelScope) }
            }
        },
        anchor = anchor,
    )
}

@Composable
fun SafarComposablesTooltip(
    enabled: Boolean = true,
    side: TooltipSide = TooltipSide.Top,
    alignment: TooltipAlignment = TooltipAlignment.Center,
    sideOffset: Dp = 8.dp,
    alignmentOffset: Dp = 0.dp,
    longPressShowDurationMillis: Long = 1500L,
    hoverDelayMillis: Long = 0L,
    panel: @Composable TooltipScope.() -> Unit,
    anchor: @Composable () -> Unit,
) {
    val popupDensity = LocalDensity.current
    LibraryTooltip(
        enabled = enabled,
        side = side,
        alignment = alignment,
        sideOffset = sideOffset,
        alignmentOffset = alignmentOffset,
        longPressShowDurationMillis = longPressShowDurationMillis,
        hoverDelayMillis = hoverDelayMillis,
        panel = {
            val panelScope = this
            ProvideSafarPopupScale(popupDensity) {
                SafarAnchoredPopupContent(maximumWidth = 320.dp) { panel(panelScope) }
            }
        },
        anchor = anchor,
    )
}
