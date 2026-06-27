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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.safarparmar.app.ui.butterfly.ButterflyTourState
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.studyPlannerTourSteps
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
    TopicStatus.DONE,
    TopicStatus.REVISION_NEEDED,
)

internal fun plannerTopicStatusDisplayLabel(status: TopicStatus): String =
    status.label

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
)

@Immutable
private data class StudyPlansListState(
    val plans: List<StudyPlan> = emptyList(),
    val templates: List<ExamTemplateSummary> = emptyList(),
    val loading: Boolean = false,
)

private data class PlannerTemplateOption(
    val id: String,
    val title: String,
    val examBody: String,
    val categoryLabel: String,
    val description: String,
    val topicCount: Int,
    val dailyGoal: Int,
    val startColor: Color,
    val endColor: Color,
)

private data class PlannerCapacityWarning(
    val topicCount: Int,
    val studyDays: Int,
    val dailyGoal: Int,
    val requiredPerDay: Int,
)

private fun countStudyDaysUntilExam(examDateIso: String, offDays: Set<Int>): Int {
    val examDate = runCatching { LocalDate.parse(examDateIso.take(10)) }.getOrNull() ?: return 0
    var cursor = LocalDate.now()
    var count = 0
    while (cursor.isBefore(examDate)) {
        if (cursor.dayOfWeek.value % 7 !in offDays) count += 1
        cursor = cursor.plusDays(1)
    }
    return count
}

private fun buildPlannerCapacityWarning(
    topicCount: Int,
    examDateIso: String,
    dailyGoal: Int,
    offDays: Set<Int>,
): PlannerCapacityWarning? {
    if (topicCount <= 0 || examDateIso.isBlank()) return null
    val studyDays = countStudyDaysUntilExam(examDateIso, offDays)
    if (studyDays <= 0) return null
    val goal = dailyGoal.coerceAtLeast(1)
    val requiredPerDay = kotlin.math.ceil(topicCount.toDouble() / studyDays.toDouble()).toInt()
    if (requiredPerDay <= goal) return null
    return PlannerCapacityWarning(topicCount, studyDays, goal, requiredPerDay)
}

private val plannerTemplateOptions = listOf(
    PlannerTemplateOption("ssc-cgl-tier1", "SSC CGL Tier-1", "SSC", "GOVT EXAM", "Combined Graduate Level Examination Tier-1 — 100 questions, 200 marks, 60 minutes", 233, 4, Color(0xFF233B6E), Color(0xFF345895)),
    PlannerTemplateOption("railway-ntpc", "Railway NTPC CBT-1", "RRB", "GOVT EXAM", "Non-Technical Popular Categories CBT-1 — 100 questions, 100 marks, 90 minutes", 148, 4, Color(0xFF664014), Color(0xFF9A681E)),
    PlannerTemplateOption("bank-po-prelims", "Bank PO Prelims", "IBPS / SBI", "BANKING", "IBPS PO / SBI PO Preliminary Exam — 100 questions, 100 marks, 60 minutes", 130, 3, Color(0xFF145447), Color(0xFF26735F)),
    PlannerTemplateOption("jee-mains", "JEE Mains", "NTA", "ENGINEERING", "Joint Entrance Examination (Main) — 90 questions across Physics, Chemistry, and Mathematics", 217, 4, Color(0xFF39236B), Color(0xFF5A3A9D)),
    PlannerTemplateOption("neet-ug", "NEET UG", "NTA", "MEDICAL", "National Eligibility cum Entrance Test (UG) — 200 questions across Physics, Chemistry, and Biology", 230, 4, Color(0xFF651F38), Color(0xFF92304F)),
)

