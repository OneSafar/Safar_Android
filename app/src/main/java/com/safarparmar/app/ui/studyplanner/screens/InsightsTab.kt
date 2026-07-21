package com.safarparmar.app.ui.studyplanner.screens
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Insights
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import kotlin.math.cos
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
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
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
// ── Liquid Glass design system ──────────────────────────────────────────────
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.subjectMeterBrush
import com.safarparmar.app.ui.studyplanner.components.flatCard

@Composable
internal fun InsightsTab(
    plan: StudyPlan,
    state: StudyPlannerUiState,
    actions: PlannerActions,
    isPremium: Boolean,
    onUpgrade: () -> Unit = {},
) {
    val insights = remember(plan, state.calendar, state.analytics) {
        PlannerInsightsCalculator.compute(plan, state.calendar, state.analytics)
    }
    val rollup = remember(plan.id, plan.subjects, plan.dailyTodos, plan.dailyTodoLogs) { plan.rollup() }
    val dailyGoal = (plan.dailyGoal ?: 1).coerceAtLeast(1)
    val examDays = daysUntil(plan.examDate)
        ?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        ?.toInt()

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    // Real study time, sourced from Ekagra sessions linked to this plan's topics —
    // the planner otherwise only ever knows "done" vs "not done", never how long
    // a topic actually took.
    val ekagraViewModel = hiltViewModel<com.safarparmar.app.ui.ekagra.EkagraViewModel>()
    val topicLinkedSessions by ekagraViewModel.topicLinkedSessions.collectAsStateWithLifecycle()
    // Keyed on updatedAt (not just id) so the list refreshes after the plan is
    // reloaded live — e.g. when an Ekagra session just linked a new topic session.
    LaunchedEffect(plan.id, plan.updatedAt) { ekagraViewModel.loadTopicLinkedSessions(plan.id) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 1. Solid canvas backdrop (white in light mode, black in dark mode)
        Box(modifier = Modifier.fillMaxSize())

        // 2. Main content column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                InsightsOverallProgressRedesign(
                    overallProgressPercent = rollup.overallProgressPercent,
                    dailyTodoProgressPercent = rollup.dailyTodoProgressPercent,
                    doneTopics = rollup.doneTopics,
                    totalTopics = rollup.totalTopics,
                    isLight = isLight
                )
            }
            item {
                InsightsMetricSquares(examDays = examDays, dailyGoal = dailyGoal, isLight = isLight)
            }
            item {
                ConsistencyStreakCard(consistency = insights.consistency, isLight = isLight)
            }
            item {
                InsightsStudySpeedCard(
                    recentTopicsPerDay = insights.summary.recentTopicsPerStudyDay,
                    requiredPerDay = insights.summary.requiredTopicsPerStudyDay,
                    dailyGoal = dailyGoal,
                    isLight = isLight
                )
            }
            item {
                // Prefer the forecast projected from the student's real recent
                // pace; only fall back to the daily-goal projection when there
                // isn't enough recent activity to measure a pace.
                val velocityForecast = insights.summary.velocityForecastCompletionDate
                val shownForecast = velocityForecast ?: insights.summary.forecastCompletionDate
                InsightsFinishLineCard(
                    examDateIso = plan.examDate,
                    forecastDateIso = shownForecast,
                    studyDaysLeft = insights.summary.availableStudyDays,
                    basedOnRecentPace = velocityForecast != null,
                    isLight = isLight,
                )
            }
            item {
                SubjectProgressChart(
                    subjects = insights.subjectRows,
                    isLight = isLight
                )
            }
            item {
                InsightsRevisionPulseCard(
                    plan = plan,
                    actions = actions,
                    isLight = isLight,
                )
            }
            if (topicLinkedSessions.isNotEmpty()) {
                item {
                    LinkedEkagraSessionsCard(sessions = topicLinkedSessions, isLight = isLight)
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "“Jo Paani se Nahayega Woh Libaaz Badelga , Jo Paseene Se Nahayega Woh Ithihaas Badlega”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextMuted
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Real study time per topic, sourced from Ekagra sessions linked via the
 * topic row's "Focus with Ekagra" action — the only place in the planner
 * that shows actual time spent, as opposed to just done/not-done.
 */
@Composable
internal fun LinkedEkagraSessionsCard(
    sessions: List<com.safarparmar.app.domain.model.TopicLinkedSession>,
    isLight: Boolean = false,
) {
    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val totalMinutes = totalSeconds / 60

    val tint = if (isLight) Color.Black else Color.White
    val tintAlpha = if (isLight) 0.04f else 0.05f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .flatCard(shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Focus sessions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF8A65) // Warm Orange!
                )
                Text(
                    "${totalMinutes} min total",
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
            sessions.take(10).forEach { session ->
                val mins = session.durationSeconds / 60
                val secs = session.durationSeconds % 60
                val durationLabel = if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isLight) Color.Black.copy(alpha = 0.03f)
                            else         Color.White.copy(alpha = 0.05f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = session.topicTitle ?: if (session.topicExists) "Untitled topic" else "Deleted topic",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            durationLabel,
                            fontSize = 12.sp,
                            color = PlannerFlatColors.TextMuted
                        )
                    }
                    Text(
                        "Ekagra",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFFF8A65)) // Accent Orange
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun InsightsTopHeader(
    plan: StudyPlan,
    days: Int?,
) {
    val examDays = daysUntil(plan.examDate)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = plan.title.ifBlank { plan.examType ?: "Selected exam" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val examDateLabel = readableDate(plan.examDate).takeUnless { it == "Not set" }
            if (!examDateLabel.isNullOrBlank()) {
                Text(
                    text = examDateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        ExamDaysCountdownBadge(days = examDays)
    }
}




@Composable

internal fun InsightsLiquidOverallProgressBar(
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

    val shape = CircleShape
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    val description = "Overall syllabus progress, $completionPercent percent"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), shape)
            .semantics { contentDescription = description },
    ) {
        if (animatedFraction > 0.001f) {
            BoxWithConstraints(
                Modifier
                    .fillMaxHeight()
                    .width(maxWidth * animatedFraction)
                    .clip(shape)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(PlannerFlatColors.PrimaryAccent),
                )
            }
        }
    }
}

