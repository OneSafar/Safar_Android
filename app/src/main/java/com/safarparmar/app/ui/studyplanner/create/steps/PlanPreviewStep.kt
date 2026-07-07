package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.data.remote.api.PlanPreviewResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

private data class PreviewWeek(val label: String, val days: List<Pair<LocalDate, List<String>>>)

/** Groups the (sparse — only study days are keys) calendarPreview map into 7-day weeks
 *  running from the first scheduled date, filling gaps as rest days so the student sees
 *  their whole week, not just the days with topics. */
private fun buildWeeks(preview: PlanPreviewResult): List<PreviewWeek> {
    val byDate = preview.calendarPreview.mapValues { (_, topics) -> topics.map { it.topicName } }
    val dates = byDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (dates.isEmpty()) return emptyList()
    val start = dates.min()
    val end = dates.max()
    val allDays = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    return allDays.chunked(7).mapIndexed { index, week ->
        PreviewWeek(
            label = "Week ${index + 1} — ${week.first().format(dayFormatter)} to ${week.last().format(dayFormatter)}",
            days = week.map { date -> date to (byDate[date.toString()] ?: emptyList()) },
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
    modifier: Modifier = Modifier,
) {
    val weeks = remember(preview) { buildWeeks(preview) }
    val examDateLabel = remember(preview.examDate) {
        preview.examDate?.take(10)?.let { runCatching { LocalDate.parse(it).format(dayFormatter) }.getOrNull() }
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

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(weeks, key = { it.label }) { week ->
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text(week.label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
                    week.days.forEach { (date, topics) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ":",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                            if (topics.isEmpty()) {
                                Text("Rest day 🌿", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(topics.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (examDateLabel != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("Exam Date: $examDateLabel 🎯", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f), enabled = !isConfirming) {
                Text("Let me adjust")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = !isConfirming) {
                if (isConfirming) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Looks good — save my plan", fontWeight = FontWeight.Bold)
                }
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
