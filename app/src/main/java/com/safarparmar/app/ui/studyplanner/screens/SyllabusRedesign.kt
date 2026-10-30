package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import com.safarparmar.app.ui.studyplanner.logic.CourseMapNudge
import com.safarparmar.app.ui.studyplanner.logic.CourseMapNudgeKind
import com.safarparmar.app.ui.studyplanner.logic.progressState
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/** Syllabus overview and cards share the planner theme and existing editing actions. */
@Composable
internal fun SyllabusMagazineHeader(
    planTitle: String,
    completionPercent: Int,
    subjectCount: Int,
    chapterCount: Int,
    topicCount: Int,
    daysUntilExam: Int?,
    notStartedChapters: Int,
    onExamDateClick: () -> Unit,
    onPlanTitleClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showTitle) Text(stringResource(R.string.planner_warm_syllabus), style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
        Text(planTitle, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = scheme.onSurfaceVariant, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onPlanTitleClick))
        Surface(color = scheme.surface, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.planner_syllabus_done), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("$completionPercent%", style = MaterialTheme.typography.headlineSmall)
                }
                LinearProgressIndicator(progress = { completionPercent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = scheme.secondary, trackColor = scheme.outlineVariant)
                Text(stringResource(R.string.planner_warm_syllabus_counts, subjectCount, chapterCount, topicCount), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                if (notStartedChapters > 0) Text(stringResource(R.string.planner_warm_chapters_not_started, notStartedChapters), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onExamDateClick) {
            Text(if (daysUntilExam == null) stringResource(R.string.planner_warm_set_exam_date) else when {
                daysUntilExam < 0 -> stringResource(R.string.planner_warm_exam_passed)
                daysUntilExam == 0 -> stringResource(R.string.planner_warm_exam_today)
                else -> stringResource(R.string.planner_warm_days_left, daysUntilExam)
            })
        }
    }
}

/**
 * One obvious edit entry point. It stays in the flat magazine system: two
 * hairlines and type hierarchy, without an elevated or filled card.
 */
@Composable
internal fun SyllabusChangePlanBand(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(18.dp)) {
        Text(stringResource(R.string.planner_change_study_plan), maxLines = 2)
    }
}

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

/**
 * Full exam/plan name card — sits just above the bottom navigation buttons
 * when a clipped title is tapped.
 */
