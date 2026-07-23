package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * The planner's overflow popup, styled as macOS glass instead of a stock
 * Material 3 menu.
 *
 * A popup floats *over* content, which is exactly the case the macOS recipe in
 * [PlannerGlass] exists for — so these use [glassSurface] (translucent body,
 * top-edge light border, depth shadow) rather than the flat hairline recipe,
 * which is for inline surfaces sitting *in* the page. The trigger dots stay
 * flat-hairline; only the floating panel is glass.
 *
 * [DropdownMenu]'s own container is neutralised (transparent, no shadow, no
 * padding) so the glass surface underneath is the only chrome visible — M3's
 * tonal surface and elevation would otherwise show through as a second panel.
 */
@Composable
fun PlannerOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // Strip M3's own surface so only the glass body renders.
        containerColor = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(14.dp),
        properties = PopupProperties(focusable = true),
    ) {
        // A Popup is its own window too, so it gets the same real backdrop blur
        // as the dialogs — the menu frosts the screen behind it rather than just
        // sitting on a translucent panel. A smaller radius than the dialogs use:
        // a menu covers little of the screen, and a heavy blur under a small
        // panel reads as a smudge rather than glass.
        rememberPlannerBackdropBlur(radiusPx = 32)
        Column(
            modifier = Modifier
                .widthIn(min = 180.dp)
                .glassSurface(shape = RoundedCornerShape(14.dp), isDarkTheme = isDark)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

/**
 * One row inside a [PlannerOverflowMenu]. Mirrors M3's DropdownMenuItem shape
 * and touch target, but takes its type and colour from the planner palette so a
 * menu never looks like a different app from the card that opened it.
 *
 * [destructive] tints the row with the theme's error colour — used for Delete,
 * so the dangerous action is distinguishable without an icon.
 */
@Composable
fun PlannerOverflowMenuItem(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val contentColor = if (destructive) scheme.error else scheme.onSurface
    Row(
        modifier = modifier
            .widthIn(min = 180.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp),
            )
            Spacer14()
        }
        Text(
            text = text,
            fontSize = 13.5.sp,
            color = contentColor,
        )
    }
}

@Composable
private fun Spacer14() {
    androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
}
