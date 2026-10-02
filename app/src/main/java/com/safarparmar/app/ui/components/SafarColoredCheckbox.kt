/*
 * Adapted from Composables UI Checkbox.kt.
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

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.composeunstyled.CheckedIndicator
import com.composeunstyled.UnstyledCheckbox

/** Composables UI source variant for checkboxes with feature specific colors. */
@Composable
fun SafarColoredCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color,
    uncheckedColor: Color,
    checkmarkColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
) {
    val shape = RoundedCornerShape(5.dp)
    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        accessibilityLabel = accessibilityLabel,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Box(Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
        CheckedIndicator(
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.clip(shape)
                .background(if (checked) checkedColor else Color.Transparent, shape)
                .border(1.dp, if (checked) checkedColor else uncheckedColor, shape)
                .size(20.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(14.dp)) {
                    if (checked) {
                        val stroke = 2.dp.toPx()
                        drawLine(checkmarkColor, Offset(size.width * 0.2f, size.height * 0.52f),
                            Offset(size.width * 0.42f, size.height * 0.74f), stroke, cap = StrokeCap.Round)
                        drawLine(checkmarkColor, Offset(size.width * 0.42f, size.height * 0.74f),
                            Offset(size.width * 0.8f, size.height * 0.28f), stroke, cap = StrokeCap.Round)
                    }
                }
            }
        }
        }
    }
}
