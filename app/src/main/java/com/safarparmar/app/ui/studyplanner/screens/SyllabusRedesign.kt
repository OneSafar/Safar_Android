package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material.icons.filled.DragHandle
import com.safarparmar.app.ui.studyplanner.logic.isLate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.effectiveSize
import com.safarparmar.app.domain.model.studyplanner.progressPercentValue
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.PlannerOverflowMenu
import com.safarparmar.app.ui.studyplanner.components.PlannerOverflowMenuItem
import com.safarparmar.app.ui.studyplanner.components.TopicEffortBars
import com.safarparmar.app.ui.studyplanner.components.glassSurface
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.ui.studyplanner.logic.NodeProgress
import com.safarparmar.app.ui.studyplanner.logic.NodeState
import com.safarparmar.app.ui.studyplanner.logic.SyllabusReadinessStatus
import androidx.compose.ui.res.pluralStringResource
import com.safarparmar.app.ui.studyplanner.logic.progressState
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/** Callbacks for the one action the readiness card can offer. */
internal class SyllabusReadinessActions(
    val onCreateDates: () -> Unit = {},
    val onSetExamDate: () -> Unit = {},
    val onGiveDates: () -> Unit = {},
    val onUpdateDates: () -> Unit = {},
    val onDismissRatings: () -> Unit = {},
    val onCatchUp: () -> Unit = {},
    val onStudyMore: () -> Unit = {},
)

/**
 * The top of the Syllabus tab: how much is covered, how long is left, and ONE
 * status line with at most ONE button (see [syllabusReadiness] for the order).
 * Replaces the old progress card, days-left link, "Change your study plan"
 * button, ratings bar and course-map nudge.
 */
