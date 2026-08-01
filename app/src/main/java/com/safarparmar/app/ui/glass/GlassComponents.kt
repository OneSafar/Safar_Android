package com.safarparmar.app.ui.glass

/**
 * SAFAR LIQUID GLASS — EXAM PLANNER COMPONENTS
 * ───────────────────────────────────────────────────────────────────────────
 * Pre-built glass-styled components for the ExamPlanner Home tab.
 *
 * Design rules enforced here:
 *  • Only the OUTER card surface uses liquidGlass.
 *  • Nested elements use clean clip + background (no nested shadows/borders).
 *  • Every component accepts `isLight` so it responds to the system theme.
 *  • Transparency: panels are mostly transparent (low alphas like 0.05f to 0.08f)
 *    to display beautifully over solid black and white backdrops.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.isLightBackground

// ─────────────────────────────────────────────────────────────────────────────
// SAFAR GLASS (Dhyan recipe) — Dashboard / Ekagra / Profile
// Do not change ExamPlannerGlass* defaults below — those keep Exam Planner chrome.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SafarGlassCard(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    shape: Shape = RoundedCornerShape(SafarGlassChromeRadius),
    tintAlpha: Float? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safarFrostedPanel(isLight = isLight, shape = shape, tintAlpha = tintAlpha)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SafarGlassChip(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    size: Dp = 34.dp,
    shape: Shape = RoundedCornerShape(SafarGlassChromeRadius),
    tintAlpha: Float? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .safarFrostedPanel(isLight = isLight, shape = shape, tintAlpha = tintAlpha)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Volumetric liquid-glass CTA (Control Center recipe) with brand color tint.
 * Layered: colored frost → top specular gloss → reflective rim → content.
 */
@Composable
fun SafarGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = MaterialTheme.colorScheme.background.isLightBackground(),
    customTint: Color? = null,
    customTextColor: Color? = null,
) {
    val isDarkTheme = !isLight
    val cardShape = RoundedCornerShape(28.dp)
    val accent = customTint ?: SafarSemanticColors.brandPurple(isDarkTheme = !isLight)

    val textColor = customTextColor ?: if (isDarkTheme) Color.White else accent
    val iconOnDisc = Color.White

    // Colored glass body — frost + brand pigment (not grey-only)
    // Light uses the same pigment strength as dark so purple isn't washed out.
    val baseGradient = listOf(
        accent.copy(alpha = 0.42f),
        accent.copy(alpha = 0.22f),
    )
    val highlightGradient = if (isDarkTheme) {
        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
    } else {
        listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
    }
    val borderGradient = if (isDarkTheme) {
        listOf(
            Color.White.copy(alpha = 0.65f),
            accent.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.18f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.95f),
            accent.copy(alpha = 0.35f),
            Color.Black.copy(alpha = 0.10f),
        )
    }
    val shadowSpotColor = if (isDarkTheme) {
        accent.copy(alpha = 0.35f)
    } else {
        accent.copy(alpha = 0.28f)
    }
    val iconDiscBrush = Brush.verticalGradient(
        colors = listOf(
            accent.copy(alpha = if (isDarkTheme) 1f else 0.95f),
            accent.copy(alpha = if (isDarkTheme) 0.82f else 0.78f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = if (isDarkTheme) 14.dp else 10.dp,
                shape = cardShape,
                clip = false,
                spotColor = shadowSpotColor,
                ambientColor = shadowSpotColor.copy(alpha = shadowSpotColor.alpha * 0.7f),
            )
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // LAYER 1: Colored glass body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(baseGradient)),
        )

        // LAYER 2: Specular top highlight (glossy curve)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 5.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(highlightGradient)),
        )

        // LAYER 3: Reflective rim (white + accent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(borderGradient),
                    shape = cardShape,
                ),
        )

        // LAYER 4: Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconDiscBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconOnDisc,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Full-width macOS Control Center Primary Action Button Component.
 * Enforces the exact macOS Control Center recipe:
 * 1. Shape: RoundedCornerShape(20.dp) (or CircleShape for full-width action pills)
 * 2. Background:
 *    - Light Mode: #7845E5 (Auth Purple)
 *    - Dark Mode: #A78BFA (Lighter Purple)
 * 3. 0.5.dp Precise Border:
 *    - Dark Mode: Top light catch Brush.verticalGradient(White 25% -> White 2%)
 *    - Light Mode: Metallic silver Brush.verticalGradient(#E5E5EA -> #D1D1D6)
 * 4. Ambient Black Drop Shadow:
 *    - Dark Mode: 12.dp elevation with Black 80% opacity
 *    - Light Mode: 4.dp elevation with Black 12% opacity
 */
@Composable
fun MacOSPrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isLight: Boolean = MaterialTheme.colorScheme.background.isLightBackground(),
    customAccent: Color? = null,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val accent = customAccent ?: SafarSemanticColors.brandPurple(isDarkTheme = !isLight)

    val bodyColor = if (!enabled) {
        if (customAccent != null) {
            if (isLight) accent.copy(alpha = 0.15f) else accent.copy(alpha = 0.22f)
        } else {
            if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E).copy(alpha = 0.50f)
        }
    } else {
        accent
    }

    val textColor = if (!enabled) {
        if (customAccent != null) {
            if (isLight) accent.copy(alpha = 0.50f) else accent.copy(alpha = 0.60f)
        } else {
            if (isLight) Color(0xFF8E8E93) else Color.White.copy(alpha = 0.40f)
        }
    } else {
        Color.White
    }

    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }

    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) shadowElevation else 0.dp,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = shape,
            )
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = textColor,
                    strokeWidth = 2.5.dp,
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp).padding(end = 6.dp),
                    )
                }
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// macOS CONTROL CENTER — solid panel recipe (Home tab)
// ─────────────────────────────────────────────────────────────────────────────

