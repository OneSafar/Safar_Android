package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.progressPercentValue
import kotlin.math.roundToInt
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.PlannerOverflowMenu
import com.safarparmar.app.ui.studyplanner.components.PlannerOverflowMenuItem
import com.safarparmar.app.ui.studyplanner.components.subjectDotColor
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import com.safarparmar.app.ui.studyplanner.logic.plannerExamCountdownHeroNumber
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * The Home ("Today") tab as one flat surface.
 *
 * The previous screen stacked four bordered cards — a status hero, a separate
 * Daily To-Do card, a pill-tab switcher and the agenda list — for what is really
 * one continuous task. These pieces drop the containers and use hairlines and
 * whitespace instead, so the whole page reads as a single sheet.
 *
 * Colours all come from [PlannerFlatColors], which already resolves per
 * light/dark via LocalPlannerIsDarkTheme — the planner keeps its own coral
 * accent rather than taking on a fixed accent from the mock.
 */

/** Hairline rule — the only container this redesign uses. */
@Composable
internal fun PlanHairline(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PlannerFlatColors.BorderSoft.copy(alpha = alpha)),
    )
}

/** Wide-tracked uppercase label that opens the page. */
@Composable
internal fun PlanEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Page header: eyebrow on the left, a bare outlined overflow button on the
 * right, then the plan title on its own line.
 */
@Composable
internal fun PlanHomeHeader(
    planTitle: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanEyebrow("Study Planner", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Plan settings",
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = planTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = PlannerFlatColors.TextDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Hero: progress ring on the left, exam countdown and topics-done on the right,
 * bounded by hairlines top and bottom instead of sitting inside a card.
 */
@Composable
internal fun PlanHomeHero(
    plan: StudyPlan,
    progress: PlanProgress,
    modifier: Modifier = Modifier,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val examDays = daysUntil(plan.examDate)
    val animatedPercent by animateFloatAsState(
        targetValue = progress.completionPercent / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "planHomeRing",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            val trackColor = PlannerFlatColors.BorderSoft
            Canvas(modifier = Modifier.size(84.dp)) {
                val strokeWidth = 6.dp.toPx()
                // Inset by half the stroke: drawArc centres the stroke on the arc
                // bounds, so without this the outer half of the ring is clipped
                // off by the canvas edge.
                val inset = strokeWidth / 2f
                val arcTopLeft = Offset(inset, inset)
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedPercent,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "${progress.completionPercent}%",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plannerExamCountdownHeroNumber(examDays),
                    fontFamily = LoraFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = planHomeCountdownCaption(examDays),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${progress.doneTopics} of ${progress.totalTopics} topics done",
                fontSize = 12.5.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Sentence-case countdown caption — the flat hero reads as a phrase, not a stacked label. */
private fun planHomeCountdownCaption(days: Long?): String = when {
    days == null -> "set exam date"
    days < 0L -> "exam passed"
    days == 0L -> "exam is today"
    days == 1L -> "day until exam"
    else -> "days until exam"
}

/**
 * Four counts in a row, separated by vertical hairlines rather than boxed in.
 *
 * Each count is a real entry point, not just a readout: Today/Done switch the
 * tab below, Missed opens the missed-topics list and Upcoming jumps to the
 * Calendar. A stat with nothing behind it (count of 0) stays inert so the user
 * never taps into an empty screen.
 */
@Composable
internal fun PlanHomeStatStrip(
    todayCount: Int,
    overdueCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    modifier: Modifier = Modifier,
    onTodayClick: (() -> Unit)? = null,
    onOverdueClick: (() -> Unit)? = null,
    onUpcomingClick: (() -> Unit)? = null,
    onDoneClick: (() -> Unit)? = null,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val ink = PlannerFlatColors.TextDark
    data class Stat(
        val value: Int,
        val label: String,
        val color: Color,
        val onClick: (() -> Unit)?,
    )
    val stats = listOf(
        Stat(todayCount, "TODAY", accent, onTodayClick),
        Stat(overdueCount, "MISSED", if (overdueCount > 0) Color(0xFFEF4444) else ink, onOverdueClick),
        Stat(upcomingCount, "UPCOMING", ink, onUpcomingClick),
        Stat(completedCount, "DONE", ink, onDoneClick),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 18.dp),
    ) {
        stats.forEachIndexed { index, stat ->
            val (value, label, color) = Triple(stat.value, stat.label, stat.color)
            if (index > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(PlannerFlatColors.BorderSoft),
                )
            }
            // Only clickable when there's something to show — a 0 count would
            // otherwise open an empty list.
            val enabled = stat.onClick != null && stat.value > 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (enabled) {
                            Modifier.clickable { stat.onClick?.invoke() }
                        } else {
                            Modifier
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$value",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = color,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = PlannerFlatColors.TextMuted,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Daily to-do collapses to a single hairline-bounded row: accent dot, label and
 * a "3 of 5" count. Expanding reveals the existing checklist in place.
 */
@Composable
internal fun PlanHomeDailyTodoRow(
    doneCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    expandedContent: @Composable () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "planHomeTodoChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PlannerFlatColors.PrimaryAccent),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Daily to-do",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextDark,
                modifier = Modifier.weight(1f),
            )
            if (totalCount > 0) {
                Text(
                    text = "$doneCount of $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = PlannerFlatColors.TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(visible = expanded) { expandedContent() }
    }
}

/**
 * Today / Done as underlined text tabs — no pill backgrounds competing with the
 * accent elsewhere on the page.
 */
@Composable
internal fun PlanHomeTabs(
    activeTab: StudyPlannerTab,
    onTabSelected: (StudyPlannerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        StudyPlannerTab.TODAY to "Today",
        StudyPlannerTab.COMPLETED to "Done",
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        tabs.forEach { (tab, label) ->
            val selected = activeTab == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Size to the label, so the underline below doesn't inherit the
                // Row's loose width constraint and stretch across the screen.
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onTabSelected(tab) },
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) PlannerFlatColors.TextDark else PlannerFlatColors.TextMuted,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (selected) PlannerFlatColors.PrimaryAccent else Color.Transparent
                        ),
                )
            }
        }
    }
}

/** "Today's agenda" + a right-aligned count, on the page rather than in a card. */
@Composable
internal fun PlanHomeSectionHeader(
    title: String,
    trailing: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 11.5.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
            )
        }
    }
}

