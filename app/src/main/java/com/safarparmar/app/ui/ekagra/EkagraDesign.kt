package com.safarparmar.app.ui.ekagra

import androidx.compose.ui.res.stringResource

import com.composables.ui.components.TabList
import com.composables.ui.components.Tab

import com.composables.ui.components.Text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import com.composables.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

/** Control accents are separate from the existing visual background gradients. */
internal fun ekagraControlAccent(name: String, dark: Boolean): Color = ekagraWarmPalette(name).let { if (dark) it.light else it.accent }

/** +5% size / +15% stroke weight for timer chrome (nav, ring, action pills). */
internal object EkagraChrome {
    const val SizeScale = 1.05f
    const val WeightScale = 1.15f

    fun size(baseDp: Float) = (baseDp * SizeScale).dp
    fun stroke(baseDp: Float) = (baseDp * WeightScale).dp
    fun text(baseSp: Float) = (baseSp * SizeScale).sp
}

/** Display face for numerals and section titles. */
val EkagraSerif: FontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily

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
    return if (onCanvas) EkagraInk(Color.White, Color.White.copy(alpha = .86f), Color.White.copy(alpha = .72f), Color.White.copy(alpha = .25f), Color.White.copy(alpha = .15f))
    else EkagraInk(scheme.onSurface, scheme.onSurfaceVariant, scheme.onSurfaceVariant, scheme.outlineVariant, scheme.surfaceVariant)

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
    Box(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 8.dp)) {
        Text(stringResource(com.safarparmar.app.R.string.module_ekagra), modifier = Modifier.align(Alignment.Center),
            color = ink.primaryText, fontFamily = EkagraSerif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        com.composables.ui.components.IconButton(onClick = onOpenDrawer, style = com.composables.ui.components.ButtonStyle.Ghost,
            modifier = Modifier.align(Alignment.CenterStart).size(48.dp)) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(com.safarparmar.app.R.string.common_open_menu), tint = ink.primaryText)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = trailing)
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
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp,
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
            .height(EkagraChrome.stroke(1f))
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
    com.composables.ui.components.Button(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = CircleShape,
        contentColor = if (selected) contrastOn(accent) else ink.secondaryText,
        contentPadding = PaddingValues(horizontal = EkagraChrome.size(14f), vertical = EkagraChrome.size(7f)),
        modifier = modifier.heightIn(min = 48.dp).then(
            if (selected) Modifier.background(accent, CircleShape)
            else Modifier.border(EkagraChrome.stroke(1f), ink.hairline, CircleShape)),
    ) {
        com.composables.ui.components.Text(label, fontSize = EkagraChrome.text(12f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) contrastOn(accent) else ink.secondaryText)
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
    com.composables.ui.components.Tabs(selectedTab = selected, onSelectedTabChange = onSelect, orderedTabs = items) {
        TabList(modifier = modifier) {
            items.forEach { item ->
                Tab(
                    key = item,
                    icon = icon?.let { render -> { render(item, if (item == selected) accent else ink.mutedText) } },
                    text = { Text(label(item), fontSize = EkagraChrome.text(13f), maxLines = 1) },
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
    com.composables.ui.components.Button(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = CircleShape,
        contentColor = contrastOn(accent),
        contentPadding = PaddingValues(horizontal = EkagraChrome.size(26f)),
        modifier = modifier.heightIn(min = 48.dp)
            .background(accent, CircleShape),
    ) {
        com.composables.ui.components.Text(label, fontSize = EkagraChrome.text(14.5f),
            fontWeight = FontWeight.Bold, color = contrastOn(accent), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val scheme = MaterialTheme.colorScheme
    com.composables.ui.components.Button(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = CircleShape,
        contentColor = scheme.onSurface,
        contentPadding = PaddingValues(horizontal = EkagraChrome.size(20f)),
        modifier = modifier.heightIn(min = 48.dp)
            .background(scheme.surface.copy(alpha = 0.94f), CircleShape)
            .border(1.dp, scheme.outlineVariant, CircleShape),
    ) {
        com.composables.ui.components.Text(label, fontSize = EkagraChrome.text(14f),
            fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/**
 * Icon with +5% size and a soft underlay so filled glyphs read ~15% heavier
 * against the timer canvas without swapping to custom stroke assets.
 */
@Composable
internal fun EkagraChromeIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String?,
    baseSizeDp: Float = 20f,
    modifier: Modifier = Modifier,
) {
    val size = EkagraChrome.size(baseSizeDp)
    val weightSize = size * EkagraChrome.WeightScale
    Box(
        modifier = modifier.size(weightSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint.copy(alpha = (tint.alpha * 0.40f).coerceIn(0f, 1f)),
            modifier = Modifier.size(size * 1.10f),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Applies a saturation adjustment (0f = black & white / greyscale, 1f = full colour).
 * Uses canvas layer filtering with a ColorMatrix across all Android versions.
 */
internal fun Modifier.saturation(saturation: Float): Modifier {
    if (saturation >= 0.999f) return this
    val matrix = ColorMatrix().apply {
        setToSaturation(saturation.coerceIn(0f, 1f))
    }
    return this.drawWithContent {
        val paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(matrix)
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(this.size.toRect(), paint)
            drawContent()
            canvas.restore()
        }
    }
}

