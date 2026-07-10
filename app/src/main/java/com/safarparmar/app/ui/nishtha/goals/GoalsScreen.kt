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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private val BrandNavy = Color(0xFF0C2B61)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: NishthaViewModel = hiltViewModel()
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
                            TimeDigitField(
                                value = studyHours,
                                onValueChange = { studyHours = it },
                                maxValue = 99,
                            )
                        }
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MINUTES", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            TimeDigitField(
                                value = studyMinutes,
                                onValueChange = { studyMinutes = it },
                                maxValue = 59,
                            )
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
                            Toast.makeText(context, "Goal completed!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ButtonDefaults.shape,
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
                        shape = ButtonDefaults.outlinedShape
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
                    .padding(horizontal = 20.dp).padding(bottom = 40.dp).imePadding(),
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
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
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
                    },
                    enabled = editTitle.isNotBlank() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ButtonDefaults.shape,
                ) { Text("Save Changes") }
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
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp).imePadding(),
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
                    ScheduledDatePickerRow(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true }
                    )
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
                            unitType = "binary",
                            linkedFocusEnabled = false,
                            plannedFocusMinutes = null,
                            targetValue = null,
                            achievedValue = 0,
                            status = "not_started",
                            carryForwardMode = newCarryForward
                        )
                    },
                    enabled = newTitle.isNotBlank() && !uiState.isSavingGoal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ButtonDefaults.shape,
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

    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    // Match the indigo/slate surface used by Today's progress.
    val addButtonBg = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val addButtonFg = if (isDark) Color(0xFF1E293B) else Color.White
    val tabSelectedColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "My Goals",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStatusSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Status", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { onNavigate(Routes.nishthaAnalytics("goals")) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Insights", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = addButtonBg, contentColor = addButtonFg),
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Goal", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = FontWeight.SemiBold)
                }
            }

            val todayKey = IstDateUtils.todayKey()
            val standardGoals = uiState.goals.filter { it.source != "ekagra" }
            val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
            val doneToday = manualCompletedGoals.count { it.completedDateKey() == todayKey }
            val pendingToday = standardGoals.count {
                !it.completed &&
                it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
                !(it.lifecycleStatus == "missed" && it.nextInstanceCreated) &&
                !it.isDormant(todayKey)
            }
            val totalToday = doneToday + pendingToday
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Today's progress", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("$doneToday of $totalToday goals done 🎯", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = tabSelectedColor
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            title,
                            fontSize = 13.sp,
                            color = if (selectedTab == i) tabSelectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
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
                onDelete = { goal -> viewModel.deleteGoal(goal.id); Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show() },
                onRepeat = { goal ->
                    val today = IstDateUtils.todayKey()
                    viewModel.repeatGoal(goal.id, IstDateUtils.dateKeyToUtcIso(today))
                    Toast.makeText(context, "Goal repeated for today!", Toast.LENGTH_SHORT).show()
                },
                onRolloverRetry = { goal -> viewModel.respondToRollover(goal.id, "retry") },
                onRolloverArchive = { goal -> viewModel.respondToRollover(goal.id, "archive") },
            )
            2 -> HistoryTab(uiState.goals)
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
        modifier = Modifier.size(width = 90.dp, height = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
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
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "00",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                textAlign = TextAlign.Center,
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