/**
 * One agenda row: a tappable circular checkbox, a subject dot, and the topic with
 * its subject/chapter subtext. A hairline above separates rows — no card and no
 * left accent bar.
 *
 * The mock shows no trailing control, but Focus-with-Ekagra / Edit / Replace /
 * Remove have no other entry point on this screen, so they stay behind a muted
 * overflow rather than being dropped.
 */
@Composable
internal fun PlanHomeTaskRow(
    ref: TopicRef,
    onDoneChange: (Boolean) -> Unit,
    onReplace: (() -> Unit)? = null,
    onRemoveFromToday: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val done = ref.topic.status == TopicStatus.DONE
    val needsRevision = ref.topic.status == TopicStatus.REVISION_NEEDED
    val dotColor = when {
        done -> Color(0xFF10B981)
        needsRevision -> Color(0xFFF97316)
        else -> subjectDotColor(ref.subject.color)
    }

    var showMenu by remember { mutableStateOf(false) }
    val hasMenu = onEdit != null || onReplace != null ||
        (onRemoveFromToday != null && !done) || (onFocus != null && !done)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circular checkbox — fills with the accent once complete
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(PlannerFlatColors.PrimaryAccent)
                    else Modifier.border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                )
                .clickable { onDoneChange(!done) },
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ref.topic.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = PlannerFlatColors.TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${ref.subject.name} · ${ref.chapter.name}",
                fontSize = 11.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (needsRevision) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "To revise",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF97316),
            )
        }
        if (hasMenu) {
            Spacer(Modifier.width(6.dp))
            Box {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Topic options",
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (onFocus != null && !done) {
                        PlannerOverflowMenuItem("Focus with Ekagra", icon = Icons.Default.Timer) {
                            showMenu = false; onFocus()
                        }
                    }
                    if (onEdit != null) {
                        PlannerOverflowMenuItem("Edit topic", icon = Icons.Default.Edit) {
                            showMenu = false; onEdit()
                        }
                    }
                    if (onReplace != null) {
                        PlannerOverflowMenuItem("Replace topic", icon = Icons.Default.SwapHoriz) {
                            showMenu = false; onReplace()
                        }
                    }
                    if (onRemoveFromToday != null && !done) {
                        PlannerOverflowMenuItem("Remove from today", icon = Icons.Default.RemoveCircleOutline) {
                            showMenu = false; onRemoveFromToday()
                        }
                    }
                }
            }
        }
    }
}

/** The two add-actions as plain text links under the agenda. */
@Composable
internal fun PlanHomeAddActions(
    onAddFromSyllabus: () -> Unit,
    onAddCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "+ Add a topic for today",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.PrimaryAccent,
            modifier = Modifier.clickable(onClick = onAddFromSyllabus),
        )
        Text(
            text = "+ Add custom",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextMuted,
            modifier = Modifier.clickable(onClick = onAddCustom),
        )
    }
}

/** Quiet one-line empty state, replacing the bordered empty card. */
@Composable
internal fun PlanHomeEmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = PlannerFlatColors.TextMuted,
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
    )
}
