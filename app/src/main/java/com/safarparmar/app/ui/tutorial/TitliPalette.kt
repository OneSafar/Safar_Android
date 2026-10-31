package com.safarparmar.app.ui.tutorial

import androidx.compose.ui.graphics.Color

/** Same butterfly geometry, with a readable silhouette on the app's dark surfaces. */
internal data class TitliPalette(
    val wing: Color,
    val accent: Color,
    val outline: Color,
    val detail: Color,
    val outlineWidth: Float,
)

internal fun titliPalette(dark: Boolean): TitliPalette = if (dark) {
    TitliPalette(
        wing = Color(0xFF542347),
        accent = Color(0xFFFFA8CA),
        outline = Color(0xFFF0BEDC),
        detail = Color(0xFFF5D6EA),
        outlineWidth = 9f,
    )
} else {
    TitliPalette(
        wing = ButterflyColors.InkBlack,
        accent = ButterflyColors.AccentPink,
        outline = ButterflyColors.AccentPink.copy(alpha = 0.22f),
        detail = ButterflyColors.InkBlack,
        outlineWidth = 5f,
    )
}