@Composable

internal fun InsightsPaceBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
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
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun StudentInsightHero(
    plan: StudyPlan,
    rollup: PlanProgress,
) {
    val topics = remember(plan.subjects) { plan.flattenTopics().map { it.topic } }
    val total = rollup.totalTopics.coerceAtLeast(0)
    val done = rollup.doneTopics.coerceAtLeast(0)
    val revision = topics.count { it.status == TopicStatus.REVISION_NEEDED }
    val left = (total - done - revision).coerceAtLeast(0)
    val doneColor = Color(0xFF16A34A)
    val revisionColor = Color(0xFFF59E0B)
    val leftColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(doneColor.copy(alpha = 0.12f))
                        .border(1.dp, doneColor.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${rollup.completionPercent}%",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = doneColor,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Plan progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$done of $total topics completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (total > 0) {
                    if (done > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(done.toFloat())
                                .background(doneColor),
                        )
                    }
                    if (revision > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(revision.toFloat())
                                .background(revisionColor),
                        )
                    }
                    if (left > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(left.toFloat())
                                .background(leftColor),
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .background(leftColor),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PlannerLegendStat(
                    label = "Done",
                    value = done.toString(),
                    tint = doneColor,
                    modifier = Modifier.weight(1f),
                )
                PlannerLegendStat(
                    label = "Revision",
                    value = revision.toString(),
                    tint = revisionColor,
                    modifier = Modifier.weight(1f),
                )
                PlannerLegendStat(
                    label = "Left",
                    value = left.toString(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlannerLegendStat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun StudyPlannerAchievementsStrip(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier,
) {
    if (achievements.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Study Planner Rewards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "Badges and titles from your planner progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(achievements, key = { it.id }) { achievement ->
                    StudyPlannerAchievementCard(achievement = achievement)
                }
            }
        }
    }
}

