package com.safar.app.ui.studyplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.R
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.PremiumReason
import com.safar.app.domain.model.studyplanner.StudyChapter
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.StudySubject
import com.safar.app.domain.model.studyplanner.StudyTopic
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    currentRoute: String = Routes.STUDY_PLANNER,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    viewModel: StudyPlannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.message) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearTransient() }
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearTransient() }
    }

    if (state.premiumReason != null) {
        PremiumGateSheet(
            reason = state.premiumReason!!,
            onDismiss = viewModel::clearTransient,
            onUpgrade = viewModel::upgradePlan,
        )
    }

    SafarDrawerScaffold(
        title = "Study Planner",
        subtitle = "Ekagra",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarActions = {
            if (state.selectedPlan != null) {
                IconButton(onClick = viewModel::closePlan) {
                    Icon(Icons.Default.Close, contentDescription = "Close planner")
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
    ) { padding ->
        Scaffold(
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (state.selectedPlan == null) FloatingActionButton(onClick = { viewModel.refreshPlans() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            bottomBar = {
                state.selectedPlan?.let {
                    PlannerBottomNav(selected = state.section, onSelect = viewModel::setSection)
                }
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                if (state.selectedPlan == null) {
                    StudyPlansScreen(state = state, viewModel = viewModel)
                } else {
                    PlannerHome(state = state, viewModel = viewModel)
                }
                if (state.loading || state.mutating) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyPlansScreen(state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StudyPlan?>(null) }

    if (showCreate) QuickStartSheet(state, viewModel, onDismiss = { showCreate = false })
    pendingDelete?.let { plan ->
        ConfirmActionDialog(
            title = "Delete plan?",
            body = "This removes ${plan.title} and its syllabus.",
            onDismiss = { pendingDelete = null },
            onConfirm = { viewModel.deletePlan(plan.id); pendingDelete = null },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Study Planner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Plans, syllabus, calendar, settings, and insights in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Button(onClick = { showCreate = true }, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }
        if (state.plans.isEmpty() && !state.loading) {
            item {
                EmptyPlannerCard(
                    title = "No plans yet",
                    body = "Create one from an exam template or start with a custom syllabus.",
                    action = "Create Plan",
                    onAction = { showCreate = true },
                )
            }
        }
        items(state.plans, key = { it.id }) { plan ->
            PlanCard(plan, onOpen = { viewModel.openPlan(plan.id) }, onDelete = { pendingDelete = plan })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickStartSheet(state: StudyPlannerUiState, viewModel: StudyPlannerViewModel, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf("template") }
    var templateId by remember(state.templates) { mutableStateOf(state.templates.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var examType by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableStateOf("3") }
    var pasteSyllabus by remember { mutableStateOf("") }
    val offDays = remember { mutableStateOf(setOf<Int>()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Quick Start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "template", onClick = { mode = "template" }, label = { Text("Exam Template") })
                FilterChip(selected = mode == "custom", onClick = { mode = "custom" }, label = { Text("Custom Plan") })
            }
            if (mode == "template") {
                Text("Choose template", fontWeight = FontWeight.SemiBold)
                state.templates.forEach { template ->
                    PlannerRow(
                        title = template.name,
                        subtitle = "${template.subjectCount ?: 0} subjects • ${template.topicCount ?: 0} topics",
                        icon = Icons.Default.School,
                        selected = templateId == template.id,
                        onClick = {
                            templateId = template.id
                            if (title.isBlank()) title = template.name
                        },
                    )
                }
            } else {
                OutlinedTextField(value = examType, onValueChange = { examType = it }, label = { Text("Exam name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pasteSyllabus, onValueChange = { pasteSyllabus = it }, label = { Text("Optional pasted syllabus") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Text("${parseBulkSyllabus(pasteSyllabus).sumOf { it.second.size }} topics detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Plan title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = examDate, onValueChange = { examDate = it }, label = { Text("Exam date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dailyGoal, onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Daily goal") }, modifier = Modifier.fillMaxWidth())
            OffDayPicker(selected = offDays.value, onToggle = { day ->
                offDays.value = if (day in offDays.value) offDays.value - day else offDays.value + day
            })
            Button(
                onClick = {
                    val goal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3
                    if (mode == "template" && templateId.isNotBlank()) {
                        viewModel.createFromTemplate(templateId, title.ifBlank { "Study Plan" }, examDate.ifBlank { null }, goal, offDays.value.toList())
                    } else {
                        viewModel.createPlan(title.ifBlank { "Study Plan" }, examType.ifBlank { null }, examDate.ifBlank { null }, goal, offDays.value.toList())
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Create Plan") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlannerHome(state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val plan = state.selectedPlan ?: return
    when (state.section) {
        PlannerSection.TODAY -> TodayTab(plan, state, viewModel)
        PlannerSection.SYLLABUS -> SyllabusTab(plan, viewModel)
        PlannerSection.CALENDAR -> CalendarTab(plan, state, viewModel)
        PlannerSection.PLAN -> PlanSettingsTab(plan, viewModel)
        PlannerSection.INSIGHTS -> InsightsTab(plan, state)
    }
}

@Composable
private fun TodayTab(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val today = todayKey()
    val refs = plan.flattenTopics()
    val todayTopics = refs.filter { it.topic.plannedDate?.take(10) == today }
    val overdue = refs.filter { (it.topic.plannedDate?.take(10) ?: "9999") < today && it.topic.status != TopicStatus.DONE }
    val upcoming = refs.filter { (it.topic.plannedDate?.take(10) ?: "") > today }.take(6)
    val progress = plan.rollup()
    var selectedTopic by remember { mutableStateOf<TopicRef?>(null) }

    selectedTopic?.let { TopicDetailSheet(it, viewModel, onDismiss = { selectedTopic = null }) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PlannerHeader(plan)
            SetupGuideCard(plan, viewModel)
            ProgressCard(progress.completionPercent, "${progress.doneTopics} / ${progress.totalTopics} topics done", "Required pace: ${plan.dailyGoal ?: 3} topics/day")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PlannerActionButton("Add Topics", Icons.Default.PlaylistAdd, Modifier.weight(1f)) { viewModel.setSection(PlannerSection.SYLLABUS) }
                PlannerActionButton("Build Schedule", Icons.Default.CalendarMonth, Modifier.weight(1f)) { viewModel.autoDistribute(includeRevision = false, lockExisting = true) }
            }
        }
        item { SectionTitle("Today", "${todayTopics.size} planned") }
        items(todayTopics, key = { it.topic.id }) { ref -> TopicRow(ref, onClick = { selectedTopic = ref }, onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) }) }
        if (overdue.isNotEmpty()) {
            item { SectionTitle("Overdue", "${overdue.size} need attention") }
            items(overdue.take(8), key = { it.topic.id }) { ref -> TopicRow(ref, onClick = { selectedTopic = ref }, onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) }) }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Upcoming", "Next planned topics") }
            items(upcoming, key = { it.topic.id }) { ref -> TopicRow(ref, onClick = { selectedTopic = ref }, onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) }) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SyllabusTab(plan: StudyPlan, viewModel: StudyPlannerViewModel) {
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<TopicStatus?>(null) }
    var selectedTopic by remember { mutableStateOf<TopicRef?>(null) }
    var addSubject by remember { mutableStateOf(false) }
    var addChapterFor by remember { mutableStateOf<StudySubject?>(null) }
    var addTopicFor by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }
    var bulkFor by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }
    var deleteSubject by remember { mutableStateOf<StudySubject?>(null) }
    var deleteChapter by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }

    selectedTopic?.let { TopicDetailSheet(it, viewModel, onDismiss = { selectedTopic = null }) }
    if (addSubject) TextInputDialog("Add subject", "Subject name", onDismiss = { addSubject = false }) { viewModel.addSubject(it); addSubject = false }
    addChapterFor?.let { subject -> TextInputDialog("Add chapter", "Chapter name", onDismiss = { addChapterFor = null }) { viewModel.addChapter(subject.id, it); addChapterFor = null } }
    addTopicFor?.let { pair -> TextInputDialog("Add topic", "Topic name", onDismiss = { addTopicFor = null }) { viewModel.addTopic(pair.first.id, pair.second.id, it); addTopicFor = null } }
    bulkFor?.let { pair -> BulkAddSheet(pair, viewModel, onDismiss = { bulkFor = null }) }
    deleteSubject?.let { subject -> ConfirmActionDialog("Delete subject?", "This removes ${subject.name}.", { deleteSubject = null }) { viewModel.deleteSubject(subject.id); deleteSubject = null } }
    deleteChapter?.let { pair -> ConfirmActionDialog("Delete chapter?", "This removes ${pair.second.name}.", { deleteChapter = null }) { viewModel.deleteChapter(pair.first.id, pair.second.id); deleteChapter = null } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search syllabus") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = status == null, onClick = { status = null }, label = { Text("All") })
                TopicStatus.entries.forEach { st -> FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) }) }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { addSubject = true }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Subject")
            }
        }
        items(plan.subjects, key = { it.id }) { subject ->
            SubjectBlock(
                subject = subject,
                query = query,
                status = status,
                onAddChapter = { addChapterFor = subject },
                onDeleteSubject = { deleteSubject = subject },
                onAddTopic = { chapter -> addTopicFor = subject to chapter },
                onBulkAdd = { chapter -> bulkFor = subject to chapter },
                onDeleteChapter = { chapter -> deleteChapter = subject to chapter },
                onTopic = { ref -> selectedTopic = ref },
            )
        }
    }
}

@Composable
private fun CalendarTab(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    var selectedDate by remember { mutableStateOf(todayKey()) }
    val dates = (-3..10).map { java.time.LocalDate.now().plusDays(it.toLong()).toString() }
    val topics = state.calendar[selectedDate].orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(readableDate(selectedDate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dates.take(7).forEach { date ->
                    val count = state.calendar[date]?.size ?: 0
                    DayChip(date.takeLast(2), count, selectedDate == date) { selectedDate = date }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                OutlinedButton(onClick = { viewModel.setSection(PlannerSection.SYLLABUS) }, shape = RoundedCornerShape(12.dp)) { Text("Open Syllabus") }
                OutlinedButton(onClick = { viewModel.clearFutureDates() }, shape = RoundedCornerShape(12.dp)) { Text("Clear Future") }
            }
        }
        if (topics.isEmpty()) {
            item { EmptyPlannerCard("No topics on this day", "Use Plan settings to build a calendar or assign dates from topic details.", "Build Schedule") { viewModel.autoDistribute(false, true) } }
        }
        items(topics, key = { it.topicId }) { item ->
            PlannerSurface {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.topicName, fontWeight = FontWeight.Bold)
                        Text("${item.subjectName} • ${item.chapterName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { viewModel.updateTopic(item.topicId, status = TopicStatus.DONE) }) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun PlanSettingsTab(plan: StudyPlan, viewModel: StudyPlannerViewModel) {
    var title by remember(plan.id) { mutableStateOf(plan.title) }
    var examType by remember(plan.id) { mutableStateOf(plan.examType.orEmpty()) }
    var examDate by remember(plan.id) { mutableStateOf(plan.examDate?.take(10).orEmpty()) }
    var dailyGoal by remember(plan.id) { mutableStateOf((plan.dailyGoal ?: 3).toString()) }
    var includeRevision by remember { mutableStateOf(false) }
    var lockExisting by remember { mutableStateOf(true) }
    var resetConfirm by remember { mutableStateOf(false) }
    if (resetConfirm) ConfirmActionDialog("Reset entire plan?", "All topics return to Todo and planned dates are cleared.", { resetConfirm = false }) { viewModel.resetPlan(); resetConfirm = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SettingsCard("Basics") {
                OutlinedTextField(title, { title = it }, label = { Text("Plan title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(examType, { examType = it }, label = { Text("Exam type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(examDate, { examDate = it }, label = { Text("Exam date") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.updatePlan(UpdatePlanRequest(title = title, examType = examType, examDate = examDate.ifBlank { null })) }, shape = RoundedCornerShape(14.dp)) { Text("Save Basics") }
            }
            SettingsCard("Study Capacity") {
                OutlinedTextField(dailyGoal, { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Daily goal") }, modifier = Modifier.fillMaxWidth())
                OffDayPicker(plan.offDays.toSet(), onToggle = { })
                Button(onClick = { viewModel.updatePlan(UpdatePlanRequest(dailyGoal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3)) }, shape = RoundedCornerShape(14.dp)) { Text("Save Capacity") }
            }
            SettingsCard("Advanced Scheduling") {
                ToggleRow("Include revision topics", includeRevision) { includeRevision = it }
                ToggleRow("Keep already planned dates", lockExisting) { lockExisting = it }
                Button(onClick = { viewModel.autoDistribute(includeRevision, lockExisting) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Create Planner Calendar") }
                OutlinedButton(onClick = { viewModel.autoDistribute(includeRevision, false) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Reschedule") }
            }
            SettingsCard("Danger Zone") {
                OutlinedButton(onClick = viewModel::clearFutureDates, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Clear Future Dates") }
                Button(onClick = { resetConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(14.dp)) { Text("Reset Entire Plan") }
            }
        }
    }
}

@Composable
private fun InsightsTab(plan: StudyPlan, state: StudyPlannerUiState) {
    val progress = state.analytics?.progress ?: plan.rollup()
    val days = daysUntil(plan.examDate)
    val remaining = progress.totalTopics - progress.doneTopics
    val pace = if (days != null && days > 0) "%.1f".format(remaining.toFloat() / days) else "-"
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PlannerHeader(plan)
            ProgressCard(progress.completionPercent, "$remaining topics remaining", "Required pace: $pace topics/day")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Study Days", days?.coerceAtLeast(0)?.toString() ?: "-", Modifier.weight(1f))
                MetricCard("Revision", progress.revisionTopics.toString(), Modifier.weight(1f))
            }
            SectionTitle("Coverage", "Subject progress")
        }
        items(plan.subjects, key = { it.id }) { subject ->
            PlannerSurface {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text(subject.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${subject.percentDone()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { subject.percentDone() / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)))
                }
            }
        }
        item {
            SettingsCard("Recommendations") {
                Text(if (remaining == 0) "Plan complete. Keep revision active." else "Focus on overdue topics first, then rebuild the calendar if your pace changes.")
                Text("Use Insights after every schedule change to catch overload days and lagging subjects.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailSheet(ref: TopicRef, viewModel: StudyPlannerViewModel, onDismiss: () -> Unit) {
    var name by remember(ref.topic.id) { mutableStateOf(ref.topic.name) }
    var notes by remember(ref.topic.id) { mutableStateOf(ref.topic.notes.orEmpty()) }
    var date by remember(ref.topic.id) { mutableStateOf(ref.topic.plannedDate?.take(10).orEmpty()) }
    var status by remember(ref.topic.id) { mutableStateOf(ref.topic.status) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ref.chapter.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedTextField(name, { name = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopicStatus.entries.forEach { st -> FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) }) }
            }
            OutlinedTextField(date, { date = it }, label = { Text("Planned date") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.deleteTopic(ref.topic.id); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Delete") }
                Button(onClick = { viewModel.updateTopic(ref.topic.id, status, name, date.ifBlank { "" }, notes); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkAddSheet(target: Pair<StudySubject, StudyChapter>, viewModel: StudyPlannerViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val count = parseBulkSyllabus(text).flatMap { it.second }.size
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bulk Add", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${target.first.name} • ${target.second.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Paste Text") }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, Modifier.size(16.dp)) })
                AssistChip(onClick = { viewModel.showPremium(PremiumReason.BULK_ADD) }, label = { Text("Import File") }, leadingIcon = { Icon(Icons.Default.UploadFile, null, Modifier.size(16.dp)) })
            }
            OutlinedTextField(text, { text = it }, label = { Text("Paste topics or chapter: lines") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            Text("$count topics detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { viewModel.bulkAdd(target.first.id, target.second.id, text); onDismiss() }, modifier = Modifier.fillMaxWidth(), enabled = count > 0) { Text("Import Topics") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SubjectBlock(
    subject: StudySubject,
    query: String,
    status: TopicStatus?,
    onAddChapter: () -> Unit,
    onDeleteSubject: () -> Unit,
    onAddTopic: (StudyChapter) -> Unit,
    onBulkAdd: (StudyChapter) -> Unit,
    onDeleteChapter: (StudyChapter) -> Unit,
    onTopic: (TopicRef) -> Unit,
) {
    PlannerSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${subject.chapters.size} chapters • ${subject.percentDone()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAddChapter) { Icon(Icons.Default.Add, contentDescription = "Add chapter") }
                IconButton(onClick = onDeleteSubject) { Icon(Icons.Default.Delete, contentDescription = "Delete subject") }
            }
            subject.chapters.forEach { chapter ->
                val topics = chapter.topics
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || chapter.name.contains(query, ignoreCase = true) || subject.name.contains(query, ignoreCase = true) }
                    .filter { status == null || it.status == status }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chapter.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${chapter.percentDone()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { onAddTopic(chapter) }) { Icon(Icons.Default.Add, contentDescription = "Add topic") }
                        IconButton(onClick = { onBulkAdd(chapter) }) { Icon(Icons.Default.PlaylistAdd, contentDescription = "Bulk add") }
                        IconButton(onClick = { onDeleteChapter(chapter) }) { Icon(Icons.Default.Delete, contentDescription = "Delete chapter") }
                    }
                    topics.forEach { topic ->
                        TopicRow(TopicRef(subject, chapter, topic), onClick = { onTopic(TopicRef(subject, chapter, topic)) }, onDone = {})
                    }
                    if (topics.isEmpty()) Text("No matching topics.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: StudyPlan, onOpen: () -> Unit, onDelete: () -> Unit) {
    val progress = plan.rollup()
    PlannerSurface(onClick = onOpen) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${daysUntil(plan.examDate)?.let { "$it days left" } ?: "Exam date not set"} • ${plan.subjectCount ?: plan.subjects.size} subjects • ${progress.totalTopics} topics", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                LinearProgressIndicator(progress = { progress.completionPercent / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)))
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.MoreVert, contentDescription = "Plan actions") }
        }
    }
}

@Composable
private fun TopicRow(ref: TopicRef, onClick: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(ref.topic.status)
        Column(Modifier.weight(1f)) {
            Text(ref.topic.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${ref.subject.name} • ${ref.chapter.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (ref.topic.status != TopicStatus.DONE) IconButton(onClick = onDone) { Icon(Icons.Default.Check, contentDescription = "Mark done") }
    }
}

@Composable
private fun PlannerBottomNav(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    NavigationBar {
        val icons = mapOf(
            PlannerSection.TODAY to Icons.Default.Today,
            PlannerSection.SYLLABUS to Icons.Default.FactCheck,
            PlannerSection.CALENDAR to Icons.Default.CalendarMonth,
            PlannerSection.PLAN to Icons.Default.Settings,
            PlannerSection.INSIGHTS to Icons.Default.Insights,
        )
        PlannerSection.entries.forEach { section ->
            NavigationBarItem(
                selected = selected == section,
                onClick = { onSelect(section) },
                icon = { Icon(icons.getValue(section), contentDescription = section.label) },
                label = { Text(section.label, fontSize = 10.sp) },
            )
        }
    }
}

@Composable private fun PlannerHeader(plan: StudyPlan) {
    val days = daysUntil(plan.examDate)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(plan.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(if (days != null) "$days days left • ${readableDate(plan.examDate)}" else "Set exam date in Plan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable private fun ProgressCard(percent: Int, main: String, sub: String) {
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$percent%", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp)))
            Text(main, fontWeight = FontWeight.SemiBold)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable private fun SetupGuideCard(plan: StudyPlan, viewModel: StudyPlannerViewModel) {
    val hasDate = !plan.examDate.isNullOrBlank()
    val hasTopics = plan.flattenTopics().isNotEmpty()
    PlannerSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Setup Guide", fontWeight = FontWeight.Bold)
            GuideStep("Set exam date", hasDate) { viewModel.setSection(PlannerSection.PLAN) }
            GuideStep("Add topics", hasTopics) { viewModel.setSection(PlannerSection.SYLLABUS) }
            GuideStep("Build schedule", plan.flattenTopics().any { !it.topic.plannedDate.isNullOrBlank() }) { viewModel.autoDistribute(false, true) }
            GuideStep("Review calendar", false) { viewModel.setSection(PlannerSection.CALENDAR) }
        }
    }
}

@Composable private fun GuideStep(label: String, done: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.Check else Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (done) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(if (done) "Done" else "Next", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun PlannerActionButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp)
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier) {
    PlannerSurface(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            content()
        }
    }
}

@Composable private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun OffDayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Off days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { index, label ->
                FilterChip(selected = index in selected, onClick = { onToggle(index) }, label = { Text(label) })
            }
        }
    }
}

@Composable private fun DayChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(44.dp).height(62.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        Text(count.toString(), fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun StatusDot(status: TopicStatus) {
    val color = when (status) {
        TopicStatus.TODO -> MaterialTheme.colorScheme.outline
        TopicStatus.IN_PROGRESS -> Color(0xFF2563EB)
        TopicStatus.DONE -> Color(0xFF16A34A)
        TopicStatus.REVISION_NEEDED -> Color(0xFFF59E0B)
    }
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable private fun PlannerSurface(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) { content() }
}

@Composable private fun PlannerRow(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null)
    }
}

@Composable private fun EmptyPlannerCard(title: String, body: String, action: String, onAction: () -> Unit) {
    PlannerSurface {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(action) }
        }
    }
}

@Composable private fun TextInputDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = text.trim().length >= 2, onClick = { onConfirm(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun ConfirmActionDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PremiumGateSheet(reason: PremiumReason, onDismiss: () -> Unit, onUpgrade: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Text(reason.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(reason.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Unlock Premium") }
            TextButton(onClick = onDismiss) { Text("Not now") }
            Spacer(Modifier.height(18.dp))
        }
    }
}
