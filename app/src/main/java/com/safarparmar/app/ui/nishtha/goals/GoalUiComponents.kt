package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.composables.ui.components.*
import com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog as AlertDialog
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun GoalTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                            contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, style = ButtonStyle.Ghost, contentPadding = contentPadding, contentColor = GoalsFlatColors.Primary, content = content)
}

/** Bridge the existing form state to the library's state-based text fields. */
@Composable
internal fun GoalInput(value: String, onValueChange: (String) -> Unit,
                       modifier: Modifier = Modifier, label: @Composable (() -> Unit)? = null,
                       singleLine: Boolean = false, shape: Shape, enabled: Boolean = true, accessibilityLabel: String? = null) {
    val state = rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.distinctUntilChanged().collect { if (it != currentValue) callback(it) } }
    LaunchedEffect(value) { if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        label?.invoke()
        TextField(state = state, modifier = Modifier.fillMaxWidth(), shape = shape, enabled = enabled, accessibilityLabel = accessibilityLabel,
            backgroundColor = GoalsFlatColors.Paper, contentColor = GoalsFlatColors.Text, borderColor = GoalsFlatColors.Hairline,
            fontSize = 15.sp, contentPadding = PaddingValues(14.dp),
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 5))
    }
}

@Composable
internal fun GoalDialog(onDismissRequest: () -> Unit, title: @Composable (() -> Unit)? = null,
                        text: @Composable (() -> Unit)? = null, confirmButton: @Composable () -> Unit,
                        dismissButton: @Composable (() -> Unit)? = null,
                        containerColor: androidx.compose.ui.graphics.Color,
                        shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(24),
                        modifier: Modifier = Modifier) {
    AlertDialog(visible = true, onDismissRequest = onDismissRequest, modifier = modifier,
        title = title, text = { text?.invoke() }, positiveButton = confirmButton, negativeButton = dismissButton,
        shape = shape, backgroundColor = containerColor, contentColor = GoalsFlatColors.Text, supportingTextColor = GoalsFlatColors.Muted)
}

@Composable
internal fun GoalProgress(progress: () -> Float, modifier: Modifier = Modifier,
                          color: androidx.compose.ui.graphics.Color, trackColor: androidx.compose.ui.graphics.Color) {
    ProgressIndicator(progress = progress(), modifier = modifier, height = androidx.compose.ui.unit.Dp(3f),
        indicatorColor = color, trackColor = trackColor)
}

/** Typed number input and external quick-add chips share the same bounded value. */
@Composable
internal fun GoalNumberInput(value: Int, onValueChange: (Int) -> Unit, maxValue: Int) {
    val state = rememberTextFieldState(if (value == 0) "" else value.toString())
    val callback by rememberUpdatedState(onValueChange)
    val current by rememberUpdatedState(value)
    LaunchedEffect(value) {
        if ((state.text.toString().toIntOrNull() ?: 0) != value) state.setTextAndPlaceCursorAtEnd(if (value == 0) "" else value.toString())
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString().toIntOrNull() ?: 0 }.distinctUntilChanged().collect { if (it != current) callback(it) }
    }
    TextField(state = state, modifier = Modifier.width(90.dp).heightIn(min = 72.dp),
        accessibilityLabel = stringResource(if (maxValue == 59) R.string.common_minutes_upper else R.string.common_hours_upper),
        lineLimits = TextFieldLineLimits.SingleLine, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        backgroundColor = GoalsFlatColors.PrimarySoft, contentColor = GoalsFlatColors.Primary,
        borderColor = GoalsFlatColors.Hairline, placeholder = { Text("00", color = GoalsFlatColors.Muted) },
        inputTransformation = {
            val digits = asCharSequence().filter(Char::isDigit).take(2)
            val bounded = if (digits.isEmpty()) "" else (digits.toString().toIntOrNull() ?: 0).coerceAtMost(maxValue).toString()
            if (asCharSequence().toString() != bounded) replace(0, length, bounded)
        })
}
