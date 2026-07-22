package com.safarparmar.app.ui.studyplanner.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.safarparmar.app.ui.studyplanner.components.PlannerTabAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerRevisionAccent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.safarparmar.app.util.bounceClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.data.remote.api.StructuredChapter
import com.safarparmar.app.data.remote.api.StructuredSubject
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview
import com.safarparmar.app.data.remote.api.SyllabusStats
import com.safarparmar.app.domain.model.Achievement
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.studyplanner.plan.DailyTodoSetupSheet
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.flatCard
import com.safarparmar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
import com.safarparmar.app.ui.studyplanner.plan.StudyStyleOption
import com.safarparmar.app.ui.butterfly.ButterflyTourState
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.studyPlannerTourSteps
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
// ── Liquid Glass design system ──────────────────────────────────────────────
import com.safarparmar.app.ui.glass.MacOSControlActionButton
import com.safarparmar.app.ui.glass.MacOSControlEmptyState
import com.safarparmar.app.ui.glass.MacOSControlIconBadge
import com.safarparmar.app.ui.glass.MacOSExamPlanCard

import com.safarparmar.app.ui.glass.GlassDivider as GlassHDivider

private val plannerTopicStatusFilterChips = listOf(
    TopicStatus.TODO,
    TopicStatus.DONE,
)

private fun syllabusTopicMatchesFilter(topicStatus: TopicStatus, filter: TopicStatus?): Boolean {
    if (filter == null) return true
    return topicStatus == filter
}

@Immutable
private data class StudyPlannerChromeState(
    val selectedPlan: StudyPlan? = null,
    val section: PlannerSection = PlannerSection.PLAN,
    val loading: Boolean = false,
    val mutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val rolloverUndoToken: String? = null,
    val deleteUndoToken: String? = null,
    val finishDayUndoAvailable: Boolean = false,
    val pendingOpenUnscheduledTopics: Boolean = false,
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
    val onboardingCompletedSteps: Set<String> = emptySet(),
    val plannerAchievements: List<Achievement> = emptyList(),
    val preferredStudyStrategy: String = "interleaved",
    val pendingManualSubjectOrder: Boolean = false,
    val activePlanTab: StudyPlannerTab = StudyPlannerTab.TODAY,
)

