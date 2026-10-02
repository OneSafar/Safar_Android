/*
 * Adapted from Composables UI theming documentation.
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
package com.safarparmar.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composables.interactioncapabilities.currentInteractionCapabilities
import com.composables.ui.theme.*
import com.composeunstyled.theme.ThemeComposable
import com.composeunstyled.theme.buildTheme

/** Override only when a feature has its own accent, such as Toppers Batch. */
val LocalSafarComposablesAccent = staticCompositionLocalOf<Color?> { null }
val LocalSafarComposablesScrim = staticCompositionLocalOf<Color?> { null }

/** Keeps Composables UI controls aligned with the active SAFAR Material palette. */
val SafarComposablesTheme: ThemeComposable = buildTheme {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalSafarComposablesAccent.current ?: SafarSemanticColors.brandPurple()
    val onAccent = if (accent.luminance() > 0.55f) scheme.background else Color.White
    val dark = LocalColorScheme.current == ColorScheme.Dark
    val mode = LocalInteractionMode.current
        ?: if (currentInteractionCapabilities().hasPointer) InteractionMode.Pointer else InteractionMode.Touch
    val touch = mode == InteractionMode.Touch

    properties[colors] = mapOf(
        backgroundColor to scheme.background,
        onBackgroundColor to scheme.onBackground,
        panelColor to scheme.surface,
        onPanelColor to scheme.onSurface,
        mutedColor to scheme.onSurfaceVariant,
        primaryColor to accent,
        onPrimaryColor to onAccent,
        secondaryColor to scheme.secondaryContainer,
        onSecondaryColor to scheme.onSecondaryContainer,
        controlColor to scheme.surfaceVariant,
        onControlColor to scheme.onSurfaceVariant,
        thumbColor to scheme.surface,
        switchTrackColor to scheme.surfaceVariant,
        switchSelectedTrackColor to accent,
        switchThumbColor to scheme.onPrimary,
        selectedControlColor to accent.copy(alpha = if (dark) 0.24f else 0.12f),
        onSelectedControlColor to accent,
        destructiveColor to scheme.error,
        onDestructiveColor to scheme.onError,
        borderColor to scheme.outlineVariant,
        fieldColor to scheme.surfaceContainerHigh,
        onFieldColor to scheme.onSurface,
        scrimColor to (LocalSafarComposablesScrim.current ?: scheme.scrim.copy(alpha = if (dark) 0.48f else 0.38f)),
        ringColor to accent.copy(alpha = 0.30f),
    )
    properties[textSelectionColors] = mapOf(
        com.composables.ui.theme.defaultTextSelectionColors to TextSelectionColors(
            handleColor = accent,
            backgroundColor = accent.copy(alpha = 0.24f),
        ),
    )
    properties[shapes] = mapOf(
        smallShape to RoundedCornerShape(8.dp),
        mediumShape to RoundedCornerShape(12.dp),
        largeShape to RoundedCornerShape(18.dp),
        buttonShape to RoundedCornerShape(if (touch) 14.dp else 10.dp),
        dialogShape to RoundedCornerShape(24.dp),
        sheetShape to RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        menuShape to RoundedCornerShape(16.dp),
        fieldShape to RoundedCornerShape(14.dp),
    )
    properties[shadows] = mapOf(
        raisedShadow to Shadow(radius = 16.dp, color = Color.Black,
            offset = DpOffset(0.dp, 4.dp), alpha = if (dark) 0.22f else 0.12f),
        overlayShadow to Shadow(radius = 24.dp, color = Color.Black,
            offset = DpOffset(0.dp, 8.dp), alpha = if (dark) 0.36f else 0.20f),
    )
    properties[alphas] = mapOf(disabledAlpha to 0.38f)
    defaultTextStyle = MaterialTheme.typography.bodyLarge

    val normalRipple = ripple(color = accent.copy(alpha = 0.10f))
    val inverseRipple = ripple(color = onAccent.copy(alpha = 0.14f))
    defaultIndication = normalRipple
    properties[indications] = mapOf(
        com.composables.ui.theme.defaultIndication to normalRipple,
        inverseIndication to inverseRipple,
    )
    extend { content ->
        CompositionLocalProvider(LocalInteractionMode provides mode) { content() }
    }
}
