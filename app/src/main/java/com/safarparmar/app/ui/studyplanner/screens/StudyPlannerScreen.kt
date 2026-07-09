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
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.components.chapterHierarchyBrush
import com.safarparmar.app.ui.studyplanner.components.subjectHeaderBrush
import com.safarparmar.app.ui.studyplanner.components.subjectMeterBrush
import com.safarparmar.app.ui.studyplanner.components.topicHierarchyBrush
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
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: StudyPlannerViewModel = hiltViewModel(),
) {
    val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = hiltViewModel()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val canUsePremiumPlannerFeatures = premiumStatus.hasAnyPaidAccess || premiumStatus.canUseStudyPlannerInsights
    val actionsForPlanId: PlannerActions = viewModel
    androidx.compose.runtime.LaunchedEffect(planId) {
        // Confirming a new plan (created via the CreatePlan wizard) navigates back here
        // with ?planId=<confirmed id> so the user lands straight on it — on Calendar,
        // not the exam list or My Plan, per the wizard's "save & land on Calendar" step.
        planId?.let {
            actionsForPlanId.openPlan(it)
            actionsForPlanId.setSection(PlannerSection.CALENDAR)
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

    LaunchedEffect(chromeState.error, chromeState.message, detailState.hydrateWarning, detailState.importError, detailState.importResultSummary, detailState.importStatus, detailState.structureError, detailState.structuredImportError, detailState.structuredImportSuccessMessage) {
        chromeState.error?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        chromeState.message?.let {
            val hasUndo = chromeState.rolloverUndoToken != null || chromeState.deleteUndoToken != null
            val result = snackbar.showSnackbar(
                message = it,
                actionLabel = if (hasUndo) "Undo" else null,
            )
            if (result == SnackbarResult.ActionPerformed) {
                when {
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
                0, 1, 2 -> {
                    // Steps 0, 1, and 2 are on the MY TARGET EXAMS screen
                    if (chromeState.selectedPlan != null) {
                        actions.closePlan()
                    }
                    if (chromeState.section != PlannerSection.YOUR_EXAMS) {
                        actions.setSection(PlannerSection.YOUR_EXAMS)
                    }
                }
                3, 4, 5, 6 -> {
                    // PLAN tab: Auto-open first plan if none is open
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
                7, 8 -> {
                    // SYLLABUS tab: Auto-open first plan if none is open
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
                9, 10, 11, 12 -> {
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
                13 -> {
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
                14 -> {
                    // Bottom Nav highlights (PLAN tab)
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
            chromeState.section == PlannerSection.SYLLABUS ->
            selectedPlanForDrawer.title.takeIf { it.isNotBlank() } ?: PlannerSection.SYLLABUS.label
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

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        SafarDrawerScaffold(
            title = drawerTitle,
            subtitle = drawerSubtitle,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            showTopBarTitle = chromeState.section != PlannerSection.SYLLABUS,
            topBarActions = {},
        ) { padding ->
            Scaffold(
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                containerColor = SafarSemanticColors.plannerBackground(isDarkTheme),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
                ),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (canUsePremiumPlannerFeatures) {
                        PlannerBottomBar(
                            selected = chromeState.section.takeIf { chromeState.selectedPlan != null }
                                ?: PlannerSection.YOUR_EXAMS,
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
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.background
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.25f to bg.copy(alpha = 0.88f),
            0.45f to bg,
        )
    )

    Box(
        modifier = modifier
            .background(gradient)
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
            Button(
                onClick = onUpgrade,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Upgrade to Safar Premium", fontWeight = FontWeight.Bold)
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

    Column(modifier = Modifier.fillMaxSize()) {
        SafarPullRefreshBox(
            isRefreshing = state.loading && state.plans.isNotEmpty(),
            onRefresh = { actions.refreshPlans() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                                color = if (isDark) Color(0xFFE0E3E5) else Color(0xFF1A1C1E),
                            )
                            Text(
                                text = "Choose an exam to create a focused study plan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFC4C7C7) else Color(0xFF5E6266),
                            )
                        }
                        Surface(
                            color = if (isDark) Color(0xFF2D1F4D) else Color(0xFFF0EAFE),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFB39DDB) else Color(0xFF7C5AD9),
                                modifier = Modifier.padding(16.dp).size(40.dp),
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
                            title = "No target exam yet",
                            body = "Plan an exam and it will appear here.",
                            action = "Plan Your Exams",
                            onAction = {
                                onAdvanceTour()
                                onNavigate(Routes.CREATE_PLAN)
                            },
                        )
                    }
                } else if (state.plans.isNotEmpty()) {
                    items(state.plans, key = { it.id }) { plan ->
                        PlannerTargetExamRow(
                            plan = plan,
                            isActive = plan.id == selectedPlanId,
                            onOpen = { actions.openPlan(plan.id) },
                            onDelete = { pendingDelete = plan },
                        )
                    }
                }
            }
        }

        // Pinned "Create Your New Plan" bar — only when plans exist, so it never
        // scrolls out of view on this important create-plan surface. In the empty
        // state the "No target exam yet" card already carries the create action.
        if (state.plans.isNotEmpty()) {
            PlannerCreateNewPlanBar(
                isDark = isDark,
                onClick = {
                    onAdvanceTour()
                    onNavigate(Routes.CREATE_PLAN)
                },
            )
        }
    }
}

@Composable
private fun PlannerCreateNewPlanBar(
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF3D257B), Color(0xFF5B3B9B)))
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Create Your New Plan",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val cardGradient = if (isDark) DarkExpressiveGradient else LightExpressiveGradient
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        Box(modifier = Modifier.background(cardGradient)) {
            content()
        }
    }
}

@Composable
private fun PlannerEmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val cardColor = if (isDark) Color(0xFF181C1E) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) Color(0xFF444748).copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val iconBackground = if (isDark) Color(0xFF2C353D) else MaterialTheme.colorScheme.primaryContainer
    val buttonColor = if (isDark) Color(0xFFE0E3E5) else MaterialTheme.colorScheme.primary
    val buttonContent = if (isDark) Color(0xFF101416) else MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = iconBackground, shape = CircleShape) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFB9E0FF) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(26.dp),
                )
            }
            Text(
                title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 40.dp),
                shape = ButtonDefaults.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonContent,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(action)
            }
        }
    }
}