@Immutable
private data class StudyPlannerHomeTarget(
    val section: PlannerSection,
    val selectedPlanId: String?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StudyPlannerScreen(
    currentRoute: String = Routes.STUDY_PLANNER,
    isDarkTheme: Boolean = false,
    planId: String? = null,
    showDailyTodoSetup: Boolean = false,
    openTab: String? = null,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: StudyPlannerViewModel = hiltViewModel(),
) {
    val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = hiltViewModel()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val canUsePremiumPlannerFeatures = premiumStatus.hasAnyPaidAccess || premiumStatus.canUseStudyPlannerInsights
    val actionsForPlanId: PlannerActions = viewModel
    var dailyTodoSetupVisible by remember(planId, showDailyTodoSetup) {
        mutableStateOf(showDailyTodoSetup)
    }
    androidx.compose.runtime.LaunchedEffect(planId, showDailyTodoSetup, openTab) {
        // A newly-confirmed plan explicitly requests Home + Daily To-Do setup. Other
        // plan-id deep links retain their existing Calendar landing behavior. A
        // revision reminder deep link opens straight on the plan's Revision tab.
        planId?.let {
            actionsForPlanId.openPlan(it)
            when {
                openTab.equals("revision", ignoreCase = true) ->
                    actionsForPlanId.openRevisionTopics()
                showDailyTodoSetup -> actionsForPlanId.setSection(PlannerSection.PLAN)
                else -> actionsForPlanId.setSection(PlannerSection.CALENDAR)
            }
        }
    }
    val initialChromeState = remember(viewModel) {
        val state = viewModel.uiState.value
        StudyPlannerChromeState(
            selectedPlan = state.selectedPlan,
            section = state.section,
            loading = state.loading,
            mutating = state.mutating,
            error = state.error,
            message = state.message,
            rolloverUndoToken = state.rolloverUndoToken,
            deleteUndoToken = state.deleteUndoToken,
            finishDayUndoAvailable = state.finishDayUndo != null,
            pendingOpenUnscheduledTopics = state.pendingOpenUnscheduledTopics,
        )
    }
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
                    rolloverUndoToken = state.rolloverUndoToken,
                    deleteUndoToken = state.deleteUndoToken,
                    finishDayUndoAvailable = state.finishDayUndo != null,
                    pendingOpenUnscheduledTopics = state.pendingOpenUnscheduledTopics,
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialChromeState)
    val initialPlansState = remember(viewModel) {
        val state = viewModel.uiState.value
        StudyPlansListState(
            plans = state.plans,
            templates = state.templates,
            loading = state.loading,
        )
    }
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
    }.collectAsStateWithLifecycle(initialPlansState)
    val initialDetailState = remember(viewModel) {
        val state = viewModel.uiState.value
        StudyPlannerDetailState(
            calendar = state.calendar,
            analytics = state.analytics,
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
            onboardingCompletedSteps = state.onboardingCompletedSteps,
            plannerAchievements = state.plannerAchievements,
            preferredStudyStrategy = state.preferredStudyStrategy,
            pendingManualSubjectOrder = state.pendingManualSubjectOrder,
            activePlanTab = state.activePlanTab,
        )
    }
    val detailState by remember(viewModel) {
        viewModel.uiState
            .map { state ->
                StudyPlannerDetailState(
                    calendar = state.calendar,
                    analytics = state.analytics,
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
                    onboardingCompletedSteps = state.onboardingCompletedSteps,
                    plannerAchievements = state.plannerAchievements,
                    preferredStudyStrategy = state.preferredStudyStrategy,
                    pendingManualSubjectOrder = state.pendingManualSubjectOrder,
                    activePlanTab = state.activePlanTab,
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialDetailState)
    val actions: PlannerActions = viewModel
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }

    val dailyTodoSetupPlan = chromeState.selectedPlan
    if (
        dailyTodoSetupVisible &&
        chromeState.section == PlannerSection.PLAN &&
        dailyTodoSetupPlan?.id == planId
    ) {
        dailyTodoSetupPlan?.let { setupPlan ->
            DailyTodoSetupSheet(
                plan = setupPlan,
                actions = actions,
                onDismiss = { dailyTodoSetupVisible = false },
            )
        }
    }

    LaunchedEffect(chromeState.error, chromeState.message, detailState.hydrateWarning, detailState.importError, detailState.importResultSummary, detailState.importStatus, detailState.structureError, detailState.structuredImportError, detailState.structuredImportSuccessMessage) {
        chromeState.error?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        chromeState.message?.let {
            val hasUndo = chromeState.finishDayUndoAvailable ||
                chromeState.rolloverUndoToken != null || chromeState.deleteUndoToken != null
            val result = snackbar.showSnackbar(
                message = it,
                actionLabel = if (hasUndo) "Undo" else null,
            )
            if (result == SnackbarResult.ActionPerformed) {
                when {
                    chromeState.finishDayUndoAvailable -> actions.undoFinishDay()
                    chromeState.deleteUndoToken != null -> actions.undoDelete()
                    chromeState.rolloverUndoToken != null -> actions.undoRollover()
                }
            }
            actions.clearTransient()
        }
        detailState.hydrateWarning?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        detailState.importStatus?.let { snackbar.showSnackbar(it) }
        detailState.importResultSummary?.let { snackbar.showSnackbar(it) }
        detailState.importError?.let { snackbar.showSnackbar(it) }
        detailState.structureError?.let { snackbar.showSnackbar(it) }
        detailState.structuredImportError?.let { snackbar.showSnackbar(it) }
        detailState.structuredImportSuccessMessage?.let { snackbar.showSnackbar(it) }
    }

    var preTourSection by remember { mutableStateOf<PlannerSection?>(null) }
    var preTourPlanId by remember { mutableStateOf<String?>(null) }
    var wasTourVisible by remember { mutableStateOf(false) }

    LaunchedEffect(tourState?.isVisible) {
        val state = tourState ?: return@LaunchedEffect
        if (state.isVisible) {
            wasTourVisible = true
            preTourSection = chromeState.section
            preTourPlanId = chromeState.selectedPlan?.id
        } else {
            if (wasTourVisible) {
                wasTourVisible = false
                // Restore previous state when tour ends
                val prevSec = preTourSection
                if (prevSec != null) {
                    actions.setSection(prevSec)
                    preTourSection = null
                }
                val prevPlanId = preTourPlanId
                if (prevPlanId != null && chromeState.selectedPlan?.id != prevPlanId) {
                    actions.openPlan(prevPlanId)
                    preTourPlanId = null
                } else if (prevPlanId == null && chromeState.selectedPlan != null) {
                    actions.navigateBack()
                    preTourPlanId = null
                }
            }
        }
    }

    LaunchedEffect(tourState?.currentStepIndex, tourState?.isVisible) {
        val state = tourState ?: return@LaunchedEffect
        if (state.isVisible) {
            when (state.currentStepIndex) {
                0, 1 -> {
                    // Steps 0 and 1 are on the MY TARGET EXAMS screen
                    if (chromeState.selectedPlan != null) {
                        actions.closePlan()
                    }
                    if (chromeState.section != PlannerSection.YOUR_EXAMS) {
                        actions.setSection(PlannerSection.YOUR_EXAMS)
                    }
                }
                2, 3, 4, 5 -> {
                    // PLAN tab (Dashboard, Quick Filters, Daily Todo, Mission)
                    if (chromeState.selectedPlan == null) {
                        val firstPlan = plansState.plans.firstOrNull()
                        if (firstPlan != null) {
                            actions.openPlan(firstPlan.id)
                        } else {
                            state.dismiss()
                            return@LaunchedEffect
                        }
                    }
                    if (chromeState.section != PlannerSection.PLAN) {
                        actions.setSection(PlannerSection.PLAN)
                    }
                }
                6 -> {
                    // SYLLABUS tab
                    if (chromeState.selectedPlan == null) {
                        val firstPlan = plansState.plans.firstOrNull()
                        if (firstPlan != null) {
                            actions.openPlan(firstPlan.id)
                        } else {
                            state.dismiss()
                            return@LaunchedEffect
                        }
                    }
                    if (chromeState.section != PlannerSection.SYLLABUS) {
                        actions.setSection(PlannerSection.SYLLABUS)
                    }
                }
                7, 8 -> {
                    // CALENDAR tab
                    if (chromeState.selectedPlan == null) {
                        val firstPlan = plansState.plans.firstOrNull()
                        if (firstPlan != null) {
                            actions.openPlan(firstPlan.id)
                        } else {
                            state.dismiss()
                            return@LaunchedEffect
                        }
                    }
                    if (chromeState.section != PlannerSection.CALENDAR) {
                        actions.setSection(PlannerSection.CALENDAR)
                    }
                }
                9 -> {
                    // INSIGHTS tab
                    if (chromeState.selectedPlan == null) {
                        val firstPlan = plansState.plans.firstOrNull()
                        if (firstPlan != null) {
                            actions.openPlan(firstPlan.id)
                        } else {
                            state.dismiss()
                            return@LaunchedEffect
                        }
                    }
                    if (chromeState.section != PlannerSection.INSIGHTS) {
                        actions.setSection(PlannerSection.INSIGHTS)
                    }
                }
            }
        }
    }

    // ── Internal back-press handling ────────────────────────────────────────────
    // The Study Planner manages its own sub-screens via ViewModel state (PlannerSection)
    // rather than NavController entries. Without this BackHandler the system back press
    // would skip all internal sections and jump straight to Home.
    //
    // Hierarchy:
    //   [sub-section B] → [sub-section A] → [plan list / YOUR_EXAMS] → Home (NavController)
    val hasInternalBackState = chromeState.selectedPlan != null
    BackHandler(enabled = hasInternalBackState) {
        actions.navigateBack()
        // navigateBack() always returns true when enabled (plan is open), so we just
        // let it update the ViewModel state. The BackHandler disables itself automatically
        // once selectedPlan becomes null (after closePlan()), letting the NavController
        // handle the final back press back to Home.
    }

    val selectedPlanForDrawer = chromeState.selectedPlan
    val drawerTitle = when {
        selectedPlanForDrawer != null &&
            (chromeState.section == PlannerSection.SYLLABUS ||
                chromeState.section == PlannerSection.INSIGHTS) ->
            selectedPlanForDrawer.title.takeIf { it.isNotBlank() }
                ?: chromeState.section.label
        selectedPlanForDrawer != null &&
            chromeState.section != PlannerSection.YOUR_EXAMS ->
            chromeState.section.label
        chromeState.section == PlannerSection.YOUR_EXAMS -> PlannerSection.YOUR_EXAMS.label
        else -> "Exam Planner"
    }
    val drawerSubtitle: String? = if (selectedPlanForDrawer != null && chromeState.section == PlannerSection.SYLLABUS) {
        PlannerSection.SYLLABUS.label
    } else {
        null
    }
    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(
        LocalDensity provides clampedDensity,
        com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme provides isDarkTheme
    ) {
        SafarDrawerScaffold(
            title = drawerTitle,
            subtitle = drawerSubtitle,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            topBarActions = {
                IconButton(onClick = { tourState?.start() }) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_butterfly_tour),
                        contentDescription = "Guide",
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        ) { padding ->
            Scaffold(
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                containerColor = SafarSemanticColors.plannerBackground(),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
                ),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (canUsePremiumPlannerFeatures) {
                        PlannerBottomBar(
                            selected = chromeState.section.takeIf { chromeState.selectedPlan != null }
                                // A first-time user is on Home, not the plan picker.
                                ?: if (plansState.plans.isEmpty()) PlannerSection.PLAN else PlannerSection.YOUR_EXAMS,
                            onSelect = { section ->
                                val activePlan = chromeState.selectedPlan
                                when {
                                    section == PlannerSection.YOUR_EXAMS || activePlan != null ->
                                        actions.setSection(section)
                                    else -> coroutineScope.launch {
                                        snackbar.showSnackbar("Please select an exam plan first.")
                                    }
                                }
                            },
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
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = StudyPlannerHomeTarget(
                                section = chromeState.section,
                                selectedPlanId = chromeState.selectedPlan?.id,
                            ),
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                            label = "StudyPlannerHome",
                            modifier = Modifier.fillMaxSize(),
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
                                canUsePremiumInsights = canUsePremiumPlannerFeatures,
                                isDarkTheme = isDarkTheme,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent,
                                onAdvanceTour = {
                                    if (tourState?.currentStepIndex == 2) tourState?.next()
                                },
                                isTourActive = tourState?.isVisible == true,
                                viewModel = viewModel,
                            )
                        }
                    }
                    val hasCachedContent = plansState.plans.isNotEmpty() || chromeState.selectedPlan != null
                    SafarInlineRefreshIndicator(
                        isRefreshing = chromeState.loading && hasCachedContent,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )

                    if (!canUsePremiumPlannerFeatures) {
                        StudyPlannerPremiumLockOverlay(
                            modifier = Modifier.fillMaxSize(),
                            onUpgrade = { onNavigate(Routes.PREMIUM) },
                        )
                    }

                    TourManager(
                        dataStore = viewModel.dataStore,
                        steps = studyPlannerTourSteps,
                        section = "study_planner",
                        askOnFirstVisit = true,
                        onTourStateReady = { tourState = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyPlannerPremiumLockOverlay(
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit,
) {
    val bgCream = com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors.BgCream
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .background(bgCream.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.28f),
                                scheme.primary.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        ),
                        CircleShape,
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.6f),
                                scheme.secondary.copy(alpha = 0.3f),
                            )
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Safar Premium feature",
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = "Safar Premium Feature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onBackground,
            )
            Text(
                text = "Upgrade to unlock Exam Planner, manual planning, AI syllabus setup, calendar, and insights.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
            GlassButton(
                onClick = onUpgrade,
                accentColor = scheme.primary,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = scheme.onPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Upgrade to Safar Premium", fontWeight = FontWeight.Bold, color = scheme.onPrimary)
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
    canUsePremiumPlannerFeatures: Boolean,
    onUpgrade: () -> Unit,
    onNavigate: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAdvanceTour: () -> Unit = {},
    selectedPlanId: String? = null,
) {
    var pendingDelete by remember { mutableStateOf<StudyPlan?>(null) }
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()

    pendingDelete?.let { plan ->
        ConfirmActionDialog(
            title = "Delete plan?",
            body = "This will delete ${plan.title} and its syllabus.",
            onDismiss = { pendingDelete = null },
            onConfirm = { actions.deletePlan(plan.id); pendingDelete = null },
        )
    }

    // ── Flat 2.0 layout ──────────────────────────────────────────
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // 2. Screen content column
        Column(modifier = Modifier.fillMaxSize()) {
            SafarPullRefreshBox(
                isRefreshing = state.loading && state.plans.isNotEmpty(),
                onRefresh = { actions.refreshPlans() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {

                    // ── Header ──────────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "My Target Exams",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PlannerFlatColors.TextDark,
                                )
                                Text(
                                    text = "Choose an exam to create a focused study plan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PlannerFlatColors.TextMuted,
                                )
                            }
                            
                            MacOSControlIconBadge(
                                accentColor = PlannerFlatColors.PrimaryAccent,
                                isLight = !isDark,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    // ── Loading skeletons ────────────────────────────────────
                    if (state.loading && state.plans.isEmpty()) {
                        items(3) {
                            PlanCardSkeleton(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    // ── Empty state ──────────────────────────────────────────
                    if (state.plans.isEmpty() && !state.loading) {
                        item {
                            PlannerEmptyState(
                                title = "No target exam yet",
                                body = "Plan an exam and it will appear here.",
                                action = "Plan Your Exams",
                                isLight = !isDark,
                                onAction = {
                                    onAdvanceTour()
                                    onNavigate(Routes.CREATE_PLAN)
                                },
                            )
                        }
                    }

                    // ── Plan cards ───────────────────────────────────────────
                    if (state.plans.isNotEmpty()) {
                        items(state.plans, key = { it.id }) { plan ->
                            PlannerTargetExamRow(
                                plan = plan,
                                isActive = plan.id == selectedPlanId,
                                isLight = !isDark,
                                onOpen = { actions.openPlan(plan.id) },
                                onDelete = { pendingDelete = plan },
                            )
                        }
                    }

                    // Bottom breathing room above the create-bar
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }

            // ── Pinned "Create Your New Plan" bar ────────────────────────────
            if (state.plans.isNotEmpty()) {
                PlannerCreateNewPlanBar(
                    isDark = isDark,
                    isLight = !isDark,
                    onClick = {
                        onAdvanceTour()
                        onNavigate(Routes.CREATE_PLAN)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlannerCreateNewPlanBar(
    isDark: Boolean,
    isLight: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        MacOSControlActionButton(
            text = "Create Your New Plan",
            icon = Icons.Default.Add,
            onClick = onClick,
            isLight = isLight,
            accentColor = PlannerFlatColors.PrimaryAccent,
            subtitle = "Add a new target exam",
        )
    }
}





@Composable
private fun PlannerEmptyState(
    title: String,
    body: String,
    action: String,
    isLight: Boolean = false,
    onAction: () -> Unit,
) {
    MacOSControlEmptyState(
        title = title,
        body = body,
        actionText = action,
        actionIcon = Icons.Default.Add,
        onAction = onAction,
        isLight = isLight,
        accentColor = PlannerFlatColors.PrimaryAccent,
    )
}

private data class TargetExamTone(
    val accent: Color,
    val softBackground: Color,
    val chipBackground: Color,
)

private fun targetExamTone(planId: String, isDark: Boolean): TargetExamTone {
    val accent = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val bg = if (isDark) Color(0xFF818CF8).copy(alpha = 0.16f) else Color(0xFF4F46E5).copy(alpha = 0.10f)
    val chipBg = if (isDark) Color(0xFF818CF8).copy(alpha = 0.25f) else Color(0xFF4F46E5).copy(alpha = 0.18f)
    return TargetExamTone(accent, bg, chipBg)
}



@Composable
private fun PlannerTargetExamRow(
    plan: StudyPlan,
    isActive: Boolean = false,
    isLight: Boolean = false,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val tone = targetExamTone(plan.id, isDark)
    val title = plan.title.ifBlank { plan.examType ?: "Study plan" }
    val subtitle = "Strategy • Practice • Success"
    val days = daysUntil(plan.examDate)
    var menuExpanded by remember { mutableStateOf(false) }
    val menuIconTint = PlannerFlatColors.TextMuted
    // ── Flat 2.0 tile ────────────────────────────────────────────
    MacOSExamPlanCard(
        title      = title,
        subtitle   = subtitle,
        accentColor = tone.accent,
        badgeText  = examBadgeLabel(days),
        isActive   = isActive,
        isLight    = isLight,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingContent = {
            Box {
                androidx.compose.material3.IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Open $title options",
                        tint = menuIconTint,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete plan") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        },
        onOpen = onOpen,
    )
}

@Composable
internal fun PremiumPlannerGateCard(
    title: String,
    body: String,
    action: String,
    onUpgrade: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
            }
            TextButton(onClick = onUpgrade) {
                Text(action, fontWeight = FontWeight.Bold)
            }
        }
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
    canUsePremiumInsights: Boolean,
    isDarkTheme: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAdvanceTour: () -> Unit = {},
    isTourActive: Boolean = false,
    viewModel: StudyPlannerViewModel,
) {
    val plan = chromeState.selectedPlan
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
            onboardingCompletedSteps = detailState.onboardingCompletedSteps,
            plannerAchievements = detailState.plannerAchievements,
        )
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (plan == null) {
                if (
                    chromeState.section == PlannerSection.YOUR_EXAMS ||
                    plansState.loading ||
                    plansState.plans.isNotEmpty()
                ) {
                    // The Plan tab always owns the full "My Target Exams" screen,
                    // including its empty state and Create Your New Plan action.
                    StudyPlansScreen(
                        state = plansState,
                        importState = detailState,
                        actions = actions,
                        canUsePremiumPlannerFeatures = canUsePremiumInsights,
                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                        onNavigate = onNavigate,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAdvanceTour = onAdvanceTour,
                        selectedPlanId = chromeState.selectedPlan?.id,
                    )
                } else {
                    // Keep first-time users in the planner flow: the Plan tab exposes
                    // the same target-exam setup screen shown in the product design.
                    PlannerHomeEmptyState(onCreatePlan = { actions.setSection(PlannerSection.YOUR_EXAMS) })
                }
            } else {
                when (chromeState.section) {
                    PlannerSection.YOUR_EXAMS -> StudyPlansScreen(
                        state = plansState,
                        importState = detailState,
                        actions = actions,
                        canUsePremiumPlannerFeatures = canUsePremiumInsights,
                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                        onNavigate = onNavigate,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAdvanceTour = onAdvanceTour,
                        selectedPlanId = chromeState.selectedPlan?.id,
                    )
                    PlannerSection.PLAN -> {
                    PlanTabScreen(
                        plan = plan,
                        actions = actions,
                        activeTab = detailState.activePlanTab,
                        onNavigate = { route ->
                            if (route.startsWith("syllabus/subjects/")) {
                                actions.setSection(PlannerSection.SYLLABUS)
                            } else {
                                onNavigate(route)
                            }
                        },
                        onboardingCompletedSteps = detailState.onboardingCompletedSteps,
                        preferredStudyStrategy = detailState.preferredStudyStrategy,
                        pendingManualSubjectOrder = detailState.pendingManualSubjectOrder,
                        pendingOpenUnscheduledTopics = chromeState.pendingOpenUnscheduledTopics,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    }
                    PlannerSection.SYLLABUS -> {
                        SyllabusSubjectsScreen(
                            viewModel = viewModel,
                            planId = plan.id,
                            isDarkTheme = isDarkTheme,
                            onNavigate = onNavigate,
                            onBack = { actions.setSection(PlannerSection.PLAN) },
                            onPlannerSectionSelect = { section ->
                                actions.setSection(section)
                            },
                            showBottomBar = false,
                        )
                    }
                    PlannerSection.CALENDAR -> CalendarTab(plan, activePlanState, actions)
                    PlannerSection.INSIGHTS -> {
                    InsightsTab(
                        plan = plan,
                        state = activePlanState,
                        actions = actions,
                        isPremium = canUsePremiumInsights,
                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                    )
                    }
                    PlannerSection.REVISION -> com.safarparmar.app.ui.studyplanner.plan.RevisionScreen(
                        plan = plan,
                        actions = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerHomeEmptyState(onCreatePlan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp).size(32.dp),
                )
            }
            Text(
                text = "Create your exam plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Set up an exam to see your daily study plan here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onCreatePlan) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create your exam plan")
            }
        }
    }
}




@Composable
internal fun PlannerBottomBar(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    val sections = listOf(
        PlannerSection.PLAN,
        PlannerSection.YOUR_EXAMS,
        PlannerSection.SYLLABUS,
        PlannerSection.CALENDAR,
        PlannerSection.INSIGHTS,
    )
    val icons = mapOf(
        PlannerSection.YOUR_EXAMS to Icons.Default.School,
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
            val tabAccent = when (section) {
                PlannerSection.PLAN -> PlannerTabAccent.Home
                PlannerSection.YOUR_EXAMS -> PlannerTabAccent.Plan
                PlannerSection.SYLLABUS -> PlannerTabAccent.Syllabus
                PlannerSection.CALENDAR -> PlannerTabAccent.Calendar
                PlannerSection.INSIGHTS -> PlannerTabAccent.Progress
                PlannerSection.REVISION -> PlannerRevisionAccent.Parent
            }
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
                    selectedIconColor = tabAccent,
                    selectedTextColor = tabAccent,
                    unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    indicatorColor = if (scheme.background.isLightBackground()) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f),
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
            GuideStep("Tap Build Planner", plan.flattenTopics().any { !it.topic.plannedDate.isNullOrBlank() }) { actions.setSection(PlannerSection.SYLLABUS) }
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

@Composable internal fun ConfirmActionDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) { Text(title) } },
        text = { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) { Text(body) } },
        confirmButton = {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}











@Composable
private fun ManualExampleLine(
    text: String,
    textColor: Color,
    separatorColor: Color,
) {
    val parts = text.split(" > ")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEachIndexed { index, part ->
            Text(
                part,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (index < parts.size - 1) {
                Text(
                    ">",
                    style = MaterialTheme.typography.bodyMedium,
                    color = separatorColor,
                )
            }
        }
    }
}





@Composable
private fun ManualGuideRule(
    label: String,
    description: String,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
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
