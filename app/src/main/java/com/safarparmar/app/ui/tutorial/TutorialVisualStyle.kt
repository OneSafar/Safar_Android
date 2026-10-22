package com.safarparmar.app.ui.tutorial

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
    Color(0xFF211921), Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB),
    Color(0xFFF0BEDC), Color(0xFF3B1230), Color(0xFF684047), Color(0xFF28574D))
else TutorialVisualColors(Color(0xFFFFF5F0), Color(0xFFFFFDFC), Color(0xFF39132E), Color(0xFF75616F),
    Color(0xFF542347), Color.White, Color(0xFFFFCBBE), Color(0xFFC9EFE0))

/** Scope this only to tutorial chrome, never to the native feature presentation behind a spotlight. */
@Composable
internal fun TutorialVisualStyle(content: @Composable () -> Unit) {
    val dark = !MaterialTheme.colorScheme.background.isLightBackground()
    val c = tutorialVisualColors(dark)
    val scheme = MaterialTheme.colorScheme.copy(primary = c.action, onPrimary = c.onAction,
        primaryContainer = c.peach, onPrimaryContainer = c.ink, secondary = c.action,
        onSecondary = c.onAction, secondaryContainer = c.mint, onSecondaryContainer = c.ink,
        background = c.canvas, onBackground = c.ink, surface = c.card, onSurface = c.ink,
        surfaceVariant = c.canvas, onSurfaceVariant = c.muted, surfaceContainerLow = c.canvas,
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
