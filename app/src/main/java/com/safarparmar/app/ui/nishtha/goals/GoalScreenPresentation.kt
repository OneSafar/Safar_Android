package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.ui.platform.LocalLocale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import com.composables.ui.components.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Switch
import com.composables.ui.components.Text
import com.composables.ui.components.Icon as ComposablesIcon
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.nishtha.goals.GoalEyebrow as PlanEyebrow
import com.safarparmar.app.ui.nishtha.goals.GoalDivider as PlanHairline
import com.safarparmar.app.ui.theme.PoppinsFontFamily as LoraFontFamily
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.assignedDateKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.safarparmar.app.ui.nishtha.goals.GoalInput as OutlinedTextField
import com.safarparmar.app.ui.nishtha.goals.GoalTextButton as TextButton

/** Side-effect-free Goals presentation shared with feature practice. */
@Composable
internal fun GoalCreationContent(
    title: String, description: String, scheduled: Boolean, repeatDaily: Boolean,
    selectedDate: LocalDate, onTitleChange: (String) -> Unit, onDescriptionChange: (String) -> Unit,
    onToggleSchedule: () -> Unit, onPickDate: () -> Unit, onRepeatChange: (Boolean) -> Unit,
    onCreate: () -> Unit, isSaving: Boolean = false, error: String? = null, editing: Boolean = false,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlanEyebrow(stringResource(R.string.goals_eyebrow))
        Text(
            stringResource(if (editing) R.string.goals_edit_goal else R.string.goals_new_goal),
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = GoalsFlatColors.Text,
        )
        PlanHairline()
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.goals_title_prompt)) },
            accessibilityLabel = stringResource(R.string.goals_title_prompt),
            modifier = Modifier.fillMaxWidth().tutorialTarget("goals.title"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving,
        )
        PlanHairline(alpha = 0.6f)
        val isScheduled = scheduled
        Box(Modifier.tutorialTarget("goals.schedule")) {
            AssistOptionRow(
                selected = isScheduled,
                title = stringResource(R.string.goals_schedule_future),
                subtitle = if (isScheduled) stringResource(R.string.goals_set_for_date, selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", LocalLocale.current.platformLocale))) else stringResource(if (editing) R.string.goals_set_today_pick_future else R.string.goals_default_today_pick_future),
                onClick = onToggleSchedule
            )
        }
        if (isScheduled) {
            Box(Modifier.tutorialTarget("goals.date")) {
                ScheduledDatePickerRow(selectedDate = selectedDate, onClick = onPickDate)
            }
        }
        PlanHairline(alpha = 0.6f)
        Row(
            modifier = Modifier.fillMaxWidth().tutorialTarget("goals.daily"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.goals_repeat_every_day), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                Text(stringResource(R.string.goals_repeat_every_day_help), fontSize = 12.sp, color = GoalsFlatColors.Muted)
            }
            Switch(checked = repeatDaily, onCheckedChange = onRepeatChange)
        }
        PlanHairline(alpha = 0.6f)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.goals_add_details_optional)) },
            accessibilityLabel = stringResource(R.string.goals_add_details_optional),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving,
        )
        if (error != null) {
            Text(error, color = GoalsFlatColors.Danger, fontSize = 13.sp)
        }
        val createEnabled = title.isNotBlank() && !isSaving
        Button(onClick = onCreate, style = ButtonStyle.Primary, enabled = createEnabled, modifier = Modifier.fillMaxWidth().tutorialTarget("goals.create")) {
            if (isSaving && !editing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = GoalsFlatColors.OnPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!editing) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoalsFlatColors.OnPrimary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(stringResource(if (editing) R.string.goals_save_changes else R.string.goals_create_goal), fontWeight = FontWeight.Bold, color = GoalsFlatColors.OnPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun GoalsOverviewHeader(
    tabs: List<String>, selectedTab: Int, totalToday: Int, doneToday: Int,
    onSelectTab: (Int) -> Unit, onAdd: () -> Unit, onStatus: () -> Unit,
    onRepeat: () -> Unit, onInsights: () -> Unit, onDeleted: () -> Unit, onLearn: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.goals_my_goals), fontSize = 25.sp, fontWeight = FontWeight.Bold,
                color = GoalsFlatColors.Text, modifier = Modifier.weight(1f))
            Button(onClick = onAdd, style = ButtonStyle.Primary, modifier = Modifier.heightIn(min = 48.dp).tutorialTarget("goals.add")) {
                ComposablesIcon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.goals_add_goal), fontSize = 13.sp, maxLines = 1)
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(GoalsFlatColors.PrimarySoft).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (totalToday == 0) stringResource(R.string.goals_nothing_planned_today)
                else stringResource(R.string.goals_done_today_count, doneToday, totalToday),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Primary)
            Text(if (totalToday == 0) stringResource(R.string.goals_add_to_get_started) else stringResource(R.string.goals_done_today_label),
                fontSize = 13.sp, color = GoalsFlatColors.Muted)
            if (totalToday > 0) GoalProgress({ doneToday.toFloat() / totalToday }, Modifier.fillMaxWidth().padding(top = 6.dp),
                GoalsFlatColors.Primary, GoalsFlatColors.Paper)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val actions = listOf(
                Triple(R.string.goals_status, Icons.Default.BarChart, onStatus) to "goals.status",
                Triple(R.string.goals_repeat_goals, Icons.Default.Repeat, onRepeat) to "goals.repeat",
                Triple(R.string.goals_insights, Icons.AutoMirrored.Filled.TrendingUp, onInsights) to "goals.insights",
                Triple(R.string.goals_recently_deleted, Icons.Default.DeleteSweep, onDeleted) to "goals.deleted",
            )
            actions.forEach { (action, target) ->
                Button(onClick = action.third, style = ButtonStyle.Outlined,
                    modifier = Modifier.heightIn(min = 48.dp).tutorialTarget(target),
                    shape = RoundedCornerShape(14.dp), contentColor = GoalsFlatColors.Text,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    ComposablesIcon(action.second, contentDescription = null, modifier = Modifier.size(17.dp), tint = GoalsFlatColors.Primary)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(action.first), fontSize = 12.sp)
                }
            }
            if (com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) TextButton(onClick = onLearn) {
                ComposablesIcon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.goals_tutorial_learn), fontSize = 12.sp)
            }
        }
        Box(Modifier.tutorialTarget("goals.tabs")) { GoalsUnderlineTabs(tabs, selectedTab, onSelectTab) }
    }
}

