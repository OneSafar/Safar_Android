package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.create.DeepFocusOutlineSubject
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Deep Focus's "finish topics in syllabus order" only makes sense if the user can
 * pick that order — this screen lets them drag subjects, chapters, and topics into
 * the sequence they want to study in, drilling down one subject at a time exactly
 * like the live Syllabus screen (subject list → tap → that subject's chapter list →
 * tap a chapter → its topics in a bottom sheet). The drag mechanics on every level
 * (long-press-then-drag, threshold-based swap, scale/elevation, haptics) are copied
 * verbatim from `SyllabusSubjectAccordionCard` / `SyllabusChapterAccordionRow` /
 * `SyllabusTopicAccordionRow` in `SyllabusTreeComponents.kt` so this screen feels
 * identical to that one, not just similar.
 */
/**
 * Lazy-list keys must be unique, but a syllabus routinely repeats names ("Revision",
 * "Test", the same chapter title under two subjects) and duplicate keys crash the list
 * with IllegalArgumentException while it measures. Keep the name as the identity — the
 * drag-reorder animations depend on it following the item — and only suffix the repeats.
 */
private fun uniqueNameKeys(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        val occurrence = seen.getOrElse(name) { 0 }
        seen[name] = occurrence + 1
        if (occurrence == 0) name else "$name\u0000$occurrence"
    }
}

