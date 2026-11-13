package com.safarparmar.app.ui.tutorial

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.isLightBackground

internal data class TutorialVisualColors(val canvas: Color, val card: Color, val ink: Color,
    val muted: Color, val action: Color, val onAction: Color, val peach: Color, val mint: Color)

internal fun tutorialVisualColors(dark: Boolean) = if (dark) TutorialVisualColors(
    BgDark, Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB),
    RoyalPurpleDark, Color(0xFF3B1230), RoyalPurpleSoftDark, Color(0xFF28574D))
else TutorialVisualColors(Color(0xFFFAF8FF), Color(0xFFFFFDFC), Color(0xFF241544), Color(0xFF75616F),
    RoyalPurple, Color.White, RoyalPurpleSoft, Color(0xFFC9EFE0))

/** Scope this only to tutorial chrome, never to the native feature presentation behind a spotlight. */
@Composable
internal fun TutorialVisualStyle(content: @Composable () -> Unit) {
    val dark = !MaterialTheme.colorScheme.background.isLightBackground()
    val c = tutorialVisualColors(dark)
    val componentSurface = if (dark) Color(0xFF211921) else c.canvas
    val scheme = MaterialTheme.colorScheme.copy(primary = c.action, onPrimary = c.onAction,
        primaryContainer = c.peach, onPrimaryContainer = c.ink, secondary = c.action,
        onSecondary = c.onAction, secondaryContainer = c.mint, onSecondaryContainer = c.ink,
        background = c.canvas, onBackground = c.ink, surface = c.card, onSurface = c.ink,
        surfaceVariant = componentSurface, onSurfaceVariant = c.muted, surfaceContainerLow = componentSurface,
        outline = c.muted, outlineVariant = c.peach)
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
internal fun TutorialLessonCard(title: String, completed: Boolean, icon: ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = tutorialVisualColors(!MaterialTheme.colorScheme.background.isLightBackground())
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = c.card, contentColor = c.ink)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(c.peach, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.ink, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (completed) Text(stringResource(R.string.ekagra_tutorial_completed), color = c.muted, style = MaterialTheme.typography.bodySmall)
            }
            if (completed) Box(Modifier.size(32.dp).background(c.mint, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = c.ink, modifier = Modifier.size(20.dp))
            } else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
    }
}

internal fun PracticeLesson.lessonIcon(): ImageVector = when (this) {
    PracticeLesson.BATCH_CORE, PracticeLesson.DHYAN_CORE -> Icons.Default.PlayArrow
    PracticeLesson.BATCH_REVIEW, PracticeLesson.ANALYTICS_CORE -> Icons.Default.BarChart
    PracticeLesson.HABITS_CORE -> Icons.Default.CheckCircle
    PracticeLesson.HABITS_REVIEW -> Icons.Default.Insights
    PracticeLesson.MEHFIL_CORE, PracticeLesson.JOURNAL_CORE -> Icons.Default.Edit
    PracticeLesson.MEHFIL_REVIEW -> Icons.Default.Forum
    PracticeLesson.DHYAN_REVIEW -> Icons.Default.MusicNote
    PracticeLesson.CIRCLE_CORE -> Icons.Default.Groups
    PracticeLesson.CIRCLE_CREATE -> Icons.Default.AddCircle
    PracticeLesson.CHECKIN_CORE -> Icons.Default.Mood
    PracticeLesson.STREAKS_CORE -> Icons.Default.LocalFireDepartment
    PracticeLesson.PLANNER_CORE -> Icons.Default.CalendarMonth
}
