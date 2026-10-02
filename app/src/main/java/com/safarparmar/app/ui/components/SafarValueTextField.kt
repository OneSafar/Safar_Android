package com.safarparmar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.components.TextFieldStyle
import kotlinx.coroutines.flow.drop

/** Value callback bridge for the state based Composables UI text field. */
@Composable
fun SafarValueTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputFilter: ((String) -> String)? = null,
) {
    val state = rememberTextFieldState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect(currentOnValueChange)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) Text(label, style = MaterialTheme.typography.labelMedium)
        TextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = label ?: placeholder ?: "Text field",
            placeholder = placeholder?.let { { Text(it) } },
            style = TextFieldStyle.Default,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.Transparent,
            borderColor = MaterialTheme.colorScheme.outline,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputFilter?.let(::safarInputFilter),
        )
        if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall)
    }
}

/** Slot based variant for existing forms with localized Compose labels. */
@Composable
fun SafarSlotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accessibilityLabel: String? = null,
    inputFilter: ((String) -> String)? = null,
) {
    val state = rememberTextFieldState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect(currentOnValueChange)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelMedium) { label() }
        TextField(
            state = state,
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 48.dp else (48 + (minLines - 1) * 24).dp),
            accessibilityLabel = accessibilityLabel,
            placeholder = placeholder,
            style = TextFieldStyle.Default,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.Transparent,
            borderColor = MaterialTheme.colorScheme.outline,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputFilter?.let(::safarInputFilter),
        )
    }
}
