package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.ui.platform.LocalResources

import androidx.compose.ui.platform.LocalLocale

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.safarparmar.app.ui.nishtha.goals.GoalDialog as AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import com.safarparmar.app.ui.components.popups.SafarDatePickerDialog as DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.ui.components.popups.SafarModalBottomSheet as ModalBottomSheet
import com.safarparmar.app.ui.nishtha.goals.GoalInput as OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Tab
import com.composables.ui.components.TabList
import com.composables.ui.components.Tabs
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.Switch
import com.composables.ui.components.Text
import com.safarparmar.app.ui.nishtha.goals.GoalTextButton as TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
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
import com.safarparmar.app.util.isVisibleInGoals
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
    val tutorialModel: GoalsTutorialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Automatically refresh goals when this screen comes into focus
    LaunchedEffect(Unit) {
        viewModel.loadTab(com.safarparmar.app.ui.nishtha.NishthaTab.GOALS)
    }

    val context = LocalContext.current
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val locale = LocalLocale.current.platformLocale
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.common_today),
        stringResource(R.string.goals_upcoming),
        stringResource(R.string.goals_missed),
        stringResource(R.string.goals_history),
    )
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
                }) { Text(stringResource(R.string.common_ok), color = GoalsFlatColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel), color = GoalsFlatColors.Muted)
                }
            },
        ) { DatePicker(state = datePickerState) }
    }

    // Study time completion dialog
    if (showRepeatPicker) {
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(visible = true, onDismissRequest = { showRepeatPicker = false },
            paneTitle = "Goals", modifier = Modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp)) {
            GoalRepeatContent(uiState.goals, uiState.isSavingGoal, onRepeat = { selectedGoals, source, daily ->
                if (source == 0) viewModel.repeatGoalsOnDate(selectedGoals, LocalDate.parse(IstDateUtils.todayKey()).plusDays(1).toString(), daily)
                else viewModel.repeatGoals(selectedGoals.map { it.id })
                showRepeatPicker = false
            })
        }
    }

    if (showDeletedSheet) {
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(visible = true, onDismissRequest = { showDeletedSheet = false },
            paneTitle = "Goals", modifier = Modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp)) {
            GoalDeletedContent(uiState.recentlyDeletedGoals, uiState.isLoadingDeletedGoals, uiState.isSavingGoal,
                onRestore = { viewModel.restoreGoal(it.id) })
        }
    }

    deleteGoal?.let { goal ->
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
            visible = true,
            onDismissRequest = { deleteGoal = null },
            title = { Text(stringResource(R.string.goals_delete_title)) },
            text = { Text(stringResource(R.string.goals_delete_body, goal.title), textAlign = TextAlign.Center) },
            positiveButton = {
                Button(
                    enabled = !uiState.isSavingGoal,
                    onClick = { viewModel.deleteGoal(goal.id) },
                    style = ButtonStyle.Destructive,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isSavingGoal) stringResource(R.string.common_moving) else stringResource(R.string.common_delete),
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            negativeButton = {
                Button(
                    onClick = { deleteGoal = null },
                    style = ButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.goals_keep_it))
                }
            },
        )
    }

    completeGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { completeGoal = null; studyHours = 0; studyMinutes = 0 },
            containerColor = GoalsFlatColors.Bg,
            title = {
                Text(
                    stringResource(R.string.goals_study_time_question),
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = GoalsFlatColors.Text,
                )
            },
            text = {
                GoalStudyTimeContent(studyHours, studyMinutes, { studyHours = it }, { studyMinutes = it })
            },
            confirmButton = {
                GoalCompletionActions(studyHours, studyMinutes, uiState.isSavingGoal,
                    onComplete = { requestCompletion(goal, it) },
                    onCancel = { completeGoal = null; studyHours = 0; studyMinutes = 0 })
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp),
        )
    }

    overdueCompletion?.let { (goal, totalMins) ->
        val originalDateKey = goal.assignedDateKey().orEmpty()
        val originalDate = runCatching {
            LocalDate.parse(originalDateKey).format(
                DateTimeFormatter.ofPattern("MMMM d", locale),
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
                    stringResource(R.string.goals_earlier_day_title),
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = GoalsFlatColors.Text,
                )
            },
            text = {
                Text(
                    stringResource(R.string.goals_earlier_day_body, originalDate),
                    fontSize = 14.sp,
                    color = GoalsFlatColors.Muted,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                GoalOverdueActions(originalDate, uiState.isSavingGoal,
                    onMoveToToday = {
                viewModel.completeGoal(
                    id = goal.id,
                    studiedMinutes = totalMins,
                    scheduledDate = IstDateUtils.todayKey(),
                )
            }, onKeepDate = {
                viewModel.completeGoal(goal.id, totalMins)
            }, onCancel = {
                overdueCompletion = null
                studyHours = 0
                studyMinutes = 0
            })
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp),
        )
    }

    // Edit sheet
    editGoal?.let { goal ->
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(visible = true, onDismissRequest = { editGoal = null },
            paneTitle = "Goals", modifier = Modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp)) {
            GoalCreationContent(
                title = editTitle, description = editDesc, scheduled = editGoalKind == "scheduled",
                repeatDaily = editRepeatDaily, selectedDate = selectedDate,
                onTitleChange = { editTitle = it }, onDescriptionChange = { editDesc = it },
                onToggleSchedule = {
                    if (editGoalKind == "scheduled") {
                        editGoalKind = "today"
                        selectedDate = LocalDate.now(IstDateUtils.zone)
                        editScheduleChanged = true
                    } else {
                        editGoalKind = "scheduled"
                        selectedDate = LocalDate.now(IstDateUtils.zone).plusDays(1)
                        editScheduleChanged = true
                        showDatePicker = true
                    }
                },
                onPickDate = { editScheduleChanged = true; showDatePicker = true },
                onRepeatChange = { editRepeatDaily = it }, isSaving = uiState.isSavingGoal,
                onCreate = {
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
                }, editing = true,
            )
        }
    }

    LaunchedEffect(uiState.goalError) {
        if (uiState.goalError != null) Toast.makeText(context, uiState.goalError, Toast.LENGTH_SHORT).show()
    }

    if (showAddSheet) {
        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(visible = true, onDismissRequest = { showAddSheet = false },
            paneTitle = "Goals", modifier = Modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp)) {
            GoalCreationContent(
                title = newTitle, description = newDesc, scheduled = newGoalKind == "scheduled",
                repeatDaily = newRepeatDaily, selectedDate = selectedDate,
                onTitleChange = { newTitle = it }, onDescriptionChange = { newDesc = it },
                onToggleSchedule = {
                    if (newGoalKind == "scheduled") {
                        newGoalKind = "today"
                        selectedDate = LocalDate.now(IstDateUtils.zone)
                    } else {
                        newGoalKind = "scheduled"
                        selectedDate = LocalDate.now(IstDateUtils.zone).plusDays(1)
                        showDatePicker = true
                    }
                },
                onPickDate = { showDatePicker = true }, onRepeatChange = { newRepeatDaily = it },
                isSaving = uiState.isSavingGoal, error = uiState.goalError, onCreate = {
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
                },
            )
        }
    }

    if (showStatusSheet) {
        AlertDialog(
            onDismissRequest = { showStatusSheet = false },
            containerColor = GoalsFlatColors.Bg,
            confirmButton = {
                TextButton(onClick = { showStatusSheet = false }) {
                    Text(stringResource(R.string.common_close), color = GoalsFlatColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            title = {
                Text(
                    stringResource(R.string.goals_status),
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
                        message = localizedResources.getString(R.string.goals_moved_to_recently_deleted),
                        actionLabel = localizedResources.getString(R.string.common_undo),
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
    val standardGoals = uiState.goals.filter { it.isVisibleInGoals() }
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
        GoalsOverviewHeader(
            tabs = tabs, selectedTab = selectedTab, totalToday = totalToday, doneToday = doneToday,
            onSelectTab = { selectedTab = it }, onAdd = { showAddSheet = true },
            onStatus = { showStatusSheet = true }, onRepeat = { showRepeatPicker = true },
            onInsights = { onNavigate(Routes.nishthaAnalytics("goals")) },
            onDeleted = { showDeletedSheet = true; viewModel.loadRecentlyDeletedGoals() },
            onLearn = { tutorialModel.open() },
        )

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
                    editGoalKind = goal.editScheduleKind()
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
                onDelete = { goal -> deleteGoal = goal },
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
    )
    }
    GoalsTutorialEntry(
        dataStore = viewModel.dataStore,
        eligible = goalsTutorialEligible(uiState.isLoadingGoals, uiState.isSavingGoal,
            showAddSheet || showStatusSheet || showDeletedSheet || showDatePicker || showRepeatPicker ||
                editGoal != null || completeGoal != null || overdueCompletion != null || deleteGoal != null),
        model = tutorialModel,
    )
}

@Composable
internal fun GoalsUnderlineTabs(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Tabs(
        selectedTab = selectedTab,
        onSelectedTabChange = onTabSelected,
        orderedTabs = tabs.indices.toList(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TabList(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            dividerColor = GoalsFlatColors.Hairline,
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    key = i,
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

/** A tappable, typeable hours/minutes digit box for the "How long did you study?"
 *  dialog — previously a plain [Text] with no input handling at all, so the quick-add
 *  chips were the only way to set a value. */
@Composable
internal fun TimeDigitField(
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
