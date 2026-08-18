package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isGoalCompleted
import com.safarparmar.app.util.isMissedGoal
import com.safarparmar.app.util.isTodayGoal
import com.safarparmar.app.util.isUpcomingGoal

@Composable
internal fun GoalsTab(
    filterMode: String = "today",
    goals: List<Goal>,
    ekagraAnalytics: com.safarparmar.app.domain.model.EkagraAnalyticsStats,
    isLoading: Boolean,
    goalError: String? = null,
    onRefresh: () -> Unit = {},
    onAddClick: () -> Unit,
    onComplete: (Goal) -> Unit,
    onReopen: (Goal) -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = standardGoals.filter { it.isTodayGoal(todayKey) }
        .sortedBy { it.startedAt ?: it.createdAt ?: it.scheduledDate ?: "" }
    val scheduled = standardGoals.filter { it.isUpcomingGoal(todayKey) }
        .sortedBy { it.assignedDateKey() ?: "" }
    val missed = standardGoals.filter { it.isMissedGoal(todayKey) }
        .sortedByDescending { it.assignedDateKey() ?: "" }
    val completed = standardGoals.filter { it.isGoalCompleted() }
        .sortedByDescending { it.completedAt ?: it.createdAt ?: "" }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val todayGoals = standardGoals.filter { it.anchorDateKey() == todayKey }
    // Count every completion, however it happened — a goal finished through a
    // linked Ekagra session is still a goal the student completed. Only the
    // MINUTES below stay manual-only, because focus minutes are summed
    // separately from ekagraAnalytics and would otherwise be double counted.
    val allCompletedGoals = standardGoals.filter { it.isCompletedForStats() }
    val doneToday = allCompletedGoals.count { it.anchorDateKey() == todayKey }
    val completionRate = if (standardGoals.isNotEmpty()) (allCompletedGoals.size * 100 / standardGoals.size) else 0
    val dailyProgress = if (todayGoals.isNotEmpty()) {
        (todayGoals.count { it.isCompletedForStats() } * 100 / todayGoals.size)
    } else {
        0
    }
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
        // GoalsScreen owns the page's vertical scroll. A LazyColumn here would be
        // measured with an infinite max height during the initial loading state.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GoalsFlatColors.Bg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) { GoalRowSkeleton() }
        }
        return
    }
    if (goals.isEmpty()) {
        SafarEmptyState(
            title = "No goals yet",
            message = "Add a goal to get started on your study plan.",
            primaryActionLabel = "Add Goal",
            onPrimaryAction = onAddClick,
            modifier = Modifier.fillMaxSize().background(GoalsFlatColors.Bg),
        )
        return
    }

    SafarPullRefreshBox(
        isRefreshing = isLoading && goals.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().background(GoalsFlatColors.Bg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 336.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GoalsFlatColors.Primary, RoundedCornerShape(16.dp))
                    .background(GoalsFlatColors.Primary.copy(alpha = 0.03f)),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (filterMode == "today") {
                        if (pending.isNotEmpty()) {
                            item { FlatSectionEyebrow("Pending · ${pending.size} tasks") }
                            itemsIndexed(pending, key = { _, g -> g.id }) { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < pending.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            item { EmptyGoalsCard("All caught up! Time to plan more?", "Anything scheduled for later stays in the upcoming section.") }
                        }
                        val completedToday = completed.filter { it.anchorDateKey() == todayKey }
                        if (completedToday.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(18.dp))
                                PlanHairline()
                                Spacer(Modifier.height(14.dp))
                                FlatSectionEyebrow("Completed · ${completedToday.size} tasks")
                            }
                            itemsIndexed(completedToday, key = { _, g -> g.id }) { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < completedToday.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        }
                    } else if (filterMode == "upcoming") {
                        if (scheduled.isNotEmpty()) {
                            item { FlatSectionEyebrow("Scheduled · ${scheduled.size} tasks") }
                            itemsIndexed(scheduled, key = { _, g -> "scheduled-${g.id}" }) { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < scheduled.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            item { EmptyGoalsCard("No upcoming tasks", "You have no tasks scheduled for later dates.") }
                        }
                    } else if (filterMode == "missed") {
                        if (missed.isNotEmpty()) {
                            item { FlatSectionEyebrow("Missed · ${missed.size} tasks") }
                            itemsIndexed(missed, key = { _, g -> "missed-${g.id}" }) { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < missed.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            item { EmptyGoalsCard("No missed goals", "Goals that pass their assigned date will appear here.") }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                PlanHairline()
                Spacer(Modifier.height(14.dp))
                LivePulseCard(
                    completedToday = doneToday,
                    openManualGoals = pending.size,
                    completionRate = completionRate,
                    studyToday = manualTodayMinutes + focusTodayMinutes,
                    manualToday = manualTodayMinutes,
                    ekagraToday = focusTodayMinutes,
                    dailyProgress = dailyProgress,
                    totalManual = manualTotalMinutes,
                    totalEkagra = focusTotalMinutes,
                )
                Spacer(Modifier.height(18.dp))
                PlanHairline(alpha = 0.6f)
                Spacer(Modifier.height(16.dp))
                ProTipCard()
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FlatSectionEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = GoalsFlatColors.Muted,
        modifier = Modifier.padding(bottom = 10.dp, top = 4.dp),
    )
}

@Composable
internal fun EmptyGoalsCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, GoalsFlatColors.Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoalsFlatColors.Primary, modifier = Modifier.size(22.dp))
        }
        Text(title, fontFamily = LoraFontFamily, fontSize = 16.sp, color = GoalsFlatColors.Text, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 12.sp, color = GoalsFlatColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun StatInfoCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = GoalsFlatColors.Primary,
) {
    Column(
        modifier = modifier
            .heightIn(min = 100.dp)
            .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(0.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Muted, letterSpacing = 0.8.sp)
        Text(value, fontFamily = LoraFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Normal, color = accent)
        if (subtitle.isNotBlank()) {
            Text(subtitle, fontSize = 11.sp, color = GoalsFlatColors.Muted, lineHeight = 15.sp)
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
    totalEkagra: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Today Pulse",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = GoalsFlatColors.Progress,
        )
        Text(
            "$completedToday completed",
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            color = GoalsFlatColors.Done,
        )
        Text("$openManualGoals open manual goals", fontSize = 13.sp, color = GoalsFlatColors.Muted)
        Text("$completionRate% overall completion rate", fontSize = 13.sp, color = GoalsFlatColors.Muted)

        PlanHairline(alpha = 0.5f)

        Text("Study time today", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = GoalsFlatColors.Muted)
        Text(
            formatStudyTime(studyToday),
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            color = GoalsFlatColors.Progress,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            StatInfoCard("Manual", formatStudyTime(manualToday), "", Modifier.weight(1f), accent = GoalsFlatColors.Done)
            StatInfoCard("Ekagra", formatStudyTime(ekagraToday), "", Modifier.weight(1f), accent = GoalsFlatColors.Ekagra)
        }

        Text("Daily progress", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Text)
        LinearProgressIndicator(
            progress = { (dailyProgress / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.dp)),
            color = GoalsFlatColors.Done,
            trackColor = GoalsFlatColors.Hairline,
        )
        Text("$dailyProgress%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Done)

        Spacer(Modifier.height(4.dp))
        Text("Total time studied", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = GoalsFlatColors.Muted)
        GoalTimeRow(Icons.Default.Timer, "Ekagra Mode", "", formatStudyTime(totalEkagra), GoalsFlatColors.Ekagra)
        Spacer(Modifier.height(8.dp))
        GoalTimeRow(Icons.Default.Book, "Manual Goal", "", formatStudyTime(totalManual), GoalsFlatColors.Done)
    }
}

@Composable
internal fun ProTipCard() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoalsFlatColors.Scheduled, modifier = Modifier.size(16.dp))
            Text(
                "Pro tip",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = GoalsFlatColors.Scheduled,
            )
        }
        Text(
            "Consistent daily completion is better than occasional bursts. Break large goals into smaller ekagra tasks.",
            fontSize = 13.sp,
            color = GoalsFlatColors.Muted,
            lineHeight = 19.sp,
        )
    }
}

