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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.safar.app.data.remote.api.UpdatePlanRequest
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val plannerTopicStatusFilterChips = listOf(
    TopicStatus.TODO,
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
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(StudyPlannerDetailState())
    val actions: PlannerActions = viewModel
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(chromeState.error, chromeState.message) {
        chromeState.error?.let { snackbar.showSnackbar(it); actions.clearTransient() }
        chromeState.message?.let { snackbar.showSnackbar(it); actions.clearTransient() }
    }

    val premiumReason = chromeState.premiumReason
    if (premiumReason != null) {
        PremiumGateSheet(
            reason = premiumReason,
            onDismiss = actions::clearTransient,
            onUpgrade = actions::upgradePlan,
        )
    }

    SafarDrawerScaffold(
        title = "Study Planner",
        subtitle = chromeState.selectedPlan?.title?.takeIf { it.isNotBlank() },
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
    actions: PlannerActions,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StudyPlan?>(null) }
    val quickStartState = remember(state.templates, state.loading) {
        StudyPlannerUiState(templates = state.templates, loading = state.loading)
    }

    if (showCreate) QuickStartSheet(quickStartState, actions, onDismiss = { showCreate = false })
    pendingDelete?.let { plan ->
        ConfirmActionDialog(
            title = "Delete plan?",
            body = "This will delete ${plan.title} and its syllabus.",
            onDismiss = { pendingDelete = null },
            onConfirm = { actions.deletePlan(plan.id); pendingDelete = null },
        )
    }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val screenBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFAF8F5)

    SafarPullRefreshBox(
        isRefreshing = state.loading && state.plans.isNotEmpty(),
        onRefresh = { actions.refreshPlans() },
        modifier = Modifier.fillMaxSize().background(screenBg),
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
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1A1E29)
                        )
                    )
                    Button(
                        onClick = { showCreate = true },
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2542A5).copy(alpha = 0.3f) else Color(0xFF2542A5).copy(alpha = 0.15f),
                            contentColor = if (isDark) Color(0xFF93A8F4) else Color(0xFF2542A5)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New plan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New",
                            fontSize = 14.sp,
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
                        shape = RoundedCornerShape(24.dp),
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
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Select an exam to begin.",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
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
                fontSize = 13.sp,
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
        shape = RoundedCornerShape(16.dp),
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
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
            Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.06f)
    Card(
        modifier = sharedModifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White else Color(0xFF1A1E29)
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
                    val badgeText = when {
                        examDays == null -> "—"
                        examDays < 0 -> "Ended"
                        examDays == 0L -> "Today"
                        else -> "$examDays days left"
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeBrush, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Plan actions",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
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
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = { progress.completionPercent / 100f },
                color = if (isDark) Color(0xFF7C8EFF) else Color(0xFF2542A5),
                trackColor = if (isDark) Color(0xFF334155) else Color(0xFFEAE8E4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.completionPercent}% complete",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Open",
                    color = if (isDark) Color(0xFF93A8F4) else Color(0xFF2542A5),
                    fontSize = 16.sp,
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.06f)
    Card(
        modifier = sharedModifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) Color.White else Color(0xFF1A1E29),
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
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
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
            val badgeText = when {
                examDays == null -> "—"
                examDays < 0 -> "Ended"
                examDays == 0L -> "Today"
                else -> "$examDays days left"
            }
            Box(
                modifier = Modifier
                    .background(badgeBrush, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${plan.subjectCount ?: plan.subjects.size} subjects",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.completionPercent / 100f },
                    color = if (isDark) Color(0xFF7C8EFF) else Color(0xFF2542A5),
                    trackColor = if (isDark) Color(0xFF334155) else Color(0xFFEAE8E4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Text(
                    text = "${progress.completionPercent}% complete",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontSize = 12.sp,
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
                OutlinedButton(onClick = { showAdvanced = !showAdvanced }, shape = RoundedCornerShape(12.dp)) {
                    Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Paste syllabus (optional)")
                }
                if (showAdvanced) {
                    OutlinedTextField(value = pasteSyllabus, onValueChange = { pasteSyllabus = it }, label = { Text("Paste syllabus") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text("${parseBulkSyllabus(pasteSyllabus).sumOf { it.second.size }} topics detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Plan title") }, modifier = Modifier.fillMaxWidth())
            PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it })
            OutlinedTextField(value = dailyGoal, onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) }, label = { Text("Topics per day") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { showAdvanced = !showAdvanced }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showAdvanced) "Hide weekly off days" else "Weekly off days")
            }
            if (showAdvanced) {
                OffDayPicker(selected = offDays.value, onToggle = { day ->
                    offDays.value = if (day in offDays.value) offDays.value - day else offDays.value + day
                })
            }
            Button(
                onClick = {
                    val goal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3
                    if (mode == "template" && templateId.isNotBlank()) {
                        actions.createFromTemplate(templateId, title.ifBlank { "Study Plan" }, examDate.ifBlank { null }, goal, offDays.value.toList())
                    } else {
                        actions.createPlan(title.ifBlank { "Study Plan" }, examType.ifBlank { null }, examDate.ifBlank { null }, goal, offDays.value.toList())
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (plan != null) {
            SelectedExamStrip(
                plan = plan,
                onChangeExam = { actions.setSection(PlannerSection.YOUR_EXAMS) },
            )
        }
        Box(Modifier.weight(1f)) {
            when (chromeState.section) {
                PlannerSection.YOUR_EXAMS -> StudyPlansScreen(
                    state = plansState,
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
        shape = RoundedCornerShape(16.dp),
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
                    fontSize = 12.sp,
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
                        fontSize = 12.sp,
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
                    fontSize = 13.sp,
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
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                template.description ?: "Pre-loaded syllabus template",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                maxLines = 1,
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
            .heightIn(min = 132.dp)
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
                fontSize = 19.sp,
            )
            Text(
                "Build your own plan from scratch. Paste your syllabus or set it up manually.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = 2,
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
            Text("Set Up Plan", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
private fun CalendarTab(plan: StudyPlan, state: StudyPlannerUiState, actions: PlannerActions) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
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
            actions = actions,
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
                        actions.setError("PDF export failed: ${e.localizedMessage}")
                    }
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Days Left Stacked Card Deck
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Stacked layer 2 (shadow)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                )
                // Stacked layer 1 (shadow border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                        .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                        .border(1.dp, if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                )
                // Top Main Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Small export button in the top right of the card
                        IconButton(
                            onClick = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.FileDownload,
                                contentDescription = "Export PDF",
                                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.8f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${daysUntil(plan.examDate)}",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "DAYS",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = FontStyle.Italic,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    )
                                    Text(
                                        text = "LEFT",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = FontStyle.Italic,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = readableDate(plan.examDate).takeIf { it.isNotBlank() } ?: "May 31, 2026",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            // Month navigation with chevron icons
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = Color.Gray
                    )
                }
                Text(
                    text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, locale).uppercase(locale)} ${visibleMonth.year}",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color.Black
                    )
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = Color.Gray
                    )
                }
            }
        }

        item {
            // Days of the week row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }

        item {
            // Grid of elevated day slots
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weeks.forEach { week ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        week.forEach { date ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
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
        }

        item {
            // Legend
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalendarLegendDot(Color(0xFF2563EB), "Planned")
                Spacer(Modifier.width(16.dp))
                CalendarLegendDot(Color(0xFF22C55E), "Done")
                Spacer(Modifier.width(16.dp))
                CalendarLegendDot(Color(0xFFEF4444), "Overdue")
                Spacer(Modifier.width(16.dp))
                CalendarLegendDot(Color(0xFFF59E0B), "Off")
            }
        }


    }
}

