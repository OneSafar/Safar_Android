package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.studyplanner.components.TextInputDialog
import com.safarparmar.app.ui.studyplanner.create.DraftChapter
import com.safarparmar.app.ui.studyplanner.create.DraftSubject

@Composable
fun ManualTopicTreeStep(
    title: String,
    subjects: List<DraftSubject>,
    validationError: String?,
    onTitleChange: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onRemoveSubject: (String) -> Unit,
    onAddChapter: (String, String) -> Unit,
    onRemoveChapter: (String, String) -> Unit,
    onAddTopic: (String, String, String) -> Unit,
    onRemoveTopic: (String, String, String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    val totalTopics = subjects.sumOf { s -> s.chapters.sumOf { it.topics.size } }

    Column(
        modifier = modifier.fillMaxWidth().imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Add the subjects, chapters, and topics you want to study.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Plan title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = { showAddSubjectDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Add your first subject".takeIf { subjects.isEmpty() } ?: "Add subject", fontWeight = FontWeight.Bold)
                }
            }
            items(subjects, key = { it.localId }) { subject ->
                ManualSubjectCard(
                    subject = subject,
                    onRemoveSubject = { onRemoveSubject(subject.localId) },
                    onAddChapter = { name -> onAddChapter(subject.localId, name) },
                    onRemoveChapter = { chapterId -> onRemoveChapter(subject.localId, chapterId) },
                    onAddTopic = { chapterId, name -> onAddTopic(subject.localId, chapterId, name) },
                    onRemoveTopic = { chapterId, topicId -> onRemoveTopic(subject.localId, chapterId, topicId) },
                )
            }
            if (subjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
        }

        validationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = onContinue,
            enabled = totalTopics > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(18.dp),
            )
        }
    }

    if (showAddSubjectDialog) {
        TextInputDialog(
            title = "Add subject",
            label = "Subject name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Please type the subject name",
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { rawInput ->
                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(onAddSubject)
                showAddSubjectDialog = false
            },
        )
    }
}

@Composable
private fun ManualSubjectCard(
    subject: DraftSubject,
    onRemoveSubject: () -> Unit,
    onAddChapter: (String) -> Unit,
    onRemoveChapter: (String) -> Unit,
    onAddTopic: (String, String) -> Unit,
    onRemoveTopic: (String, String) -> Unit,
) {
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var addTopicForChapterId by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(subject.name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemoveSubject) {
                    Icon(Icons.Default.Close, contentDescription = "Remove subject")
                }
            }
            subject.chapters.forEach { chapter ->
                ManualChapterRow(
                    chapter = chapter,
                    onRemoveChapter = { onRemoveChapter(chapter.localId) },
                    onAddTopic = { addTopicForChapterId = chapter.localId },
                    onRemoveTopic = { topicId -> onRemoveTopic(chapter.localId, topicId) },
                )
            }
            TextButton(onClick = { showAddChapterDialog = true }) {
                Text("Add chapter", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddChapterDialog) {
        TextInputDialog(
            title = "Add chapter",
            label = "Chapter name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Please type the chapter name",
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { rawInput ->
                // A comma in the typed name is a bulk add, same as the live Syllabus screen —
                // "Motion, Gravitation, Work and Energy" adds all three chapters at once.
                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(onAddChapter)
                showAddChapterDialog = false
            },
        )
    }

    addTopicForChapterId?.let { chapterId ->
        TextInputDialog(
            title = "Add topic",
            label = "Topic name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Please type the topic name",
            onDismiss = { addTopicForChapterId = null },
            onConfirm = { rawInput ->
                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    .forEach { name -> onAddTopic(chapterId, name) }
                addTopicForChapterId = null
            },
        )
    }
}

@Composable
private fun ManualChapterRow(
    chapter: DraftChapter,
    onRemoveChapter: () -> Unit,
    onAddTopic: () -> Unit,
    onRemoveTopic: (String) -> Unit,
) {
    Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(chapter.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemoveChapter) {
                Icon(Icons.Default.Close, contentDescription = "Remove chapter", modifier = Modifier.padding(2.dp))
            }
        }
        chapter.topics.forEach { topic ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(topic.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemoveTopic(topic.localId) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove topic", modifier = Modifier.padding(2.dp))
                }
            }
        }
        TextButton(onClick = onAddTopic) {
            Text("Add topic", style = MaterialTheme.typography.labelMedium)
        }
    }
}
