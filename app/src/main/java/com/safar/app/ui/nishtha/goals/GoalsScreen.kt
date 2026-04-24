package com.safar.app.ui.nishtha.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.safar.app.ui.nishtha.NishthaEvent
import com.safar.app.ui.nishtha.NishthaViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Goals", "History", "Analytics")
    var showAddSheet by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("medium") }
    var newSubtaskInput by remember { mutableStateOf("") }
    var newSubtasks by remember { mutableStateOf(listOf<String>()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableStateOf(java.time.LocalTime.now().hour) }
    var selectedMinute by remember { mutableStateOf(java.time.LocalTime.now().minute) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Edit state
    var editGoal by remember { mutableStateOf<Goal?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("medium") }

    // Complete / study time dialog
    var completeGoal by remember { mutableStateOf<Goal?>(null) }
    var studyHours by remember { mutableIntStateOf(0) }
    var studyMinutes by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = LocalDate.now()
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
                    val isToday = selectedDate == LocalDate.now()
                    val picked = java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    if (isToday && picked.isBefore(java.time.LocalTime.now())) {
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
                OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text("Priority", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { p ->
                        FilterChip(selected = editPriority == p, onClick = { editPriority = p }, label = { Text(p.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) })
                    }
                }
                Button(
                    onClick = {
                        viewModel.updateGoal(goal.id, editTitle.trim(), editDesc.ifBlank { null }, editPriority)
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
            selectedDate = LocalDate.now()
            java.time.LocalTime.now().also { selectedHour = it.hour; selectedMinute = it.minute }
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
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Subtasks (optional)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newSubtaskInput, onValueChange = { newSubtaskInput = it }, placeholder = { Text("Add subtask...") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                        IconButton(onClick = {
                            if (newSubtaskInput.isNotBlank()) { newSubtasks = newSubtasks + newSubtaskInput.trim(); newSubtaskInput = "" }
                        }) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    newSubtasks.forEachIndexed { i, sub ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(sub, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { newSubtasks = newSubtasks.toMutableList().also { it.removeAt(i) } }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())), fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                        val amPm = if (selectedHour < 12) "AM" else "PM"
                        val h12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                        Text("%d:%02d %s".format(h12, selectedMinute, amPm), fontSize = 13.sp)
                    }
                }
                Text("Priority", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { p ->
                        FilterChip(selected = newPriority == p, onClick = { newPriority = p }, label = { Text(p.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) })
                    }
                }
                if (uiState.goalError != null) Text(uiState.goalError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(
                    onClick = {
                        val scheduledDate = selectedDate.toString()
                        val startedAtIso = "%sT%02d:%02d:00.000Z".format(scheduledDate, (selectedHour - 5 + 24) % 24, selectedMinute)
                        viewModel.onEvent(NishthaEvent.AddGoal(newTitle.trim(), newDesc.ifBlank { null }, newPriority, scheduledDate, startedAtIso, newSubtasks))
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Goals", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text("Stay focused and consistent.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                Text("Add", fontSize = 13.sp, color = Color.White)
            }
        }
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { i, title -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title, fontSize = 13.sp) }) }
        }
        when (selectedTab) {
            0 -> GoalsTab(
                goals = uiState.goals, isLoading = uiState.isLoadingGoals, onAddClick = { showAddSheet = true },
                onComplete = { goal -> completeGoal = goal; studyHours = 0; studyMinutes = 0 },
                onEdit = { goal -> editGoal = goal; editTitle = goal.title; editDesc = goal.description ?: ""; editPriority = goal.priority },
                onDelete = { goal -> viewModel.deleteGoal(goal.id); Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show() },
                onRepeat = { goal ->
                    val tomorrow = LocalDate.now().plusDays(1).toString()
                    viewModel.onEvent(NishthaEvent.AddGoal(goal.title, goal.description, goal.priority, tomorrow, "${tomorrow}T04:00:00.000Z"))
                    Toast.makeText(context, "Goal repeated for tomorrow!", Toast.LENGTH_SHORT).show()
                },
            )
            1 -> HistoryTab(uiState.goals)
            2 -> GoalsAnalyticsTab(uiState.goals)
        }
    }
}