@Composable
private fun InsightsTab(plan: StudyPlan, state: StudyPlannerUiState, actions: PlannerActions) {
    val insights = remember(plan, state.calendar, state.analytics) {
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
                        actions.setError("PDF export failed: ${e.localizedMessage}")
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
    val paceBannerMessage = remember(s, insights.backlog, dailyGoal, pace) {
        buildInsightsPaceMessage(s, insights.backlog, dailyGoal, pace)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InsightsTopHeader(
                plan = plan,
                days = days,
                onExport = { exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf") },
            )
        }

        paceBannerMessage?.let { message ->
            item { InsightsPaceBanner(message) }
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
            AddTopicsToPlanButton(onClick = { actions.setSection(PlannerSection.SYLLABUS) })
        }
    }
}

private fun buildInsightsPaceMessage(
    summary: PlannerInsightSummary,
    backlog: PlannerInsightBacklog,
    dailyGoal: Int,
    requiredPace: Int,
): String? {
    if (summary.remainingTopics <= 0) return null
    if (summary.onTrackStatus != InsightTrackStatus.BEHIND && summary.onTrackStatus != InsightTrackStatus.AT_RISK) return null

    val targetPace = requiredPace.takeIf { it > 0 } ?: dailyGoal.coerceAtLeast(1)
    val targetTopicText = "Try $targetPace topic${if (targetPace == 1) "" else "s"} per day"
    val overflowStudyDays = summary.daysBuffer?.takeIf { it < 0 }?.let { -it } ?: 0

    return when {
        summary.daysUntilExam != null && summary.daysUntilExam < 0 ->
            "This exam date has passed. Update the exam date or archive this plan."

        backlog.overdueTotal > 0 && overflowStudyDays > 0 ->
            "${backlog.overdueTotal} topic${if (backlog.overdueTotal == 1) " is" else "s are"} overdue, and you may not finish before the exam. $targetTopicText, remove some topics, or change the exam date."

        backlog.overdueTotal > 0 ->
            "${backlog.overdueTotal} topic${if (backlog.overdueTotal == 1) " is" else "s are"} overdue. $targetTopicText to recover."

        overflowStudyDays > 0 ->
            "This plan needs more time. At ${dailyGoal.coerceAtLeast(1)} topic${if (dailyGoal == 1) "" else "s"} per day, you may not finish before the exam. $targetTopicText, remove some topics, or change the exam date."

        else ->
            "This plan is tight. $targetTopicText to stay on track."
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
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale.coerceAtMost(1.3f)
                    )
                ) {
                    Text(
                        "$completionPercent%",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
        modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick)
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
internal fun PlannerTopicDetailSheet(ref: TopicRef, openNonce: Int, actions: PlannerActions, onDismiss: () -> Unit) {
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
                OutlinedButton(onClick = { actions.deleteTopic(ref.topic.id); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Delete") }
                Button(
                    onClick = {
                        actions.updateTopic(ref.topic.id, status, name, date.ifBlank { "" }, notes)
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
internal fun BulkAddSheet(
    target: Pair<StudySubject, StudyChapter>,
    state: StudyPlannerUiState,
    actions: PlannerActions,
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
                actions.setError("Could not read the selected file.")
                return@launch
            }
            actions.importSyllabusFile(part.first, part.second)
        }
    }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        val extracted = extractBulkTopicsFromSyllabusCode(state.syllabusImportDraft)
        if (extracted.isNotBlank()) {
            text = extracted
        } else {
                actions.setError("No topics found in this file.")
        }
        actions.clearSyllabusImportDraft()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Many Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${target.first.name} • ${target.second.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Paste Text") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(16.dp)) })
                AssistChip(
                    onClick = { filePicker.launch(mimeTypes) },
                    label = { Text("Import File") },
                    leadingIcon = { Icon(Icons.Default.UploadFile, null, Modifier.size(16.dp)) },
                )
            }
            OutlinedTextField(text, { text = it }, label = { Text("Paste topics or chapter lines") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            state.syllabusImportFileName?.let { name ->
                Text("Imported: $name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$count topics detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            if (topics.isEmpty()) Text("No topics found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun PlannerBottomBar(selected: PlannerSection, onSelect: (PlannerSection) -> Unit) {
    val icons = mapOf(
        PlannerSection.YOUR_EXAMS to Icons.Default.School,
        PlannerSection.SYLLABUS to Icons.AutoMirrored.Filled.FactCheck,
        PlannerSection.CALENDAR to Icons.Default.CalendarMonth,
        PlannerSection.PLAN to Icons.Default.Today,
        PlannerSection.INSIGHTS to Icons.Default.Insights,
    )
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        val density = LocalDensity.current
        val stableDensity = Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(1.05f)
        )
        CompositionLocalProvider(LocalDensity provides stableDensity) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlannerSection.entries.forEach { section ->
                    val isSelected = selected == section
                    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(section) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icons.getValue(section),
                                contentDescription = section.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (section == PlannerSection.YOUR_EXAMS) "Exams" else section.label,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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

@Composable internal fun StatusDot(status: TopicStatus) {
    val color = when (status) {
        TopicStatus.TODO -> MaterialTheme.colorScheme.outline
        TopicStatus.DONE -> Color(0xFF16A34A)
        TopicStatus.REVISION_NEEDED -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.outline
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

@Composable internal fun EmptyPlannerCard(title: String, body: String, action: String, onAction: () -> Unit) {
    PlannerSurface {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(action) }
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
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun SyllabusFullImportCard(state: StudyPlannerUiState, actions: PlannerActions) {
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
                actions.setError("Could not read the selected file.")
                return@launch
            }
            actions.importSyllabusFile(part.first, part.second)
        }
    }
    LaunchedEffect(state.syllabusImportDraft) {
        if (state.syllabusImportDraft.isBlank()) return@LaunchedEffect
        text = state.syllabusImportDraft.trim()
        actions.clearSyllabusImportDraft()
    }
    val parsed = remember(text) { parseBulkSubjectsFromTxt(text) }
    val groups = parsed.getOrNull()
    val topicCount = groups?.let { countBulkSubjectsTopics(it) } ?: 0
    val chapterCount = groups?.let { countBulkSubjectsChapters(it) } ?: 0
    PlannerSurface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Import Syllabus", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                placeholder = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Paste the Syllabus",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Use - for subject, _ for chapter, and > for topic.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                onClick = { actions.importFullSyllabusFromTxt(text); text = "" },
                enabled = chapterCount > 0 && parsed.isSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Add to Plan")
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val todayK = todayKey()
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val overdue = if (dateIso < todayK) items.count { it.status != TopicStatus.DONE } else 0
    val dayNum = LocalDate.parse(dateIso).dayOfMonth.toString()

    val backgroundColor = when {
        isToday -> if (isDark) Color(0xFF1E293B) else Color.White
        planned > 0 && done == planned -> if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7) // Soft green for Completed
        planned > 0 && overdue > 0 -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2) // Soft red for Overdue
        isOff && planned > 0 -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7) // Soft yellow
        else -> if (isDark) Color(0xFF1E293B) else Color.White // Normal days or empty off days
    }

    val contentColor = if (isDark) Color.White else Color.Black

    val borderStroke = when {
        isToday -> BorderStroke(2.dp, Color(0xFF2563EB))
        selected -> BorderStroke(2.dp, Color(0xFF2563EB))
        else -> BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color.Black.copy(alpha = 0.05f))
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = if (isToday) 6.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = if (isToday) Color(0xFF2563EB) else Color.Black,
                spotColor = if (isToday) Color(0xFF2563EB) else Color.Black
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isToday) {
                Text(
                    text = "TODAY",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2563EB),
                    maxLines = 1
                )
            }
            Text(
                text = dayNum,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = contentColor,
                maxLines = 1
            )
            if (isOff && planned == 0) {
                Text(
                    text = "off",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B),
                    maxLines = 1
                )
            } else {
                // Topic Dots (max 4)
                val dots = items.take(4).map { item ->
                    when {
                        item.status == TopicStatus.DONE -> Color(0xFF22C55E) // Green for completed
                        dateIso < todayK && item.status != TopicStatus.DONE -> Color(0xFFEF4444) // Red for overdue
                        isOff -> Color(0xFFF59E0B) // Orange for off
                        else -> Color(0xFF2563EB) // Blue for planned
                    }
                }
                if (dots.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        dots.forEach { dotColor ->
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF94A3B8) else Color.DarkGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedDayLogSheet(
    dateIso: String,
    plan: StudyPlan,
    items: List<CalendarTopicItem>,
    actions: PlannerActions,
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
            Text("Day Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(readableDate(dateIso), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Planned $planned", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Done $done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                Text("Missed $missed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider()
            if (items.isEmpty()) {
                Text(
                    "No topics planned for this day.",
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
                                TextButton(onClick = { actions.updateTopic(item.topicId, status = TopicStatus.DONE) }) { Text("Done") }
                                TextButton(onClick = { actions.updateTopic(item.topicId, status = TopicStatus.REVISION_NEEDED) }) { Text("Revision") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(
                                    onClick = {
                                        val next = findNextAvailablePlannedDateIso(dateIso, plan.offDays)
                                        actions.updateTopic(item.topicId, plannedDate = next)
                                    },
                                ) {
                                    Text("Move")
                                }
                                TextButton(onClick = { actions.updateTopic(item.topicId, plannedDate = "") }) { Text("Remove Date") }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Button(
                    onClick = {
                        val next = findNextAvailablePlannedDateIso(dateIso, plan.offDays)
                        actions.moveTopicsToDate(items.map { it.topicId }, next)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Move all to next study day")
                }
                OutlinedButton(
                    onClick = {
                        actions.clearTopicDates(items.map { it.topicId })
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Clear Day")
                }
            }
        }
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
