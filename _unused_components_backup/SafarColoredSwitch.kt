/*
 * Adapted from Composables UI Switch.kt.
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.Thumb
import com.composeunstyled.Track
import com.composeunstyled.UnstyledSwitch

/** Composables UI source variant with the explicit colors used by feature switches. */
@Composable
fun SafarColoredSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedTrackColor: Color,
    uncheckedTrackColor: Color,
    checkedThumbColor: Color = Color.White,
    uncheckedThumbColor: Color = Color.White,
    checkedBorderColor: Color = checkedTrackColor,
    uncheckedBorderColor: Color = uncheckedTrackColor,
    disabledCheckedTrackColor: Color = checkedTrackColor.copy(alpha = 0.36f),
    disabledUncheckedTrackColor: Color = uncheckedTrackColor.copy(alpha = 0.5f),
    disabledCheckedThumbColor: Color = checkedThumbColor.copy(alpha = 0.7f),
    disabledUncheckedThumbColor: Color = uncheckedThumbColor.copy(alpha = 0.45f),
    disabledCheckedBorderColor: Color = checkedBorderColor.copy(alpha = 0.36f),
    disabledUncheckedBorderColor: Color = uncheckedBorderColor.copy(alpha = 0.5f),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
) {
    val trackColor by animateColorAsState(
        if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor,
        label = "Switch track",
    )
    val thumbColor by animateColorAsState(
        if (enabled) {
            if (checked) checkedThumbColor else uncheckedThumbColor
        } else if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor,
        label = "Switch thumb",
    )
    val shape = RoundedCornerShape(999.dp)
    val borderColor = if (enabled) {
        if (checked) checkedBorderColor else uncheckedBorderColor
    } else if (checked) disabledCheckedBorderColor else disabledUncheckedBorderColor
    UnstyledSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        modifier = modifier
            .then(if (accessibilityLabel != null) Modifier.semantics { contentDescription = accessibilityLabel } else Modifier)
            .alpha(1f),
    ) {
        Track(Modifier.background(trackColor, shape).border(1.dp, borderColor, shape)
            .size(44.dp, 24.dp).padding(2.dp)) {
            Thumb(animationSpec = spring()) {
                Box(Modifier.size(20.dp).background(thumbColor, CircleShape))
            }
        }
    }
}