@Composable
internal fun GoalItem(
    goal: Goal,
    onComplete: () -> Unit,
    onReopen: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = goal.progressPercent()
    val showProgress = goal.unitType != "binary" &&
        (goal.unitType == "checklist" || goal.targetValue != null || goal.plannedFocusMinutes != null)
    val badgeColor = when (goal.goalKind) {
        "today" -> GoalsFlatColors.Today
        "scheduled" -> GoalsFlatColors.Scheduled
        "repeat" -> GoalsFlatColors.Repeat
        else -> GoalsFlatColors.Progress
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(
                    1.5.dp,
                    if (goal.completed) GoalsFlatColors.Done else GoalsFlatColors.Hairline,
                    CircleShape,
                )
                .background(if (goal.completed) GoalsFlatColors.Done else Color.Transparent)
                .clickable(
                    enabled = !goal.completed,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onComplete() },
            contentAlignment = Alignment.Center,
        ) {
            if (goal.completed) {
                Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (goal.completed) GoalsFlatColors.Muted else GoalsFlatColors.Text,
                textDecoration = if (goal.completed) TextDecoration.LineThrough else null,
            )
            if (!goal.description.isNullOrBlank()) {
                Text(goal.description, fontSize = 12.sp, color = GoalsFlatColors.Muted, maxLines = 2)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                if (goal.completed) {
                    val studiedText = if ((goal.studiedMinutes ?: 0) > 0) {
                        " · ${formatStudyTime(goal.studiedMinutes ?: 0)} studied"
                    } else {
                        ""
                    }
                    FlatBadge("✓ Done$studiedText", GoalsFlatColors.Done)
                } else {
                    FlatBadge(goal.goalKindLabel(), badgeColor)
                    if (goal.isMissedGoal()) {
                        FlatBadge("Missed", GoalsFlatColors.Danger)
                    }
                    if (goal.unitType != "binary") {
                        FlatBadge(goal.unitTypeLabel(), GoalsFlatColors.Muted)
                    }
                }
            }
            if (goal.source == "ekagra" && !goal.completed) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 5.dp),
                ) {
                    FlatBadge("Ekagra mode task", GoalsFlatColors.Ekagra)
                }
            }
            goal.assignedDateKey()?.let {
                Text(
                    if (goal.isMissedGoal()) "Assigned ${IstDateUtils.labelFor(it)}" else IstDateUtils.labelFor(it),
                    fontSize = 11.sp,
                    color = GoalsFlatColors.Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (showProgress) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = GoalsFlatColors.Done,
                    trackColor = GoalsFlatColors.Hairline,
                )
                Text(
                    goal.progressLabel(),
                    fontSize = 10.sp,
                    color = GoalsFlatColors.Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(18.dp),
                    tint = GoalsFlatColors.Muted,
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!goal.completed) {
                    DropdownMenuItem(
                        text = { Text("Mark as done") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = GoalsFlatColors.Primary) },
                        onClick = { showMenu = false; onComplete() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showMenu = false; onEdit() },
                    )
                } else if (onReopen != null) {
                    DropdownMenuItem(
                        text = { Text("Reopen") },
                        leadingIcon = { Icon(Icons.Default.Restore, null, tint = GoalsFlatColors.Primary) },
                        onClick = { showMenu = false; onReopen() },
                    )
                }
                // "Repeat Task" lived here. Removed: it was a third way to say
                // "repeat", it cloned goals one at a time with no dedupe (the source
                // of duplicated goals), and the "Bring forward" picker covers the
                // single-goal case by simply ticking one row.
                DropdownMenuItem(
                    text = { Text("Delete", color = GoalsFlatColors.Danger) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = GoalsFlatColors.Danger) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
internal fun RolloverPromptItem(goal: Goal, onRetry: () -> Unit, onArchive: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GoalsFlatColors.Text)
        Text(
            "This missed goal can be carried into today or archived.",
            fontSize = 12.sp,
            color = GoalsFlatColors.Muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlatFilledAction("Retry Today", GoalsFlatColors.Primary, onRetry, Modifier.weight(1f))
            FlatOutlineAction("Archive", onArchive, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun FlatBadge(label: String, accent: Color) {
    Box(
        Modifier
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

@Composable
internal fun SmallBadge(label: String, bg: Color, fg: Color) {
    // Kept for call-site compatibility; prefers outlined flat badge when bg is unused.
    FlatBadge(label, fg)
}

@Composable
internal fun PriorityBadge(priority: String) {
    val accent = when (priority) {
        "high" -> GoalsFlatColors.Danger
        "medium" -> GoalsFlatColors.Primary
        else -> GoalsFlatColors.Muted
    }
    FlatBadge(priority.replaceFirstChar { it.uppercase() }, accent)
}

@Composable
internal fun FlatFilledAction(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
internal fun FlatOutlineAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Muted)
    }
}