@Composable
internal fun SyllabusReadinessCard(
    completionPercent: Int,
    doneTopics: Int,
    totalTopics: Int,
    daysUntilExam: Int?,
    status: SyllabusReadinessStatus,
    busy: Boolean,
    actions: SyllabusReadinessActions,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val warn = Color(0xFFC2410C)
    val (message, actionLabel, onAction, isWarning) = when (status) {
        SyllabusReadinessStatus.ReorderThenCreateDates -> ReadinessLine(
            stringResource(R.string.planner_armed_rebuild_body), stringResource(R.string.planner_armed_rebuild_action), actions.onCreateDates, false)
        SyllabusReadinessStatus.ExamDatePassed -> ReadinessLine(
            stringResource(R.string.planner_ready_exam_passed), stringResource(R.string.planner_ready_set_new_exam_date), actions.onSetExamDate, true)
        SyllabusReadinessStatus.NoExamDate -> ReadinessLine(
            stringResource(R.string.planner_ready_no_exam), stringResource(R.string.planner_ready_add_exam_date), actions.onSetExamDate, false)
        is SyllabusReadinessStatus.NeedsDates -> ReadinessLine(
            pluralStringResource(R.plurals.planner_ready_needs_dates, status.unscheduledCount, status.unscheduledCount),
            stringResource(R.string.planner_ready_give_dates), actions.onGiveDates, true)
        SyllabusReadinessStatus.RatingsChanged -> ReadinessLine(
            stringResource(R.string.planner_ready_ratings_changed), stringResource(R.string.planner_ready_update_dates), actions.onUpdateDates, false)
        is SyllabusReadinessStatus.Late -> ReadinessLine(
            pluralStringResource(R.plurals.planner_ready_late, status.lateCount, status.lateCount, status.chapterName),
            stringResource(R.string.planner_ready_catch_up), actions.onCatchUp, true)
        is SyllabusReadinessStatus.FinishAfterExam -> ReadinessLine(
            pluralStringResource(R.plurals.planner_ready_after_exam, status.daysAfterExam, readableDate(status.forecastDate), status.daysAfterExam),
            stringResource(R.string.planner_ready_study_more), actions.onStudyMore, true)
        is SyllabusReadinessStatus.OnTrack -> ReadinessLine(
            if (status.forecastDate != null) stringResource(R.string.planner_ready_on_track_date, readableDate(status.forecastDate))
            else stringResource(R.string.planner_ready_on_track), null, null, false)
        SyllabusReadinessStatus.Finished -> ReadinessLine(
            stringResource(R.string.planner_ready_finished), null, null, false)
    }
    Surface(color = scheme.surface, shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.planner_ready_covered, completionPercent), style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface, modifier = Modifier.weight(1f))
                daysUntilExam?.let { days ->
                    Text(when {
                        days < 0 -> stringResource(R.string.planner_warm_exam_passed)
                        days == 0 -> stringResource(R.string.planner_warm_exam_today)
                        else -> stringResource(R.string.planner_warm_days_left, days)
                    }, style = MaterialTheme.typography.labelLarge, color = if (days < 0) warn else scheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(progress = { completionPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = scheme.secondary, trackColor = scheme.outlineVariant)
            Text(stringResource(R.string.planner_ready_topics_done, doneTopics, totalTopics),
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            PlanHairline()
            Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (isWarning) warn else scheme.onSurface)
            if (actionLabel != null && onAction != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Button(onClick = onAction, enabled = !busy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                        Text(actionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (status == SyllabusReadinessStatus.RatingsChanged) {
                        IconButton(onClick = actions.onDismissRatings, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_dismiss), tint = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class ReadinessLine(
    val message: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
    val isWarning: Boolean,
)


@Composable
private fun SyllabusNodeProgressRule(
    progress: NodeProgress,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(2.dp)
    val base = modifier
        .widthIn(max = 180.dp)
        .fillMaxWidth()
        .height(3.dp)
        .clip(shape)
    Box(
        base.then(
            if (progress.state == NodeState.NOT_STARTED) {
                Modifier.border(0.5.dp, PlannerFlatColors.BorderSoft, shape)
            } else {
                Modifier.background(PlannerFlatColors.BorderSoft)
            },
        ),
    ) {
        if (progress.state != NodeState.NOT_STARTED) {
            val fill = if (progress.state == NodeState.FINISHED) 1f else progress.percent / 100f
            Box(
                Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(PlannerFlatColors.PrimaryAccent),
            )
        }
    }
}

private fun NodeProgress.stateLabel(): String = when (state) {
    NodeState.NOT_STARTED -> "Not started"
    NodeState.DOING -> "Started · ${finishedTopics + startedTopics} of $totalTopics"
    NodeState.FINISHED -> "Finished"
}


/** macOS translucent circled "+" — shared Add control for subjects / chapters / topics. */
@Composable
internal fun SyllabusGlassAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Add",
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Default.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text(contentDescription)
    }
}

/** "Your subjects" on the left, with a glass circled "+" add control. */
@Composable
internal fun SyllabusMagazineListHeader(
    title: String,
    onAddSubject: () -> Unit,
    modifier: Modifier = Modifier,
    addContentDescription: String = "Add",
    /** Adding is an Edit-mode action; Study mode shows the title alone. */
    showAdd: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PlanHairline()
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showAdd) {
                SyllabusGlassAddButton(
                    onClick = onAddSubject,
                    contentDescription = addContentDescription,
                )
            } else {
                Spacer(Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

/** Drag state for one reorderable row: the row moves, the ≡ handle drives it. */
internal class ReorderDragState {
    var offsetY by mutableFloatStateOf(0f)
    var dragging by mutableStateOf(false)
    var itemHeightPx by mutableIntStateOf(0)
}

/** Lifts the dragged row above its neighbours and follows the finger. */
internal fun Modifier.reorderRow(state: ReorderDragState): Modifier = this
    .zIndex(if (state.dragging) 1f else 0f)
    .onGloballyPositioned { state.itemHeightPx = it.size.height }
    .graphicsLayer { translationY = if (state.dragging) state.offsetY else 0f }

/**
 * Edit-mode ≡ handle. Dragging starts immediately — no long-press to guess —
 * and each half-row of travel swaps the row one place, as before.
 */
@Composable
internal fun SyllabusReorderHandle(
    state: ReorderDragState,
    key: Any,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val moveUp by rememberUpdatedState(onMoveUp)
    val moveDown by rememberUpdatedState(onMoveDown)
    val dragEnd by rememberUpdatedState(onDragEnd)
    Box(
        modifier = modifier
            .size(48.dp)
            .pointerInput(key) {
                detectDragGestures(
                    onDragStart = {
                        state.dragging = true
                        state.offsetY = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        state.dragging = false
                        state.offsetY = 0f
                        dragEnd()
                    },
                    onDragCancel = {
                        state.dragging = false
                        state.offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.offsetY += dragAmount.y
                        val height = state.itemHeightPx
                        val threshold = height * 0.5f
                        if (threshold > 0) {
                            while (state.offsetY >= threshold) {
                                moveDown()
                                state.offsetY -= height
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            while (state.offsetY <= -threshold) {
                                moveUp()
                                state.offsetY += height
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.planner_drag_to_reorder),
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** "2 / 34 topics · 3 late" — the late part in orange so the list shows where to go. */
@Composable
private fun SyllabusRowMeta(done: Int, total: Int, late: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.planner_topics_progress, done, total),
            fontSize = 13.sp,
            color = PlannerFlatColors.TextMuted,
            maxLines = 1,
        )
        if (late > 0) {
            Text(
                text = " · " + stringResource(R.string.planner_late_count, late),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SyllabusLateColor,
                maxLines = 1,
            )
        }
    }
}

internal val SyllabusLateColor = Color(0xFFC2410C)

private fun List<StudyTopic>.lateCount(today: String): Int = count { it.isLate(today) }

/**
 * Subject row. Study mode: name, progress and late count — tap opens the
 * subject. Edit mode adds the ≡ handle and a ⋮ for add/rename/mark/delete.
 */
@Composable
internal fun SyllabusMagazineSubjectRow(
    subject: StudySubject,
    onClick: () -> Unit,
    onAddChapter: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    editMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = subject.progressState()
    val topics = subject.chapters.flatMap { it.topics }
    val late = topics.lateCount(todayKey())
    val drag = remember(subject.id) { ReorderDragState() }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .reorderRow(drag)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(start = if (editMode) 4.dp else 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editMode) {
            SyllabusReorderHandle(drag, subject.id, onMoveUp, onMoveDown, onDragEnd)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progress.percent}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.PrimaryAccent,
                )
            }
            Spacer(Modifier.height(4.dp))
            SyllabusRowMeta(done = topics.count { it.status.isStudied }, total = topics.size, late = late)
            Spacer(Modifier.height(8.dp))
            SyllabusNodeProgressRule(progress)
        }
        if (editMode) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.planner_subject_options), tint = PlannerFlatColors.TextMuted)
                }
                PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showMenu = false; onMoveUp(); onDragEnd() }
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showMenu = false; onMoveDown(); onDragEnd() }
                    PlannerOverflowMenuItem("Add chapter") { showMenu = false; onAddChapter() }
                    PlannerOverflowMenuItem("Rename") { showMenu = false; onRename() }
                    PlannerOverflowMenuItem("Mark all done") { showMenu = false; onMarkDone() }
                    PlannerOverflowMenuItem("Delete", destructive = true) { showMenu = false; onDelete() }
                }
            }
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlannerFlatColors.TextMuted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
    }
}

