package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.performance.LocalMotionPolicy
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

/**
 * Replicates the blue "Drink Water" card from the reference image:
 * Features a smooth continuous white line sparkline connecting daily points with circular dots
 * and date labels along the x-axis.
 */
@Composable
fun HabitLineSparklineCard(
    title: String,
    progressLabel: String,
    dailyStats: List<DailyHabitStats>,
    today: LocalDate = LocalDate.now(),
    icon: ImageVector = Icons.Rounded.WaterDrop,
    cardColor: Color = Color(0xFF0091EA), // Vibrant blue matching reference
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show the last 18 days
    val recentStats = remember(dailyStats, today) {
        val daysList = mutableListOf<DailyHabitStats>()
        for (i in 17 downTo 0) {
            val date = today.minusDays(i.toLong())
            val stat = dailyStats.find { it.date == date } ?: DailyHabitStats(date, 0, 0, 0f)
            daysList.add(stat)
        }
        daysList
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

        Spacer(Modifier.height(20.dp))

        // Continuous Line Sparkline Canvas with Dots
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = recentStats.size
                if (n < 2) return@Canvas

                val stepX = size.width / (n - 1)
                val topPadding = 6.dp.toPx()
                val bottomPadding = 6.dp.toPx()
                val usableHeight = size.height - topPadding - bottomPadding

                val points = recentStats.mapIndexed { idx, stat ->
                    val x = idx * stepX
                    // completionRate is 0f..1f. Map 1f -> top, 0f -> bottom
                    val y = size.height - bottomPadding - (stat.completionRate.coerceIn(0f, 1f) * usableHeight)
                    Offset(x, y)
                }

                // Draw path line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                // Gradient area under curve
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )

                drawPath(
                    path = linePath,
                    color = Color.White,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw dots at each node
                points.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Date Axis Ticks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show ~3-4 dates along the axis (e.g. 15 days ago, 10 days ago, 5 days ago, Today)
            val markIndices = listOf(0, recentStats.size / 3, (recentStats.size * 2) / 3, recentStats.size - 1)
            markIndices.forEach { idx ->
                val date = recentStats.getOrNull(idx)?.date ?: today
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/**
 * Displays the Weekly Rhythm using the green card design with rounded vertical pill tracks
 * for Monday through Sunday (M, T, W, T, F, S, S).
 */
@Composable
fun HabitWeeklyRhythmCard(
    dailyStats: List<DailyHabitStats>,
    today: LocalDate = LocalDate.now(),
    title: String = "Weekly Rhythm",
    cardColor: Color = Color(0xFF2E9D4E), // Vibrant green matching reference
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )
    val recordedByWeekday = remember(dailyStats, today) {
        weekdays.associateWith { weekday ->
            dailyStats.filter {
                it.date.dayOfWeek == weekday && !it.date.isAfter(today) && it.scheduledCount > 0
            }
        }
    }
    val rates = remember(recordedByWeekday) {
        weekdays.associateWith { weekday ->
            val stats = recordedByWeekday[weekday]
            if (stats.isNullOrEmpty()) 0f else stats.map { it.completionRate }.average().toFloat()
        }
    }
    val avgRate = remember(rates) {
        val nonZero = rates.values.filter { it > 0f }
        if (nonZero.isEmpty()) 0 else (nonZero.average() * 100).toInt()
    }
    val strongestDay = remember(rates) {
        rates.maxByOrNull { it.value }?.takeIf { it.value > 0f }?.key
    }

    val progressLabel = if (strongestDay != null) {
        "Best: ${strongestDay.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    } else if (avgRate > 0) {
        "$avgRate% avg"
    } else {
        "0% avg"
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
                        imageVector = Icons.Rounded.DateRange,
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

        // 7 Weekday Pill Tracks (M, T, W, T, F, S, S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            weekdays.forEach { weekday ->
                val isToday = weekday == today.dayOfWeek
                val rate = rates[weekday]?.coerceIn(0f, 1f) ?: 0f
                val pct = (rate * 100).toInt()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    // Percentage above pill
                    Text(
                        text = if (pct > 0) "$pct%" else "0%",
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) Color.White else Color.White.copy(alpha = 0.70f)
                    )

                    Spacer(Modifier.height(6.dp))

                    // Vertical pill track with filled progress
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (rate > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(rate)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Day of week label underneath (M, T, W, T, F, S, S)
                    Text(
                        text = weekday.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = if (isToday) 14.sp else 12.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isToday) Color.White else Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
