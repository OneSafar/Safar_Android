package com.safar.app.ui.studyplanner

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.R
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.PremiumReason
import com.safar.app.domain.model.studyplanner.CalendarTopicItem
import com.safar.app.domain.model.studyplanner.PlanProgress
import com.safar.app.domain.model.studyplanner.StudyChapter
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.StudySubject
import com.safar.app.domain.model.studyplanner.StudyTopic
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val plannerTopicStatusFilterChips = listOf(
    TopicStatus.TODO,
    TopicStatus.IN_PROGRESS,
    TopicStatus.DONE,
    TopicStatus.REVISION_NEEDED,
)

private fun plannerTopicStatusDisplayLabel(status: TopicStatus): String =
    status.label

private fun syllabusTopicMatchesFilter(topicStatus: TopicStatus, filter: TopicStatus?): Boolean {
    if (filter == null) return true
    return topicStatus == filter
}

private data class LandingTemplatePreset(
    val id: String,
    val title: String,
    val examBody: String,
    val categoryLabel: String,
    val description: String,
    val estimatedTopics: Int,
    val recommendedDailyGoal: Int,
    val startColor: Color,
    val endColor: Color,
)

private data class LandingTemplateCardModel(
    val id: String,
    val title: String,
    val examBody: String,
    val categoryLabel: String,
    val description: String,
    val estimatedTopics: Int,
    val recommendedDailyGoal: Int,
    val startColor: Color,
    val endColor: Color,
)

private val landingTemplateCatalog = listOf(
    LandingTemplatePreset(
        id = "ssc-cgl-tier1",
        title = "SSC CGL Tier-1",
        examBody = "SSC",
        categoryLabel = "GOVT EXAM",
        description = "Combined Graduate Level Examination Tier-1 — 100 questions, 200 marks, 60 minutes",
        estimatedTopics = 196,
        recommendedDailyGoal = 4,
        startColor = Color(0xFF19264A),
        endColor = Color(0xFF23366C),
    ),
    LandingTemplatePreset(
        id = "railway-ntpc",
        title = "Railway NTPC CBT-1",
        examBody = "RRB",
        categoryLabel = "GOVT EXAM",
        description = "Non-Technical Popular Categories CBT-1 — 100 questions, 100 marks, 90 minutes",
        estimatedTopics = 148,
        recommendedDailyGoal = 4,
        startColor = Color(0xFF3E2812),
        endColor = Color(0xFF6E4514),
    ),
    LandingTemplatePreset(
        id = "bank-po-prelims",
        title = "Bank PO Prelims",
        examBody = "IBPS / SBI",
        categoryLabel = "BANKING",
        description = "IBPS PO / SBI PO Preliminary Exam — 100 questions, 100 marks, 60 minutes",
        estimatedTopics = 130,
        recommendedDailyGoal = 3,
        startColor = Color(0xFF0E3D31),
        endColor = Color(0xFF19664F),
    ),
    LandingTemplatePreset(
        id = "jee-mains",
        title = "JEE Mains",
        examBody = "NTA",
        categoryLabel = "ENGINEERING",
        description = "Joint Entrance Examination (Main) — 90 questions across Physics, Chemistry, and Mathematics.",
        estimatedTopics = 217,
        recommendedDailyGoal = 4,
        startColor = Color(0xFF2C1D52),
        endColor = Color(0xFF4A2C84),
    ),
    LandingTemplatePreset(
        id = "neet-ug",
        title = "NEET UG",
        examBody = "NTA",
        categoryLabel = "MEDICAL",
        description = "National Eligibility cum Entrance Test (UG) — 200 questions across Physics, Chemistry, and Biology.",
        estimatedTopics = 230,
        recommendedDailyGoal = 4,
        startColor = Color(0xFF4C1C2B),
        endColor = Color(0xFF7A2444),
    ),
)