@Composable
internal fun GoalStudyTimeContent(hours: Int, minutes: Int, onHoursChange: (Int) -> Unit, onMinutesChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.common_hours_upper),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoalsFlatColors.Primary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                TimeDigitField(
                    value = hours,
                    onValueChange = onHoursChange,
                    maxValue = 99,
                )
            }
            Text(
                ":",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GoalsFlatColors.Primary,
                modifier = Modifier.padding(top = 20.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.common_minutes_upper),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoalsFlatColors.Primary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                TimeDigitField(
                    value = minutes,
                    onValueChange = onMinutesChange,
                    maxValue = 59,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("+15m" to 15, "+30m" to 30, "+1h" to 60, "+2h" to 120).forEach { (label, mins) ->
                Button(onClick = {
                    val (nextHours, nextMinutes) = addStudyTime(hours, minutes, mins)
                    onHoursChange(nextHours)
                    onMinutesChange(nextMinutes)
                }, style = ButtonStyle.Outlined, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp), contentColor = GoalsFlatColors.Text) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                }
            }
        }
    }
}

@Composable
internal fun GoalCompletionActions(hours: Int, minutes: Int, isSaving: Boolean, onComplete: (Int) -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onComplete(hours * 60 + minutes) }, enabled = !isSaving,
            style = ButtonStyle.Primary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(if (isSaving) R.string.common_saving else R.string.common_done), fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = { onComplete(0) }, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.goals_complete_without_time), fontSize = 13.sp)
        }
        Button(onClick = onCancel, enabled = !isSaving, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.common_cancel))
        }
    }
}

