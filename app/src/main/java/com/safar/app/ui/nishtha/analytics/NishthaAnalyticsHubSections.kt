package com.safar.app.ui.nishtha.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.domain.model.EkagraAnalyticsStats
import com.safar.app.domain.model.EkagraTimerDurationUsage
import com.safar.app.domain.model.Goal
import com.safar.app.domain.model.MonthlyReport
import com.safar.app.util.IstDateUtils
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Overview", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Quick read across goals, focus, and the monthly review.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Timer, "Focus Today", formatStudyTime(focusToday), "Ekagra focus completed today", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            HubMetricCard(Icons.Default.Flag, "Goals Today", "$completedToday/${todayGoals.size}", "Completed against today's goals", Color(0xFF10A968), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Bolt, "Consistency", report?.let { "${it.consistencyScore.toInt()}%" } ?: "-", "Monthly review preview", Color(0xFFFFB300), Modifier.weight(1f))
            HubMetricCard(Icons.Default.TrackChanges, "Focus Depth", report?.let { "${it.totalFocusMinutes}m/day" } ?: "-", "From Monthly Review", Color(0xFF4F46E5), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Analytics Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Use Goals for completion patterns, Focus for timer depth, Sessions for the work log, and Monthly Review for reflection.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Goal Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Completion insights and goal progress from Nishtha goals.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("COMPLETION RATE", "$rate%", "${manualCompletedGoals.size} of $total active manual goals completed", Color(0xFF10A968), Modifier.weight(1f))
            GoalMetricCard("AVERAGE PROGRESS", "$avgProgress%", "Future scheduled goals stay excluded until their date arrives.", Color(0xFF0EA5E9), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("CONSISTENCY (7 DAYS)", "$consistencyDays/7", "Days with at least one completed manual goal", Color(0xFF8B5CF6), Modifier.weight(1f))
            GoalMetricCard("CURRENT STREAK", "${currentStreak}d", "Consecutive days with completions", Color(0xFFFFB300), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10A968), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Goal Consistency Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Weekly Growth Pulse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    val accent = MaterialTheme.colorScheme.primary
    val linkedSessionCount = analytics.focusSessions.count { !it.associatedGoalId.isNullOrBlank() }
    val freeFocusSessionCount = analytics.focusSessions.count { it.associatedGoalId.isNullOrBlank() }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Focus Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Focused metrics from Ekagra timer sessions.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Total focus time", formatStudyTime(analytics.totalFocusMinutes), null, accent, Modifier.weight(1f))
            CleanMetricCard("Breaks taken", analytics.breakSessionsCount.toString(), "Short ${analytics.shortBreakSessionsCount} | Long ${analytics.longBreakSessionsCount}", accent, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Average session length", formatStudyTime(analytics.averageTimerMinutes), null, accent, Modifier.weight(1f))
            CleanMetricCard("Most used duration", analytics.mostUsedTimerDurationMinutes?.let { formatStudyTime(it) } ?: "-", null, accent, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Goal-linked focus", linkedSessionCount.toString(), "Saved sessions assigned to goals", accent, Modifier.weight(1f))
            CleanMetricCard("Free focus", freeFocusSessionCount.toString(), "Saved sessions without a goal", accent, Modifier.weight(1f))
        }
        TimerDurationUsageCard(analytics.timerDurationUsage, accent)
    }
}

@Composable
internal fun SessionHistorySection(analytics: EkagraAnalyticsStats) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Text("Session History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text("${analytics.focusSessions.size} sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("All closed focus sessions are listed here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (analytics.focusSessions.isEmpty()) {
                    Text("No focus sessions available yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    analytics.focusSessions.forEach { session ->
                        FocusSessionRow(
                            title = session.taskText ?: "Unlabeled task",
                            meta = "${formatDateTime(session.endedAt ?: session.startedAt)} - Planned ${session.durationMinutes}m - Actual ${session.actualMinutes}m${if (session.pauseCount > 0) " - ${session.pauseCount} pauses" else ""}",
                            accent = accent,
                            badge = if (session.associatedGoalId.isNullOrBlank()) "Free Focus" else "Goal Focus",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubMetricCard(icon: ImageVector, label: String, value: String, sub: String, accent: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 132.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalMetricCard(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 130.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleanMetricCard(label: String, value: String, sub: String?, accent: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = modifier.heightIn(min = 118.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label.uppercase(Locale.US), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            if (!sub.isNullOrBlank()) Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimerDurationUsageCard(rows: List<EkagraTimerDurationUsage>, accent: Color) {
    val durationRows = rows.filter { it.count > 0 }.sortedByDescending { it.count }.take(5)
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Timer Duration Usage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Includes focus timers and both break types.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .background(if (day.completed > 0) Color(0xFF10A968) else MaterialTheme.colorScheme.surfaceVariant)
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
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF10A968), trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
        Text("Average progress: ${entry.avgProgress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FocusSessionRow(title: String, meta: String, accent: Color, badge: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = 0.12f)) {
                    Text(badge, fontSize = 9.sp, color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
        else -> "Focus $duration"
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