private fun resolvedPlannerTemplateOptions(
    liveTemplates: List<ExamTemplateSummary>,
): List<PlannerTemplateOption> {
    val liveById = liveTemplates.associateBy { it.id }
    return plannerTemplateOptions.map { preset ->
        val live = liveById[preset.id]
        preset.copy(
            title = live?.name ?: preset.title,
            description = live?.description ?: preset.description,
            topicCount = live?.topicCount ?: preset.topicCount,
            dailyGoal = live?.recommendedDailyGoal ?: preset.dailyGoal,
        )
    }
}

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
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: StudyPlannerViewModel = hiltViewModel(),
) {
    val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = hiltViewModel()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val initialChromeState = remember(viewModel) {
        val state = viewModel.uiState.value
        StudyPlannerChromeState(
            selectedPlan = state.selectedPlan,
            section = state.section,
            loading = state.loading,
            mutating = state.mutating,
            error = state.error,
            message = state.message,
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
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialDetailState)
    val actions: PlannerActions = viewModel
    val snackbar = remember { SnackbarHostState() }
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }

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

    val drawerTitle = when {
        chromeState.selectedPlan != null &&
            chromeState.section != PlannerSection.YOUR_EXAMS &&
            chromeState.section != PlannerSection.SYLLABUS ->
            chromeState.section.label
        chromeState.section == PlannerSection.YOUR_EXAMS -> PlannerSection.YOUR_EXAMS.label
        else -> "Study Planner"
    }
    val drawerSubtitle: String? = null
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
            topBarActions = {
                IconButton(onClick = { tourState?.startDirectly() }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Tour")
                }
            },
        ) { padding ->
            Scaffold(
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
                ),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    PlannerBottomBar(
                        selected = chromeState.section.takeIf { chromeState.selectedPlan != null }
                            ?: PlannerSection.YOUR_EXAMS,
                        onSelect = { section ->
                            val activePlan = chromeState.selectedPlan
                            when {
                                section == PlannerSection.YOUR_EXAMS || activePlan != null ->
                                    actions.setSection(section)
                            }
                        },
                    )
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
                                canUsePremiumInsights = premiumStatus.canUseStudyPlannerInsights,
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
                    TourManager(
                        dataStore = viewModel.dataStore,
                        steps = studyPlannerTourSteps,
                        section = "study_planner",
                        askOnFirstVisit = false,
                        onTourStateReady = { tourState = it },
                    )
                }
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
    onAdvanceTour: () -> Unit = {},
) {
    var showCreate by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf("template") }
    var showTemplateCatalog by remember { mutableStateOf(false) }
    var selectedTemplateForSetup by remember { mutableStateOf<String?>(null) }
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
            initialMode = createMode,
            fixedTemplateId = selectedTemplateForSetup,
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

    BackHandler(enabled = showTemplateCatalog) { showTemplateCatalog = false }

    if (showTemplateCatalog) {
        PlannerTemplateCatalogScreen(
            templates = resolvedPlannerTemplateOptions(state.templates),
            onBack = { showTemplateCatalog = false },
            onSelectTemplate = { template ->
                selectedTemplateForSetup = template.id
                createMode = "template"
                showTemplateCatalog = false
                showCreate = true
            },
        )
    } else SafarPullRefreshBox(
        isRefreshing = state.loading && state.plans.isNotEmpty(),
        onRefresh = { actions.refreshPlans() },
        modifier = Modifier.fillMaxSize(),
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Choose an exam to create a focused study plan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        color = Color(0xFFF0EAFE),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF7C5AD9),
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
                        action = "Plan More Exams",
                        onAction = {
                            onAdvanceTour()
                            showTemplateCatalog = true
                        },
                    )
                }
            } else if (state.plans.isNotEmpty()) {
                items(state.plans, key = { it.id }) { plan ->
                    PlannerTargetExamRow(
                        plan = plan,
                        onOpen = { actions.openPlan(plan.id) },
                        onDelete = { pendingDelete = plan },
                    )
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                PlannerThemeActionCard(
                    title = "Plan More Exams",
                    subtitle = "Add more exams to your planner",
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    colors = listOf(Color(0xFF3D257B), Color(0xFF5B3B9B)),
                    onClick = {
                        onAdvanceTour()
                        showTemplateCatalog = true
                    },
                )
            }
            item {
                PlannerThemeActionCard(
                    title = "Custom Plan",
                    subtitle = "Create your own personalized plan",
                    icon = Icons.Default.Edit,
                    colors = listOf(Color(0xFF174777), Color(0xFF29619C)),
                    onClick = {
                        onAdvanceTour()
                        selectedTemplateForSetup = null
                        createMode = "custom"
                        showCreate = true
                    },
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
    val isDark = isSystemInDarkTheme()
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
    PlannerCard {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
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

private data class TargetExamTone(
    val accent: Color,
    val softBackground: Color,
    val chipBackground: Color,
)

private fun targetExamTone(planId: String): TargetExamTone {
    val tones = listOf(
        TargetExamTone(Color(0xFF4FAD38), Color(0xFFE6F3E2), Color(0xFFECF6E9)),
        TargetExamTone(Color(0xFF8358D3), Color(0xFFEDE7FB), Color(0xFFF1ECFC)),
        TargetExamTone(Color(0xFFFF8A37), Color(0xFFFFECDD), Color(0xFFFFF0E5)),
        TargetExamTone(Color(0xFF438DDD), Color(0xFFE3F0FD), Color(0xFFEAF3FE)),
        TargetExamTone(Color(0xFF45A1A7), Color(0xFFE0F2F3), Color(0xFFE8F5F5)),
        TargetExamTone(Color(0xFFE477A1), Color(0xFFFBE7EF), Color(0xFFFCECF2)),
    )
    return tones[(planId.hashCode() and Int.MAX_VALUE) % tones.size]
}

@Composable
private fun PlannerTargetExamRow(
    plan: StudyPlan,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val tone = targetExamTone(plan.id)
    val title = plan.title.ifBlank { plan.examType ?: "Study plan" }
    val subtitle = plan.examType
        ?.takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
        ?: "Strategy • Practice • Success"
    val days = daysUntil(plan.examDate)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .background(tone.accent, CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open $title options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun PlannerThemeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(13.dp).size(28.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
            }
            Surface(color = Color.White.copy(alpha = 0.14f), shape = CircleShape) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun PlannerTemplateCatalogScreen(
    templates: List<PlannerTemplateOption>,
    onBack: () -> Unit,
    onSelectTemplate: (PlannerTemplateOption) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to My Target Exams")
                }
                Column {
                    Text("Plan More Exams", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Pick a template to set up your next plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(templates, key = { it.id }) { template ->
            PlannerTemplateSelectionCard(
                template = template,
                selected = false,
                onClick = { onSelectTemplate(template) },
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickStartSheet(
    state: StudyPlannerUiState,
    actions: PlannerActions,
    initialMode: String = "template",
    fixedTemplateId: String? = null,
    onDismiss: () -> Unit,
) {
    var mode by remember(initialMode, fixedTemplateId) { mutableStateOf(if (fixedTemplateId != null) "template" else initialMode) }
    val templateOptions = remember(state.templates) { resolvedPlannerTemplateOptions(state.templates) }
    var templateId by remember(templateOptions, fixedTemplateId) { mutableStateOf(fixedTemplateId ?: templateOptions.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var examType by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableStateOf("3") }
    var pasteSyllabus by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var examDateError by remember { mutableStateOf(false) }
    val offDays = remember { mutableStateOf(setOf<Int>()) }
    val selectedTemplate = templateOptions.firstOrNull { it.id == templateId }
    var capacityWarning by remember { mutableStateOf<PlannerCapacityWarning?>(null) }

    LaunchedEffect(fixedTemplateId, selectedTemplate) {
        if (fixedTemplateId != null && selectedTemplate != null) {
            title = selectedTemplate.title
            examType = selectedTemplate.title
            dailyGoal = selectedTemplate.dailyGoal.toString()
        }
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    fun submitPlan(skipCapacityWarning: Boolean = false) {
        val goal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3
        val requiredExamDate = examDate.take(10).takeIf { it.isNotBlank() }
        if (requiredExamDate == null) {
            examDateError = true
            return
        }
        val topicCount = if (mode == "template") {
            selectedTemplate?.topicCount ?: 0
        } else {
            parseBulkSyllabus(pasteSyllabus).sumOf { it.second.size }
        }
        if (!skipCapacityWarning) {
            val warning = buildPlannerCapacityWarning(topicCount, examDate, goal, offDays.value)
            if (warning != null) {
                capacityWarning = warning
                return
            }
        }

        val hasSyllabusImport = mode == "custom" && pasteSyllabus.isNotBlank()
        if (mode == "template" && templateId.isNotBlank()) {
            actions.createFromTemplate(templateId, title.ifBlank { "Study Plan" }, requiredExamDate, goal, offDays.value.toList())
            onDismiss()
        } else {
            actions.createPlan(
                title = title.ifBlank { "Study Plan" },
                examType = examType.ifBlank { null },
                examDate = requiredExamDate,
                dailyGoal = goal,
                offDays = offDays.value.toList(),
                syllabusText = pasteSyllabus,
            )
            if (!hasSyllabusImport) onDismiss()
        }
    }

    capacityWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { capacityWarning = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("This plan may be too tight") },
            text = {
                Text(
                    "You have ${warning.topicCount} topics and about ${warning.studyDays} study days. " +
                        "To finish before the exam, this needs around ${warning.requiredPerDay} topics/day, " +
                        "but your current setting is ${warning.dailyGoal}.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    capacityWarning = null
                    submitPlan(skipCapacityWarning = true)
                }) {
                    Text("Continue anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { capacityWarning = null }) {
                    Text("Go back")
                }
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PlannerSectionHeader(
                    title = "Create plan",
                    subtitle = "Select exam and exam date.",
                )
                if (fixedTemplateId == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "template", onClick = { mode = "template" }, label = { Text("Template") })
                        FilterChip(selected = mode == "custom", onClick = { mode = "custom" }, label = { Text("Custom") })
                    }
                }
                if (mode == "template") {
                    if (fixedTemplateId != null && selectedTemplate != null) {
                        Text(
                            text = selectedTemplate.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text("Choose an exam template", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        templateOptions.forEach { template ->
                            PlannerTemplateSelectionCard(
                                template = template,
                                selected = templateId == template.id,
                                onClick = {
                                    templateId = template.id
                                    title = template.title
                                    examType = template.title
                                    dailyGoal = template.dailyGoal.toString()
                                },
                            )
                        }
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
                PlannerExamDateField(
                    examDateIso = examDate,
                    onExamDateChange = {
                        examDate = it
                        examDateError = false
                    },
                    isError = examDateError,
                )
                if (examDateError) {
                    Text(
                        text = "Exam date is required to create a planner.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
                                val requiredExamDate = examDate.take(10).takeIf { it.isNotBlank() }
                                if (requiredExamDate == null) {
                                    examDateError = true
                                    return@TextButton
                                }
                                actions.createPlan(
                                    title = title.ifBlank { "Study Plan" },
                                    examType = examType.ifBlank { null },
                                    examDate = requiredExamDate,
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
                        submitPlan()
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
}

@Composable
private fun PlannerTemplateSelectionCard(
    template: PlannerTemplateOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.24f),
        ),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(template.startColor, template.endColor)))
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = Color.White.copy(alpha = 0.14f), shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
                Surface(color = Color(0xFF315AAB).copy(alpha = 0.9f), shape = CircleShape) {
                    Text(
                        text = template.categoryLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                text = template.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${template.topicCount} topics  •  ${template.examBody}  •  ${template.dailyGoal}/day",
                color = Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Selected", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
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
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onAdvanceTour = onAdvanceTour,
                )
            } else {
                when (chromeState.section) {
                    PlannerSection.YOUR_EXAMS -> StudyPlansScreen(
                        state = plansState,
                        importState = detailState,
                        actions = actions,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAdvanceTour = onAdvanceTour,
                    )
                    PlannerSection.PLAN -> {
                    PlanTabScreen(
                        plan = plan,
                        actions = actions,
                        onNavigate = { route ->
                            if (route.startsWith("syllabus/subjects/")) {
                                actions.setSection(PlannerSection.SYLLABUS)
                            } else {
                                onNavigate(route)
                            }
                        },
                        onboardingCompletedSteps = detailState.onboardingCompletedSteps,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    }
                    PlannerSection.SYLLABUS -> {
                        SyllabusSubjectsScreen(
                            viewModel = viewModel,
                            planId = plan.id,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlannerTopicDetailSheet(
    ref: TopicRef,
    openNonce: Int,
    actions: PlannerActions,
    swapCandidates: List<TopicRef> = emptyList(),
    onDismiss: () -> Unit,
) {
    var name by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.name) }
    var notes by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.notes.orEmpty()) }
    var status by remember(ref.topic.id, openNonce) { mutableStateOf(ref.topic.status) }
    var showSwapPicker by remember(ref.topic.id, openNonce) { mutableStateOf(false) }
    val currentDate = ref.topic.plannedDate?.take(10).orEmpty()
    val dateSwapCandidates = remember(ref.topic.id, currentDate, swapCandidates) {
        swapCandidates
            .filter {
                it.topic.id != ref.topic.id &&
                    !it.topic.plannedDate.isNullOrBlank() &&
                    it.topic.plannedDate?.take(10) != currentDate
            }
            .sortedWith(
                compareBy<TopicRef> { it.topic.plannedDate?.take(10).orEmpty() }
                    .thenBy { it.topic.name.lowercase() },
            )
    }
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }
    if (showSwapPicker) {
        AlertDialog(
            onDismissRequest = { showSwapPicker = false },
            title = { Text("Swap planned date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pick a scheduled topic to exchange dates with.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(dateSwapCandidates, key = { it.topic.id }) { candidate ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        actions.swapTopicDates(ref.topic.id, candidate.topic.id)
                                        showSwapPicker = false
                                        onDismiss()
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = candidate.topic.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${readableDate(candidate.topic.plannedDate)} • ${candidate.subject.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwapPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ref.chapter.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plannerTopicStatusSheetChips.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(plannerTopicStatusDisplayLabel(st)) })
                    }
                }
                if (currentDate.isNotBlank() && dateSwapCandidates.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showSwapPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Swap Date")
                    }
                }
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
                            actions.updateTopic(ref.topic.id, status, name, notes = notes)
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
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
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
        PlannerSection.YOUR_EXAMS,
        PlannerSection.PLAN,
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
        text = {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
                OutlinedTextField(text, { text = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
                TextButton(enabled = text.trim().length >= 2, onClick = { onConfirm(text.trim()) }) { Text("Save") }
            }
        },
        dismissButton = {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides clampedDensity) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
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
internal fun SyllabusFullImportCard(state: StudyPlannerUiState, actions: PlannerActions) {
    var text by remember { mutableStateOf(state.rawSyllabusText) }
    val aiImportEnabled = BuildConfig.AI_SYLLABUS_IMPORT_ENABLED
    var mode by remember(aiImportEnabled) { mutableStateOf(if (aiImportEnabled) "ai" else "manual") }
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