@Composable
internal fun GoalRepeatContent(
    goals: List<Goal>, isSaving: Boolean, onRepeat: (List<Goal>, Int, Boolean) -> Unit,
    initialSource: Int = 0, initialDaily: Boolean = false,
) {
    var repeatSource by remember { mutableIntStateOf(initialSource) }
    var repeatDaily by remember { mutableStateOf(initialDaily) }
    val todayKeyForRepeat = IstDateUtils.todayKey()
    val yesterdayKey = remember {
        runCatching {
            LocalDate.parse(IstDateUtils.todayKey()).minusDays(1).toString()
        }.getOrDefault(IstDateUtils.todayKey())
    }
    val sourceDateKey = if (repeatSource == 0) todayKeyForRepeat else yesterdayKey
    val candidates = remember(goals, sourceDateKey) {
        goals.filter {
            it.source != "ekagra" &&
                it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
                IstDateUtils.getDateKey(it.scheduledDate ?: it.createdAt) == sourceDateKey
        }
    }
    val selectedIds = remember(candidates, repeatSource) {
        mutableStateListOf<String>().apply { addAll(candidates.map { it.id }) }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanEyebrow(stringResource(R.string.goals_eyebrow))
        Text(
            stringResource(R.string.goals_repeat_goals),
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = GoalsFlatColors.Text,
        )
        Text(
            if (candidates.isEmpty()) {
                if (repeatSource == 0) stringResource(R.string.goals_no_goals_today) else stringResource(R.string.goals_no_goals_yesterday)
            } else {
                if (repeatSource == 0) stringResource(R.string.goals_select_today_repeat_tomorrow)
                else stringResource(R.string.goals_select_yesterday_bring_today)
            },
            fontSize = 13.sp,
            color = GoalsFlatColors.Muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(stringResource(R.string.common_today) to 0, stringResource(R.string.common_yesterday) to 1).forEach { (label, index) ->
                TextButton(onClick = { repeatSource = index }) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        color = if (repeatSource == index) GoalsFlatColors.Primary else GoalsFlatColors.Muted,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (candidates.isNotEmpty()) {
                TextButton(onClick = {
                    if (selectedIds.size == candidates.size) selectedIds.clear()
                    else {
                        selectedIds.clear()
                        selectedIds.addAll(candidates.map { it.id })
                    }
                }) {
                    Text(
                        if (selectedIds.size == candidates.size) stringResource(R.string.common_clear_all) else stringResource(R.string.common_select_all),
                        color = GoalsFlatColors.Primary,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp).tutorialTarget("goals.repeatSelection")
                .verticalScroll(rememberScrollState()),
        ) {
            candidates.forEach { goal ->
                val checked = goal.id in selectedIds
                com.composables.ui.components.Checkbox(checked = checked, accessibilityLabel = goal.title, enabled = !isSaving,
                    onCheckedChange = { if (it) selectedIds.add(goal.id) else selectedIds.remove(goal.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            goal.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoalsFlatColors.Text,
                            maxLines = 1,
                        )
                        Text(
                            if (goal.completed) stringResource(R.string.goals_completed) else goal.goalKindLabel(),
                            fontSize = 11.5.sp,
                            color = GoalsFlatColors.Muted,
                        )
                    }
                }
                PlanHairline(alpha = 0.5f)
            }
        }

        if (repeatSource == 0 && candidates.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().tutorialTarget("goals.repeatAuto").padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.goals_auto_repeat_selected), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                    Text(stringResource(R.string.goals_auto_repeat_help), fontSize = 11.5.sp, color = GoalsFlatColors.Muted)
                }
                Switch(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
            }
        }

        TextButton(
            onClick = { onRepeat(candidates.filter { it.id in selectedIds }, repeatSource, repeatDaily) },
            enabled = selectedIds.isNotEmpty() && !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (selectedIds.isEmpty()) stringResource(R.string.goals_choose_at_least_one)
                else if (repeatSource == 0) stringResource(R.string.goals_repeat_selected_tomorrow, selectedIds.size)
                else stringResource(R.string.goals_repeat_selected_today, selectedIds.size),
                fontWeight = FontWeight.Bold,
                color = if (selectedIds.isEmpty()) GoalsFlatColors.Muted else GoalsFlatColors.Primary,
            )
        }
    }
}

@Composable
internal fun GoalDeletedContent(goals: List<Goal>, isLoading: Boolean, isSaving: Boolean, onRestore: (Goal) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanEyebrow(stringResource(R.string.goals_eyebrow))
        Text(
            stringResource(R.string.goals_recently_deleted),
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            color = GoalsFlatColors.Text,
        )
        Text(
            stringResource(R.string.goals_recently_deleted_help),
            fontSize = 13.sp,
            color = GoalsFlatColors.Muted,
        )
        when {
            isLoading -> Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = GoalsFlatColors.Primary) }
            goals.isEmpty() -> Text(
                stringResource(R.string.goals_recently_deleted_empty),
                color = GoalsFlatColors.Muted,
                modifier = Modifier.padding(vertical = 28.dp),
            )
            else -> Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                goals.forEach { goal ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoalsFlatColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                goal.assignedDateKey()?.let { stringResource(R.string.goals_assigned_date_value, IstDateUtils.labelFor(it)) }
                                    ?: stringResource(R.string.goals_original_date_unavailable),
                                fontSize = 11.sp,
                                color = GoalsFlatColors.Muted,
                            )
                        }
                        TextButton(
                            enabled = !isSaving,
                            onClick = { onRestore(goal) },
                            modifier = Modifier.tutorialTarget("goals.restore"),
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.common_restore))
                        }
                    }
                    PlanHairline(alpha = 0.5f)
                }
            }
        }
    }
}

@Composable
internal fun GoalOverdueActions(originalDate: String, isSaving: Boolean, onMoveToToday: () -> Unit, onKeepDate: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onMoveToToday, enabled = !isSaving, style = ButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(if (isSaving) R.string.common_saving else R.string.goals_move_today_complete), fontSize = 14.sp)
        }
        Button(onClick = onKeepDate, enabled = !isSaving, style = ButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.goals_complete_for_date, originalDate), fontSize = 13.sp)
        }
        TextButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.common_cancel)) }
    }
}
