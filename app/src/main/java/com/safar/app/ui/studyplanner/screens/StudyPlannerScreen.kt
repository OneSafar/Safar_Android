package com.safar.app.ui.studyplanner.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.safar.app.util.bounceClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.R
import com.safar.app.BuildConfig
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.data.remote.api.StructuredChapter
import com.safar.app.data.remote.api.StructuredSubject
import com.safar.app.data.remote.api.StructuredSyllabusPreview
import com.safar.app.data.remote.api.SyllabusStats
import com.safar.app.domain.model.studyplanner.CalendarMap
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.PremiumReason
import com.safar.app.domain.model.studyplanner.CalendarTopicItem
import com.safar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.PlanProgress
import com.safar.app.domain.model.studyplanner.StudyChapter
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.StudySubject
import com.safar.app.domain.model.studyplanner.StudyTopic
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.isLightBackground
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.studyplanner.StudyPlannerUiState
import com.safar.app.ui.studyplanner.StudyPlannerViewModel
import com.safar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safar.app.ui.studyplanner.components.PlannerExamDateField
import com.safar.app.ui.studyplanner.components.chapterHierarchyBrush
import com.safar.app.ui.studyplanner.components.subjectHeaderBrush
import com.safar.app.ui.studyplanner.components.subjectMeterBrush
import com.safar.app.ui.studyplanner.components.topicHierarchyBrush
import com.safar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safar.app.ui.studyplanner.logic.*
import com.safar.app.ui.components.PlanCardSkeleton
import com.safar.app.ui.components.SafarInlineRefreshIndicator
import com.safar.app.ui.components.SafarPullRefreshBox
import com.safar.app.ui.components.PlanCardSkeleton
import com.safar.app.ui.components.SafarInlineRefreshIndicator
import com.safar.app.ui.components.SafarPullRefreshBox
import com.safar.app.ui.studyplanner.plan.PlanTabScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val plannerTopicStatusFilterChips = listOf(
    TopicStatus.TODO,
    TopicStatus.DONE,
)

private val plannerTopicStatusSheetChips = listOf(
    TopicStatus.TODO,
    TopicStatus.IN_PROGRESS,
    TopicStatus.DONE,
    TopicStatus.REVISION_NEEDED,
)

internal fun plannerTopicStatusDisplayLabel(status: TopicStatus): String =
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

@Immutable
private data class StudyPlannerChromeState(
    val selectedPlan: StudyPlan? = null,
    val section: PlannerSection = PlannerSection.PLAN,
    val loading: Boolean = false,
    val mutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val premiumReason: PremiumReason? = null,
)

@Immutable
private data class StudyPlansListState(
    val plans: List<StudyPlan> = emptyList(),
    val templates: List<ExamTemplateSummary> = emptyList(),
    val loading: Boolean = false,
)

@Immutable
private data class StudyPlannerDetailState(
    val calendar: CalendarMap = emptyMap(),
    val analytics: PlannerAnalytics? = null,
    val syllabusImportDraft: String = "",
    val syllabusImportFileName: String? = null,
    val isImporting: Boolean = false,
    val importStatus: String? = null,
    val importError: String? = null,
    val importResultSummary: String? = null,
    val rawSyllabusText: String = "",
    val isStructuringSyllabus: Boolean = false,
    val structureError: String? = null,
    val structuredPreview: StructuredSyllabusPreview? = null,
    val isImportingStructuredSyllabus: Boolean = false,
    val structuredImportError: String? = null,
    val structuredImportSuccessMessage: String? = null,
    val hydrateWarning: String? = null,
)

