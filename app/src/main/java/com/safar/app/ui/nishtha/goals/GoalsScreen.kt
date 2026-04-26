package com.safar.app.ui.nishtha.goals

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.domain.model.Goal
import com.safar.app.domain.model.GoalSubtask
import com.safar.app.ui.nishtha.NishthaEvent
import com.safar.app.ui.nishtha.NishthaViewModel
import com.safar.app.ui.navigation.Routes
import com.safar.app.util.IstDateUtils
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: NishthaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Goals", "History", "Analytics")
    var showAddSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("medium") }
    var newGoalKind by remember { mutableStateOf("today") }
    var newUnitType by remember { mutableStateOf("binary") }
    var newLinkedFocus by remember { mutableStateOf(false) }
    var newPlannedMinutes by remember { mutableIntStateOf(25) }
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
    var editLinkedFocus by remember { mutableStateOf(false) }
    var editPlannedMinutes by remember { mutableIntStateOf(25) }
    var editStatus by remember { mutableStateOf("not_started") }
    var editCarryForward by remember { mutableStateOf("none") }

    // Complete / study time dialog
    var completeGoal by remember { mutableStateOf<Goal?>(null) }
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Start Time (IST)") },
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    // Study time completion dialog
    completeGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { completeGoal = null; studyHours = 0; studyMinutes = 0 },
            title = { Text("How long did you study?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HOURS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.size(width = 90.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("%02d".format(studyHours), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MINUTES", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.size(width = 90.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("%02d".format(studyMinutes), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("+15m" to 15, "+30m" to 30, "+1h" to 60, "+2h" to 120).forEach { (label, mins) ->
                            OutlinedButton(
                                onClick = {
                                    val total = studyHours * 60 + studyMinutes + mins
                                    studyHours = total / 60
                                    studyMinutes = total % 60
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) { Text(label, fontSize = 11.sp) }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val totalMins = studyHours * 60 + studyMinutes
                            viewModel.completeGoal(goal.id, totalMins)
                            completeGoal = null; studyHours = 0; studyMinutes = 0
                            Toast.makeText(context, "Goal completed! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Done", fontWeight = FontWeight.SemiBold) }
                    TextButton(
                        onClick = {
                            viewModel.completeGoal(goal.id, 0)
                            completeGoal = null
                            Toast.makeText(context, "Goal completed!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Skip", color = MaterialTheme.colorScheme.primary) }
                    OutlinedButton(
                        onClick = { completeGoal = null; studyHours = 0; studyMinutes = 0 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Cancel") }
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Edit Goal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("What do you want to do?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Goal Type", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    buildList {
                        if (editGoalKind == "one_time") add(Triple("one_time", "One-time (legacy)", "No fixed day. Complete it whenever."))
                        if (editGoalKind == "repeat") add(Triple("repeat", "Repeat (legacy)", "Recurs daily. Carries forward if not completed."))
                        add(Triple("today", "Today", "A task for today only. Disappears tomorrow."))
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
                            }
                        )
                    }
                }
                if (editGoalKind == "scheduled") {
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
                }
                Text("How will you track it?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    buildList {
                        add("binary" to "Done / Not done")
                        add("duration_minutes" to "Time (focus timer)")
                        if (editUnitType == "checklist") add("checklist" to "Checklist")
                    }.forEach { (value, label) ->
                        FilterChip(
                            selected = editUnitType == value,
                            onClick = { editUnitType = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Add details (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Button(
                    onClick = {
                        val scheduledDate = when (editGoalKind) {
                            "today" -> IstDateUtils.todayKey()
                            "scheduled" -> selectedDate.toString()
                            "one_time" -> goal.scheduledDate
                            else -> IstDateUtils.todayKey()
                        }
                        val preservedTarget = goal.targetValue ?: goal.plannedFocusMinutes
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
                            linkedFocusEnabled = editUnitType == "duration_minutes",
                            plannedFocusMinutes = if (editUnitType == "duration_minutes") preservedTarget else null,
                            targetValue = if (editUnitType == "duration_minutes") preservedTarget else null,
                            achievedValue = goal.achievedValue,
                            status = editStatus,
                            carryForwardMode = if (editGoalKind == "scheduled" || editGoalKind == "one_time") "none" else editCarryForward
                        )
                        editGoal = null
                        Toast.makeText(context, "Goal updated!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = editTitle.isNotBlank() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Save Changes") }
            }
        }
    }

    LaunchedEffect(uiState.goalSaveSuccess) {
        if (uiState.goalSaveSuccess) {
            Toast.makeText(context, "Goal saved!", Toast.LENGTH_SHORT).show()
            showAddSheet = false; newTitle = ""; newDesc = ""; newSubtasks = emptyList(); newSubtaskInput = ""
            newGoalKind = "today"; newUnitType = "binary"; newLinkedFocus = false; newPlannedMinutes = 25; newCarryForward = "none"
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("New Goal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("What do you want to do?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Goal Type", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("today", "Today", "A task for today only. Disappears tomorrow."),
                        Triple("scheduled", "Scheduled", "Set a goal for a future date.")
                    ).forEach { (value, label, hint) ->
                        AssistOptionRow(
                            selected = newGoalKind == value,
                            title = label,
                            subtitle = hint,
                            onClick = {
                                newGoalKind = value
                                newCarryForward = "none"
                                selectedDate = if (value == "scheduled") LocalDate.now(IstDateUtils.zone).plusDays(1) else LocalDate.now(IstDateUtils.zone)
                            }
                        )
                    }
                }
                if (newGoalKind == "scheduled") {
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
                }
                Text("How will you track it?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("binary" to "Done / Not done", "duration_minutes" to "Time (focus timer)").forEach { (value, label) ->
                        FilterChip(
                            selected = newUnitType == value,
                            onClick = { newUnitType = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Add details (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                if (uiState.goalError != null) Text(uiState.goalError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(
                    onClick = {
                        val scheduledDate = if (newGoalKind == "scheduled") selectedDate.toString() else IstDateUtils.todayKey()
                        viewModel.addGoal(
                            title = newTitle.trim(),
                            description = newDesc.ifBlank { null },
                            priority = newPriority,
                            scheduledDate = scheduledDate,
                            startedAt = null,
                            subtasks = emptyList(),
                            goalKind = newGoalKind,
                            unitType = newUnitType,
                            linkedFocusEnabled = newUnitType == "duration_minutes",
                            plannedFocusMinutes = null,
                            targetValue = null,
                            achievedValue = 0,
                            status = "not_started",
                            carryForwardMode = "none"
                        )
                    },
                    enabled = newTitle.isNotBlank() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (uiState.isSavingGoal) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Create Goal") }
                }
            }
        }
    }

    if (showStatusSheet) {
        AlertDialog(
            onDismissRequest = { showStatusSheet = false },
            confirmButton = {
                TextButton(onClick = { showStatusSheet = false }) { Text("Close") }
            },
            title = { Text("Status", fontWeight = FontWeight.Bold) },
            text = {
                StatusGrid(goals = uiState.goals, ekagraAnalytics = uiState.ekagraAnalytics)
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Command Center", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("Manage your goals and track progress efficiently.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = { showStatusSheet = true },
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Status", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showAddSheet = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Add Goal", fontSize = 13.sp, color = Color.White)
            }
        }
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { i, title -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title, fontSize = 13.sp) }) }
        }
        when (selectedTab) {
            0 -> GoalsTab(
                goals = uiState.goals,
                rolloverPrompts = uiState.rolloverPrompts,
                ekagraAnalytics = uiState.ekagraAnalytics,
                isLoading = uiState.isLoadingGoals,
                onAddClick = { showAddSheet = true },
                onStartFocus = { goal -> onNavigate(Routes.ekagraForGoal(goal.id, goal.title.ifBlank { goal.text })) },
                onComplete = { goal -> completeGoal = goal; studyHours = 0; studyMinutes = 0 },
                onEdit = { goal ->
                    editGoal = goal
                    editTitle = goal.title
                    editDesc = goal.description ?: ""
                    editPriority = goal.priority
                    editGoalKind = goal.goalKind
                    editUnitType = goal.unitType
                    editLinkedFocus = goal.linkedFocusEnabled
                    editPlannedMinutes = goal.plannedFocusMinutes ?: goal.targetValue ?: 25
                    editStatus = goal.status
                    editCarryForward = goal.carryForwardMode
                    selectedDate = IstDateUtils.getDateKey(goal.scheduledDate)
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?: LocalDate.now(IstDateUtils.zone).plusDays(1)
                },
                onDelete = { goal -> viewModel.deleteGoal(goal.id); Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show() },
                onRepeat = { goal ->
                    val today = IstDateUtils.todayKey()
                    viewModel.repeatGoal(goal.id, IstDateUtils.dateKeyToUtcIso(today))
                    Toast.makeText(context, "Goal repeated for today!", Toast.LENGTH_SHORT).show()
                },
                onRolloverRetry = { goal -> viewModel.respondToRollover(goal.id, "retry") },
                onRolloverArchive = { goal -> viewModel.respondToRollover(goal.id, "archive") },
            )
            1 -> HistoryTab(uiState.goals)
            2 -> GoalsAnalyticsTab(uiState.goals, uiState.ekagraAnalytics)
        }
    }
}

@Composable
private fun StatusGrid(goals: List<Goal>, ekagraAnalytics: com.safar.app.domain.model.EkagraAnalyticsStats) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = standardGoals.filter { !it.completed && it.lifecycleStatus !in listOf("abandoned", "rolled_over") && !it.isDormant(todayKey) }
    val scheduled = standardGoals.filter { !it.completed && it.isDormant(todayKey) }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val doneToday = manualCompletedGoals.count { it.completedDateKey() == todayKey }
    val timerLinked = standardGoals.count { !it.isDormant(todayKey) && it.linkedFocusEnabled }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatInfoCard("Done Today", doneToday.toString(), "Manual goals completed on today's date.", Modifier.weight(1f), Color(0xFF10B981))
            StatInfoCard("Open Now", pending.size.toString(), "Active manual goals available to work on right now.", Modifier.weight(1f), Color(0xFF0EA5E9))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatInfoCard("Scheduled Ahead", scheduled.size.toString(), "Future goals parked until their scheduled day arrives.", Modifier.weight(1f), Color(0xFF8B5CF6))
            StatInfoCard("Timer Linked", timerLinked.toString(), "Goals that can jump straight into Ekagra sessions.", Modifier.weight(1f), Color(0xFFFFB300))
        }
    }
}

@Composable
private fun GoalsTab(
    goals: List<Goal>,
    rolloverPrompts: List<Goal>,
    ekagraAnalytics: com.safar.app.domain.model.EkagraAnalyticsStats,
    isLoading: Boolean,
    onAddClick: () -> Unit,
    onStartFocus: (Goal) -> Unit,
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
    val timerLinked = standardGoals.count { !it.isDormant(todayKey) && it.linkedFocusEnabled }
    val focusTodayMinutes = ekagraAnalytics.focusSessions
        .filter { !it.associatedGoalId.isNullOrBlank() && IstDateUtils.getDateKey(it.startedAt) == todayKey }
        .sumOf { it.actualMinutes }
    val focusTotalMinutes = ekagraAnalytics.focusSessions
        .filter { !it.associatedGoalId.isNullOrBlank() }
        .sumOf { it.actualMinutes }
    val manualTodayMinutes = manualCompletedGoals.filter { it.completedDateKey() == todayKey }.sumOf { it.studiedMinutes ?: 0 }
    val manualTotalMinutes = manualCompletedGoals.sumOf { it.studiedMinutes ?: 0 }
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }; return }
    if (goals.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎯", fontSize = 48.sp)
                Text("No goals yet! Add a goal to get started.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Button(onClick = onAddClick, shape = RoundedCornerShape(12.dp)) { Text("Add Goal") }
            }
        }; return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pending.isNotEmpty()) {
            item { SectionHeader("Pending", "Only goals that are active today appear here.", "${pending.size} Tasks") }
            items(pending, key = { it.id }) { GoalItem(it, onStartFocus = { onStartFocus(it) }, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        } else {
            item { EmptyGoalsCard("All caught up! Time to plan more?", "Anything scheduled for later stays in the upcoming section below.") }
        }
        if (scheduled.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); SectionHeader("Scheduled Tasks", "These stay quiet until their scheduled date arrives.", "${scheduled.size} upcoming") }
            items(scheduled, key = { "scheduled-${it.id}" }) { GoalItem(it, onStartFocus = { }, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        }
        if (completed.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); Text("Completed", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(completed.take(5), key = { it.id }) { GoalItem(it, onStartFocus = { }, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
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

@Composable
private fun AssistOptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScheduledDatePickerRow(selectedDate: LocalDate, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("When should this activate?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(IstDateUtils.labelFor(selectedDate.toString()))
        }
        Text("This goal will activate on ${IstDateUtils.labelFor(selectedDate.toString())}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuideSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        items.forEach { item ->
            Text("- $item", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, badge: String) {
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
private fun EmptyGoalsCard(title: String, subtitle: String) {
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
private fun StatInfoCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 124.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LivePulseCard(
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
            Text("Live Pulse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Focus Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$completedToday Completed", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("$openManualGoals open manual goals", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$completionRate% overall completion rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Text("Study Time Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatStudyTime(studyToday), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatInfoCard("Manual", formatStudyTime(manualToday), "", Modifier.weight(1f))
                StatInfoCard("Ekagra", formatStudyTime(ekagraToday), "", Modifier.weight(1f))
            }
            Text("Daily Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (dailyProgress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            Text("Total Time Studied", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GoalTimeRow(Icons.Default.Timer, "Ekagra Mode", "", formatStudyTime(totalEkagra), Color(0xFF4F46E5))
            GoalTimeRow(Icons.Default.Book, "Manual Goal", "", formatStudyTime(totalManual), Color(0xFF0F9F8A))
        }
    }
}

@Composable
private fun ProTipCard() {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Pro Tip", fontWeight = FontWeight.Bold)
            Text("Consistent daily completion is better than occasional bursts. Break large goals into smaller focus tasks.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalItem(goal: Goal, onStartFocus: () -> Unit, onComplete: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onRepeat: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = goal.progressPercent()
    val showProgress = goal.unitType != "binary" && (goal.unitType == "checklist" || goal.targetValue != null || goal.plannedFocusMinutes != null)
    Card(
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .border(1.5.dp, if (goal.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape)
                    .background(if (goal.completed) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(enabled = !goal.completed) { onComplete() },
                contentAlignment = Alignment.Center,
            ) { if (goal.completed) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Column(Modifier.weight(1f)) {
                Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (!goal.description.isNullOrBlank()) {
                    Text(goal.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    SmallBadge(goal.goalKindLabel(), MaterialTheme.colorScheme.primary.copy(0.10f), MaterialTheme.colorScheme.primary)
                    SmallBadge(goal.unitTypeLabel(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goal.status !in listOf("not_started", "completed") || goal.completed) {
                        SmallBadge(goal.statusLabel(), statusBadgeBg(goal.status), statusBadgeFg(goal.status))
                    }
                }
                if (goal.source == "ekagra" || goal.linkedFocusEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 5.dp)) {
                        if (goal.source == "ekagra") SmallBadge("Ekagra mode task", MaterialTheme.colorScheme.tertiary.copy(0.12f), MaterialTheme.colorScheme.tertiary)
                        if (goal.linkedFocusEnabled) SmallBadge("Timer linked", Color(0xFFFFB300).copy(alpha = 0.14f), Color(0xFFB26A00))
                    }
                }
                goal.scheduledDate?.let { Text(IstDateUtils.labelFor(it), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                if (goal.completed && (goal.studiedMinutes ?: 0) > 0) {
                    Text("${formatStudyTime(goal.studiedMinutes ?: 0)} studied", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                }
                if (showProgress) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (goal.linkedFocusEnabled) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
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
                        if (goal.lifecycleStatus !in listOf("abandoned", "rolled_over") && !goal.isDormant(IstDateUtils.todayKey())) {
                            DropdownMenuItem(text = { Text("Start Focus") }, leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showMenu = false; onStartFocus() })
                        }
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
private fun RolloverPromptItem(goal: Goal, onRetry: () -> Unit, onArchive: () -> Unit) {
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
private fun SmallBadge(label: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority) {
        "high"   -> MaterialTheme.colorScheme.error.copy(0.12f) to MaterialTheme.colorScheme.error
        "medium" -> MaterialTheme.colorScheme.primary.copy(0.12f) to MaterialTheme.colorScheme.primary
        else     -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.1f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(priority.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(goals: List<Goal>) {
    val completed = goals.filter { it.completed }
    val today = LocalDate.now(IstDateUtils.zone)
    var selectedDate by remember { mutableStateOf(today) }
    var showHistoryDatePicker by remember { mutableStateOf(false) }
    val historyDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val filtered = completed.filter { goal ->
        val d = goal.completedAt?.take(10) ?: goal.scheduledDate?.take(10)
        d == selectedDate.toString()
    }

    if (showHistoryDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showHistoryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    historyDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showHistoryDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showHistoryDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = historyDatePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Review what was completed on a specific day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showHistoryDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())), fontSize = 13.sp)
                }
                if (selectedDate != today) {
                    TextButton(onClick = { selectedDate = today }) { Text("Reset", fontSize = 12.sp) }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing found for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { GoalItem(it, onStartFocus = {}, onComplete = {}, onEdit = {}, onDelete = {}, onRepeat = {}) }
        }
    }
}

@Composable
private fun GoalsAnalyticsTab(goals: List<Goal>, ekagraAnalytics: com.safar.app.domain.model.EkagraAnalyticsStats) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val activeTodayGoals = standardGoals.filter { !it.isDormant(todayKey) }
    val total = activeTodayGoals.size
    val rate = if (total > 0) kotlin.math.round(manualCompletedGoals.size * 100f / total).toInt() else 0
    val linkedFocusTotal = ekagraAnalytics.focusSessions.filter { !it.associatedGoalId.isNullOrBlank() }.sumOf { it.actualMinutes }
    val totalStudiedMinutes = manualCompletedGoals.sumOf { it.studiedMinutes ?: 0 }
    val avgStudiedMinutes = if (manualCompletedGoals.isNotEmpty()) totalStudiedMinutes / manualCompletedGoals.size else 0
    val avgProgress = if (activeTodayGoals.isNotEmpty()) kotlin.math.round(activeTodayGoals.map { it.progressPercent() }.average()).toInt() else 0
    val sevenDaySeries = remember(goals, todayKey) {
        val today = LocalDate.now(IstDateUtils.zone)
        (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val key = day.toString()
            val dayGoals = standardGoals.filter { goal -> goal.anchorDateKey() == key }
            val done = dayGoals.count { goal -> goal.statusBucket() == "completed" }
            val avg = if (dayGoals.isNotEmpty()) kotlin.math.round(dayGoals.map { it.progressPercent() }.average()).toInt() else 0
            GoalAnalyticsDay(day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), key, done, dayGoals.size, avg)
        }
    }
    val consistencyDays = sevenDaySeries.count { it.completed > 0 }
    val currentStreak = sevenDaySeries.asReversed().takeWhile { it.completed > 0 }.size
    val averageDailyCompletion = if (sevenDaySeries.isNotEmpty()) {
        (sevenDaySeries.sumOf { it.completed }.toFloat() / sevenDaySeries.size).let { "%.1f".format(Locale.US, it) }
    } else "0.0"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Goal Analytics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Completion insights and goal progress from Nishtha goals.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("COMPLETION RATE", "$rate%", "${manualCompletedGoals.size} of $total active manual goals completed", Color(0xFF10A968), Modifier.weight(1f))
            GoalMetricCard("AVERAGE PROGRESS", "$avgProgress%", "Future scheduled goals stay excluded until their date arrives.", Color(0xFF0EA5E9), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalMetricCard("CONSISTENCY (7 DAYS)", "$consistencyDays/7", "Days with at least one completed manual goal", Color(0xFF8B5CF6), Modifier.weight(1f))
            GoalMetricCard("CURRENT STREAK", "${currentStreak}d", "Consecutive days with completions", Color(0xFFFFB300), Modifier.weight(1f))
        }

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Study Time Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("How your total study time is distributed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StudyTimeSplitBar(manualMinutes = totalStudiedMinutes, focusMinutes = linkedFocusTotal)
                GoalTimeRow(Icons.Default.Book, "Manual Study", "Self-reported when completing goals", formatStudyTime(totalStudiedMinutes), Color(0xFF0F9F8A))
                GoalTimeRow(Icons.Default.Timer, "Focus Timer", "Ekagra sessions linked to goals only", formatStudyTime(linkedFocusTotal), Color(0xFF4F46E5))
                GoalTimeRow(Icons.Default.BarChart, "Avg per Completed Goal", "", formatStudyTime(avgStudiedMinutes), MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10A968), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Goal Consistency Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Your goal completion over the last 7 days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                GoalConsistencyChart(sevenDaySeries)
            }
        }

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Weekly Growth Pulse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text("$averageDailyCompletion avg/day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                sevenDaySeries.forEach { entry -> WeeklyGrowthRow(entry) }
            }
        }
    }
}

private data class GoalAnalyticsDay(
    val dayLabel: String,
    val dayKey: String,
    val completed: Int,
    val total: Int,
    val avgProgress: Int
)

@Composable
private fun GoalMetricCard(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier.heightIn(min = 130.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudyTimeSplitBar(manualMinutes: Int, focusMinutes: Int) {
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
        val manual = manualMinutes.coerceAtLeast(0)
        val focus = focusMinutes.coerceAtLeast(0)
        if (manual + focus == 0) {
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            if (manual > 0) Box(Modifier.weight(manual.toFloat()).fillMaxHeight().background(Color(0xFF0F9F8A)))
            if (focus > 0) Box(Modifier.weight(focus.toFloat()).fillMaxHeight().background(Color(0xFF4F46E5)))
        }
    }
}

@Composable
private fun GoalTimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.07f)).border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
    }
}

@Composable
private fun GoalConsistencyChart(days: List<GoalAnalyticsDay>) {
    val maxScore = 100
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val score = if (day.total > 0) day.completed * 100 / day.total else 0
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth(0.72f)
                            .fillMaxHeight((score.toFloat() / maxScore).coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (day.completed > 0) Color(0xFF10A968) else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(day.dayLabel.take(1), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeeklyGrowthRow(entry: GoalAnalyticsDay) {
    val pct = if (entry.total > 0) entry.completed * 100 / entry.total else 0
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.dayLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${entry.completed}/${entry.total} done", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF10A968), trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
        Text("Average progress: ${entry.avgProgress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatStudyTime(mins: Int): String {
    if (mins <= 0) return "0m"
    val hours = mins / 60
    val minutes = mins % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun Goal.isCompletedForStats(): Boolean =
    completed || !completedAt.isNullOrBlank()

private fun Goal.completedDateKey(): String? =
    IstDateUtils.getDateKey(completedAt)

private fun Goal.anchorDateKey(): String? =
    IstDateUtils.getDateKey(scheduledDate)
        ?: IstDateUtils.getDateKey(createdAt)
        ?: IstDateUtils.getDateKey(startedAt)

private fun Goal.statusBucket(): String = when {
    status == "cancelled" -> "cancelled"
    status == "missed" || status == "expired" -> "missed"
    status == "partial" -> "partial"
    isCompletedForStats() -> "completed"
    else -> "open"
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(shape = RoundedCornerShape(14.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Goal.isDormant(todayKey: String): Boolean {
    if (goalKind != "scheduled") return false
    val key = IstDateUtils.getDateKey(scheduledDate) ?: return false
    return key > todayKey
}

private fun Goal.goalKindLabel(): String = when (goalKind) {
    "scheduled" -> "Scheduled"
    "repeat" -> "Repeat"
    "one_time" -> "One-time"
    else -> "Today"
}

private fun Goal.unitTypeLabel(): String = when (unitType) {
    "duration_minutes" -> "Time"
    "count" -> "Count"
    "checklist" -> "Checklist"
    else -> "Done"
}

private fun Goal.statusLabel(): String = when (if (completed) "completed" else status) {
    "in_progress" -> "In progress"
    "partial" -> "Partial"
    "missed" -> "Missed"
    "cancelled" -> "Cancelled"
    "expired" -> "Expired"
    "rolled_over" -> "Rolled over"
    "completed" -> "Completed"
    else -> "Not started"
}

private fun Goal.progressPercent(): Int {
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

private fun Goal.progressLabel(): String = when (unitType) {
    "duration_minutes" -> {
        val target = targetValue ?: plannedFocusMinutes
        if (target != null && target > 0) "Progress $achievedValue / ${target}m" else "Progress ${achievedValue}m"
    }
    "count" -> if ((targetValue ?: 0) > 0) "Progress $achievedValue / $targetValue" else "Progress $achievedValue"
    "checklist" -> "${subtasks.count { it.done }} / ${subtasks.size} subtasks"
    else -> ""
}

@Composable
private fun statusBadgeBg(status: String): Color = when (status) {
    "missed", "expired", "cancelled" -> MaterialTheme.colorScheme.error.copy(0.12f)
    "in_progress", "partial" -> Color(0xFFFFB300).copy(alpha = 0.14f)
    "completed" -> MaterialTheme.colorScheme.primary.copy(0.12f)
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun statusBadgeFg(status: String): Color = when (status) {
    "missed", "expired", "cancelled" -> MaterialTheme.colorScheme.error
    "in_progress", "partial" -> Color(0xFFB26A00)
    "completed" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