@Composable
fun DeepFocusOrderStep(
    outline: List<DeepFocusOutlineSubject>,
    drillSubjectIndex: Int?,
    onDrillIntoSubject: (Int) -> Unit,
    onMoveSubject: (fromIndex: Int, toIndex: Int) -> Unit,
    onMoveChapter: (subjectName: String, fromIndex: Int, toIndex: Int) -> Unit,
    onMoveTopic: (subjectName: String, chapterName: String, fromIndex: Int, toIndex: Int) -> Unit,
    onContinue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val subject = drillSubjectIndex?.let { outline.getOrNull(it) }

    if (subject == null) {
        Column(
            modifier = modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Tap a subject to order its chapters and topics. Press and hold anywhere on a card, then drag, to reorder it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val subjectKeys = remember(outline) { uniqueNameKeys(outline.map { it.name }) }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(outline, key = { index, _ -> subjectKeys[index] }) { subjectIndex, s ->
                    val topicCount = s.chapters.sumOf { it.topicNames.size }
                    DeepFocusRow(
                        order = subjectIndex + 1,
                        title = s.name,
                        subtitle = "${s.chapters.size} chapters • $topicCount topics",
                        scale = 1.02f,
                        elevation = 10f,
                        cornerRadius = 18.dp,
                        onClick = { onDrillIntoSubject(subjectIndex) },
                        onMoveUp = { onMoveSubject(subjectIndex, subjectIndex - 1) },
                        onMoveDown = { onMoveSubject(subjectIndex, subjectIndex + 1) },
                        useCardSurface = true,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            val isLight = MaterialTheme.colorScheme.background.isLightBackground()
            MacOSPrimaryActionButton(
                text = "Continue",
                onClick = onContinue,
                isLight = isLight,
                customAccent = PlannerAccent.Coral,
            )
        }
    } else {
        // Holds just the chapter's name, not the chapter object itself — the sheet
        // below looks the chapter up fresh from `subject.chapters` on every
        // recomposition so a topic reordered while the sheet is open shows the new
        // order immediately instead of the stale snapshot from when it was opened.
        var openChapterName by remember(subject.name) { mutableStateOf<String?>(null) }

        Column(
            modifier = modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(subject.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Tap a chapter to order its topics. Press and hold, then drag, to reorder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val chapterKeys = remember(subject.chapters) { uniqueNameKeys(subject.chapters.map { it.name }) }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(subject.chapters, key = { index, _ -> chapterKeys[index] }) { chapterIndex, chapter ->
                    DeepFocusRow(
                        order = chapterIndex + 1,
                        title = chapter.name,
                        subtitle = "${chapter.topicNames.size} topics",
                        scale = 1.02f,
                        elevation = 8f,
                        cornerRadius = 14.dp,
                        onClick = { openChapterName = chapter.name },
                        onMoveUp = { onMoveChapter(subject.name, chapterIndex, chapterIndex - 1) },
                        onMoveDown = { onMoveChapter(subject.name, chapterIndex, chapterIndex + 1) },
                        useCardSurface = false,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        val openChapter = openChapterName?.let { name -> subject.chapters.find { it.name == name } }
        if (openChapter != null) {
            DeepFocusTopicsSheet(
                chapterName = openChapter.name,
                topicNames = openChapter.topicNames,
                onDismiss = { openChapterName = null },
                onMoveTopic = { fromIndex, toIndex -> onMoveTopic(subject.name, openChapter.name, fromIndex, toIndex) },
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DeepFocusTopicsSheet(
    chapterName: String,
    topicNames: List<String>,
    onDismiss: () -> Unit,
    onMoveTopic: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = chapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                "Press and hold a topic, then drag, to reorder it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            if (topicNames.isEmpty()) {
                Text(
                    "No topics in this chapter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                val topicKeys = remember(topicNames) { uniqueNameKeys(topicNames) }
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    itemsIndexed(topicNames, key = { index, _ -> topicKeys[index] }) { index, name ->
                        DeepFocusRow(
                            order = index + 1,
                            title = name,
                            subtitle = null,
                            scale = 1.04f,
                            elevation = 6f,
                            cornerRadius = 8.dp,
                            onClick = null,
                            onMoveUp = { onMoveTopic(index, index - 1) },
                            onMoveDown = { onMoveTopic(index, index + 1) },
                            useCardSurface = false,
                            modifier = Modifier.animateItem(),
                        )
                        if (index < topicNames.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One draggable row shared by all three levels — subject, chapter, and topic — with
 * only the scale/elevation/corner-radius/surface-style differing per level, matching
 * `SyllabusSubjectAccordionCard` (scale 1.02/elevation 10), `SyllabusChapterAccordionRow`
 * (1.02/8), and `SyllabusTopicAccordionRow` (1.04/6) exactly.
 */
@Composable
private fun DeepFocusRow(
    order: Int,
    title: String,
    subtitle: String?,
    scale: Float,
    elevation: Float,
    cornerRadius: Dp,
    onClick: (() -> Unit)?,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    useCardSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    val animatedScale by animateFloatAsState(targetValue = if (isDragging) scale else 1f, label = "rowScale")
    val animatedElevation by animateFloatAsState(targetValue = if (isDragging) elevation else 0f, label = "rowElevation")
    val animatedDragOffsetY by animateFloatAsState(targetValue = dragOffsetY, label = "rowDragOffset")
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    val currentOnMoveUp by rememberUpdatedState(onMoveUp)
    val currentOnMoveDown by rememberUpdatedState(onMoveDown)

    val shared = modifier
        .fillMaxWidth()
        .padding(bottom = if (useCardSurface) 0.dp else 6.dp)
        .zIndex(if (isDragging) 1f else 0f)
        .onGloballyPositioned { coordinates -> itemHeightPx = coordinates.size.height }
        .graphicsLayer {
            translationY = if (isDragging) dragOffsetY else animatedDragOffsetY
            scaleX = animatedScale
            scaleY = animatedScale
            shadowElevation = animatedElevation * density
            shape = RoundedCornerShape(cornerRadius)
            clip = false
        }
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
                    // Swap at the midpoint (50%) rather than waiting for 75% of the
                    // row to pass. `while` (not `if`) lets a single continuous drag
                    // carry the card past EVERY other card — each time the finger
                    // clears the next sibling's midpoint another swap fires —
                    // instead of stopping after one neighbour on a fast/long drag.
                    val threshold = itemHeightPx * 0.5f
                    if (threshold > 0) {
                        while (dragOffsetY >= threshold) {
                            currentOnMoveDown()
                            dragOffsetY -= itemHeightPx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        while (dragOffsetY <= -threshold) {
                            currentOnMoveUp()
                            dragOffsetY += itemHeightPx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                },
            )
        }

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = scheme.primaryContainer,
                modifier = Modifier.size(26.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = order.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (useCardSurface) {
        Surface(
            modifier = shared,
            shape = RoundedCornerShape(cornerRadius),
            color = if (isDragging) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        ) { content() }
    } else {
        Surface(
            modifier = shared,
            shape = RoundedCornerShape(cornerRadius),
            color = if (isDragging) scheme.surfaceContainerHighest else scheme.surfaceContainerLow,
        ) { content() }
    }
}
