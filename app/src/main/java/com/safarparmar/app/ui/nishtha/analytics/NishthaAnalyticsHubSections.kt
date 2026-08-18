package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

    val overviewThemeColor = primaryText(isLight)
    val goalsThemeColor = if (isLight) Color(0xFF047857) else Color(0xFF4ADE80)
    val focusThemeColor = if (isLight) Color(0xFFC2410C) else Color(0xFFFF8A65)
    val monthlyThemeColor = if (isLight) Color(0xFF581C87) else Color(0xFFC084FC)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(
            title = "Overview",
            subtitle = "Quick read across goals, ekagra, and the monthly review.",
            isLight = isLight,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Timer, "Ekagra Today", formatStudyTime(focusToday), "Ekagra completed today", focusThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.Flag, "Goals Today", "$completedToday/${todayGoals.size}", "Completed against today's goals", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.AutoMirrored.Filled.FormatListBulleted, "Goals Set", totalGoalsSet.toString(), "Total goals you've created", goalsThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.CheckCircle, "Goals Completed", totalGoalsCompleted.toString(), "Total goals finished", goalsThemeColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubMetricCard(Icons.Default.Bolt, "Consistency", report?.let { "${it.consistencyScore.toInt()}%" } ?: "-", "Monthly review preview", monthlyThemeColor, isLight, Modifier.weight(1f))
            HubMetricCard(Icons.Default.TrackChanges, "Ekagra Depth", report?.let { "${it.focusDepth.toInt()}m/day" } ?: "-", "From Monthly Review", focusThemeColor, isLight, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(secondaryText(isLight).copy(alpha = 0.06f))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Analytics Home", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText(isLight))
                Text(
                    "Use Goals for completion patterns, Ekagra for timer depth, and Monthly Review for reflection.",
                    fontSize = 12.sp,
                    color = secondaryText(isLight),
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
    val completedGoals = standardGoals.filter { it.isCompletedForStats() }
    val total = standardGoals.size
    val rate = if (total > 0) kotlin.math.round(completedGoals.size * 100f / total).toInt().coerceIn(0, 100) else 0
    val avgProgress = if (standardGoals.isNotEmpty()) kotlin.math.round(standardGoals.map { it.progressPercent() }.average()).toInt() else 0
    val sevenDaySeries = remember(goals, todayKey) {
        val today = LocalDate.now(IstDateUtils.zone)
        (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val key = day.toString()
            val dayGoals = standardGoals.filter { goal ->
                if (goal.isCompletedForStats()) {
                    goal.completedDateKey() == key
                } else {
                    goal.anchorDateKey() == key
                }
            }
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

    val goalsThemeColor = if (isLight) Color(0xFF047857) else Color(0xFF4ADE80)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(
            title = "Goal Insights",
            subtitle = "Completion insights and goal progress from Nishtha goals.",
            isLight = isLight,
        )
        val greenColor = if (isLight) Color(0xFF045435) else Color(0xFF10B981)
        val indigoColor = if (isLight) Color(0xFF4338CA) else Color(0xFF818CF8)
        val amberColor = if (isLight) Color(0xFFD97706) else Color(0xFFF59E0B)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("GOALS SET", standardGoals.size.toString(), "Total goals created", indigoColor, isLight, Modifier.weight(1f))
            GoalMetricCard("GOALS COMPLETED", standardGoals.count { it.isCompletedForStats() }.toString(), "Total goals finished", greenColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("COMPLETION RATE", "$rate%", "${completedGoals.size} of $total goals completed", greenColor, isLight, Modifier.weight(1f))
            GoalMetricCard("AVERAGE PROGRESS", "$avgProgress%", "Average progress across all goals.", amberColor, isLight, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("CONSISTENCY (7 DAYS)", "$consistencyDays/7", "Days with completed manual goal", indigoColor, isLight, Modifier.weight(1f))
            GoalMetricCard("CURRENT STREAK", "${currentStreak}d", "Consecutive completion days", amberColor, isLight, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(secondaryText(isLight).copy(alpha = 0.06f))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = greenColor, modifier = Modifier.size(18.dp))
                    Column {
                        Text("Goal Consistency Trend", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText(isLight))
                        Text(
                            "Your goal completion over the last 7 days",
                            fontSize = 11.sp,
                            color = secondaryText(isLight)
                        )
                    }
                }
                GoalConsistencyChart(sevenDaySeries, greenColor, isLight)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(secondaryText(isLight).copy(alpha = 0.06f))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = goalsThemeColor, modifier = Modifier.size(16.dp))
                        Text("Weekly Growth Pulse", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText(isLight))
                    }
                    Text(
                        "$averageDailyCompletion avg/day",
                        fontSize = 11.sp,
                        color = secondaryText(isLight)
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

    val accent = if (isLight) Color(0xFFC2410C) else Color(0xFFFF8A65)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(
            title = "Ekagra Insights",
            subtitle = "Focused metrics from Ekagra timer sessions.",
            isLight = isLight,
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
private fun SectionHeading(title: String, subtitle: String, isLight: Boolean) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Text(subtitle, fontSize = 12.sp, color = secondaryText(isLight))
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
            .heightIn(min = 126.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(secondaryText(isLight).copy(alpha = 0.06f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isLight) 0.12f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            }
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText(isLight)
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = primaryText(isLight))
            }
            Text(
                sub,
                fontSize = 11.sp,
                color = secondaryText(isLight),
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
            .heightIn(min = 124.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(secondaryText(isLight).copy(alpha = 0.06f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText(isLight)
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
            }
            Text(
                sub,
                fontSize = 11.sp,
                color = secondaryText(isLight),
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
            .heightIn(min = 114.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(secondaryText(isLight).copy(alpha = 0.06f))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText(isLight)
            )
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f)
                )
            ) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = primaryText(isLight))
            }
            if (!sub.isNullOrBlank()) {
                Text(
                    sub,
                    fontSize = 11.sp,
                    color = secondaryText(isLight),
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
            .clip(RoundedCornerShape(16.dp))
            .background(secondaryText(isLight).copy(alpha = 0.06f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Timer Duration Usage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText(isLight))
            Text(
                "Includes ekagra timers and both break types.",
                fontSize = 11.sp,
                color = secondaryText(isLight)
            )
            if (durationRows.isEmpty()) {
                Text(
                    "No timer duration usage yet.",
                    fontSize = 12.sp,
                    color = secondaryText(isLight)
                )
            } else {
                durationRows.forEachIndexed { index, row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                        Text(
                            timerDurationUsageLabel(row.sessionType, row.durationMinutes),
                            fontSize = 13.sp,
                            color = primaryText(isLight),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            row.count.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText(isLight)
                        )
                    }
                    if (index < durationRows.size - 1) {
                        HorizontalDivider(color = secondaryText(isLight).copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalConsistencyChart(days: List<GoalAnalyticsDay>, defaultThemeColor: Color, isLight: Boolean) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val rangeMax = 50f
    val gridColor = secondaryText(isLight).copy(alpha = 0.12f)
    val axisLabelColor = secondaryText(isLight)

    val greenColor = if (isLight) Color(0xFF045435) else Color(0xFF10B981)
    val indigoColor = if (isLight) Color(0xFF4338CA) else Color(0xFF818CF8)
    val amberColor = if (isLight) Color(0xFFD97706) else Color(0xFFF59E0B)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Enlarged Selection Tooltip Banner
        AnimatedVisibility(
            visible = selectedIndex != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            selectedIndex?.let { idx ->
                val day = days.getOrNull(idx)
                if (day != null) {
                    val pct = if (day.total > 0) day.completed * 100 / day.total else 0
                    val activeColor = when {
                        day.completed == 0 -> if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                        pct >= 70 || day.completed >= 10 -> greenColor
                        pct >= 30 || day.completed >= 5 -> indigoColor
                        else -> amberColor
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeColor.copy(alpha = 0.14f))
                            .border(1.2.dp, activeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(activeColor),
                            )
                            Text(
                                text = "${day.dayLabel}:",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText(isLight),
                            )
                            Text(
                                text = "${day.completed} goal${if (day.completed == 1) "" else "s"} completed",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = activeColor.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "$pct% progress",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        // Enlarged Chart area (190dp height) with Y-axis labels + grid + bars
        Row(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Y-Axis Range Labels Column (50G, 10G, 0G)
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .padding(bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text("50G", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = axisLabelColor)
                Text("10G", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = axisLabelColor)
                Text("0G", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = axisLabelColor)
            }

            Spacer(Modifier.width(8.dp))

            // Bars Container overlaid with Grid Lines
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Grid lines background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor))
                }

                // 7-day Bars Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    days.forEachIndexed { index, day ->
                        val isSelected = selectedIndex == index
                        val pct = if (day.total > 0) day.completed * 100 / day.total else 0
                        // 3-color palette bar logic
                        val dayBarColor = when {
                            day.completed == 0 -> if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
                            pct >= 70 || day.completed >= 10 -> greenColor
                            pct >= 30 || day.completed >= 5 -> indigoColor
                            else -> amberColor
                        }

                        // Calculate bar height ratio based on goals completed up to 50G range max
                        val goalRatio = (day.completed.toFloat() / rangeMax).coerceIn(0.04f, 1f)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // Enlarged Tooltip badge over bar if selected
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (day.completed > 0) dayBarColor else primaryText(isLight))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = "${day.completed}G",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.76f)
                                        .fillMaxHeight(goalRatio)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.completed > 0) {
                                                if (isSelected) dayBarColor else dayBarColor.copy(alpha = 0.85f)
                                            } else {
                                                if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
                                            },
                                        )
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, primaryText(isLight), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                selectedIndex = if (selectedIndex == index) null else index
                                            },
                                        ),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                day.dayLabel.take(1),
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) (if (day.completed > 0) dayBarColor else primaryText(isLight)) else secondaryText(isLight),
                            )
                        }
                    }
                }
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
            .clip(RoundedCornerShape(12.dp))
            .background(secondaryText(isLight).copy(alpha = 0.05f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                entry.dayLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight)
            )
            Text(
                "${entry.completed}/${entry.total} done",
                fontSize = 12.sp,
                color = secondaryText(isLight)
            )
        }
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            color = barColor,
            trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
        )
        Text(
            "Average progress: ${entry.avgProgress}%",
            fontSize = 11.sp,
            color = secondaryText(isLight)
        )
    }
}

private fun primaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

private fun secondaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

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
