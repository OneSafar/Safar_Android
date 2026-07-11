package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

private val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

private data class PreviewWeek(val label: String, val days: List<Pair<LocalDate, List<CalendarTopicItem>>>)

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

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat(value = preview.summary.totalTopics.toString(), label = "Total topics")
            SummaryStat(value = (preview.summary.requiredPerDay ?: 0).toString(), label = "Topics/day")
            SummaryStat(value = (preview.summary.daysUntilExam ?: 0).toString(), label = "Days to exam")
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
                    onPrevious = { weekIndex = (weekIndex - 1).coerceAtLeast(0) },
                    onNext = { weekIndex = (weekIndex + 1).coerceAtMost(weeks.lastIndex) },
                    hasPrevious = weekIndex > 0,
                    hasNext = weekIndex < weeks.lastIndex,
                )

                DayChipRow(
                    days = week.days.map { it.first },
                    selectedIndex = dayIndex,
                    onSelect = { dayIndex = it },
                )

                val selectedDay = week.days.getOrNull(dayIndex)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedDay != null) {
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
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                modifier = Modifier.size(8.dp).clip(CircleShape).background(PlannerAccent.Teal),
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
