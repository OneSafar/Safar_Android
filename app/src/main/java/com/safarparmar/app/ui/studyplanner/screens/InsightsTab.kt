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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.safarparmar.app.ui.studyplanner.components.chapterHierarchyBrush
import com.safarparmar.app.ui.studyplanner.components.subjectHeaderBrush
import com.safarparmar.app.ui.studyplanner.components.subjectMeterBrush
import com.safarparmar.app.ui.studyplanner.components.topicHierarchyBrush
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
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
    val rollup = remember(plan.id, plan.subjects) { plan.rollup() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPremium) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SelectedExamStrip(
                            plan = plan,
                            onChangeExam = { actions.setSection(PlannerSection.YOUR_EXAMS) },
                            outerPadding = PaddingValues(0.dp),
                        )
                    }
                    item {
                        StudentInsightHero(
                            plan = plan,
                            rollup = rollup,
                        )
                    }
                    item {
                        StudyPlannerAchievementsStrip(
                            achievements = state.plannerAchievements,
                        )
                    }
                    item {
                        SubjectProgressChart(
                            subjects = insights.subjectRows,
                        )
                    }
                    item {
                        ConsistencyInsightsCard(consistency = insights.consistency)
                    }
                }
            }
        } else {
            InsightsPremiumLockOverlay(
                onUpgrade = onUpgrade,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun InsightsPremiumLockOverlay(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
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
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onUpgrade
            )
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
                    .border(1.5.dp, Brush.linearGradient(
                        listOf(scheme.primary.copy(alpha = 0.6f), scheme.secondary.copy(alpha = 0.3f))
                    ), CircleShape),
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
                text = "Upgrade to see simple charts for progress, subject completion, and weekly study load.",
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

internal fun buildInsightsPaceMessage(
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


/** Dynamic theme gradient for Insights overall progress. */
@Composable
internal fun insightsOverallProgressFillBrush(): Brush = Brush.horizontalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary
    ),
)

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
            title = "Tap Build Planner"
            body = "$unplanned topics are not in calendar. Go to Syllabus and tap Build Planner."
            button = "Go to Syllabus"
            icon = Icons.AutoMirrored.Filled.PlaylistAdd
            tint = Color(0xFFF59E0B)
            action = { actions.setSection(PlannerSection.SYLLABUS) }
        }
        overdue > 0 -> {
            title = "Clear overdue first"
            body = "$overdue topics are late. Finish them before starting new topics."
            button = "Go to Plan"
            icon = Icons.Default.Warning
            tint = MaterialTheme.colorScheme.error
            action = { actions.setSection(PlannerSection.PLAN) }
        }
        insights.summary.onTrackStatus == InsightTrackStatus.BEHIND -> {
            title = "You are lagging"
            body = "Build Planner again or increase daily topics."
            button = "Go to Syllabus"
            icon = Icons.Default.Refresh
            tint = MaterialTheme.colorScheme.error
            action = { actions.setSection(PlannerSection.SYLLABUS) }
        }
        else -> {
            title = "Keep going"
            body = "Your plan looks fine. Follow Today's Agenda."
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
internal fun SubjectProgressChart(
    subjects: List<PlannerInsightSubjectRow>,
) {
    val chartSubjects = subjects
        .sortedWith(
            compareByDescending<PlannerInsightSubjectRow> { it.overdueTopics }
                .thenByDescending { it.remainingTopics }
        )
        .take(5)

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Subject progress", fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (chartSubjects.isEmpty()) {
                Text("Add topics to see subject chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                chartSubjects.forEach { row ->
                    StudentSubjectBar(row)
                }
            }
        }
    }
}