private fun buildLandingTemplateCards(
    stateTemplates: List<com.safar.app.domain.model.studyplanner.ExamTemplateSummary>,
): List<LandingTemplateCardModel> {
    val templatesById = stateTemplates.associateBy { it.id }
    return landingTemplateCatalog.map { preset ->
        val live = templatesById[preset.id]
        LandingTemplateCardModel(
            id = preset.id,
            title = live?.name ?: preset.title,
            examBody = preset.examBody,
            categoryLabel = preset.categoryLabel,
            description = live?.description ?: preset.description,
            estimatedTopics = live?.topicCount ?: preset.estimatedTopics,
            recommendedDailyGoal = live?.recommendedDailyGoal ?: preset.recommendedDailyGoal,
            startColor = preset.startColor,
            endColor = preset.endColor,
        )
    }
}

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                if (state.selectedPlan != null || state.section == PlannerSection.YOUR_EXAMS) {
                    PlannerBottomNav(selected = state.section, onSelect = viewModel::setSection)
                }
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                PlannerHome(state = state, viewModel = viewModel)
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
                Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                    Text("Study Planner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Plans, syllabus, calendar, settings, and insights in one place.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
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
            Text("Exam date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it })
            OutlinedTextField(value = dailyGoal, onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Topics per day") }, modifier = Modifier.fillMaxWidth())
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
    val plan = state.selectedPlan
    when (state.section) {
        PlannerSection.YOUR_EXAMS -> StudyPlansScreen(state = state, viewModel = viewModel)
        PlannerSection.PLAN -> if (plan != null) PlanTabMerged(plan, state, viewModel) else PlannerExamPickerLanding(state = state, viewModel = viewModel, onOpenExams = { viewModel.setSection(PlannerSection.YOUR_EXAMS) })
        PlannerSection.SYLLABUS -> if (plan != null) SyllabusTab(plan, state, viewModel) else PlannerExamPickerLanding(state = state, viewModel = viewModel, onOpenExams = { viewModel.setSection(PlannerSection.YOUR_EXAMS) })
        PlannerSection.CALENDAR -> if (plan != null) CalendarTab(plan, state, viewModel) else PlannerExamPickerLanding(state = state, viewModel = viewModel, onOpenExams = { viewModel.setSection(PlannerSection.YOUR_EXAMS) })
        PlannerSection.INSIGHTS -> if (plan != null) InsightsTab(plan, state, viewModel) else PlannerExamPickerLanding(state = state, viewModel = viewModel, onOpenExams = { viewModel.setSection(PlannerSection.YOUR_EXAMS) })
    }
}

@Composable
private fun PlannerExamPickerLanding(
    state: StudyPlannerUiState,
    viewModel: StudyPlannerViewModel,
    onOpenExams: () -> Unit,
) {
    val landingTemplates = remember(state.templates) { buildLandingTemplateCards(state.templates) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var useCustomPlan by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableStateOf("3") }
    var examType by remember { mutableStateOf("") }
    var landingError by remember { mutableStateOf("") }
    val offDays = remember { mutableStateOf(setOf(0)) }

    val selectedTemplate = landingTemplates.firstOrNull { it.id == selectedTemplateId }

    LaunchedEffect(selectedTemplateId, selectedTemplate, useCustomPlan) {
        if (useCustomPlan) return@LaunchedEffect
        val template = selectedTemplate ?: return@LaunchedEffect
        if (title.isBlank()) title = template.title
        if (examType.isBlank()) examType = template.title
        if (dailyGoal == "3") {
            dailyGoal = template.recommendedDailyGoal.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Pick your exam.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Choose a pre-loaded syllabus template and we'll build your study schedule in seconds.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val isTwoColumn = maxWidth >= 560.dp
                val items = landingTemplates + null
                if (isTwoColumn) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { template ->
                                    if (template != null) {
                                        PlannerTemplateCard(
                                            template = template,
                                            selected = selectedTemplateId == template.id && !useCustomPlan,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                useCustomPlan = false
                                                selectedTemplateId = template.id
                                                examType = template.title
                                                title = template.title
                                                dailyGoal = template.recommendedDailyGoal.toString()
                                            },
                                        )
                                    } else {
                                        PlannerCustomPlanCard(
                                            selected = useCustomPlan,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                useCustomPlan = true
                                                selectedTemplateId = null
                                            },
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { template ->
                            if (template != null) {
                                PlannerTemplateCard(
                                    template = template,
                                    selected = selectedTemplateId == template.id && !useCustomPlan,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        useCustomPlan = false
                                        selectedTemplateId = template.id
                                        examType = template.title
                                        title = template.title
                                        dailyGoal = template.recommendedDailyGoal.toString()
                                    },
                                )
                            } else {
                                PlannerCustomPlanCard(
                                    selected = useCustomPlan,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        useCustomPlan = true
                                        selectedTemplateId = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (useCustomPlan) {
            item {
                PlannerQuickCreateForm(
                    title = title,
                    onTitleChange = { title = it },
                    examType = examType,
                    onExamTypeChange = { examType = it },
                    examDate = examDate,
                    onExamDateChange = { examDate = it },
                    dailyGoal = dailyGoal,
                    onDailyGoalChange = { dailyGoal = it.filter(Char::isDigit).take(2) },
                    offDays = offDays.value,
                    onToggleOffDay = { day -> offDays.value = if (day in offDays.value) offDays.value - day else offDays.value + day },
                    confirmLabel = if (state.mutating) "Creating..." else "Create Custom Plan",
                    onConfirm = {
                        if (examDate.isBlank()) {
                            landingError = "Set your exam date to generate a schedule"
                            return@PlannerQuickCreateForm
                        }
                        landingError = ""
                        viewModel.createPlan(
                            title.ifBlank { "Study Plan" },
                            examType.ifBlank { null },
                            examDate,
                            dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                            offDays.value.toList(),
                        )
                    },
                    isDateError = landingError.isNotBlank() && examDate.isBlank(),
                )
            }
        } else if (selectedTemplate != null) {
            item {
                PlannerQuickCreateForm(
                    title = title,
                    onTitleChange = { title = it },
                    examType = selectedTemplate.title,
                    onExamTypeChange = {},
                    examDate = examDate,
                    onExamDateChange = { examDate = it },
                    dailyGoal = dailyGoal,
                    onDailyGoalChange = { dailyGoal = it.filter(Char::isDigit).take(2) },
                    offDays = offDays.value,
                    onToggleOffDay = { day -> offDays.value = if (day in offDays.value) offDays.value - day else offDays.value + day },
                    confirmLabel = if (state.mutating) "Creating..." else "Create My Plan",
                    onConfirm = {
                        if (examDate.isBlank()) {
                            landingError = "Set your exam date to generate a schedule"
                            return@PlannerQuickCreateForm
                        }
                        landingError = ""
                        viewModel.createFromTemplateOrLocal(
                            selectedTemplate.id,
                            title.ifBlank { selectedTemplate.title },
                            examDate,
                            dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: selectedTemplate.recommendedDailyGoal,
                            offDays.value.toList(),
                        )
                    },
                    isDateError = landingError.isNotBlank() && examDate.isBlank(),
                )
            }
        }

        if (landingError.isNotBlank()) {
            item {
                Text(
                    landingError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onOpenExams) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun PlannerTemplateCard(
    template: LandingTemplateCardModel,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = Brush.linearGradient(listOf(template.startColor, template.endColor))

    Box(
        modifier = modifier
            .heightIn(min = 166.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .border(
                if (selected) 2.5.dp else 1.dp,
                if (selected) Color.White else Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                if (selected) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF1B212D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Surface(color = Color(0xFF2D4B8C).copy(alpha = 0.88f), shape = RoundedCornerShape(99.dp)) {
                    Text(
                        template.categoryLabel,
                        color = Color(0xFFD6E4FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Text(
                template.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                template.description ?: "Pre-loaded syllabus template",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${template.estimatedTopics} topics",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("•", color = Color.White.copy(alpha = 0.7f))
                Text(
                    template.examBody,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("•", color = Color.White.copy(alpha = 0.7f))
                Text(
                    "${template.recommendedDailyGoal}/day",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PlannerCustomPlanCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 166.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF111827))
            .drawBehind {
                val stroke = Stroke(
                    width = if (selected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
                )
                drawRoundRect(
                    color = if (selected) Color(0xFF93C5FD) else Color(0xFF64748B),
                    style = stroke,
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                )
            }
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                if (selected) {
                    Surface(
                        color = Color(0xFF93C5FD),
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Text(
                "Custom Plan",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
            )
            Text(
                "Build your own plan from scratch. Paste your syllabus or set it up manually.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Any exam • Your syllabus",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PlannerQuickCreateForm(
    title: String,
    onTitleChange: (String) -> Unit,
    examType: String,
    onExamTypeChange: (String) -> Unit,
    examDate: String,
    onExamDateChange: (String) -> Unit,
    dailyGoal: String,
    onDailyGoalChange: (String) -> Unit,
    offDays: Set<Int>,
    onToggleOffDay: (Int) -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    isDateError: Boolean = false,
) {
    PlannerSurface {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set Up Your Exam Plan", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Plan title") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (examType.isNotBlank()) {
                Text(
                    examType,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                OutlinedTextField(
                    value = examType,
                    onValueChange = onExamTypeChange,
                    label = { Text("Exam name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Exam date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isDateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            PlannerExamDateField(examDateIso = examDate, onExamDateChange = onExamDateChange, isError = isDateError)
            OutlinedTextField(
                value = dailyGoal,
                onValueChange = onDailyGoalChange,
                label = { Text("Topics per day") },
                modifier = Modifier.fillMaxWidth(),
            )
            OffDayPicker(selected = offDays, onToggle = onToggleOffDay)
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun PlanTabMerged(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val today = todayKey()
    val refs = plan.flattenTopics()
    val todayTopics = refs.filter { it.topic.plannedDate?.take(10) == today }
    val overdue = refs.filter { (it.topic.plannedDate?.take(10) ?: "9999") < today && it.topic.status != TopicStatus.DONE }
    val upcoming = refs.filter { (it.topic.plannedDate?.take(10) ?: "") > today }.take(6)
    val progress = plan.rollup()
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
                        viewModel.setError("Failed to export PDF: ${e.localizedMessage}")
                    }
                }
            }
        }
    )

    var selectedTopic by remember { mutableStateOf<TopicRef?>(null) }
    var topicSheetNonce by remember(plan.id) { mutableStateOf(0) }
    var title by remember(plan.id) { mutableStateOf(plan.title) }
    var examType by remember(plan.id) { mutableStateOf(plan.examType.orEmpty()) }
    var examDate by remember(plan.id) { mutableStateOf(plan.examDate?.take(10).orEmpty()) }
    var dailyGoal by remember(plan.id) { mutableStateOf((plan.dailyGoal ?: 3).toString()) }
    var offDays by remember(plan.id) { mutableStateOf(plan.offDays.toSet()) }
    LaunchedEffect(plan.id) {
        title = plan.title
        examType = plan.examType.orEmpty()
        examDate = plan.examDate?.take(10).orEmpty()
        dailyGoal = (plan.dailyGoal ?: 3).toString()
        offDays = plan.offDays.toSet()
    }
    var todayExpanded by remember(plan.id) { mutableStateOf(true) }
    var upcomingExpanded by remember(plan.id) { mutableStateOf(true) }
    var resetConfirm by remember { mutableStateOf(false) }
    if (resetConfirm) ConfirmActionDialog("Reset entire plan?", "All topics return to Todo and planned dates are cleared.", { resetConfirm = false }) { viewModel.resetPlan(); resetConfirm = false }

    selectedTopic?.let { TopicDetailSheet(it, openNonce = topicSheetNonce, viewModel = viewModel, onDismiss = { selectedTopic = null }) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PlannerHeader(plan, onExport = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") }) }
        item { SetupGuideCard(plan, viewModel) }
        item { ProgressCard(progress.completionPercent, "${progress.doneTopics} / ${progress.totalTopics} topics done", "Required pace: ${plan.dailyGoal ?: 3} topics/day") }
        item {
            PlannerActionButton("Add Topics", Icons.AutoMirrored.Filled.PlaylistAdd, Modifier.fillMaxWidth()) {
                viewModel.setSection(PlannerSection.SYLLABUS)
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
        }
        item {
            SettingsCard("Basics") {
                OutlinedTextField(title, { title = it }, label = { Text("Plan title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(examType, { examType = it }, label = { Text("Exam type") }, modifier = Modifier.fillMaxWidth())
                Text("Exam date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it })
                Button(
                    onClick = { viewModel.updatePlan(UpdatePlanRequest(title = title, examType = examType, examDate = examDate.ifBlank { null })) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Save Basics")
                }
            }
            SettingsCard("Study Capacity") {
                OutlinedTextField(dailyGoal, { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Topics per day") }, modifier = Modifier.fillMaxWidth())
                OffDayPicker(
                    selected = offDays,
                    onToggle = { day ->
                        offDays = if (day in offDays) offDays - day else offDays + day
                    },
                )
                Button(
                    onClick = {
                        viewModel.updatePlan(
                            UpdatePlanRequest(
                                dailyGoal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                                offDays = offDays.toList(),
                            ),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Save Capacity")
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { todayExpanded = !todayExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (todayExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (todayExpanded) "Collapse Today" else "Expand Today",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SectionTitle("Today", "${todayTopics.size} planned")
            }
        }
        if (todayExpanded) {
            items(todayTopics, key = { it.topic.id }) { ref ->
                TopicRow(
                    ref,
                    onClick = {
                        topicSheetNonce += 1
                        selectedTopic = ref
                    },
                    onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) },
                )
            }
        }
        if (overdue.isNotEmpty()) {
            item { SectionTitle("Overdue", "${overdue.size} need attention") }
            items(overdue.take(8), key = { it.topic.id }) { ref ->
                TopicRow(
                    ref,
                    onClick = {
                        topicSheetNonce += 1
                        selectedTopic = ref
                    },
                    onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) },
                )
            }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { upcomingExpanded = !upcomingExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (upcomingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (upcomingExpanded) "Collapse Upcoming" else "Expand Upcoming",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionTitle("Upcoming", "Next planned topics")
                }
            }
            if (upcomingExpanded) {
                items(upcoming, key = { it.topic.id }) { ref ->
                    TopicRow(
                        ref,
                        onClick = {
                            topicSheetNonce += 1
                            selectedTopic = ref
                        },
                        onDone = { viewModel.updateTopic(ref.topic.id, status = TopicStatus.DONE) },
                    )
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
        item {
            SettingsCard("Danger Zone") {
                Button(
                    onClick = { resetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Reset Entire Plan")
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SyllabusTab(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val subjectsOrdered = remember(plan.subjects) { plan.subjects.sortedBy { it.id } }
    val subjectIndexById = remember(plan.subjects) {
        subjectsOrdered.mapIndexed { index, s -> s.id to index }.toMap()
    }
    val subjectColorCount = max(1, plan.subjects.size)
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<TopicStatus?>(null) }
    var selectedTopic by remember { mutableStateOf<TopicRef?>(null) }
    var topicSheetNonce by remember(plan.id) { mutableStateOf(0) }
    var addSubject by remember { mutableStateOf(false) }
    var addChapterFor by remember { mutableStateOf<StudySubject?>(null) }
    var addTopicFor by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }
    var bulkFor by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }
    var deleteSubject by remember { mutableStateOf<StudySubject?>(null) }
    var deleteChapter by remember { mutableStateOf<Pair<StudySubject, StudyChapter>?>(null) }

    selectedTopic?.let { TopicDetailSheet(it, openNonce = topicSheetNonce, viewModel = viewModel, onDismiss = { selectedTopic = null }) }
    if (addSubject) TextInputDialog("Add subject", "Subject name", onDismiss = { addSubject = false }) { viewModel.addSubject(it); addSubject = false }
    addChapterFor?.let { subject -> TextInputDialog("Add chapter", "Chapter name", onDismiss = { addChapterFor = null }) { viewModel.addChapter(subject.id, it); addChapterFor = null } }
    addTopicFor?.let { pair -> TextInputDialog("Add topic", "Topic name", onDismiss = { addTopicFor = null }) { viewModel.addTopic(pair.first.id, pair.second.id, it); addTopicFor = null } }
    bulkFor?.let { pair -> BulkAddSheet(pair, state, viewModel, onDismiss = { bulkFor = null }) }
    deleteSubject?.let { subject -> ConfirmActionDialog("Delete subject?", "This removes ${subject.name}.", { deleteSubject = null }) { viewModel.deleteSubject(subject.id); deleteSubject = null } }
    deleteChapter?.let { pair -> ConfirmActionDialog("Delete chapter?", "This removes ${pair.second.name}.", { deleteChapter = null }) { viewModel.deleteChapter(pair.first.id, pair.second.id); deleteChapter = null } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
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
                                viewModel.setError("Failed to export PDF: ${e.localizedMessage}")
                            }
                        }
                    }
                }
            )

            PlannerHeader(plan, onExport = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") })
        }
        item {
            Button(
                onClick = { viewModel.autoDistribute(includeRevision = false, lockExisting = true) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Build schedule")
            }
        }
        item { SyllabusFullImportCard(state, viewModel) }
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
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(selected = status == null, onClick = { status = null }, label = { Text("All") })
                }
                items(plannerTopicStatusFilterChips.size, key = { plannerTopicStatusFilterChips[it].name }) { idx ->
                    val st = plannerTopicStatusFilterChips[idx]
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { addSubject = true }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Subject")
            }
        }
        items(subjectsOrdered, key = { it.id }) { subject ->
            SubjectBlock(
                subject = subject,
                subjectColorIndex = subjectIndexById[subject.id] ?: 0,
                subjectColorCount = subjectColorCount,
                query = query,
                status = status,
                onAddChapter = { addChapterFor = subject },
                onDeleteSubject = { deleteSubject = subject },
                onAddTopic = { chapter -> addTopicFor = subject to chapter },
                onBulkAdd = { chapter -> bulkFor = subject to chapter },
                onDeleteChapter = { chapter -> deleteChapter = subject to chapter },
                onTopic = { ref ->
                    topicSheetNonce += 1
                    selectedTopic = ref
                },
                onMarkDone = { topicId -> viewModel.updateTopic(topicId, status = TopicStatus.DONE) },
            )
        }
    }
}

@Composable
private fun CalendarTab(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val todayK = todayKey()
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    val locale = Locale.getDefault()
    val monthSlots = remember(visibleMonth) { monthCalendarSlots(visibleMonth) }
    val weeks = remember(monthSlots) { monthSlots.chunked(7) }
    var sheetDay by remember { mutableStateOf<String?>(null) }

    sheetDay?.let { day ->
        SelectedDayLogSheet(
            dateIso = day,
            plan = plan,
            items = state.calendar[day].orEmpty(),
            viewModel = viewModel,
            onDismiss = { sheetDay = null },
        )
    }

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
                        viewModel.setError("Failed to export PDF: ${e.localizedMessage}")
                    }
                }
            }
        }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PlannerHeader(plan, onExport = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") })
            Text("Tap any date to see your study plan for that day.\nOpen the log to Edit and/or move topics per day.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        visibleMonth.month.getDisplayName(TextStyle.FULL, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(visibleMonth.year.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { visibleMonth = YearMonth.now() }) {
                    Text("Today")
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                weeks.forEach { week ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        week.forEach { date ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            ) {
                                if (date != null) {
                                    val dateIso = date.toString()
                                    val dayItems = state.calendar[dateIso].orEmpty()
                                    CalendarDayChip(
                                        dateIso = dateIso,
                                        items = dayItems,
                                        selected = sheetDay == dateIso,
                                        isToday = dateIso == todayK,
                                        isOff = jsDayOfWeek(date) in plan.offDays.toSet(),
                                        dense = true,
                                        onClick = { sheetDay = dateIso },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalendarLegendDot(MaterialTheme.colorScheme.primary, "Planned")
                CalendarLegendDot(Color(0xFF16A34A), "Done")
                CalendarLegendDot(MaterialTheme.colorScheme.error, "Overdue")
                CalendarLegendDot(Color(0xFFF59E0B), "Off")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                OutlinedButton(onClick = { viewModel.setSection(PlannerSection.SYLLABUS) }, shape = RoundedCornerShape(12.dp)) { Text("Open Syllabus") }
            }
        }
    }
}

private sealed class InsightSummaryFocus {
    data object BarDone : InsightSummaryFocus()
    data object BarRemaining : InsightSummaryFocus()
    data object Complete : InsightSummaryFocus()
    data object Exam : InsightSummaryFocus()
    data object Pace : InsightSummaryFocus()
    data object Forecast : InsightSummaryFocus()
    data object Buffer : InsightSummaryFocus()
}

@Composable
private fun InsightsStackedCompletionBar(
    doneTopics: Int,
    totalTopics: Int,
    completionPercent: Int,
    focus: InsightSummaryFocus,
    onBarFocus: (InsightSummaryFocus) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 48.dp,
) {
    if (totalTopics <= 0) {
        Text("No topics yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
        return
    }
    val remaining = (totalTopics - doneTopics).coerceAtLeast(0)
    val shape = RoundedCornerShape(14.dp)

    val doneFraction by animateFloatAsState(
        targetValue = doneTopics.toFloat() / totalTopics.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "doneFraction",
    )
    val doneColor by animateColorAsState(
        targetValue = if (focus == InsightSummaryFocus.BarDone) Color(0xFF22C55E) else Color(0xFF16A34A),
        animationSpec = tween(200),
        label = "doneColor",
    )
    val remColor by animateColorAsState(
        targetValue = if (focus == InsightSummaryFocus.BarRemaining) Color(0xFFD1D5DB) else Color(0xFFE5E7EB),
        animationSpec = tween(200),
        label = "remColor",
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(shape)
                .background(remColor)
                .semantics { contentDescription = "Completion bar, $doneTopics done, $remaining remaining" }
                .clickable { onBarFocus(InsightSummaryFocus.BarRemaining) },
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(doneFraction)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF16A34A), Color(0xFF22C55E))))
                    .semantics { contentDescription = "Done, $doneTopics topics" }
                    .clickable { onBarFocus(InsightSummaryFocus.BarDone) },
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "$completionPercent% done",
                    color = Color(0xFF1F2937),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$remaining left",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InsightSummaryFocusCaption(focus: InsightSummaryFocus, insights: PlannerInsights, rollup: PlanProgress) {
    val s = insights.summary
    val remaining = (rollup.totalTopics - rollup.doneTopics).coerceAtLeast(0)
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val (text, color) = when (focus) {
        InsightSummaryFocus.BarDone ->
            "Done: ${rollup.doneTopics} topics marked complete." to variant
        InsightSummaryFocus.BarRemaining ->
            "Remaining: $remaining topics still to finish." to variant
        InsightSummaryFocus.Complete ->
            "${s.completionPercent}% of your syllabus is complete (${rollup.doneTopics} / ${rollup.totalTopics} topics)." to variant
        InsightSummaryFocus.Exam ->
            when (val d = s.daysUntilExam) {
                null -> "Set an exam date in Plan to see the countdown." to variant
                else -> "Exam is in $d day${if (d == 1) "" else "s"}." to variant
            }
        InsightSummaryFocus.Pace ->
            when (val p = s.requiredTopicsPerStudyDay) {
                null -> "Add topics and an exam date to estimate daily pace." to variant
                else -> "You need about ${"%.1f".format(p)} topics per study day to finish on time." to variant
            }
        InsightSummaryFocus.Forecast ->
            when (val fc = s.forecastCompletionDate) {
                null -> "Not enough schedule data yet to forecast a finish date." to variant
                else -> "At your current pace you may finish around ${readableDate(fc)}." to variant
            }
        InsightSummaryFocus.Buffer ->
            when (val buf = s.daysBuffer) {
                null ->
                    "Finish more planned days to see how many buffer days you have before the exam." to variant
                else ->
                    if (buf >= 0) {
                        "$buf study-day buffer before the exam — room to breathe." to MaterialTheme.colorScheme.primary
                    } else {
                        "Behind by ${-buf} study days — accelerate pace or reschedule topics." to MaterialTheme.colorScheme.error
                    }
            }
    }
    Text(text, fontSize = 12.sp, color = color, lineHeight = 16.sp)
}

@Composable
private fun InsightMetricPill(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "pillBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "pillText",
    )
    val valueColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "pillValue",
    )
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = valueColor, maxLines = 1)
        Text(label, fontSize = 10.sp, color = textColor, maxLines = 1, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InsightsSummaryInteractivePanel(
    insights: PlannerInsights,
    rollup: PlanProgress,
    planId: String,
    modifier: Modifier = Modifier,
) {
    var focus by remember(planId) { mutableStateOf<InsightSummaryFocus>(InsightSummaryFocus.Complete) }
    val s = insights.summary

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InsightStatusBadge(s.onTrackStatus)
            Text(
                "${s.remainingTopics} topics left",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        InsightsStackedCompletionBar(
            doneTopics = rollup.doneTopics,
            totalTopics = rollup.totalTopics,
            completionPercent = s.completionPercent,
            focus = focus,
            onBarFocus = { focus = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InsightMetricPill(
                label = "Complete",
                value = "${s.completionPercent}%",
                selected = focus == InsightSummaryFocus.Complete,
                onClick = { focus = InsightSummaryFocus.Complete },
            )
            InsightMetricPill(
                label = "Exam",
                value = s.daysUntilExam?.let { "${it}d" } ?: "—",
                selected = focus == InsightSummaryFocus.Exam,
                onClick = { focus = InsightSummaryFocus.Exam },
            )
            InsightMetricPill(
                label = "Pace/day",
                value = s.requiredTopicsPerStudyDay?.let { "%.1f".format(it) } ?: "—",
                selected = focus == InsightSummaryFocus.Pace,
                onClick = { focus = InsightSummaryFocus.Pace },
            )
            InsightMetricPill(
                label = "Buffer",
                value = s.daysBuffer?.let { if (it >= 0) "+${it}d" else "${it}d" } ?: "—",
                selected = focus == InsightSummaryFocus.Buffer,
                onClick = { focus = InsightSummaryFocus.Buffer },
            )
        }

        AnimatedContent(
            targetState = focus,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "captionAnim",
        ) { f ->
            InsightSummaryFocusCaption(focus = f, insights = insights, rollup = rollup)
        }
    }
}

@Composable
private fun InsightsCoverageSubjectRow(
    row: PlannerInsightSubjectRow,
    subjectColorIndex: Int,
    subjectColorCount: Int,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(
                row.subjectName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${row.remainingTopics} left · ${row.revisionTopics} revision",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(108.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(row.completionPercent / 100f)
                        .background(brush = subjectMeterBrush(subjectColorIndex, subjectColorCount)),
                )
            }
            Text(
                "${row.completionPercent}%",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(min = 36.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun InsightsTab(plan: StudyPlan, state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    val insights = remember(plan.id, state.calendar, state.analytics, plan.subjects.size) {
        PlannerInsightsCalculator.compute(plan, state.calendar, state.analytics)
    }
    val rollup = remember(plan.id, plan.subjects) { plan.rollup() }
    val subjectIndexById = remember(plan.subjects) {
        plan.subjects.sortedBy { it.id }.mapIndexed { index, s -> s.id to index }.toMap()
    }
    val subjectCount = max(1, plan.subjects.size)
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
                        viewModel.setError("Failed to export PDF: ${e.localizedMessage}")
                    }
                }
            }
        }
    )

    val days: Int? = daysUntil(plan.examDate)?.toInt()
    val s = insights.summary
    val totalTopics = rollup.totalTopics
    val doneTopics = rollup.doneTopics
    val remaining = (totalTopics - doneTopics).coerceAtLeast(0)
    val pace = s.requiredTopicsPerStudyDay?.let { kotlin.math.ceil(it.toDouble()).toInt() } ?: 0
    val dailyGoal = plan.dailyGoal?.takeIf { it > 0 } ?: pace
    val behindDays = s.daysBuffer?.let { if (it < 0) -it else 0 } ?: 0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InsightsTopHeader(
                plan = plan,
                days = days,
                onExport = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") },
            )
        }

        if (s.onTrackStatus == InsightTrackStatus.BEHIND || s.onTrackStatus == InsightTrackStatus.AT_RISK) {
            val today = if (pace > 0) pace else dailyGoal
            val msg = if (behindDays > 0) {
                "You are $behindDays day${if (behindDays == 1) "" else "s"} behind pace. Cover $today topic${if (today == 1) "" else "s"} today to get back on track."
            } else {
                "You're slightly off pace. Cover $today topic${if (today == 1) "" else "s"} today to stay on track."
            }
            item { InsightsPaceBanner(msg) }
        }

        item {
            OverallProgressCard(
                completionPercent = s.completionPercent,
                doneTopics = doneTopics,
                totalTopics = totalTopics,
                remaining = remaining,
                days = days,
                dailyGoal = dailyGoal,
            )
        }

        item {
            SubjectProgressCard(
                rows = insights.subjectRows,
                subjectCount = subjectCount,
                subjectIndexById = subjectIndexById,
            )
        }

        item {
            AddTopicsToPlanButton(onClick = { viewModel.setSection(PlannerSection.SYLLABUS) })
        }
    }
}

@Composable
private fun InsightsTopHeader(
    plan: StudyPlan,
    days: Int?,
    onExport: () -> Unit,
) {
    val examDays = daysUntil(plan.examDate)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                plan.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    if (!plan.examDate.isNullOrBlank()) append(readableDate(plan.examDate))
                    if (days != null) {
                        if (isNotEmpty()) append(" · ")
                        append("$days days remaining")
                    }
                    if (isEmpty()) append("Set exam date in Plan")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExamDaysCountdownBadge(days = examDays)
            PlannerExportButton(onClick = onExport)
        }
    }
}

/** Navy → teal → cyan for Insights overall progress (matches product gradient). */
private fun insightsOverallProgressFillBrush(): Brush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF050F1F),
        Color(0xFF0C2540),
        Color(0xFF0D5C5C),
        Color(0xFF0891B2),
        Color(0xFF22D3EE),
        Color(0xFF5AF5E5),
    ),
)

@Composable
private fun InsightsLiquidOverallProgressBar(
    completionPercent: Int,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val target = (completionPercent / 100f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "insightsLiquidFill",
    )
    val infinite = rememberInfiniteTransition(label = "insightsLiquidShimmer")
    val shimmerPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerPhase",
    )
    val shimmerWave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (kotlin.math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "liquidWave",
    )

    val shape = RoundedCornerShape(999.dp)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    val description = "Overall syllabus progress, $completionPercent percent"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
            .semantics { contentDescription = description },
    ) {
        if (animatedFraction > 0.001f) {
            BoxWithConstraints(
                Modifier
                    .fillMaxHeight()
                    .width(maxWidth * animatedFraction)
                    .clip(shape)
                    .graphicsLayer {
                        scaleY = 0.94f + sin(shimmerWave.toDouble()).toFloat() * 0.06f
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(insightsOverallProgressFillBrush()),
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.55f)
                        .graphicsLayer { alpha = 0.85f }
                        .offset {
                            val wPx = maxWidth.roundToPx().toFloat().coerceAtLeast(1f)
                            val x = (shimmerPhase - 0.35f) * wPx * 1.6f
                            IntOffset(x.roundToInt(), 0)
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun InsightsPaceBanner(message: String) {
    val amber = Color(0xFFF59E0B)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = amber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, amber.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(18.dp),
            )
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverallProgressCard(
    completionPercent: Int,
    doneTopics: Int,
    totalTopics: Int,
    remaining: Int,
    days: Int?,
    dailyGoal: Int,
) {
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Overall progress", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$completionPercent%",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "$doneTopics of $totalTopics topics complete",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$remaining topics remaining",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            InsightsLiquidOverallProgressBar(completionPercent = completionPercent)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightsInnerStat(
                    label = "Exam in",
                    value = days?.let { "$it days" } ?: "—",
                    valueColor = if (days != null && days <= 30) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Topics per day",
                    value = if (dailyGoal > 0) "$dailyGoal topic${if (dailyGoal == 1) "" else "s"}" else "—",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InsightsInnerStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun SubjectProgressCard(
    rows: List<PlannerInsightSubjectRow>,
    subjectCount: Int,
    subjectIndexById: Map<String, Int>,
) {
    PlannerSurface {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Subject progress",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${rows.size} subject${if (rows.size == 1) "" else "s"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rows.isEmpty()) {
                Text(
                    "Add topics to see subject progress.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEachIndexed { idx, row ->
                    val colorIdx = subjectIndexById[row.subjectId] ?: 0
                    SubjectProgressRow(row, colorIdx, max(1, subjectCount))
                    if (idx < rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectProgressRow(
    row: PlannerInsightSubjectRow,
    subjectColorIndex: Int,
    subjectColorCount: Int,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                Text(
                    row.subjectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.remainingTopics} topic${if (row.remainingTopics == 1) "" else "s"} remaining",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${row.completionPercent}%",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            if (row.completionPercent > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((row.completionPercent / 100f).coerceIn(0f, 1f))
                        .background(brush = subjectMeterBrush(subjectColorIndex, subjectColorCount)),
                )
            }
        }
    }
}

@Composable
private fun AddTopicsToPlanButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add topics to plan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailSheet(ref: TopicRef, openNonce: Int, viewModel: StudyPlannerViewModel, onDismiss: () -> Unit) {
    var name by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.name) }
    var notes by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.notes.orEmpty()) }
    var date by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.plannedDate?.take(10).orEmpty()) }
    var status by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.status) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ref.chapter.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedTextField(name, { name = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plannerTopicStatusFilterChips.forEach { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }
            OutlinedTextField(date, { date = it }, label = { Text("Planned date") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.deleteTopic(ref.topic.id); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Delete") }
                Button(
                    onClick = {
                        viewModel.updateTopic(ref.topic.id, status, name, date.ifBlank { "" }, notes)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkAddSheet(
    target: Pair<StudySubject, StudyChapter>,
    state: StudyPlannerUiState,
    viewModel: StudyPlannerViewModel,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val count = parseBulkSyllabus(text).flatMap { it.second }.size
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
    )
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val part = withContext(Dispatchers.IO) { buildSyllabusUploadPart(context, uri) }
            if (part == null) {
                viewModel.setError("Unable to read selected file.")
                return@launch
            }
            viewModel.importSyllabusFile(part.first, part.second)
        }
    }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        val extracted = extractBulkTopicsFromSyllabusCode(state.syllabusImportDraft)
        if (extracted.isNotBlank()) {
            text = extracted
        } else {
            viewModel.setError("No topics found in imported file.")
        }
        viewModel.clearSyllabusImportDraft()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bulk Add", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${target.first.name} • ${target.second.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Paste Text") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(16.dp)) })
                AssistChip(
                    onClick = { filePicker.launch(mimeTypes) },
                    label = { Text("Import File") },
                    leadingIcon = { Icon(Icons.Default.UploadFile, null, Modifier.size(16.dp)) },
                )
            }
            OutlinedTextField(text, { text = it }, label = { Text("Paste topics or chapter: lines") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            state.syllabusImportFileName?.let { name ->
                Text("Imported: $name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$count topics detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { viewModel.bulkAdd(target.first.id, target.second.id, text); onDismiss() }, modifier = Modifier.fillMaxWidth(), enabled = count > 0) { Text("Import Topics") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SubjectBlock(
    subject: StudySubject,
    subjectColorIndex: Int,
    subjectColorCount: Int,
    query: String,
    status: TopicStatus?,
    onAddChapter: () -> Unit,
    onDeleteSubject: () -> Unit,
    onAddTopic: (StudyChapter) -> Unit,
    onBulkAdd: (StudyChapter) -> Unit,
    onDeleteChapter: (StudyChapter) -> Unit,
    onTopic: (TopicRef) -> Unit,
    onMarkDone: (String) -> Unit,
) {
    var subjectExpanded by remember(subject.id) { mutableStateOf(false) }
    val chapterExpanded = remember(subject.id) { mutableStateMapOf<String, Boolean>() }

    PlannerSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val subjectHeaderShape = RoundedCornerShape(12.dp)
            val subjectHeaderColors = Modifier
                .fillMaxWidth()
                .clip(subjectHeaderShape)
                .background(brush = subjectHeaderBrush(subjectColorIndex, subjectColorCount))
                .padding(horizontal = 4.dp, vertical = 4.dp)
            Row(subjectHeaderColors, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { subjectExpanded = !subjectExpanded }) {
                    Icon(
                        if (subjectExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (subjectExpanded) "Collapse subject" else "Expand subject",
                    )
                }
                Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                    Text(
                        subject.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        "${subject.chapters.size} chapters • ${subject.percentDone()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onAddChapter) { Icon(Icons.Default.Add, contentDescription = "Add chapter") }
                IconButton(onClick = onDeleteSubject) { Icon(Icons.Default.Delete, contentDescription = "Delete subject") }
            }
            if (subjectExpanded) {
                subject.chapters.forEach { chapter ->
                    val chExpanded = chapterExpanded[chapter.id] ?: false
                    val isPlaceholderChapter = isBulkPlaceholderChapter(chapter)
                    val chapterShape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(chapterShape)
                            .background(
                                if (isPlaceholderChapter) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .then(
                                if (isPlaceholderChapter) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                                        chapterShape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .then(if (isPlaceholderChapter) Modifier else Modifier.background(chapterHierarchyBrush()))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    chapterExpanded[chapter.id] = !chExpanded
                                },
                            ) {
                                Icon(
                                    if (chExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (chExpanded) "Collapse chapter" else "Expand chapter",
                                )
                            }
                            Text(
                                chapter.name,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                                color = if (isPlaceholderChapter) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isPlaceholderChapter) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Needs chapter name",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                "${chapter.percentDone()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            IconButton(onClick = { onAddTopic(chapter) }) { Icon(Icons.Default.Add, contentDescription = "Add topic") }
                            IconButton(onClick = { onBulkAdd(chapter) }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Bulk add") }
                            IconButton(onClick = { onDeleteChapter(chapter) }) { Icon(Icons.Default.Delete, contentDescription = "Delete chapter") }
                        }
                        if (chExpanded) {
                            val topics = remember(chapter.topics, query, status, chapter.name, subject.name) {
                                val q = query.trim()
                                val matchSubject = q.isNotBlank() && subject.name.contains(q, ignoreCase = true)
                                val matchChapter = q.isNotBlank() && chapter.name.contains(q, ignoreCase = true)
                                chapter.topics.filter { t ->
                                    val matchesQuery = q.isBlank() || matchSubject || matchChapter || t.name.contains(q, ignoreCase = true)
                                    matchesQuery && syllabusTopicMatchesFilter(t.status, status)
                                }
                            }
                            topics.forEach { topic ->
                                val ref = TopicRef(subject, chapter, topic)
                                TopicRow(
                                    ref,
                                    onClick = { onTopic(ref) },
                                    onDone = { onMarkDone(topic.id) },
                                    useSyllabusHierarchyBackground = true,
                                )
                            }
                            if (topics.isEmpty()) Text("No matching topics.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: StudyPlan, onOpen: () -> Unit, onDelete: () -> Unit) {
    val progress = plan.rollup()
    val examDays = daysUntil(plan.examDate)
    PlannerSurface(onClick = onOpen) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackCountdown = maxWidth < 360.dp
                if (stackCountdown) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).widthIn(min = 0.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "${plan.subjectCount ?: plan.subjects.size} subjects • ${progress.totalTopics} topics",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDelete) { Icon(Icons.Default.MoreVert, contentDescription = "Plan actions") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ExamDaysCountdownBadge(days = examDays)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).widthIn(min = 0.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "${plan.subjectCount ?: plan.subjects.size} subjects • ${progress.totalTopics} topics",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ExamDaysCountdownBadge(days = examDays)
                        IconButton(onClick = onDelete) { Icon(Icons.Default.MoreVert, contentDescription = "Plan actions") }
                    }
                }
            }
            LinearProgressIndicator(progress = { progress.completionPercent / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)))
        }
    }
}

@Composable
private fun TopicRow(
    ref: TopicRef,
    onClick: () -> Unit,
    onDone: () -> Unit,
    useSyllabusHierarchyBackground: Boolean = false,
) {
    val rowBg = if (useSyllabusHierarchyBackground) {
        Modifier.background(topicHierarchyBrush())
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(rowBg).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(ref.topic.status)
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(
                ref.topic.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                "${ref.subject.name} • ${ref.chapter.name}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (ref.topic.status != TopicStatus.DONE) IconButton(onClick = onDone) { Icon(Icons.Default.Check, contentDescription = "Mark done") }
    }
}

@Composable
private fun PlannerBottomNav(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    NavigationBar {
        val icons = mapOf(
            PlannerSection.YOUR_EXAMS to Icons.Default.School,
            PlannerSection.SYLLABUS to Icons.AutoMirrored.Filled.FactCheck,
            PlannerSection.CALENDAR to Icons.Default.CalendarMonth,
            PlannerSection.PLAN to Icons.Default.Today,
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

@Composable
private fun PlannerExportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun PlannerHeader(plan: StudyPlan, onExport: (() -> Unit)? = null) {
    val days = daysUntil(plan.examDate)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                plan.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            if (onExport != null) {
                PlannerExportButton(onClick = onExport)
            }
        }
        Text(
            if (days != null) "$days days left • ${readableDate(plan.examDate)}" else "Set exam date in Plan",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable private fun ProgressCard(percent: Int, main: String, sub: String) {
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$percent%",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text("complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(99.dp)))
            Text(main, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
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
        Icon(if (done) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (done) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f).widthIn(min = 0.dp),
        )
        Text(
            subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).widthIn(min = 0.dp),
            textAlign = TextAlign.End,
        )
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
        Text("My Rest Days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { index, label ->
                FilterChip(selected = index in selected, onClick = { onToggle(index) }, label = { Text(label) })
            }
        }
    }
}

@Composable private fun StatusDot(status: TopicStatus) {
    val color = when (status) {
        TopicStatus.TODO -> MaterialTheme.colorScheme.outline
        TopicStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
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

@Composable
private fun SyllabusFullImportCard(state: StudyPlannerUiState, viewModel: StudyPlannerViewModel) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mimeTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
    )
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val part = withContext(Dispatchers.IO) { buildSyllabusUploadPart(context, uri) }
            if (part == null) {
                viewModel.setError("Unable to read selected file.")
                return@launch
            }
            viewModel.importSyllabusFile(part.first, part.second)
        }
    }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        text = state.syllabusImportDraft.trim()
        viewModel.clearSyllabusImportDraft()
    }
    val parsed = remember(text) { parseBulkSubjectsFromTxt(text) }
    val groups = parsed.getOrNull()
    val topicCount = groups?.let { countBulkSubjectsTopics(it) } ?: 0
    val chapterCount = groups?.let { countBulkSubjectsChapters(it) } ?: 0
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Import syllabus (TXT or file)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Use lines starting with - for subject, _ for chapter, > for topic (same as the website).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { filePicker.launch(mimeTypes) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import File")
            }
            state.syllabusImportFileName?.let { name ->
                Text("Imported: $name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                text,
                { text = it },
                label = { Text("Paste structured syllabus") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )
            when {
                text.isBlank() -> Unit
                parsed.isSuccess && groups != null ->
                    Text("$topicCount topics / $chapterCount chapters / ${groups.size} subjects", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else ->
                    Text(parsed.exceptionOrNull()?.message ?: "Invalid format", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { viewModel.importFullSyllabusFromTxt(text); text = "" },
                enabled = chapterCount > 0 && parsed.isSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Import to plan")
            }
        }
    }
}

private fun buildSyllabusUploadPart(context: Context, uri: Uri): Pair<MultipartBody.Part, String>? {
    val resolver = context.contentResolver
    val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
    val name = rawName?.takeIf { it.isNotBlank() } ?: "syllabus.pdf"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

    // The Express backend (`SAFAR/server/routes/syllabus-import.ts`) rejects anything outside
    // the allow-list { application/pdf, application/vnd...wordprocessingml.document, text/plain }.
    // Some Storage Access Framework providers report `null` or `application/octet-stream`, which
    // would fail that check, so resolve the MIME from the file extension as a fallback.
    val resolverMime = resolver.getType(uri)?.lowercase(Locale.US)
    val extensionMime = when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt" -> "text/plain"
        else -> null
    }
    val mimeType = when {
        !resolverMime.isNullOrBlank() && resolverMime != "application/octet-stream" -> resolverMime
        extensionMime != null -> extensionMime
        else -> "application/octet-stream"
    }

    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    val part = MultipartBody.Part.createFormData("file", name, body)
    return part to name
}

@Composable
private fun CalendarDayChip(
    dateIso: String,
    items: List<CalendarTopicItem>,
    selected: Boolean,
    isToday: Boolean,
    isOff: Boolean,
    dense: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(54.dp),
) {
    val todayK = todayKey()
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val overdue = if (dateIso < todayK) items.count { it.status != TopicStatus.DONE } else 0
    val dayNum = LocalDate.parse(dateIso).dayOfMonth.toString()
    val scheme = MaterialTheme.colorScheme
    val dayFont = if (dense) 12.sp else 15.sp
    val metaFont = if (dense) 7.sp else 8.sp
    val offFont = if (dense) 7.sp else 8.sp
    val padH = if (dense) 2.dp else 4.dp
    val padV = if (dense) 4.dp else 6.dp
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = when {
                    isToday -> 2.dp
                    selected -> 1.5.dp
                    else -> 1.dp
                },
                color = when {
                    isToday -> scheme.primary
                    selected -> scheme.primary.copy(alpha = 0.75f)
                    else -> scheme.outline.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(14.dp),
            )
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (dense) 1.dp else 2.dp),
    ) {
        Text(dayNum, fontWeight = FontWeight.Bold, fontSize = dayFont, maxLines = 1)
        if (isToday) {
            Box(Modifier.size(if (dense) 4.dp else 6.dp).clip(CircleShape).background(scheme.primary))
        }
        if (isOff) {
            Text("Off", fontSize = offFont, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (planned > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (dense) 2.dp else 3.dp)) {
                Text("${planned}P", fontSize = metaFont, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("${done}D", fontSize = metaFont, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                if (overdue > 0) {
                    Text("${overdue}O", fontSize = metaFont, color = scheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedDayLogSheet(
    dateIso: String,
    plan: StudyPlan,
    items: List<CalendarTopicItem>,
    viewModel: StudyPlannerViewModel,
    onDismiss: () -> Unit,
) {
    val todayK = todayKey()
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val missed = if (dateIso < todayK) items.count { it.status != TopicStatus.DONE } else 0
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Selected Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(readableDate(dateIso), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Planned $planned", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Done $done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                Text("Missed $missed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider()
            if (items.isEmpty()) {
                Text(
                    "No modules scheduled for this day.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    PlannerSurface {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.topicName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(plannerTopicStatusDisplayLabel(item.status), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("${item.subjectName} · ${item.chapterName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = { viewModel.updateTopic(item.topicId, status = TopicStatus.DONE) }) { Text("Done") }
                                TextButton(onClick = { viewModel.updateTopic(item.topicId, status = TopicStatus.REVISION_NEEDED) }) { Text("Revision") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(
                                    onClick = {
                                        val next = findNextAvailablePlannedDateIso(dateIso, plan.offDays)
                                        viewModel.updateTopic(item.topicId, plannedDate = next)
                                    },
                                ) {
                                    Text("Move date")
                                }
                                TextButton(onClick = { viewModel.updateTopic(item.topicId, plannedDate = "") }) { Text("Remove date") }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Button(
                    onClick = {
                        val next = findNextAvailablePlannedDateIso(dateIso, plan.offDays)
                        viewModel.moveTopicsToDate(items.map { it.topicId }, next)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Move all to next available day")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.clearTopicDates(items.map { it.topicId })
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Clear this day")
                }
            }
        }
    }
}

@Composable
private fun InsightStatusBadge(status: InsightTrackStatus) {
    val (label, color) = when (status) {
        InsightTrackStatus.ON_TRACK -> "On track" to Color(0xFF16A34A)
        InsightTrackStatus.AT_RISK -> "At risk" to Color(0xFFF59E0B)
        InsightTrackStatus.BEHIND -> "Behind" to Color(0xFFDC2626)
        InsightTrackStatus.AHEAD -> "Ahead" to Color(0xFF2563EB)
        InsightTrackStatus.NEEDS_DATA -> "Set exam date" to MaterialTheme.colorScheme.outline
    }
    Surface(shape = RoundedCornerShape(99.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            label.uppercase(Locale.US),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun MetricMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
