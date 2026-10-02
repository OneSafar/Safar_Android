package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Replicates the green "Reading" card from the reference image:
 * Features a row of vertical rounded pill tracks, filled up to the daily value/completion rate,
 * with day numbers underneath and today highlighted.
 */
@Composable
fun HabitBarHistogramCard(
    title: String,
    progressLabel: String,
    dailyStats: List<DailyHabitStats>,
    today: LocalDate = LocalDate.now(),
    icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook,
    cardColor: Color = Color(0xFF2E9D4E), // Vibrant green matching reference
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show the last 14 to 21 days up to today
    val recentStats = remember(dailyStats, today) {
        val daysList = mutableListOf<DailyHabitStats>()
        for (i in 17 downTo 0) {
            val date = today.minusDays(i.toLong())
            val stat = dailyStats.find { it.date == date } ?: DailyHabitStats(date, 0, 0, 0f)
            daysList.add(stat)
        }
        daysList
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(recentStats.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardColor)
            .padding(18.dp)
    ) {
        // Header: Icon + Title | Progress Label + Edit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = progressLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (onEditClick != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_edit),
                                tint = cardColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Bar Histogram Row (Pill tracks + day numbers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            recentStats.forEach { stat ->
                val isToday = stat.date == today
                val completionRate = stat.completionRate.coerceIn(0f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(16.dp)
                ) {
                    // Vertical pill track with filled progress
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (completionRate > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(completionRate)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Date number underneath
                    Text(
                        text = stat.date.dayOfMonth.toString(),
                        fontSize = if (isToday) 13.sp else 11.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isToday) Color.White else Color.White.copy(alpha = 0.70f)
                    )
                }
            }
        }
    }
}

/** Daily completion rates on a fixed 0–100% scale. Only scheduled days contribute values. */
@Composable
fun HabitLineSparklineCard(
    title: String,
    progressLabel: String,
    dailyStats: List<DailyHabitStats>,
    today: LocalDate = LocalDate.now(),
    icon: ImageVector = Icons.Rounded.WaterDrop,
    cardColor: Color = Color(0xFF0091EA),
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val recentStats = remember(dailyStats, today) {
        dailyStats.filter { !it.date.isAfter(today) && !it.date.isBefore(today.minusDays(17)) && it.scheduledCount > 0 }
            .sortedBy { it.date }
    }
    AnalyticsHabitCard(title, progressLabel, "Daily completion · last 18 days", icon, cardColor, onEditClick, modifier) {
        if (recentStats.isEmpty()) {
            AnalyticsEmptyMessage("Your trend appears after your first scheduled habit day.")
        } else {
            val dates = remember(recentStats) { recentStats.map { it.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) } }
            com.safarparmar.app.ui.components.analytics.SafarAnalyticsChart(
                values = remember(recentStats) { recentStats.map { it.completionRate.coerceIn(0f, 1f) * 100 } },
                labels = dates,
                xValues = remember(recentStats) { recentStats.map { java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(17), it.date) } },
                color = cardColor,
                percent = true,
                description = recentStats.joinToString { "${it.date}: ${(it.completionRate * 100).toInt()} percent completed" },
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Text("Touch the chart to inspect a day", style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Weekday averages include every scheduled day, including zero completion. */
@Composable
fun HabitWeeklyRhythmCard(
    dailyStats: List<DailyHabitStats>,
    today: LocalDate = LocalDate.now(),
    title: String = "Weekly Rhythm",
    cardColor: Color = Color(0xFF2E9D4E),
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val weekdays = remember { DayOfWeek.values().toList() }
    val recorded = remember(dailyStats, today) { dailyStats.filter { !it.date.isAfter(today) && it.scheduledCount > 0 } }
    val rates = remember(recorded) {
        weekdays.map { day -> recorded.filter { it.date.dayOfWeek == day }.let { stats ->
            if (stats.isEmpty()) 0f else stats.map { it.completionRate.coerceIn(0f, 1f) }.average().toFloat() * 100
        } }
    }
    val recordedWeekdayRates = weekdays.indices.filter { index -> recorded.any { it.date.dayOfWeek == weekdays[index] } }.map { rates[it] }
    val avg = if (recordedWeekdayRates.isEmpty()) 0 else recordedWeekdayRates.average().toInt()
    AnalyticsHabitCard(title, "$avg% avg", "Average completion by weekday", Icons.Rounded.DateRange, cardColor, onEditClick, modifier) {
        if (recorded.isEmpty()) {
            AnalyticsEmptyMessage("Complete your scheduled habits to discover your weekly rhythm.")
        } else {
            com.safarparmar.app.ui.components.analytics.SafarAnalyticsChart(
                values = rates,
                labels = weekdays.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                color = cardColor,
                columns = true,
                percent = true,
                description = weekdays.indices.joinToString { "${weekdays[it]}: ${rates[it].toInt()} percent average" },
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )
            val best = rates.indices.maxByOrNull { rates[it] }?.takeIf { rates[it] > 0 }
            Text(if (best == null) "Every scheduled day counts. Start with one completion today."
                else "Strongest day: ${weekdays[best].getDisplayName(TextStyle.FULL, Locale.getDefault())} · ${rates[best].toInt()}%",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnalyticsHabitCard(
    title: String, metric: String, subtitle: String, icon: ImageVector, accent: Color,
    onEditClick: (() -> Unit)?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.20f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onEditClick != null) IconButton(onClick = onEditClick) {
                    Icon(Icons.Rounded.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_edit), tint = accent)
                }
            }
            Text(metric, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
private fun AnalyticsEmptyMessage(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
}