private data class TargetExamTone(
    val accent: Color,
    val softBackground: Color,
    val chipBackground: Color,
)

private fun targetExamTone(planId: String, isDark: Boolean): TargetExamTone {
    val tones = if (isDark) {
        listOf(
            TargetExamTone(Color(0xFF81C784), Color(0xFF1B3B22), Color(0xFF2E5B36)), // Green
            TargetExamTone(Color(0xFFB39DDB), Color(0xFF2D1F4D), Color(0xFF453075)), // Purple
            TargetExamTone(Color(0xFFFFB74D), Color(0xFF4E2C0F), Color(0xFF75451D)), // Orange
            TargetExamTone(Color(0xFF64B5F6), Color(0xFF0F3156), Color(0xFF1B4E85)), // Blue
            TargetExamTone(Color(0xFF4DD0E1), Color(0xFF0D3A40), Color(0xFF185D66)), // Teal
            TargetExamTone(Color(0xFFF06292), Color(0xFF451325), Color(0xFF6B223E)), // Pink
        )
    } else {
        listOf(
            TargetExamTone(Color(0xFF4FAD38), Color(0xFFE6F3E2), Color(0xFFECF6E9)),
            TargetExamTone(Color(0xFF8358D3), Color(0xFFEDE7FB), Color(0xFFF1ECFC)),
            TargetExamTone(Color(0xFFFF8A37), Color(0xFFFFECDD), Color(0xFFFFF0E5)),
            TargetExamTone(Color(0xFF438DDD), Color(0xFFE3F0FD), Color(0xFFEAF3FE)),
            TargetExamTone(Color(0xFF45A1A7), Color(0xFFE0F2F3), Color(0xFFE8F5F5)),
            TargetExamTone(Color(0xFFE477A1), Color(0xFFFBE7EF), Color(0xFFFCECF2)),
        )
    }
    return tones[(planId.hashCode() and Int.MAX_VALUE) % tones.size]
}