/**
 * Subject screen header: completion, counts, and — in Edit mode only — the
 * "+ Add chapter" control.
 */
@Composable
internal fun SyllabusMagazineChapterHeader(
    completionPercent: Int,
    chapterCount: Int,
    topicCount: Int,
    onAddChapter: () -> Unit,
    modifier: Modifier = Modifier,
    showAdd: Boolean = true,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val animatedPercent by animateFloatAsState(
        targetValue = (completionPercent / 100f).coerceIn(0f, 1f),
        label = "syllabusChapterHeaderProgress",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$chapterCount chapters · $topicCount topics",
                fontSize = 13.sp,
                color = PlannerFlatColors.TextMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$completionPercent%",
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 18.sp,
                color = accent,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PlannerFlatColors.BorderSoft),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedPercent)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
        }
        Spacer(Modifier.height(22.dp))
        SyllabusMagazineListHeader(
            title = stringResource(R.string.planner_chapters),
            onAddSubject = onAddChapter,
            addContentDescription = "Add chapter",
            showAdd = showAdd,
        )
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * Chapter row. Study mode: name, progress, late count and a read-only size tag
 * — tap opens its topics. Edit mode adds the ≡ handle, ⋮, and (on effort-
 * planned plans) the Easy/Normal/Tough chips.
 */
