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
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Liquid Glass design system ──────────────────────────────────────────────
import com.safarparmar.app.ui.glass.macOSControlPanel
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.GlassDivider

@Composable
internal fun AnalyticsOverviewSection(
    goals: List<Goal>,
    ekagraAnalytics: EkagraAnalyticsStats,
    report: MonthlyReport?,
) {
    val todayKey = IstDateUtils.todayKey()
    // Fix: use anchor date only so the denominator stays fixed at the number of
    // goals the user planned for today. The previous "|| completedDateKey() == todayKey"
    // clause caused the list to shrink to only the completed goal(s), making the
    // card show "1/1" instead of "1/5" when only some goals were done.
    // Ekagra-sourced goals are excluded (auto-created from focus sessions) to
    // match the same filter used everywhere else in the analytics screen.
    val todayGoals = goals.filter { goal ->
        goal.source != "ekagra" && goal.anchorDateKey() == todayKey
    }
    val completedToday = todayGoals.count { it.isCompletedForStats() }
    val focusToday = ekagraAnalytics.focusSessions
        .filter { IstDateUtils.getDateKey(it.startedAt) == todayKey || IstDateUtils.getDateKey(it.endedAt) == todayKey }
        .sumOf { it.actualMinutes }
    val standardGoalsAllTime = goals.filter { it.source != "ekagra" }
    val totalGoalsSet = standardGoalsAllTime.size
    val totalGoalsCompleted = standardGoalsAllTime.count { it.isCompletedForStats() }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    val overviewThemeColor = if (isLight) Color(0xFF1E3A8A) else Color(0xFF90CAF9)
    val goalsThemeColor = if (isLight) Color(0xFF065F46) else Color(0xFF81C784)
    val focusThemeColor = if (isLight) Color(0xFF9A3412) else Color(0xFFFF8A65)
    val monthlyThemeColor = if (isLight) Color(0xFF5B21B6) else Color(0xFFB39DDB)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Overview",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = overviewThemeColor
        )
        Text(
            "Quick read across goals, ekagra, and the monthly review.",
            fontSize = 12.sp,
            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Timer, "Ekagra Today", formatStudyTime(focusToday), "Ekagra completed today", focusThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.Flag, "Goals Today", "$completedToday/${todayGoals.size}", "Completed against today's goals", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.AutoMirrored.Filled.FormatListBulleted, "Goals Set", totalGoalsSet.toString(), "Total goals you've created", goalsThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.CheckCircle, "Goals Completed", totalGoalsCompleted.toString(), "Total goals you've finished", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Bolt, "Consistency", report?.let { "${it.consistencyScore.toInt()}%" } ?: "-", "Monthly review preview", monthlyThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.TrackChanges, "Ekagra Depth", report?.let { "${it.focusDepth.toInt()}m/day" } ?: "-", "From Monthly Review", focusThemeColor, isLight, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Analytics Home", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = overviewThemeColor)
                Text(
                    "Use Goals for completion patterns, Ekagra for timer depth, and Monthly Review for reflection.",
                    fontSize = 12.sp,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                    lineHeight = 16.sp
                )
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

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    val goalsThemeColor = if (isLight) Color(0xFF065F46) else Color(0xFF81C784)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Goal Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = goalsThemeColor)
        Text(
            "Completion insights and goal progress from Nishtha goals.",
            fontSize = 12.sp,
            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("GOALS SET", standardGoals.size.toString(), "Total goals you've created", goalsThemeColor, isLight, Modifier.weight(1f))
            GoalMetricCard("GOALS COMPLETED", standardGoals.count { it.isCompletedForStats() }.toString(), "Total goals you've finished", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("COMPLETION RATE", "$rate%", "${manualCompletedGoals.size} of $total active manual goals completed", goalsThemeColor, isLight, Modifier.weight(1f))
            GoalMetricCard("AVERAGE PROGRESS", "$avgProgress%", "Future scheduled goals stay excluded until their date arrives.", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("CONSISTENCY (7 DAYS)", "$consistencyDays/7", "Days with at least one completed manual goal", goalsThemeColor, isLight, Modifier.weight(1f))
            GoalMetricCard("CURRENT STREAK", "${currentStreak}d", "Consecutive days with completions", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = goalsThemeColor, modifier = Modifier.size(18.dp))
                    Column {
                        Text("Goal Consistency Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = goalsThemeColor)
                        Text(
                            "Your goal completion over the last 7 days",
                            fontSize = 12.sp,
                            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                        )
                    }
                }
                GoalConsistencyChart(sevenDaySeries, goalsThemeColor, isLight)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = goalsThemeColor, modifier = Modifier.size(16.dp))
                        Text("Weekly Growth Pulse", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = goalsThemeColor)
                    }
                    Text(
                        "$averageDailyCompletion avg/day",
                        fontSize = 12.sp,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                }
                sevenDaySeries.forEach { entry -> WeeklyGrowthRow(entry, goalsThemeColor, isLight) }
            }
        }
    }
}

