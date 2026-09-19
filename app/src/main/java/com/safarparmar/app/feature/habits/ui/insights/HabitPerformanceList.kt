package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitPerformance
import com.safarparmar.app.feature.habits.ui.HabitColors

@Composable
fun HabitPerformanceList(
    performanceList: List<HabitPerformance>,
    modifier: Modifier = Modifier
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary
    val textTertiary = HabitColors.TextTertiary
    val purple = HabitColors.RoyalPurple
    val checkDone = HabitColors.CheckDone

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Habit Performance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Completion rates and streak records",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (performanceList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habit performance data recorded",
                    fontSize = 13.sp,
                    color = textTertiary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                performanceList.forEach { perf ->
                    HabitPerformanceRow(
                        performance = perf,
                        outlineColor = outlineColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        purple = purple,
                        checkDone = checkDone
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitPerformanceRow(
    performance: HabitPerformance,
    outlineColor: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    textTertiary: androidx.compose.ui.graphics.Color,
    purple: androidx.compose.ui.graphics.Color,
    checkDone: androidx.compose.ui.graphics.Color
) {
    val percentage = (performance.completionRate * 100f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HabitColors.Parchment)
            .border(1.dp, outlineColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = performance.habitName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (performance.isArchived) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Archived",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = textTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(outlineColor.copy(alpha = 0.4f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = "$percentage%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 80) checkDone else purple
            )
        }

        Spacer(Modifier.height(8.dp))

        // Progress bar (clean, avoids M3 stop indicator dot artifacts at 0%)
        val progressFraction = performance.completionRate.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(outlineColor.copy(alpha = 0.35f))
        ) {
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (percentage >= 80) checkDone else purple)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Streak numbers: Current: 12 days | Best: 31 days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${performance.completedCount}/${performance.scheduledCount} done",
                fontSize = 11.sp,
                color = textSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current: ${performance.currentStreak}d",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
                Text(
                    text = "Best: ${performance.bestStreak}d",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = purple
                )
            }
        }
    }
}