@Composable
private fun StudyPlannerAchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val earned = achievement.earned
    val tint = if (earned) Color(0xFF16A34A) else scheme.primary
    val progress = when {
        achievement.earned -> 1f
        achievement.targetValue > 0 -> (achievement.currentValue.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)
        else -> (achievement.progress.toFloat() / 100f).coerceIn(0f, 1f)
    }
    val typeLabel = if (achievement.type.equals("title", ignoreCase = true)) "Title" else "Badge"
    val progressLabel = when {
        earned -> "Earned"
        achievement.targetValue > 0 -> "${achievement.currentValue}/${achievement.targetValue}"
        achievement.progress > 0 -> "${achievement.progress}%"
        else -> "Locked"
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (earned) tint.copy(alpha = 0.10f) else scheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (earned) tint.copy(alpha = 0.32f) else scheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.width(172.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = if (earned) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (achievement.type.equals("title", ignoreCase = true)) Icons.Rounded.AutoAwesome else Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint,
                    maxLines = 1,
                )
            }

            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            Text(
                text = achievement.description?.takeIf { it.isNotBlank() } ?: achievement.requirement,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                minLines = 2,
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(scheme.outlineVariant.copy(alpha = 0.45f)),
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(CircleShape)
                                .background(tint),
                        )
                    }
                }
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (earned) tint else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun StudentNextStepCard(
    plan: StudyPlan,
    insights: PlannerInsights,
    days: Int?,
    actions: PlannerActions,
) {
    val overdue = insights.backlog.overdueTotal
    val unplanned = insights.backlog.unplannedUnfinished
    val title: String
    val body: String
    val button: String
    val icon: ImageVector
    val tint: Color
    val action: () -> Unit

    when {
        plan.examDate.isNullOrBlank() || days == null -> {
            title = "Set exam date"
            body = "Without exam date, your planner cannot guide you properly."
            button = "Edit plan"
            icon = Icons.Default.CalendarMonth
            tint = MaterialTheme.colorScheme.primary
            action = { actions.setSection(PlannerSection.PLAN) }
        }
        unplanned > 0 -> {
            title = "Schedule your topics"
            body = "$unplanned topics have no study date. Pick dates for them in Unscheduled Topics."
            button = "View Unscheduled"
            icon = Icons.AutoMirrored.Filled.PlaylistAdd
            tint = Color(0xFFF59E0B)
            action = { actions.openUnscheduledTopics() }
        }
        overdue > 0 -> {
            title = "Clear overdue first"
            body = "$overdue topics are late. Finish them before starting new topics."
            button = "View Missed Topics"
            icon = Icons.Default.Warning
            tint = MaterialTheme.colorScheme.error
            action = { actions.openMissedTopics() }
        }
        insights.summary.onTrackStatus == InsightTrackStatus.BEHIND -> {
            title = "You are behind"
            body = "Build Planner again or increase your daily target."
            button = "Go to Syllabus"
            icon = Icons.Default.Refresh
            tint = MaterialTheme.colorScheme.error
            action = { actions.setSection(PlannerSection.SYLLABUS) }
        }
        else -> {
            title = "Keep going"
            body = "Your plan looks fine. Follow Today's Plan."
            button = "Go to Today"
            icon = Icons.Rounded.CheckCircle
            tint = Color(0xFF16A34A)
            action = { actions.setSection(PlannerSection.PLAN) }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                Button(
                    onClick = action,
                    colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.White),
                    shape = ButtonDefaults.shape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Text(button, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
internal fun InsightsMetricGrid(
    summary: PlannerInsightSummary,
    backlog: PlannerInsightBacklog,
    workload: PlannerInsightWorkload,
    dailyGoal: Int,
    requiredPace: Int,
) {
    val forecast = summary.forecastCompletionDate?.let { readableDate(it) }?.takeUnless { it == "Not set" } ?: "—"
    val buffer = summary.daysBuffer
    val statusLabel = when (summary.onTrackStatus) {
        InsightTrackStatus.ON_TRACK -> "On track"
        InsightTrackStatus.AT_RISK -> "At risk"
        InsightTrackStatus.BEHIND -> "Behind"
        InsightTrackStatus.AHEAD -> "Ahead"
        InsightTrackStatus.NEEDS_DATA -> "Needs data"
    }
    val statusColor = when (summary.onTrackStatus) {
        InsightTrackStatus.ON_TRACK, InsightTrackStatus.AHEAD -> Color(0xFF16A34A)
        InsightTrackStatus.AT_RISK -> Color(0xFFF59E0B)
        InsightTrackStatus.BEHIND -> MaterialTheme.colorScheme.error
        InsightTrackStatus.NEEDS_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val next14Total = workload.next14Days.sumOf { it.plannedCount }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InsightsMetricTile(
                label = "Plan status",
                value = statusLabel,
                helper = buffer?.let { if (it >= 0) "$it study days extra" else "${-it} study days short" } ?: "Build planner to calculate",
                icon = Icons.Default.Insights,
                tint = statusColor,
                modifier = Modifier.weight(1f),
            )
            InsightsMetricTile(
                label = "Need per day",
                value = if (requiredPace > 0) "$requiredPace/day" else "—",
                helper = "Goal: ${dailyGoal.coerceAtLeast(1)}/day",
                icon = Icons.Default.Today,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InsightsMetricTile(
                label = "May finish on",
                value = forecast,
                helper = summary.availableStudyDays?.let { "$it study days left" } ?: "Set exam date",
                icon = Icons.Default.CalendarMonth,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            InsightsMetricTile(
                label = "Dates added",
                value = summary.scheduleCoveragePercent?.let { "$it%" } ?: "—",
                helper = if (backlog.unplannedUnfinished > 0) "${backlog.unplannedUnfinished} topics need dates" else "$next14Total topics next 14 days",
                icon = Icons.AutoMirrored.Filled.FactCheck,
                tint = if (backlog.unplannedUnfinished > 0) Color(0xFFF59E0B) else Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun InsightsPaceForecastCard(
    summary: PlannerInsightSummary,
    dailyGoal: Int,
    requiredPace: Int,
) {
    val forecast = summary.forecastCompletionDate?.let { readableDate(it) }?.takeUnless { it == "Not set" } ?: "—"
    val statusLabel = when (summary.onTrackStatus) {
        InsightTrackStatus.ON_TRACK -> "On track"
        InsightTrackStatus.AT_RISK -> "At risk"
        InsightTrackStatus.BEHIND -> "Behind"
        InsightTrackStatus.AHEAD -> "Ahead"
        InsightTrackStatus.NEEDS_DATA -> "Needs data"
    }
    val statusColor = when (summary.onTrackStatus) {
        InsightTrackStatus.ON_TRACK, InsightTrackStatus.AHEAD -> Color(0xFF16A34A)
        InsightTrackStatus.AT_RISK -> Color(0xFFF59E0B)
        InsightTrackStatus.BEHIND -> MaterialTheme.colorScheme.error
        InsightTrackStatus.NEEDS_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val paceRatio = when {
        summary.remainingTopics <= 0 -> 1f
        requiredPace > 0 -> (dailyGoal.toFloat() / requiredPace.toFloat()).coerceIn(0f, 1f)
        summary.onTrackStatus == InsightTrackStatus.NEEDS_DATA -> 0.28f
        summary.onTrackStatus == InsightTrackStatus.BEHIND -> 0.42f
        summary.onTrackStatus == InsightTrackStatus.AT_RISK -> 0.72f
        else -> 1f
    }
    val bufferText = summary.daysBuffer?.let {
        if (it >= 0) "$it study day${if (it == 1) "" else "s"} buffer"
        else "${-it} study day${if (it == -1) "" else "s"} short"
    } ?: "Set exam date and study dates to calculate"

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PaceGauge(
                    progress = paceRatio,
                    statusLabel = statusLabel,
                    color = statusColor,
                    modifier = Modifier.size(118.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Study speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                    )
                    Text(
                        text = bufferText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InsightsInnerStat(
                    label = "Need per day",
                    value = if (requiredPace > 0) "$requiredPace/day" else "—",
                    valueColor = statusColor,
                    icon = Icons.Default.Today,
                    iconTint = statusColor,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Your target",
                    value = "$dailyGoal/day",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.School,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InsightsInnerStat(
                    label = "May finish on",
                    value = forecast,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.CalendarMonth,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Study days left",
                    value = summary.availableStudyDays?.toString() ?: "—",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.Insights,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PaceGauge(
    progress: Float,
    statusLabel: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "paceGaugeProgress",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    val centerText = "${(animatedProgress * 100).roundToInt()}%"
    Box(
        modifier = modifier.semantics {
            contentDescription = "Study speed $statusLabel, $centerText"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
            )
            Text(
                text = "speed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightsMetricTile(
    label: String,
    value: String,
    helper: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
internal fun InsightsWorkloadCard(workload: PlannerInsightWorkload) {
    val next14Total = workload.next14Days.sumOf { it.plannedCount }
    val busiest = workload.busiestDay
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Upcoming workload", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightsInnerStat(
                    label = "Next 14 days",
                    value = "$next14Total topics",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.Today,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Busiest day",
                    value = busiest?.let { "${it.plannedCount} topics" } ?: "—",
                    valueColor = if ((busiest?.plannedCount ?: 0) >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.CalendarMonth,
                    iconTint = if ((busiest?.plannedCount ?: 0) >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = when {
                    workload.overloadDays > 0 -> "${workload.overloadDays} heavy day${if (workload.overloadDays == 1) "" else "s"} detected. Use Build Planner again if the load feels uneven."
                    workload.emptyStudyDays > 7 -> "${workload.emptyStudyDays} empty days in the next 14. Build Planner may need to run after adding topics."
                    else -> "Your next 14 days look balanced."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun InsightsRevisionStudyCardWidget(
    plan: StudyPlan,
    actions: PlannerActions,
    isLight: Boolean = false,
) {
    InsightsRevisionPulseCard(plan = plan, actions = actions, isLight = isLight)
}

/**
 * "Your study streak" — merges what used to be two separate cards (a celebratory streak
 * card and a harshly-toned "Study regularity / Missed days" card showing the same
 * underlying [consistency] data) into one. Days that had unfinished topics are still
 * shown, but as a soft secondary line in neutral tones rather than a red/amber bar list,
 * so falling behind reads as a nudge, not a scolding.
 */
@Composable
internal fun OverallProgressCard(
    completionPercent: Int,
    doneTopics: Int,
    totalTopics: Int,
    remaining: Int,
    days: Int?,
    dailyGoal: Int,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "Overall study progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Syllabus progress",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale.coerceAtMost(1.3f)
                    )
                ) {
                    Text(
                        text = "$completionPercent%",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$doneTopics of $totalTopics topics complete",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$remaining topics remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            InsightsLiquidOverallProgressBar(
                completionPercent = completionPercent,
                height = 18.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightsInnerStat(
                    label = "Exam left",
                    value = days?.let { "$it days" } ?: "—",
                    valueColor = if (days != null && days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.CalendarMonth,
                    iconTint = if (days != null && days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Daily target",
                    value = if (dailyGoal > 0) "$dailyGoal topic${if (dailyGoal == 1) "" else "s"}" else "—",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.School,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun InsightsInnerStat(
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color,
    isLight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .flatCard(shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextMuted
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
internal fun SubjectProgressCard(
    rows: List<PlannerInsightSubjectRow>,
    subjectCount: Int,
    subjectIndexById: Map<String, Int>,
    isLight: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .flatCard(shape = RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Subject progress",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PlannerFlatColors.TextDark,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${rows.size} subject${if (rows.size == 1) "" else "s"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            if (rows.isEmpty()) {
                Text(
                    text = "Add topics to see subject progress.",
                    fontSize = 14.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            } else {
                rows.forEachIndexed { idx, row ->
                    val colorIdx = subjectIndexById[row.subjectId] ?: 0
                    SubjectProgressRow(row, colorIdx, max(1, subjectCount), isLight = isLight)
                    if (idx < rows.lastIndex) {
                        HorizontalDivider(
                            color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubjectProgressRow(
    row: PlannerInsightSubjectRow,
    subjectColorIndex: Int,
    subjectColorCount: Int,
    isLight: Boolean = false,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(brush = subjectMeterBrush(subjectColorIndex, subjectColorCount))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                Text(
                    text = row.subjectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${row.remainingTopics} topic${if (row.remainingTopics == 1) "" else "s"} remaining",
                    fontSize = 11.sp,
                    color = PlannerFlatColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${row.completionPercent}%",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = PlannerFlatColors.TextDark,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(
                    if (isLight) Color.Black.copy(alpha = 0.06f)
                    else         Color.White.copy(alpha = 0.08f)
                ),
        ) {
            if (row.completionPercent > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((row.completionPercent / 100f).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(brush = subjectMeterBrush(subjectColorIndex, subjectColorCount)),
                )
            }
        }
    }
}



@Composable
internal fun InsightsLaggingSubjectsCard(chapters: List<PlannerInsightLaggingChapter>) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Needs attention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            chapters.forEach { row ->
                Text(
                    "${row.subjectName} · ${row.chapterName} — ${row.remainingTopics} left (${row.overdueTopics} overdue)",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}



// Exam-date editing was consolidated into the single canonical PlanSettingsSheet
// (opened from the Home gear). The former duplicate PlannerExamDateFieldDialog
// here was dead and has been removed — Progress reads plan.examDate for display
// only and points users to Settings to change it.

// --- NEW INSIGHTS REDESIGN ---

@Composable
internal fun InsightsDetailedMetricsList(
    requiredPace: Int,
    dailyGoal: Int,
    forecastDate: String?,
    studyDaysLeft: Int?,
    isLight: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InsightsMetricRow(
            icon = Icons.Default.Speed,
            iconColor = Color(0xFFE47AB5), // Pink/Coral
            label = "Need per day",
            value = "$requiredPace/day",
            isLight = isLight
        )
        InsightsMetricRow(
            icon = Icons.Default.TrackChanges,
            iconColor = Color(0xFF7C5AD9), // Violet
            label = "Your target",
            value = "$dailyGoal/day",
            isLight = isLight
        )
        InsightsMetricRow(
            icon = Icons.Default.CalendarToday,
            iconColor = Color(0xFFFFB300), // Amber
            label = "May finish on",
            value = forecastDate ?: "Unknown",
            isLight = isLight
        )
        InsightsMetricRow(
            icon = Icons.Default.DateRange,
            iconColor = Color(0xFF00ACC1), // Cyan
            label = "Study days left",
            value = "${studyDaysLeft ?: "-"}",
            isLight = isLight
        )
    }
}

@Composable
internal fun InsightsMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    isLight: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .flatCard(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlannerFlatColors.TextDark
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextDark
            )
        }
    }
}
