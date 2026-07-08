package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.GoalSubtask
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    Card(shape = RoundedCornerShape(14.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun Goal.isDormant(todayKey: String): Boolean {
    if (goalKind != "scheduled") return false
    val key = IstDateUtils.getDateKey(scheduledDate) ?: return false
    return key > todayKey
}

internal fun Goal.goalKindLabel(): String = when (goalKind) {
    "scheduled" -> "Scheduled"
    "repeat" -> "Repeat"
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
internal fun statusBadgeBg(status: String): Color = when (status) {
    "missed", "expired", "cancelled" -> MaterialTheme.colorScheme.error.copy(0.12f)
    "in_progress", "partial" -> Color(0xFFFFB300).copy(alpha = 0.14f)
    "completed" -> Color(0xFF065F46).copy(alpha = 0.12f)
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
internal fun statusBadgeFg(status: String): Color = when (status) {
    "missed", "expired", "cancelled" -> MaterialTheme.colorScheme.error
    "in_progress", "partial" -> Color(0xFFB26A00)
    "completed" -> Color(0xFF065F46)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
