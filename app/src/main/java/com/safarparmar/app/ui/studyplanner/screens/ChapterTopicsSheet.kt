package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.ui.theme.SafarSemanticColors

/**
 * A chapter's topics get their own dedicated sheet instead of inlining into the subject
 * accordion — a chapter with 50 topics now scrolls in one obvious, unambiguous container
 * (this sheet's own LazyColumn) rather than fighting with the outer subject list's scroll.
 *
 * "Add Topic" is answered with an inline text row inside this same sheet rather than a
 * separate `AlertDialog` — two independent floating Compose surfaces (a dialog window and
 * this sheet's own window) race for top z-order when opened together, and the dialog kept
 * losing that race and rendering underneath the sheet. Keeping the input in this sheet's
 * own composition sidesteps that entirely: there's only one window to stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterTopicsSheet(
    chapterName: String,
    topics: List<StudyTopic>,
    onDismiss: () -> Unit,
    onAddTopic: (String) -> Unit,
    onTopicClick: (StudyTopic) -> Unit,
    onRenameTopic: (StudyTopic) -> Unit,
    onDeleteTopic: (StudyTopic) -> Unit,
    onAssignToday: (StudyTopic) -> Unit,
    canReorder: Boolean,
    onMoveTopicUp: (StudyTopic) -> Unit,
    onMoveTopicDown: (StudyTopic) -> Unit,
    onTopicDragEnd: (StudyTopic) -> Unit,
    onChangeDate: (StudyTopic) -> Unit = {},
    onMarkDoneTopic: (StudyTopic) -> Unit = {},
    onToReviseTopic: (StudyTopic) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    var showAddRow by remember { mutableStateOf(false) }
    var newTopicText by remember { mutableStateOf("") }
    // Capped to 60% of the screen (bottom half plus a 10% margin) instead of letting
    // a long topic list stretch the sheet all the way to the top — the topic list
    // scrolls within that cap instead.
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SafarSemanticColors.plannerBackground(isSystemInDarkTheme()),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).padding(bottom = 24.dp)) {
            Text(
                text = chapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (showAddRow) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newTopicText,
                        onValueChange = { newTopicText = it },
                        label = { Text("Topic name (comma-separated for multiple)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SyllabusAddButton(
                            label = "Add",
                            onClick = {
                                onAddTopic(newTopicText)
                                newTopicText = ""
                                showAddRow = false
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showAddRow = false; newTopicText = "" }) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                SyllabusAddButton(
                    label = "Add Topic",
                    onClick = { showAddRow = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            if (topics.isEmpty()) {
                Text(
                    text = "No topics yet. Tap Add Topic to add one — separate names with commas to add several at once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
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
                                onChangeDate = { onChangeDate(topic) },
                                onMarkDone = { onMarkDoneTopic(topic) },
                                onToRevise = { onToReviseTopic(topic) },
                                modifier = Modifier.animateItem(),
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
