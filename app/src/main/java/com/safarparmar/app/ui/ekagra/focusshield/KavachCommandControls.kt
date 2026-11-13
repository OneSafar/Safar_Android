package com.safarparmar.app.ui.ekagra.focusshield

import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.ButtonStyle
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import androidx.compose.runtime.CompositionLocalProvider

internal object KavachCommandWorkspaceColors {
    val BgCream @Composable get() = if (isPlannerDark) BgDark else Color(0xFFFFF5F0)
    val CardWhite @Composable get() = if (isPlannerDark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val TextDark @Composable get() = if (isPlannerDark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val TextMuted @Composable get() = if (isPlannerDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val BorderSoft @Composable get() = if (isPlannerDark) Color(0xFF654D66) else Color(0xFFE8DEE5)
    val Peach @Composable get() = if (isPlannerDark) RoyalPurpleSoftDark else RoyalPurpleSoft
}

@Composable
internal fun KavachCommandButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp), colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF542347), contentColor = Color.White),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border: BorderStroke? = null, elevation: androidx.compose.material3.ButtonElevation? = null, content: @Composable RowScope.() -> Unit) {
    KavachCommandControl(onClick, modifier, enabled, shape, colors, contentPadding, ButtonStyle.Primary, content)
}
@Composable
internal fun KavachCommandOutlineButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp), colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border: BorderStroke? = null, elevation: androidx.compose.material3.ButtonElevation? = null, content: @Composable RowScope.() -> Unit) {
    KavachCommandControl(onClick, modifier, enabled, shape, colors, contentPadding, ButtonStyle.Outlined, content)
}
@Composable
internal fun KavachCommandTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp), contentPadding: PaddingValues = PaddingValues(8.dp), colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit) {
    KavachCommandControl(onClick, modifier, enabled, RoundedCornerShape(12.dp), colors, contentPadding, ButtonStyle.Ghost, content)
}
@Composable
private fun KavachCommandControl(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, shape: Shape,
    colors: ButtonColors, padding: PaddingValues, style: ButtonStyle, content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalSafarComposablesAccent provides Color(0xFF542347)) {
        SafarComposablesTheme {
            Button(onClick = onClick, enabled = enabled, style = if (style == ButtonStyle.Primary) ButtonStyle.Ghost else style, shape = shape,
                modifier = modifier.heightIn(min = 48.dp).then(if (style == ButtonStyle.Primary) Modifier.background(
                    if (enabled) colors.containerColor else colors.disabledContainerColor, shape) else Modifier), contentPadding = padding,
                contentColor = if (style == ButtonStyle.Primary) colors.contentColor else KavachCommandWorkspaceColors.TextDark) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, content = content)
            }
        }
    }
}

@Composable
internal fun KavachCommandAlert(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null, shape: Shape = RoundedCornerShape(24.dp)) {
    KavachCommandTheme {
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
            visible = true, onDismissRequest = onDismissRequest, title = title, text = { text?.invoke() },
            positiveButton = confirmButton, negativeButton = dismissButton, shape = shape,
            backgroundColor = KavachCommandWorkspaceColors.CardWhite, contentColor = KavachCommandWorkspaceColors.TextDark)
    }
}



@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun KavachCommandSheet(onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState = androidx.compose.material3.rememberModalBottomSheetState(),
    containerColor: Color = KavachCommandWorkspaceColors.CardWhite, shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), dragHandle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val state = com.composables.ui.components.rememberBottomSheetState(detents = listOf(
        com.composables.ui.components.BottomSheetDetent.Hidden, com.composables.ui.components.BottomSheetDetent.FullyExpanded))
    androidx.compose.runtime.LaunchedEffect(Unit) { state.targetDetent = com.composables.ui.components.BottomSheetDetent.FullyExpanded }
    KavachCommandTheme {
        com.safarparmar.app.ui.components.popups.SafarComposablesBottomSheet(state = state,
            onDismissRequest = onDismissRequest, backgroundColor = KavachCommandWorkspaceColors.CardWhite,
            contentColor = KavachCommandWorkspaceColors.TextDark, shape = shape, body = content)
    }
}

@Composable
internal fun KavachCommandField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, singleLine: Boolean = true, placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null, supportingText: (@Composable () -> Unit)? = null, shape: Shape = RoundedCornerShape(14.dp),
    colors: androidx.compose.material3.TextFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(),
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    val state = androidx.compose.foundation.text.input.rememberTextFieldState(value)
    val callback = androidx.compose.runtime.rememberUpdatedState(onValueChange)
    val latest = androidx.compose.runtime.rememberUpdatedState(value)
    androidx.compose.runtime.LaunchedEffect(value) { if (state.text.toString() != value) state.edit { replace(0, length, value) } }
    androidx.compose.runtime.LaunchedEffect(state) { androidx.compose.runtime.snapshotFlow { state.text.toString() }.collect {
        if (it != latest.value) callback.value(it)
    } }
    KavachCommandTheme {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            label?.invoke()
            com.composables.ui.components.TextField(state = state, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                placeholder = placeholder, leading = leadingIcon, trailing = trailingIcon, shape = shape,
                backgroundColor = KavachCommandWorkspaceColors.CardWhite, contentColor = KavachCommandWorkspaceColors.TextDark,
                borderColor = KavachCommandWorkspaceColors.BorderSoft, keyboardOptions = keyboardOptions,
                lineLimits = if (singleLine) androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
                    else androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine())
            supportingText?.invoke()
        }
    }
}

