package com.example.liquidglass

/**
 * LIQUID GLASS — COMPONENTS
 * ---------------------------------------------------------------------------
 * Everyday building blocks, all styled with Modifier.liquidGlass from
 * GlassEffects.kt. Use these the same way you'd use their Material
 * equivalents (Text, Button, Card, ListItem, ...).
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

// ─────────────────────────────────────────────────────────────────────────
// LABELS
// ─────────────────────────────────────────────────────────────────────────

enum class GlassLabelVariant { Title, Subtitle, Body, Caption }

/** Typed text style for headings/body/captions on glass backgrounds. */
@Composable
fun GlassLabel(
    text: String,
    modifier: Modifier = Modifier,
    variant: GlassLabelVariant = GlassLabelVariant.Body,
) {
    val (size, weight, color) = when (variant) {
        GlassLabelVariant.Title -> Triple(22.sp, FontWeight.Bold, GlassPalette.TextPrimary)
        GlassLabelVariant.Subtitle -> Triple(16.sp, FontWeight.SemiBold, GlassPalette.TextPrimary.copy(alpha = 0.9f))
        GlassLabelVariant.Body -> Triple(14.sp, FontWeight.Normal, GlassPalette.TextPrimary.copy(alpha = 0.8f))
        GlassLabelVariant.Caption -> Triple(12.sp, FontWeight.Normal, GlassPalette.TextSecondary)
    }
    Text(text = text, modifier = modifier, fontSize = size, fontWeight = weight, color = color)
}

// ─────────────────────────────────────────────────────────────────────────
// BUTTONS
// ─────────────────────────────────────────────────────────────────────────

enum class GlassButtonStyle { Primary, Secondary, Danger }

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val tint = when (style) {
        GlassButtonStyle.Primary -> GlassPalette.Violet
        GlassButtonStyle.Secondary -> Color.White
        GlassButtonStyle.Danger -> GlassPalette.Coral
    }
    val tintAlpha = if (style == GlassButtonStyle.Secondary) 0.10f else 0.38f

    Row(
        modifier = modifier
            .liquidGlass(shape = shape, surfaceTint = tint, tintAlpha = tintAlpha, elevation = 10.dp)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            it()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .liquidGlass(shape = CircleShape, tintAlpha = 0.14f, elevation = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// CHIPS
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                surfaceTint = if (selected) GlassPalette.Cyan else Color.White,
                tintAlpha = if (selected) 0.38f else 0.10f,
                elevation = 6.dp,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// BADGE
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .liquidGlass(shape = CircleShape, surfaceTint = GlassPalette.Coral, tintAlpha = 0.85f, elevation = 4.dp)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// SWITCH
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = GlassPalette.Violet.copy(alpha = 0.7f),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
            uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
            uncheckedBorderColor = Color.White.copy(alpha = 0.2f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────
// TEXT FIELD
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, tintAlpha = 0.10f, elevation = 6.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            it()
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f, fill = true)) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(GlassPalette.Cyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// CARD
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape)
            .padding(contentPadding),
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────────────────
// LIST ITEM + DIVIDER
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp), tintAlpha = 0.10f, elevation = 8.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(text = it, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            }
        }
        trailing?.let {
            Spacer(Modifier.width(10.dp))
            it()
        }
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.25f), Color.Transparent),
                ),
            ),
    )
}
