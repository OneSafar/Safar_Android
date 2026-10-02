/*
 * Adapted from Composables UI Buttons.kt for selected filter controls.
 * Copyright (c) 2026 Composable Horizons
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.safarparmar.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.ui.components.bouncyPress
import com.composables.ui.components.focusRing
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.UnstyledButton

/** Selection chip using Composables UI button interaction and SAFAR colors. */
@Composable
fun SafarChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = Color.Transparent,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedLabelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
) {
    val source = remember { MutableInteractionSource() }
    UnstyledButton(
        onClick = onClick,
        interactionSource = source,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.defaultMinSize(minHeight = 40.dp)
            .bouncyPress(source, enabled = true)
            .focusRing(source, shape = shape)
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) selectedContainerColor else containerColor, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
    ) {
        ProvideContentColor(if (selected) selectedLabelColor else labelColor) {
            CompositionLocalProvider(LocalContentColor provides if (selected) selectedLabelColor else labelColor) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    Box(contentAlignment = Alignment.Center) { label() }
                }
            }
        }
    }
}
