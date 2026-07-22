package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * macOS "Subtle Control Center" glass system for the Exam Planner, derived from
 * the master recipe in macOS_UI_Agent_Guidelines.md (MacControlButton). The
 * master is treated as read-only; these are inheriting components.
 *
 * Two surfaces, one shared chrome:
 *  - [glassSurface]  — neutral translucent body for cards/containers (the true
 *    macOS "glass" look: translucent gray body + top-edge light border + depth
 *    shadow + tight radius).
 *  - [GlassButton]   — keeps the caller's own [accentColor] as the fill and adds
 *    only the glass chrome on top (light-gradient border, depth shadow, tight
 *    radius, subtle top sheen). Button colors are never changed — the fill is
 *    exactly the color passed in, content is passed via a slot.
 *
 * Theme is driven by the planner's own light/dark state (matching the rest of
 * the planner) rather than only the system setting, so it tracks in-app theme
 * toggles. Compose has no native backdrop blur below API 31, so — like the
 * source recipe — glass is simulated with translucency, a light border, and
 * shadow rather than a real blur.
 */

private fun plannerIsDark(scheme: androidx.compose.material3.ColorScheme): Boolean =
    !scheme.background.isLightBackground()

/** Top-edge light border that simulates light hitting the glass, per the recipe. */
private fun glassBorderBrush(isDark: Boolean): Brush =
    if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)),
        )
    }

private fun glassShadowElevation(isDark: Boolean) = if (isDark) 12.dp else 4.dp
private fun glassShadowColor(isDark: Boolean) =
    if (isDark) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)

/**
 * Translucent glass body for cards/containers — the drop-in replacement for the
 * flat `flatCard` modifier. Does not tint content — callers keep their own
 * text/icon colors.
 *
 * Pass a [tint] to get a *colored* glass body: a translucent wash of that color
 * is composited over the neutral glass base (stronger at the top, fading down),
 * so the surface reads as coloured glass rather than plain gray while staying
 * translucent. Omit [tint] for the neutral macOS look.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color? = null,
    isDarkTheme: Boolean = plannerIsDark(MaterialTheme.colorScheme),
    /** How saturated the [tint] wash reads. Defaults are subtle enough for a
     *  card behind dark text; raise them when the surface carries white content
     *  (e.g. the day-sheet stat tiles) so it stays legible. */
    tintTopAlpha: Float = if (isDarkTheme) 0.22f else 0.16f,
    tintBottomAlpha: Float = if (isDarkTheme) 0.07f else 0.05f,
): Modifier {
    val bodyColor = if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.65f) else Color(0xFFF9F9FB)
    var m = this
        .shadow(
            elevation = glassShadowElevation(isDarkTheme),
            shape = shape,
            spotColor = glassShadowColor(isDarkTheme),
            ambientColor = glassShadowColor(isDarkTheme),
        )
        .clip(shape)
        .background(bodyColor)
    if (tint != null) {
        // Colored wash over the glass base — the surface stays glassy and
        // translucent, just tinted.
        m = m.background(
            Brush.verticalGradient(
                colors = listOf(tint.copy(alpha = tintTopAlpha), tint.copy(alpha = tintBottomAlpha)),
            ),
        )
    }
    return m.border(width = 0.5.dp, brush = glassBorderBrush(isDarkTheme), shape = shape)
}

/**
 * A button rendered as COLORED translucent glass: the caller's [accentColor] is
 * washed over the neutral glass base (the same tinted-glass treatment as a
 * [glassSurface] with a tint, just stronger so white content stays legible),
 * then finished with the glass chrome — top-edge light border, depth shadow and
 * tight radius. The button's color is preserved (it's exactly [accentColor]),
 * only rendered as translucent glass rather than a solid fill. Content
 * (text/icon and their colors) is supplied by the caller.
 *
 * [tintTopAlpha]/[tintBottomAlpha] tune how saturated the colored glass reads —
 * defaults are picked so white text is legible over the common accent colors,
 * and can be lowered for a more see-through button.
 *
 * [greyShadeAlpha] adds the neutral grey scrim that gives real macOS Control
 * Center glass its muted, "colour seen through frosted grey" quality instead of
 * a flat saturated fill. It is laid over the colour wash, so it slightly
 * desaturates the accent (and marginally *improves* white-content contrast).
 * Set it to 0f for pure, unmuted colour.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    isDarkTheme: Boolean = plannerIsDark(MaterialTheme.colorScheme),
    tintTopAlpha: Float = if (isDarkTheme) 0.72f else 0.68f,
    tintBottomAlpha: Float = if (isDarkTheme) 0.52f else 0.50f,
    greyShadeAlpha: Float = if (isDarkTheme) 0.20f else 0.14f,
    content: @Composable RowScope.() -> Unit,
) {
    val bodyColor = if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.65f) else Color(0xFFF9F9FB)
    // Colored wash over the glass base — brighter at the top (light hits the
    // glass), fading down — so the button reads as coloured translucent glass.
    val tintBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = tintTopAlpha),
            accentColor.copy(alpha = tintBottomAlpha),
        ),
    )
    // Neutral grey scrim on top of the colour — the frosted-glass diffusion that
    // macOS control tiles have. Dark mode uses systemGray5-ish, light mode
    // systemGray, so the mute reads correctly against each background.
    val greyScrim = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFF8E8E93)
    Row(
        modifier = modifier
            .shadow(
                elevation = glassShadowElevation(isDarkTheme),
                shape = shape,
                spotColor = glassShadowColor(isDarkTheme),
                ambientColor = glassShadowColor(isDarkTheme),
            )
            .clip(shape)
            .background(bodyColor)
            .background(tintBrush)
            .background(greyScrim.copy(alpha = greyShadeAlpha))
            .border(width = 0.5.dp, brush = glassBorderBrush(isDarkTheme), shape = shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}