@Composable
private fun GoalsTab(
    goals: List<Goal>, isLoading: Boolean, onAddClick: () -> Unit,
    onComplete: (Goal) -> Unit, onEdit: (Goal) -> Unit, onDelete: (Goal) -> Unit, onRepeat: (Goal) -> Unit,
) {
    val pending   = goals.filter { !it.completed }
    val completed = goals.filter { it.completed }
    if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }; return }
    if (goals.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎯", fontSize = 48.sp)
                Text("No goals yet", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddClick, shape = RoundedCornerShape(12.dp)) { Text("Add Your First Goal") }
            }
        }; return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pending.isNotEmpty()) {
            item { Text("Active", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(pending, key = { it.id }) { GoalItem(it, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        }
        if (completed.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); Text("Completed", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(completed.take(5), key = { it.id }) { GoalItem(it, onComplete = { onComplete(it) }, onEdit = { onEdit(it) }, onDelete = { onDelete(it) }, onRepeat = { onRepeat(it) }) }
        }
    }
}

@Composable
private fun GoalItem(goal: Goal, onComplete: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onRepeat: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
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
                goal.scheduledDate?.let { Text(it.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            PriorityBadge(goal.priority)
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!goal.completed) {
                        DropdownMenuItem(text = { Text("Mark Complete") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showMenu = false; onComplete() })
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onEdit() })
                    }
                    DropdownMenuItem(text = { Text("Repeat") }, leadingIcon = { Icon(Icons.Default.Repeat, null) }, onClick = { showMenu = false; onRepeat() })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
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

@Composable
private fun HistoryTab(goals: List<Goal>) {
    val completed = goals.filter { it.completed }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val completedDates = remember(completed) {
        completed.mapNotNull { goal ->
            (goal.completedAt?.take(10) ?: goal.scheduledDate?.take(10))
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }.toSet()
    }
    val filtered = if (selectedDate != null) {
        completed.filter { goal ->
            val d = goal.completedAt?.take(10) ?: goal.scheduledDate?.take(10)
            d == selectedDate.toString()
        }
    } else completed

    Column(modifier = Modifier.fillMaxSize()) {
        if (completedDates.isNotEmpty()) {
            val today = LocalDate.now()
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Filter by date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.forEach { day ->
                        val hasGoals = day in completedDates
                        val isSelected = selectedDate == day
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(when { isSelected -> MaterialTheme.colorScheme.primary; hasGoals -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f); else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) })
                                .clickable { selectedDate = if (isSelected) null else day }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                day.format(DateTimeFormatter.ofPattern("EEE\ndd", Locale.getDefault())),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasGoals) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (hasGoals && !isSelected) { Spacer(Modifier.height(3.dp)); Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
                        }
                    }
                }
                if (selectedDate != null) {
                    TextButton(onClick = { selectedDate = null }, contentPadding = PaddingValues(0.dp)) {
                        Text("Clear filter", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (selectedDate != null) "No completed goals on this date." else "No completed goals yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { GoalItem(it, onComplete = {}, onEdit = {}, onDelete = {}, onRepeat = {}) }
        }
    }
}

@Composable
private fun GoalsAnalyticsTab(goals: List<Goal>) {
    val total = goals.size; val completed = goals.count { it.completed }; val pending = total - completed
    val rate = if (total > 0) completed * 100 / total else 0
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Analytics", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Total", "$total", Modifier.weight(1f)); StatCard("Done", "$completed", Modifier.weight(1f)); StatCard("Pending", "$pending", Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Completion Rate", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("$rate%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }
                LinearProgressIndicator(progress = { rate / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.15f))
            }
        }
    }
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