@Composable
internal fun SyllabusMagazineChapterRow(
    chapter: StudyChapter,
    onOpenTopics: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    onRate: (String?) -> Unit,
    editMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    /** False on a "Same every day" plan, where rating a chapter would break the
     *  exact-count promise. Defaults true so older plans are unaffected. */
    showDifficultyChips: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val progress = chapter.progressState()
    val late = chapter.topics.lateCount(todayKey())
    val drag = remember(chapter.id) { ReorderDragState() }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .reorderRow(drag)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenTopics)
            .padding(start = if (editMode) 4.dp else 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (editMode) {
                SyllabusReorderHandle(drag, chapter.id, onMoveUp, onMoveDown, onDragEnd)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chapter.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.TextDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val difficulty = chapter.difficulty
                    if (!editMode && showDifficultyChips && difficulty != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = difficulty.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlannerFlatColors.TextMuted,
                            modifier = Modifier
                                .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f, fill = true).widthIn(min = 4.dp))
                    Text(
                        text = "${progress.percent}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.PrimaryAccent,
                    )
                }
                Spacer(Modifier.height(4.dp))
                SyllabusRowMeta(done = chapter.topics.count { it.status.isStudied }, total = chapter.topics.size, late = late)
                Spacer(Modifier.height(8.dp))
                SyllabusNodeProgressRule(progress)
            }
            if (editMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.planner_chapter_options), tint = PlannerFlatColors.TextMuted)
                    }
                    PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showMenu = false; onMoveUp(); onDragEnd() }
                        PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showMenu = false; onMoveDown(); onDragEnd() }
                        PlannerOverflowMenuItem("Rename") { showMenu = false; onRename() }
                        PlannerOverflowMenuItem("Mark all done") { showMenu = false; onMarkDone() }
                        PlannerOverflowMenuItem("Delete", destructive = true) { showMenu = false; onDelete() }
                    }
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.padding(start = 4.dp).size(20.dp),
                )
            }
        }
        if (editMode && showDifficultyChips) {
            Spacer(Modifier.height(10.dp))
            SyllabusMagazineDifficultyChips(
                selected = chapter.difficulty,
                onSelect = onRate,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}

private val ShortDateFormat = java.time.format.DateTimeFormatter.ofPattern("d MMM")

private fun shortDate(iso: String?): String =
    runCatching { java.time.LocalDate.parse(iso!!.take(10)).format(ShortDateFormat) }.getOrDefault(iso.orEmpty())

/**
 * Topic row in the chapter sheet.
 *
 * Study mode: a two-way tick, the name, and ONE date chip ("Today", "14 Nov",
 * "Late · 2 Nov" or "+ Plan") whose menu sets the date. Tapping the row offers
 * Focus now / To revise / Mark not done.
 *
 * Edit mode: ≡ handle, the name, and ⋮ for move/rename/delete.
 */
@Composable
internal fun SyllabusMagazineTopicRow(
    topic: StudyTopic,
    chapter: StudyChapter?,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAssignToday: () -> Unit,
    onAssignTomorrow: () -> Unit,
    onChangeDate: () -> Unit,
    onMarkDone: () -> Unit,
    onMarkNotDone: () -> Unit,
    onToRevise: () -> Unit,
    onFocus: () -> Unit,
    editMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    /** Outlined when the student arrived here looking for this topic. */
    highlighted: Boolean = false,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val done = topic.status == TopicStatus.DONE
    val needsRevision = topic.status == TopicStatus.REVISION_NEEDED
    val today = todayKey()
    val plannedDay = topic.plannedDate?.take(10)?.takeIf { it.isNotBlank() }
    val late = topic.isLate(today)
    val effective = topic.effectiveSize(chapter)
    val showSize = topic.size != null || chapter?.difficulty != null
    val drag = remember(topic.id) { ReorderDragState() }
    var showRowMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().reorderRow(drag)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .then(if (highlighted) Modifier.border(2.dp, accent, RoundedCornerShape(18.dp)) else Modifier)
                .then(if (editMode) Modifier else Modifier.clickable { showRowMenu = true })
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editMode) {
                SyllabusReorderHandle(drag, topic.id, onMoveUp, onMoveDown, onDragEnd)
            } else {
                // Two-way, like Today: a mis-tap must be fixable right here.
                Checkbox(
                    checked = done,
                    onCheckedChange = { checked -> if (checked) onMarkDone() else onMarkNotDone() },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = topic.name },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.secondary,
                        uncheckedColor = if (needsRevision) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topic.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (done) PlannerFlatColors.TextMuted else PlannerFlatColors.TextDark,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showSize) {
                        Spacer(Modifier.width(6.dp))
                        TopicEffortBars(size = effective)
                    }
                }
                if (needsRevision) {
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.planner_to_revise), fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (editMode) {
                Box {
                    IconButton(onClick = { showRowMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.planner_topic_options), tint = PlannerFlatColors.TextMuted)
                    }
                    PlannerOverflowMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
                        PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showRowMenu = false; onMoveUp(); onDragEnd() }
                        PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showRowMenu = false; onMoveDown(); onDragEnd() }
                        PlannerOverflowMenuItem("Rename") { showRowMenu = false; onRename() }
                        PlannerOverflowMenuItem("Delete", destructive = true) { showRowMenu = false; onDelete() }
                    }
                }
            } else if (!done) {
                Box {
                    val chipText = when {
                        plannedDay == today -> stringResource(R.string.planner_warm_today)
                        late -> stringResource(R.string.planner_late_on, shortDate(plannedDay))
                        plannedDay != null -> shortDate(plannedDay)
                        else -> stringResource(R.string.planner_plan_chip)
                    }
                    val chipColor = when {
                        late -> SyllabusLateColor
                        plannedDay == today || plannedDay == null -> accent
                        else -> PlannerFlatColors.TextMuted
                    }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDateMenu = true }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = chipColor,
                            maxLines = 1,
                            modifier = Modifier
                                .border(1.dp, chipColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                    PlannerOverflowMenu(expanded = showDateMenu, onDismissRequest = { showDateMenu = false }) {
                        if (plannedDay != today) {
                            PlannerOverflowMenuItem(stringResource(R.string.planner_study_today)) { showDateMenu = false; onAssignToday() }
                        }
                        PlannerOverflowMenuItem(stringResource(R.string.planner_tomorrow)) { showDateMenu = false; onAssignTomorrow() }
                        PlannerOverflowMenuItem(stringResource(R.string.planner_pick_date)) { showDateMenu = false; onChangeDate() }
                    }
                }
            }
        }
        if (!editMode) {
            // Anchored to the row so it opens where the student tapped.
            PlannerOverflowMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
                if (!done) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_focus_now)) { showRowMenu = false; onFocus() }
                }
                if (done || needsRevision) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_to_revise)) { showRowMenu = false; onToRevise() }
                }
                if (done) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_mark_not_done)) { showRowMenu = false; onMarkNotDone() }
                } else {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_mark_done)) { showRowMenu = false; onMarkDone() }
                }
            }
        }
    }
}

