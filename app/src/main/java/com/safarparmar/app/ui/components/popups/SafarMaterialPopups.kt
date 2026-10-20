package com.safarparmar.app.ui.components.popups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.DatePickerDialog as MaterialDatePickerDialog
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet
import androidx.compose.ui.window.Dialog as PlatformDialog

/** Keep native dialog focus, Back handling and properties while matching the caller's scale. */
@Composable
fun SafarDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    fullScreen: Boolean = false,
    content: @Composable () -> Unit,
) {
    val maximumWidth = LocalSafarPopupMaxWidth.current
    SafarPopupEnvironment {
        PlatformDialog(onDismissRequest = onDismissRequest, properties = properties) {
            SafarPopupContent {
                if (fullScreen) content() else {
                    Box(
                        modifier = Modifier.widthIn(max = maximumWidth),
                        contentAlignment = Alignment.Center,
                    ) { content() }
                }
            }
        }
    }
}

@Composable
fun SafarMaterialAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val maximumWidth = LocalSafarPopupMaxWidth.current
    SafarPopupEnvironment {
        MaterialAlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = { SafarPopupContent { confirmButton() } },
            modifier = Modifier.widthIn(max = maximumWidth).then(modifier),
            dismissButton = dismissButton?.let { slot -> { SafarPopupContent { slot() } } },
            icon = icon?.let { slot -> { SafarPopupContent { slot() } } },
            title = title?.let { slot -> { SafarPopupContent { slot() } } },
            text = text?.let { slot -> { SafarPopupContent { slot() } } },
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val maximumWidth = minOf(sheetMaxWidth, LocalSafarPopupMaxWidth.current)
    SafarPopupEnvironment {
        MaterialModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.widthIn(max = maximumWidth).then(modifier),
            sheetState = sheetState,
            sheetMaxWidth = maximumWidth,
            sheetGesturesEnabled = sheetGesturesEnabled,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle?.let { slot -> { SafarPopupContent { slot() } } },
            contentWindowInsets = contentWindowInsets,
            properties = properties,
            content = { val columnScope = this; SafarPopupContent { content(columnScope) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit,
) {
    val maximumWidth = LocalSafarPopupMaxWidth.current
    SafarPopupEnvironment {
        MaterialDatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = { SafarPopupContent { confirmButton() } },
            modifier = Modifier.widthIn(max = maximumWidth).then(modifier),
            dismissButton = dismissButton?.let { slot -> { SafarPopupContent { slot() } } },
            shape = shape,
            tonalElevation = tonalElevation,
            colors = colors,
            properties = properties,
            content = { val columnScope = this; SafarPopupContent { content(columnScope) } },
        )
    }
}
