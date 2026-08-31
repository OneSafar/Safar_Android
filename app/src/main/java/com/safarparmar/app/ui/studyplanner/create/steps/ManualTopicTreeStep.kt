// Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4
// Hallmark · genre: modern-minimal · reference: Kavach Analytics · designed-as-app
package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.studyplanner.components.TextInputDialog
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.create.DraftChapter
import com.safarparmar.app.ui.studyplanner.create.DraftSubject

private data class BuilderPalette(
    val ink: Color,
    val muted: Color,
    val rule: Color,
    val surface: Color,
    val subject: Color,
    val chapter: Color,
    val topic: Color,
    val success: Color,
)

@Composable
private fun builderPalette() = BuilderPalette(
    ink = MaterialTheme.colorScheme.onSurface,
    muted = MaterialTheme.colorScheme.onSurfaceVariant,
    rule = MaterialTheme.colorScheme.outlineVariant,
    surface = MaterialTheme.colorScheme.surfaceContainerLow,
    subject = Color(0xFF7357B6),
    chapter = Color(0xFF287BA3),
    topic = Color(0xFF18845F),
    success = Color(0xFF18845F),
)

@Composable
fun ManualTopicTreeStep(
    title: String,
    subjects: List<DraftSubject>,
    validationError: String?,
    isSaving: Boolean,
    isAutosaving: Boolean,
    onTitleChange: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onRemoveSubject: (String) -> Unit,
    onAddChapter: (String, String) -> Unit,
    onRemoveChapter: (String, String) -> Unit,
    onAddTopic: (String, String, String) -> Unit,
    onRemoveTopic: (String, String, String) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = builderPalette()
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    val totalChapters = subjects.sumOf { it.chapters.size }
    val totalTopics = subjects.sumOf { subject -> subject.chapters.sumOf { it.topics.size } }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        BuilderHeader(isAutosaving = isAutosaving, onBack = onBack, colors = colors)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Syllabus name",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink,
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("e.g. SSC CGL Tier 1 — 2026") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = colors.rule,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { HierarchyKey(colors) }

            if (subjects.isEmpty()) {
                item {
                    EmptyBuilderState(colors = colors, onAddSubject = { showAddSubjectDialog = true })
                }
            } else {
                items(subjects, key = { it.localId }) { subject ->
                    SubjectSection(
                        subject = subject,
                        colors = colors,
                        onRemoveSubject = { onRemoveSubject(subject.localId) },
                        onAddChapter = { onAddChapter(subject.localId, it) },
                        onRemoveChapter = { onRemoveChapter(subject.localId, it) },
                        onAddTopic = { chapterId, name -> onAddTopic(subject.localId, chapterId, name) },
                        onRemoveTopic = { chapterId, topicId -> onRemoveTopic(subject.localId, chapterId, topicId) },
                    )
                }

                item {
                    TextButton(onClick = { showAddSubjectDialog = true }, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add subject", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        validationError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        BuilderFooter(
            subjectCount = subjects.size,
            chapterCount = totalChapters,
            topicCount = totalTopics,
            isSaving = isSaving,
            onContinue = onContinue,
            colors = colors,
        )
    }

    if (showAddSubjectDialog) {
        TextInputDialog(
            title = "Add subject",
            label = "Subject name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Type a subject name",
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { rawInput ->
                rawInput.split(',').map(String::trim).filter(String::isNotBlank).forEach(onAddSubject)
                showAddSubjectDialog = false
            },
        )
    }
}

@Composable
private fun BuilderHeader(isAutosaving: Boolean, onBack: (() -> Unit)?, colors: BuilderPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = "Build syllabus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isAutosaving) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = colors.muted)
                Text("Saving…", style = MaterialTheme.typography.labelMedium, color = colors.muted)
            } else {
                Box(Modifier.size(7.dp).background(colors.success, CircleShape))
                Text("Draft saved", style = MaterialTheme.typography.labelMedium, color = colors.muted)
            }
        }
    }
    HorizontalDivider(color = colors.rule)
}

@Composable
private fun HierarchyKey(colors: BuilderPalette) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        LevelKey("Subject", colors.subject)
        LevelKey("Chapter", colors.chapter)
        LevelKey("Topic", colors.topic)
    }
}

@Composable
private fun LevelKey(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyBuilderState(colors: BuilderPalette, onAddSubject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).background(colors.subject, CircleShape))
        Text("No subjects yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Add a subject, then organise its chapters and topics.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )
        TextButton(onClick = onAddSubject, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add first subject", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SubjectSection(
    subject: DraftSubject,
    colors: BuilderPalette,
    onRemoveSubject: () -> Unit,
    onAddChapter: (String) -> Unit,
    onRemoveChapter: (String) -> Unit,
    onAddTopic: (String, String) -> Unit,
    onRemoveTopic: (String, String) -> Unit,
) {
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var addTopicForChapterId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.rule),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(colors.subject, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemoveSubject, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${subject.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (subject.chapters.isEmpty()) {
                Text(
                    "No chapters yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(start = 19.dp, top = 4.dp, bottom = 4.dp),
                )
            } else {
                subject.chapters.forEachIndexed { index, chapter ->
                    if (index > 0) HorizontalDivider(color = colors.rule, modifier = Modifier.padding(start = 19.dp))
                    ChapterSection(
                        index = index + 1,
                        chapter = chapter,
                        colors = colors,
                        onRemoveChapter = { onRemoveChapter(chapter.localId) },
                        onAddTopic = { addTopicForChapterId = chapter.localId },
                        onRemoveTopic = { onRemoveTopic(chapter.localId, it) },
                    )
                }
            }

            TextButton(onClick = { showAddChapterDialog = true }, modifier = Modifier.height(44.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add chapter", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showAddChapterDialog) {
        TextInputDialog(
            title = "Add chapter",
            label = "Chapter name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Type a chapter name",
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { rawInput ->
                rawInput.split(',').map(String::trim).filter(String::isNotBlank).forEach(onAddChapter)
                showAddChapterDialog = false
            },
        )
    }

    addTopicForChapterId?.let { chapterId ->
        TextInputDialog(
            title = "Add topic",
            label = "Topic name (comma-separated for multiple)",
            confirmLabel = "Add",
            emptyHint = "Type a topic name",
            onDismiss = { addTopicForChapterId = null },
            onConfirm = { rawInput ->
                rawInput.split(',').map(String::trim).filter(String::isNotBlank).forEach { onAddTopic(chapterId, it) }
                addTopicForChapterId = null
            },
        )
    }
}

@Composable
private fun ChapterSection(
    index: Int,
    chapter: DraftChapter,
    colors: BuilderPalette,
    onRemoveChapter: () -> Unit,
    onAddTopic: () -> Unit,
    onRemoveTopic: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 19.dp, top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(colors.chapter, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                "$index. ${chapter.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAddTopic, modifier = Modifier.height(44.dp)) {
                Text("Add topic", fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onRemoveChapter, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete ${chapter.name}", tint = colors.muted)
            }
        }

        chapter.topics.forEach { topic ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).background(colors.topic, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemoveTopic(topic.localId) }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete ${topic.name}",
                        tint = colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BuilderFooter(
    subjectCount: Int,
    chapterCount: Int,
    topicCount: Int,
    isSaving: Boolean,
    onContinue: () -> Unit,
    colors: BuilderPalette,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "$subjectCount subjects  ·  $chapterCount chapters  ·  $topicCount topics",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            Button(
                onClick = onContinue,
                enabled = topicCount > 0 && !isSaving,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PlannerFlatColors.PrimaryAccent),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(if (isSaving) "Saving…" else "Plan settings", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
