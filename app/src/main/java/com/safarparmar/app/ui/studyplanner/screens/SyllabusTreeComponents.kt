package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.logic.percentDone
import com.safarparmar.app.ui.studyplanner.logic.readableDate

@Composable
internal fun SyllabusSubjectAccordionCard(
    subject: StudySubject,
    onClick: () -> Unit,
    onAddChapter: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    canReorder: Boolean = false,
    onMoveSubjectUp: () -> Unit = {},
    onMoveSubjectDown: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val chapterCount = subject.chapters.size
    val topicCount = subject.chapters.sumOf { it.topics.size }
    val completion = subject.percentDone()

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        label = "subjectScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 10f else 0f,
        label = "subjectElevation"
    )
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        label = "subjectDragOffset"
    )
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // While dragging, this item must paint above its siblings regardless of
            // which direction it's moving. Compose draws later-composed siblings on
            // top by default, so without an explicit zIndex, dragging an item DOWN
            // past items composed after it made it visually slide underneath them
            // instead of gliding over the top — the "goes from down under" glitch.
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                itemHeightPx = coordinates.size.height
            }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
                shape = RoundedCornerShape(18.dp)
                clip = false
            }
            .then(
                // No dedicated drag-handle icon — press and hold anywhere on the card,
                // then drag, to reorder. A quick tap still falls through to
                // onClick below since long-press-drag only engages after the
                // long-press threshold.
                if (canReorder) {
                    Modifier.pointerInput(Unit) {
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
                                // Swap at the midpoint (50%) rather than waiting for
                                // 75% of the row to pass. `while` (not `if`) lets a
                                // single continuous drag carry the card past EVERY
                                // other card — each time the finger clears the next
                                // sibling's midpoint another swap fires — instead of
                                // stopping after one neighbour on a fast/long drag.
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY >= threshold) {
                                        onMoveSubjectDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY <= -threshold) {
                                        onMoveSubjectUp()
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
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isDragging) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubjectInitialBadge(subject.name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "$chapterCount chapters • $topicCount topics • $completion%",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                RowActionIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add chapter",
                    onClick = onAddChapter,
                )
                SubjectOverflowMenuMinimal(onRename = onRename, onDelete = onDelete, onMarkDone = onMarkDone)
            }

            LinearProgressIndicator(
                progress = { completion / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(5.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
            )
        }
    }
}

/**
 * A chapter row is always collapsed to name + count + a slim progress bar — tapping it
 * opens [ChapterTopicsSheet] instead of inlining every topic into this accordion. A
 * chapter with 50 topics used to push everything below it far down the outer subject
 * list; the sheet gives each chapter's topic list its own single, unambiguous scroll
 * container instead of nesting one inside the subject-level LazyColumn.
 */
@Composable
internal fun SyllabusChapterAccordionRow(
    chapter: StudyChapter,
    onOpenTopics: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit,
    canReorder: Boolean,
    onMoveChapterUp: () -> Unit,
    onMoveChapterDown: () -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val completion = chapter.percentDone()

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        label = "chapterScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        label = "chapterElevation"
    )
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        label = "chapterDragOffset"
    )
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                itemHeightPx = coordinates.size.height
            }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
                shape = RoundedCornerShape(14.dp)
                clip = false
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDragging) scheme.surfaceContainerHighest else scheme.surfaceContainerLow)
            .then(
                // No dedicated drag-handle icon — press and hold anywhere on the row,
                // then drag, to reorder. A quick tap still opens the topics sheet
                // below since long-press-drag only engages after the long-press
                // threshold.
                if (canReorder) {
                    Modifier.pointerInput(Unit) {
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
                                // Swap at the midpoint (50%) rather than waiting for
                                // 75% of the row to pass. `while` (not `if`) lets a
                                // single continuous drag carry the card past EVERY
                                // other card — each time the finger clears the next
                                // sibling's midpoint another swap fires — instead of
                                // stopping after one neighbour on a fast/long drag.
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY >= threshold) {
                                        onMoveChapterDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY <= -threshold) {
                                        onMoveChapterUp()
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
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTopics)
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${chapter.topics.size} topics • $completion%",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            SubjectOverflowMenuMinimal(onRename = onRename, onDelete = onDelete, onMarkDone = onMarkDone)
        }
        LinearProgressIndicator(
            progress = { completion / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 8.dp)
                .height(4.dp)
                .clip(CircleShape),
            color = PlannerAccent.Teal,
            trackColor = scheme.surfaceContainerHighest,
        )
    }
}

