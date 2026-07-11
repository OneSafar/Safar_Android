package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.EkagraTimerDurationUsage
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.MonthlyReport
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun AnalyticsOverviewSection(
    goals: List<Goal>,
    ekagraAnalytics: EkagraAnalyticsStats,
    report: MonthlyReport?,
) {
    val todayKey = IstDateUtils.todayKey()
    val todayGoals = goals.filter { goal ->
        goal.anchorDateKey() == todayKey || goal.completedDateKey() == todayKey
    }
    val completedToday = todayGoals.count { it.isCompletedForStats() }
    val focusToday = ekagraAnalytics.focusSessions
        .filter { IstDateUtils.getDateKey(it.startedAt) == todayKey || IstDateUtils.getDateKey(it.endedAt) == todayKey }
        .sumOf { it.actualMinutes }
    val standardGoalsAllTime = goals.filter { it.source != "ekagra" }
    val totalGoalsSet = standardGoalsAllTime.size
    val totalGoalsCompleted = standardGoalsAllTime.count { it.isCompletedForStats() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Overview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A)))
        Text("Quick read across goals, ekagra, and the monthly review.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Timer, "Ekagra Today", formatStudyTime(focusToday), "Ekagra ekagra completed today", Color(0xFF9A3412), Modifier.weight(1f))
            HubMetricCard(Icons.Default.Flag, "Goals Today", "$completedToday/${todayGoals.size}", "Completed against today's goals", Color(0xFF065F46), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.AutoMirrored.Filled.FormatListBulleted, "Goals Set", totalGoalsSet.toString(), "Total goals you've created", Color(0xFF065F46), Modifier.weight(1f))
            HubMetricCard(Icons.Default.CheckCircle, "Goals Completed", totalGoalsCompleted.toString(), "Total goals you've finished", Color(0xFF065F46), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Bolt, "Consistency", report?.let { "${it.consistencyScore.toInt()}%" } ?: "-", "Monthly review preview", Color(0xFF5B21B6), Modifier.weight(1f))
            HubMetricCard(Icons.Default.TrackChanges, "Ekagra Depth", report?.let { "${it.totalFocusMinutes}m/day" } ?: "-", "From Monthly Review", Color(0xFF9A3412), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Analytics Home", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E3A8A))
                Text("Use Goals for completion patterns, Ekagra for timer depth, and Monthly Review for reflection.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun GoalInsightsSection(goals: List<Goal>) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val activeGoals = standardGoals.filter { !it.isDormant(todayKey) }
    val total = activeGoals.size
    val rate = if (total > 0) kotlin.math.round(manualCompletedGoals.size * 100f / total).toInt() else 0
    val avgProgress = if (activeGoals.isNotEmpty()) kotlin.math.round(activeGoals.map { it.progressPercent() }.average()).toInt() else 0
    val sevenDaySeries = remember(goals, todayKey) {
        val today = LocalDate.now(IstDateUtils.zone)
        (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val key = day.toString()
            val dayGoals = standardGoals.filter { goal -> goal.anchorDateKey() == key }
            val done = dayGoals.count { goal -> goal.statusBucket() == "completed" }
            val avg = if (dayGoals.isNotEmpty()) kotlin.math.round(dayGoals.map { it.progressPercent() }.average()).toInt() else 0
            GoalAnalyticsDay(day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), key, done, dayGoals.size, avg)
        }
    }
    val consistencyDays = sevenDaySeries.count { it.completed > 0 }
    val currentStreak = sevenDaySeries.asReversed().takeWhile { it.completed > 0 }.size
    val averageDailyCompletion = if (sevenDaySeries.isNotEmpty()) {
        (sevenDaySeries.sumOf { it.completed }.toFloat() / sevenDaySeries.size).let { "%.1f".format(Locale.US, it) }
    } else "0.0"

    val goalsThemeColor = Color(0xFF065F46)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Goal Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = goalsThemeColor))
        Text("Completion insights and goal progress from Nishtha goals.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("GOALS SET", standardGoals.size.toString(), "Total goals you've created", goalsThemeColor, Modifier.weight(1f))
            GoalMetricCard("GOALS COMPLETED", standardGoals.count { it.isCompletedForStats() }.toString(), "Total goals you've finished", goalsThemeColor, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("COMPLETION RATE", "$rate%", "${manualCompletedGoals.size} of $total active manual goals completed", goalsThemeColor, Modifier.weight(1f))
            GoalMetricCard("AVERAGE PROGRESS", "$avgProgress%", "Future scheduled goals stay excluded until their date arrives.", goalsThemeColor, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("CONSISTENCY (7 DAYS)", "$consistencyDays/7", "Days with at least one completed manual goal", goalsThemeColor, Modifier.weight(1f))
            GoalMetricCard("CURRENT STREAK", "${currentStreak}d", "Consecutive days with completions", goalsThemeColor, Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = goalsThemeColor, modifier = Modifier.size(18.dp))
                    Column {
                        Text("Goal Consistency Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = goalsThemeColor)
                        Text("Your goal completion over the last 7 days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                GoalConsistencyChart(sevenDaySeries)
            }
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = goalsThemeColor, modifier = Modifier.size(16.dp))
                        Text("Weekly Growth Pulse", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = goalsThemeColor)
                    }
                    Text("$averageDailyCompletion avg/day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                sevenDaySeries.forEach { entry -> WeeklyGrowthRow(entry) }
            }
        }
    }
}

