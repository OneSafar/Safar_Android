package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Text
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.SafarComposablesTheme

/** Includes native calendar, library popups, history, and tutorial practice in one local palette. */
@Composable
internal fun GoalsWarmTheme(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme.copy(
        background = GoalsFlatColors.Bg, surface = GoalsFlatColors.Paper,
        surfaceContainer = GoalsFlatColors.Paper, surfaceContainerHigh = GoalsFlatColors.Paper,
        onSurface = GoalsFlatColors.Text, onSurfaceVariant = GoalsFlatColors.Muted,
        onBackground = GoalsFlatColors.Text, primary = GoalsFlatColors.Primary,
        onPrimary = GoalsFlatColors.OnPrimary, secondaryContainer = GoalsFlatColors.PrimarySoft,
        onSecondaryContainer = GoalsFlatColors.Text, surfaceVariant = GoalsFlatColors.PrimarySoft,
        outline = GoalsFlatColors.Hairline, outlineVariant = GoalsFlatColors.Hairline,
    )
    CompositionLocalProvider(LocalSafarComposablesAccent provides GoalsFlatColors.Primary) {
        MaterialTheme(colorScheme = scheme) { SafarComposablesTheme { content() } }
    }
}

@Composable
internal fun GoalEyebrow(text: String) {
    Text(text, color = GoalsFlatColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PoppinsFontFamily)
}

@Composable
internal fun GoalDivider(modifier: Modifier = Modifier, alpha: Float = 1f) {
    HorizontalSeparator(modifier = modifier, color = GoalsFlatColors.Hairline.copy(alpha = alpha))
}

/** Library modal shell around the existing locale-aware, date-constrained calendar. */
@Composable
internal fun GoalDatePickerDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
        visible = true, onDismissRequest = onDismissRequest,
        text = { Column { content() } }, positiveButton = confirmButton, negativeButton = dismissButton,
        backgroundColor = GoalsFlatColors.Paper, contentColor = GoalsFlatColors.Text,
    )
}
