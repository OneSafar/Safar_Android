package com.safarparmar.app.ui.studyplanner.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.TextInputDialog
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.SubjectUiModel
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
    data class RenameTopic(val topic: StudyTopic) : SyllabusDialogState
    data class DeleteTopic(val topic: StudyTopic) : SyllabusDialogState
    data class ReviseTopic(val topic: StudyTopic) : SyllabusDialogState
    data class DuplicateNameConfirm(val message: String, val onConfirm: () -> Unit) : SyllabusDialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusSubjectsScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
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
    var activeSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    // Hoisted here (rather than owned by the chapter row itself) so the sheet always
    // reads its topic list fresh from the same reactive `localSubjects`/`state` this
    // whole screen renders from — the topic row previously owned its own "is the sheet
    // open" flag and closed over a `chapter` parameter that could lag one recomposition
    // behind a just-added topic, which is what made a freshly added topic only show up
    // after leaving and reopening the sheet.
    var openTopicsChapterId by rememberSaveable { mutableStateOf<String?>(null) }
    var topicForDatePicker by remember { mutableStateOf<StudyTopic?>(null) }
    var showReorderBuildInfo by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (activeSubjectId != null) activeSubjectId = null else onBack()
    }

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }

    val selectedPlan = state.selectedPlan
    val rawSubjects = selectedPlan?.subjects.orEmpty()
    var localSubjects by remember(rawSubjects) { mutableStateOf(rawSubjects) }

    fun subjectMatchesQuery(subject: StudySubject, query: String): Boolean {
        val q = query.lowercase()
        if (subject.name.lowercase().contains(q)) return true
        return subject.chapters.any { chapter ->
            chapter.name.lowercase().contains(q) || chapter.topics.any { it.name.lowercase().contains(q) }
        }
    }



    // A comma in the typed name is treated as a bulk add, same as chapters and topics —
    // "Physics, Chemistry, Maths" adds all three subjects in one go. Duplicate-name
    // confirmation only applies to the single-subject path; a batch is added as-is.
    fun requestAddSubject(rawInput: String) {
        val names = rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
        when (names.size) {
            0 -> Unit
            1 -> {
                val name = names[0]
                if (findDuplicateSiblingName(name, rawSubjects.map { it.name })) {
                    dialogState = SyllabusDialogState.DuplicateNameConfirm(
                        message = "You already have a subject called '$name'. Add it again?",
                        onConfirm = { actions.addSubject(name) },
                    )
                } else {
                    actions.addSubject(name)
                }
            }
            else -> actions.addSubjects(names)
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

    // A comma in the typed name is treated as a bulk add, same as topics —
    // "Motion, Gravitation, Work and Energy" adds all three chapters in one go.
    // Duplicate-name confirmation only applies to the single-chapter path; a batch
    // is added as-is (a duplicate confirm dialog per item wouldn't scale to a list).
    fun requestAddChapter(subjectId: String, rawInput: String) {
        val names = rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
        when (names.size) {
            0 -> Unit
            1 -> {
                val name = names[0]
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
            else -> actions.addChapters(subjectId, names)
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

    // A comma in the typed name is treated as a bulk add — "Analogy, Blood Relations,
    // Coding-Decoding" adds all three in one go instead of requiring the removed
    // separate "Add Many topics" sheet. Duplicate-name confirmation only applies to
    // the single-topic path; a batch is added as-is (a duplicate confirm dialog per
    // item wouldn't scale to a comma-separated list).
    fun requestAddTopic(subjectId: String, chapterId: String, rawInput: String) {
        val names = rawInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
        when (names.size) {
            0 -> Unit
            1 -> {
                val name = names[0]
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
            else -> actions.addTopics(subjectId, chapterId, names)
        }
    }

    fun requestRenameTopic(topicId: String, name: String) {
        actions.updateTopic(topicId, name = name)
    }

    val totalTopics = localSubjects.sumOf { subject -> subject.chapters.sumOf { it.topics.size } }
    val totalChapters = localSubjects.sumOf { it.chapters.size }
    val doneTopics = localSubjects.sumOf { subject ->
        subject.chapters.sumOf { chapter -> chapter.topics.count { it.status == TopicStatus.DONE } }
    }
    val planProgress = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0
    val isTemplatePlan = !selectedPlan?.templateId.isNullOrBlank()
    val shouldShowFullImport = localSubjects.isEmpty() && !isTemplatePlan

    val filteredSubjects = remember(localSubjects, searchQuery) {
        if (searchQuery.isBlank()) localSubjects
        else localSubjects.filter { subjectMatchesQuery(it, searchQuery) }
    }
    val canReorderSyllabus = searchQuery.isBlank() && !state.mutating

    fun <T> moveItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
        return items.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun moveSubject(subjectId: String, direction: Int) {
        val fromIndex = localSubjects.indexOfFirst { it.id == subjectId }
        val nextSubjects = moveItem(localSubjects, fromIndex, fromIndex + direction)
        if (nextSubjects !== localSubjects) {
            localSubjects = nextSubjects
        }
    }

    fun saveSubjectOrder() {
        actions.reorderSyllabus(subjectIds = localSubjects.map { it.id })
    }

    fun moveChapter(subjectId: String, chapterId: String, direction: Int) {
        val subjectIndex = localSubjects.indexOfFirst { it.id == subjectId }
        if (subjectIndex == -1) return
        val subject = localSubjects[subjectIndex]
        val fromIndex = subject.chapters.indexOfFirst { it.id == chapterId }
        val nextChapters = moveItem(subject.chapters, fromIndex, fromIndex + direction)
        if (nextChapters !== subject.chapters) {
            localSubjects = localSubjects.toMutableList().apply {
                this[subjectIndex] = subject.copy(chapters = nextChapters)
            }
        }
    }

    fun saveChapterOrder(subjectId: String) {
        val subject = localSubjects.find { it.id == subjectId } ?: return
        actions.reorderSyllabus(chapterIdsBySubjectId = mapOf(subjectId to subject.chapters.map { it.id }))
    }

    fun moveTopic(chapterId: String, topicId: String, direction: Int) {
        var subjectIndex = -1
        var chapterIndex = -1
        for (si in localSubjects.indices) {
            val ci = localSubjects[si].chapters.indexOfFirst { it.id == chapterId }
            if (ci != -1) {
                subjectIndex = si
                chapterIndex = ci
                break
            }
        }
        if (subjectIndex == -1 || chapterIndex == -1) return
        val subject = localSubjects[subjectIndex]
        val chapter = subject.chapters[chapterIndex]
        val fromIndex = chapter.topics.indexOfFirst { it.id == topicId }
        val nextTopics = moveItem(chapter.topics, fromIndex, fromIndex + direction)
        if (nextTopics !== chapter.topics) {
            localSubjects = localSubjects.toMutableList().apply {
                this[subjectIndex] = subject.copy(
                    chapters = subject.chapters.toMutableList().apply {
                        this[chapterIndex] = chapter.copy(topics = nextTopics)
                    }
                )
            }
        }
    }

    fun saveTopicOrder(chapterId: String) {
        val chapter = localSubjects.asSequence()
            .flatMap { it.chapters.asSequence() }
            .firstOrNull { it.id == chapterId } ?: return
        actions.reorderSyllabus(topicIdsByChapterId = mapOf(chapterId to chapter.topics.map { it.id }))
    }

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        androidx.compose.animation.AnimatedContent(
            targetState = activeSubjectId,
            label = "drill_down",
            transitionSpec = {
                (androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn()).togetherWith(
                    androidx.compose.animation.slideOutHorizontally { -it } + androidx.compose.animation.fadeOut()
                )
            }
        ) { currentSubjectId ->
            if (currentSubjectId != null) {
                val subject = localSubjects.find { it.id == currentSubjectId }
                if (subject == null) {
                    activeSubjectId = null
                    return@AnimatedContent
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground(isDarkTheme),
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        androidx.compose.material3.TopAppBar(
                            title = { Text(subject.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            navigationIcon = {
                                IconButton(onClick = { activeSubjectId = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            )
                        )
                    },
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SyllabusAddButton(
                                label = "Add Chapter",
                                onClick = {
                                    subjects.firstOrNull { it.id == subject.id }?.let {
                                        dialogState = SyllabusDialogState.AddChapter(it)
                                    }
                                },
                            )
                        }
                        if (subject.chapters.isEmpty()) {
                            item {
                                Text(
                                    text = "No chapters yet. Tap Add Chapter to add one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        } else {
                            items(subject.chapters, key = { it.id }) { chapter ->
                                SyllabusChapterAccordionRow(
                                    chapter = chapter,
                                    onOpenTopics = { openTopicsChapterId = chapter.id },
                                    onRename = { dialogState = SyllabusDialogState.RenameChapter(subject.id, chapter) },
                                    onDelete = { dialogState = SyllabusDialogState.DeleteChapter(subject.id, chapter) },
                                    onMarkDone = { actions.batchMarkTopicsDone(chapter.topics.map { it.id }) },
                                    canReorder = canReorderSyllabus,
                                    onMoveChapterUp = { moveChapter(subject.id, chapter.id, -1) },
                                    onMoveChapterDown = { moveChapter(subject.id, chapter.id, 1) },
                                    onDragEnd = { saveChapterOrder(subject.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                // Looked up fresh from `subject.chapters` (itself derived from
                // `localSubjects`/`state.selectedPlan`) on every recomposition, keyed
                // only by id — so a topic added while this sheet is open shows up
                // immediately instead of needing the sheet to be reopened.
                val openChapter = openTopicsChapterId?.let { id -> subject.chapters.find { it.id == id } }
                if (openChapter != null) {
                    ChapterTopicsSheet(
                        chapterName = openChapter.name,
                        topics = openChapter.topics,
                        isDarkTheme = isDarkTheme,
                        onDismiss = { openTopicsChapterId = null },
                        onAddTopic = { name -> requestAddTopic(subject.id, openChapter.id, name) },
                        // Renaming/deleting/scheduling a topic are all reachable from its
                        // overflow menu — there's no separate detail sheet to open on tap
                        // (that sheet was removed).
                        onTopicClick = {},
                        onRenameTopic = { topic -> dialogState = SyllabusDialogState.RenameTopic(topic) },
                        onDeleteTopic = { topic -> dialogState = SyllabusDialogState.DeleteTopic(topic) },
                        onAssignToday = { topic -> actions.updateTopic(topic.id, plannedDate = todayKey(), pinned = true) },
                        canReorder = canReorderSyllabus,
                        onMoveTopicUp = { topic -> moveTopic(openChapter.id, topic.id, -1) },
                        onMoveTopicDown = { topic -> moveTopic(openChapter.id, topic.id, 1) },
                        onTopicDragEnd = { saveTopicOrder(openChapter.id) },
                        onChangeDate = { topic -> topicForDatePicker = topic },
                        onMarkDoneTopic = { topic -> actions.batchMarkTopicsDone(listOf(topic.id)) },
                        onToReviseTopic = { topic -> dialogState = SyllabusDialogState.ReviseTopic(topic) },
                    )
                }
            } else {


        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground(isDarkTheme),
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
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item {
                                SyllabusOverviewCard(
                                    planTitle = selectedPlan?.title.orEmpty().ifBlank { "Study Plan" },
                                    examType = selectedPlan?.examType,
                                    progress = planProgress,
                                    subjectCount = localSubjects.size,
                                    chapterCount = totalChapters,
                                    topicCount = totalTopics,
                                    isTemplatePlan = isTemplatePlan,
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SyllabusBuildButton(
                                        onClick = {
                                            actions.autoDistribute(
                                                lockExisting = false,
                                                strategy = "sequential",
                                                preserveToday = true,
                                            )
                                        },
                                        enabled = localSubjects.isNotEmpty() && !state.mutating,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { showReorderBuildInfo = true },
                                        enabled = !state.mutating,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                RoundedCornerShape(14.dp),
                                            ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Visibility,
                                            contentDescription = "What does Build re-ordered syllabus do?",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }



                            if (localSubjects.isNotEmpty()) {
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
                                    title = if (isTemplatePlan) "Syllabus" else "Your syllabus",
                                    subtitle = if (isTemplatePlan) null else "$totalChapters chapters • $totalTopics topics",
                                )
                            }

                            if (localSubjects.isEmpty() && !shouldShowFullImport) {
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
                                item {
                                    SyllabusAddButton(
                                        label = "Add Subject",
                                        onClick = { dialogState = SyllabusDialogState.AddSubject },
                                    )
                                }
                                items(filteredSubjects, key = { it.id }) { subject ->
                                    SyllabusSubjectAccordionCard(
                                        subject = subject,
                                        onClick = { activeSubjectId = subject.id },
                                        onAddChapter = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.AddChapter(it) }
                                        },
                                        onRename = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.RenameSubject(it) }
                                        },
                                        onDelete = {
                                            subjects.firstOrNull { it.id == subject.id }?.let { dialogState = SyllabusDialogState.DeleteSubject(it) }
                                        },
                                        onMarkDone = {
                                            actions.batchMarkTopicsDone(subject.chapters.flatMap { it.topics }.map { it.id })
                                        },
                                        canReorder = canReorderSyllabus,
                                        onMoveSubjectUp = { moveSubject(subject.id, -1) },
                                        onMoveSubjectDown = { moveSubject(subject.id, 1) },
                                        onDragEnd = { saveSubjectOrder() },
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
    }

    // Rendered after the Scaffold (which owns the ChapterTopicsSheet bottom sheet
    // deep in its subject list) so these dialogs compose — and therefore attach
    // their window — last. Two floating Compose surfaces stack in window-attach
    // order, not source position within the tree; putting these dialogs first used
    // to mean the bottom sheet's window attached afterward and painted on top of
    // the "Add Topic" dialog, hiding it. Composing them last here fixes that for
    // every dialog, not just AddTopicTo.
    if (topicForDatePicker != null) {
        val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val picked = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    return !picked.isBefore(today)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { topicForDatePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val ld = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                            topicForDatePicker?.let { topic ->
                                actions.updateTopic(topicId = topic.id, plannedDate = ld.toString(), pinned = true)
                            }
                        }
                        topicForDatePicker = null
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { topicForDatePicker = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    when (val ds = dialogState) {
        is SyllabusDialogState.AddSubject -> {
            TextInputDialog(
                "Add Subject",
                "Subject name (comma-separated for multiple)",
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                confirmLabel = "Add",
                emptyHint = "Please type the subject name",
            ) {
                requestAddSubject(it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.RenameSubject -> {
            TextInputDialog(
                "Rename Subject",
                ds.subject.name,
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                emptyHint = "Please type the subject name",
            ) {
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
            TextInputDialog(
                "Add Chapter",
                "Chapter name (comma-separated for multiple)",
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                confirmLabel = "Add",
                emptyHint = "Please type the chapter name",
            ) {
                requestAddChapter(ds.subject.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.RenameChapter -> {
            TextInputDialog(
                "Rename Chapter",
                ds.chapter.name,
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                emptyHint = "Please type the chapter name",
            ) {
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
        is SyllabusDialogState.RenameTopic -> {
            TextInputDialog(
                "Rename Topic",
                ds.topic.name,
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                emptyHint = "Please type the topic name",
            ) {
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
        is SyllabusDialogState.ReviseTopic -> {
            com.safarparmar.app.ui.studyplanner.plan.RevisionScheduleSheet(
                topicName = ds.topic.name,
                examDate = state.selectedPlan?.examDate,
                onRevisionScheduled = { dates, scheduleType ->
                    actions.markForRevision(ds.topic.id, dates, scheduleType)
                    dialogState = SyllabusDialogState.Closed
                },
                onDismiss = { dialogState = SyllabusDialogState.Closed },
                isAlreadyRevisionNeeded = ds.topic.status == TopicStatus.REVISION_NEEDED,
                onCancelRevision = {
                    actions.updateTopic(ds.topic.id, status = TopicStatus.DONE)
                    dialogState = SyllabusDialogState.Closed
                }
            )
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

    if (showReorderBuildInfo) {
        AlertDialog(
            onDismissRequest = { showReorderBuildInfo = false },
            title = { Text("Build re-ordered syllabus") },
            text = {
                Text(
                    "This rebuilds your study plan in the subject, chapter, and topic order you chose."
                )
            },
            confirmButton = {
                TextButton(onClick = { showReorderBuildInfo = false }) {
                    Text("Got it")
                }
            },
        )
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
        color = scheme.surfaceContainerHighest,
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
                        text = listOfNotNull(examType?.takeIf { it.isNotBlank() }, if (isTemplatePlan) "Syllabus" else "Custom syllabus").joinToString(" • "),
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

/** Rectangular "Add X" action pinned above a list (subjects, chapters, or topics)
 *  instead of a floating action button — a FAB visually sits on top of the cards
 *  underneath it, which reads as covering/obscuring content rather than being a
 *  clearly separate action. A distinct teal fill (vs. the blue "Build re-ordered
 *  syllabus" button) makes it unmistakably the "add" action at a glance. */
@Composable
internal fun SyllabusAddButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PlannerAccent.Teal,
            contentColor = Color.White,
        ),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

/** Replaces the old "Add Subject" + "Build Planner" pair — the FAB already covers
 *  adding a subject, so this single button just builds the plan in exactly the
 *  order the syllabus is arranged on screen (the "in order" strategy). */
@Composable
private fun SyllabusBuildButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val buildGradient = if (enabled) {
        Brush.horizontalGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
    } else {
        Brush.horizontalGradient(colors = listOf(scheme.onSurface.copy(alpha = 0.12f), scheme.onSurface.copy(alpha = 0.12f)))
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(buildGradient, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (enabled) Color.White else scheme.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        Text("Build re-ordered syllabus", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
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
                        text = if (isTemplatePlan) "Copy-paste in the syllabus screen" else "Import or update syllabus",
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
private fun SyllabusSectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyllabusEmptySubjectsCard(onAddSubject: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No subjects yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your first subject to start building your custom study planner syllabus.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            val addSubGradient = Brush.horizontalGradient(colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
            Button(
                onClick = onAddSubject,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .background(addSubGradient, shape = RoundedCornerShape(50))
                    .heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add Subject", fontWeight = FontWeight.Bold)
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
