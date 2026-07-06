package com.safarparmar.app.ui.studyplanner.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarResultSlot
import com.safarparmar.app.ui.components.SyllabusRowSkeleton
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.SubjectUiModel
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.deleteImpact
import com.safarparmar.app.ui.studyplanner.logic.findDuplicateSiblingName
import com.safarparmar.app.ui.studyplanner.logic.todayKey

internal sealed interface SyllabusDialogState {
    object Closed : SyllabusDialogState
    object AddSubject : SyllabusDialogState
    data class RenameSubject(val subject: SubjectUiModel) : SyllabusDialogState
    data class DeleteSubject(val subject: SubjectUiModel) : SyllabusDialogState
    data class AddChapter(val subject: SubjectUiModel) : SyllabusDialogState
    data class RenameChapter(val subjectId: String, val chapter: StudyChapter) : SyllabusDialogState
    data class DeleteChapter(val subjectId: String, val chapter: StudyChapter) : SyllabusDialogState
    data class AddTopicTo(val subjectId: String, val chapterId: String) : SyllabusDialogState
    data class BulkAddTo(val subject: StudySubject, val chapter: StudyChapter) : SyllabusDialogState
    data class RenameTopic(val topic: StudyTopic) : SyllabusDialogState
    data class DeleteTopic(val topic: StudyTopic) : SyllabusDialogState
    data class DuplicateNameConfirm(val message: String, val onConfirm: () -> Unit) : SyllabusDialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusSubjectsScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlannerSectionSelect: (PlannerSection) -> Unit,
    showBottomBar: Boolean = true,
) {
    val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    var dialogState by remember { mutableStateOf<SyllabusDialogState>(SyllabusDialogState.Closed) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val expandedSubjects = remember { mutableStateMapOf<String, Boolean>() }
    val expandedChapters = remember { mutableStateMapOf<String, Boolean>() }
    var selectedTopicRef by remember { mutableStateOf<TopicRef?>(null) }
    var detailNonce by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }

    val selectedPlan = state.selectedPlan
    val rawSubjects = selectedPlan?.subjects.orEmpty()

    fun subjectMatchesQuery(subject: StudySubject, query: String): Boolean {
        val q = query.lowercase()
        if (subject.name.lowercase().contains(q)) return true
        return subject.chapters.any { chapter ->
            chapter.name.lowercase().contains(q) || chapter.topics.any { it.name.lowercase().contains(q) }
        }
    }

    fun chapterMatchesQuery(chapter: StudyChapter, query: String): Boolean {
        val q = query.lowercase()
        if (chapter.name.lowercase().contains(q)) return true
        return chapter.topics.any { it.name.lowercase().contains(q) }
    }

    fun isSubjectExpanded(subjectId: String, subject: StudySubject): Boolean {
        expandedSubjects[subjectId]?.let { return it }
        return searchQuery.isNotBlank() && subjectMatchesQuery(subject, searchQuery)
    }

    fun isChapterExpanded(subjectId: String, chapterId: String, chapter: StudyChapter): Boolean {
        val key = "$subjectId:$chapterId"
        expandedChapters[key]?.let { return it }
        return searchQuery.isNotBlank() && chapterMatchesQuery(chapter, searchQuery)
    }

    fun requestAddSubject(name: String) {
        if (findDuplicateSiblingName(name, rawSubjects.map { it.name })) {
            dialogState = SyllabusDialogState.DuplicateNameConfirm(
                message = "You already have a subject called '$name'. Add it again?",
                onConfirm = { actions.addSubject(name) },
            )
        } else {
            actions.addSubject(name)
        }
    }

    fun requestRenameSubject(subjectId: String, name: String) {
        val siblings = rawSubjects.filter { it.id != subjectId }.map { it.name }
        if (findDuplicateSiblingName(name, siblings)) {
            dialogState = SyllabusDialogState.DuplicateNameConfirm(
                message = "You already have a subject called '$name'. Add it again?",
                onConfirm = { actions.renameSubject(subjectId, name) },
            )
        } else {
            actions.renameSubject(subjectId, name)
        }
    }

    fun requestAddChapter(subjectId: String, name: String) {
        val subject = rawSubjects.find { it.id == subjectId }
        val siblings = subject?.chapters.orEmpty().map { it.name }
        if (findDuplicateSiblingName(name, siblings)) {
            dialogState = SyllabusDialogState.DuplicateNameConfirm(
                message = "You already have a chapter called '$name'. Add it again?",
                onConfirm = { actions.addChapter(subjectId, name) },
            )
        } else {
            actions.addChapter(subjectId, name)
        }
    }

    fun requestRenameChapter(subjectId: String, chapterId: String, name: String) {
        val subject = rawSubjects.find { it.id == subjectId }
        val siblings = subject?.chapters.orEmpty().filter { it.id != chapterId }.map { it.name }
        if (findDuplicateSiblingName(name, siblings)) {
            dialogState = SyllabusDialogState.DuplicateNameConfirm(
                message = "You already have a chapter called '$name'. Add it again?",
                onConfirm = { actions.renameChapter(subjectId, chapterId, name) },
            )
        } else {
            actions.renameChapter(subjectId, chapterId, name)
        }
    }

    fun requestAddTopic(subjectId: String, chapterId: String, name: String) {
        val chapter = rawSubjects.find { it.id == subjectId }?.chapters?.find { it.id == chapterId }
        val siblings = chapter?.topics.orEmpty().map { it.name }
        if (findDuplicateSiblingName(name, siblings)) {
            dialogState = SyllabusDialogState.DuplicateNameConfirm(
                message = "You already have a topic called '$name'. Add it again?",
                onConfirm = { actions.addTopic(subjectId, chapterId, name) },
            )
        } else {
            actions.addTopic(subjectId, chapterId, name)
        }
    }

    fun requestRenameTopic(topicId: String, name: String) {
        actions.updateTopic(topicId, name = name)
    }

    when (val ds = dialogState) {
        is SyllabusDialogState.AddSubject -> {
            TextInputDialog("Add Subject", "Subject name", onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestAddSubject(it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.RenameSubject -> {
            TextInputDialog("Rename Subject", ds.subject.name, onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestRenameSubject(ds.subject.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.DeleteSubject -> {
            val rawSubject = rawSubjects.find { it.id == ds.subject.id }
            val impact = rawSubject?.deleteImpact()
            val body = if (impact != null) {
                "This will remove ${impact.chapterCount} chapters and ${impact.topicCount} topics. " +
                    "${impact.scheduledTopicCount} of them already have a date set. " +
                    "You can undo this from the message that appears after."
            } else {
                "This will delete ${ds.subject.name}. You can undo this from the message that appears after."
            }
            ConfirmActionDialog("Delete this subject?", body, { dialogState = SyllabusDialogState.Closed }) {
                actions.deleteSubject(ds.subject.id)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.AddChapter -> {
            TextInputDialog("Add Chapter", "Chapter name", onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestAddChapter(ds.subject.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.RenameChapter -> {
            TextInputDialog("Rename Chapter", ds.chapter.name, onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestRenameChapter(ds.subjectId, ds.chapter.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.DeleteChapter -> {
            val impact = ds.chapter.deleteImpact()
            val body = "This will remove ${impact.topicCount} topics. ${impact.scheduledTopicCount} of them already have a date set. " +
                "You can undo this from the message that appears after."
            ConfirmActionDialog("Delete this chapter?", body, { dialogState = SyllabusDialogState.Closed }) {
                actions.deleteChapter(ds.subjectId, ds.chapter.id)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.AddTopicTo -> {
            TextInputDialog("Add Topic", "Topic name", onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestAddTopic(ds.subjectId, ds.chapterId, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.BulkAddTo -> {
            BulkAddSheet(Pair(ds.subject, ds.chapter), state, actions, onDismiss = { dialogState = SyllabusDialogState.Closed })
        }
        is SyllabusDialogState.RenameTopic -> {
            TextInputDialog("Rename Topic", ds.topic.name, onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                requestRenameTopic(ds.topic.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.DeleteTopic -> {
            ConfirmActionDialog(
                "Delete this topic?",
                "This will remove '${ds.topic.name}'. You can undo this from the message that appears after.",
                { dialogState = SyllabusDialogState.Closed },
            ) {
                actions.deleteTopic(ds.topic.id)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.DuplicateNameConfirm -> {
            AlertDialog(
                onDismissRequest = { dialogState = SyllabusDialogState.Closed },
                title = { Text("Name already used") },
                text = { Text(ds.message) },
                confirmButton = {
                    TextButton(onClick = { ds.onConfirm(); dialogState = SyllabusDialogState.Closed }) {
                        Text("Add Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogState = SyllabusDialogState.Closed }) { Text("Cancel") }
                },
            )
        }
        SyllabusDialogState.Closed -> {}
    }

    selectedTopicRef?.let { ref ->
        PlannerTopicDetailSheet(
            ref = ref,
            openNonce = detailNonce,
            actions = actions,
            swapCandidates = selectedPlan?.let { plan ->
                plan.subjects.flatMap { s -> s.chapters.flatMap { c -> c.topics.map { t -> TopicRef(s, c, t) } } }
            }.orEmpty(),
            onDismiss = { selectedTopicRef = null },
        )
    }

    val totalTopics = rawSubjects.sumOf { subject -> subject.chapters.sumOf { it.topics.size } }
    val totalChapters = rawSubjects.sumOf { it.chapters.size }
    val doneTopics = rawSubjects.sumOf { subject ->
        subject.chapters.sumOf { chapter -> chapter.topics.count { it.status == TopicStatus.DONE } }
    }
    val planProgress = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0
    val isTemplatePlan = !selectedPlan?.templateId.isNullOrBlank()
    val shouldShowFullImport = rawSubjects.isEmpty() && !isTemplatePlan

    val filteredSubjects = remember(rawSubjects, searchQuery) {
        if (searchQuery.isBlank()) rawSubjects
        else rawSubjects.filter { subjectMatchesQuery(it, searchQuery) }
    }

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            ),
            bottomBar = {
                if (showBottomBar) {
                    PlannerBottomBar(
                        selected = PlannerSection.SYLLABUS,
                        onSelect = onPlannerSectionSelect,
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { dialogState = SyllabusDialogState.AddSubject },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.error != null && subjects.isEmpty() && !state.loading -> {
                        SafarResultSlot(modifier = Modifier.fillMaxSize()) {
                            SafarErrorState(message = state.error!!, onRetry = { actions.refreshPlans() })
                        }
                    }
                    state.loading && subjects.isEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(4) { SyllabusRowSkeleton() }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item {
                                SyllabusOverviewCard(
                                    planTitle = selectedPlan?.title.orEmpty().ifBlank { "Study Plan" },
                                    examType = selectedPlan?.examType,
                                    progress = planProgress,
                                    subjectCount = rawSubjects.size,
                                    chapterCount = totalChapters,
                                    topicCount = totalTopics,
                                    isTemplatePlan = isTemplatePlan,
                                )
                            }

                            item {
                                SyllabusQuickActions(
                                    onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                    onBuildPlanner = { actions.autoDistribute(lockExisting = true) },
                                    canBuildPlanner = rawSubjects.isNotEmpty(),
                                )
                            }

                            if (shouldShowFullImport) {
                                item {
                                    SyllabusFullImportCard(
                                        state = state,
                                        actions = actions,
                                        canUseAiImport = premiumStatus.canUseStudyPlannerInsights,
                                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                                    )
                                }
                            }

                            if (rawSubjects.isNotEmpty()) {
                                item {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search subjects, chapters, topics") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                    )
                                }
                            }

                            item {
                                SyllabusSectionHeader(
                                    title = if (isTemplatePlan) "Template syllabus" else "Your syllabus",
                                    subtitle = "$totalChapters chapters • $totalTopics topics",
                                )
                            }

                            if (rawSubjects.isEmpty()) {
                                item {
                                    SyllabusEmptySubjectsCard(
                                        onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                    )
                                }
                            } else if (filteredSubjects.isEmpty()) {
                                item {
                                    Text(
                                        text = "No matches for '$searchQuery'",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 24.dp),
                                    )
                                }
                            } else {
                                items(filteredSubjects, key = { it.id }) { subject ->
                                    SyllabusSubjectAccordionCard(
                                        subject = subject,
                                        isExpanded = isSubjectExpanded(subject.id, subject),
                                        onToggleExpand = {
                                            expandedSubjects[subject.id] = !isSubjectExpanded(subject.id, subject)
                                        },
                                        onAddChapter = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.AddChapter(it) }
                                        },
                                        onRename = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.RenameSubject(it) }
                                        },
                                        onDelete = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.DeleteSubject(it) }
                                        },
                                        onAddTopic = { chapter -> dialogState = SyllabusDialogState.AddTopicTo(subject.id, chapter.id) },
                                        onBulkAdd = { chapter -> dialogState = SyllabusDialogState.BulkAddTo(subject, chapter) },
                                        onRenameChapter = { chapter -> dialogState = SyllabusDialogState.RenameChapter(subject.id, chapter) },
                                        onDeleteChapter = { chapter -> dialogState = SyllabusDialogState.DeleteChapter(subject.id, chapter) },
                                        onTopicClick = { chapter, topic ->
                                            selectedTopicRef = TopicRef(subject, chapter, topic)
                                            detailNonce++
                                        },
                                        onRenameTopic = { _, topic -> dialogState = SyllabusDialogState.RenameTopic(topic) },
                                        onDeleteTopic = { _, topic -> dialogState = SyllabusDialogState.DeleteTopic(topic) },
                                        onAssignToday = { topic -> actions.updateTopic(topic.id, plannedDate = todayKey(), pinned = true) },
                                        isChapterExpanded = { chapterId ->
                                            val chapter = subject.chapters.find { it.id == chapterId }
                                            chapter != null && isChapterExpanded(subject.id, chapterId, chapter)
                                        },
                                        onToggleChapterExpand = { chapterId ->
                                            val key = "${subject.id}:$chapterId"
                                            val chapter = subject.chapters.find { it.id == chapterId }
                                            val current = chapter != null && isChapterExpanded(subject.id, chapterId, chapter)
                                            expandedChapters[key] = !current
                                        },
                                    )
                                }
                            }

                            if (!shouldShowFullImport && rawSubjects.isNotEmpty()) {
                                item {
                                    SyllabusImportTray(
                                        isTemplatePlan = isTemplatePlan,
                                        state = state,
                                        actions = actions,
                                        canUseAiImport = premiumStatus.canUseStudyPlannerInsights,
                                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusOverviewCard(
    planTitle: String,
    examType: String?,
    progress: Int,
    subjectCount: Int,
    chapterCount: Int,
    topicCount: Int,
    isTemplatePlan: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = planTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(examType?.takeIf { it.isNotBlank() }, if (isTemplatePlan) "Predefined template" else "Custom syllabus").joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyllabusMetaPill(label = "Subjects", value = subjectCount.toString(), modifier = Modifier.weight(1f))
                SyllabusMetaPill(label = "Chapters", value = chapterCount.toString(), modifier = Modifier.weight(1f))
                SyllabusMetaPill(label = "Topics", value = topicCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SyllabusMetaPill(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyllabusQuickActions(
    onAddSubject: () -> Unit,
    onBuildPlanner: () -> Unit,
    canBuildPlanner: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onAddSubject,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Subject", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
            onClick = onBuildPlanner,
            enabled = canBuildPlanner,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Build Planner", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SyllabusImportTray(
    isTemplatePlan: Boolean,
    state: StudyPlannerUiState,
    actions: PlannerActions,
    canUseAiImport: Boolean,
    onUpgrade: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isTemplatePlan) "Need to update this syllabus?" else "Import or update syllabus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = if (isTemplatePlan) "Open only when you want to add or replace template topics." else "Paste syllabus text when you want to merge or replace topics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    SyllabusFullImportCard(
                        state = state,
                        actions = actions,
                        canUseAiImport = canUseAiImport,
                        onUpgrade = onUpgrade,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusSectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SyllabusEmptySubjectsCard(onAddSubject: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, scheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = scheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No subjects yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add your first subject to build your syllabus.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onAddSubject,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text("Add Subject", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun SubjectInitialBadge(name: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "S" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onPrimaryContainer,
        )
    }
}
