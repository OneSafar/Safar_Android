package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.studyplanner.components.flatCard
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.studyplanner.components.subjectDotColor
import com.safarparmar.app.ui.studyplanner.logic.HeatmapCell
import com.safarparmar.app.ui.studyplanner.logic.PlannerInsightConsistency
import com.safarparmar.app.ui.studyplanner.logic.PlannerInsightSubjectRow
import com.safarparmar.app.util.bounceClick
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.TextButton
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics
import com.safarparmar.app.ui.studyplanner.logic.parsePlannerDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private fun formatTierSubtitle(raw: String): String =
    raw.replace("&", "and").replace(Regex("\\s+"), " ").trim()

private fun subjectLegendLabel(fullName: String): String =
    subjectDisplayName(fullName).lowercase(Locale.getDefault())

private val ExamRingPurple = Color(0xFFB39DDB)
private val StreakPeach = Color(0xFFE8A88A)
private val GaugeOrange = Color(0xFFFF8A65)
private val GaugeAmber = Color(0xFFFFB300)
private val GaugeGreen = Color(0xFF66BB6A)
private val GaugeNeutral = Color(0xFF7C5AD9)

private val SubjectRingFallbackColors = listOf(
    Color(0xFFB39DDB),
    Color(0xFF66BB6A),
    Color(0xFF42A5F5),
    Color(0xFFEF5350),
    Color(0xFFFFB300),
)

/** Shared exam-tier suffix like "(Tier 1 & 2)" — strip from row labels, show once. */
private val ExamTierSuffixRegex = Regex(
    """\s*\(\s*Tier\s*\d+(?:\s*&\s*\d+)*\s*\)\s*$""",
    RegexOption.IGNORE_CASE,
)

private fun subjectDisplayName(fullName: String): String =
    fullName.replace(ExamTierSuffixRegex, "").trim().ifBlank { fullName }

private val FinishLineGreen = Color(0xFF66BB6A)
private val FinishLinePink = Color(0xFFE47AB5)
private val FinishLineRed = Color(0xFFEF5350)
private val RevisionDoneTeal = Color(0xFF26A69A)
private val RevisionPendingOrange = Color(0xFFFFB300)
private val RevisionSpikeGold = Color(0xFFE8C547)
// Manual (custom-date) revision uses a distinct violet ring so it reads
// differently from spaced revision — matching the Revision screen's cards.
private val RevisionCustomViolet = Color(0xFF8B5CF6)

private fun formatShortMonthDay(iso: String?): String? {
    val date = parsePlannerDate(iso?.take(10)) ?: return null
    return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}

private fun sharedExamTierLabel(subjects: List<PlannerInsightSubjectRow>): String? {
    val suffixes = subjects.mapNotNull { row ->
        ExamTierSuffixRegex.find(row.subjectName)?.value?.trim()?.removeSurrounding("(", ")")?.trim()
    }
    return suffixes.distinct().singleOrNull()?.let(::formatTierSubtitle)
}

@Composable
internal fun rememberStaggeredEntrance(delayMs: Int = 0): Float {
    // Fresh Animatable each composition entry — InsightsTab is disposed when leaving the tab.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
    }
    return progress.value
}

internal fun Modifier.insightsEntrance(progress: Float): Modifier = this.graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * 12f
}

@Composable
internal fun rememberCountUp(target: Int, durationMs: Int = 900, delayMs: Int = 0): Int {
    val anim = remember { Animatable(0f) }
    // Unit key: re-run every time this screen enters composition (tab revisit).
    LaunchedEffect(Unit, target) {
        anim.snapTo(0f)
        if (delayMs > 0) delay(delayMs.toLong())
        anim.animateTo(
            target.toFloat(),
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        )
    }
    return anim.value.roundToInt()
}

