/* Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 */
package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.ui.HabitColors
import com.safarparmar.app.performance.LocalMotionPolicy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekdayRhythmChart(
    dailyStats: List<DailyHabitStats>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val weekdays = DayOfWeek.entries
    val recordedByWeekday = remember(dailyStats, today) {
        weekdays.associateWith { weekday ->
            dailyStats.filter {
                it.date.dayOfWeek == weekday && !it.date.isAfter(today) && it.scheduledCount > 0
            }
        }
    }
    val rates = remember(recordedByWeekday) {
        recordedByWeekday.mapValues { (_, values) ->
            if (values.isEmpty()) 0f else values.map { it.completionRate }.average().toFloat()
        }
    }
    val strongest = rates.maxByOrNull { it.value }?.takeIf { it.value > 0f }?.key
    var selectedWeekday by remember { mutableStateOf<DayOfWeek?>(null) }
    val inspectedWeekday = selectedWeekday ?: strongest

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HabitColors.Surface)
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text("Weekly rhythm", color = HabitColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Average completion by weekday",
            color = HabitColors.TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(154.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            weekdays.forEach { weekday ->
                WeekdayBar(
                    label = weekday.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    rate = rates.getValue(weekday),
                    highlighted = weekday == strongest,
                    selected = weekday == inspectedWeekday,
                    onClick = { selectedWeekday = weekday },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        inspectedWeekday?.let { weekday ->
            val rate = rates.getValue(weekday)
            val activeDays = recordedByWeekday.getValue(weekday).size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HabitColors.Parchment)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekday.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    color = HabitColors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(rate * 100).toInt()}% average • $activeDays active ${if (activeDays == 1) "day" else "days"}",
                    color = if (weekday == strongest) HabitColors.DeepOrange else HabitColors.RoyalPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WeekdayBar(
    label: String,
    rate: Float,
    highlighted: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionPolicy.current
    val animatedRate by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(if (motion.decorationsEnabled) 420 else 0),
        label = "weekdayBar",
    )
    val trackColor = HabitColors.Parchment
    val fillColor = if (highlighted) HabitColors.DeepOrange else HabitColors.RoyalPurple
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) HabitColors.RoyalPurpleBg.copy(alpha = 0.45f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${(animatedRate * 100).toInt()}%",
            color = if (highlighted) HabitColors.DeepOrange else HabitColors.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .width(18.dp)
        ) {
            val radius = size.width / 2f
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(radius, radius)
            )
            if (animatedRate > 0f) {
                val fillHeight = size.height * animatedRate
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(0f, size.height - fillHeight),
                    size = Size(size.width, fillHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = HabitColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
