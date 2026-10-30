package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Settings
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

/** Warm Clarity presentation for Today, using the existing planner callbacks. */

@Composable
internal fun PlanHairline(modifier: Modifier = Modifier, alpha: Float = 1f) {
    if (com.safarparmar.app.ui.studyplanner.components.LocalPlannerWarmClarity.current) {
        Spacer(modifier.height(8.dp))
        return
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PlannerFlatColors.BorderSoft.copy(alpha = alpha)),
    )
}

/** Compact section label. */
@Composable
internal fun PlanEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
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
    onSelectExam: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f), modifier = Modifier.weight(1f)) {
                TextButton(onClick = { onSelectExam?.invoke() }, enabled = onSelectExam != null) {
                    Text(planTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, color = PlannerFlatColors.TextDark, style = MaterialTheme.typography.titleMedium)
                }
            }
            TextButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.planner_plan_settings), style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.planner_warm_today), style = MaterialTheme.typography.headlineMedium, color = PlannerFlatColors.TextDark)
            Text(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(androidx.compose.ui.platform.LocalLocale.current.platformLocale)),
                style = MaterialTheme.typography.bodySmall, color = PlannerFlatColors.TextMuted)
        }
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
            val trackColor = PlannerFlatColors.ProgressTrack
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
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plannerExamCountdownHeroNumber(examDays),
                    fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
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
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { stat.onClick?.invoke() }
                        } else {
                            Modifier
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$value",
                    fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
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
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleExpanded)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .padding(vertical = 16.dp, horizontal = 14.dp),
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
                text = stringResource(R.string.planner_daily_todo),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
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
        StudyPlannerTab.TODAY to stringResource(R.string.planner_warm_today),
        StudyPlannerTab.COMPLETED to stringResource(R.string.planner_warm_done),
    )
    Row(modifier = modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (tab, label) ->
            val selected = activeTab == tab
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) }),
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
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

/** Topic card with completion, visible Focus and the existing editing actions. */
@Composable
internal fun PlanHomeTaskRow(
    ref: TopicRef,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onReplace: (() -> Unit)? = null,
    onRemoveFromToday: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
) {
    val done = ref.topic.status.isStudied
    val needsRevision = ref.topic.status == TopicStatus.REVISION_NEEDED
    var showMenu by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = onDoneChange,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = ref.topic.name },
                    colors = CheckboxDefaults.colors(checkedColor = scheme.secondary, uncheckedColor = scheme.outline))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(ref.topic.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    Text("${ref.subject.name} · ${ref.chapter.name}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (needsRevision) Text(stringResource(R.string.planner_to_revise), style = MaterialTheme.typography.labelMedium, color = scheme.tertiary)
                }
                if (onFocus != null && (!done || needsRevision)) Button(onClick = onFocus, modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(18.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.planner_warm_focus))
                }
                if (onEdit != null || onReplace != null || onRemoveFromToday != null) Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.planner_topic_options), Modifier.size(20.dp)) }
                    PlannerOverflowMenu(showMenu, { showMenu = false }) {
                        onEdit?.let { action -> PlannerOverflowMenuItem(stringResource(R.string.planner_warm_edit_topic), icon = Icons.Default.Edit) { showMenu = false; action() } }
                        onReplace?.let { action -> PlannerOverflowMenuItem(stringResource(R.string.planner_warm_replace_topic), icon = Icons.Default.SwapHoriz) { showMenu = false; action() } }
                        if (!done) onRemoveFromToday?.let { action -> PlannerOverflowMenuItem(stringResource(R.string.planner_warm_remove_today), icon = Icons.Default.RemoveCircleOutline) { showMenu = false; action() } }
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
    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        FilledTonalButton(onClick = onAddFromSyllabus, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.planner_add_topic_today_plus))
        }
        TextButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.planner_add_custom_plus)) }
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

@Composable
internal fun PlanHomeTodaySummary(
    done: Int, total: Int, revision: Int, missed: Int,
    onRevision: () -> Unit, onMissed: () -> Unit, onUpcoming: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.planner_warm_tasks_done, done, total), style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = scheme.secondary, trackColor = scheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onRevision) { Text(stringResource(R.string.planner_warm_revision_count, revision)) }
                if (missed > 0) TextButton(onClick = onMissed) { Text(stringResource(R.string.planner_warm_missed_count, missed), color = scheme.error) }
                else TextButton(onClick = onUpcoming) { Text(stringResource(R.string.planner_warm_upcoming)) }
            }
        }
    }
}
