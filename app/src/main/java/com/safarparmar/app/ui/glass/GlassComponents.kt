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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// GENERAL GLASS CARD
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
    // Mostly transparent base tints to look premium over black/white canvas backgrounds.
    val surfaceTint = when {
        isLight && isActive -> SafarGlassPalette.LightViolet
        isLight             -> Color.Black
        isActive            -> SafarGlassPalette.Violet
        else                -> Color.White
    }
    val tintAlpha = when {
        isLight && isActive -> 0.08f
        isLight             -> 0.04f   // mostly transparent black on white canvas
        isActive            -> 0.08f
        else                -> 0.05f   // mostly transparent white on black canvas
    }
    val titleColor    = if (isLight) SafarGlassPalette.LightTextPrimary   else SafarGlassPalette.TextPrimary
    val subtitleColor = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape       = RoundedCornerShape(24.dp),
                surfaceTint = surfaceTint,
                tintAlpha   = tintAlpha,
                isLight     = isLight,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leadingIcon()

        // Accent bar — gradient from accent to transparent
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.2f)),
                    ),
                    shape = CircleShape,
                ),
        )

        // Title + subtitle + active pill
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
            if (isActive) {
                GlassActivePill(isLight = isLight)
            }
        }

        // Right column: countdown badge + overflow menu
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLight)
                            accentColor.copy(alpha = 0.12f)
                        else
                            Color.White.copy(alpha = 0.14f),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = badgeText,
                    color      = if (isLight) accentColor else Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            trailingContent()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVE PILL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassActivePill(isLight: Boolean = false) {
    val bg        = if (isLight) SafarGlassPalette.LightViolet.copy(alpha = 0.12f)
                    else         SafarGlassPalette.Violet.copy(alpha = 0.24f)
    val dotColor  = if (isLight) Color(0xFF00C896) else Color(0xFF64FFDA)
    val textColor = if (isLight) SafarGlassPalette.LightViolet else Color.White

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text       = "Active",
            color      = textColor,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
        )
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
    val tint      = customTint ?: (if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet)
    val tintAlpha = if (isLight) 0.12f else 0.16f
    val textColor = customTextColor ?: (if (isLight) SafarGlassPalette.LightViolet else Color.White)
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