@Composable
private fun ActivePlanBadge(isDark: Boolean) {
    // Light mode: solid deep-purple pill (high contrast on white card)
    // Dark mode: translucent green pill (already high contrast on dark card)
    val activeColor = if (isDark) Color(0xFF10B981) else Color(0xFF4A2D8F)
    val pillBg     = if (isDark) activeColor.copy(alpha = 0.18f) else activeColor
    val textColor  = if (isDark) activeColor else Color.White
    val dotColor   = if (isDark) activeColor else Color.White

    val infiniteTransition = rememberInfiniteTransition(label = "activeDotPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Surface(
        color = pillBg,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dotColor.copy(alpha = dotAlpha), CircleShape),
            )
            Text(
                text = "Active",
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlannerTargetExamRow(
    plan: StudyPlan,
    isActive: Boolean = false,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val tone = targetExamTone(plan.id, isDark)
    val title = plan.title.ifBlank { plan.examType ?: "Study plan" }
    val subtitle = "Strategy • Practice • Success"
    val days = daysUntil(plan.examDate)
    var menuExpanded by remember { mutableStateOf(false) }

    val activeGreen = Color(0xFF10B981)
    val activePurple = Color(0xFF4A2D8F)
    // Active indicator color: green in dark mode (high contrast on dark card),
    // deep purple in light mode (high contrast on white card, matches brand).
    val activeAccent = if (isDark) activeGreen else activePurple
    val cardBg = if (isDark) Color(0xFF181C1E) else Color.White
    val cardBorder = when {
        isActive && isDark  -> activeAccent.copy(alpha = 0.55f)
        isActive            -> activeAccent               // full-opacity purple on white = clearly visible
        isDark              -> Color(0xFF444748).copy(alpha = 0.45f)
        else                -> Color.Transparent
    }
    val accentBarColor = if (isActive) activeAccent else tone.accent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isActive || isDark) BorderStroke(if (isActive) 1.5.dp else 1.dp, cardBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = tone.softBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = tone.accent,
                    modifier = Modifier.padding(17.dp).size(30.dp),
                )
            }
            // Left-edge accent bar — green when active, plan tone otherwise
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .background(accentBarColor, CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    color = if (isDark) Color(0xFFE0E3E5) else Color(0xFF1A1C1E),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = if (isDark) Color(0xFFC4C7C7) else Color(0xFF5E6266),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Active indicator badge — only shown for the currently open plan
                if (isActive) {
                    ActivePlanBadge(isDark = isDark)
                }
            }
            Surface(color = tone.chipBackground, shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = examBadgeLabel(days),
                    color = tone.accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Open $title options",
                        tint = if (isDark) Color(0xFFC4C7C7) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete plan") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
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
                    val badgeColors = when {
                        examDays == null -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        examDays < 0 -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        examDays <= 7 -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        examDays <= 14 -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        else -> CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    val badgeText = examBadgeLabel(examDays)
                    Box(
                        modifier = Modifier
                            .background(badgeColors.containerColor, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColors.contentColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Plan actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            val badgeColors = when {
                examDays == null -> CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                examDays < 0 -> CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                examDays <= 7 -> CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                examDays <= 14 -> CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                else -> CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            val badgeText = examBadgeLabel(examDays)
            Box(
                modifier = Modifier
                    .background(badgeColors.containerColor, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = badgeColors.contentColor,
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
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
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

/** One of the two top-level "how do you want to start" choices in the New Study Plan sheet. */
@Composable
internal fun CreatePlanModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.5f) else scheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = scheme.onSurface.copy(alpha = 0.06f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(10.dp).size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
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
    canUsePremiumInsights: Boolean,
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
                // The target-exam list is the single planner landing screen.
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
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    }
                    PlannerSection.SYLLABUS -> {
                        SyllabusSubjectsScreen(
                            viewModel = viewModel,
                            planId = plan.id,
                            isDarkTheme = isSystemInDarkTheme(),
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
                }
            }
        }
    }
}

@Composable
internal fun SelectedExamStrip(
    plan: StudyPlan,
    onChangeExam: () -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(outerPadding),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyllabusFullImportCard(
    state: StudyPlannerUiState,
    actions: PlannerActions,
    canUseAiImport: Boolean,
    onUpgrade: () -> Unit,
) {
    var text by remember { mutableStateOf(state.rawSyllabusText) }
    val aiImportEnabled = BuildConfig.AI_SYLLABUS_IMPORT_ENABLED
    var mode by remember(aiImportEnabled, canUseAiImport) { mutableStateOf(if (aiImportEnabled && canUseAiImport) "ai" else "manual") }
    var showManualGuide by remember { mutableStateOf(false) }
    var showManualImportChoice by remember { mutableStateOf(false) }
    var showAiImportChoice by remember { mutableStateOf(false) }
    val manualSyllabusExample = remember {
        """
        Maths > Algebra > Linear Equations
        Maths > Algebra > Quadratic Equations
        Science > Physics > Motion
        """.trimIndent()
    }
    val parsed = remember(text) { parseBulkSubjectsFromTxt(text) }
    val groups = parsed.getOrNull()
    val topicCount = groups?.let { countBulkSubjectsTopics(it) } ?: 0
    val chapterCount = groups?.let { countBulkSubjectsChapters(it) } ?: 0
    val preview = state.structuredPreview
    val scheme = MaterialTheme.colorScheme
    val hasExistingSyllabus = remember(state.selectedPlan) {
        state.selectedPlan?.subjects?.any { subject ->
            subject.chapters.any { chapter -> chapter.topics.isNotEmpty() }
        } == true
    }
    fun submitManualSyllabus(importMode: String) {
        actions.importFullSyllabusFromTxt(text, importMode)
    }

    if (showManualGuide) {
        ManualSyllabusGuideDialog(
            example = manualSyllabusExample,
            onDismiss = { showManualGuide = false },
            onAddExample = {
                text = if (text.isBlank()) {
                    manualSyllabusExample
                } else {
                    "${text.trimEnd()}\n\n$manualSyllabusExample"
                }
                showManualGuide = false
            },
        )
    }
    if (showManualImportChoice) {
        SyllabusImportChoiceSheet(
            topicCount = topicCount,
            chapterCount = chapterCount,
            onDismiss = { showManualImportChoice = false },
            onMerge = {
                showManualImportChoice = false
                submitManualSyllabus("merge")
            },
            onReplace = {
                showManualImportChoice = false
                submitManualSyllabus("replace")
            },
        )
    }
    if (showAiImportChoice) {
        val aiTopicCount = preview?.stats?.topicCount ?: 0
        val aiChapterCount = preview?.stats?.chapterCount ?: 0
        SyllabusImportChoiceSheet(
            topicCount = aiTopicCount,
            chapterCount = aiChapterCount,
            onDismiss = { showAiImportChoice = false },
            onMerge = {
                showAiImportChoice = false
                actions.importStructuredSyllabus("merge")
            },
            onReplace = {
                showAiImportChoice = false
                actions.importStructuredSyllabus("replace")
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier
                .background(scheme.surfaceContainerHighest)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (hasExistingSyllabus) "Update syllabus" else "Create syllabus",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface,
                )
                Text(
                    if (hasExistingSyllabus) {
                        "Add new topics or change your old syllabus easily."
                    } else {
                        "Paste your syllabus and SAFAR will turn it into subjects, chapters, and topics."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (aiImportEnabled) {
                SyllabusImportModeTabs(
                    mode = mode,
                    canUseAiImport = canUseAiImport,
                    onAiClick = {
                        if (canUseAiImport) mode = "ai" else onUpgrade()
                    },
                    onManualClick = { mode = "manual" },
                )
            }

            if (aiImportEnabled && mode == "ai" && canUseAiImport) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (preview != null) actions.updateStructuredPreview(null)
                    },
                    placeholder = {
                        Text(
                            "Paste your syllabus here...",
                            color = scheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    minLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !state.isStructuringSyllabus && !state.isImportingStructuredSyllabus,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    )
                )
                val isAiBtnEnabled = text.isNotBlank() && !state.isStructuringSyllabus && !state.isImportingStructuredSyllabus
                val aiBtnGradient = if (isAiBtnEnabled) {
                    Brush.horizontalGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                } else {
                    Brush.horizontalGradient(colors = listOf(scheme.onSurface.copy(alpha = 0.12f), scheme.onSurface.copy(alpha = 0.12f)))
                }
                Button(
                    onClick = { actions.structureSyllabusPreview(text) },
                    enabled = isAiBtnEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(aiBtnGradient, shape = RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (isAiBtnEnabled) Color.White else scheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    if (state.isStructuringSyllabus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Organizing your syllabus...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
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
                    val isAddToPlanEnabled = preview.subjects.isNotEmpty() && !state.isImportingStructuredSyllabus
                    val addToPlanGradient = if (isAddToPlanEnabled) {
                        Brush.horizontalGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                    } else {
                        Brush.horizontalGradient(colors = listOf(scheme.onSurface.copy(alpha = 0.12f), scheme.onSurface.copy(alpha = 0.12f)))
                    }
                    Button(
                        onClick = { if (hasExistingSyllabus) showAiImportChoice = true else actions.importStructuredSyllabus("merge") },
                        enabled = isAddToPlanEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .background(addToPlanGradient, shape = RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isAddToPlanEnabled) Color.White else scheme.onSurface.copy(alpha = 0.38f)
                        ),
                    ) {
                        if (state.isImportingStructuredSyllabus) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Adding syllabus to your plan...")
                        } else {
                            Text("Add to Plan")
                        }
                    }
                }
                state.structuredImportError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            } else {
                if (aiImportEnabled && !canUseAiImport) {
                    AiSyllabusPremiumCard(onUpgrade = onUpgrade)
                }
                ManualSyllabusHelpRow(onGuideClick = { showManualGuide = true })
                OutlinedTextField(
                    text,
                    { text = it },
                    placeholder = {
                        ManualSyllabusExamplePlaceholder()
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            text = if (text.isBlank()) {
                                manualSyllabusExample
                            } else {
                                "${text.trimEnd()}\n\n$manualSyllabusExample"
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Add example",
                                tint = scheme.primary,
                            )
                        }
                    },
                    minLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.primary.copy(alpha = 0.32f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                )
                when {
                    text.isBlank() -> Unit
                    parsed.isSuccess && groups != null -> Text("$topicCount topics / $chapterCount chapters / ${groups.size} subjects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Text(parsed.exceptionOrNull()?.message ?: "Invalid format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "One topic per line: Subject > Chapter > Topic. Same subject or chapter name on other lines? They group together automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val isManualBtnEnabled = chapterCount > 0 && parsed.isSuccess && !state.isImporting && !state.mutating
                val manualBtnGradient = if (isManualBtnEnabled) {
                    Brush.horizontalGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                } else {
                    Brush.horizontalGradient(colors = listOf(scheme.onSurface.copy(alpha = 0.12f), scheme.onSurface.copy(alpha = 0.12f)))
                }
                Button(
                    onClick = {
                        if (hasExistingSyllabus) showManualImportChoice = true else submitManualSyllabus("merge")
                    },
                    enabled = isManualBtnEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(manualBtnGradient, shape = RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (isManualBtnEnabled) Color.White else scheme.onSurface.copy(alpha = 0.38f)
                    ),
                ) {
                    Text(
                        if (state.mutating) "Adding..." else "Add Syllabus to Plan",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusImportModeTabs(
    mode: String,
    canUseAiImport: Boolean,
    onAiClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AI Scanner Choice Card
        Surface(
            onClick = onAiClick,
            shape = RoundedCornerShape(16.dp),
            color = if (mode == "ai") scheme.primaryContainer.copy(alpha = 0.5f) else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (mode == "ai") scheme.primary else scheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (mode == "ai") scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "AI Scanner",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mode == "ai") scheme.primary else scheme.onSurface
                    )
                    if (!canUseAiImport) {
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Premium Required",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    "Copy-paste your syllabus text from any PDF or website.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mode == "ai") scheme.onSurface else scheme.onSurfaceVariant
                )
            }
        }

        // Manual Setup Choice Card
        Surface(
            onClick = onManualClick,
            shape = RoundedCornerShape(16.dp),
            color = if (mode == "manual") scheme.primaryContainer.copy(alpha = 0.5f) else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (mode == "manual") scheme.primary else scheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (mode == "manual") scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Type Yourself",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mode == "manual") scheme.primary else scheme.onSurface
                    )
                }
                Text(
                    "Type your topics one by one easily.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mode == "manual") scheme.onSurface else scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiSyllabusPremiumCard(onUpgrade: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val blue = Color(0xFF2563EB)
    val deepBlue = Color(0xFF1D4ED8)
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF142036),
            Color(0xFF101827),
            Color(0xFF0F2748),
        )
    } else {
        listOf(
            Color(0xFFEFF6FF),
            Color(0xFFFFFFFF),
            Color(0xFFDDEBFF),
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF355581) else Color(0xFFBFDBFE)),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(gradientColors),
                )
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(88.dp),
                tint = Color(0xFF93C5FD).copy(alpha = 0.32f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = blue,
                    border = BorderStroke(8.dp, if (isDark) Color(0xFF1D355A) else Color.White.copy(alpha = 0.75f)),
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "AI syllabus import\nis premium",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                    )
                    Text(
                        "Manual syllabus entry stays free. Start Premium to let AI structure your syllabus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onUpgrade,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = deepBlue,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text("Start 7-day free trial", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSyllabusHelpRow(onGuideClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = scheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.72f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "Need help adding topics?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSurface,
            )
            Text(
                "Tap the guide for a simple example.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onGuideClick) {
            Text("Guide", fontWeight = FontWeight.Bold, color = scheme.primary)
            Spacer(Modifier.width(4.dp))
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = scheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualSyllabusExamplePlaceholder() {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(50),
            color = scheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.72f),
        ) {
            Text(
                "Example",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        ManualExampleLine("Maths > Algebra > Linear Equations", textColor = scheme.onSurface, separatorColor = scheme.onSurfaceVariant.copy(alpha = 0.5f))
        ManualExampleLine("Maths > Algebra > Quadratic Equations", textColor = scheme.onSurface, separatorColor = scheme.onSurfaceVariant.copy(alpha = 0.5f))
        ManualExampleLine("Science > Physics > Motion", textColor = scheme.onSurface, separatorColor = scheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyllabusImportChoiceSheet(
    topicCount: Int,
    chapterCount: Int,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.onSurfaceVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "How should we update this syllabus?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface,
                )
                Text(
                    "Found $topicCount topics across $chapterCount chapters in your text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Path 1: Merge
            Surface(
                onClick = onMerge,
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Keep existing + Add new",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onSurface
                        )
                        Text(
                            "Dono subjects ko jodein",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Best for small edits. Your current dates, notes, and progress stay safe. Duplicates are automatically skipped.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Path 2: Replace
            Surface(
                onClick = onReplace,
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = scheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Delete current & Start fresh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.error
                        )
                        Text(
                            "Purana delete karein aur naya shuru karein",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Warning: This will delete all current subjects, progress, and study dates in this plan and replace them completely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ManualSyllabusGuideDialog(
    example: String,
    onDismiss: () -> Unit,
    onAddExample: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Manual setup guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Use one topic per line to build your syllabus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManualGuideRule("Format", "Subject > Chapter > Topic, one topic per line")
                    ManualGuideRule("Grouping", "Repeat a subject or chapter name on other lines to group topics together")
                    ManualGuideRule("Duplicates", "The same topic twice under one chapter is only added once")
                }
                Text(
                    "Example",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                Surface(
                    color = scheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                ) {
                    Text(
                        example,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onAddExample) {
                Text("Add this example")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
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
    val scheme = MaterialTheme.colorScheme
    val expandedSubjects = remember { mutableStateMapOf<Int, Boolean>() }
    val expandedChapters = remember { mutableStateMapOf<String, Boolean>() }

    var editingSubjectIndex by remember { mutableStateOf<Int?>(null) }
    var editingChapterKey by remember { mutableStateOf<String?>(null) } // "subjectIndex-chapterIndex"
    var editingTopicKey by remember { mutableStateOf<String?>(null) } // "subjectIndex-chapterIndex-topicIndex"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Verify AI Organized Syllabus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = scheme.onSurface
            )
            Text(
                "${preview.stats.subjectCount} subjects • ${preview.stats.chapterCount} chapters • ${preview.stats.topicCount} topics",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (preview.warnings.isNotEmpty()) {
            Surface(
                color = scheme.errorContainer,
                contentColor = scheme.onErrorContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Warnings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    preview.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        if (preview.subjects.isEmpty()) {
            Text(
                "No syllabus structure was detected. Edit the text and try again, or use manual format.",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        preview.subjects.forEachIndexed { subjectIndex, subject ->
            val isSubjectExpanded = expandedSubjects[subjectIndex] ?: true
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Subject Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { expandedSubjects[subjectIndex] = !isSubjectExpanded }) {
                            Icon(
                                imageVector = if (isSubjectExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isSubjectExpanded) "Collapse" else "Expand",
                                tint = scheme.primary
                            )
                        }

                        if (editingSubjectIndex == subjectIndex) {
                            var tempName by remember { mutableStateOf(subject.name) }
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !isImporting,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = scheme.surface,
                                    unfocusedContainerColor = scheme.surface
                                )
                            )
                            IconButton(
                                onClick = {
                                    onPreviewChange(preview.renameSubject(subjectIndex, tempName))
                                    editingSubjectIndex = null
                                },
                                enabled = !isImporting && tempName.isNotBlank()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save name", tint = scheme.primary)
                            }
                        } else {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { editingSubjectIndex = subjectIndex }, enabled = !isImporting) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename subject", tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onPreviewChange(preview.deleteSubject(subjectIndex)) }, enabled = !isImporting) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete subject", tint = scheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Chapters under Subject (Indented)
                    if (isSubjectExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            subject.chapters.forEachIndexed { chapterIndex, chapter ->
                                val chapKey = "$subjectIndex-$chapterIndex"
                                val isChapExpanded = expandedChapters[chapKey] ?: false

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Chapter Row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(
                                                onClick = { expandedChapters[chapKey] = !isChapExpanded },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isChapExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isChapExpanded) "Collapse" else "Expand",
                                                    tint = scheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            if (editingChapterKey == chapKey) {
                                                var tempName by remember { mutableStateOf(chapter.name) }
                                                OutlinedTextField(
                                                    value = tempName,
                                                    onValueChange = { tempName = it },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    enabled = !isImporting,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = scheme.surface,
                                                        unfocusedContainerColor = scheme.surface
                                                    )
                                                )
                                                IconButton(
                                                    onClick = {
                                                        onPreviewChange(preview.renameChapter(subjectIndex, chapterIndex, tempName))
                                                        editingChapterKey = null
                                                    },
                                                    enabled = !isImporting && tempName.isNotBlank()
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Save name", tint = scheme.primary)
                                                }
                                            } else {
                                                Text(
                                                    text = chapter.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = scheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(onClick = { editingChapterKey = chapKey }, enabled = !isImporting, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Rename chapter", tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { onPreviewChange(preview.deleteChapter(subjectIndex, chapterIndex)) }, enabled = !isImporting, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete chapter", tint = scheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        // Topics under Chapter (Further Indented)
                                        if (isChapExpanded) {
                                            Column(
                                                modifier = Modifier.padding(start = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                chapter.topics.forEachIndexed { topicIndex, topic ->
                                                    val topKey = "$subjectIndex-$chapterIndex-$topicIndex"
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(scheme.primary.copy(alpha = 0.7f))
                                                        )

                                                        if (editingTopicKey == topKey) {
                                                            var tempName by remember { mutableStateOf(topic) }
                                                            OutlinedTextField(
                                                                value = tempName,
                                                                onValueChange = { tempName = it },
                                                                modifier = Modifier.weight(1f),
                                                                singleLine = true,
                                                                enabled = !isImporting,
                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                    focusedContainerColor = scheme.surface,
                                                                    unfocusedContainerColor = scheme.surface
                                                                )
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    onPreviewChange(preview.renameTopic(subjectIndex, chapterIndex, topicIndex, tempName))
                                                                    editingTopicKey = null
                                                                },
                                                                enabled = !isImporting && tempName.isNotBlank()
                                                            ) {
                                                                Icon(Icons.Default.Check, contentDescription = "Save name", tint = scheme.primary)
                                                            }
                                                        } else {
                                                            Text(
                                                                text = topic,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = scheme.onSurface,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            IconButton(onClick = { editingTopicKey = topKey }, enabled = !isImporting, modifier = Modifier.size(28.dp)) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Rename topic", tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                            }
                                                            IconButton(onClick = { onPreviewChange(preview.deleteTopic(subjectIndex, chapterIndex, topicIndex)) }, enabled = !isImporting, modifier = Modifier.size(28.dp)) {
                                                                Icon(Icons.Default.Close, contentDescription = "Delete topic", tint = scheme.error, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }

                                                // Add topic helper
                                                var newTopic by remember(subjectIndex, chapterIndex, preview.stats.topicCount) { mutableStateOf("") }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = newTopic,
                                                        onValueChange = { newTopic = it },
                                                        placeholder = { Text("Add missing topic...", style = MaterialTheme.typography.bodySmall) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        enabled = !isImporting,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = scheme.surface,
                                                            unfocusedContainerColor = scheme.surface
                                                        )
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            onPreviewChange(preview.addTopic(subjectIndex, chapterIndex, newTopic))
                                                            newTopic = ""
                                                        },
                                                        enabled = newTopic.isNotBlank() && !isImporting
                                                    ) {
                                                        Text("Add", style = MaterialTheme.typography.labelLarge)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = { onPreviewChange(null) }, enabled = !isImporting, modifier = Modifier.fillMaxWidth()) {
            Text("Back to raw text")
        }
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