@Composable
internal fun FocusInsightsSection(analytics: EkagraAnalyticsStats) {
    val accent = Color(0xFF9A3412)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ekagra Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = accent))
        Text("Focused metrics from Ekagra timer sessions.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Total ekagra time", formatStudyTime(analytics.totalFocusMinutes), null, accent, Modifier.weight(1f))
            CleanMetricCard("Breaks taken", analytics.breakSessionsCount.toString(), "Short ${analytics.shortBreakSessionsCount}", accent, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Average session length", formatStudyTime(analytics.averageTimerMinutes), null, accent, Modifier.weight(1f))
            CleanMetricCard("Most used duration", analytics.mostUsedTimerDurationMinutes?.let { formatStudyTime(it) } ?: "-", null, accent, Modifier.weight(1f))
        }
        // Time breakdown — straight from the backend's goalLinkedTime/untitledTime
        // aggregation, no client-side re-derivation.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard(
                "Goal-linked time",
                formatStudyTime(analytics.goalLinkedTime),
                "${analytics.goalLinkedSessionCount} session${if (analytics.goalLinkedSessionCount == 1) "" else "s"}",
                accent,
                Modifier.weight(1f),
            )
            CleanMetricCard(
                "Untitled time",
                formatStudyTime(analytics.untitledTime),
                "${analytics.untitledSessionCount} session${if (analytics.untitledSessionCount == 1) "" else "s"}",
                accent,
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard(
                "Topic-linked time",
                formatStudyTime(analytics.topicLinkedTime),
                "${analytics.topicLinkedSessionCount} session${if (analytics.topicLinkedSessionCount == 1) "" else "s"} · Study Planner",
                accent,
                Modifier.weight(1f),
            )
        }
        TimerDurationUsageCard(analytics.timerDurationUsage, accent)
    }
}

@Composable
private fun HubMetricCard(icon: ImageVector, label: String, value: String, sub: String, accent: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 132.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalMetricCard(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 130.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color)
            }
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleanMetricCard(label: String, value: String, sub: String?, accent: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = modifier.heightIn(min = 118.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label.uppercase(Locale.US), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
            if (!sub.isNullOrBlank()) Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimerDurationUsageCard(rows: List<EkagraTimerDurationUsage>, accent: Color) {
    val durationRows = rows.filter { it.count > 0 }.sortedByDescending { it.count }.take(5)
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Timer Duration Usage", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
            Text("Includes ekagra timers and both break types.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (durationRows.isEmpty()) {
                Text("No timer duration usage yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                durationRows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
                        Text(timerDurationUsageLabel(row.sessionType, row.durationMinutes), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(row.count.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalConsistencyChart(days: List<GoalAnalyticsDay>) {
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val score = if (day.total > 0) day.completed * 100 / day.total else 0
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth(0.72f)
                            .fillMaxHeight((score.toFloat() / 100f).coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (day.completed > 0) Color(0xFF065F46) else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(day.dayLabel.take(1), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeeklyGrowthRow(entry: GoalAnalyticsDay) {
    val pct = if (entry.total > 0) entry.completed * 100 / entry.total else 0
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.dayLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${entry.completed}/${entry.total} done", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF065F46), trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
        Text("Average progress: ${entry.avgProgress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private data class GoalAnalyticsDay(
    val dayLabel: String,
    val dayKey: String,
    val completed: Int,
    val total: Int,
    val avgProgress: Int,
)

private fun timerDurationUsageLabel(sessionType: String, durationMinutes: Int): String {
    val duration = "${durationMinutes.coerceAtLeast(0)}m"
    return when (sessionType) {
        "short_break" -> "Short break $duration"
        "long_break" -> "Long break $duration"
        else -> "Ekagra $duration"
    }
}

private fun formatStudyTime(mins: Int): String {
    if (mins <= 0) return "0m"
    val hours = mins / 60
    val minutes = mins % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return value.replace("T", " ").take(16)
}

private fun Goal.isCompletedForStats(): Boolean =
    completed || !completedAt.isNullOrBlank()

private fun Goal.completedDateKey(): String? =
    IstDateUtils.getDateKey(completedAt)

private fun Goal.anchorDateKey(): String? =
    IstDateUtils.getDateKey(scheduledDate)
        ?: IstDateUtils.getDateKey(createdAt)
        ?: IstDateUtils.getDateKey(startedAt)

private fun Goal.statusBucket(): String = when {
    status == "cancelled" -> "cancelled"
    status == "missed" || status == "expired" -> "missed"
    status == "partial" -> "partial"
    isCompletedForStats() -> "completed"
    else -> "open"
}

private fun Goal.isDormant(todayKey: String): Boolean {
    if (goalKind != "scheduled") return false
    val key = IstDateUtils.getDateKey(scheduledDate) ?: return false
    return key > todayKey
}

private fun Goal.progressPercent(): Int {
    if (completed) return 100
    if (unitType == "checklist") {
        if (subtasks.isEmpty()) return 0
        return ((subtasks.count { it.done }.toFloat() / subtasks.size) * 100).toInt().coerceIn(0, 100)
    }
    if (unitType == "binary") return if (achievedValue > 0) 100 else 0
    val target = targetValue ?: plannedFocusMinutes ?: 0
    if (target <= 0) return 0
    return ((achievedValue.toFloat() / target) * 100).toInt().coerceIn(0, 100)
}
