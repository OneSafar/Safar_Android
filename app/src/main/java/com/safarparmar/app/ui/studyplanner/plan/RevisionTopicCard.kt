package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.util.bounceClick
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Mirrors the SM-2 interval labels used when scheduling. The Nth session (dates
// sorted ascending) maps to the Nth label, so this stays correct as sessions
// move from "remaining" to "completed" without changing the overall order.
private val REVISION_SESSION_LABELS = listOf("Day 1", "Day 3", "Week 1", "Week 2", "Month 1")

private val RevisionTeal = Color(0xFF26A69A)
private val RevisionAmber = Color(0xFFFFB300)
private val RevisionRed = Color(0xFFEF5350)

private data class RevisionSessionUi(
    val date: String,
    val label: String,
    val formattedDate: String,
    val done: Boolean,
    val overdue: Boolean,
)

/**
 * A revision topic rendered as a checklist of its individual spaced sessions.
 * Each session can be ticked/unticked independently, so a student can genuinely
 * progress through sessions 2..5 instead of the first tick ending the topic.
 */
@Composable
internal fun RevisionTopicCard(
    ref: TopicRef,
    onCompleteSession: (String) -> Unit,
    onUncompleteSession: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val today = remember { todayKey() }
    val isSpaced = ref.topic.revisionScheduleType == "spaced"

    val sessions = remember(ref.topic.revisionReminderDates, ref.topic.revisionCompletedDates) {
        val completed = ref.topic.revisionCompletedDates.map { it.take(10) }.toSet()
        val remaining = ref.topic.revisionReminderDates.map { it.take(10) }
        val ordered = (completed.toList() + remaining).filter { it.isNotBlank() }.distinct().sorted()
        ordered.mapIndexed { index, date ->
            val done = date in completed
            RevisionSessionUi(
                date = date,
                label = if (isSpaced) REVISION_SESSION_LABELS.getOrNull(index) ?: "Session ${index + 1}" else "Revision",
                formattedDate = formatRevisionDate(date),
                done = done,
                overdue = !done && date < today,
            )
        }
    }
    val doneCount = sessions.count { it.done }
    val total = sessions.size

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            RevisionAmber.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = ref.topic.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${ref.subject.name} · ${ref.chapter.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit revision schedule",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Progress summary — the count the student previously had no way to see.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = RevisionTeal.copy(alpha = 0.14f),
                    contentColor = RevisionTeal,
                ) {
                    Text(
                        text = "$doneCount / $total done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                if (isSpaced) {
                    Text(
                        text = "Spaced revision",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            sessions.forEach { session ->
                RevisionSessionRow(
                    session = session,
                    onToggle = {
                        if (session.done) onUncompleteSession(session.date)
                        else onCompleteSession(session.date)
                    },
                )
            }
        }
    }
}

@Composable
private fun RevisionSessionRow(
    session: RevisionSessionUi,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val accent = when {
        session.done -> RevisionTeal
        session.overdue -> RevisionRed
        else -> RevisionAmber
    }
    val circleColor by animateColorAsState(
        targetValue = if (session.done) RevisionTeal else Color.Transparent,
        label = "revisionSessionFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (session.done) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "revisionSessionCheck",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (session.done) RevisionTeal.copy(alpha = 0.06f)
                else scheme.surfaceVariant.copy(alpha = 0.35f),
            )
            .bounceClick(scaleDown = 0.96f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(
                    width = if (session.done) 0.dp else 1.5.dp,
                    color = accent.copy(alpha = 0.6f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .scale(checkScale)
                    .graphicsLayer { alpha = checkScale },
            )
        }

        Text(
            text = session.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (session.done) RevisionTeal else scheme.onSurface,
            modifier = Modifier.width(64.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = session.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (session.overdue) {
                Text(
                    text = "Due — tap to mark revised",
                    style = MaterialTheme.typography.labelSmall,
                    color = RevisionRed,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = if (session.done) "Revised" else "Tap",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (session.done) RevisionTeal else scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(2.dp))
    }
}

private fun formatRevisionDate(iso: String): String {
    val date = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: return iso
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
}
