package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.composables.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class DayCompletion(val date: LocalDate, val count: Int)
internal data class TrendPoint(val date: LocalDate, val completed: Int)

private fun completionDay(lecture: BatchLecture): LocalDate? {
    if (lecture.completionDateUnknown) return null
    val stamp = lecture.completedAt ?: return null
    return runCatching { Instant.parse(stamp).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() }
        .getOrElse { runCatching { LocalDate.parse(stamp.take(10)) }.getOrNull() }
}

/** Completion is grouped by the day the student tapped Done, in India time. */
internal fun completionActivity(lectures: List<BatchLecture>, end: LocalDate, days: Int): List<DayCompletion> {
    require(days > 0)
    val counts = lectures.mapNotNull(::completionDay).groupingBy { it }.eachCount()
    return (days - 1 downTo 0).map { daysAgo ->
        val day = end.minusDays(daysAgo.toLong())
        DayCompletion(day, counts[day] ?: 0)
    }
}

internal fun weeklyCompletions(lectures: List<BatchLecture>, today: LocalDate): List<DayCompletion> =
    completionActivity(lectures, today, 7)

/** Running total of completed lectures, including work finished before this window. */
internal fun completionTrend(lectures: List<BatchLecture>, today: LocalDate, days: Int = 14): List<TrendPoint> {
    require(days > 0)
    val start = today.minusDays(days.toLong() - 1)
    val dates = lectures.mapNotNull(::completionDay)
    var running = dates.count { it.isBefore(start) }
    val byDay = dates.groupingBy { it }.eachCount()
    return (0 until days).map { offset ->
        val date = start.plusDays(offset.toLong())
        running += byDay[date] ?: 0
        TrendPoint(date, running)
    }
}

@Composable
internal fun RecentActivityCalendar(lectures: List<BatchLecture>, today: LocalDate) {
    val strings = rememberBatchStrings()
    val end = today.plusDays((7 - today.dayOfWeek.value).toLong())
    val days = remember(lectures, today) { completionActivity(lectures, end, 28) }
    var selectedDate by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = days.firstOrNull { it.date.toString() == selectedDate } ?: days.first { it.date == today }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.text(R.string.toppers_batch_recent_activity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(strings.text(R.string.toppers_batch_darker_dates_mean_more_lectures_finished), style = MaterialTheme.typography.bodySmall, color = muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                strings.weekdays().forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = muted)
                    }
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        val future = day.date.isAfter(today)
                        val shade = when {
                            future -> Color.Transparent
                            day.count == 0 -> empty
                            day.count == 1 -> Color(0xFFF9D5E5)
                            day.count == 2 -> Color(0xFFE879A7)
                            else -> Color(0xFF9D174D)
                        }
                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp)
                                .then(if (future) Modifier else Modifier.clickable(role = Role.Button, onClick = batchFeatureAction({
                                    selectedDate = day.date.toString()
                                }))).semantics {
                                    contentDescription = strings.text(R.string.toppers_batch_count_count_lectures_finished, strings.date(day.date.toString()), day.count) +
                                        if (day.date == selected.date) strings.text(R.string.toppers_batch_selected) else ""
                                },
                            color = shade,
                            shape = RoundedCornerShape(8.dp),
                            border = if (day.date == selected.date) BorderStroke(2.dp, Color(0xFF344054)) else null,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!future) Text(day.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        day.count >= 3 -> Color.White
                                        day.count > 0 -> Color(0xFF4A102A)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    })
                            }
                        }
                    }
                }
            }
            Text("${selected.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", strings.locale))} · " +
                strings.quantity(R.plurals.toppers_batch_count_lectures_finished, selected.count, selected.count),
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun CompletionTrendChart(lectures: List<BatchLecture>, today: LocalDate) {
    val strings = rememberBatchStrings()
    val trend = remember(lectures, today) { completionTrend(lectures, today) }
    val last = trend.last()
    val gained = remember(lectures, today) { completionActivity(lectures, today, 14).sumOf { it.count } }
    val pink = Color(0xFFBE185D)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.text(R.string.toppers_batch_lectures_finished_over_time), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(strings.text(R.string.toppers_batch_cumulative_completion_last_14_days),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (last.completed == 0) {
                Text(strings.text(R.string.toppers_batch_finish_a_lecture_to_start_your_progress_graph),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AnimatedCompletionLine(trend, pink, Modifier.fillMaxWidth().height(200.dp), showAxis = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${last.completed}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(strings.text(R.string.toppers_batch_lectures_completed), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = pink.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
                        Text(strings.text(R.string.toppers_batch_count_in_14_days, gained), modifier = Modifier.padding(12.dp),
                            color = pink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Text(strings.text(R.string.toppers_batch_touch_the_chart_to_see_a_day_s_total), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A compact, subject-only view of the same real completion history used on Progress. */
@Composable
internal fun SubjectCompletionTrend(lectures: List<BatchLecture>, today: LocalDate, color: Color) {
    val strings = rememberBatchStrings()
    val trend = remember(lectures, today) { completionTrend(lectures, today) }
    val gained = remember(lectures, today) { completionActivity(lectures, today, 14).sumOf { it.count } }
    val last = trend.last()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.text(R.string.toppers_batch_your_progress_last_14_days), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(strings.quantity(R.plurals.toppers_batch_count_lectures, gained, gained), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = color)
        }
        if (last.completed == 0) {
            Text(strings.text(R.string.toppers_batch_finish_a_lecture_to_start_your_progress_graph),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AnimatedCompletionLine(trend, color, Modifier.fillMaxWidth().height(116.dp), showAxis = false)

        }
    }
}

@Composable
private fun AnimatedCompletionLine(
    trend: List<TrendPoint>,
    color: Color,
    modifier: Modifier,
    showAxis: Boolean,
) {
    val strings = rememberBatchStrings()
    val dateFormat = remember(strings.locale) { DateTimeFormatter.ofPattern("d MMM", strings.locale) }
    com.safarparmar.app.ui.components.analytics.SafarAnalyticsChart(
        values = remember(trend) { trend.map { it.completed } },
        labels = remember(trend, strings.locale) { trend.map { it.date.format(dateFormat) } },
        color = color,
        description = strings.text(R.string.toppers_batch_lecture_completion_history) + trend.joinToString { strings.text(R.string.toppers_batch_count_count_completed, strings.date(it.date.toString()), it.completed) },
        modifier = modifier,
        showAxis = showAxis,
    )
}
