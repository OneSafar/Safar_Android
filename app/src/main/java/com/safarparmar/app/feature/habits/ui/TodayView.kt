package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletion
import com.safarparmar.app.performance.LocalMotionPolicy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayView(
    date: LocalDate,
    habits: List<HabitWithCompletion>,
    allHabits: List<HabitEntity> = emptyList(),
    onToggle: (Long, Boolean) -> Unit,
    onEditHabit: (HabitEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    hasHabits: Boolean = true,
    onAdd: () -> Unit = {}
) {
    val done = habits.count { it.completed }
    val left = (habits.size - done).coerceAtLeast(0)
    val percent = if (habits.isEmpty()) 0 else ((done.toFloat() / habits.size) * 100).toInt()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModernPageHeader(
                eyebrow = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                title = "Today's Habits"
            )
        }

        item {
            ModernSoftCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when {
                                habits.isEmpty() -> "No habits today"
                                done == habits.size -> "All done for today"
                                left == 1 -> "1 habit remaining"
                                else -> "$left habits remaining"
                            },
                            color = HabitColors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (habits.isEmpty()) "Enjoy your day off or add a new habit." else "$done of ${habits.size} completed",
                            color = HabitColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    TodayProgressRing(percent)
                }
            }
        }

        item { ModernSectionRow("Today's Habits", "${habits.size} scheduled") }

        item {
            ModernSoftCard(Modifier.fillMaxWidth()) {
                when {
                    habits.isNotEmpty() -> habits.forEachIndexed { index, item ->
                        ModernHabitRow(
                            habit = item.habit,
                            completed = item.completed,
                            onEdit = { onEditHabit(item.habit) },
                            onToggle = { onToggle(item.habit.id, item.completed) }
                        )
                        if (index != habits.lastIndex) HorizontalDivider(color = HabitColors.Outline)
                    }
                    !hasHabits -> ModernEmptyState("No habits yet", "Tap + New Habit below to build your routine.")
                    else -> ModernEmptyState(
                        "No habits for today",
                        "Take a rest day, or tap + New Habit to add one."
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayProgressRing(percent: Int) {
    val trackColor = HabitColors.Parchment
    val progressColor = HabitColors.RoyalPurple
    val motion = LocalMotionPolicy.current
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100).toFloat(),
        animationSpec = tween(if (motion.animationsEnabled) 700 else 0),
        label = "todayProgress",
    )
    Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            drawCircle(
                color = trackColor,
                style = Stroke(width = stroke),
                radius = size.minDimension / 2 - stroke / 2
            )
            if (animatedPercent > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (animatedPercent / 100f),
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text("${animatedPercent.toInt()}%", color = HabitColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}
