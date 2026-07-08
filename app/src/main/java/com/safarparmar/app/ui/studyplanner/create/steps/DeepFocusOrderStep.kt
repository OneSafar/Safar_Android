package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.safarparmar.app.ui.studyplanner.create.DeepFocusOutlineSubject

/**
 * Deep Focus's "finish topics in syllabus order" only makes sense if the user
 * can pick that order — this screen lets them drag subjects and chapters (never
 * topics; that granularity isn't ordered here) into the sequence they want to
 * study in. The drag mechanics (offset, threshold-based swap, haptics) match the
 * syllabus editor's own reorder gesture — press and hold anywhere on a card, then
 * drag, no separate handle icon needed.
 */
@Composable
fun DeepFocusOrderStep(
    outline: List<DeepFocusOutlineSubject>,
    onMoveSubject: (fromIndex: Int, toIndex: Int) -> Unit,
    onMoveChapter: (subjectName: String, fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Order your syllabus", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Press and hold a subject or chapter, then drag to reorder it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(outline, key = { _, subject -> subject.name }) { subjectIndex, subject ->
                DeepFocusSubjectCard(
                    subject = subject,
                    onMoveSubjectUp = { onMoveSubject(subjectIndex, subjectIndex - 1) },
                    onMoveSubjectDown = { onMoveSubject(subjectIndex, subjectIndex + 1) },
                    onMoveChapterUp = { chapterIndex -> onMoveChapter(subject.name, chapterIndex, chapterIndex - 1) },
                    onMoveChapterDown = { chapterIndex -> onMoveChapter(subject.name, chapterIndex, chapterIndex + 1) },
                )
            }
        }
    }
}

@Composable
private fun DeepFocusSubjectCard(
    subject: DeepFocusOutlineSubject,
    onMoveSubjectUp: () -> Unit,
    onMoveSubjectDown: () -> Unit,
    onMoveChapterUp: (Int) -> Unit,
    onMoveChapterDown: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f, label = "subjectScale")
    val elevation by animateFloatAsState(targetValue = if (isDragging) 10f else 0f, label = "subjectElevation")
    val animatedDragOffsetY by animateFloatAsState(targetValue = dragOffsetY, label = "subjectDragOffset")
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates -> itemHeightPx = coordinates.size.height }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
                shape = RoundedCornerShape(18.dp)
                clip = false
            },
        shape = RoundedCornerShape(18.dp),
        color = if (isDragging) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // The drag gesture is scoped to just this header row, not the whole card —
            // the card also contains the chapter rows below, each with their own
            // independent drag detector. If a parent's pointerInput region visually
            // contained a child's, both would race to interpret the same touch
            // (this is what made dragging a chapter also perturb its subject), so the
            // subject-level drag must never span the chapter list underneath it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val threshold = itemHeightPx * 0.75f
                                if (threshold > 0) {
                                    if (dragOffsetY >= threshold) {
                                        onMoveSubjectDown()
                                        dragOffsetY -= itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (dragOffsetY <= -threshold) {
                                        onMoveSubjectUp()
                                        dragOffsetY += itemHeightPx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        text = "${subject.chapterNames.size} chapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp)) {
                subject.chapterNames.forEachIndexed { chapterIndex, chapterName ->
                    DeepFocusChapterRow(
                        name = chapterName,
                        onMoveUp = { onMoveChapterUp(chapterIndex) },
                        onMoveDown = { onMoveChapterDown(chapterIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeepFocusChapterRow(
    name: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f, label = "chapterScale")
    val elevation by animateFloatAsState(targetValue = if (isDragging) 8f else 0f, label = "chapterElevation")
    val animatedDragOffsetY by animateFloatAsState(targetValue = dragOffsetY, label = "chapterDragOffset")
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .onGloballyPositioned { coordinates -> itemHeightPx = coordinates.size.height }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
                shape = RoundedCornerShape(14.dp)
                clip = false
            }
            .background(if (isDragging) scheme.surfaceContainerHighest else scheme.surfaceContainerLow, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragOffsetY = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        val threshold = itemHeightPx * 0.75f
                        if (threshold > 0) {
                            if (dragOffsetY >= threshold) {
                                onMoveDown()
                                dragOffsetY -= itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY <= -threshold) {
                                onMoveUp()
                                dragOffsetY += itemHeightPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                )
            }
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
