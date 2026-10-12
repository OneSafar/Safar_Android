/* Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 */
package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.ui.HabitColors
import com.safarparmar.app.performance.LocalMotionPolicy
import java.time.LocalDate
import kotlin.math.roundToInt

private data class WeekRing(
    val number: Int,
    val firstDay: Int,
    val lastDay: Int,
    val completed: Int,
    val scheduled: Int,
    val containsToday: Boolean,
) {
    val progress: Float
        get() = if (scheduled == 0) 0f else completed.toFloat() / scheduled
}

@Composable
fun ProgressChart(
    dailyStats: List<DailyHabitStats>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val visibleDays = remember(dailyStats, today) {
        dailyStats.filterNot { it.date.isAfter(today) }
    }
    val weekRings = remember(visibleDays, today) {
        visibleDays
            .groupBy { (it.date.dayOfMonth - 1) / 7 }
            .toSortedMap()
            .map { (weekIndex, days) ->
                WeekRing(
                    number = weekIndex + 1,
                    firstDay = days.first().date.dayOfMonth,
                    lastDay = days.last().date.dayOfMonth,
                    completed = days.sumOf { it.completedCount },
                    scheduled = days.sumOf { it.scheduledCount },
                    containsToday = days.any { it.date == today },
                )
            }
    }
    val totalScheduled = visibleDays.sumOf { it.scheduledCount }
    val totalCompleted = visibleDays.sumOf { it.completedCount }
    val overallProgress = if (totalScheduled == 0) 0f else totalCompleted.toFloat() / totalScheduled
    val targetPercentage = (overallProgress * 100f).roundToInt()
    val weekDescriptions = mutableListOf<String>()
    for (week in weekRings) {
        weekDescriptions += androidx.compose.ui.res.stringResource(
            com.safarparmar.app.R.string.habits_week_completion_a11y,
            week.number,
            (week.progress * 100).roundToInt(),
        )
    }
    val chartDescription = androidx.compose.ui.res.stringResource(
        com.safarparmar.app.R.string.habits_monthly_completion_a11y,
        targetPercentage,
    ) + " " + weekDescriptions.joinToString()
    val motion = LocalMotionPolicy.current
    val reveal = remember { Animatable(0f) }
    val percentage by animateIntAsState(
        targetValue = targetPercentage,
        animationSpec = tween(if (motion.animationsEnabled) 550 else 0),
        label = "concentricMonthlyPercentage",
    )

    LaunchedEffect(weekRings) {
        reveal.snapTo(0f)
        if (motion.animationsEnabled) reveal.animateTo(1f, tween(650)) else reveal.snapTo(1f)
    }

    val purple = HabitColors.RoyalPurple
    val purple2 = HabitColors.RoyalPurple2
    val orange = HabitColors.DeepOrange
    val ringTrack = HabitColors.Outline.copy(alpha = 0.58f)
    val ringColors = remember(purple, purple2, orange, weekRings) {
        weekRings.mapIndexed { index, week ->
            when {
                week.containsToday -> orange
                index % 2 == 0 -> purple
                else -> purple2
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HabitColors.Surface)
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_monthly_rhythm),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = HabitColors.TextPrimary,
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_ring_explanation),
            fontSize = 12.sp,
            color = HabitColors.TextSecondary,
        )

        Spacer(Modifier.height(18.dp))

        if (totalScheduled == 0) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_none_scheduled_period),
                    fontSize = 13.sp,
                    color = HabitColors.TextTertiary,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(224.dp)
                    .semantics { contentDescription = chartDescription },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(218.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val ringGap = 7.dp.toPx()
                    val outerDiameter = minOf(size.width, size.height) - strokeWidth

                    weekRings.forEachIndexed { index, week ->
                        val diameter = outerDiameter - index * (strokeWidth + ringGap) * 2f
                        if (diameter <= strokeWidth) return@forEachIndexed
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            x = (size.width - diameter) / 2f,
                            y = (size.height - diameter) / 2f,
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                        drawArc(
                            color = ringTrack,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                        if (week.progress > 0f) {
                            drawArc(
                                color = ringColors[index],
                                startAngle = -90f,
                                sweepAngle = 360f * week.progress.coerceIn(0f, 1f) * reveal.value,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabitColors.TextPrimary,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_this_month),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HabitColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            WeekRingLegend(weekRings = weekRings, colors = ringColors)
        }
    }
}

@Composable
private fun WeekRingLegend(
    weekRings: List<WeekRing>,
    colors: List<Color>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weekRings.chunked(3).forEachIndexed { rowIndex, weeks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weeks.forEachIndexed { itemIndex, week ->
                    val index = rowIndex * 3 + itemIndex
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors[index]),
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_week_progress, week.number, (week.progress * 100).roundToInt()),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HabitColors.TextPrimary,
                            )
                            Text(
                                text = "${week.firstDay}–${week.lastDay}",
                                fontSize = 9.sp,
                                color = HabitColors.TextTertiary,
                            )
                        }
                    }
                }
                repeat(3 - weeks.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
