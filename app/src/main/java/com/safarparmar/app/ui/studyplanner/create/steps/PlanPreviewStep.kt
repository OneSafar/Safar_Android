package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.data.remote.api.PlanPreviewResult
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.TopicSize

private val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

private data class PreviewWeek(val label: String, val days: List<Pair<LocalDate, List<CalendarTopicItem>>>) {
    val topicCount: Int get() = days.sumOf { it.second.size }
    val studyDays: Int get() = days.count { it.second.isNotEmpty() }
}

/** A day's load judged in effort points against the daily budget (goal x 2), so
 *  a day of four big topics reads as heavy even though four is the goal. */
private enum class DayLoad(val label: String) { REST("Rest day"), LIGHT("Light"), FULL("Full"), HEAVY("Heavy") }

private fun dayLoadOf(items: List<CalendarTopicItem>, dailyGoal: Int): DayLoad {
    if (items.isEmpty()) return DayLoad.REST
    val budget = (dailyGoal.coerceAtLeast(1) * 2).toFloat()
    val points = items.sumOf { it.points }.toFloat()
    return when {
        points < budget * 0.75f -> DayLoad.LIGHT
        points <= budget * 1.1f -> DayLoad.FULL
        else -> DayLoad.HEAVY
    }
}

/** Groups the (sparse — only study days are keys) calendarPreview map into 7-day weeks
 *  running from the first scheduled date, filling gaps as rest days so the student sees
 *  their whole week, not just the days with topics. */
private fun buildWeeks(preview: PlanPreviewResult): List<PreviewWeek> {
    val dates = preview.calendarPreview.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (dates.isEmpty()) return emptyList()
    val start = dates.min()
    val end = dates.max()
    val allDays = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    return allDays.chunked(7).mapIndexed { index, week ->
        PreviewWeek(
            label = "Week ${index + 1}",
            days = week.map { date -> date to (preview.calendarPreview[date.toString()] ?: emptyList()) },
        )
    }
}

