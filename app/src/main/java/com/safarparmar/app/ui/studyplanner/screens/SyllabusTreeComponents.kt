package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.logic.percentDone
import com.safarparmar.app.ui.studyplanner.logic.readableDate

@Composable
internal fun SyllabusSubjectAccordionCard(
    subject: StudySubject,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddChapter: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddTopic: (StudyChapter) -> Unit,
    onBulkAdd: (StudyChapter) -> Unit,
    onRenameChapter: (StudyChapter) -> Unit,
    onDeleteChapter: (StudyChapter) -> Unit,
    onTopicClick: (StudyChapter, StudyTopic) -> Unit,
    onRenameTopic: (StudyChapter, StudyTopic) -> Unit,
    onDeleteTopic: (StudyChapter, StudyTopic) -> Unit,
    onAssignToday: (StudyTopic) -> Unit,
    isChapterExpanded: (String) -> Boolean,
    onToggleChapterExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val chapterCount = subject.chapters.size
    val topicCount = subject.chapters.sumOf { it.topics.size }
    val completion = subject.percentDone()
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "subjectChevron")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
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
                SubjectOverflowMenuMinimal(onRename = onRename, onDelete = onDelete)
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                )
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

            if (isExpanded) {
                if (subject.chapters.isEmpty()) {
                    Text(
                        text = "No chapters yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 8.dp, top = 4.dp),
                    ) {
                        subject.chapters.forEach { chapter ->
                            SyllabusChapterAccordionRow(
                                chapter = chapter,
                                isExpanded = isChapterExpanded(chapter.id),
                                onToggleExpand = { onToggleChapterExpand(chapter.id) },
                                onAddTopic = { onAddTopic(chapter) },
                                onBulkAdd = { onBulkAdd(chapter) },
                                onRename = { onRenameChapter(chapter) },
                                onDelete = { onDeleteChapter(chapter) },
                                onTopicClick = { topic -> onTopicClick(chapter, topic) },
                                onRenameTopic = { topic -> onRenameTopic(chapter, topic) },
                                onDeleteTopic = { topic -> onDeleteTopic(chapter, topic) },
                                onAssignToday = onAssignToday,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusChapterAccordionRow(
    chapter: StudyChapter,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddTopic: () -> Unit,
    onBulkAdd: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTopicClick: (StudyTopic) -> Unit,
    onRenameTopic: (StudyTopic) -> Unit,
    onDeleteTopic: (StudyTopic) -> Unit,
    onAssignToday: (StudyTopic) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val completion = chapter.percentDone()
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chapterChevron")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
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
            RowActionIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add topic",
                onClick = onAddTopic,
            )
            IconButton(onClick = onBulkAdd, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add Many topics",
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            SubjectOverflowMenuMinimal(onRename = onRename, onDelete = onDelete)
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation),
            )
        }

        if (isExpanded) {
            if (chapter.topics.isEmpty()) {
                Text(
                    text = "No topics yet. Tap + to add one, or Add Many to paste a list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    chapter.topics.forEachIndexed { index, topic ->
                        SyllabusTopicAccordionRow(
                            topic = topic,
                            onClick = { onTopicClick(topic) },
                            onRename = { onRenameTopic(topic) },
                            onDelete = { onDeleteTopic(topic) },
                            onAssignToday = { onAssignToday(topic) },
                        )
                        if (index < chapter.topics.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 40.dp, end = 12.dp),
                                color = scheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusTopicAccordionRow(
    topic: StudyTopic,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAssignToday: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDone = topic.status == TopicStatus.DONE
    val hasDate = !topic.plannedDate.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            StatusDot(topic.status)
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
        SubjectOverflowMenuMinimal(onRename = onRename, onDelete = onDelete)
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

/** Overflow menu reserved for Rename/Delete only — Add actions are direct-tap icons, not menu items. */
@Composable
private fun SubjectOverflowMenuMinimal(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}
