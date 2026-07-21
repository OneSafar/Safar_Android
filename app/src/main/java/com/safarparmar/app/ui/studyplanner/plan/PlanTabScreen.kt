package com.safarparmar.app.ui.studyplanner.plan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.effectiveSize
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.studyplanner.StudyPlannerOnboardingSteps
import com.safarparmar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics
import com.safarparmar.app.ui.studyplanner.logic.isUnscheduled
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.logic.rollup
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarMonth
import com.safarparmar.app.ui.studyplanner.components.flatCard

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanTabScreen(
    plan: StudyPlan,
    actions: PlannerActions,
    activeTab: StudyPlannerTab,
    onNavigate: (String) -> Unit,
    onboardingCompletedSteps: Set<String> = emptySet(),
    preferredStudyStrategy: String = "interleaved",
    pendingManualSubjectOrder: Boolean = false,
    pendingOpenUnscheduledTopics: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val today = remember { todayKey() }
    val refs = remember(plan.subjects) { plan.flattenTopics() }
    
    val todayTopics = remember(refs, today) {
        refs.filter { ref ->
            ref.topic.plannedDate?.take(10) == today ||
                (ref.topic.status == TopicStatus.REVISION_NEEDED &&
                    today in ref.topic.revisionReminderDates.map { it.take(10) })
        }
    }
    val todayDoneCount = remember(todayTopics) {
        todayTopics.count { it.topic.status == TopicStatus.DONE }
    }
    val overdueTopics = remember(refs, today) {
        refs.filter { (it.topic.plannedDate?.take(10) ?: "9999") < today && it.topic.status != TopicStatus.DONE }
    }
    val upcomingTopics = remember(refs, today) {
        refs
            .filter { (it.topic.plannedDate?.take(10) ?: "") > today && it.topic.status != TopicStatus.DONE }
            .sortedWith(
                compareBy<TopicRef> { it.topic.plannedDate?.take(10).orEmpty() }
                    .thenBy { it.topic.name.lowercase() },
            )
    }
    val completedTopics = remember(refs) {
        refs.filter { it.topic.status == TopicStatus.DONE }
    }
    val hasTopics = refs.isNotEmpty()
    
    val progress = remember(plan.id, plan.subjects, plan.dailyTodos, plan.dailyTodoLogs) { plan.rollup() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            StudyPlannerExportUtils.generateStudyPlanPdf(plan, outputStream)
                        }
                    } catch (e: Exception) {
                        actions.setError("PDF export failed: ${e.localizedMessage}")
                    }
                }
            }
        },
    )

    var showSettings by remember(plan.id) { mutableStateOf(false) }
    var showReschedule by remember(plan.id) { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    var completionPromptTopic by remember { mutableStateOf<TopicRef?>(null) }

    // Editable Today's Study Plan state
    var replaceSheetTopic by remember { mutableStateOf<TopicRef?>(null) }
    var showPullTopicSheet by remember { mutableStateOf(false) }
    var dailyTodoExpanded by rememberSaveable { mutableStateOf(false) }
    // Boolean = lockExisting; null means sheet is closed
    var pendingDistributeAction by remember { mutableStateOf<Boolean?>(null) }
    var removeFromTodayConfirmTopic by remember { mutableStateOf<TopicRef?>(null) }
    var showAddCustomTopic by remember { mutableStateOf(false) }
    var editTopicRef by remember { mutableStateOf<TopicRef?>(null) }
    var revisionTopicRef by remember { mutableStateOf<TopicRef?>(null) }
    var showUnscheduledTopicsScreen by remember { mutableStateOf(false) }
    var showMissedTopicsScreen by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pendingOpenUnscheduledTopics) {
        if (pendingOpenUnscheduledTopics) {
            showUnscheduledTopicsScreen = true
            actions.clearPendingOpenUnscheduledTopics()
        }
    }
    var showManualOrderSheet by remember(plan.id) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pendingManualSubjectOrder, plan.subjects) {
        if (pendingManualSubjectOrder && plan.subjects.isNotEmpty()) {
            showManualOrderSheet = true
        }
    }
    val unscheduledTopics = remember(refs) {
        refs.filter { it.topic.isUnscheduled() }
    }
    fun exportPlan() {
        // Some devices (customised OEM ROMs, or ones where the system document picker
        // is disabled/removed) have no activity that handles ACTION_CREATE_DOCUMENT, so
        // launch() throws ActivityNotFoundException. Surface a message instead of crashing.
        try {
            exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf")
        } catch (e: android.content.ActivityNotFoundException) {
            actions.setError("Couldn't open the file picker to export. Your device may not support saving files this way.")
        }
    }

    if (showSettings) {
        PlanSettingsSheet(
            plan = plan,
            actions = actions,
            onExport = ::exportPlan,
            onReset = { resetConfirm = true },
            onDismiss = { showSettings = false },
            onExamDateChanged = { showReschedule = true },
        )
    }

    if (showReschedule) {
        RescheduleFlowSheet(
            subjects = plan.subjects,
            onRebuildNow = { strategy, overloadMode, priority ->
                actions.rescheduleAfterExamDateChange(strategy, overloadMode, priority)
                showReschedule = false
            },
            onReorderFirst = { strategy, overloadMode, priority ->
                actions.armRebuild(strategy, overloadMode, priority)
                actions.setSection(PlannerSection.SYLLABUS)
                showReschedule = false
            },
            onDismiss = { showReschedule = false },
        )
    }

    if (resetConfirm) {
        PlanConfirmDialog(
            title = "Reset plan?",
            body = "All topics will move back to Todo and dates will be removed.",
            onDismiss = { resetConfirm = false },
            onConfirm = {
                actions.resetPlan()
                resetConfirm = false
                showSettings = false
            },
        )
    }

    completionPromptTopic?.let { ref ->
        AlertDialog(
            onDismissRequest = { completionPromptTopic = null },
            title = { Text("Mark this topic as?") },
            text = {
                Text(
                    text = ref.topic.name,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            // Open the revision scheduler instead of directly setting status
                            revisionTopicRef = ref
                            completionPromptTopic = null
                        },
                    ) {
                        Text("To Revise")
                    }
                    Button(
                        onClick = {
                            actions.updateTopic(ref.topic.id, status = TopicStatus.DONE)
                            completionPromptTopic = null
                        },
                    ) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { completionPromptTopic = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    revisionTopicRef?.let { ref ->
        RevisionScheduleSheet(
            topicName = ref.topic.name,
            examDate = plan.examDate,
            onRevisionScheduled = { dates, scheduleType ->
                actions.markForRevision(ref.topic.id, dates, scheduleType)
            },
            onDismiss = { revisionTopicRef = null },
            isAlreadyRevisionNeeded = ref.topic.status == TopicStatus.REVISION_NEEDED,
            onCancelRevision = { actions.cancelRevision(ref.topic.id) }
        )
    }

    fun handleTopicDoneCheck(ref: TopicRef, checked: Boolean) {
        if (checked && ref.topic.status != TopicStatus.DONE) {
            if (ref.topic.status == TopicStatus.REVISION_NEEDED) {
                // Completing one revision must advance its cadence rather than mark
                // the whole topic done and silently remove revisions 2 and 3.
                actions.completeRevisionForDate(ref.topic.id, today)
            } else {
                completionPromptTopic = ref
            }
        } else if (!checked && ref.topic.status == TopicStatus.DONE) {
            actions.updateTopic(ref.topic.id, status = TopicStatus.TODO)
        }
    }


    // ── Build Planner Strategy Sheet (Replacing Build Schedule Mode Sheet) ──
    pendingDistributeAction?.let { lockExisting ->
        // lockExisting=false is set only by the "Rebuild Plan" entry point (only shown
        // once a schedule already exists) — lockExisting=true is the first-ever build.
        val isRebuild = !lockExisting
        val currentStyleLabel = if (preferredStudyStrategy == "sequential") "Deep Focus mode" else "Balanced mode"
        ModalBottomSheet(
            onDismissRequest = { pendingDistributeAction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isRebuild) "Rebuild your plan" else "Build planner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isRebuild) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            text = "Currently: $currentStyleLabel",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "This reschedules your remaining, unfinished topics — today's list and anything already done stays untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                } else {
                    Text(
                        text = "Choose how SAFAR should place your topics on study days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                StudyStyleOption(
                    title = "Balanced mode",
                    body = "Mix subjects daily.",
                    recommended = preferredStudyStrategy == "interleaved",
                    onClick = {
                        actions.autoDistribute(lockExisting = lockExisting, strategy = "interleaved")
                        pendingDistributeAction = null
                    },
                )
                StudyStyleOption(
                    title = "Deep Focus mode",
                    body = "Finish topics in the same order as your syllabus.",
                    recommended = preferredStudyStrategy == "sequential",
                    onClick = {
                        actions.autoDistribute(lockExisting = lockExisting, strategy = "sequential")
                        pendingDistributeAction = null
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ── Manual mode: set subject priority order right after creation ──
    if (showManualOrderSheet) {
        ManualSubjectOrderSheet(
            subjects = plan.subjects,
            onConfirm = { orderedIds ->
                actions.reorderSyllabus(subjectIds = orderedIds)
                actions.clearPendingManualSubjectOrder()
                showManualOrderSheet = false
            },
            onSkip = {
                actions.clearPendingManualSubjectOrder()
                showManualOrderSheet = false
            },
        )
    }

    // ── Replace Topic Sheet ────────────────────────────────────────
    if (replaceSheetTopic != null || showPullTopicSheet) {
        ReplaceTopicSheet(
            currentRef = replaceSheetTopic,
            allRefs = refs,
            today = today,
            onSwap = { currentId, replacementId ->
                actions.swapTopicDates(currentId, replacementId)
            },
            onReplace = { currentId, replacementId, todayDate ->
                actions.replaceTopicToday(currentId, replacementId, todayDate)
            },
            onPull = { topicId ->
                actions.updateTopic(topicId = topicId, plannedDate = today, pinned = true)
            },
            onDismiss = {
                replaceSheetTopic = null
                showPullTopicSheet = false
            },
        )
    }

    // ── Remove from Today confirmation ─────────────────────────────
    removeFromTodayConfirmTopic?.let { ref ->
        PlanConfirmDialog(
            title = "Remove from today?",
            body = "\"${ref.topic.name}\" will be marked as Not Assigned. You can re-add it later.",
            onDismiss = { removeFromTodayConfirmTopic = null },
            onConfirm = {
                actions.clearTopicDates(listOf(ref.topic.id))
                removeFromTodayConfirmTopic = null
            },
        )
    }

    // ── Add a brand-new custom topic to today ──────────────────────
    if (showAddCustomTopic) {
        AddCustomTopicDialog(
            onDismiss = { showAddCustomTopic = false },
            onConfirm = { name ->
                actions.addCustomTopicToToday(name)
                showAddCustomTopic = false
            },
        )
    }

    // ── Edit topic / chapter / subject names ───────────────────────
    editTopicRef?.let { ref ->
        EditTopicDialog(
            ref = ref,
            onDismiss = { editTopicRef = null },
            onSave = { topicName, chapterName, subjectName, size ->
                val nameToSend = topicName.takeIf { it != ref.topic.name && it.isNotBlank() }
                if (nameToSend != null || size != null) {
                    actions.updateTopic(ref.topic.id, name = nameToSend, size = size)
                }
                if (chapterName != ref.chapter.name && chapterName.isNotBlank()) {
                    actions.renameChapter(ref.subject.id, ref.chapter.id, chapterName)
                }
                if (subjectName != ref.subject.name && subjectName.isNotBlank()) {
                    actions.renameSubject(ref.subject.id, subjectName)
                }
                editTopicRef = null
            },
        )
    }

    var showCreatePlanSheet by remember { mutableStateOf(false) }

    if (showCreatePlanSheet) {
        CreatePlanPromptSheet(
            onGoToSyllabus = {
                showCreatePlanSheet = false
                onNavigate(Routes.ROUTE_SYLLABUS_SUBJECTS.replace("{planId}", plan.id))
            },
            onDismiss = { showCreatePlanSheet = false },
        )
    }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark
    val remainingToday = todayTopics.filter { it.topic.status != TopicStatus.DONE }
    val todayCompleted = todayTopics.isNotEmpty() && remainingToday.isEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasTopics) {
        EmptyPlanTabState(onCreateClick = { showCreatePlanSheet = true })
      } else {
        // ── One flat surface ──────────────────────────────────────────────
        // Header, ring, stats, Daily To-Do, tabs and agenda used to be four
        // separate bordered cards. They are now one continuous page divided
        // only by hairlines, so no vertical gap between items either.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "status", contentType = "status") {
                PlanHomeHeader(
                    planTitle = plan.title,
                    onSettingsClick = { showSettings = true },
                )
                Spacer(Modifier.height(18.dp))
                PlanHairline()
                PlanHomeHero(plan = plan, progress = progress)
                PlanHairline()
                PlanHomeStatStrip(
                    todayCount = todayTopics.size,
                    overdueCount = overdueTopics.size,
                    upcomingCount = upcomingTopics.size,
                    completedCount = completedTopics.size,
                    onTodayClick = { actions.setPlanTab(StudyPlannerTab.TODAY) },
                    // Overdue opens the missed-topics list in place; Upcoming is
                    // date-shaped so it hands off to the Calendar month grid.
                    onOverdueClick = { showMissedTopicsScreen = true },
                    onUpcomingClick = { actions.setSection(PlannerSection.CALENDAR) },
                    onDoneClick = { actions.setPlanTab(StudyPlannerTab.COMPLETED) },
                )
                PlanHairline()
                if (unscheduledTopics.isNotEmpty()) {
                    UnscheduledWarningBanner(
                        count = unscheduledTopics.size,
                        onClick = { showUnscheduledTopicsScreen = true }
                    )
                }
            }

            item(key = "daily_todo_collapsible", contentType = "daily_todo_collapsible") {
                val todos = plan.dailyTodos.orEmpty()
                val logs = plan.dailyTodoLogs?.get(today).orEmpty()
                PlanHomeDailyTodoRow(
                    doneCount = todos.count { it.id in logs },
                    totalCount = todos.size,
                    expanded = dailyTodoExpanded,
                    onToggleExpanded = { dailyTodoExpanded = !dailyTodoExpanded },
                ) {
                    DailyTodoSection(plan = plan, actions = actions, todayStr = today)
                }
                PlanHairline()
                PlanHomeTabs(
                    activeTab = activeTab,
                    onTabSelected = { actions.setPlanTab(it) },
                    modifier = Modifier.padding(top = 22.dp),
                )
            }

            // Tab Content Items
            when (activeTab) {
                StudyPlannerTab.TODAY -> {
                    if (todayTopics.isEmpty()) {
                        item(key = "today_empty") {
                            PlanHomeEmptyNote(
                                "Nothing planned for today. Pull a topic in, or add a one-off.",
                            )
                            PlanHomeAddActions(
                                onAddFromSyllabus = { showPullTopicSheet = true },
                                onAddCustom = { showAddCustomTopic = true },
                                modifier = Modifier.padding(top = 0.dp),
                            )
                        }
                    } else {
                        if (todayCompleted) {
                            item(key = "today_conquered") {
                                val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
                                val conqueredShape = RoundedCornerShape(20.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = if (isDark) {
                                                    listOf(Color(0xFF102A20), Color(0xFF153327))
                                                } else {
                                                    listOf(Color(0xFFDCFCE7), Color(0xFFA7F3D0))
                                                }
                                            ),
                                            shape = conqueredShape
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            brush = if (isDark) {
                                                Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.02f)))
                                            } else {
                                                Brush.verticalGradient(colors = listOf(Color(0xFFDCFCE7), Color(0xFFA7F3D0)))
                                            },
                                            shape = conqueredShape
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(
                                                    color = if (isDark) Color(0xFF2D3732) else Color(0xFF4ADE80),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎉", fontSize = 24.sp)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Today's Queue Conquered!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDark) Color.White else Color(0xFF064E3B),
                                            )
                                            Text(
                                                text = "Nice. Your completed tasks stay below so the checkmarks feel visible and satisfying.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF047857),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "today_list_header") {
                            PlanHomeSectionHeader(
                                title = "Today's agenda",
                                trailing = "${todayTopics.size} planned",
                                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
                            )
                        }
                        items(
                            items = todayTopics.take(10),
                            key = { ref -> "today_${ref.topic.id}" },
                            contentType = { "todayTopic" }
                        ) { ref ->
                            PlanHairline(alpha = 0.6f)
                            PlanHomeTaskRow(
                                ref = ref,
                                onDoneChange = { done -> handleTopicDoneCheck(ref, done) },
                                onReplace = { replaceSheetTopic = ref },
                                onRemoveFromToday = { removeFromTodayConfirmTopic = ref },
                                onEdit = { editTopicRef = ref },
                                onFocus = { onNavigate(Routes.ekagraForTopic(ref.topic.id, ref.topic.name, plan.id)) },
                                onSetProgress = { percent -> actions.setTopicProgress(ref.topic.id, percent) },
                            )
                        }

                        item(key = "pull_extra_topic") {
                            PlanHomeAddActions(
                                onAddFromSyllabus = { showPullTopicSheet = true },
                                onAddCustom = { showAddCustomTopic = true },
                            )
                        }

                        if (todayCompleted) {
                            val bonusTopics = (overdueTopics + upcomingTopics).take(5)
                            if (bonusTopics.isNotEmpty()) {
                                item(key = "bonus_header") {
                                    PlanHomeSectionHeader(
                                        title = "Bonus, to get ahead",
                                        trailing = "${bonusTopics.size} suggested",
                                        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
                                    )
                                }
                                items(
                                    items = bonusTopics,
                                    key = { ref -> "bonus_${ref.topic.id}" },
                                    contentType = { "bonusTopic" }
                                ) { ref ->
                                    PlanHairline(alpha = 0.6f)
                                    PlanHomeTaskRow(
                                        ref = ref,
                                        onDoneChange = { done -> handleTopicDoneCheck(ref, done) },
                                        onSetProgress = { percent -> actions.setTopicProgress(ref.topic.id, percent) },
                                        onEdit = { editTopicRef = ref },
                                        onFocus = { onNavigate(Routes.ekagraForTopic(ref.topic.id, ref.topic.name, plan.id)) },
                                        // No Replace / Remove-from-today here: a bonus
                                        // topic isn't on today's list, so neither applies.
                                    )
                                }
                            }
                        }
                    }
                }

                StudyPlannerTab.DAILY_TODO -> {
                    // Daily To-Do now lives in a bottom sheet (opened from the chip). If an old
                    // persisted selection lands here, recover to Today instead of a blank view.
                    item(key = "daily_todo_recover") {
                        androidx.compose.runtime.LaunchedEffect(Unit) { actions.setPlanTab(StudyPlannerTab.TODAY) }
                    }
                }

                // Overdue now lives in Calendar's Missed Topics list, Upcoming in
                // the Calendar month grid, and Revision in its own
                // PlannerSection.REVISION screen — none are Home sub-tabs anymore.
                // Handled together purely to keep this `when` exhaustive; all three
                // are unreachable via the (Today/Done) quick-links.
                StudyPlannerTab.OVERDUE,
                StudyPlannerTab.UPCOMING,
                StudyPlannerTab.REVISION -> Unit

                StudyPlannerTab.COMPLETED -> {
                    if (completedTopics.isEmpty()) {
                        item(key = "completed_empty") {
                            PlanHomeEmptyNote(
                                "Nothing finished yet. Your completed topics collect here.",
                            )
                        }
                    } else {
                        item(key = "completed_list_header") {
                            PlanHomeSectionHeader(
                                title = "Completed",
                                trailing = "${completedTopics.size} total",
                                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
                            )
                        }
                        items(
                            items = completedTopics.take(15),
                            key = { ref -> "completed_${ref.topic.id}" },
                            contentType = { "completedTopic" }
                        ) { ref ->
                            PlanHairline(alpha = 0.6f)
                            PlanHomeTaskRow(
                                ref = ref,
                                onDoneChange = { done -> handleTopicDoneCheck(ref, done) },
                                // Completed topics only get Edit — the row itself
                                // already hides Focus and Remove-from-today once a
                                // topic is done, and Replace is meaningless here.
                                onEdit = { editTopicRef = ref },
                            )
                        }
                    }
                }
            }
        }
      }

      if (hasTopics && activeTab == StudyPlannerTab.TODAY && !todayCompleted && todayTopics.isNotEmpty()) {
          DoneForTheDayBar(
              onClick = {
                  val incompleteTopics = todayTopics.filter { it.topic.status != TopicStatus.DONE }
                  val topicIds = incompleteTopics.map { it.topic.id }
                  if (topicIds.isNotEmpty()) {
                      actions.finishDay(topicIds)
                  }
              },
              modifier = Modifier.align(Alignment.BottomCenter)
          )
      }
        if (showUnscheduledTopicsScreen) {
            UnscheduledTopicsScreen(
                plan = plan,
                unscheduledTopics = unscheduledTopics,
                actions = actions,
                onDismiss = { showUnscheduledTopicsScreen = false }
            )
        }
        if (showMissedTopicsScreen) {
            MissedTopicsScreen(
                plan = plan,
                missedTopics = overdueTopics,
                actions = actions,
                onDismiss = { showMissedTopicsScreen = false }
            )
        }
    }
}

@Composable
private fun EmptyPlanTabState(onCreateClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📚", fontSize = 48.sp)
            Text(
                text = "Your plan is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Add subjects and topics first. Then you can build your study schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = ButtonDefaults.shape,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create your plan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanPromptSheet(
    onGoToSyllabus: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Let's set up your plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "First, add your subjects and topics in the Syllabus tab. Once that's done, come back here to build your study schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onGoToSyllabus,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = ButtonDefaults.shape,
            ) {
                Text("Go to Syllabus", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun AddCustomTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a topic to today") },
        text = {
            Column {
                Text(
                    text = "This adds a one-off topic to Today's Study Plan only. It won't change your daily goal or future days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().length >= 2,
            ) { Text("Add to today") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditTopicDialog(
    ref: TopicRef,
    onDismiss: () -> Unit,
    onSave: (topicName: String, chapterName: String, subjectName: String, size: String?) -> Unit,
) {
    var topicName by remember(ref.topic.id) { mutableStateOf(ref.topic.name) }
    var chapterName by remember(ref.topic.id) { mutableStateOf(ref.chapter.name) }
    var subjectName by remember(ref.topic.id) { mutableStateOf(ref.subject.name) }
    var selectedSize by remember(ref.topic.id) {
        mutableStateOf(ref.topic.effectiveSize(ref.chapter))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Topic size — big topics count as more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopicSize.entries.forEach { option ->
                        FilterChip(
                            selected = option == selectedSize,
                            onClick = { selectedSize = option },
                            label = { Text("${option.shortLabel} · ${option.label}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Chapter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Only send a size when it actually changed from the current
                    // effective size, so an untouched dialog stays a no-op patch.
                    val sizeToSend = selectedSize.wireValue
                        .takeIf { selectedSize != ref.topic.effectiveSize(ref.chapter) }
                    onSave(topicName.trim(), chapterName.trim(), subjectName.trim(), sizeToSend)
                },
                enabled = topicName.trim().length >= 2,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun UnscheduledWarningBanner(
    count: Int,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.errorContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = scheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count Not Assigned Topics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onErrorContainer
                )
                Text(
                    text = "Topics are in your syllabus but not assigned. Tap to assign them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(contentColor = scheme.error)
            ) {
                Text("Schedule", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnscheduledTopicsScreen(
    plan: StudyPlan,
    unscheduledTopics: List<TopicRef>,
    actions: PlannerActions,
    onDismiss: () -> Unit,
) {
    TopicSchedulingScreen(plan, unscheduledTopics, actions, onDismiss, "Unscheduled Topics", "Search unscheduled topics...")
}

@Composable
internal fun MissedTopicsScreen(
    plan: StudyPlan,
    missedTopics: List<TopicRef>,
    actions: PlannerActions,
    onDismiss: () -> Unit,
) {
    TopicSchedulingScreen(plan, missedTopics, actions, onDismiss, "Missed Topics", "Search missed topics...")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicSchedulingScreen(
    plan: StudyPlan,
    unscheduledTopics: List<TopicRef>,
    actions: PlannerActions,
    onDismiss: () -> Unit,
    screenTitle: String,
    searchPlaceholder: String,
) {
    val scheme = MaterialTheme.colorScheme
    var selectedTopicForDatePicker by remember { mutableStateOf<TopicRef?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTopics = remember(unscheduledTopics, searchQuery) {
        if (searchQuery.isBlank()) {
            unscheduledTopics
        } else {
            val q = searchQuery.lowercase()
            unscheduledTopics.filter { it.topic.name.lowercase().contains(q) }
        }
    }

    if (selectedTopicForDatePicker != null) {
        val today = LocalDate.now(ZoneOffset.UTC)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val picked = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !picked.isBefore(today)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { selectedTopicForDatePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val ld = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            selectedTopicForDatePicker?.let { ref ->
                                actions.updateTopic(topicId = ref.topic.id, plannedDate = ld.toString(), pinned = true)
                            }
                        }
                        selectedTopicForDatePicker = null
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTopicForDatePicker = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = scheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "${unscheduledTopics.size} topics need a date",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredTopics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "All topics are scheduled!" else "No matches for '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTopics, key = { it.topic.id }) { ref ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTopicForDatePicker = ref },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = ref.topic.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = "${ref.subject.name} • ${ref.chapter.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Schedule",
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneForTheDayBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0B1221),
                contentColor = if (isDark) Color(0xFF0B1221) else Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "DONE FOR THE DAY",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 16.sp
            )
        }
    }
}