private val MacOSControlShape = RoundedCornerShape(20.dp)

private object MacOSControlStyle {
    fun bodyColor(isLight: Boolean): Color =
        if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E).copy(alpha = 0.65f)

    fun borderBrush(isLight: Boolean): Brush =
        if (isLight) {
            Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
        } else {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
            )
        }

    fun shadowElevation(isLight: Boolean) = if (isLight) 4.dp else 12.dp

    fun shadowColor(isLight: Boolean): Color =
        if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)

    fun titleColor(isLight: Boolean): Color =
        if (isLight) Color.Black else Color.White

    fun subtitleColor(isLight: Boolean): Color =
        if (isLight) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
}

fun Modifier.macOSControlPanel(
    isLight: Boolean,
    shape: Shape = MacOSControlShape,
): Modifier {
    val shadowColor = MacOSControlStyle.shadowColor(isLight)
    return this
        .shadow(
            elevation = MacOSControlStyle.shadowElevation(isLight),
            shape = shape,
            spotColor = shadowColor,
            ambientColor = shadowColor,
        )
        .clip(shape)
        .background(MacOSControlStyle.bodyColor(isLight))
        .border(
            width = 0.5.dp,
            brush = MacOSControlStyle.borderBrush(isLight),
            shape = shape,
        )
}

@Composable
fun MacOSControlIconBadge(
    accentColor: Color,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .macOSControlPanel(isLight = isLight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun MacOSExamPlanCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    badgeText: String,
    isActive: Boolean,
    isLight: Boolean = false,
    leadingIcon: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MacOSControlShape
    val activeBorder = if (isLight) Color.Black else Color.White
    val panelModifier = if (isActive) {
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = MacOSControlStyle.shadowElevation(isLight),
                shape = shape,
                spotColor = MacOSControlStyle.shadowColor(isLight),
                ambientColor = MacOSControlStyle.shadowColor(isLight),
            )
            .clip(shape)
            .background(MacOSControlStyle.bodyColor(isLight))
            .border(width = 2.5.dp, color = activeBorder, shape = shape)
    } else {
        Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = shape)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .then(panelModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingIcon()
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        color = MacOSControlStyle.titleColor(isLight),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = subtitle,
                        color = MacOSControlStyle.subtitleColor(isLight),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = badgeText,
                        color = if (isLight) accentColor else Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp,
                    )
                    trailingContent()
                }
            }
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(12.dp)
                    .border(
                        width = 2.dp,
                        color = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E),
                        shape = CircleShape,
                    )
                    .background(Color(0xFFFF3B30), CircleShape),
            )
        }
    }
}

@Composable
fun MacOSControlActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    accentColor: Color = SafarSemanticColors.brandPurple(isDarkTheme = !isLight),
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (subtitle == null) Arrangement.Center else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = text,
                color = MacOSControlStyle.titleColor(isLight),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MacOSControlStyle.subtitleColor(isLight),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

