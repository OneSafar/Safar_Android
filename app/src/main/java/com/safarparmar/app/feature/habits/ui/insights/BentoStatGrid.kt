package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.ui.HabitColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BentoStatGrid(
    currentStreak: Int,
    bestStreak: Int,
    startedDate: LocalDate?,
    daysSinceStarted: Long,
    totalCompleted: Int,
    completionRate: Float,
    totalMissed: Int,
    missedRate: Float,
    modifier: Modifier = Modifier
) {
    val completionPercentage = (completionRate * 100f).toInt()
    val missedPercentage = (missedRate * 100f).toInt()
    val startedText = when {
        startedDate == null -> "Not started"
        daysSinceStarted == 0L -> "Started today"
        daysSinceStarted == 1L -> "1 day ago"
        daysSinceStarted < 30L -> "$daysSinceStarted days ago"
        daysSinceStarted < 365L -> "${daysSinceStarted / 30} mos ago"
        else -> "${daysSinceStarted / 365} yrs ago"
    }
    val startedSubtext = startedDate?.let {
        "Since " + it.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    } ?: "No data yet"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Current Streak & Started
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoStatCard(
                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_current_streak),
                value = if (currentStreak == 1) "1 day" else "$currentStreak days",
                subtext = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_best_days, bestStreak),
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = if (currentStreak > 0) HabitColors.DeepOrange else HabitColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )

            BentoStatCard(
                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_started),
                value = startedText,
                subtext = startedSubtext,
                icon = Icons.Rounded.CalendarToday,
                iconTint = HabitColors.RoyalPurple,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Completions & Missed Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoStatCard(
                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_completions),
                value = "$totalCompleted done",
                subtext = "$completionPercentage% success rate",
                icon = Icons.Rounded.CheckCircle,
                iconTint = HabitColors.CheckDone,
                modifier = Modifier.weight(1f)
            )

            BentoStatCard(
                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_missed_days),
                value = "$totalMissed missed",
                subtext = "$missedPercentage% of scheduled",
                icon = Icons.Rounded.HighlightOff,
                iconTint = if (totalMissed > 0) HabitColors.DeepOrange.copy(alpha = 0.85f) else HabitColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoStatCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary,
                maxLines = 1
            )
            Text(
                text = subtext,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}