/**
 * Easy / Normal / Tough as flat hairline chips — the rating still weights the
 * whole chapter for scheduling, but it reads as text with a rule around it
 * rather than a filled pill competing with the accent.
 */
@Composable
internal fun SyllabusMagazineDifficultyChips(
    selected: ChapterDifficulty?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChapterDifficulty.entries.forEach { option ->
            val isSelected = option == selected
            val tint = if (isSelected) PlannerFlatColors.PrimaryAccent else PlannerFlatColors.TextMuted
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) tint else PlannerFlatColors.BorderSoft,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelect(if (isSelected) null else option.wireValue) }
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                // The bars sit on the chip itself, so "Tough means each topic
                // takes more room" is visible in the control rather than needing
                // a sentence above it.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = option.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                    )
                    TopicEffortBars(
                        size = when (option) {
                            ChapterDifficulty.EASY -> TopicSize.SMALL
                            ChapterDifficulty.NORMAL -> TopicSize.MEDIUM
                            ChapterDifficulty.TOUGH -> TopicSize.BIG
                        },
                        activeColor = tint,
                    )
                }
            }
        }
    }
}

/** Quiet one-line empty state, replacing the bordered empty card. */
@Composable
internal fun SyllabusMagazineEmptyNote(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 28.dp)) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = PlannerFlatColors.TextMuted,
        )
        if (onAction != null) {
            Spacer(Modifier.height(14.dp))
            SyllabusGlassAddButton(
                onClick = onAction,
                contentDescription = actionLabel ?: "Add",
            )
        }
    }
}

/**
 * One search hit (chapter or topic): its name, where it lives ("Subject ·
 * Chapter") and, for a topic, its date chip text. Tap opens the chapter.
 */
@Composable
internal fun SyllabusSearchResultRow(
    title: String,
    path: String,
    done: Boolean,
    trailing: String?,
    trailingIsLate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (done) PlannerFlatColors.TextMuted else PlannerFlatColors.TextDark,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(path, fontSize = 12.sp, color = PlannerFlatColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(trailing, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (trailingIsLate) SyllabusLateColor else PlannerFlatColors.TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = PlannerFlatColors.TextMuted, modifier = Modifier.padding(start = 4.dp).size(20.dp))
    }
}

/** "Today", "14 Nov", "Late · 2 Nov" or "+ Plan" — the same wording as the topic row's chip. */
@Composable
internal fun syllabusTopicDateLabel(topic: StudyTopic): String? {
    if (topic.status == TopicStatus.DONE) return null
    val today = todayKey()
    val day = topic.plannedDate?.take(10)?.takeIf { it.isNotBlank() }
    return when {
        day == today -> stringResource(R.string.planner_warm_today)
        topic.isLate(today) -> stringResource(R.string.planner_late_on, shortDate(day))
        day != null -> shortDate(day)
        else -> stringResource(R.string.planner_plan_chip)
    }
}