@Composable
fun MacOSControlEmptyState(
    title: String,
    body: String,
    actionText: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    accentColor: Color = SafarSemanticColors.brandPurple(isDarkTheme = !isLight),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            color = MacOSControlStyle.titleColor(isLight),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            color = MacOSControlStyle.subtitleColor(isLight),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(4.dp))
        MacOSControlActionButton(
            text = actionText,
            icon = actionIcon,
            onClick = onAction,
            isLight = isLight,
            accentColor = accentColor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GENERAL GLASS CARD (Exam Planner — keep defaults)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExamPlannerGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    surfaceTint: Color = Color.White,
    tintAlpha: Float = 0.06f,
    isLight: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, surfaceTint = surfaceTint, tintAlpha = tintAlpha, isLight = isLight)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier,
            )
            .padding(contentPadding),
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// EXAM PLAN CARD (replaces PlannerTargetExamRow)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExamPlanGlassCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    badgeText: String,
    isActive: Boolean,
    isLight: Boolean = false,
    leadingIcon: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceTint = if (isLight) Color.Black else Color.White
    val tintAlpha = if (isLight) 0.04f else 0.05f
    val titleColor    = if (isLight) SafarGlassPalette.LightTextPrimary   else SafarGlassPalette.TextPrimary
    val subtitleColor = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
    val activeBorder = if (isLight) Color.Black else Color.White
    val cardShape = RoundedCornerShape(24.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape       = cardShape,
                    surfaceTint = surfaceTint,
                    tintAlpha   = tintAlpha,
                    isLight     = isLight,
                )
                .then(
                    if (isActive) {
                        Modifier.border(width = 2.5.dp, color = activeBorder, shape = cardShape)
                    } else Modifier,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen,
                )
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leadingIcon()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text       = title,
                    color      = titleColor,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines   = 1,
                )
                Text(
                    text     = subtitle,
                    color    = subtitleColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text       = badgeText,
                    color      = if (isLight) accentColor else Color.White.copy(alpha = 0.88f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                trailingContent()
            }
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(12.dp)
                    .border(
                        width = 2.dp,
                        color = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E),
                        shape = CircleShape,
                    )
                    .background(Color(0xFFFF3B30), CircleShape),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLASS BUTTON (Create Plan bar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExamPlannerGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    customTint: Color? = null,
    customTextColor: Color? = null,
) {
    // Transparent glass button: subtle tinted background.
    val tint      = customTint ?: SafarSemanticColors.brandPurple(isDarkTheme = !isLight)
    val tintAlpha = if (isLight) 0.12f else 0.16f
    val textColor = customTextColor ?: (if (isLight) SafarSemanticColors.brandPurple(isDarkTheme = false) else Color.White)
    val iconCircleBg = if (isLight) tint.copy(alpha = 0.12f)
                       else         Color.White.copy(alpha = 0.16f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape       = RoundedCornerShape(20.dp),
                surfaceTint = tint,
                tintAlpha   = tintAlpha,
                isLight     = isLight,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconCircleBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text       = text,
            color      = textColor,
            fontSize   = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLASS EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExamPlannerGlassEmptyState(
    title: String,
    body: String,
    actionText: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
) {
    val panelTint  = if (isLight) Color.Black else Color.White
    val tintAlpha  = if (isLight) 0.04f else 0.05f
    val titleColor = if (isLight) SafarGlassPalette.LightTextPrimary   else SafarGlassPalette.TextPrimary
    val bodyColor  = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
    val iconBg     = if (isLight) SafarGlassPalette.LightViolet.copy(alpha = 0.12f)
                     else         SafarGlassPalette.Violet.copy(alpha = 0.20f)
    val iconTint   = if (isLight) SafarGlassPalette.LightViolet else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape       = RoundedCornerShape(24.dp),
                surfaceTint = panelTint,
                tintAlpha   = tintAlpha,
                isLight     = isLight,
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text       = title,
            color      = titleColor,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text     = body,
            color    = bodyColor,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(4.dp))
        ExamPlannerGlassButton(
            text    = actionText,
            icon    = actionIcon,
            onClick = onAction,
            isLight = isLight,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLASS DIVIDER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
