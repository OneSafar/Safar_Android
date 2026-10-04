package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.TextFieldColors
import com.composables.ui.components.*
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun GoalTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                            content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, style = ButtonStyle.Ghost, content = content)
}

/** Bridge the existing form state to the library's state-based text fields. */
@Composable
internal fun GoalInput(value: String, onValueChange: (String) -> Unit,
                       modifier: Modifier = Modifier, label: @Composable (() -> Unit)? = null,
                       singleLine: Boolean = false, shape: Shape, colors: TextFieldColors) {
    val state = rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.distinctUntilChanged().collect { callback(it) } }
    LaunchedEffect(value) { if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value) }
    Column(modifier) {
        label?.invoke()
        TextField(state = state, modifier = Modifier.fillMaxWidth(), shape = shape,
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
        title = title, text = { text?.invoke() }, positiveButton = confirmButton, negativeButton = dismissButton)
}

@Composable
internal fun GoalProgress(progress: () -> Float, modifier: Modifier = Modifier,
                          color: androidx.compose.ui.graphics.Color, trackColor: androidx.compose.ui.graphics.Color) {
    ProgressIndicator(progress = progress(), modifier = modifier, height = androidx.compose.ui.unit.Dp(3f),
        indicatorColor = color, trackColor = trackColor)
}