@Composable
fun PlanPreviewStep(
    preview: PlanPreviewResult,
    isConfirming: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onAdjust: () -> Unit,
    /** Rebuilds with the daily budget allowed to stretch so nothing is dropped.
     *  Null once this preview was already built that way — re-offering it would
     *  be a button that changes nothing. */
    onScheduleAnyway: (() -> Unit)?,
    onEditTopic: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(preview) { buildWeeks(preview) }
    val examDateLabel = remember(preview.examDate) {
        preview.examDate?.take(10)?.let { runCatching { LocalDate.parse(it).format(dayFormatter) }.getOrNull() }
    }

    var weekIndex by remember(weeks) { mutableIntStateOf(0) }
    val week = weeks.getOrNull(weekIndex)

    var dayIndex by remember(weekIndex, weeks) {
        val today = LocalDate.now()
        val defaultIndex = week?.days?.indexOfFirst { it.first == today }?.takeIf { it >= 0 }
            ?: week?.days?.indexOfFirst { it.second.isNotEmpty() }?.takeIf { it >= 0 }
            ?: 0
        mutableIntStateOf(defaultIndex)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Here's your plan — does it look right?", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)

        val goal = preview.dailyGoal ?: 0
        val needed = preview.summary.requiredPerDay ?: 0
        val skipped = preview.summary.scheduleSkipped

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat(value = preview.summary.scheduleAssigned.toString(), label = "Topics scheduled")
            // "Topics/day" used to show requiredPerDay right next to the number the
            // user typed in settings, with no way to tell them apart. When they
            // disagree, that gap IS the story, so both are labelled explicitly.
            SummaryStat(
                value = if (goal > 0) goal.toString() else needed.toString(),
                label = "Your goal/day",
            )
            SummaryStat(value = (preview.summary.daysUntilExam ?: 0).toString(), label = "Days to exam")
        }

        // The honest verdict. scheduleSkipped was computed and sent by the server
        // all along but never shown, so a plan that silently dropped 40 topics
        // looked identical to one that fit perfectly.
        if (skipped > 0) {
            PreviewVerdictCard(
                accent = MaterialTheme.colorScheme.error,
                icon = Icons.Default.Warning,
                title = "$skipped ${if (skipped == 1) "topic doesn't" else "topics don't"} fit before your exam",
                body = buildString {
                    append("Only ${preview.summary.scheduleAssigned} of ${preview.summary.totalTopics} topics could be scheduled. ")
                    append(
                        if (needed > 0 && goal in 1 until needed) {
                            "Raise your daily goal to about $needed, push the exam date, or trim topics."
                        } else {
                            "Push the exam date back, or remove some topics."
                        },
                    )
                },
                actionLabel = onScheduleAnyway?.let { "Schedule them anyway" },
                actionHint = if (onScheduleAnyway == null) null else if (needed > 0) {
                    "Fits everything in by studying about $needed a day instead of $goal."
                } else {
                    "Fits everything in by making your days fuller than your goal."
                },
                onAction = onScheduleAnyway,
            )
        } else if (needed > goal && goal > 0) {
            PreviewVerdictCard(
                accent = PlannerAccent.Amber,
                icon = Icons.Default.Warning,
                title = "This plan needs about $needed topics a day",
                body = "That's more than the $goal you asked for. Everything fits before your exam, but the days will be fuller than your goal. Go back if you'd rather raise the goal yourself or trim topics.",
            )
        } else {
            PreviewVerdictCard(
                accent = PlannerAccent.Teal,
                icon = Icons.Default.CheckCircle,
                title = "Everything fits before your exam",
                body = "All ${preview.summary.totalTopics} topics are scheduled within your daily goal.",
            )
        }

        // Server-side scheduling warnings — most importantly Mixed Bag's
        // deferral notice, which tells the user exactly when their non-chosen
        // subjects start. Shown before they commit, not discovered in March.
        preview.warnings.forEach { warning ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        if (week == null) {
            Text(
                "We couldn't build a schedule preview yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekNavHeader(
                    weekNumber = weekIndex + 1,
                    rangeLabel = "${week.days.first().first.format(dayFormatter)} – ${week.days.last().first.format(dayFormatter)}",
                    loadLabel = "${week.topicCount} topics · ${week.studyDays} study days",
                    onPrevious = { weekIndex = (weekIndex - 1).coerceAtLeast(0) },
                    onNext = { weekIndex = (weekIndex + 1).coerceAtMost(weeks.lastIndex) },
                    hasPrevious = weekIndex > 0,
                    hasNext = weekIndex < weeks.lastIndex,
                )

                DayChipRow(
                    days = week.days.map { it.first },
                    loads = week.days.map { dayLoadOf(it.second, goal) },
                    selectedIndex = dayIndex,
                    onSelect = { dayIndex = it },
                )

                val selectedDay = week.days.getOrNull(dayIndex)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedDay != null) {
                        val load = dayLoadOf(selectedDay.second, goal)
                        if (selectedDay.second.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${selectedDay.second.size} topics · ${load.label.lowercase()} day",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (load) {
                                        DayLoad.HEAVY -> MaterialTheme.colorScheme.error
                                        DayLoad.FULL -> PlannerAccent.Amber
                                        else -> PlannerAccent.Teal
                                    },
                                )
                            }
                        }
                        if (selectedDay.second.isEmpty()) {
                            item {
                                Text(
                                    "Rest day 🌿",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        } else {
                            items(selectedDay.second) { topic ->
                                TopicPill(
                                    topic = topic,
                                    onEditTopic = onEditTopic
                                )
                            }
                        }
                    }
                    if (examDateLabel != null) {
                        item {
                            Text(
                                "Exam Date: $examDateLabel 🎯",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f), enabled = !isConfirming) {
                Text("Go Back")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = !isConfirming) {
                if (isConfirming) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Looks Good", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WeekNavHeader(
    weekNumber: Int,
    rangeLabel: String,
    loadLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PlannerAccent.Teal.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous week",
                    tint = if (hasPrevious) PlannerAccent.Teal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "WEEK $weekNumber",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = PlannerAccent.Teal,
                )
                Text(
                    rangeLabel,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    loadLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next week",
                    tint = if (hasNext) PlannerAccent.Teal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun DayChipRow(
    days: List<LocalDate>,
    loads: List<DayLoad>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEachIndexed { index, date ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(index) },
                shape = RoundedCornerShape(50),
                color = if (selected) PlannerAccent.Teal else MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Load dot — lets a heavy day be spotted while scanning the
                    // week rather than only after tapping into it.
                    val load = loads.getOrNull(index) ?: DayLoad.REST
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    selected -> Color.White.copy(alpha = if (load == DayLoad.REST) 0.35f else 0.9f)
                                    load == DayLoad.REST -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                    load == DayLoad.LIGHT -> PlannerAccent.Teal
                                    load == DayLoad.FULL -> PlannerAccent.Amber
                                    else -> MaterialTheme.colorScheme.error
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicPill(
    topic: CalendarTopicItem,
    onEditTopic: (String, String) -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember(topic.topicName) { mutableStateOf(topic.topicName) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Topic Name") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank() && editName != topic.topicName) {
                            onEditTopic(topic.topicId, editName)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(
                    when (topic.size) {
                        TopicSize.BIG -> PlannerAccent.Coral
                        TopicSize.SMALL -> PlannerAccent.Teal.copy(alpha = 0.55f)
                        else -> PlannerAccent.Teal
                    },
                ),
            )
            Column(
                modifier = Modifier.padding(start = 12.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    topic.topicName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (topic.subjectName.isNotBlank()) {
                        Text(
                            topic.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (topic.subjectName.isNotBlank() && topic.chapterName.isNotBlank()) {
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    if (topic.chapterName.isNotBlank()) {
                        Text(
                            topic.chapterName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    editName = topic.topicName
                    showEditDialog = true
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit topic",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}



@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
        ) {
            Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The one-line answer to "will this plan actually work?", shown directly under
 * the summary stats. Green when everything fits, amber when it fits but only by
 * exceeding the daily goal, red when topics had to be dropped entirely.
 */
@Composable
private fun PreviewVerdictCard(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    actionHint: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionLabel != null && onAction != null) {
                    // The one route to flex scheduling, offered only here — with
                    // the real numbers on screen — instead of as a setting picked
                    // blind before the plan exists.
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.Bold, color = accent)
                    }
                    if (actionHint != null) {
                        Text(
                            text = actionHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