@Composable
internal fun SyllabusFullPlanTitleCard(
    planTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassSurface(shape = RoundedCornerShape(16.dp), isDarkTheme = isDarkTheme)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = planTitle,
            fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = PlannerFlatColors.TextDark,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = PlannerFlatColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun SyllabusRatingRebuildBar(
    hasExamDate: Boolean,
    rebuilding: Boolean,
    onRebuild: () -> Unit,
    onSetExamDate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (hasExamDate) {
                "Ratings changed. Today's topics stay where they are."
            } else {
                "Ratings changed. Set an exam date to rebuild."
            },
            fontSize = 13.sp,
            color = PlannerFlatColors.TextDark,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (hasExamDate) "Rebuild schedule" else "Set exam date",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.PrimaryAccent,
            modifier = Modifier.clickable(
                enabled = !rebuilding,
                onClick = if (hasExamDate) onRebuild else onSetExamDate,
            ),
        )
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_dismiss), tint = PlannerFlatColors.TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun SyllabusCourseMapNudge(
    nudge: CourseMapNudge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lead = when (nudge.kind) {
        CourseMapNudgeKind.LATE -> "Study first: "
        CourseMapNudgeKind.TODAY -> "Today's work: "
        CourseMapNudgeKind.TODAY_FINISHED -> "Today's work is finished ✓"
        CourseMapNudgeKind.NEEDS_SCHEDULE -> "Make study dates to see today's work"
        CourseMapNudgeKind.ON_TRACK -> "You're on track · No late topics"
    }
    val actionable = nudge.kind == CourseMapNudgeKind.LATE ||
        nudge.kind == CourseMapNudgeKind.TODAY ||
        nudge.kind == CourseMapNudgeKind.NEEDS_SCHEDULE
    val markerColor = when (nudge.kind) {
        CourseMapNudgeKind.LATE -> Color(0xFFF97316)
        CourseMapNudgeKind.NEEDS_SCHEDULE -> PlannerFlatColors.PrimaryAccent
        else -> PlannerFlatColors.PrimaryAccent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = actionable, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(markerColor.copy(alpha = if (actionable) 1f else 0.45f)),
        )
        Spacer(Modifier.width(12.dp))
        if (nudge.chapterName != null) {
            Text(lead, fontSize = 13.sp, color = PlannerFlatColors.TextMuted)
            Text(
                nudge.chapterName,
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val detail = when (nudge.kind) {
                CourseMapNudgeKind.LATE -> if (nudge.topicCount == 1) "1 topic late" else "${nudge.topicCount} topics late"
                else -> if (nudge.topicCount == 1) "1 topic left" else "${nudge.topicCount} topics left"
            }
            Text(
                text = " · $detail",
                fontSize = 13.sp,
                color = if (nudge.kind == CourseMapNudgeKind.LATE) Color(0xFFF97316) else PlannerFlatColors.TextMuted,
            )
        } else {
            Text(
                text = lead,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (nudge.kind == CourseMapNudgeKind.NEEDS_SCHEDULE) {
                    PlannerFlatColors.PrimaryAccent
                } else {
                    PlannerFlatColors.TextDark
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (actionable) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (nudge.kind == CourseMapNudgeKind.NEEDS_SCHEDULE) "Make dates" else "Open",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.PrimaryAccent,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlannerFlatColors.PrimaryAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
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
            SyllabusGlassAddButton(
                onClick = onAddSubject,
                contentDescription = addContentDescription,
            )
        }
    }
}

/**
 * One flat subject row: a hairline-bordered initial badge, the subject name with
 * its chapter/topic meta and a slim inline progress bar — no bordered card, no
 * per-subject wrapper.
 *
 * The mock shows only a chevron, but rename / add-chapter / mark-done / delete
 * have no other entry point on this screen, so they stay behind a muted overflow
 * rather than being dropped. Long-press-and-drag still reorders, as before.
 */
@Composable
internal fun SyllabusMagazineSubjectRow(
    subject: StudySubject,
    onClick: () -> Unit,
    onAddChapter: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    canReorder: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val progress = subject.progressState()
    val chapterCount = subject.chapters.size
    val topicCount = subject.chapters.sumOf { it.topics.size }

    var showMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { itemHeightPx = it.size.height }
            .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
            .then(
                if (canReorder) {
                    Modifier.pointerInput(subject.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffsetY = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY >= threshold) {
                                        onMoveDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY <= -threshold) {
                                        onMoveUp()
                                        dragOffsetY += itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Initial badge — soft accent wash + 0.5dp hairline (flat magazine chip).
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlannerFlatColors.AccentTint)
                .border(0.5.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = subject.name.trim().take(1).uppercase(),
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = accent,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                fontFamily = com.safarparmar.app.ui.theme.MulishFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$chapterCount chapters · $topicCount topics",
                fontSize = 13.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${progress.stateLabel()} · ${progress.percent}%",
                fontSize = 13.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            SyllabusNodeProgressRule(progress)
        }

        Spacer(Modifier.width(8.dp))

        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.planner_subject_options),
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (canReorder) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showMenu = false; onMoveUp(); onDragEnd() }
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showMenu = false; onMoveDown(); onDragEnd() }
                }
                PlannerOverflowMenuItem("Add chapter") { showMenu = false; onAddChapter() }
                PlannerOverflowMenuItem("Rename") { showMenu = false; onRename() }
                PlannerOverflowMenuItem("Mark all done") { showMenu = false; onMarkDone() }
                PlannerOverflowMenuItem("Delete", destructive = true) { showMenu = false; onDelete() }
            }
        }

        Text(
            text = stringResource(R.string.common_open),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.PrimaryAccent,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PlannerFlatColors.PrimaryAccent,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Drill-down header: the subject's completion rule and counts, then the
 * "Chapters" list header with "+ Add chapter" as a text link — replacing the
 * full-width filled "Add Chapter" button.
 */
@Composable
internal fun SyllabusMagazineChapterHeader(
    completionPercent: Int,
    chapterCount: Int,
    topicCount: Int,
    onAddChapter: () -> Unit,
    modifier: Modifier = Modifier,
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
        )
        Spacer(Modifier.height(6.dp))
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

/**
 * One flat chapter row: name, topic count and completion, a slim inline progress
 * bar, and the Easy/Normal/Tough rating chips underneath — no bordered accordion
 * card. Tap opens the topics sheet; long-press-and-drag still reorders.
 */
@Composable
internal fun SyllabusMagazineChapterRow(
    chapter: StudyChapter,
    onOpenTopics: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    onRate: (String?) -> Unit,
    canReorder: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    /** False on a "Same every day" plan, where rating a chapter would break the
     *  exact-count promise. Defaults true so older plans are unaffected. */
    showDifficultyChips: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val progress = chapter.progressState()
    var showMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { itemHeightPx = it.size.height }
            .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
            .then(
                if (canReorder) {
                    Modifier.pointerInput(chapter.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffsetY = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY >= threshold) {
                                        onMoveDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY <= -threshold) {
                                        onMoveUp()
                                        dragOffsetY += itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTopics),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${progress.stateLabel()} · ${progress.percent}%",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                SyllabusNodeProgressRule(progress)
            }

            Spacer(Modifier.width(8.dp))

            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.planner_chapter_options),
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (canReorder) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showMenu = false; onMoveUp(); onDragEnd() }
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showMenu = false; onMoveDown(); onDragEnd() }
                }
                    PlannerOverflowMenuItem("Rename") { showMenu = false; onRename() }
                    PlannerOverflowMenuItem("Mark all done") { showMenu = false; onMarkDone() }
                    PlannerOverflowMenuItem("Delete", destructive = true) { showMenu = false; onDelete() }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlannerFlatColors.ProgressTrack,
                modifier = Modifier.size(18.dp),
            )
        }

        // Only an effort-planned plan gets rating chips. On a "Same every day"
        // plan a rating would silently make one day heavier than another, breaking
        // the exact-count promise the student chose — so the control is absent
        // rather than present-but-broken.
        if (showDifficultyChips) {
            Spacer(Modifier.height(12.dp))
            SyllabusMagazineDifficultyChips(
                selected = chapter.difficulty,
                onSelect = onRate,
            )
        }
    }
}

