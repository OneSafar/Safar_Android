package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuItemStyle
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.IconButton as ComposablesIconButton
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.Text as ComposablesText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.safarparmar.app.ui.nishtha.goals.GoalProgress as LinearProgressIndicator
import com.composables.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isVisibleInGoals
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
    val goalListScroll = rememberScrollState()
    val goalContainerHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).coerceIn(280f, 480f).dp
    LaunchedEffect(filterMode) { goalListScroll.scrollTo(0) }
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.isVisibleInGoals() }
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
            title = stringResource(R.string.goals_none_yet),
            message = stringResource(R.string.goals_none_help),
            primaryActionLabel = stringResource(R.string.goals_add_goal),
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
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(goalContainerHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GoalsFlatColors.Primary, RoundedCornerShape(16.dp))
                    .background(GoalsFlatColors.Primary.copy(alpha = 0.03f))
                    .verticalScroll(goalListScroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (filterMode == "today") {
                        if (pending.isNotEmpty()) {
                            FlatSectionEyebrow(stringResource(R.string.goals_pending_tasks, pending.size))
                            pending.forEachIndexed { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < pending.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            EmptyGoalsCard(stringResource(R.string.goals_all_caught_up), stringResource(R.string.goals_upcoming_stays_help))
                        }
                        val completedToday = completed.filter { it.anchorDateKey() == todayKey }
                        if (completedToday.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            PlanHairline()
                            Spacer(Modifier.height(14.dp))
                            FlatSectionEyebrow(stringResource(R.string.goals_completed_tasks, completedToday.size))
                            completedToday.forEachIndexed { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < completedToday.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        }
                    } else if (filterMode == "upcoming") {
                        if (scheduled.isNotEmpty()) {
                            FlatSectionEyebrow(stringResource(R.string.goals_scheduled_tasks, scheduled.size))
                            scheduled.forEachIndexed { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < scheduled.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            EmptyGoalsCard(stringResource(R.string.goals_no_upcoming), stringResource(R.string.goals_no_upcoming_help))
                        }
                    } else if (filterMode == "missed") {
                        if (missed.isNotEmpty()) {
                            FlatSectionEyebrow(stringResource(R.string.goals_missed_tasks, missed.size))
                            missed.forEachIndexed { index, goal ->
                                GoalItem(goal, onComplete = { onComplete(goal) }, onReopen = { onReopen(goal) }, onEdit = { onEdit(goal) }, onDelete = { onDelete(goal) })
                                if (index < missed.lastIndex) PlanHairline(alpha = 0.5f)
                            }
                        } else {
                            EmptyGoalsCard(stringResource(R.string.goals_no_missed), stringResource(R.string.goals_no_missed_help))
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
        if (subtitle.isNotBlank() && !subtitle.equals(title, ignoreCase = true)) {
            Text(subtitle, fontSize = 12.sp, color = GoalsFlatColors.Muted, textAlign = TextAlign.Center)
        }
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
            stringResource(R.string.goals_today_pulse),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = GoalsFlatColors.Progress,
        )
        Text(
            stringResource(R.string.goals_completed_count, completedToday),
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            color = GoalsFlatColors.Done,
        )
        Text(stringResource(R.string.goals_open_manual_count, openManualGoals), fontSize = 13.sp, color = GoalsFlatColors.Muted)
        Text(stringResource(R.string.goals_completion_rate, completionRate), fontSize = 13.sp, color = GoalsFlatColors.Muted)

        PlanHairline(alpha = 0.5f)

        Text(stringResource(R.string.goals_study_time_today), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = GoalsFlatColors.Muted)
        Text(
            formatStudyTime(studyToday),
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            color = GoalsFlatColors.Progress,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            StatInfoCard(stringResource(R.string.goals_manual), formatStudyTime(manualToday), "", Modifier.weight(1f), accent = GoalsFlatColors.Done)
            StatInfoCard(stringResource(R.string.module_ekagra), formatStudyTime(ekagraToday), "", Modifier.weight(1f), accent = GoalsFlatColors.Ekagra)
        }

        Text(stringResource(R.string.goals_daily_progress), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Text)
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
        Text(stringResource(R.string.goals_total_time_studied), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = GoalsFlatColors.Muted)
        GoalTimeRow(Icons.Default.Timer, stringResource(R.string.goals_ekagra_mode), "", formatStudyTime(totalEkagra), GoalsFlatColors.Ekagra)
        Spacer(Modifier.height(8.dp))
        GoalTimeRow(Icons.Default.Book, stringResource(R.string.goals_manual_goal), "", formatStudyTime(totalManual), GoalsFlatColors.Done)
    }
}

@Composable
internal fun ProTipCard() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoalsFlatColors.Scheduled, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.goals_pro_tip),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = GoalsFlatColors.Scheduled,
            )
        }
        Text(
            stringResource(R.string.goals_pro_tip_body),
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
    completedViaEkagra: Boolean = goal.completedViaFocus,
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
        com.composables.ui.components.Checkbox(checked = goal.completed,
            enabled = !goal.completed || onReopen != null,
            onCheckedChange = { checked -> if (checked) onComplete() else onReopen?.invoke() },
            modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (goal.completed) GoalsFlatColors.Muted else GoalsFlatColors.Text,
                style = androidx.compose.ui.text.TextStyle(textDecoration = if (goal.completed) TextDecoration.LineThrough else null),
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
                    if (completedViaEkagra) {
                        FlatBadge(stringResource(R.string.goals_completed_via_ekagra), GoalsFlatColors.Ekagra)
                    }
                } else {
                    FlatBadge(goal.goalKindLabel(), badgeColor)
                    if (goal.isMissedGoal()) {
                        FlatBadge(stringResource(R.string.goals_missed), GoalsFlatColors.Danger)
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
                    FlatBadge(stringResource(R.string.goals_ekagra_task), GoalsFlatColors.Ekagra)
                }
            }
            goal.assignedDateKey()?.let {
                Text(
                    if (goal.isMissedGoal()) stringResource(R.string.goals_assigned_date, IstDateUtils.labelFor(it)) else IstDateUtils.labelFor(it),
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
        DropdownMenu(
            expanded = showMenu,
            onExpandedChange = { showMenu = it },
            alignment = DropdownMenuAlignment.End,
            panel = {
                DropdownMenuPanel {
                    if (!goal.completed) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onComplete()
                            },
                            leading = {
                                ComposablesIcon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoalsFlatColors.Primary,
                                )
                            },
                        ) {
                            ComposablesText(stringResource(R.string.goals_mark_done))
                        }
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leading = {
                                ComposablesIcon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        ) {
                            ComposablesText(stringResource(R.string.common_edit))
                        }
                    } else if (onReopen != null) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onReopen()
                            },
                            leading = {
                                ComposablesIcon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoalsFlatColors.Primary,
                                )
                            },
                        ) {
                            ComposablesText(stringResource(R.string.goals_reopen))
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        style = DropdownMenuItemStyle.Destructive,
                        leading = {
                            ComposablesIcon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoalsFlatColors.Danger,
                            )
                        },
                    ) {
                        ComposablesText(stringResource(R.string.common_delete))
                    }
                }
            },
            anchor = {
                ComposablesIconButton(
                    onClick = { showMenu = showMenu.not() },
                    style = ButtonStyle.Ghost,
                    buttonSize = ButtonSize.Small,
                    modifier = Modifier.size(28.dp),
                ) {
                    ComposablesIcon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.common_more_options),
                        modifier = Modifier.size(18.dp),
                        tint = GoalsFlatColors.Muted,
                    )
                }
            },
        )
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
            stringResource(R.string.goals_missed_action_help),
            fontSize = 12.sp,
            color = GoalsFlatColors.Muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlatFilledAction(stringResource(R.string.goals_retry_today), GoalsFlatColors.Primary, onRetry, Modifier.weight(1f))
            FlatOutlineAction(stringResource(R.string.goals_archive), onArchive, Modifier.weight(1f))
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
    com.composables.ui.components.Button(onClick = onClick, modifier = modifier) {
        ComposablesText(label)
    }
}

@Composable
internal fun FlatOutlineAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.composables.ui.components.Button(onClick = onClick, modifier = modifier, style = ButtonStyle.Outlined) {
        ComposablesText(label)
    }
}