@Immutable
private data class StudyPlannerHomeTarget(
    val section: PlannerSection,
    val selectedPlanId: String?,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
    val chromeState by remember(viewModel) {
        viewModel.uiState
            .map { state ->
                StudyPlannerChromeState(
                    selectedPlan = state.selectedPlan,
                    section = state.section,
                    loading = state.loading,
                    mutating = state.mutating,
                    error = state.error,
                    message = state.message,
                    premiumReason = state.premiumReason,
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(StudyPlannerChromeState())
    val plansState by remember(viewModel) {
        viewModel.uiState
            .map { state ->
                StudyPlansListState(
                    plans = state.plans,
                    templates = state.templates,
                    loading = state.loading,
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(StudyPlansListState())
    val detailState by remember(viewModel) {
        viewModel.uiState
            .map { state ->
                StudyPlannerDetailState(
                    calendar = state.calendar,
                    analytics = state.analytics,
                    syllabusImportDraft = state.syllabusImportDraft,
                    syllabusImportFileName = state.syllabusImportFileName,
                    isImporting = state.isImporting,
                    importStatus = state.importStatus,
                    importError = state.importError,
                    importResultSummary = state.importResultSummary,
                    rawSyllabusText = state.rawSyllabusText,
                    isStructuringSyllabus = state.isStructuringSyllabus,
                    structureError = state.structureError,
                    structuredPreview = state.structuredPreview,
                    isImportingStructuredSyllabus = state.isImportingStructuredSyllabus,
                    structuredImportError = state.structuredImportError,
                    structuredImportSuccessMessage = state.structuredImportSuccessMessage,
                    hydrateWarning = state.hydrateWarning,
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(StudyPlannerDetailState())
    val actions: PlannerActions = viewModel
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(chromeState.error, chromeState.message, detailState.hydrateWarning, detailState.importError, detailState.importResultSummary, detailState.importStatus, detailState.structureError, detailState.structuredImportError, detailState.structuredImportSuccessMessage) {
        chromeState.error?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        chromeState.message?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        detailState.hydrateWarning?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        detailState.importStatus?.let { snackbar.showSnackbar(it) }
        detailState.importResultSummary?.let { snackbar.showSnackbar(it) }
        detailState.importError?.let { snackbar.showSnackbar(it) }
        detailState.structureError?.let { snackbar.showSnackbar(it) }
        detailState.structuredImportError?.let { snackbar.showSnackbar(it) }
        detailState.structuredImportSuccessMessage?.let { snackbar.showSnackbar(it) }
    }

    val premiumReason = chromeState.premiumReason
    if (premiumReason != null) {
        PremiumGateSheet(
            reason = premiumReason,
            onDismiss = actions::clearTransient,
            onUpgrade = actions::upgradePlan,
        )
    }

    val drawerTitle = when {
        chromeState.selectedPlan != null &&
            chromeState.section != PlannerSection.YOUR_EXAMS &&
            chromeState.section != PlannerSection.SYLLABUS ->
            chromeState.section.label
        chromeState.section == PlannerSection.YOUR_EXAMS -> PlannerSection.YOUR_EXAMS.label
        else -> "Study Planner"
    }
    val drawerSubtitle: String? = null

    SafarDrawerScaffold(
        title = drawerTitle,
        subtitle = drawerSubtitle,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
    ) { padding ->
        Scaffold(
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
            ),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (chromeState.selectedPlan != null) {
                    PlannerBottomBar(selected = chromeState.section, onSelect = { section ->
                        val activePlan = chromeState.selectedPlan
                        if (section == PlannerSection.SYLLABUS && activePlan != null) {
                            onNavigate(Routes.ROUTE_SYLLABUS_SUBJECTS.replace("{planId}", activePlan.id))
                        } else {
                            actions.setSection(section)
                        }
                    })
                }
            },
            floatingActionButton = {
                if (chromeState.selectedPlan != null && chromeState.section == PlannerSection.PLAN) {
                    androidx.compose.material3.ExtendedFloatingActionButton(
                        onClick = { actions.checkIn() },
                        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.CheckCircle, contentDescription = null) },
                        text = { androidx.compose.material3.Text("Check In") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = StudyPlannerHomeTarget(
                            section = chromeState.section,
                            selectedPlanId = chromeState.selectedPlan?.id,
                        ),
                        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                        label = "StudyPlannerHome",
                    ) { target ->
                        val targetPlan = remember(target.selectedPlanId, chromeState.selectedPlan, plansState.plans) {
                            when (target.selectedPlanId) {
                                null -> null
                                chromeState.selectedPlan?.id -> chromeState.selectedPlan
                                else -> plansState.plans.firstOrNull { it.id == target.selectedPlanId }
                            }
                        }
                        PlannerHome(
                            chromeState = chromeState.copy(
                                section = target.section,
                                selectedPlan = targetPlan,
                            ),
                            plansState = plansState,
                            detailState = detailState,
                            actions = actions,
                            onNavigate = onNavigate,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                        )
                    }
                }
                val hasCachedContent = plansState.plans.isNotEmpty() || chromeState.selectedPlan != null
                SafarInlineRefreshIndicator(
                    isRefreshing = chromeState.loading && hasCachedContent,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StudyPlansScreen(
    state: StudyPlansListState,
    importState: StudyPlannerDetailState,
    actions: PlannerActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StudyPlan?>(null) }
    val quickStartState = remember(state.templates, state.loading, importState.isImporting, importState.importStatus, importState.importError) {
        StudyPlannerUiState(
            templates = state.templates,
            loading = state.loading,
            isImporting = importState.isImporting,
            importStatus = importState.importStatus,
            importError = importState.importError,
        )
    }

    LaunchedEffect(importState.isImporting, importState.importResultSummary, importState.importError) {
        if (showCreate && !importState.isImporting && (importState.importResultSummary != null || importState.importError != null)) {
            showCreate = false
        }
    }

    if (showCreate) {
        QuickStartSheet(
            state = quickStartState,
            actions = actions,
            onDismiss = { if (!quickStartState.isImporting) showCreate = false },
        )
    }
    pendingDelete?.let { plan ->
        ConfirmActionDialog(
            title = "Delete plan?",
            body = "This will delete ${plan.title} and its syllabus.",
            onDismiss = { pendingDelete = null },
            onConfirm = { actions.deletePlan(plan.id); pendingDelete = null },
        )
    }

    SafarPullRefreshBox(
        isRefreshing = state.loading && state.plans.isNotEmpty(),
        onRefresh = { actions.refreshPlans() },
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Exams",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = { showCreate = true },
                        shape = ButtonDefaults.shape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New plan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (state.loading && state.plans.isEmpty()) {
                items(3) {
                    PlanCardSkeleton(modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            if (state.plans.isEmpty() && !state.loading) {
                item {
                    PlannerEmptyState(
                        title = "No study plan yet",
                        body = "Use a template or make your own plan.",
                        action = "Create Plan",
                        onAction = { showCreate = true },
                    )
                }
            } else if (state.plans.isNotEmpty()) {
                // Promotional Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreate = true },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF7C8EFF), Color(0xFF2D449E))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Focus: Plan your success today!",
                                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Select an exam to begin.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f)),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), MaterialTheme.shapes.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // First plan rendered full width
                val firstPlan = state.plans.first()
                item {
                    PlanCardSimplified(
                        plan = firstPlan,
                        onOpen = { actions.openPlan(firstPlan.id) },
                        onDelete = { pendingDelete = firstPlan },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }

                // Subsequent plans rendered in a 2-column grid
                val remainingPlans = state.plans.drop(1)
                val pairs = remainingPlans.chunked(2)
                items(pairs, key = { pair -> pair.map { it.id }.joinToString("-") }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pair.forEach { plan ->
                            Box(modifier = Modifier.weight(1f)) {
                                PlanCardCompact(
                                    plan = plan,
                                    onOpen = { actions.openPlan(plan.id) },
                                    onDelete = { pendingDelete = plan },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerSectionHeader(
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (action != null) action()
    }
}

@Composable
private fun PlannerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) { content() }
}

@Composable
private fun PlannerEmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    PlannerCard {
        Column(
            Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(26.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 40.dp), shape = ButtonDefaults.shape) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(action)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlanCardSimplified(
    plan: StudyPlan,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val progress = plan.rollup()
    val examDays = daysUntil(plan.examDate)
    var menuExpanded by remember { mutableStateOf(false) }
    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = "study-plan-card:${plan.id}"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
    Card(
        modifier = sharedModifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val badgeBrush = when {
                        examDays == null -> Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9)))
                        examDays < 0 -> Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
                        examDays <= 7 -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                        examDays <= 13 -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316)))
                        examDays <= 14 -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFFBBF24)))
                        else -> Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF10B981)))
                    }
                    val badgeText = examBadgeLabel(examDays)
                    Box(
                        modifier = Modifier
                            .background(badgeBrush, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Plan actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
            Text(
                text = "${plan.subjectCount ?: plan.subjects.size} subjects / ${progress.totalTopics} topics",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = { progress.completionPercent / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.completionPercent}% complete",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Open",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpen)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlanCardCompact(
    plan: StudyPlan,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val progress = plan.rollup()
    val examDays = daysUntil(plan.examDate)
    var menuExpanded by remember { mutableStateOf(false) }
    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = "study-plan-card:${plan.id}"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
    Card(
        modifier = sharedModifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Plan actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            val badgeBrush = when {
                examDays == null -> Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9)))
                examDays < 0 -> Brush.horizontalGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
                examDays <= 7 -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                examDays <= 13 -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316)))
                examDays <= 14 -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFFBBF24)))
                else -> Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF10B981)))
            }
            val badgeText = examBadgeLabel(examDays)
            Box(
                modifier = Modifier
                    .background(badgeBrush, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${plan.subjectCount ?: plan.subjects.size} subjects",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.completionPercent / 100f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = "${progress.completionPercent}% complete",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickStartSheet(state: StudyPlannerUiState, actions: PlannerActions, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf("template") }
    var templateId by remember(state.templates) { mutableStateOf(state.templates.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var examType by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableStateOf("3") }
    var pasteSyllabus by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val offDays = remember { mutableStateOf(setOf<Int>()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PlannerSectionHeader(
                title = "Create plan",
                subtitle = "Select exam and exam date.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "template", onClick = { mode = "template" }, label = { Text("Template") })
                FilterChip(selected = mode == "custom", onClick = { mode = "custom" }, label = { Text("Custom") })
            }
            if (mode == "template") {
                Text("Exam template", fontWeight = FontWeight.SemiBold)
                state.templates.take(8).forEach { template ->
                    PlannerActionRow(
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
                OutlinedButton(onClick = { showAdvanced = !showAdvanced }, shape = ButtonDefaults.outlinedShape) {
                    Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Paste syllabus (optional)")
                }
                if (showAdvanced) {
                    OutlinedTextField(value = pasteSyllabus, onValueChange = { pasteSyllabus = it }, label = { Text("Paste syllabus") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text("${parseBulkSyllabus(pasteSyllabus).sumOf { it.second.size }} topics detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Plan title") }, modifier = Modifier.fillMaxWidth())
            PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it })
            OutlinedTextField(value = dailyGoal, onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Topics per day") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { showAdvanced = !showAdvanced }, shape = ButtonDefaults.outlinedShape) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showAdvanced) "Hide weekly off days" else "Weekly off days")
            }
            if (showAdvanced) {
                OffDayPicker(selected = offDays.value, onToggle = { day ->
                    offDays.value = if (day in offDays.value) offDays.value - day else offDays.value + day
                })
            }
            if (state.isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = state.importStatus ?: "Importing syllabus...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            state.importError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (mode == "custom" && pasteSyllabus.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val goal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3
                            actions.createPlan(
                                title = title.ifBlank { "Study Plan" },
                                examType = examType.ifBlank { null },
                                examDate = examDate.ifBlank { null },
                                dailyGoal = goal,
                                offDays = offDays.value.toList(),
                                syllabusText = pasteSyllabus,
                            )
                        },
                        enabled = !state.isImporting,
                    ) { Text("Retry syllabus import") }
                }
            }
            Button(
                onClick = {
                    val goal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3
                    val hasSyllabusImport = mode == "custom" && pasteSyllabus.isNotBlank()
                    if (mode == "template" && templateId.isNotBlank()) {
                        actions.createFromTemplate(templateId, title.ifBlank { "Study Plan" }, examDate.ifBlank { null }, goal, offDays.value.toList())
                        onDismiss()
                    } else {
                        actions.createPlan(
                            title = title.ifBlank { "Study Plan" },
                            examType = examType.ifBlank { null },
                            examDate = examDate.ifBlank { null },
                            dailyGoal = goal,
                            offDays = offDays.value.toList(),
                            syllabusText = pasteSyllabus,
                        )
                        if (!hasSyllabusImport) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = ButtonDefaults.shape,
                enabled = !state.isImporting,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Plan")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlannerActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PlannerHome(
    chromeState: StudyPlannerChromeState,
    plansState: StudyPlansListState,
    detailState: StudyPlannerDetailState,
    actions: PlannerActions,
    onNavigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val plan = chromeState.selectedPlan
    val landingState = remember(plansState.templates, chromeState.mutating) {
        StudyPlannerUiState(
            templates = plansState.templates,
            mutating = chromeState.mutating,
        )
    }
    val activePlanState = remember(chromeState, plansState, detailState) {
        StudyPlannerUiState(
            plans = plansState.plans,
            templates = plansState.templates,
            selectedPlan = chromeState.selectedPlan,
            calendar = detailState.calendar,
            analytics = detailState.analytics,
            section = chromeState.section,
            loading = chromeState.loading,
            mutating = chromeState.mutating,
            error = chromeState.error,
            message = chromeState.message,
            premiumReason = chromeState.premiumReason,
            syllabusImportDraft = detailState.syllabusImportDraft,
            syllabusImportFileName = detailState.syllabusImportFileName,
            isImporting = detailState.isImporting,
            importStatus = detailState.importStatus,
            importError = detailState.importError,
            importResultSummary = detailState.importResultSummary,
            rawSyllabusText = detailState.rawSyllabusText,
            isStructuringSyllabus = detailState.isStructuringSyllabus,
            structureError = detailState.structureError,
            structuredPreview = detailState.structuredPreview,
            isImportingStructuredSyllabus = detailState.isImportingStructuredSyllabus,
            structuredImportError = detailState.structuredImportError,
            structuredImportSuccessMessage = detailState.structuredImportSuccessMessage,
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (plan != null && chromeState.section != PlannerSection.INSIGHTS) {
            SelectedExamStrip(
                plan = plan,
                onChangeExam = { actions.setSection(PlannerSection.YOUR_EXAMS) },
            )
        }
        Box(Modifier.weight(1f)) {
            when (chromeState.section) {
                PlannerSection.YOUR_EXAMS -> StudyPlansScreen(
                    state = plansState,
                    importState = detailState,
                    actions = actions,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                PlannerSection.PLAN -> if (plan != null) {
                    PlanTabScreen(
                        plan = plan,
                        actions = actions,
                        onNavigate = onNavigate,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                } else {
                    PlannerExamPickerLanding(state = landingState, actions = actions, onOpenExams = { actions.setSection(PlannerSection.YOUR_EXAMS) })
                }
                PlannerSection.SYLLABUS -> if (plan != null) {
                    LaunchedEffect(plan.id) {
                        onNavigate(Routes.ROUTE_SYLLABUS_SUBJECTS.replace("{planId}", plan.id))
                    }
                    Box(Modifier.fillMaxSize())
                } else {
                    PlannerExamPickerLanding(state = landingState, actions = actions, onOpenExams = { actions.setSection(PlannerSection.YOUR_EXAMS) })
                }
                PlannerSection.CALENDAR -> if (plan != null) CalendarTab(plan, activePlanState, actions) else PlannerExamPickerLanding(state = landingState, actions = actions, onOpenExams = { actions.setSection(PlannerSection.YOUR_EXAMS) })
                PlannerSection.INSIGHTS -> if (plan != null) InsightsTab(plan, activePlanState, actions) else PlannerExamPickerLanding(state = landingState, actions = actions, onOpenExams = { actions.setSection(PlannerSection.YOUR_EXAMS) })
            }
        }
    }
}

@Composable
private fun SelectedExamStrip(
    plan: StudyPlan,
    onChangeExam: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                Text(
                    text = plan.title.ifBlank { plan.examType ?: "Selected exam" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = plan.examType?.takeIf { it.isNotBlank() } ?: "Selected exam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onChangeExam) {
                Text("Change")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerExamPickerLanding(
    state: StudyPlannerUiState,
    actions: PlannerActions,
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
    var showSetupSheet by remember { mutableStateOf(false) }

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

    if (showSetupSheet && (useCustomPlan || selectedTemplate != null)) {
        ModalBottomSheet(
            onDismissRequest = { showSetupSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
            ) {
                if (useCustomPlan) {
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
                        confirmLabel = if (state.mutating) "Creating..." else "Create Plan",
                        onConfirm = {
                            if (examDate.isBlank()) {
                                landingError = "Please select exam date."
                                return@PlannerQuickCreateForm
                            }
                            landingError = ""
                            actions.createPlan(
                                title.ifBlank { "Study Plan" },
                                examType.ifBlank { null },
                                examDate,
                                dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                                offDays.value.toList(),
                            )
                            showSetupSheet = false
                        },
                        isDateError = landingError.isNotBlank() && examDate.isBlank(),
                    )
                } else if (selectedTemplate != null) {
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
                        confirmLabel = if (state.mutating) "Creating..." else "Create Plan",
                        onConfirm = {
                            if (examDate.isBlank()) {
                                landingError = "Please select exam date."
                                return@PlannerQuickCreateForm
                            }
                            landingError = ""
                            actions.createFromTemplateOrLocal(
                                selectedTemplate.id,
                                title.ifBlank { selectedTemplate.title },
                                examDate,
                                dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: selectedTemplate.recommendedDailyGoal,
                                offDays.value.toList(),
                            )
                            showSetupSheet = false
                        },
                        isDateError = landingError.isNotBlank() && examDate.isBlank(),
                    )
                }
                
                if (landingError.isNotBlank()) {
                    Text(
                        landingError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
                Text(
                    "Start Your Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Start,
                )
                Text(
                    "Use a template or make your own plan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
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
                                                showSetupSheet = true
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
                                                showSetupSheet = true
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
                                        showSetupSheet = true
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
                                        showSetupSheet = true
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

        item {
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.Center) {
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
            .heightIn(min = 132.dp)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .border(
                if (selected) 2.5.dp else 1.dp,
                if (selected) Color.White else Color.White.copy(alpha = 0.18f),
                MaterialTheme.shapes.large,
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Surface(color = Color(0xFF2D4B8C).copy(alpha = 0.88f), shape = CircleShape) {
                    Text(
                        template.categoryLabel,
                        color = Color(0xFFD6E4FF),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Text(
                template.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                template.description ?: "Pre-loaded syllabus template",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${template.estimatedTopics} topics",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("•", color = Color.White.copy(alpha = 0.7f))
                Text(
                    template.examBody,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("•", color = Color.White.copy(alpha = 0.7f))
                Text(
                    "${template.recommendedDailyGoal}/day",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
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
    val cardShape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(cardShape)
            .background(Color(0xFF111827))
            .drawBehind {
                val stroke = Stroke(
                    width = if (selected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
                )
                val cornerSizePx = cardShape.topStart.toPx(size, this)
                drawRoundRect(
                    color = if (selected) Color(0xFF93C5FD) else Color(0xFF64748B),
                    style = stroke,
                    cornerRadius = CornerRadius(cornerSizePx, cornerSizePx),
                )
            }
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Build your own plan from scratch. Paste your syllabus or set it up manually.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Any exam • Your syllabus",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodySmall,
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
            Text("Set Up Plan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Plan title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = examType,
                onValueChange = onExamTypeChange,
                label = { Text("Exam name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Exam date", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = if (isDateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = ButtonDefaults.shape,
            ) {
                Text(confirmLabel)
            }
        }
    }
}


@Composable
internal fun AddTopicsToPlanButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add topics to plan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlannerTopicDetailSheet(ref: TopicRef, openNonce: Int, actions: PlannerActions, onDismiss: () -> Unit) {
    var name by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.name) }
    var notes by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.notes.orEmpty()) }
    var date by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.plannedDate?.take(10).orEmpty()) }
    var status by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.status) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ref.chapter.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plannerTopicStatusSheetChips.forEach { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }
            OutlinedTextField(date, { date = it }, label = { Text("Planned date") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { actions.deleteTopic(ref.topic.id); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text("Delete")
                }
                Button(
                    onClick = {
                        actions.updateTopic(ref.topic.id, status, name, date.ifBlank { "" }, notes)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BulkAddSheet(
    target: Pair<StudySubject, StudyChapter>,
    state: StudyPlannerUiState,
    actions: PlannerActions,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val count by produceState(0, text) {
        value = withContext(Dispatchers.Default) {
            parseBulkSyllabus(text).flatMap { it.second }.size
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Many Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${target.first.name} • ${target.second.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Paste Text") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(16.dp)) })
            }
            OutlinedTextField(text, { text = it }, label = { Text("Paste topics or chapter lines") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            Text("$count topics detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { actions.bulkAdd(target.first.id, target.second.id, text); onDismiss() }, modifier = Modifier.fillMaxWidth(), enabled = count > 0) { Text("Add Topics") }
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
    onRenameSubject: () -> Unit,
    onDeleteSubject: () -> Unit,
    onAddTopic: (StudyChapter) -> Unit,
    onRenameChapter: (StudyChapter) -> Unit,
    onBulkAdd: (StudyChapter) -> Unit,
    onDeleteChapter: (StudyChapter) -> Unit,
    onTopic: (TopicRef) -> Unit,
    onMarkDone: (String) -> Unit,
) {
    var subjectExpanded by remember(subject.id, query, status) { mutableStateOf(query.isNotBlank() || status != null) }
    val chapterExpanded = remember(subject.id) { mutableStateMapOf<String, Boolean>() }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    PlannerSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val subjectHeaderShape = MaterialTheme.shapes.medium
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
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${subject.chapters.size} chapters • ${subject.percentDone()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onAddChapter) { Icon(Icons.Default.Add, contentDescription = "Add chapter") }
                Box {
                    IconButton(onClick = { subjectMenuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Subject actions") }
                    DropdownMenu(expanded = subjectMenuExpanded, onDismissRequest = { subjectMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                subjectMenuExpanded = false
                                onRenameSubject()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                subjectMenuExpanded = false
                                onDeleteSubject()
                            },
                        )
                    }
                }
            }
            if (subjectExpanded) {
                subject.chapters.forEach { chapter ->
                    val chExpanded = chapterExpanded[chapter.id] ?: false
                    var chapterMenuExpanded by remember(chapter.id) { mutableStateOf(false) }
                    val isPlaceholderChapter = isBulkPlaceholderChapter(chapter)
                    val chapterShape = MaterialTheme.shapes.medium
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            IconButton(onClick = { onAddTopic(chapter) }) { Icon(Icons.Default.Add, contentDescription = "Add topic") }
                            Box {
                                IconButton(onClick = { chapterMenuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Chapter actions") }
                                DropdownMenu(expanded = chapterMenuExpanded, onDismissRequest = { chapterMenuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Bulk add") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            chapterMenuExpanded = false
                                            onBulkAdd(chapter)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            chapterMenuExpanded = false
                                            onRenameChapter(chapter)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            chapterMenuExpanded = false
                                            onDeleteChapter(chapter)
                                        },
                                    )
                                }
                            }
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
                            if (topics.isEmpty()) Text("No topics found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(plan.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${plan.subjectCount ?: plan.subjects.size} subjects • ${progress.totalTopics} topics",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
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
                            Text(plan.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${plan.subjectCount ?: plan.subjects.size} subjects • ${progress.totalTopics} topics",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ExamDaysCountdownBadge(days = examDays)
                        IconButton(onClick = onDelete) { Icon(Icons.Default.MoreVert, contentDescription = "Plan actions") }
                    }
                }
            }
            LinearProgressIndicator(progress = { progress.completionPercent / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
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
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).then(rowBg).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(ref.topic.status)
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(
                ref.topic.name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${ref.subject.name} • ${ref.chapter.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(
            checked = ref.topic.status == TopicStatus.DONE,
            onCheckedChange = { checked ->
                if (checked) onDone()
            },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF16A34A),
                checkmarkColor = Color.White,
            ),
        )
    }
}

@Composable
internal fun PlannerBottomBar(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    val sections = listOf(
        PlannerSection.PLAN,
        PlannerSection.SYLLABUS,
        PlannerSection.CALENDAR,
        PlannerSection.INSIGHTS,
    )
    val icons = mapOf(
        PlannerSection.PLAN to Icons.Default.Today,
        PlannerSection.SYLLABUS to Icons.AutoMirrored.Filled.FactCheck,
        PlannerSection.CALENDAR to Icons.Default.CalendarMonth,
        PlannerSection.INSIGHTS to Icons.Default.Insights,
    )
    val scheme = MaterialTheme.colorScheme

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = scheme.surface,
        tonalElevation = 4.dp,
    ) {
        sections.forEach { section ->
            val isSelected = selected == section
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(section) },
                icon = {
                    Icon(
                        imageVector = icons.getValue(section),
                        contentDescription = section.label,
                    )
                },
                label = {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onSecondaryContainer,
                    selectedTextColor = scheme.onSurface,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                    indicatorColor = scheme.secondaryContainer,
                ),
            )
        }
    }
}

@Composable
internal fun PlannerExportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.outlinedShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Export", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
            plannerExamSubtitle(plan.examDate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text("complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape))
            Text(main, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun SetupGuideCard(plan: StudyPlan, actions: PlannerActions) {
    val hasDate = !plan.examDate.isNullOrBlank()
    val hasTopics = plan.flattenTopics().isNotEmpty()
    PlannerSurface {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Setup Guide", fontWeight = FontWeight.Bold)
            GuideStep("Set exam date", hasDate) { actions.setSection(PlannerSection.PLAN) }
            GuideStep("Add topics", hasTopics) { actions.setSection(PlannerSection.SYLLABUS) }
            GuideStep("Build schedule", plan.flattenTopics().any { !it.topic.plannedDate.isNullOrBlank() }) { actions.autoDistribute(false, true) }
            GuideStep("Review calendar", false) { actions.setSection(PlannerSection.CALENDAR) }
        }
    }
}

@Composable private fun GuideStep(label: String, done: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (done) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(if (done) "Done" else "Next", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun PlannerActionButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), shape = ButtonDefaults.shape) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).widthIn(min = 0.dp),
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).widthIn(min = 0.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier) {
    PlannerSurface(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
        Text("My Rest Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { index, label ->
                FilterChip(selected = index in selected, onClick = { onToggle(index) }, label = { Text(label) })
            }
        }
    }
}

@Composable internal fun StatusDot(status: TopicStatus) {
    val color = when (status) {
        TopicStatus.TODO -> MaterialTheme.colorScheme.outline
        TopicStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TopicStatus.DONE -> MaterialTheme.colorScheme.tertiary
        TopicStatus.REVISION_NEEDED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable internal fun PlannerSurface(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) { content() }
}

@Composable private fun PlannerRow(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null)
    }
}

@Composable internal fun EmptyPlannerCard(title: String, body: String, action: String, onAction: () -> Unit) {
    PlannerSurface {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onAction, shape = ButtonDefaults.shape) { Text(action) }
        }
    }
}

@Composable internal fun TextInputDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = text.trim().length >= 2, onClick = { onConfirm(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable internal fun ConfirmActionDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyllabusFullImportCard(state: StudyPlannerUiState, actions: PlannerActions) {
    var text by remember { mutableStateOf(state.rawSyllabusText) }
    val aiImportEnabled = BuildConfig.AI_SYLLABUS_IMPORT_ENABLED
    var mode by remember(aiImportEnabled) { mutableStateOf(if (aiImportEnabled) "ai" else "manual") }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        text = state.syllabusImportDraft.trim()
        mode = "manual"
        actions.clearSyllabusImportDraft()
    }
    val parsed = remember(text) { parseBulkSubjectsFromTxt(text) }
    val groups = parsed.getOrNull()
    val topicCount = groups?.let { countBulkSubjectsTopics(it) } ?: 0
    val chapterCount = groups?.let { countBulkSubjectsChapters(it) } ?: 0
    val preview = state.structuredPreview
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, scheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Import Syllabus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            if (aiImportEnabled) {
                val selectedTabIndex = if (mode == "ai") 0 else 1
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = mode == "ai",
                        onClick = { mode = "ai" },
                        text = {
                            Text(
                                "AI Import",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selectedContentColor = scheme.primary,
                        unselectedContentColor = scheme.onSurfaceVariant
                    )
                    Tab(
                        selected = mode == "manual",
                        onClick = { mode = "manual" },
                        text = {
                            Text(
                                "Manual Setup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selectedContentColor = scheme.primary,
                        unselectedContentColor = scheme.onSurfaceVariant
                    )
                }
            }

            if (aiImportEnabled && mode == "ai") {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (preview != null) actions.updateStructuredPreview(null)
                    },
                    placeholder = {
                        Text(
                            "Paste your syllabus text...",
                            color = scheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    minLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    shape = OutlinedTextFieldDefaults.shape,
                    enabled = !state.isStructuringSyllabus && !state.isImportingStructuredSyllabus,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )
                Button(
                    onClick = { actions.structureSyllabusPreview(text) },
                    enabled = text.isNotBlank() && !state.isStructuringSyllabus && !state.isImportingStructuredSyllabus,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
                    )
                ) {
                    if (state.isStructuringSyllabus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = scheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Organizing your syllabus...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Structure with AI", style = MaterialTheme.typography.labelLarge)
                    }
                }
                state.structureError?.let {
                    Text("We could not organize this syllabus. You can try again or use manual format.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (preview != null) {
                    StructuredSyllabusPreviewEditor(
                        preview = preview,
                        isImporting = state.isImportingStructuredSyllabus,
                        onPreviewChange = actions::updateStructuredPreview,
                    )
                    Button(
                        onClick = { actions.importStructuredSyllabus() },
                        enabled = preview.subjects.isNotEmpty() && !state.isImportingStructuredSyllabus,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ButtonDefaults.shape,
                    ) {
                        if (state.isImportingStructuredSyllabus) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Adding syllabus to your plan...")
                        } else {
                            Text("Add to Plan")
                        }
                    }
                }
                state.structuredImportError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            } else {
                OutlinedTextField(
                    text,
                    { text = it },
                    placeholder = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Use Manual", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("- Subject, _ Chapter, > Topic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OutlinedTextFieldDefaults.shape,
                )
                when {
                    text.isBlank() -> Unit
                    parsed.isSuccess && groups != null -> Text("$topicCount topics / $chapterCount chapters / ${groups.size} subjects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Text(parsed.exceptionOrNull()?.message ?: "Invalid format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { actions.importFullSyllabusFromTxt(text); text = "" },
                    enabled = chapterCount > 0 && parsed.isSuccess && !state.isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) { Text("Add Manual Format to Plan") }
            }
        }
    }
}

@Composable
private fun SyllabusModeToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    if (outlined && !selected) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
                contentColor = scheme.onSurface,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StructuredSyllabusPreviewEditor(
    preview: StructuredSyllabusPreview,
    isImporting: Boolean,
    onPreviewChange: (StructuredSyllabusPreview?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${preview.stats.subjectCount} subjects � ${preview.stats.chapterCount} chapters � ${preview.stats.topicCount} topics", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (preview.warnings.isNotEmpty()) {
            PlannerSurface {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Warnings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    preview.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (preview.subjects.isEmpty()) {
            Text("No syllabus structure was detected. Edit the text and try again, or use manual format.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        preview.subjects.forEachIndexed { subjectIndex, subject ->
            PlannerSurface {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = subject.name, onValueChange = { onPreviewChange(preview.renameSubject(subjectIndex, it)) }, label = { Text("Subject") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !isImporting)
                        IconButton(onClick = { onPreviewChange(preview.deleteSubject(subjectIndex)) }, enabled = !isImporting) { Icon(Icons.Default.Delete, contentDescription = "Delete subject") }
                    }
                    subject.chapters.forEachIndexed { chapterIndex, chapter ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = chapter.name, onValueChange = { onPreviewChange(preview.renameChapter(subjectIndex, chapterIndex, it)) }, label = { Text("Chapter") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !isImporting)
                                IconButton(onClick = { onPreviewChange(preview.deleteChapter(subjectIndex, chapterIndex)) }, enabled = !isImporting) { Icon(Icons.Default.Delete, contentDescription = "Delete chapter") }
                            }
                            chapter.topics.forEachIndexed { topicIndex, topic ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                                    OutlinedTextField(value = topic, onValueChange = { onPreviewChange(preview.renameTopic(subjectIndex, chapterIndex, topicIndex, it)) }, label = { Text("Topic") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !isImporting)
                                    IconButton(onClick = { onPreviewChange(preview.deleteTopic(subjectIndex, chapterIndex, topicIndex)) }, enabled = !isImporting) { Icon(Icons.Default.Close, contentDescription = "Delete topic") }
                                }
                            }
                            var newTopic by remember(subjectIndex, chapterIndex, preview.stats.topicCount) { mutableStateOf("") }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                                OutlinedTextField(value = newTopic, onValueChange = { newTopic = it }, label = { Text("Add missing topic") }, modifier = Modifier.weight(1f), singleLine = true, enabled = !isImporting)
                                TextButton(onClick = { onPreviewChange(preview.addTopic(subjectIndex, chapterIndex, newTopic)); newTopic = "" }, enabled = newTopic.isNotBlank() && !isImporting) { Text("Add") }
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = { onPreviewChange(null) }, enabled = !isImporting) { Text("Back to raw text") }
    }
}

private fun StructuredSyllabusPreview.withRecomputedStats(): StructuredSyllabusPreview {
    val chapters = subjects.sumOf { it.chapters.size }
    val topics = subjects.sumOf { subject -> subject.chapters.sumOf { it.topics.size } }
    return copy(stats = SyllabusStats(subjects.size, chapters, topics))
}

private fun StructuredSyllabusPreview.renameSubject(subjectIndex: Int, name: String) = copy(subjects = subjects.mapIndexed { index, subject -> if (index == subjectIndex) subject.copy(name = name) else subject }).withRecomputedStats()

private fun StructuredSyllabusPreview.renameChapter(subjectIndex: Int, chapterIndex: Int, name: String) = copy(subjects = subjects.mapIndexed { sIndex, subject -> if (sIndex != subjectIndex) subject else subject.copy(chapters = subject.chapters.mapIndexed { cIndex, chapter -> if (cIndex == chapterIndex) chapter.copy(name = name) else chapter }) }).withRecomputedStats()

private fun StructuredSyllabusPreview.renameTopic(subjectIndex: Int, chapterIndex: Int, topicIndex: Int, name: String) = copy(subjects = subjects.mapIndexed { sIndex, subject -> if (sIndex != subjectIndex) subject else subject.copy(chapters = subject.chapters.mapIndexed { cIndex, chapter -> if (cIndex != chapterIndex) chapter else chapter.copy(topics = chapter.topics.mapIndexed { tIndex, topic -> if (tIndex == topicIndex) name else topic }) }) }).withRecomputedStats()

private fun StructuredSyllabusPreview.deleteSubject(subjectIndex: Int) = copy(subjects = subjects.filterIndexed { index, _ -> index != subjectIndex }).withRecomputedStats()

private fun StructuredSyllabusPreview.deleteChapter(subjectIndex: Int, chapterIndex: Int) = copy(subjects = subjects.mapIndexed { sIndex, subject -> if (sIndex != subjectIndex) subject else subject.copy(chapters = subject.chapters.filterIndexed { cIndex, _ -> cIndex != chapterIndex }) }.filter { it.chapters.isNotEmpty() }).withRecomputedStats()

private fun StructuredSyllabusPreview.deleteTopic(subjectIndex: Int, chapterIndex: Int, topicIndex: Int) = copy(subjects = subjects.mapIndexed { sIndex, subject -> if (sIndex != subjectIndex) subject else subject.copy(chapters = subject.chapters.mapIndexed { cIndex, chapter -> if (cIndex != chapterIndex) chapter else chapter.copy(topics = chapter.topics.filterIndexed { tIndex, _ -> tIndex != topicIndex }) }.filter { it.topics.isNotEmpty() }) }.filter { it.chapters.isNotEmpty() }).withRecomputedStats()

private fun StructuredSyllabusPreview.addTopic(subjectIndex: Int, chapterIndex: Int, name: String): StructuredSyllabusPreview {
    val clean = name.trim()
    if (clean.isBlank()) return this
    return copy(subjects = subjects.mapIndexed { sIndex, subject -> if (sIndex != subjectIndex) subject else subject.copy(chapters = subject.chapters.mapIndexed { cIndex, chapter -> if (cIndex != chapterIndex) chapter else chapter.copy(topics = chapter.topics + clean) }) }).withRecomputedStats()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PremiumGateSheet(reason: PremiumReason, onDismiss: () -> Unit, onUpgrade: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Text(reason.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(reason.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth(), shape = ButtonDefaults.shape) { Text("Unlock Premium") }
            TextButton(onClick = onDismiss) { Text("Not now") }
            Spacer(Modifier.height(18.dp))
        }
    }
}




