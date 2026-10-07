/*
 * Copyright (c) 2026 Composable Horizons
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.DropdownMenuPanel
import com.composeunstyled.ProvideTextStyle
import com.composables.ui.components.Button as ComposablesButton
import com.composables.ui.components.ButtonStyle
import kotlinx.coroutines.flow.drop

/** Adapted from Composables UI Buttons/Utils and AlertDialog (MIT).
 * SAFAR's Material 3 colors and dialog layout remain scoped to the tracker.
 * Sources: https://composables.com/ui/docs/buttons and /alert-dialog.
 */
@Composable
private fun Modifier.batchPress(source: MutableInteractionSource, enabled: Boolean): Modifier {
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.98f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow), label = "Batch button press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary,
            RoundedCornerShape(14.dp)) else Modifier)
}

@Composable
internal fun BatchButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    destructive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    ComposablesButton(onClick = batchFeatureAction(onClick), modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        style = if (destructive) ButtonStyle.Destructive else ButtonStyle.Default,
        shape = RoundedCornerShape(14.dp), contentPadding = contentPadding,
        interactionSource = source, content = content)
}

@Composable
internal fun BatchOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    ComposablesButton(onClick = batchFeatureAction(onClick), modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        style = ButtonStyle.Outlined, shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        contentColor = MaterialTheme.colorScheme.primary,
        interactionSource = source, content = content)
}

@Composable
internal fun BatchTextButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    ComposablesButton(onClick = batchFeatureAction(onClick), modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        style = ButtonStyle.Ghost, shape = RoundedCornerShape(12.dp),
        contentColor = MaterialTheme.colorScheme.primary, interactionSource = source) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

@Composable
internal fun BatchTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null, singleLine: Boolean = false,
    accessibilityLabel: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
) {
    val strings = rememberBatchStrings()
    val access = LocalBatchFeatureAccess.current
    if (!access.allowed) {
        Column(modifier.fillMaxWidth().clickable(onClick = access.onUpgrade).heightIn(min = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            label?.invoke()
            androidx.compose.material3.Text(value.ifBlank { accessibilityLabel ?: strings.text(R.string.toppers_batch_toppers_batch_text_field) })
        }
        return
    }
    val state = rememberTextFieldState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.edit { replace(0, length, value) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect { latestOnChange(it) }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) ProvideTextStyle(MaterialTheme.typography.labelMedium) { label() }
        com.composables.ui.components.TextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            accessibilityLabel = accessibilityLabel ?: strings.text(R.string.toppers_batch_toppers_batch_text_field),
            placeholder = placeholder,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
            style = com.composables.ui.components.TextFieldStyle.Default,
        )
    }
}

/** Width-limited panel with scrolling content and reachable actions, including with the keyboard open. */
@Composable
internal fun BatchAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
) {
    val strings = rememberBatchStrings()
    com.composables.ui.components.AlertDialog(
        visible = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth().imePadding().widthIn(max = 520.dp),
        paneTitle = strings.text(R.string.toppers_batch_toppers_batch_dialog),
    ) {
        BatchClampedContent {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) { title() }
                Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                }
                com.safarparmar.app.ui.components.SafarAdaptiveRow {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

/** Tab treatment adapted from the documented Tabs component: animated selection and press/focus feedback. */
@Composable
internal fun BatchFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
) {
    val source = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface, label = "Tab background")
    Surface(modifier.batchPress(source, true), shape = RoundedCornerShape(12.dp),
        color = background, contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant)) {
        Box(Modifier.selectable(selected, role = Role.Tab, interactionSource = source,
            indication = LocalIndication.current, onClick = batchFeatureAction(onClick))
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) { label() }
        }
    }
}

@Composable
internal fun BatchIconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    com.composables.ui.components.IconButton(onClick = batchFeatureAction(onClick), modifier = modifier, enabled = enabled,
        style = ButtonStyle.Ghost, interactionSource = source, content = content)
}

/** Dialog portals can supply their own density; clamp inside their content as well. */
@Composable
internal fun BatchClampedContent(content: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
        androidx.compose.ui.unit.Density(density.density, density.fontScale.coerceIn(0.85f, 1.05f))) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) { content() }
    }
}

/** Apply the same font-scale bounds inside menu portals. */
@Composable
internal fun com.composables.ui.components.DropdownMenuScope.BatchDropdownMenuPanel(
    content: @Composable com.composables.ui.components.DropdownMenuPanelContentScope.() -> Unit,
) {
    DropdownMenuPanel {
        val scope = this
        BatchClampedContent { content(scope) }
    }
}