/**
 * One flat topic row for the chapter's topics sheet: a tappable status circle,
 * the topic name with its size badge, and a date / "not planned" meta line.
 * Hairlines separate rows — no accordion card, no left accent bar.
 */
@Composable
internal fun SyllabusMagazineTopicRow(
    topic: StudyTopic,
    chapter: StudyChapter?,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAssignToday: () -> Unit,
    onChangeDate: () -> Unit,
    onMarkDone: () -> Unit,
    onToRevise: () -> Unit,
    canReorder: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val done = topic.status == TopicStatus.DONE
    val needsRevision = topic.status == TopicStatus.REVISION_NEEDED
    val hasDate = !topic.plannedDate.isNullOrBlank()
    val effective = topic.effectiveSize(chapter)
    val showSize = topic.size != null || chapter?.difficulty != null
    val plannedToday = topic.plannedDate?.take(10) == todayKey()

    var showMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { itemHeightPx = it.size.height }
            .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
            .then(
                if (canReorder) {
                    Modifier.pointerInput(topic.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffsetY = 0f
                                onDragEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY >= threshold) {
                                        onMoveDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY <= -threshold) {
                                        onMoveUp()
                                        dragOffsetY += itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = done, enabled = !done, onCheckedChange = { checked -> if (checked && !done) onMarkDone() },
            modifier = Modifier.size(48.dp).semantics { contentDescription = topic.name },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary,
                disabledCheckedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = if (needsRevision) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline))
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.name,
                    fontSize = 13.5.sp,
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
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (hasDate) readableDate(topic.plannedDate) else "Not planned",
                    fontSize = 13.sp,
                    color = if (needsRevision) Color(0xFFF97316) else PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        if (!done && !plannedToday) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(7.dp))
                    .clickable(onClick = onAssignToday)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.planner_study_today),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(4.dp))
        }

        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.planner_topic_options),
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            PlannerOverflowMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (canReorder) {
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_up)) { showMenu = false; onMoveUp(); onDragEnd() }
                    PlannerOverflowMenuItem(stringResource(R.string.planner_warm_move_down)) { showMenu = false; onMoveDown(); onDragEnd() }
                }
                PlannerOverflowMenuItem("Rename") { showMenu = false; onRename() }
                PlannerOverflowMenuItem("Change date") { showMenu = false; onChangeDate() }
                if (!done) {
                    PlannerOverflowMenuItem("Mark done") { showMenu = false; onMarkDone() }
                }
                PlannerOverflowMenuItem("To revise") { showMenu = false; onToRevise() }
                PlannerOverflowMenuItem("Delete", destructive = true) { showMenu = false; onDelete() }
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
