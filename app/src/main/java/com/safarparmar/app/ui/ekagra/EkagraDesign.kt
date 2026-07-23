package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * Shared primitives for the Ekagra redesign — hairlines, whitespace and a serif
 * display face instead of cards and filled chips.
 *
 * Nothing here hard-codes an accent: every call site passes the live
 * `MaterialTheme.colorScheme` colour so the screen keeps adapting to whichever
 * visual theme the user picked. The only fixed colours are neutral ink/paper
 * tints derived from the surrounding surface (or from white when the content
 * sits on the Timer tab's video/gradient canvas).
 */

/** Display face for numerals and section titles. */
val EkagraSerif: FontFamily = LoraFontFamily

/**
 * Palette for one Ekagra surface. `onCanvas` is the Timer tab, whose content
 * floats over the scrimmed video/gradient background and therefore always reads
 * as light-on-dark regardless of the app's light/dark setting.
 */
class EkagraInk(
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val hairline: Color,
    val trackFaint: Color,
)

@Composable
internal fun rememberEkagraInk(
    onCanvas: Boolean,
    theme: VisualTheme? = null,
    isDarkTheme: Boolean = true,
): EkagraInk {
    val scheme = MaterialTheme.colorScheme
    val isGradientTheme = theme?.gradientColors != null

    return if (onCanvas) {
        if (!isDarkTheme && theme?.gradientColors != null) {
            val (primaryInk, secondaryInk) = when (theme.name) {
                "Focus" -> Color(0xFF0F172A) to Color(0xFF334155)
                "Habits" -> Color(0xFF062C12) to Color(0xFF1C472A)
                "Journal" -> Color(0xFF3B0F03) to Color(0xFF571B0A)
                "Peace" -> Color(0xFF1E0C24) to Color(0xFF3B1A45)
                else -> Color(0xFF0F172A) to Color(0xFF334155)
            }
            EkagraInk(
                primaryText = primaryInk,
                secondaryText = secondaryInk,
                mutedText = secondaryInk.copy(alpha = 0.75f),
                hairline = primaryInk.copy(alpha = 0.20f),
                trackFaint = primaryInk.copy(alpha = 0.14f),
            )
        } else {
            EkagraInk(
                primaryText = Color.White,
                secondaryText = Color.White.copy(alpha = 0.88f),
                mutedText = Color.White.copy(alpha = 0.65f),
                hairline = Color.White.copy(alpha = 0.22f),
                trackFaint = Color.White.copy(alpha = 0.16f),
            )
        }
    } else {
        EkagraInk(
            primaryText = scheme.onSurface,
            secondaryText = scheme.onSurfaceVariant,
            mutedText = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            hairline = scheme.outlineVariant.copy(alpha = 0.55f),
            trackFaint = scheme.onSurfaceVariant.copy(alpha = 0.16f),
        )
    }
}

/**
 * Minimal header that replaces the old floating liquid-glass bar — a bare
 * hamburger, empty space, and whatever trailing actions the caller supplies,
 * drawn straight on the canvas instead of inside a frosted capsule. Tint
 * follows [ink] so it stays legible over the Timer tab's video/gradient
 * canvas and over the plain surface on the other tabs.
 */
@Composable
internal fun EkagraTopBar(
    ink: EkagraInk,
    onOpenDrawer: () -> Unit,
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = ink.primaryText)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Small uppercase, wide-tracked label that opens each screen. */
@Composable
internal fun EkagraEyebrow(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
        color = color,
        modifier = modifier,
    )
}

/** Serif line that names the screen, sitting under the eyebrow. */
@Composable
internal fun EkagraDisplayTitle(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontFamily = EkagraSerif,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        color = color,
        modifier = modifier,
    )
}

/** 1px rule — the redesign's only container. */
@Composable
internal fun EkagraHairline(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/**
 * Outlined pill used for presets and status chips. Selected state fills with the
 * caller-supplied accent so it still tracks the active visual theme.
 */
@Composable
internal fun EkagraPill(
    label: String,
    selected: Boolean,
    accent: Color,
    ink: EkagraInk,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(accent)
                else Modifier.border(1.dp, ink.hairline, CircleShape),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) contrastOn(accent) else ink.secondaryText,
        )
    }
}

/**
 * Text tabs with an accent underline on the active one — replaces the filled
 * segmented pill. Supports optional icon per tab.
 */
@Composable
internal fun <T> EkagraTextTabs(
    items: List<T>,
    selected: T,
    accent: Color,
    ink: EkagraInk,
    label: (T) -> String,
    icon: (@Composable (T, Color) -> Unit)? = null,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        items.forEach { item ->
            val isSelected = item == selected
            val tabColor = if (isSelected) ink.primaryText else ink.mutedText
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .clickable(
                        interactionSource = remembered(),
                        indication = null,
                    ) { onSelect(item) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (icon != null) {
                        icon(item, tabColor)
                    }
                    Text(
                        text = label(item),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = tabColor,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSelected) accent else Color.Transparent),
                )
            }
        }
    }
}

/** Accent-filled action pill (Start / Pause / Save). */
@Composable
internal fun EkagraPrimaryAction(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(accent)
            .border(1.dp, contrastOn(accent).copy(alpha = 0.35f), CircleShape)
            .clickable(interactionSource = remembered(), indication = null) { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = contrastOn(accent),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Hairline bordered action pill beside the primary pill (End / Cancel). */
@Composable
internal fun EkagraGhostAction(
    label: String,
    ink: EkagraInk,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .border(1.dp, ink.hairline, CircleShape)
            .clickable(interactionSource = remembered(), indication = null) { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Picks black or white for text drawn on top of [background]. Keeps label text
 * legible whichever accent the current visual theme supplies.
 */
internal fun contrastOn(background: Color): Color =
    if (background.luminanceCompat() > 0.55f) Color(0xFF16161A) else Color.White

private fun Color.luminanceCompat(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun remembered() =
    androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
