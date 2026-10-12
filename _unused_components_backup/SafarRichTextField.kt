package com.safarparmar.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.composables.ui.components.TextField
import com.composables.ui.components.TextFieldStyle
import com.composeunstyled.ProvideTextStyle
import kotlinx.coroutines.flow.drop

/** Composables UI field with the explicit Material colors already used by feature forms. */
@Composable
fun SafarRichTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onImeAction: (() -> Unit)? = null,
    maskText: Boolean = false,
    inputFilter: ((String) -> String)? = null,
    accessibilityLabel: String? = null,
) {
    val state = rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect(callback)
    }
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val fieldColor = when {
        !enabled -> colors.disabledContainerColor
        isError -> colors.errorContainerColor
        focused -> colors.focusedContainerColor
        else -> colors.unfocusedContainerColor
    }
    val borderColor = when {
        !enabled -> colors.disabledIndicatorColor
        isError -> colors.errorIndicatorColor
        focused -> colors.focusedIndicatorColor
        else -> colors.unfocusedIndicatorColor
    }
    val contentColor = when {
        !enabled -> colors.disabledTextColor
        isError -> colors.errorTextColor
        focused -> colors.focusedTextColor
        else -> colors.unfocusedTextColor
    }
    val labelColor = when {
        !enabled -> colors.disabledLabelColor
        isError -> colors.errorLabelColor
        focused -> colors.focusedLabelColor
        else -> colors.unfocusedLabelColor
    }
    val helpColor = when {
        !enabled -> colors.disabledSupportingTextColor
        isError -> colors.errorSupportingTextColor
        focused -> colors.focusedSupportingTextColor
        else -> colors.unfocusedSupportingTextColor
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) ProvideTextStyle(MaterialTheme.typography.labelMedium.copy(color = labelColor)) { label() }
        TextField(
            state = state,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 48.dp else (48 + (minLines - 1) * 24).dp),
            enabled = enabled,
            readOnly = readOnly,
            accessibilityLabel = accessibilityLabel,
            placeholder = placeholder,
            leading = leadingIcon,
            trailing = trailingIcon,
            style = TextFieldStyle.Default,
            shape = shape,
            backgroundColor = fieldColor,
            borderColor = borderColor,
            contentColor = contentColor,
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            selectionColors = colors.textSelectionColors,
            placeholderColor = if (focused) colors.focusedPlaceholderColor else colors.unfocusedPlaceholderColor,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else
                TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines),
            keyboardOptions = keyboardOptions,
            interactionSource = interactions,
            textStyle = textStyle,
            onKeyboardAction = if (onImeAction != null) ({ onImeAction() }) else null,
            outputTransformation = if (maskText) OutputTransformation {
                replace(0, length, "•".repeat(length))
            } else null,
            inputTransformation = inputFilter?.let(::safarInputFilter),
        )
        if (supportingText != null) ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = helpColor)) { supportingText() }
    }
}
