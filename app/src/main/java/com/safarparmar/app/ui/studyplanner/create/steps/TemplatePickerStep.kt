package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.ui.studyplanner.create.DraftChapter
import com.safarparmar.app.ui.studyplanner.create.DraftSubject
import com.safarparmar.app.ui.studyplanner.create.DraftTopic
import com.safarparmar.app.ui.studyplanner.create.TemplateChapterRef
import com.safarparmar.app.ui.studyplanner.components.TextInputDialog
import com.safarparmar.app.ui.studyplanner.screens.PremiumPlannerGateCard

@Composable
fun TemplatePickerStep(
    templates: List<ExamTemplateSummary>,
    loadingTemplates: Boolean,
    selectedTemplateId: String?,
    templateDetail: ExamTemplate?,
    loadingTemplateDetail: Boolean,
    excludedTopicKeys: Set<Triple<Int, Int, Int>>,
    templateExtraChapters: Map<Int, List<DraftChapter>>,
    templateExtraTopics: Map<Pair<Int, Int>, List<DraftTopic>>,
    templateExtraSubjects: List<DraftSubject>,
    drillSubjectIndex: Int?,
    drillChapter: TemplateChapterRef?,
    canUsePremiumPlannerFeatures: Boolean,
    onUpgrade: () -> Unit,
    onSelectTemplate: (String) -> Unit,
    onDrillIntoSubject: (Int) -> Unit,
    onDrillIntoChapter: (TemplateChapterRef) -> Unit,
    onDrillBack: () -> Unit,
    onToggleTopic: (Int, Int, Int) -> Unit,
    onAddTemplateSubject: (String) -> Unit,
    onRemoveTemplateSubject: (String) -> Unit,
    onAddTemplateSubjectChapter: (String, String) -> Unit,
    onRemoveTemplateSubjectChapter: (String, String) -> Unit,
    onAddTemplateSubjectTopic: (String, String, String) -> Unit,
    onRemoveTemplateSubjectTopic: (String, String, String) -> Unit,
    onAddChapter: (Int, String) -> Unit,
    onRemoveChapter: (Int, String) -> Unit,
    onAddTopicToNewChapter: (Int, String, String) -> Unit,
    onRemoveTopicFromNewChapter: (Int, String, String) -> Unit,
    onAddTopic: (Int, Int, String) -> Unit,
    onRemoveTopic: (Int, Int, String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!canUsePremiumPlannerFeatures) {
            PremiumPlannerGateCard(
                title = "Templates are premium",
                body = "You can still create a custom plan manually for free.",
                action = "View Premium",
                onUpgrade = onUpgrade,
            )
        }

        if (selectedTemplateId == null || templateDetail == null) {
            Text("Templates", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            if (loadingTemplates) {
                CircularProgressIndicator()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(templates, key = { it.id }) { template ->
                        TemplateSummaryCard(
                            template = template,
                            loading = loadingTemplateDetail && selectedTemplateId == template.id,
                            onClick = { onSelectTemplate(template.id) },
                        )
                    }
                }
            }
            return@Column
        }

        val subjectIndex = drillSubjectIndex
        val chapterRef = drillChapter

        when {
            subjectIndex == null -> {
                var showAddSubjectDialog by remember { mutableStateOf(false) }
                Text(templateDetail.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap a subject to review its chapters and topics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        TextButton(onClick = { showAddSubjectDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Add subject", fontWeight = FontWeight.Bold)
                        }
                    }
                    items(templateDetail.subjects.size) { si ->
                        val subject = templateDetail.subjects[si]
                        val extraChapterCount = templateExtraChapters[si].orEmpty().size
                        val topicCount = subject.chapters.withIndex().sumOf { (ci, chapter) ->
                            chapter.topics.withIndex().count { (ti, _) -> Triple(si, ci, ti) !in excludedTopicKeys }
                        } + templateExtraTopics.filterKeys { it.first == si }.values.sumOf { it.size } +
                            templateExtraChapters[si].orEmpty().sumOf { it.topics.size }
                        DrillDownRow(
                            title = subject.name,
                            subtitle = "${subject.chapters.size + extraChapterCount} chapters · $topicCount topics",
                            onClick = { onDrillIntoSubject(si) },
                        )
                    }
                    itemsIndexed(templateExtraSubjects, key = { _, s -> s.localId }) { extraIndex, subject ->
                        val topicCount = subject.chapters.sumOf { it.topics.size }
                        DrillDownRow(
                            title = subject.name,
                            subtitle = "${subject.chapters.size} chapters · $topicCount topics · added by you",
                            onClick = { onDrillIntoSubject(templateDetail.subjects.size + extraIndex) },
                            onRemove = { onRemoveTemplateSubject(subject.localId) },
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
                            rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                .forEach { name -> onAddTemplateSubject(name) }
                            showAddSubjectDialog = false
                        },
                    )
                }
            }

            chapterRef == null -> {
                val isCustomSubject = subjectIndex >= templateDetail.subjects.size
                if (isCustomSubject) {
                    val customSubjectIndex = subjectIndex - templateDetail.subjects.size
                    val subject = templateExtraSubjects[customSubjectIndex]
                    var showAddChapterDialog by remember(subjectIndex) { mutableStateOf(false) }

                    DrillDownHeader(title = subject.name, onBack = onDrillBack)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            TextButton(onClick = { showAddChapterDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Add chapter", fontWeight = FontWeight.Bold)
                            }
                        }
                        items(subject.chapters, key = { it.localId }) { chapter ->
                            DrillDownRow(
                                title = chapter.name,
                                subtitle = "${chapter.topics.size} topics",
                                onClick = { onDrillIntoChapter(TemplateChapterRef.Custom(chapter.localId)) },
                                onRemove = { onRemoveTemplateSubjectChapter(subject.localId, chapter.localId) }
                            )
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
                                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    .forEach { name -> onAddTemplateSubjectChapter(subject.localId, name) }
                                showAddChapterDialog = false
                            },
                        )
                    }
                } else {
                    val subject = templateDetail.subjects[subjectIndex]
                    val extras = templateExtraChapters[subjectIndex].orEmpty()
                    var showAddChapterDialog by remember(subjectIndex) { mutableStateOf(false) }

                    DrillDownHeader(title = subject.name, onBack = onDrillBack)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            TextButton(onClick = { showAddChapterDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Add chapter", fontWeight = FontWeight.Bold)
                            }
                        }
                        items(subject.chapters.size) { ci ->
                            val chapter = subject.chapters[ci]
                            val topicCount = chapter.topics.withIndex().count { (ti, _) -> Triple(subjectIndex, ci, ti) !in excludedTopicKeys } +
                                templateExtraTopics[subjectIndex to ci].orEmpty().size
                            DrillDownRow(
                                title = chapter.name,
                                subtitle = "$topicCount topics",
                                onClick = { onDrillIntoChapter(TemplateChapterRef.Original(ci)) },
                            )
                        }
                        items(extras, key = { it.localId }) { chapter ->
                            DrillDownRow(
                                title = chapter.name,
                                subtitle = "${chapter.topics.size} topics · added by you",
                                onClick = { onDrillIntoChapter(TemplateChapterRef.Custom(chapter.localId)) },
                                onRemove = { onRemoveChapter(subjectIndex, chapter.localId) },
                            )
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
                                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    .forEach { name -> onAddChapter(subjectIndex, name) }
                                showAddChapterDialog = false
                            },
                        )
                    }
                }
            }

            else -> {
                val isCustomSubject = subjectIndex >= templateDetail.subjects.size
                if (isCustomSubject) {
                    val customSubjectIndex = subjectIndex - templateDetail.subjects.size
                    val subject = templateExtraSubjects[customSubjectIndex]
                    var showAddTopicDialog by remember(subjectIndex, chapterRef) { mutableStateOf(false) }
                    
                    val chapterId = (chapterRef as TemplateChapterRef.Custom).localId
                    val chapter = subject.chapters.first { it.localId == chapterId }
                    
                    DrillDownHeader(title = chapter.name, onBack = onDrillBack)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            TextButton(onClick = { showAddTopicDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Add topic", fontWeight = FontWeight.Bold)
                            }
                        }
                        items(chapter.topics, key = { it.localId }) { topic ->
                            TopicAddedRow(
                                name = topic.name,
                                onRemove = { onRemoveTemplateSubjectTopic(subject.localId, chapterId, topic.localId) },
                            )
                        }
                    }
                    if (showAddTopicDialog) {
                        TextInputDialog(
                            title = "Add topic",
                            label = "Topic name (comma-separated for multiple)",
                            confirmLabel = "Add",
                            emptyHint = "Please type the topic name",
                            onDismiss = { showAddTopicDialog = false },
                            onConfirm = { rawInput ->
                                rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    .forEach { name -> onAddTemplateSubjectTopic(subject.localId, chapterId, name) }
                                showAddTopicDialog = false
                            },
                        )
                    }
                } else {
                    val subject = templateDetail.subjects[subjectIndex]
                    var showAddTopicDialog by remember(subjectIndex, chapterRef) { mutableStateOf(false) }

                    when (chapterRef) {
                        is TemplateChapterRef.Original -> {
                            val chapter = subject.chapters[chapterRef.index]
                            val extraTopics = templateExtraTopics[subjectIndex to chapterRef.index].orEmpty()
                            DrillDownHeader(title = chapter.name, onBack = onDrillBack)
                            Text(
                                "Uncheck any topics you already know.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    TextButton(onClick = { showAddTopicDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                        Text("Add topic", fontWeight = FontWeight.Bold)
                                    }
                                }
                                items(chapter.topics.size) { ti ->
                                    val key = Triple(subjectIndex, chapterRef.index, ti)
                                    TopicCheckboxRow(
                                        name = chapter.topics[ti],
                                        checked = key !in excludedTopicKeys,
                                        onToggle = { onToggleTopic(subjectIndex, chapterRef.index, ti) },
                                    )
                                }
                                items(extraTopics, key = { it.localId }) { topic ->
                                    TopicAddedRow(
                                        name = topic.name,
                                        onRemove = { onRemoveTopic(subjectIndex, chapterRef.index, topic.localId) },
                                    )
                                }
                            }
                            if (showAddTopicDialog) {
                                TextInputDialog(
                                    title = "Add topic",
                                    label = "Topic name (comma-separated for multiple)",
                                    confirmLabel = "Add",
                                    emptyHint = "Please type the topic name",
                                    onDismiss = { showAddTopicDialog = false },
                                    onConfirm = { rawInput ->
                                        rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                            .forEach { name -> onAddTopic(subjectIndex, chapterRef.index, name) }
                                        showAddTopicDialog = false
                                    },
                                )
                            }
                        }

                        is TemplateChapterRef.Custom -> {
                            val chapter = templateExtraChapters[subjectIndex].orEmpty().first { it.localId == chapterRef.localId }
                            DrillDownHeader(title = chapter.name, onBack = onDrillBack)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    TextButton(onClick = { showAddTopicDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                        Text("Add topic", fontWeight = FontWeight.Bold)
                                    }
                                }
                                items(chapter.topics, key = { it.localId }) { topic ->
                                    TopicAddedRow(
                                        name = topic.name,
                                        onRemove = { onRemoveTopicFromNewChapter(subjectIndex, chapterRef.localId, topic.localId) },
                                    )
                                }
                            }
                            if (showAddTopicDialog) {
                                TextInputDialog(
                                    title = "Add topic",
                                    label = "Topic name (comma-separated for multiple)",
                                    confirmLabel = "Add",
                                    emptyHint = "Please type the topic name",
                                    onDismiss = { showAddTopicDialog = false },
                                    onConfirm = { rawInput ->
                                        rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                            .forEach { name -> onAddTopicToNewChapter(subjectIndex, chapterRef.localId, name) }
                                        showAddTopicDialog = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (subjectIndex == null) {
            Button(
                onClick = onContinue,
                enabled = canUsePremiumPlannerFeatures,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DrillDownHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onBack) {
            Text("Back", fontWeight = FontWeight.Bold)
        }
    }
    Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DrillDownRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove chapter")
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).size(width = 14.dp, height = 14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopicCheckboxRow(name: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TopicAddedRow(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove topic", modifier = Modifier.padding(2.dp))
        }
    }
}

@Composable
private fun TemplateSummaryCard(
    template: ExamTemplateSummary,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(template.name, fontWeight = FontWeight.Bold)
                val subtitle = listOfNotNull(
                    template.subjectCount?.let { "$it subjects" },
                    template.topicCount?.let { "$it topics" },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
        }
    }
}