@Composable
internal fun FocusInsightsSection(analytics: EkagraAnalyticsStats) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    val accent = if (isLight) Color(0xFF9A3412) else Color(0xFFFF8A65)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ekagra Insights", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
        Text(
            "Focused metrics from Ekagra timer sessions.",
            fontSize = 12.sp,
            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Total ekagra time", formatStudyTime(analytics.totalFocusMinutes), null, accent, isLight, Modifier.weight(1f))
            CleanMetricCard("Breaks taken", analytics.breakSessionsCount.toString(), "Short ${analytics.shortBreakSessionsCount}", accent, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard("Average session length", formatStudyTime(analytics.averageTimerMinutes), null, accent, isLight, Modifier.weight(1f))
            CleanMetricCard("Most used duration", analytics.mostUsedTimerDurationMinutes?.let { formatStudyTime(it) } ?: "-", null, accent, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard(
                "Goal-linked time",
                formatStudyTime(analytics.goalLinkedTime),
                "${analytics.goalLinkedSessionCount} session${if (analytics.goalLinkedSessionCount == 1) "" else "s"}",
                accent,
                isLight,
                Modifier.weight(1f),
            )
            CleanMetricCard(
                "Untitled time",
                formatStudyTime(analytics.untitledTime),
                "${analytics.untitledSessionCount} session${if (analytics.untitledSessionCount == 1) "" else "s"}",
                accent,
                isLight,
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanMetricCard(
                "Topic-linked time",
                formatStudyTime(analytics.topicLinkedTime),
                "${analytics.topicLinkedSessionCount} session${if (analytics.topicLinkedSessionCount == 1) "" else "s"} · Study Planner",
                accent,
                isLight,
                Modifier.weight(1f),
            )
        }
        TimerDurationUsageCard(analytics.timerDurationUsage, accent, isLight)
    }
}

@Composable
private fun HubMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    accent: Color,
    isLight: Boolean,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 132.dp)
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isLight) 0.12f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
            Text(
                sub,
                fontSize = 11.sp,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun GoalMetricCard(
    label: String,
    value: String,
    sub: String,
    color: Color,
    isLight: Boolean,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 130.dp)
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = color)
            }
            Text(
                sub,
                fontSize = 11.sp,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun CleanMetricCard(
    label: String,
    value: String,
    sub: String?,
    accent: Color,
    isLight: Boolean,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 118.dp)
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            }
            if (!sub.isNullOrBlank()) {
                Text(
                    sub,
                    fontSize = 11.sp,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TimerDurationUsageCard(rows: List<EkagraTimerDurationUsage>, accent: Color, isLight: Boolean) {
    val durationRows = rows.filter { it.count > 0 }.sortedByDescending { it.count }.take(5)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Timer Duration Usage", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
            Text(
                "Includes ekagra timers and both break types.",
                fontSize = 12.sp,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
            if (durationRows.isEmpty()) {
                Text(
                    "No timer duration usage yet.",
                    fontSize = 13.sp,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                )
            } else {
                durationRows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
                        Text(
                            timerDurationUsageLabel(row.sessionType, row.durationMinutes),
                            fontSize = 13.sp,
                            color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            row.count.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalConsistencyChart(days: List<GoalAnalyticsDay>, barColor: Color, isLight: Boolean) {
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val score = if (day.total > 0) day.completed * 100 / day.total else 0
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth(0.72f)
                            .fillMaxHeight((score.toFloat() / 100f).coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (day.completed > 0) barColor else (if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    day.dayLabel.take(1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WeeklyGrowthRow(entry: GoalAnalyticsDay, barColor: Color, isLight: Boolean) {
    val pct = if (entry.total > 0) entry.completed * 100 / entry.total else 0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLight) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                entry.dayLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
            )
            Text(
                "${entry.completed}/${entry.total} done",
                fontSize = 12.sp,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
        }
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = barColor,
            trackColor = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)
        )
        Text(
            "Average progress: ${entry.avgProgress}%",
            fontSize = 11.sp,
            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
        )
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
