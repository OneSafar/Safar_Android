package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.ui.theme.SafarSemanticColors

/**
 * A chapter's topics get their own dedicated sheet instead of inlining into the subject
 * accordion — a chapter with 50 topics now scrolls in one obvious, unambiguous container
 * (this sheet's own LazyColumn) rather than fighting with the outer subject list's scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterTopicsSheet(
    chapterName: String,
    topics: List<StudyTopic>,
    onDismiss: () -> Unit,
    onAddTopic: () -> Unit,
    onBulkAdd: () -> Unit,
    onTopicClick: (StudyTopic) -> Unit,
    onRenameTopic: (StudyTopic) -> Unit,
    onDeleteTopic: (StudyTopic) -> Unit,
    onAssignToday: (StudyTopic) -> Unit,
    canReorder: Boolean,
    onMoveTopicUp: (StudyTopic) -> Unit,
    onMoveTopicDown: (StudyTopic) -> Unit,
    onTopicDragEnd: (StudyTopic) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SafarSemanticColors.plannerBackground(isSystemInDarkTheme()),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chapterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAddTopic) {
                    Icon(Icons.Default.Add, contentDescription = "Add topic", tint = scheme.primary)
                }
                IconButton(onClick = onBulkAdd) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add Many topics", tint = scheme.primary)
                }
            }

            if (topics.isEmpty()) {
                Text(
                    text = "No topics yet. Tap + to add one, or the list icon to paste a list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(topics, key = { _, topic -> topic.id }) { index, topic ->
                        key(topic.id) {
                            SyllabusTopicAccordionRow(
                                topic = topic,
                                onClick = { onTopicClick(topic) },
                                onRename = { onRenameTopic(topic) },
                                onDelete = { onDeleteTopic(topic) },
                                onAssignToday = { onAssignToday(topic) },
                                canReorder = canReorder,
                                onMoveUp = { onMoveTopicUp(topic) },
                                onMoveDown = { onMoveTopicDown(topic) },
                                onDragEnd = { onTopicDragEnd(topic) },
                            )
                            if (index < topics.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 40.dp, end = 20.dp),
                                    color = scheme.outlineVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
