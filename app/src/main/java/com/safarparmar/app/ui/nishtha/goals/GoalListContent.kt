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

@Composable
internal fun GoalsTab(
    goals: List<Goal>,
    rolloverPrompts: List<Goal>,
    ekagraAnalytics: com.safarparmar.app.domain.model.EkagraAnalyticsStats,
    isLoading: Boolean,
    goalError: String? = null,
    onRefresh: () -> Unit = {},
    onAddClick: () -> Unit,
    onComplete: (Goal) -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    onRepeat: (Goal) -> Unit,
    onRolloverRetry: (Goal) -> Unit,
    onRolloverArchive: (Goal) -> Unit,
) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = goals
        .filter { !it.completed && it.source != "ekagra" && it.lifecycleStatus !in listOf("abandoned", "rolled_over") && !it.isDormant(todayKey) }
        .sortedBy { it.startedAt ?: it.createdAt ?: it.scheduledDate ?: "" }
    val scheduled = goals
        .filter { !it.completed && it.isDormant(todayKey) }
        .sortedBy { it.scheduledDate ?: "" }
    val completed = standardGoals.filter { it.completed }.sortedByDescending { it.completedAt ?: it.createdAt ?: "" }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val todayManualGoals = standardGoals.filter { !it.completedViaFocus && it.anchorDateKey() == todayKey }
    val doneToday = manualCompletedGoals.count { it.completedDateKey() == todayKey }
    val completionRate = if (standardGoals.isNotEmpty()) (manualCompletedGoals.size * 100 / standardGoals.size) else 0
    val dailyProgress = if (todayManualGoals.isNotEmpty()) (todayManualGoals.count { it.isCompletedForStats() } * 100 / todayManualGoals.size) else 0
    val focusTodayMinutes = ekagraAnalytics.focusSessions
        .filter { !it.associatedGoalId.isNullOrBlank() && IstDateUtils.getDateKey(it.startedAt) == todayKey }
        .sumOf { it.actualMinutes }
    val focusTotalMinutes = ekagraAnalytics.focusSessions
        .filter { !it.associatedGoalId.isNullOrBlank() }
        .sumOf { it.actualMinutes }
    val manualTodayMinutes = manualCompletedGoals.filter { it.completedDateKey() == todayKey }.sumOf { it.studiedMinutes ?: 0 }
    val manualTotalMinutes = manualCompletedGoals.sumOf { it.studiedMinutes ?: 0 }
    if (goalError != null && goals.isEmpty() && !isLoading) {
        SafarErrorState(message = goalError, onRetry = onRefresh, modifier = Modifier.fillMaxSize())
        return
    }
    if (isLoading && goals.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(5) { GoalRowSkeleton() }
        }
        return
    }
    if (goals.isEmpty()) {
        SafarEmptyState(
            title = "No goals yet",
            message = "Add a goal to get started on your study plan.",
            primaryActionLabel = "Add Goal",
            onPrimaryAction = onAddClick,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    SafarPullRefreshBox(
        isRefreshing = isLoading && goals.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pending.isNotEmpty()) {
            item { SectionHeader("Pending", "Only goals that are active today appear here.", "${pending.size} Tasks") }
            items(pending, key = { it.id }) { GoalItem(it, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        } else {
            item { EmptyGoalsCard("All caught up! Time to plan more?", "Anything scheduled for later stays in the upcoming section below.") }
        }
        if (scheduled.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); SectionHeader("Scheduled Tasks", "These stay quiet until their scheduled date arrives.", "${scheduled.size} upcoming") }
            items(scheduled, key = { "scheduled-${it.id}" }) { GoalItem(it, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        }
        if (completed.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); Text("Completed", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(completed.take(5), key = { it.id }) { GoalItem(it, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        }
        item {
            LivePulseCard(
                completedToday = doneToday,
                openManualGoals = pending.size,
                completionRate = completionRate,
                studyToday = manualTodayMinutes + focusTodayMinutes,
                manualToday = manualTodayMinutes,
                ekagraToday = focusTodayMinutes,
                dailyProgress = dailyProgress,
                totalManual = manualTotalMinutes,
                totalEkagra = focusTotalMinutes
            )
        }
        item { ProTipCard() }
    }
    }
}



@Composable
internal fun SectionHeader(title: String, subtitle: String, badge: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SmallBadge(badge, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun EmptyGoalsCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun StatInfoCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 124.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun LivePulseCard(
    completedToday: Int,
    openManualGoals: Int,
    completionRate: Int,
    studyToday: Int,
    manualToday: Int,
    ekagraToday: Int,
    dailyProgress: Int,
    totalManual: Int,
    totalEkagra: Int
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Today Pulse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
            Text("$completedToday Completed", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF065F46))
            Text("$openManualGoals open manual goals", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$completionRate% overall completion rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Text("Study Time Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatStudyTime(studyToday), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatInfoCard("Manual", formatStudyTime(manualToday), "", Modifier.weight(1f), accent = Color(0xFF065F46))
                StatInfoCard("Ekagra", formatStudyTime(ekagraToday), "", Modifier.weight(1f), accent = Color(0xFF9A3412))
            }
            Text("Daily Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (dailyProgress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF065F46), trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            Text("Total Time Studied", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GoalTimeRow(Icons.Default.Timer, "Ekagra Mode", "", formatStudyTime(totalEkagra), Color(0xFF9A3412))
            GoalTimeRow(Icons.Default.Book, "Manual Goal", "", formatStudyTime(totalManual), Color(0xFF065F46))
        }
    }
}

@Composable
internal fun ProTipCard() {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, Color(0xFF5B21B6).copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF5B21B6))
            Text("Pro Tip", fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
            Text("Consistent daily completion is better than occasional bursts. Break large goals into smaller ekagra tasks.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun GoalItem(goal: Goal, onComplete: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onRepeat: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = goal.progressPercent()
    val showProgress = goal.unitType != "binary" && (goal.unitType == "checklist" || goal.targetValue != null || goal.plannedFocusMinutes != null)
    val completedColor = Color(0xFF065F46)
    val badgeColor = when (goal.goalKind) {
        "today" -> Color(0xFF065F46)
        "scheduled" -> Color(0xFF5B21B6)
        else -> Color(0xFF1E3A8A)
    }

    Card(
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .border(1.5.dp, if (goal.completed) completedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape)
                    .background(if (goal.completed) completedColor else Color.Transparent)
                    .clickable(enabled = !goal.completed) { onComplete() },
                contentAlignment = Alignment.Center,
            ) { if (goal.completed) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Column(Modifier.weight(1f)) {
                Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (!goal.description.isNullOrBlank()) {
                    Text(goal.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    SmallBadge(goal.goalKindLabel(), badgeColor.copy(0.10f), badgeColor)
                    SmallBadge(goal.unitTypeLabel(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goal.status !in listOf("not_started", "completed") || goal.completed) {
                        SmallBadge(goal.statusLabel(), statusBadgeBg(goal.status), statusBadgeFg(goal.status))
                    }
                }
                if (goal.source == "ekagra") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 5.dp)) {
                        SmallBadge("Ekagra mode task", Color(0xFF9A3412).copy(0.12f), Color(0xFF9A3412))
                    }
                }
                goal.scheduledDate?.let { Text(IstDateUtils.labelFor(it), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                if (goal.completed && (goal.studiedMinutes ?: 0) > 0) {
                    Text("${formatStudyTime(goal.studiedMinutes ?: 0)} studied", fontSize = 11.sp, color = completedColor, modifier = Modifier.padding(top = 4.dp))
                }
                if (showProgress) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = completedColor,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.14f)
                    )
                    Text(goal.progressLabel(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!goal.completed) {
                        DropdownMenuItem(text = { Text("Mark as done") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showMenu = false; onComplete() })
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onEdit() })
                    }
                    DropdownMenuItem(text = { Text("Repeat Task") }, leadingIcon = { Icon(Icons.Default.Repeat, null) }, onClick = { showMenu = false; onRepeat() })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
internal fun RolloverPromptItem(goal: Goal, onRetry: () -> Unit, onArchive: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("This missed goal can be carried into today or archived.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Retry Today", fontSize = 12.sp) }
                OutlinedButton(onClick = onArchive, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Archive", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
internal fun SmallBadge(label: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
internal fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority) {
        "high"   -> MaterialTheme.colorScheme.error.copy(0.12f) to MaterialTheme.colorScheme.error
        "medium" -> MaterialTheme.colorScheme.primary.copy(0.12f) to MaterialTheme.colorScheme.primary
        else     -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.1f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(priority.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