@Composable
internal fun SyllabusTopicAccordionRow(
    topic: StudyTopic,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAssignToday: () -> Unit,
    canReorder: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragEnd: () -> Unit = {},
    onChangeDate: () -> Unit = {},
    onMarkDone: () -> Unit = {},
    onToRevise: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDone = topic.status == TopicStatus.DONE
    val hasDate = !topic.plannedDate.isNullOrBlank()

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        label = "topicScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 6f else 0f,
        label = "topicElevation"
    )
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        label = "topicDragOffset"
    )
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                itemHeightPx = coordinates.size.height
            }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
                shape = RoundedCornerShape(8.dp)
                clip = false
            }
            .then(
                // No dedicated drag-handle icon — press and hold anywhere on the row,
                // then drag, to reorder. A quick tap still opens the topic detail
                // sheet since long-press-drag only engages after the long-press
                // threshold.
                if (canReorder) {
                    Modifier.pointerInput(Unit) {
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
                                // Swap at the midpoint (50%) rather than waiting for
                                // 75% of the row to pass. `while` (not `if`) lets a
                                // single continuous drag carry the card past EVERY
                                // other card — each time the finger clears the next
                                // sibling's midpoint another swap fires — instead of
                                // stopping after one neighbour on a fast/long drag.
                                // Guarded on itemHeightPx > 0 so a drag that starts
                                // before the row's first layout pass can't fire a
                                // spurious swap against a zero threshold.
                                val threshold = itemHeightPx * 0.5f
                                if (threshold > 0) {
                                    while (dragOffsetY > threshold) {
                                        onMoveDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    while (dragOffsetY < -threshold) {
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
            ),
        color = if (isDragging) scheme.surfaceContainerHighest else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                TopicStatusBadge(topic.status)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isDone) scheme.onSurfaceVariant else scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasDate) {
                    Text(
                        text = readableDate(topic.plannedDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                } else {
                    Surface(
                        modifier = Modifier.clickable(onClick = onAssignToday),
                        shape = RoundedCornerShape(50),
                        color = scheme.secondaryContainer.copy(alpha = 0.6f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = scheme.onSecondaryContainer,
                            )
                            Text(
                                text = "Not planned yet · Add to today",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            SubjectOverflowMenuMinimal(
                onRename = onRename,
                onDelete = onDelete,
                onChangeDate = onChangeDate,
                onMarkDone = onMarkDone,
                onToRevise = onToRevise
            )
        }
    }
}

/** Small "+" icon with a comfortable 44dp touch target, used for the direct-tap add actions. */
@Composable
private fun RowActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Topic status affordance — a ~24dp icon badge instead of a plain 12dp color dot, so
 * status is legible and tappable-looking at a glance while browsing a long chapter, not
 * just a faint colored speck. Shape + icon carries the meaning, not color alone.
 */
@Composable
internal fun TopicStatusBadge(status: TopicStatus) {
    val (background, tint, icon) = when (status) {
        TopicStatus.DONE -> Triple(PlannerAccent.Teal.copy(alpha = 0.18f), PlannerAccent.Teal, Icons.Default.Check)
        TopicStatus.REVISION_NEEDED -> Triple(PlannerAccent.Amber.copy(alpha = 0.18f), PlannerAccent.Amber, Icons.Default.Schedule)
        TopicStatus.IN_PROGRESS -> Triple(PlannerAccent.Coral.copy(alpha = 0.18f), PlannerAccent.Coral, Icons.Default.PlayArrow)
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.outline, Icons.Outlined.Circle)
    }
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = status.name, tint = tint, modifier = Modifier.size(14.dp))
    }
}

/** Overflow menu reserved for Rename/Delete/Change Date only — Add actions are direct-tap icons, not menu items. */
@Composable
private fun SubjectOverflowMenuMinimal(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeDate: (() -> Unit)? = null,
    onMarkDone: (() -> Unit)? = null,
    onToRevise: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            if (onChangeDate != null) {
                DropdownMenuItem(text = { Text("Change Date") }, onClick = { expanded = false; onChangeDate() })
            }
            if (onMarkDone != null) {
                DropdownMenuItem(text = { Text("Mark Done") }, onClick = { expanded = false; onMarkDone() })
            }
            if (onToRevise != null) {
                DropdownMenuItem(text = { Text("To Revise") }, onClick = { expanded = false; onToRevise() })
            }
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}
