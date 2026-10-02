/*
 * Visual adaptation of Composables UI RadioGroup.kt for existing selectable rows.
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Composables UI sized radio visual for rows that already own selection state. */
@Composable
fun SafarRadioIndicator(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val color = if (enabled) {
        if (selected) colors.selectedColor else colors.unselectedColor
    } else if (selected) colors.disabledSelectedColor else colors.disabledUnselectedColor
    Box(modifier.size(48.dp)
        .then(if (onClick != null) Modifier.selectable(selected = selected, enabled = enabled,
            role = Role.RadioButton, interactionSource = interactionSource, indication = null,
            onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val radius = 10.dp.toPx().coerceAtMost(size.minDimension / 2f)
            drawCircle(color, radius = radius - 1.dp.toPx(), center = center,
                style = Stroke(width = 2.dp.toPx()))
            if (selected) drawCircle(color, radius = 5.dp.toPx(), center = Offset(center.x, center.y))
        }
    }
}
