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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import java.time.LocalDate
import java.time.LocalTime

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
    val tabs = listOf("Today", "Upcoming", "Completed")
    var showAddSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("medium") }
    var newGoalKind by remember { mutableStateOf("today") }
    var newCarryForward by remember { mutableStateOf("none") }
    var newSubtaskInput by remember { mutableStateOf("") }
    var newSubtasks by remember { mutableStateOf(listOf<String>()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now(IstDateUtils.zone)) }
    var selectedHour by remember { mutableStateOf(LocalTime.now(IstDateUtils.zone).hour) }
    var selectedMinute by remember { mutableStateOf(LocalTime.now(IstDateUtils.zone).minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Edit state
    var editGoal by remember { mutableStateOf<Goal?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("medium") }
    var editGoalKind by remember { mutableStateOf("today") }
    var editUnitType by remember { mutableStateOf("binary") }
    var editStatus by remember { mutableStateOf("not_started") }
    var editCarryForward by remember { mutableStateOf("none") }

    // Complete / study time dialog
    var completeGoal by remember { mutableStateOf<Goal?>(null) }
    // Deleting a goal is permanent and has no undo, and the menu item sits right
    // under "Repeat Task" — one stray tap used to destroy a goal outright.
    var deleteGoal by remember { mutableStateOf<Goal?>(null) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var studyHours by remember { mutableIntStateOf(0) }
    var studyMinutes by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = LocalDate.now(IstDateUtils.zone)
                val date = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(today) && !date.isAfter(today.plusDays(7))
            }
        }
    )
    val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = false)

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

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = GoalsFlatColors.Bg,
            title = {
                Text(
                    "Select Start Time (IST)",
                    color = GoalsFlatColors.Text,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val isToday = selectedDate == LocalDate.now(IstDateUtils.zone)
                    val picked = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    if (isToday && picked.isBefore(LocalTime.now(IstDateUtils.zone))) {
                        Toast.makeText(context, "Cannot set a past time for today's goal.", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                }) { Text("OK", color = GoalsFlatColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = GoalsFlatColors.Muted)
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    // Study time completion dialog
    if (showRepeatPicker) {
        // Yesterday's list, most recent first. Completed ones are included — a
        // finished daily habit is exactly what a student wants to bring forward —
        // but everything is opt-out, so nothing returns unless they leave it ticked.
        val yesterdayKey = remember {
            runCatching {
                LocalDate.parse(IstDateUtils.todayKey()).minusDays(1).toString()
            }.getOrDefault(IstDateUtils.todayKey())
        }
        val candidates = remember(uiState.goals, yesterdayKey) {
            uiState.goals.filter {
                it.source != "ekagra" &&
                    it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
                    IstDateUtils.getDateKey(it.scheduledDate ?: it.createdAt) == yesterdayKey
            }
        }
        val selectedIds = remember(candidates) {
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
                    "Bring yesterday forward",
                    fontFamily = LoraFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = GoalsFlatColors.Text,
                )
                Text(
                    if (candidates.isEmpty()) {
                        "You had no goals yesterday."
                    } else {
                        "Untick anything you are done with."
                    },
                    fontSize = 13.sp,
                    color = GoalsFlatColors.Muted,
                )
                PlanHairline()

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
                                    if (goal.completed) "Done yesterday" else goal.goalKindLabel(),
                                    fontSize = 11.5.sp,
                                    color = GoalsFlatColors.Muted,
                                )
                            }
                        }
                        PlanHairline(alpha = 0.5f)
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.repeatGoals(selectedIds.toList())
                        showRepeatPicker = false
                    },
                    enabled = selectedIds.isNotEmpty() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedIds.isEmpty()) "Choose at least one"
                        else "Repeat ${selectedIds.size} goal${if (selectedIds.size == 1) "" else "s"} today",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIds.isEmpty()) GoalsFlatColors.Muted else GoalsFlatColors.Primary,
                    )
                }
            }
        }
    }

    deleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { deleteGoal = null },
            title = { Text("Delete this goal?") },
            text = { Text("\"${goal.title}\" will be removed for good. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGoal(goal.id)
                    deleteGoal = null
                    Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Delete", color = GoalsFlatColors.Danger)
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
                            .clickable {
                                val totalMins = studyHours * 60 + studyMinutes
                                viewModel.completeGoal(goal.id, totalMins)
                                completeGoal = null; studyHours = 0; studyMinutes = 0
                                Toast.makeText(context, "Goal completed!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        "Skip",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoalsFlatColors.Primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.completeGoal(goal.id, 0)
                                completeGoal = null
                                Toast.makeText(context, "Goal completed!", Toast.LENGTH_SHORT).show()
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
                Text("Goal Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Text)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    buildList {
                        if (editGoalKind == "one_time") add(Triple("one_time", "One-time (legacy)", "No fixed day. Complete it whenever."))
                        add(Triple("today", "Today", "A task for today only. Disappears tomorrow."))
                        add(Triple("repeat", "Repeat", "Recurs automatically every day. Edit it once and future days pick up the change."))
                        add(Triple("scheduled", "Scheduled", "Set a goal for a future date."))
                    }.forEach { (value, label, hint) ->
                        AssistOptionRow(
                            selected = editGoalKind == value,
                            title = label,
                            subtitle = hint,
                            onClick = {
                                editGoalKind = value
                                if (value == "scheduled" && !selectedDate.isAfter(LocalDate.now(IstDateUtils.zone))) {
                                    selectedDate = LocalDate.now(IstDateUtils.zone).plusDays(1)
                                }
                                if (value == "repeat") editCarryForward = "full"
                            }
                        )
                    }
                }
                if (editGoalKind == "scheduled") {
                    PlanHairline(alpha = 0.6f)
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
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
                            val scheduledDate = when (editGoalKind) {
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
                                goalKind = editGoalKind,
                                unitType = editUnitType,
                                linkedFocusEnabled = false,
                                plannedFocusMinutes = null,
                                targetValue = goal.targetValue,
                                achievedValue = goal.achievedValue,
                                status = editStatus,
                                carryForwardMode = if (editGoalKind == "scheduled" || editGoalKind == "one_time") "none" else editCarryForward
                            )
                            editGoal = null
                            Toast.makeText(context, "Goal updated!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    LaunchedEffect(uiState.goalSaveSuccess) {
        if (uiState.goalSaveSuccess) {
            Toast.makeText(context, "Goal saved!", Toast.LENGTH_SHORT).show()
            showAddSheet = false; newTitle = ""; newDesc = ""; newSubtasks = emptyList(); newSubtaskInput = ""
            newGoalKind = "today"; newCarryForward = "none"
            selectedDate = LocalDate.now(IstDateUtils.zone)
            LocalTime.now(IstDateUtils.zone).also { selectedHour = it.hour; selectedMinute = it.minute }
            viewModel.onEvent(NishthaEvent.ClearGoalSuccess)
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
                Text("Goal Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoalsFlatColors.Text)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("today", "Today", "A task for today only. Disappears tomorrow."),
                        Triple("repeat", "Repeat", "Recurs automatically every day. Edit it once and future days pick up the change."),
                        Triple("scheduled", "Scheduled", "Set a goal for a future date.")
                    ).forEach { (value, label, hint) ->
                        AssistOptionRow(
                            selected = newGoalKind == value,
                            title = label,
                            subtitle = hint,
                            onClick = {
                                newGoalKind = value
                                newCarryForward = if (value == "repeat") "full" else "none"
                                selectedDate = if (value == "scheduled") LocalDate.now(IstDateUtils.zone).plusDays(1) else LocalDate.now(IstDateUtils.zone)
                            }
                        )
                    }
                }
                if (newGoalKind == "scheduled") {
                    PlanHairline(alpha = 0.6f)
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
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
                                goalKind = newGoalKind,
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

    LaunchedEffect(uiState.goalMessage) {
        uiState.goalMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearGoalMessage()
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
    val doneToday = standardGoals.count {
        it.isCompletedForStats() && it.completedDateKey() == todayKey
    }
    val pendingToday = standardGoals.count {
        !it.completed &&
            it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
            !(it.lifecycleStatus == "missed" && it.nextInstanceCreated) &&
            !it.isDormant(todayKey)
    }
    val totalToday = doneToday + pendingToday

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalsFlatColors.Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 0.dp),
        ) {
            PlanEyebrow("Nishtha")
            Spacer(Modifier.height(14.dp))
            Text(
                "My Goals",
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = GoalsFlatColors.Text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                // "8 goals · 0 of 0 done today" put two different scopes side by
                // side — every goal ever, next to today's — and read as broken.
                // Say plainly when today is simply empty.
                if (totalToday == 0) {
                    "${standardGoals.size} goals · nothing planned for today"
                } else {
                    "${standardGoals.size} goals · $doneToday of $totalToday done today"
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
                FlatActionChip(
                    label = "Repeat",
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
            0, 1 -> GoalsTab(
                filterMode = if (selectedTab == 0) "today" else "upcoming",
                goals = uiState.goals,
                rolloverPrompts = uiState.rolloverPrompts,
                ekagraAnalytics = uiState.ekagraAnalytics,
                isLoading = uiState.isLoadingGoals,
                goalError = uiState.goalError,
                onRefresh = { viewModel.onEvent(com.safarparmar.app.ui.nishtha.NishthaEvent.LoadGoals) },
                onAddClick = { showAddSheet = true },
                onComplete = { goal -> completeGoal = goal; studyHours = 0; studyMinutes = 0 },
                onEdit = { goal ->
                    editGoal = goal
                    editTitle = goal.title
                    editDesc = goal.description ?: ""
                    editPriority = goal.priority
                    editGoalKind = goal.goalKind
                    editUnitType = goal.unitType
                    editStatus = goal.status
                    editCarryForward = goal.carryForwardMode
                    selectedDate = IstDateUtils.getDateKey(goal.scheduledDate)
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: LocalDate.now(IstDateUtils.zone).plusDays(1)
                },
                onDelete = { goal -> deleteGoal = goal },
                onRepeat = { goal ->
                    val today = IstDateUtils.todayKey()
                    // The toast used to fire here, before the request returned, so it
                    // claimed success even when the server deduped (or failed).
                    // It is now driven by goalMessage once the result is known.
                    viewModel.repeatGoal(goal.id, IstDateUtils.dateKeyToUtcIso(today))
                },
                onRolloverRetry = { goal -> viewModel.respondToRollover(goal.id, "retry") },
                onRolloverArchive = { goal -> viewModel.respondToRollover(goal.id, "archive") },
            )
            2 -> HistoryTab(uiState.goals)
        }
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
        modifier = Modifier.fillMaxWidth(),
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
