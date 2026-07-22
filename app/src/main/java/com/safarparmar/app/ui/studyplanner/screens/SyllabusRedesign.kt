package com.safarparmar.app.ui.studyplanner.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.logic.percentDone
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * The Syllabus tab in the same flat "magazine" system as the Home redesign.
 *
 * The old screen stacked a bordered overview card (icon, title, %, progress bar,
 * three meta pills), a two-button action row, a boxed search field, a section
 * header, a full-width "Add Subject" button, then one bordered accordion card
 * per subject — a card-on-card list where every subject was the heaviest
 * possible row. These pieces keep every action but read as one flat, scrollable
 * page: one header, one toolbar line, one list, divided only by hairlines.
 */

/**
 * Page header: eyebrow + overflow, then the plan title with its completion
 * percent, a thin progress rule, and the subject/chapter/topic count line —
 * replacing the whole overview card and its meta pills.
 */
@Composable
internal fun SyllabusMagazineHeader(
    planTitle: String,
    completionPercent: Int,
    subjectCount: Int,
    chapterCount: Int,
    topicCount: Int,
    onOverflowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val animatedPercent by animateFloatAsState(
        targetValue = (completionPercent / 100f).coerceIn(0f, 1f),
        label = "syllabusHeaderProgress",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanEyebrow("Syllabus", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                    .clickable(onClick = onOverflowClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Syllabus options",
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = planTitle,
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$completionPercent%",
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = accent,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(10.dp))

        // Thin completion rule — the progress bar without a card around it.
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

        Spacer(Modifier.height(10.dp))

        Text(
            text = "$subjectCount subjects · $chapterCount chapters · $topicCount topics",
            fontSize = 11.5.sp,
            color = PlannerFlatColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Slim borderless toolbar: inline search on the left, "Build" on the right,
 * bounded by hairlines instead of a boxed text field plus a filled button.
 */
@Composable
internal fun SyllabusMagazineToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBuild: () -> Unit,
    buildEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    Column(modifier = modifier.fillMaxWidth()) {
        PlanHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = PlannerFlatColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search",
                        fontSize = 12.5.sp,
                        color = PlannerFlatColors.TextMuted,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 12.5.sp,
                        color = PlannerFlatColors.TextDark,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onSearchChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = buildEnabled, onClick = onBuild)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Build",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (buildEnabled) accent else PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Build re-ordered syllabus",
                    tint = if (buildEnabled) accent else PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        PlanHairline()
    }
}

/** "Your syllabus" on the left, "+ Add subject" as a text link on the right. */
@Composable
internal fun SyllabusMagazineListHeader(
    title: String,
    onAddSubject: () -> Unit,
    modifier: Modifier = Modifier,
    addLabel: String = "+ Add subject",
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
        Text(
            text = addLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.PrimaryAccent,
            modifier = Modifier.clickable(onClick = onAddSubject),
        )
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
    val percent = subject.percentDone()
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
        // Initial badge — a hairline square, not a filled circle.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = subject.name.trim().take(1).uppercase(),
                fontFamily = LoraFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = accent,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$chapterCount chapters · $topicCount topics",
                fontSize = 11.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            // Inline progress — deliberately muted, so the gold accent stays
            // reserved for the page-level completion figure.
            Box(
                Modifier
                    .widthIn(max = 180.dp)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlannerFlatColors.BorderSoft),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PlannerFlatColors.TextMuted),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

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
                    contentDescription = "Subject options",
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Add chapter") },
                    onClick = { showMenu = false; onAddChapter() },
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { showMenu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("Mark all done") },
                    onClick = { showMenu = false; onMarkDone() },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PlannerFlatColors.BorderSoft,
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
                fontSize = 11.5.sp,
                color = PlannerFlatColors.TextMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$completionPercent%",
                fontFamily = LoraFontFamily,
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
            title = "Chapters",
            onAddSubject = onAddChapter,
            addLabel = "+ Add chapter",
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
                Text(
                    text = option.label,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                )
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
    modifier: Modifier = Modifier,
) {
    val percent = chapter.percentDone()
    val topicCount = chapter.topics.size

    var showMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                    text = "$topicCount topics · $percent%",
                    fontSize = 11.sp,
                    color = PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .widthIn(max = 180.dp)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PlannerFlatColors.BorderSoft),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PlannerFlatColors.TextMuted),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

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
                        contentDescription = "Chapter options",
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { showMenu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Mark all done") },
                        onClick = { showMenu = false; onMarkDone() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlannerFlatColors.BorderSoft,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        SyllabusMagazineDifficultyChips(
            selected = chapter.difficulty,
            onSelect = onRate,
        )
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

    var showMenu by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
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
        // Status circle — tapping it completes the topic, same as the old badge.
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    when {
                        done -> Modifier.background(accent)
                        needsRevision -> Modifier.border(1.dp, Color(0xFFF97316), CircleShape)
                        else -> Modifier.border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                    },
                )
                .clickable(enabled = !done, onClick = onMarkDone),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        }

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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = effective.shortLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            if (hasDate) {
                Text(
                    text = readableDate(topic.plannedDate),
                    fontSize = 11.sp,
                    color = if (needsRevision) Color(0xFFF97316) else PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = "Not planned · Add to today",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onAssignToday),
                )
            }
        }

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
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { showMenu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("Change date") },
                    onClick = { showMenu = false; onChangeDate() },
                )
                if (!done) {
                    DropdownMenuItem(
                        text = { Text("Mark done") },
                        onClick = { showMenu = false; onMarkDone() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("To revise") },
                    onClick = { showMenu = false; onToRevise() },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() },
                )
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
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = actionLabel,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.PrimaryAccent,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}