@Composable
internal fun InsightsOverallProgressRedesign(
    overallProgressPercent: Int,
    dailyTodoProgressPercent: Int,
    doneTopics: Int,
    totalTopics: Int,
    isLight: Boolean = false,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedTodo = dailyTodoProgressPercent

    val progress = overallProgressPercent.coerceIn(0, 100)
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 0)
    val ringProgress = remember { Animatable(0f) }
    val counted = rememberCountUp(progress, durationMs = 1000, delayMs = 80)

    LaunchedEffect(Unit, progress) {
        ringProgress.snapTo(0f)
        delay(80)
        ringProgress.animateTo(
            progress / 100f,
            animationSpec = tween(1100, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .safarFrostedPanel(
                isLight = isLight,
                shape = RoundedCornerShape(SafarGlassChromeRadius),
            )
            .padding(vertical = 24.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(156.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(156.dp)) {
                    val stroke = 10.dp.toPx()
                    // Faint purple track so 0% still reads as "a ring waiting to fill".
                    drawArc(
                        color = ExamRingPurple.copy(alpha = if (isLight) 0.18f else 0.28f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = ExamRingPurple,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.value,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "$counted%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = primaryText,
                    )
                    Text(
                        text = "syllabus done",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText,
                    )
                }
            }
            // "12 of 40 topics done" used to sit directly under the ring, but the
            // ring is effort-weighted (a big topic counts for more) so the two
            // numbers disagreed — teaching the student that the app's maths can't
            // be trusted. Now the caption states what is finished and what is
            // left, and one plain line explains why the % isn't just done/total.
            Text(
                text = "$doneTopics done · ${(totalTopics - doneTopics).coerceAtLeast(0)} to go",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Big topics count more",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun InsightsMetricSquares(examDays: Int?, dailyGoal: Int, isLight: Boolean = false) {
    val tint = if (isLight) Color.Black else Color.White
    val tintAlpha = if (isLight) 0.04f else 0.05f
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 120)
    val daysTarget = examDays?.coerceAtLeast(0) ?: 0
    val daysCounted = rememberCountUp(daysTarget, durationMs = 900, delayMs = 160)
    val goalCounted = rememberCountUp(dailyGoal, durationMs = 900, delayMs = 220)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricSquareCard(
            icon = Icons.Default.Timer,
            value = if (examDays == null) "—" else "$daysCounted",
            label = "days to exam",
            primaryText = primaryText,
            secondaryText = secondaryText,
            tint = tint,
            tintAlpha = tintAlpha,
            isLight = isLight,
            modifier = Modifier.weight(1f),
        )
        MetricSquareCard(
            icon = Icons.Default.TrackChanges,
            value = "$goalCounted",
            label = "daily goal",
            primaryText = primaryText,
            secondaryText = secondaryText,
            tint = tint,
            tintAlpha = tintAlpha,
            isLight = isLight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricSquareCard(
    icon: ImageVector,
    value: String,
    label: String,
    primaryText: Color,
    secondaryText: Color,
    tint: Color,
    tintAlpha: Float,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryText.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = secondaryText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ConsistencyStreakCard(
    consistency: PlannerInsightConsistency,
    isLight: Boolean = false,
) {
    val tint = if (isLight) Color.Black else Color.White
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 240)
    val haptic = LocalHapticFeedback.current
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val flamePulse = rememberInfiniteTransition(label = "streakFlame")
    val flameScale by flamePulse.animateFloat(
        initialValue = 1f,
        targetValue = if (consistency.studyStreak > 0) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streakFlameScale",
    )
    val selectedCell = consistency.heatmap.takeLast(7).firstOrNull { it.date == selectedDate }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .animateContentSize()
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Whatshot,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = flameScale
                            scaleY = flameScale
                        },
                )
                Text(
                    text = if (consistency.studyStreak > 0) {
                        "Studied ${consistency.studyStreak} day${if (consistency.studyStreak == 1) "" else "s"} in a row"
                    } else {
                        "Study today to begin"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Tap a day",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText,
                    maxLines = 1,
                )
            }

            TappableWeekStreak(
                heatmap = consistency.heatmap,
                selectedDate = selectedDate,
                isLight = isLight,
                onDayTap = { date ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedDate = if (selectedDate == date) null else date
                },
            )

            AnimatedVisibility(
                visible = selectedCell != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val cell = selectedCell ?: return@AnimatedVisibility
                val date = runCatching { LocalDate.parse(cell.date) }.getOrNull()
                val dayName = date?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: cell.date
                Text(
                    text = if (cell.count > 0) {
                        "$dayName · ${cell.count} task${if (cell.count == 1) "" else "s"} done"
                    } else {
                        "$dayName · nothing done"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                )
            }
        }
    }
}

@Composable
private fun TappableWeekStreak(
    heatmap: List<HeatmapCell>,
    selectedDate: String?,
    isLight: Boolean,
    onDayTap: (String) -> Unit,
) {
    val days = heatmap.takeLast(7)
    if (days.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { cell ->
            val date = runCatching { LocalDate.parse(cell.date) }.getOrNull()
            val dayLabel = date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.take(1) ?: "?"
            val active = cell.count > 0
            val selected = selectedDate == cell.date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick(scaleDown = 0.88f) { onDayTap(cell.date) }
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (active) StreakPeach else Color.Transparent)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = when {
                                selected -> StreakPeach
                                active -> Color.Transparent
                                isLight -> Color.Black.copy(alpha = 0.18f)
                                else -> Color.White.copy(alpha = 0.22f)
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Studied",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = PlannerFlatColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Compact pace like "2", "1.8" — one decimal, trailing ".0" stripped. */
private fun formatPace(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

/**
 * "Study pace" — compares the student's *actual* recent completion rate
 * ([recentTopicsPerDay], real ticks over the last two weeks) against what the
 * plan needs ([requiredPerDay]). This is the honest speedometer: a big goal on
 * an untouched plan no longer reads as "fast". The gauge fills to actual÷needed,
 * and colour reflects whether the real pace keeps up with the requirement.
 */
@Composable
internal fun InsightsStudySpeedCard(
    recentTopicsPerDay: Float?,
    requiredPerDay: Float?,
    dailyGoal: Int,
    isLight: Boolean = false,
) {
    val panelTint = if (isLight) Color.Black else Color.White
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 360)

    val hasRecentPace = recentTopicsPerDay != null && recentTopicsPerDay > 0f
    val noExam = requiredPerDay == null
    val nothingLeft = requiredPerDay != null && requiredPerDay <= 0f

    // Reference the pace is measured against: the requirement when an exam date
    // exists, otherwise the daily goal so the gauge still means something.
    val reference = when {
        nothingLeft -> 0f
        requiredPerDay != null -> requiredPerDay
        else -> dailyGoal.toFloat().coerceAtLeast(1f)
    }
    val ratio = when {
        nothingLeft -> 1f
        !hasRecentPace -> 0f
        reference <= 0f -> 1f
        else -> (recentTopicsPerDay!! / reference)
    }
    val gaugeFraction = ratio.coerceIn(0f, 1f)

    val accent = when {
        !hasRecentPace && !nothingLeft -> GaugeNeutral
        ratio >= 1f -> GaugeGreen
        ratio >= 0.7f -> GaugeAmber
        else -> GaugeOrange
    }

    val centerText = when {
        nothingLeft -> "Done"
        hasRecentPace -> formatPace(recentTopicsPerDay!!)
        else -> "—"
    }
    val subtitle = when {
        nothingLeft -> "Nothing left to schedule — you're ahead."
        noExam && hasRecentPace ->
            "You do about ${formatPace(recentTopicsPerDay!!)} work/day · goal $dailyGoal/day. Add your exam date to check your plan."
        noExam ->
            "Add your exam date and finish some work to check your plan."
        !hasRecentPace ->
            "No work finished in the last 2 weeks · need ${formatPace(requiredPerDay!!)}/day."
        else ->
            "You do about ${formatPace(recentTopicsPerDay!!)} work/day · need ${formatPace(requiredPerDay!!)}/day."
    }

    val arcProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit, gaugeFraction) {
        arcProgress.snapTo(0f)
        delay(400)
        arcProgress.animateTo(gaugeFraction, animationSpec = tween(1100, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Daily study",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = primaryText,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter),
                ) {
                    val stroke = 12.dp.toPx()
                    val usableWidth = size.width * 0.78f
                    val diameter = usableWidth.coerceAtMost(size.height * 2f) - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, size.height - diameter / 2f)
                    val arcSize = Size(diameter, diameter)
                    val start = 180f
                    val sweep = 180f
                    drawArc(
                        color = accent.copy(alpha = if (isLight) 0.16f else 0.22f),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = accent,
                        startAngle = start,
                        sweepAngle = sweep * arcProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(
                    modifier = Modifier.padding(bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = centerText,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = primaryText,
                    )
                    if (hasRecentPace && !nothingLeft) {
                        Text(
                            text = "work/day",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText,
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}

@Composable
internal fun SubjectProgressChart(
    subjects: List<PlannerInsightSubjectRow>,
    isLight: Boolean = false,
) {
    val chartSubjects = remember(subjects) { subjects.take(4) }
    var selectedSubjectId by remember(chartSubjects) { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val panelTint = if (isLight) Color.Black else Color.White
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 480)
    val tierLabel = remember(chartSubjects) { sharedExamTierLabel(chartSubjects) }
    val selectedSubject = chartSubjects.firstOrNull { it.subjectId == selectedSubjectId }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .animateContentSize()
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "By subject",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = primaryText,
                )
                if (tierLabel != null) {
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                    )
                }
            }
            if (chartSubjects.isEmpty()) {
                Text(text = "Add topics to see progress here.", color = secondaryText)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackVertically = maxWidth < 340.dp
                    val ringSize = if (stackVertically) 132.dp else 120.dp

                    if (stackVertically) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            ConcentricSubjectRings(
                                subjects = chartSubjects,
                                selectedSubjectId = selectedSubjectId,
                                isLight = isLight,
                                modifier = Modifier.size(ringSize),
                            )
                            SubjectLegendColumn(
                                subjects = chartSubjects,
                                selectedSubjectId = selectedSubjectId,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                isLight = isLight,
                                onSelect = { id ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedSubjectId = if (selectedSubjectId == id) null else id
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            ConcentricSubjectRings(
                                subjects = chartSubjects,
                                selectedSubjectId = selectedSubjectId,
                                isLight = isLight,
                                modifier = Modifier.size(ringSize),
                            )
                            SubjectLegendColumn(
                                subjects = chartSubjects,
                                selectedSubjectId = selectedSubjectId,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                isLight = isLight,
                                onSelect = { id ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedSubjectId = if (selectedSubjectId == id) null else id
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedSubject != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val row = selectedSubject ?: return@AnimatedVisibility
                    val color = subjectRingColor(row, chartSubjects.indexOf(row).coerceAtLeast(0))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = if (isLight) 0.08f else 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = row.subjectName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                        )
                        Text(
                            text = when {
                                row.remainingTopics <= 0 -> "All topics done"
                                row.overdueTopics > 0 ->
                                    "${row.remainingTopics} left · ${row.overdueTopics} late"
                                else ->
                                    "${row.remainingTopics} topic${if (row.remainingTopics == 1) "" else "s"} left"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectLegendColumn(
    subjects: List<PlannerInsightSubjectRow>,
    selectedSubjectId: String?,
    primaryText: Color,
    secondaryText: Color,
    isLight: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        subjects.forEachIndexed { index, row ->
            val color = subjectRingColor(row, index)
            val selected = selectedSubjectId == row.subjectId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) color.copy(alpha = if (isLight) 0.10f else 0.16f)
                        else Color.Transparent,
                    )
                    .bounceClick(scaleDown = 0.96f) { onSelect(row.subjectId) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = subjectLegendLabel(row.subjectName),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${row.completionPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 28.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ConcentricSubjectRings(
    subjects: List<PlannerInsightSubjectRow>,
    selectedSubjectId: String?,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val ring0 = remember { Animatable(0f) }
    val ring1 = remember { Animatable(0f) }
    val ring2 = remember { Animatable(0f) }
    val ring3 = remember { Animatable(0f) }
    val ringAnims = listOf(ring0, ring1, ring2, ring3)
    val targets = List(4) { index ->
        subjects.getOrNull(index)?.let { (it.completionPercent / 100f).coerceIn(0f, 1f) } ?: 0f
    }
    val subjectKeys = List(4) { index -> subjects.getOrNull(index)?.subjectId.orEmpty() }

    // Unit + keys: re-animate every tab entry even when percentages are still 0.
    LaunchedEffect(Unit, subjectKeys[0], targets[0]) {
        delay(0)
        ring0.snapTo(0f)
        ring0.animateTo(targets[0], tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit, subjectKeys[1], targets[1]) {
        delay(80)
        ring1.snapTo(0f)
        ring1.animateTo(targets[1], tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit, subjectKeys[2], targets[2]) {
        delay(160)
        ring2.snapTo(0f)
        ring2.animateTo(targets[2], tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit, subjectKeys[3], targets[3]) {
        delay(240)
        ring3.snapTo(0f)
        ring3.animateTo(targets[3], tween(900, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val stroke = 10.dp.toPx()
        val gap = 5.dp.toPx()
        val maxRadius = size.minDimension / 2f - stroke / 2f
        subjects.forEachIndexed { index, row ->
            val color = subjectRingColor(row, index)
            val radius = maxRadius - index * (stroke + gap)
            if (radius <= stroke) return@forEachIndexed
            val diameter = radius * 2f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val selected = selectedSubjectId == row.subjectId
            val width = if (selected) stroke + 1.5.dp.toPx() else stroke
            // Tinted track so 0% rings still show subject identity.
            drawArc(
                color = color.copy(alpha = if (isLight) 0.16f else 0.22f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            val fraction = ringAnims[index].value
            if (fraction > 0.001f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun subjectRingColor(row: PlannerInsightSubjectRow, index: Int): Color {
    val fromPlan = subjectDotColor(row.subjectColor)
    return if (row.subjectColor.isNotBlank() && fromPlan != Color(0xFF0EA5E9)) {
        fromPlan
    } else {
        SubjectRingFallbackColors[index % SubjectRingFallbackColors.size]
    }
}

@Composable
internal fun InsightsFinishLineCard(
    examDateIso: String?,
    forecastDateIso: String?,
    studyDaysLeft: Int?,
    /**
     * True when [forecastDateIso] was projected from the student's real recent
     * pace; false when it falls back to "if you hit your daily goal". Drives the
     * card's copy so the projection never over-claims what it actually measured.
     */
    basedOnRecentPace: Boolean = false,
    isLight: Boolean = false,
) {
    val panelTint = if (isLight) Color.Black else Color.White
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 420)

    val today = remember { LocalDate.now() }
    val examDate = remember(examDateIso) { parsePlannerDate(examDateIso?.take(10)) }
    val forecastDate = remember(forecastDateIso) { parsePlannerDate(forecastDateIso?.take(10)) }

    val timeline = remember(today, examDate, forecastDate) {
        if (examDate == null || examDate.isBefore(today)) return@remember null
        val endDate = when {
            forecastDate != null && forecastDate.isAfter(examDate) -> forecastDate
            else -> examDate
        }
        val totalDays = ChronoUnit.DAYS.between(today, endDate).coerceAtLeast(1)
        val examDays = ChronoUnit.DAYS.between(today, examDate).coerceAtLeast(0)
        val forecastDays = forecastDate?.let { ChronoUnit.DAYS.between(today, it).coerceAtLeast(0) } ?: examDays
        FinishLineTimeline(
            examFraction = (examDays.toFloat() / totalDays.toFloat()).coerceIn(0.05f, 1f),
            projectedFraction = (forecastDays.toFloat() / totalDays.toFloat()).coerceIn(0.05f, 1f),
            isBehind = forecastDate != null && forecastDate.isAfter(examDate),
            daysAfterExam = if (forecastDate != null && forecastDate.isAfter(examDate)) {
                ChronoUnit.DAYS.between(examDate, forecastDate).toInt()
            } else {
                null
            },
            examShortLabel = formatShortMonthDay(examDateIso),
            projectedShortLabel = formatShortMonthDay(forecastDateIso),
        )
    }

    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit, timeline) {
        lineProgress.snapTo(0f)
        if (timeline != null) {
            delay(200)
            lineProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = FinishLinePink,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "When you'll finish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = primaryText,
                    modifier = Modifier.weight(1f),
                )
                studyDaysLeft?.let { days ->
                    Text(
                        text = "$days day${if (days == 1) "" else "s"} left to study",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = if (basedOnRecentPace) "Based on your recent study" else "If you complete your daily goal",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
            )

            if (timeline == null) {
                Text(
                    text = "Add exam date to see when you'll finish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                )
            } else {
                FinishLineTimelineCanvas(
                    timeline = timeline,
                    progress = lineProgress.value,
                    isLight = isLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                )

                timeline.daysAfterExam?.let { lateDays ->
                    var showLateDetails by remember(lateDays, basedOnRecentPace) { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                FinishLineRed.copy(alpha = if (isLight) 0.10f else 0.16f),
                            )
                            .clickable { showLateDetails = !showLateDetails }
                            .animateContentSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "You may not finish the whole syllabus before the exam",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FinishLineRed,
                            lineHeight = 18.sp,
                        )
                        if (!showLateDetails) {
                            Text(
                                text = "Tap to know more",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = FinishLineRed.copy(alpha = 0.78f),
                            )
                        }
                        AnimatedVisibility(
                            visible = showLateDetails,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Text(
                                text = if (basedOnRecentPace) {
                                    "At your current study speed, you may finish about $lateDays day${if (lateDays == 1) "" else "s"} after the exam."
                                } else {
                                    "Even if you hit your daily goal, you may finish about $lateDays day${if (lateDays == 1) "" else "s"} after the exam."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = FinishLineRed,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                } ?: run {
                    val aheadDays = if (
                        forecastDate != null &&
                        examDate != null &&
                        !forecastDate.isAfter(examDate)
                    ) {
                        ChronoUnit.DAYS.between(forecastDate, examDate).toInt()
                    } else {
                        null
                    }
                    if (aheadDays != null && aheadDays > 0) {
                        Text(
                            text = "On track — $aheadDays day${if (aheadDays == 1) "" else "s"} before exam",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FinishLineGreen,
                        )
                    }
                }
            }
        }
    }
}

private data class FinishLineTimeline(
    val examFraction: Float,
    val projectedFraction: Float,
    val isBehind: Boolean,
    val daysAfterExam: Int?,
    val examShortLabel: String?,
    val projectedShortLabel: String?,
)

@Composable
private fun FinishLineTimelineCanvas(
    timeline: FinishLineTimeline,
    progress: Float,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f)
    val secondaryText = PlannerFlatColors.TextMuted

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.TopCenter),
        ) {
            val y = size.height / 2f
            val startX = 8.dp.toPx()
            val endX = size.width - 8.dp.toPx()
            val width = endX - startX
            val examX = startX + width * timeline.examFraction * progress
            val projectedX = startX + width * timeline.projectedFraction * progress

            drawLine(trackColor, Offset(startX, y), Offset(endX, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)

            if (timeline.isBehind) {
                drawLine(
                    FinishLineGreen,
                    Offset(startX, y),
                    Offset(examX, y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    FinishLineRed,
                    Offset(examX, y),
                    Offset(projectedX, y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                drawLine(
                    FinishLineGreen,
                    Offset(startX, y),
                    Offset(min(projectedX, examX), y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            drawCircle(FinishLineGreen, radius = 5.dp.toPx(), center = Offset(startX, y))
            drawCircle(FinishLinePink, radius = 5.dp.toPx(), center = Offset(examX, y))
            if (timeline.isBehind) {
                drawCircle(FinishLineRed, radius = 5.dp.toPx(), center = Offset(projectedX, y))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Today", style = MaterialTheme.typography.labelSmall, color = secondaryText)
            timeline.examShortLabel?.let { label ->
                Text(
                    text = "Exam · $label",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinishLinePink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (timeline.isBehind && timeline.projectedShortLabel != null) {
            Text(
                text = "Finish · ${timeline.projectedShortLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = FinishLineRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

@Composable
internal fun InsightsRevisionPulseCard(
    plan: StudyPlan,
    actions: PlannerActions,
    isLight: Boolean = false,
) {
    val towers = remember(plan.subjects) {
        plan.flattenTopics()
            .filter { it.topic.status == TopicStatus.REVISION_NEEDED }
            .map { ref ->
                val completed = ref.topic.revisionCompletedDates.orEmpty().map { d -> d.take(10) }
                    .filter { d -> d.isNotBlank() }.toSet()
                val remaining = ref.topic.revisionReminderDates.map { d -> d.take(10) }
                    .filter { d -> d.isNotBlank() }.toSet()
                val total = (completed + remaining).size.coerceAtLeast(1)
                RevisionTowerUi(
                    ref = ref,
                    total = total,
                    done = completed.size.coerceIn(0, total),
                    isCustom = ref.topic.revisionScheduleType == "custom",
                )
            }
            // Most-progressed towers first so the "almost there" ones lead the eye.
            .sortedWith(
                compareByDescending<RevisionTowerUi> { it.done.toFloat() / it.total }
                    .thenBy { it.ref.topic.plannedDate?.take(10).orEmpty().ifBlank { "9999-99-99" } }
                    .thenBy { it.ref.topic.name.lowercase() },
            )
    }
    val doneSessions = towers.sumOf { it.done }
    val totalSessions = towers.sumOf { it.total }
    val remainingSessions = (totalSessions - doneSessions).coerceAtLeast(0)

    var selectedIndex by remember(towers) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val panelTint = if (isLight) Color.Black else Color.White
    val primaryText = PlannerFlatColors.TextDark
    val secondaryText = PlannerFlatColors.TextMuted
    val entrance = rememberStaggeredEntrance(delayMs = 560)
    val towerProgress = remember { Animatable(0f) }
    LaunchedEffect(towers.size, doneSessions) {
        towerProgress.snapTo(0f)
        if (towers.isNotEmpty()) {
            delay(120)
            towerProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .insightsEntrance(entrance)
            .animateContentSize()
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MonitorHeart,
                    contentDescription = null,
                    tint = RevisionDoneTeal,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Revision",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = primaryText,
                    )
                    Text(
                        text = if (towers.isEmpty()) {
                            "Mark topics to revise. They'll show up here."
                        } else if (towers.size > 6) {
                            "Swipe to see all · tap a topic"
                        } else {
                            "Each ring is one revision · tap a topic"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText,
                    )
                }
            }

            if (towers.isNotEmpty()) {
                RevisionSpikeRingsRow(
                    towers = towers,
                    selectedIndex = selectedIndex,
                    drawProgress = towerProgress.value,
                    isLight = isLight,
                    onTowerTap = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIndex = if (selectedIndex == index) null else index
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RevisionPulseLegendChip("Revised", doneSessions.toString(), RevisionDoneTeal)
                    RevisionPulseLegendChip("Left", remainingSessions.toString(), RevisionPendingOrange)
                }

                AnimatedVisibility(
                    visible = selectedIndex != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val tower = selectedIndex?.let { towers.getOrNull(it) } ?: return@AnimatedVisibility
                    val ref = tower.ref
                    val detailAccent = if (tower.isCustom) RevisionCustomViolet else RevisionDoneTeal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(detailAccent.copy(alpha = if (isLight) 0.08f else 0.12f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = ref.topic.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                        )
                        Text(
                            text = "${ref.subject.name} · ${ref.chapter.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append("${tower.done} of ${tower.total} revision${if (tower.total == 1) "" else "s"} done")
                                append(if (tower.isCustom) " · Custom date" else " · Spaced")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = detailAccent,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            RevisionSpikeRingMini(
                                total = tower.total,
                                done = tower.done,
                                isLight = isLight,
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(112.dp),
                                doneColor = detailAccent,
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { actions.openRevisionTopics() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "See all to revise",
                        color = PlannerFlatColors.PrimaryAccent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RevisionPulseLegendChip(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** One revision topic's spaced-revision progress for the spike-and-ring visual. */
private data class RevisionTowerUi(
    val ref: TopicRef,
    val total: Int,
    val done: Int,
    val isCustom: Boolean = false,
)

/** Horizontally scrollable spike + pill rings — one column per revision topic. */
@Composable
private fun RevisionSpikeRingsRow(
    towers: List<RevisionTowerUi>,
    selectedIndex: Int?,
    drawProgress: Float,
    isLight: Boolean,
    onTowerTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val towerWidth = 72.dp
    LazyRow(
        modifier = modifier.heightIn(min = 128.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        itemsIndexed(
            items = towers,
            key = { _, tower -> tower.ref.topic.id },
        ) { index, tower ->
            RevisionSpikeRingColumn(
                tower = tower,
                selected = selectedIndex == index,
                drawProgress = drawProgress,
                isLight = isLight,
                onTap = { onTowerTap(index) },
                modifier = Modifier.width(towerWidth),
            )
        }
    }
}

@Composable
private fun RevisionSpikeRingColumn(
    tower: RevisionTowerUi,
    selected: Boolean,
    drawProgress: Float,
    isLight: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spikeColor = when {
        selected -> RevisionPendingOrange
        else -> RevisionSpikeGold
    }
    // Completed rings: violet for manual (custom-date) revision, teal for spaced.
    val ringDoneColor = if (tower.isCustom) RevisionCustomViolet else RevisionDoneTeal
    val baseColor = spikeColor.copy(alpha = if (isLight) 0.85f else 0.95f)
    val selBg = if (selected) {
        RevisionPendingOrange.copy(alpha = if (isLight) 0.08f else 0.12f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(selBg)
            .bounceClick(scaleDown = 0.94f) { onTap() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
        ) {
            drawSpikePillRings(
                total = tower.total,
                done = tower.done,
                drawProgress = drawProgress,
                spikeColor = spikeColor,
                baseColor = baseColor,
                doneColor = ringDoneColor,
                pendingColor = RevisionPendingOrange.copy(alpha = if (isLight) 0.45f else 0.55f),
                animateDrop = true,
            )
        }

        Text(
            text = "${tower.done}/${tower.total}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = if (selected) RevisionPendingOrange else ringDoneColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun RevisionSpikeRingMini(
    total: Int,
    done: Int,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    doneColor: Color = RevisionDoneTeal,
) {
    Canvas(modifier = modifier) {
        drawSpikePillRings(
            total = total,
            done = done,
            drawProgress = 1f,
            spikeColor = RevisionSpikeGold,
            baseColor = RevisionSpikeGold.copy(alpha = 0.9f),
            doneColor = doneColor,
            pendingColor = RevisionPendingOrange.copy(alpha = if (isLight) 0.40f else 0.50f),
            animateDrop = false,
        )
    }
}

/**
 * Reference layout: gold triangular spike + filled pill rings (bottom widest),
 * spike visible above and between rings.
 */
private fun DrawScope.drawSpikePillRings(
    total: Int,
    done: Int,
    drawProgress: Float,
    spikeColor: Color,
    baseColor: Color,
    doneColor: Color,
    pendingColor: Color,
    animateDrop: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val baseH = 5.dp.toPx().coerceAtLeast(h * 0.055f)
    val baseY = h - baseH
    val tipY = 2.dp.toPx().coerceAtMost(h * 0.05f)
    val spikeFootHalf = 3.5.dp.toPx()

    // Gold spike — triangle behind the rings
    val spikePath = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx - spikeFootHalf, baseY)
        lineTo(cx + spikeFootHalf, baseY)
        close()
    }
    drawPath(spikePath, spikeColor)

    // Pedestal
    drawRoundRect(
        color = baseColor,
        topLeft = Offset(cx - w * 0.30f, baseY),
        size = Size(w * 0.60f, baseH),
        cornerRadius = CornerRadius(baseH / 2f, baseH / 2f),
    )

    val slots = total.coerceAtLeast(1)
    val stackBottom = baseY - 1.5.dp.toPx()
    val stackTop = tipY + h * 0.16f
    val slotH = ((stackBottom - stackTop) / slots).coerceAtLeast(7.dp.toPx())
    val pillH = (slotH * 0.74f).coerceIn(9.dp.toPx(), 17.dp.toPx())
    val maxPillW = w * 0.90f
    val minPillW = (w * 0.46f).coerceAtMost(maxPillW)

    fun pillWidth(slotIndex: Int): Float {
        if (slots <= 1) return maxPillW
        val rise = slotIndex.toFloat() / (slots - 1).coerceAtLeast(1)
        return maxPillW - (maxPillW - minPillW) * rise
    }

    // Pending rings — outline pills above the done stack
    for (i in done until slots) {
        val cy = stackBottom - (i + 0.5f) * slotH
        val pillW = pillWidth(i)
        drawRoundRect(
            color = pendingColor,
            topLeft = Offset(cx - pillW / 2f, cy - pillH / 2f),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    // Done rings — solid pills, bottom-first drop
    val dropped = done * drawProgress
    for (i in 0 until done) {
        val ringProgress = if (animateDrop) (dropped - i).coerceIn(0f, 1f) else 1f
        if (ringProgress <= 0f) continue

        val restY = stackBottom - (i + 0.5f) * slotH
        val fromY = stackTop - pillH
        val cy = if (animateDrop) fromY + (restY - fromY) * ringProgress else restY
        val pillW = pillWidth(i)

        drawRoundRect(
            color = doneColor.copy(alpha = ringProgress),
            topLeft = Offset(cx - pillW / 2f, cy - pillH / 2f),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
        )
    }
}
