package com.safarparmar.app.feature.toppersbatch

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Shared by the tracker, native previews and layout tests.
 * Light accent matches SAFAR web's --tb-blue / --sc-accent (#BE185D).
 * Dark mode keeps the pink hue with enough contrast for text and controls.
 */
internal fun ColorScheme.batchTrackerColors(dark: Boolean): ColorScheme = copy(
    background = if (dark) Color(0xFF19151A) else Color(0xFFFAF7F2),
    surface = if (dark) Color(0xFF251E25) else Color(0xFFFFFDFC),
    surfaceVariant = if (dark) Color(0xFF352C35) else Color(0xFFF2ECEF),
    outlineVariant = if (dark) Color(0xFF514450) else Color(0xFFE8DCE2),
    primary = if (dark) Color(0xFFF9A8D4) else Color(0xFFBE185D),
    onPrimary = if (dark) Color(0xFF4A0A28) else Color.White,
    primaryContainer = if (dark) Color(0xFF4A1730) else Color(0xFFFCE7F3),
    onPrimaryContainer = if (dark) Color(0xFFFCE7F3) else Color(0xFF831843),
    secondary = if (dark) Color(0xFFF9A8D4) else Color(0xFF9D174D),
    onSecondary = if (dark) Color(0xFF4A0A28) else Color.White,
    secondaryContainer = if (dark) Color(0xFF4A1730) else Color(0xFFFCE7F3),
    onSecondaryContainer = if (dark) Color(0xFFFCE7F3) else Color(0xFF831843),
    surfaceContainerHigh = if (dark) Color(0xFF352630) else Color(0xFFFFF8FB),
)
