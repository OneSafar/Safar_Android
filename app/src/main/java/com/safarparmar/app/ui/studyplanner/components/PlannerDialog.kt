package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * The planner's confirmation/input dialog, styled as macOS glass instead of a
 * stock Material 3 [androidx.compose.material3.AlertDialog].
 *
 * Same reasoning as [PlannerOverflowMenu] and the Titli tour prompt: a dialog
 * floats *over* content, which is the case the macOS recipe in [PlannerGlass]
 * exists for — so the panel is [glassSurface], not the flat hairline recipe
 * (which is for surfaces sitting *in* the page). Nothing already on the macOS
 * recipe is changed by this; it only replaces bare M3 dialogs.
 *
 * The slot names mirror M3's AlertDialog (title / text / confirmButton /
 * dismissButton) so migrating a call site is a rename, not a rewrite.
 *
 * Real backdrop blur isn't available below API 31 for content behind a Dialog
 * window, so — matching the documented decision in [PlannerGlass] — the glass is
 * simulated with translucency, a top-edge light border and depth shadow. The
 * scrim is theme-aware rather than a flat black dim.
 */
@Composable
fun PlannerDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // True backdrop blur where the platform supports it (API 31+, and not
        // disabled by battery saver / reduced transparency). When it's live the
        // scrim is pulled back — a blurred backdrop already separates the panel
        // from the page, and dimming it as hard as before would bury the blur.
        val blurred = rememberPlannerBackdropBlur()
        val scrimColor = when {
            blurred && isDark -> Color.Black.copy(alpha = 0.28f)
            blurred -> Color(0xFF1C1C1E).copy(alpha = 0.12f)
            isDark -> Color.Black.copy(alpha = 0.55f)
            else -> Color(0xFF1C1C1E).copy(alpha = 0.28f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glassSurface(shape = RoundedCornerShape(22.dp), isDarkTheme = isDark)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (text != null) {
                    Spacer(Modifier.height(10.dp))
                    text()
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Primary action inside a [PlannerDialog] — the caller's [accentColor] rendered
 * as translucent glass by [GlassButton], so the button keeps its own meaning
 * (e.g. the error colour for a destructive confirm) while matching the panel.
 */
@Composable
fun PlannerDialogAction(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    // Last so call sites can use trailing-lambda syntax.
    onClick: () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    GlassButton(
        onClick = onClick,
        accentColor = if (enabled) accentColor else accentColor.copy(alpha = 0.35f),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        isDarkTheme = isDark,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.6f),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Secondary/cancel action inside a [PlannerDialog] — plain text, no chrome. */
@Composable
fun PlannerDialogTextAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text = text,
            color = if (enabled) color else color.copy(alpha = 0.4f),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Body text inside a [PlannerDialog], in the panel's own type and colour. */
@Composable
fun PlannerDialogText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Row scope helper kept so callers can lay out multi-action confirm slots. */
@Composable
fun PlannerDialogActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