@Composable
private fun StudentSubjectBar(row: PlannerInsightSubjectRow) {
    val progress = (row.completionPercent / 100f).coerceIn(0f, 1f)
    val tint = when {
        row.overdueTopics > 0 -> MaterialTheme.colorScheme.error
        row.completionPercent < 25 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.subjectName,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${row.completionPercent}%", fontWeight = FontWeight.Black, color = tint)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        ) {
            if (progress > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
        }
        Text(
            text = "${row.remainingTopics} topics left${if (row.overdueTopics > 0) " • ${row.overdueTopics} overdue" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ConsistencyInsightsCard(consistency: PlannerInsightConsistency) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Consistency / Missed Days", fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (consistency.missedDays.isEmpty()) {
                Text(
                    text = "Great job! You haven't missed any planned topics recently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                consistency.missedDays.forEach { day ->
                    val date = runCatching { LocalDate.parse(day.date.take(10)) }.getOrNull()
                    val label = date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: day.date.takeLast(2)
                    
                    val completionPercent = if (day.plannedCount > 0) {
                        (day.doneCount.toFloat() / day.plannedCount.toFloat())
                    } else {
                        0f
                    }
                    val displayPercent = (completionPercent * 100).toInt()

                    val tint = when {
                        completionPercent < 0.25f -> MaterialTheme.colorScheme.error
                        completionPercent < 0.75f -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(label, modifier = Modifier.width(34.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        ) {
                            if (completionPercent > 0f) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(completionPercent.coerceIn(0.06f, 1f))
                                        .clip(CircleShape)
                                        .background(tint),
                                )
                            }
                        }
                        Text("$displayPercent%", modifier = Modifier.width(36.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Black)
                    }
                }
                Text(
                    text = "Displays % completion for days where you didn't finish all topics.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                label = "Track status",
                value = statusLabel,
                helper = buffer?.let { if (it >= 0) "$it study days buffer" else "${-it} study days short" } ?: "Build planner for forecast",
                icon = Icons.Default.Insights,
                tint = statusColor,
                modifier = Modifier.weight(1f),
            )
            InsightsMetricTile(
                label = "Required pace",
                value = if (requiredPace > 0) "$requiredPace/day" else "—",
                helper = "Goal: ${dailyGoal.coerceAtLeast(1)}/day",
                icon = Icons.Default.Today,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InsightsMetricTile(
                label = "Forecast finish",
                value = forecast,
                helper = summary.availableStudyDays?.let { "$it study days left" } ?: "Set exam date",
                icon = Icons.Default.CalendarMonth,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            InsightsMetricTile(
                label = "Schedule coverage",
                value = summary.scheduleCoveragePercent?.let { "$it%" } ?: "—",
                helper = if (backlog.unplannedUnfinished > 0) "${backlog.unplannedUnfinished} unplanned" else "$next14Total topics next 14 days",
                icon = Icons.AutoMirrored.Filled.FactCheck,
                tint = if (backlog.unplannedUnfinished > 0) Color(0xFFF59E0B) else Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
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
internal fun InsightsBacklogCard(backlog: PlannerInsightBacklog) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Backlog health", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightsInnerStat(
                    label = "Overdue",
                    value = backlog.overdueTotal.toString(),
                    valueColor = if (backlog.overdueTotal > 0) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                    icon = Icons.Default.Warning,
                    iconTint = if (backlog.overdueTotal > 0) MaterialTheme.colorScheme.error else Color(0xFF16A34A),
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Revision",
                    value = backlog.revisionNeeded.toString(),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.Refresh,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightsInnerStat(
                    label = "Unplanned",
                    value = backlog.unplannedUnfinished.toString(),
                    valueColor = if (backlog.unplannedUnfinished > 0) Color(0xFFF59E0B) else Color(0xFF16A34A),
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    iconTint = if (backlog.unplannedUnfinished > 0) Color(0xFFF59E0B) else Color(0xFF16A34A),
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "8+ days late",
                    value = backlog.overdue8Plus.toString(),
                    valueColor = if (backlog.overdue8Plus > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.CalendarMonth,
                    iconTint = if (backlog.overdue8Plus > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun InsightsRecommendationsCard(recommendations: List<String>) {
    if (recommendations.isEmpty()) return
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Planner recommendations", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            recommendations.forEach { recommendation ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConsistencyStreakCard(consistency: com.safarparmar.app.ui.studyplanner.logic.PlannerInsightConsistency) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors), MaterialTheme.shapes.extraLarge),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (consistency.studyStreak > 0) Icons.Rounded.Whatshot else Icons.Rounded.EmojiEvents,
                    contentDescription = "Streak",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (consistency.studyStreak > 0) "${consistency.studyStreak} Day Streak!" else "Start Your Study Streak!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${consistency.activeDaysLast14} active days of last 14",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
                if (consistency.bestStudyWeekday.isNotEmpty() && consistency.studyStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Best day: ${consistency.bestStudyWeekday}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

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
                    text = "Overall progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Syllabus Track",
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
                    label = "Exam in",
                    value = days?.let { "$it days" } ?: "—",
                    valueColor = if (days != null && days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.CalendarMonth,
                    iconTint = if (days != null && days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                InsightsInnerStat(
                    label = "Topics / day",
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
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Subject progress",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${rows.size} subject${if (rows.size == 1) "" else "s"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rows.isEmpty()) {
                Text(
                    "Add topics to see subject progress.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEachIndexed { idx, row ->
                    val colorIdx = subjectIndexById[row.subjectId] ?: 0
                    SubjectProgressRow(row, colorIdx, max(1, subjectCount))
                    if (idx < rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
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
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${row.completionPercent}%",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)), CircleShape),
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

@Composable
internal fun NextBestActionsPanel(
    plan: StudyPlan,
    insights: PlannerInsights,
    days: Int?,
    actions: PlannerActions,
    modifier: Modifier = Modifier
) {
    val overdueCount = insights.backlog.overdueTotal
    val isBehind = insights.summary.onTrackStatus == InsightTrackStatus.BEHIND
    val examPassed = days != null && days < 0
    val examNotSet = plan.examDate.isNullOrBlank()

    val recommendations = remember(overdueCount, isBehind, examPassed, examNotSet, plan.id) {
        mutableListOf<StudyRecommendation>().apply {
            if (overdueCount > 0) {
                add(
                    StudyRecommendation(
                        id = "reschedule_overdue",
                        title = "Reschedule Overdue Tasks",
                        description = "You have $overdueCount overdue topic${if (overdueCount == 1) "" else "s"}. Tap below to automatically distribute them across your upcoming study days.",
                        actionLabel = "Auto-Schedule",
                        icon = Icons.Default.CalendarMonth,
                        iconTint = Color(0xFFEF4444),
                        onClick = { actions.autoDistribute(false, true) }
                    )
                )
            }
            if (isBehind) {
                add(
                    StudyRecommendation(
                        id = "adjust_pace",
                        title = "Extend/Flex Study Pace",
                        description = "Your schedule is currently overloaded. Redistribute your topics to reduce daily study stress and stay balanced.",
                        actionLabel = "Reschedule",
                        icon = Icons.Default.Refresh,
                        iconTint = Color(0xFFF59E0B),
                        onClick = { actions.autoDistribute(true, false) }
                    )
                )
            }
            if (examPassed || examNotSet) {
                add(
                    StudyRecommendation(
                        id = "update_exam_date",
                        title = "Update Exam Date",
                        description = if (examPassed) "Your exam date has passed. Please update it to recalculate your preparation pace." else "Please set your target exam date to plan your syllabus timelines accurately.",
                        actionLabel = "Update Date",
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFF3B82F6),
                        onClick = { /* date picker dialog is opened */ }
                    )
                )
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        PlannerExamDateFieldDialog(
            examDateIso = plan.examDate.orEmpty(),
            onExamDateChange = { newDate ->
                actions.updatePlan(
                    UpdatePlanRequest(
                        title = plan.title,
                        examType = plan.examType,
                        examDate = newDate.ifBlank { null },
                        dailyGoal = plan.dailyGoal ?: 3,
                        offDays = plan.offDays,
                    )
                )
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (recommendations.isEmpty()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Next Best Action: Keep Going!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your planner is fully optimized. Keep following your daily ekagra schedule to hit your target goals!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Next Best Actions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            recommendations.forEach { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    onActionClick = {
                        if (recommendation.id == "update_exam_date") {
                            showDatePicker = true
                        } else {
                            recommendation.onClick()
                        }
                    }
                )
            }
        }
    }
}

internal data class StudyRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val icon: ImageVector,
    val iconTint: Color,
    val onClick: () -> Unit
)

@Composable
internal fun RecommendationCard(
    recommendation: StudyRecommendation,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(recommendation.iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = recommendation.icon,
                    contentDescription = null,
                    tint = recommendation.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = recommendation.iconTint,
                        contentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Text(
                        text = recommendation.actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannerExamDateFieldDialog(
    examDateIso: String,
    onExamDateChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parsed = parsePlannerDate(examDateIso)
    val initialMillis = parsed?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val picked = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val today = LocalDate.now(ZoneOffset.UTC)
                return !picked.isBefore(today)
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ld = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onExamDateChange(ld.toString())
                    }
                    onDismiss()
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
