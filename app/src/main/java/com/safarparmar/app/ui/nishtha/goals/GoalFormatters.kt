package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.util.IstDateUtils

internal fun formatStudyTime(mins: Int): String {
    if (mins <= 0) return "0m"
    val hours = mins / 60
    val minutes = mins % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun Goal.isCompletedForStats(): Boolean =
    completed || !completedAt.isNullOrBlank()

internal fun Goal.completedDateKey(): String? =
    IstDateUtils.getDateKey(completedAt)

internal fun Goal.anchorDateKey(): String? =
    IstDateUtils.getDateKey(scheduledDate)
        ?: IstDateUtils.getDateKey(createdAt)
        ?: IstDateUtils.getDateKey(startedAt)

internal fun Goal.statusBucket(): String = when {
    status == "cancelled" -> "cancelled"
    status == "missed" || status == "expired" -> "missed"
    status == "partial" -> "partial"
    isCompletedForStats() -> "completed"
    else -> "open"
}

@Composable
internal fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Primary)
        Text(label, fontSize = 11.sp, color = GoalsFlatColors.Muted)
    }
}

internal fun Goal.isDormant(todayKey: String): Boolean {
    if (goalKind != "scheduled") return false
    val key = IstDateUtils.getDateKey(scheduledDate) ?: return false
    return key > todayKey
}

internal fun Goal.goalKindLabel(): String = when (goalKind) {
    "scheduled" -> "Scheduled"
    "repeat" -> "Daily"
    "one_time" -> "One-time"
    else -> "Today"
}

internal fun Goal.unitTypeLabel(): String = when (unitType) {
    "duration_minutes" -> "Time"
    "count" -> "Count"
    "checklist" -> "Checklist"
    else -> "Done"
}

internal fun Goal.statusLabel(): String = when (if (completed) "completed" else status) {
    "in_progress" -> "In progress"
    "partial" -> "Partial"
    "missed" -> "Missed"
    "cancelled" -> "Cancelled"
    "expired" -> "Expired"
    "rolled_over" -> "Rolled over"
    "completed" -> "Completed"
    else -> "Not started"
}

internal fun Goal.progressPercent(): Int {
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

internal fun Goal.progressLabel(): String = when (unitType) {
    "duration_minutes" -> {
        val target = targetValue ?: plannedFocusMinutes
        if (target != null && target > 0) "Progress $achievedValue / ${target}m" else "Progress ${achievedValue}m"
    }
    "count" -> if ((targetValue ?: 0) > 0) "Progress $achievedValue / $targetValue" else "Progress $achievedValue"
    "checklist" -> "${subtasks.count { it.done }} / ${subtasks.size} subtasks"
    else -> ""
}

@Composable
internal fun statusBadgeBg(status: String): Color {
    return when (status) {
        "missed", "expired", "cancelled" -> GoalsFlatColors.Danger.copy(0.12f)
        "in_progress", "partial" -> GoalsFlatColors.Amber.copy(alpha = 0.14f)
        "completed" -> GoalsFlatColors.Done.copy(alpha = 0.12f)
        else -> GoalsFlatColors.Hairline.copy(alpha = 0.5f)
    }
}

@Composable
internal fun statusBadgeFg(status: String): Color {
    return when (status) {
        "missed", "expired", "cancelled" -> GoalsFlatColors.Danger
        "in_progress", "partial" -> GoalsFlatColors.Amber
        "completed" -> GoalsFlatColors.Done
        else -> GoalsFlatColors.Muted
    }
}
