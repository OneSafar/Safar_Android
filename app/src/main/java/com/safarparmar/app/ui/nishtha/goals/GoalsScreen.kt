package com.safarparmar.app.ui.nishtha.goals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isGoalCompleted
import com.safarparmar.app.util.isHiddenFromActiveGoals
import com.safarparmar.app.util.isTodayGoal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: NishthaViewModel = hiltViewModel()
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        GoalsScreenContent(onNavigate = onNavigate, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsScreenContent(
    onNavigate: (String) -> Unit,
    viewModel: NishthaViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Automatically refresh goals when this screen comes into focus
    LaunchedEffect(Unit) {
        viewModel.onEvent(NishthaEvent.LoadGoals)
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Today", "Upcoming", "Missed", "Completed")
    var showAddSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var showDeletedSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("medium") }
    var newGoalKind by remember { mutableStateOf("today") }
    var newRepeatDaily by remember { mutableStateOf(false) }
    var newCarryForward by remember { mutableStateOf("none") }
    var newSubtaskInput by remember { mutableStateOf("") }
    var newSubtasks by remember { mutableStateOf(listOf<String>()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now(IstDateUtils.zone)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Edit state
    var editGoal by remember { mutableStateOf<Goal?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("medium") }
    var editGoalKind by remember { mutableStateOf("today") }
    var editRepeatDaily by remember { mutableStateOf(false) }
    var editScheduleChanged by remember { mutableStateOf(false) }
    var editUnitType by remember { mutableStateOf("binary") }
    var editStatus by remember { mutableStateOf("not_started") }
    var editCarryForward by remember { mutableStateOf("none") }

    // Complete / study time dialog
    var completeGoal by remember { mutableStateOf<Goal?>(null) }
    var overdueCompletion by remember { mutableStateOf<Pair<Goal, Int>?>(null) }
    // Deletion is recoverable for 30 days. Keep an explicit confirmation as a
    // second guard because the action is adjacent to frequently used menu items.
    var deleteGoal by remember { mutableStateOf<Goal?>(null) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var studyHours by remember { mutableIntStateOf(0) }
    var studyMinutes by remember { mutableIntStateOf(0) }

    fun requestCompletion(goal: Goal, studiedMinutes: Int) {
        val assignedDate = goal.assignedDateKey()
        if (assignedDate != null && assignedDate < IstDateUtils.todayKey()) {
            completeGoal = null
            overdueCompletion = goal to studiedMinutes
        } else {
            viewModel.completeGoal(goal.id, studiedMinutes)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoalsFlatColors.Primary,
        unfocusedBorderColor = GoalsFlatColors.Hairline,
        focusedTextColor = GoalsFlatColors.Text,
        unfocusedTextColor = GoalsFlatColors.Text,
        focusedLabelColor = GoalsFlatColors.Muted,
        unfocusedLabelColor = GoalsFlatColors.Muted,
        cursorColor = GoalsFlatColors.Primary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )

    if (showDatePicker) {
        // The picker leaves composition after each use, so its initial value is
        // always the date belonging to the goal currently being created/edited.
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val today = LocalDate.now(IstDateUtils.zone)
                    val date = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    return !date.isBefore(today) && !date.isAfter(today.plusDays(365))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = GoalsFlatColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = GoalsFlatColors.Muted)
                }
            },
        ) { DatePicker(state = datePickerState) }
    }

    // Study time completion dialog
    if (showRepeatPicker) {
        var repeatSource by remember { mutableIntStateOf(0) }
        var repeatDaily by remember { mutableStateOf(false) }
        val todayKeyForRepeat = IstDateUtils.todayKey()
        val yesterdayKey = remember {
            runCatching {
                LocalDate.parse(IstDateUtils.todayKey()).minusDays(1).toString()
            }.getOrDefault(IstDateUtils.todayKey())
        }
        val sourceDateKey = if (repeatSource == 0) todayKeyForRepeat else yesterdayKey
        val candidates = remember(uiState.goals, sourceDateKey) {
            uiState.goals.filter {
                it.source != "ekagra" &&
                    it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
                    IstDateUtils.getDateKey(it.scheduledDate ?: it.createdAt) == sourceDateKey
            }
        }
        val selectedIds = remember(candidates, repeatSource) {
            mutableStateListOf<String>().apply { addAll(candidates.map { it.id }) }
        }

        ModalBottomSheet(
            onDismissRequest = { showRepeatPicker = false },
            containerColor = GoalsFlatColors.Bg,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlanEyebrow("Goals")
                Text(
                    "Repeat goals",
                    fontFamily = LoraFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoalsFlatColors.Text,
                )
                Text(
                    if (candidates.isEmpty()) {
                        if (repeatSource == 0) "You have no goals today." else "You had no goals yesterday."
                    } else {
                        if (repeatSource == 0) "Select today's goals to repeat tomorrow."
                        else "Select yesterday's goals to bring into today."
                    },
                    fontSize = 13.sp,
                    color = GoalsFlatColors.Muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today" to 0, "Yesterday" to 1).forEach { (label, index) ->
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
                                if (selectedIds.size == candidates.size) "Clear all" else "Select all",
                                color = GoalsFlatColors.Primary,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    candidates.forEach { goal ->
                        val checked = goal.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(goal.id) else selectedIds.add(goal.id)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (checked) GoalsFlatColors.Primary else GoalsFlatColors.Muted,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    goal.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GoalsFlatColors.Text,
                                    maxLines = 1,
                                )
                                Text(
                                    if (goal.completed) "Completed" else goal.goalKindLabel(),
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-repeat selected goals daily", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                            Text("They will continue on following days.", fontSize = 11.5.sp, color = GoalsFlatColors.Muted)
                        }
                        Switch(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                    }
                }

                TextButton(
                    onClick = {
                        val selectedGoals = candidates.filter { it.id in selectedIds }
                        if (repeatSource == 0) {
                            val tomorrow = LocalDate.parse(todayKeyForRepeat).plusDays(1).toString()
                            viewModel.repeatGoalsOnDate(selectedGoals, tomorrow, repeatDaily)
                        } else {
                            viewModel.repeatGoals(selectedIds.toList())
                        }
                        showRepeatPicker = false
                    },
                    enabled = selectedIds.isNotEmpty() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedIds.isEmpty()) "Choose at least one"
                        else if (repeatSource == 0) "Repeat ${selectedIds.size} selected tomorrow"
                        else "Repeat ${selectedIds.size} selected today",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIds.isEmpty()) GoalsFlatColors.Muted else GoalsFlatColors.Primary,
                    )
                }
            }
        }
    }

    if (showDeletedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeletedSheet = false },
            containerColor = GoalsFlatColors.Bg,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlanEyebrow("Goals")
                Text(
                    "Recently Deleted",
                    fontFamily = LoraFontFamily,
                    fontSize = 22.sp,
                    color = GoalsFlatColors.Text,
                )
                Text(
                    "Goals remain available for 30 days. Restoring puts them back on their original assigned date.",
                    fontSize = 13.sp,
                    color = GoalsFlatColors.Muted,
                )
                when {
                    uiState.isLoadingDeletedGoals -> Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = GoalsFlatColors.Primary) }
                    uiState.recentlyDeletedGoals.isEmpty() -> Text(
                        "Nothing in Recently Deleted.",
                        color = GoalsFlatColors.Muted,
                        modifier = Modifier.padding(vertical = 28.dp),
                    )
                    else -> Column(
                        modifier = Modifier
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        uiState.recentlyDeletedGoals.forEach { goal ->
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
                                        goal.assignedDateKey()?.let { "Assigned ${IstDateUtils.labelFor(it)}" }
                                            ?: "Original date unavailable",
                                        fontSize = 11.sp,
                                        color = GoalsFlatColors.Muted,
                                    )
                                }
                                TextButton(
                                    enabled = !uiState.isSavingGoal,
                                    onClick = { viewModel.restoreGoal(goal.id) },
                                ) {
                                    Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }
                            PlanHairline(alpha = 0.5f)
                        }
                    }
                }
            }
        }
    }

    deleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { deleteGoal = null },
            title = { Text("Delete this goal?") },
            text = { Text("\"${goal.title}\" will move to Recently Deleted for 30 days.") },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSavingGoal,
                    onClick = { viewModel.deleteGoal(goal.id) },
                ) {
                    Text(if (uiState.isSavingGoal) "Moving…" else "Move to Recently Deleted", color = GoalsFlatColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteGoal = null }) { Text("Keep it") }
            },
        )
    }

    completeGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { completeGoal = null; studyHours = 0; studyMinutes = 0 },
            containerColor = GoalsFlatColors.Bg,
            title = {
                Text(
                    "How long did you study?",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = GoalsFlatColors.Text,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "HOURS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoalsFlatColors.Primary,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            TimeDigitField(
                                value = studyHours,
                                onValueChange = { studyHours = it },
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
                                "MINUTES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoalsFlatColors.Primary,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            TimeDigitField(
                                value = studyMinutes,
                                onValueChange = { studyMinutes = it },
                                maxValue = 59,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("+15m" to 15, "+30m" to 30, "+1h" to 60, "+2h" to 120).forEach { (label, mins) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            val total = studyHours * 60 + studyMinutes + mins
                                            studyHours = total / 60
                                            studyMinutes = total % 60
                                        },
                                    )
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoalsFlatColors.Primary)
                            .clickable(enabled = !uiState.isSavingGoal) {
                                val totalMins = studyHours * 60 + studyMinutes
                                requestCompletion(goal, totalMins)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (uiState.isSavingGoal) "Saving…" else "Done", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        "Complete without study time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoalsFlatColors.Primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isSavingGoal) {
                                requestCompletion(goal, 0)
                            }
                            .padding(vertical = 10.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(14.dp))
                            .clickable {
                                completeGoal = null; studyHours = 0; studyMinutes = 0
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, color = GoalsFlatColors.Muted, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp),
        )
    }

    overdueCompletion?.let { (goal, totalMins) ->
        val originalDateKey = goal.assignedDateKey().orEmpty()
        val originalDate = runCatching {
            LocalDate.parse(originalDateKey).format(
                DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()),
            )
        }.getOrDefault(originalDateKey)
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSavingGoal) {
                    overdueCompletion = null
                    studyHours = 0
                    studyMinutes = 0
                }
            },
            containerColor = GoalsFlatColors.Bg,
            title = {
                Text(
                    "This goal is from an earlier day",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = GoalsFlatColors.Text,
                )
            },
            text = {
                Text(
                    "This goal was planned for $originalDate. Where should we count it?",
                    fontSize = 14.sp,
                    color = GoalsFlatColors.Muted,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Primary: Move to today and complete
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoalsFlatColors.Primary)
                            .clickable(enabled = !uiState.isSavingGoal) {
                                viewModel.completeGoal(
                                    id = goal.id,
                                    studiedMinutes = totalMins,
                                    scheduledDate = IstDateUtils.todayKey(),
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (uiState.isSavingGoal) "Saving…" else "Move to today and complete",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }

                    // Secondary: Complete for original date
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(14.dp))
                            .clickable(enabled = !uiState.isSavingGoal) {
                                viewModel.completeGoal(goal.id, totalMins)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Complete for $originalDate",
                            fontWeight = FontWeight.SemiBold,
                            color = GoalsFlatColors.Primary,
                            fontSize = 13.sp,
                        )
                    }

                    // Cancel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(14.dp))
                            .clickable(enabled = !uiState.isSavingGoal) {
                                overdueCompletion = null
                                studyHours = 0
                                studyMinutes = 0
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Bold,
                            color = GoalsFlatColors.Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp),
        )
    }

    // Edit sheet
    editGoal?.let { goal ->
        ModalBottomSheet(
            onDismissRequest = { editGoal = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GoalsFlatColors.Bg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = GoalsFlatColors.Hairline)
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlanEyebrow("Goals")
                Text(
                    "Edit Goal",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoalsFlatColors.Text,
                )
                PlanHairline()
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("What do you want to do?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                )
                PlanHairline(alpha = 0.6f)
                val editIsScheduled = editGoalKind == "scheduled"
                AssistOptionRow(
                    selected = editIsScheduled,
                    title = "Schedule for a future date",
                    subtitle = if (editIsScheduled) "Set for ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))}" else "Set for today. Tap to pick a future date.",
                    onClick = {
                        if (editIsScheduled) {
                            editGoalKind = "today"
                            selectedDate = LocalDate.now(IstDateUtils.zone)
                            editScheduleChanged = true
                        } else {
                            editGoalKind = "scheduled"
                            selectedDate = LocalDate.now(IstDateUtils.zone).plusDays(1)
                            editScheduleChanged = true
                            showDatePicker = true
                        }
                    }
                )
                if (editIsScheduled) {
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { editScheduleChanged = true; showDatePicker = true },
                    )
                }
                PlanHairline(alpha = 0.6f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Repeat every day", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                        Text("A fresh copy is added each day; missed copies stay under Missed.", fontSize = 12.sp, color = GoalsFlatColors.Muted)
                    }
                    Switch(checked = editRepeatDaily, onCheckedChange = { editRepeatDaily = it })
                }
                PlanHairline(alpha = 0.6f)
                OutlinedTextField(
                    value = editDesc,
                    onValueChange = { editDesc = it },
                    label = { Text("Add details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                )
                val saveEnabled = editTitle.isNotBlank() && !uiState.isSavingGoal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (saveEnabled) GoalsFlatColors.Primary
                            else GoalsFlatColors.Hairline.copy(alpha = 0.55f),
                        )
                        .clickable(enabled = saveEnabled) {
                            val scheduledDate = if (!editScheduleChanged) null else when (editGoalKind) {
                                "today" -> IstDateUtils.todayKey()
                                "scheduled" -> selectedDate.toString()
                                "one_time" -> goal.scheduledDate
                                else -> IstDateUtils.todayKey()
                            }
                            viewModel.updateGoalDetails(
                                id = goal.id,
                                title = editTitle.trim(),
                                description = editDesc.ifBlank { null },
                                priority = editPriority,
                                scheduledDate = scheduledDate,
                                startedAt = goal.startedAt,
                                subtasks = if (editUnitType == "checklist") goal.subtasks else emptyList(),
                                goalKind = if (editRepeatDaily) "repeat" else editGoalKind,
                                unitType = editUnitType,
                                linkedFocusEnabled = goal.linkedFocusEnabled,
                                plannedFocusMinutes = goal.plannedFocusMinutes,
                                targetValue = goal.targetValue,
                                achievedValue = goal.achievedValue,
                                status = if (editScheduleChanged && editStatus in listOf("missed", "expired")) "not_started" else editStatus,
                                carryForwardMode = if (editGoalKind == "scheduled" || editGoalKind == "one_time") "none" else editCarryForward
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    LaunchedEffect(uiState.goalError) {
        if (uiState.goalError != null) Toast.makeText(context, uiState.goalError, Toast.LENGTH_SHORT).show()
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = GoalsFlatColors.Bg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = GoalsFlatColors.Hairline)
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlanEyebrow("Goals")
                Text(
                    "New Goal",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoalsFlatColors.Text,
                )
                PlanHairline()
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("What do you want to do?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                )
                PlanHairline(alpha = 0.6f)
                val isScheduled = newGoalKind == "scheduled"
                AssistOptionRow(
                    selected = isScheduled,
                    title = "Schedule for a future date",
                    subtitle = if (isScheduled) "Set for ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))}" else "Default is Today. Tap to pick a future date.",
                    onClick = {
                        if (isScheduled) {
                            newGoalKind = "today"
                            selectedDate = LocalDate.now(IstDateUtils.zone)
                        } else {
                            newGoalKind = "scheduled"
                            selectedDate = LocalDate.now(IstDateUtils.zone).plusDays(1)
                            showDatePicker = true
                        }
                    }
                )
                if (isScheduled) {
                    ScheduledDatePickerRow(selectedDate = selectedDate, onClick = { showDatePicker = true })
                }
                PlanHairline(alpha = 0.6f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Repeat every day", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
                        Text("A fresh copy is added each day; missed copies stay under Missed.", fontSize = 12.sp, color = GoalsFlatColors.Muted)
                    }
                    Switch(checked = newRepeatDaily, onCheckedChange = { newRepeatDaily = it })
                }
                PlanHairline(alpha = 0.6f)
                OutlinedTextField(
                    value = newDesc,
                    onValueChange = { newDesc = it },
                    label = { Text("Add details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                )
                if (uiState.goalError != null) {
                    Text(uiState.goalError!!, color = GoalsFlatColors.Danger, fontSize = 13.sp)
                }
                val createEnabled = newTitle.isNotBlank() && !uiState.isSavingGoal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (createEnabled) GoalsFlatColors.Primary
                            else GoalsFlatColors.Hairline.copy(alpha = 0.55f),
                        )
                        .clickable(enabled = createEnabled) {
                            val scheduledDate = if (newGoalKind == "scheduled") selectedDate.toString() else IstDateUtils.todayKey()
                            viewModel.addGoal(
                                title = newTitle.trim(),
                                description = newDesc.ifBlank { null },
                                priority = newPriority,
                                scheduledDate = scheduledDate,
                                startedAt = null,
                                subtasks = emptyList(),
                                goalKind = if (newRepeatDaily) "repeat" else newGoalKind,
                                unitType = "binary",
                                linkedFocusEnabled = false,
                                plannedFocusMinutes = null,
                                targetValue = null,
                                achievedValue = 0,
                                status = "not_started",
                                carryForwardMode = newCarryForward
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isSavingGoal) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Create Goal", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (showStatusSheet) {
        AlertDialog(
            onDismissRequest = { showStatusSheet = false },
            containerColor = GoalsFlatColors.Bg,
            confirmButton = {
                TextButton(onClick = { showStatusSheet = false }) {
                    Text("Close", color = GoalsFlatColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            title = {
                Text(
                    "Status",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = GoalsFlatColors.Text,
                )
            },
            text = {
                StatusGrid(goals = uiState.goals, ekagraAnalytics = uiState.ekagraAnalytics)
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    LaunchedEffect(uiState.goalMessage, uiState.goalAction) {
        uiState.goalMessage?.let { message ->
            val action = uiState.goalAction
            val actionGoalId = uiState.goalActionGoalId
            viewModel.clearGoalMessage()
            when (action) {
                "create" -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    showAddSheet = false
                    newTitle = ""; newDesc = ""; newSubtasks = emptyList(); newSubtaskInput = ""
                    newGoalKind = "today"; newRepeatDaily = false; newCarryForward = "none"
                    selectedDate = LocalDate.now(IstDateUtils.zone)
                }
                "update" -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    editGoal = null
                }
                "complete" -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    completeGoal = null
                    overdueCompletion = null
                    studyHours = 0; studyMinutes = 0
                }
                "delete" -> {
                    deleteGoal = null
                    viewModel.loadRecentlyDeletedGoals()
                    val result = snackbarHostState.showSnackbar(
                        message = "Goal moved to Recently Deleted",
                        actionLabel = "Undo",
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed && actionGoalId != null) {
                        viewModel.restoreGoal(actionGoalId)
                    }
                }
                else -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val todayKey = IstDateUtils.todayKey()
    val standardGoals = uiState.goals.filter { it.source != "ekagra" }
    // "Done today" counts EVERY completed goal, however it was finished. Counting
    // only manual completions meant a goal finished through a linked Ekagra
    // session was excluded here AND from pendingToday (it is completed), so it
    // vanished from the counter entirely: "0 of 2" became "0 of 1" instead of
    // "1 of 2". The manual-only split still matters for the minutes breakdown,
    // where Ekagra minutes are counted separately from focus sessions.
    val doneToday = standardGoals.count { it.isGoalCompleted() && it.assignedDateKey() == todayKey }
    val pendingToday = standardGoals.count { it.isTodayGoal(todayKey) }
    val totalToday = doneToday + pendingToday

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalsFlatColors.Bg)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp),
        ) {
            Text(
                "My Goals",
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = GoalsFlatColors.Text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (totalToday == 0) {
                    "Nothing planned for today"
                } else {
                    "$doneToday of $totalToday done today"
                },
                fontSize = 13.sp,
                color = GoalsFlatColors.Muted,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Keep the primary action visible on narrow screens; secondary
                // utilities may scroll, but creating a goal must never be clipped.
                FlatActionChip(
                    label = "Add Goal",
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                    },
                    filled = true,
                    onClick = { showAddSheet = true },
                )
                FlatActionChip(
                    label = "Status",
                    icon = {
                        Icon(
                            Icons.Default.BarChart,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = GoalsFlatColors.Muted,
                        )
                    },
                    filled = false,
                    onClick = { showStatusSheet = true },
                )
                // Deliberately NOT called "Repeat": that word already names a goal
                // TYPE (auto-recurring) and the badge on each row. Reusing it for a
                // one-off bulk action made four unrelated things share one label.
                FlatActionChip(
                    label = "Repeat goals",
                    icon = {
                        Icon(
                            Icons.Default.Repeat,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = GoalsFlatColors.Muted,
                        )
                    },
                    filled = false,
                    onClick = { showRepeatPicker = true },
                )
                FlatActionChip(
                    label = "Insights",
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = GoalsFlatColors.Muted,
                        )
                    },
                    filled = false,
                    onClick = { onNavigate(Routes.nishthaAnalytics("goals")) },
                )
                FlatActionChip(
                    label = "Recently deleted",
                    icon = {
                        Icon(
                            Icons.Default.DeleteSweep,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = GoalsFlatColors.Muted,
                        )
                    },
                    filled = false,
                    onClick = {
                        showDeletedSheet = true
                        viewModel.loadRecentlyDeletedGoals()
                    },
                )
            }

            Spacer(Modifier.height(18.dp))
            PlanHairline()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (totalToday == 0) "Nothing today" else "$doneToday of $totalToday",
                    fontFamily = LoraFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoalsFlatColors.Done,
                )
                Text(
                    if (totalToday == 0) "add a goal to get started" else "goals done today",
                    fontSize = 13.sp,
                    color = GoalsFlatColors.Muted,
                )
            }
            PlanHairline()

            Spacer(Modifier.height(14.dp))
            GoalsUnderlineTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
            Spacer(Modifier.height(4.dp))
        }

        when (selectedTab) {
            0, 1, 2 -> GoalsTab(
                filterMode = when (selectedTab) {
                    0 -> "today"
                    1 -> "upcoming"
                    else -> "missed"
                },
                goals = uiState.goals,
                ekagraAnalytics = uiState.ekagraAnalytics,
                isLoading = uiState.isLoadingGoals,
                goalError = uiState.goalError,
                onRefresh = { viewModel.refreshGoals() },
                onAddClick = { showAddSheet = true },
                onComplete = { goal -> completeGoal = goal; studyHours = 0; studyMinutes = 0 },
                onReopen = { goal -> viewModel.reopenGoal(goal.id) },
                onEdit = { goal ->
                    editGoal = goal
                    editTitle = goal.title
                    editDesc = goal.description ?: ""
                    editPriority = goal.priority
                    editRepeatDaily = goal.goalKind == "repeat"
                    editGoalKind = if (goal.goalKind == "scheduled") "scheduled" else "today"
                    editScheduleChanged = false
                    editUnitType = goal.unitType
                    editStatus = goal.status
                    editCarryForward = goal.carryForwardMode
                    selectedDate = IstDateUtils.getDateKey(goal.scheduledDate)
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: LocalDate.now(IstDateUtils.zone).plusDays(1)
                },
                onDelete = { goal -> deleteGoal = goal },
            )
            3 -> HistoryTab(
                goals = uiState.goals,
                isSaving = uiState.isSavingGoal,
                onReopen = { goal -> viewModel.reopenGoal(goal.id) },
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
    )
    }
}

@Composable
private fun FlatActionChip(
    label: String,
    icon: @Composable () -> Unit,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(GoalsFlatColors.Primary)
                } else {
                    Modifier.border(1.dp, GoalsFlatColors.Hairline, shape)
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(if (filled) 4.dp else 6.dp))
            Text(
                label,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                fontWeight = FontWeight.SemiBold,
                color = if (filled) Color.White else GoalsFlatColors.Text,
            )
        }
    }
}

@Composable
private fun GoalsUnderlineTabs(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        tabs.forEachIndexed { i, title ->
            val selected = selectedTab == i
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(i) },
                    ),
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) GoalsFlatColors.Text else GoalsFlatColors.Muted,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (selected) GoalsFlatColors.Primary else Color.Transparent),
                )
            }
        }
    }
}

/** A tappable, typeable hours/minutes digit box for the "How long did you study?"
 *  dialog — previously a plain [Text] with no input handling at all, so the quick-add
 *  chips were the only way to set a value. */
@Composable
private fun TimeDigitField(
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int,
) {
    var textValue by remember { mutableStateOf(if (value == 0) "" else value.toString()) }

    // Sync from outside if value changed independently of typing (e.g. from quick-add chips)
    LaunchedEffect(value) {
        if (textValue.toIntOrNull() != value) {
            textValue = if (value == 0) "" else value.toString()
        }
    }

    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GoalsFlatColors.PrimarySoft)
            .border(1.5.dp, GoalsFlatColors.Primary, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(2)
                textValue = digits
                val parsed = digits.toIntOrNull() ?: 0
                if (parsed > maxValue) {
                    textValue = maxValue.toString()
                    onValueChange(maxValue)
                } else {
                    onValueChange(parsed)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GoalsFlatColors.Primary,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(GoalsFlatColors.Primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "00",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoalsFlatColors.Primary.copy(alpha = 0.35f),
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
