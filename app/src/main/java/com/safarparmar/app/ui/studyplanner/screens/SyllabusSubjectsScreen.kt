package com.safarparmar.app.ui.studyplanner.screens

import com.safarparmar.app.ui.adaptive.usePlannerTopNavigation
import androidx.compose.ui.platform.LocalResources

import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

import com.safarparmar.app.ui.studyplanner.components.PlannerDialogTextAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogText
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import com.safarparmar.app.ui.components.popups.SafarModalBottomSheet as ModalBottomSheet
import com.safarparmar.app.ui.components.popups.SafarDatePickerDialog as DatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.TextInputDialog
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.SubjectUiModel
import com.safarparmar.app.ui.studyplanner.logic.deleteImpact
import com.safarparmar.app.ui.studyplanner.logic.findDuplicateSiblingName
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import com.safarparmar.app.ui.studyplanner.logic.NodeState
import com.safarparmar.app.ui.studyplanner.logic.PlannerInsightsCalculator
import com.safarparmar.app.ui.studyplanner.logic.syllabusReadiness
import com.safarparmar.app.ui.studyplanner.logic.searchSyllabus
import com.safarparmar.app.ui.studyplanner.logic.isLate
import com.safarparmar.app.ui.studyplanner.logic.SyllabusReadinessStatus
import com.safarparmar.app.ui.studyplanner.plan.PlanExamSettingsRow
import com.safarparmar.app.ui.studyplanner.logic.progressState
import com.safarparmar.app.ui.studyplanner.logic.rollup
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.ui.studyplanner.plan.PlanSettingsSheet
import com.safarparmar.app.ui.studyplanner.plan.RescheduleFlowSheet

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
    isDarkTheme: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlannerSectionSelect: (PlannerSection) -> Unit,
    showBottomBar: Boolean = true,
) {
    com.safarparmar.app.ui.studyplanner.components.PlannerWarmClarityTheme(isDarkTheme) {
        SyllabusSubjectsScreenContent(viewModel, planId, isDarkTheme, onNavigate, onBack, onPlannerSectionSelect, showBottomBar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyllabusSubjectsScreenContent(
    viewModel: StudyPlannerViewModel,
    planId: String,
    isDarkTheme: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlannerSectionSelect: (PlannerSection) -> Unit,
    showBottomBar: Boolean = true,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val subjects by viewModel.subjects.collectAsStateWithLifecycle()
        val actions: PlannerActions = viewModel
        val context = androidx.compose.ui.platform.LocalContext.current
        val localizedResources by rememberUpdatedState(LocalResources.current)

        var dialogState by remember { mutableStateOf<SyllabusDialogState>(SyllabusDialogState.Closed) }
        var activeSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
        // Hoisted here (rather than owned by the chapter row itself) so the sheet always
        // reads its topic list fresh from the same reactive `localSubjects`/`state` this
        // whole screen renders from — the topic row previously owned its own "is the sheet
        // open" flag and closed over a `chapter` parameter that could lag one recomposition
        // behind a just-added topic, which is what made a freshly added topic only show up
        // after leaving and reopening the sheet.
        var openTopicsChapterId by rememberSaveable { mutableStateOf<String?>(null) }
        // Topic to scroll to and outline when the sheet opens from search or Today.
        var focusTopicId by rememberSaveable { mutableStateOf<String?>(null) }
        var topicForDatePicker by remember { mutableStateOf<StudyTopic?>(null) }
        var showNewDatesConfirm by rememberSaveable { mutableStateOf(false) }
        var showPlanSettings by rememberSaveable { mutableStateOf(false) }
        var showReschedule by rememberSaveable { mutableStateOf(false) }
        // Which field Plan settings scrolls to when a readiness action opens it.
        var settingsFocusExamDate by rememberSaveable { mutableStateOf(false) }
        var settingsFocusDailyGoal by rememberSaveable { mutableStateOf(false) }

        BackHandler {
            when {
                activeSubjectId != null -> activeSubjectId = null
                else -> onBack()
            }
        }

        LaunchedEffect(planId) {
            if (state.selectedPlan?.id != planId) {
                viewModel.openPlan(planId)
            }
        }

        val selectedPlan = state.selectedPlan

        val rawSubjects = selectedPlan?.subjects.orEmpty()
        var localSubjects by remember(rawSubjects) { mutableStateOf(rawSubjects) }

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
                            message = localizedResources.getString(R.string.planner_duplicate_subject, name),
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
                    message = localizedResources.getString(R.string.planner_duplicate_subject, name),
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
                            message = localizedResources.getString(R.string.planner_duplicate_chapter, name),
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
                    message = localizedResources.getString(R.string.planner_duplicate_chapter, name),
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
                            message = localizedResources.getString(R.string.planner_duplicate_topic, name),
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
        val planProgress = selectedPlan?.rollup()?.completionPercent ?: 0
        val examDays = daysUntil(selectedPlan?.examDate)?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()
        val doneTopics = localSubjects.sumOf { subject -> subject.chapters.sumOf { chapter -> chapter.topics.count { it.status.isStudied } } }
        // Real-pace forecast, the same number the Progress tab's "Am I on track?" uses.
        val velocityForecast = remember(selectedPlan, state.calendar, state.analytics) {
            selectedPlan?.let { PlannerInsightsCalculator.compute(it, state.calendar, state.analytics).summary.velocityForecastCompletionDate }
        }
        val readiness = remember(selectedPlan, state.pendingRebuild, state.pendingRatingChapterIds, state.ratingRebuildPromptDismissed, velocityForecast) {
            selectedPlan?.syllabusReadiness(
                rebuildArmed = state.pendingRebuild != null,
                ratingsChanged = state.pendingRatingChapterIds.isNotEmpty() && !state.ratingRebuildPromptDismissed,
                velocityForecastDate = velocityForecast,
            )
        }
        fun openPlanSettings(focusExamDate: Boolean = false, focusDailyGoal: Boolean = false) {
            settingsFocusExamDate = focusExamDate
            settingsFocusDailyGoal = focusDailyGoal
            showPlanSettings = true
        }
        val isTemplatePlan = !selectedPlan?.templateId.isNullOrBlank()
        val shouldShowFullImport = localSubjects.isEmpty() && !isTemplatePlan

        var syllabusQuery by rememberSaveable { mutableStateOf("") }
        // Study mode (default): progress actions only. Edit mode: add, rename,
        // delete, reorder and chapter sizes. Shared by the list, subject screen
        // and topics sheet.
        var editMode by rememberSaveable { mutableStateOf(false) }
        var orderChangedWhileEditing by rememberSaveable { mutableStateOf(false) }
        var showOrderChangedPrompt by rememberSaveable { mutableStateOf(false) }
        val isSearching = syllabusQuery.isNotBlank()
        val searchResults = remember(localSubjects, syllabusQuery) { localSubjects.searchSyllabus(syllabusQuery) }
        val filteredSubjects = if (isSearching) searchResults.subjects else localSubjects
        fun openChapter(subjectId: String, chapterId: String, topicId: String? = null) {
            activeSubjectId = subjectId
            openTopicsChapterId = chapterId
            focusTopicId = topicId
        }
        fun toggleEditMode() {
            if (editMode) {
                editMode = false
                // A new order only reaches the calendar when dates are rebuilt.
                val hasDates = selectedPlan?.subjects.orEmpty().any { s -> s.chapters.any { c -> c.topics.any { !it.plannedDate.isNullOrBlank() } } }
                if (orderChangedWhileEditing && (hasDates || state.pendingRebuild != null)) showOrderChangedPrompt = true
                orderChangedWhileEditing = false
            } else {
                syllabusQuery = ""
                editMode = true
            }
        }

        // "Subject · Chapter" links from other tabs (Today) land here.
        LaunchedEffect(state.pendingSyllabusChapter) {
            val request = state.pendingSyllabusChapter ?: return@LaunchedEffect
            syllabusQuery = ""
            openChapter(request.subjectId, request.chapterId, request.topicId)
            actions.clearPendingSyllabusChapter()
        }

        val buildSchedule: () -> Unit = {
            val pending = state.pendingRebuild
            if (pending != null) {
                actions.rescheduleAfterExamDateChange(
                    strategy = pending.strategy,
                    prioritySubjectNames = pending.prioritySubjectNames,
                    priorityOrderMode = pending.priorityOrderMode,
                )
            } else {
                // Same study style as every other "new dates" entry point, rather
                // than a hard-coded order that differs from Plan settings.
                actions.autoDistribute(
                    lockExisting = false,
                    strategy = if (state.preferredStudyStrategy == "sequential") "sequential" else "interleaved",
                    preserveToday = true,
                )
            }
        }

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
            orderChangedWhileEditing = true
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
            orderChangedWhileEditing = true
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
            orderChangedWhileEditing = true
            actions.reorderSyllabus(topicIdsByChapterId = mapOf(chapterId to chapter.topics.map { it.id }))
        }

        val adaptiveState = com.safarparmar.app.ui.adaptive.rememberSafarAdaptiveState()
        // Two 320dp panes; portrait tablets keep their existing split when it fits.
        val isWide = maxWidth >= 640.dp && maxHeight >= 400.dp
        val useTopNavigation = showBottomBar && adaptiveState.usePlannerTopNavigation

        // Default to the first subject on wide screens if none selected
        LaunchedEffect(isWide, localSubjects) {
            if (isWide && activeSubjectId == null && localSubjects.isNotEmpty()) {
                activeSubjectId = localSubjects.first().id
            }
        }

        val syllabusWindowInsets = if (showBottomBar && !useTopNavigation) {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        } else {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        }

        @Composable
        fun ChaptersPane(subject: StudySubject, showBackButton: Boolean) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground(),
                contentWindowInsets = syllabusWindowInsets,
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    androidx.compose.material3.TopAppBar(
                        // In the planner tab the drawer's top bar already shows Titli.
                        actions = {
                            TextButton(onClick = { toggleEditMode() }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(if (editMode) R.string.common_done else R.string.common_edit), fontWeight = FontWeight.SemiBold)
                            }
                            if (!isWide && showBottomBar) com.safarparmar.app.ui.tutorial.TitliGuideButton()
                        },
                        title = { Text(subject.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        navigationIcon = {
                            if (showBackButton) {
                                Box(modifier = Modifier.padding(start = 12.dp)) {
                                    com.safarparmar.app.ui.ekagra.focusshield.StudyPlannerCircularBackButton(onClick = { activeSubjectId = null })
                                }
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
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        val subjectTopics = subject.chapters.sumOf { it.topics.size }
                        SyllabusMagazineChapterHeader(
                            completionPercent = subject.progressState().percent,
                            chapterCount = subject.chapters.size,
                            topicCount = subjectTopics,
                            showAdd = editMode,
                            onAddChapter = {
                                subjects.firstOrNull { it.id == subject.id }?.let {
                                    dialogState = SyllabusDialogState.AddChapter(it)
                                }
                            },
                        )
                    }
                    if (subject.chapters.isEmpty()) {
                        item {
                            SyllabusMagazineEmptyNote(
                                text = stringResource(R.string.planner_no_chapters_subject),
                                actionLabel = stringResource(R.string.planner_add_chapter_plus),
                                onAction = {
                                    subjects.firstOrNull { it.id == subject.id }?.let {
                                        dialogState = SyllabusDialogState.AddChapter(it)
                                    }
                                },
                            )
                        }
                    } else {
                        items(subject.chapters, key = { it.id }) { chapter ->
                            PlanHairline()
                            SyllabusMagazineChapterRow(
                                chapter = chapter,
                                onOpenTopics = { openTopicsChapterId = chapter.id },
                                onRename = { dialogState = SyllabusDialogState.RenameChapter(subject.id, chapter) },
                                onDelete = { dialogState = SyllabusDialogState.DeleteChapter(subject.id, chapter) },
                                onMarkDone = { actions.batchMarkTopicsDone(chapter.topics.map { it.id }) },
                                onRate = { difficulty -> actions.rateChapter(subject.id, chapter.id, difficulty) },
                                showDifficultyChips = state.selectedPlan?.weightedPlanning != false,
                                editMode = editMode,
                                onMoveUp = { moveChapter(subject.id, chapter.id, -1) },
                                onMoveDown = { moveChapter(subject.id, chapter.id, 1) },
                                onDragEnd = { saveChapterOrder(subject.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            val openChapter = openTopicsChapterId?.let { id -> subject.chapters.find { it.id == id } }
            if (openChapter != null) {
                ChapterTopicsSheet(
                    chapterName = openChapter.name,
                    topics = openChapter.topics,
                    chapter = openChapter,
                    isDarkTheme = isDarkTheme,
                    onDismiss = {
                        openTopicsChapterId = null
                        focusTopicId = null
                    },
                    focusTopicId = focusTopicId,
                    onAddTopic = { name -> requestAddTopic(subject.id, openChapter.id, name) },
                    onTopicClick = {},
                    onRenameTopic = { topic -> dialogState = SyllabusDialogState.RenameTopic(topic) },
                    onDeleteTopic = { topic -> dialogState = SyllabusDialogState.DeleteTopic(topic) },
                    onAssignToday = { topic -> actions.updateTopic(topic.id, plannedDate = todayKey(), pinned = true) },
                    onAssignTomorrow = { topic ->
                        val tomorrow = java.time.LocalDate.parse(todayKey()).plusDays(1).toString()
                        actions.updateTopic(topic.id, plannedDate = tomorrow, pinned = true)
                    },
                    // Same Ekagra launch as Today's Focus button.
                    onFocusTopic = { topic -> onNavigate(Routes.ekagraForTopic(topic.id, topic.name, planId)) },
                    editMode = editMode,
                    onToggleEdit = { toggleEditMode() },
                    onMoveTopicUp = { topic -> moveTopic(openChapter.id, topic.id, -1) },
                    onMoveTopicDown = { topic -> moveTopic(openChapter.id, topic.id, 1) },
                    onTopicDragEnd = { saveTopicOrder(openChapter.id) },
                    onChangeDate = { topic -> topicForDatePicker = topic },
                    onMarkDoneTopic = { topic -> actions.batchMarkTopicsDone(listOf(topic.id)) },
                    onMarkNotDoneTopic = { topic -> actions.updateTopic(topic.id, status = TopicStatus.TODO) },
                    onToReviseTopic = { topic -> dialogState = SyllabusDialogState.ReviseTopic(topic) },
                )
            }
        }

        @Composable
        fun SubjectsPane(selectedSubjectId: String?, onSelect: (String) -> Unit) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground(),
                contentWindowInsets = syllabusWindowInsets,
                bottomBar = {
                    Column {
                        if (showBottomBar && !useTopNavigation) {
                            PlannerBottomBar(
                                selected = PlannerSection.SYLLABUS,
                                onSelect = onPlannerSectionSelect,
                            )
                        }
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
                                    top = if (showBottomBar) 16.dp else 8.dp,
                                    bottom = 32.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                item {
                                    if (showBottomBar) {
                                        // Standalone route: no planner chrome above, so name the screen.
                                        Text(stringResource(R.string.planner_warm_syllabus), style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                                    }
                                    PlanExamSettingsRow(
                                        planTitle = selectedPlan?.title.orEmpty().ifBlank { "Study Plan" },
                                        onSettingsClick = { openPlanSettings() },
                                        onSelectExam = if (showBottomBar) null else ({ onPlannerSectionSelect(PlannerSection.YOUR_EXAMS) }),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    // While searching, results matter more than status; the card
                                    // would push them under the keyboard.
                                    if (!isSearching) readiness?.let { status ->
                                        SyllabusReadinessCard(
                                            completionPercent = planProgress,
                                            doneTopics = doneTopics,
                                            totalTopics = totalTopics,
                                            daysUntilExam = examDays,
                                            status = status,
                                            busy = state.mutating,
                                            actions = SyllabusReadinessActions(
                                                onCreateDates = { showNewDatesConfirm = true },
                                                onSetExamDate = { openPlanSettings(focusExamDate = true) },
                                                // Same scheduling screen as Today's "Not Assigned Topics" banner.
                                                onGiveDates = { actions.openUnscheduledTopics() },
                                                onUpdateDates = actions::rebuildAfterRatingChanges,
                                                onDismissRatings = actions::dismissRatingRebuildPrompt,
                                                onCatchUp = {
                                                    if (status is SyllabusReadinessStatus.Late) {
                                                        syllabusQuery = ""
                                                        openChapter(status.subjectId, status.chapterId)
                                                    }
                                                },
                                                onStudyMore = { openPlanSettings(focusDailyGoal = true) },
                                            ),
                                        )
                                    }
                                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (editMode) {
                                            Text(stringResource(R.string.planner_editing_hint), style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        } else {
                                            androidx.compose.material3.OutlinedTextField(value = syllabusQuery, onValueChange = { syllabusQuery = it },
                                                modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(18.dp),
                                                label = { Text(stringResource(R.string.planner_warm_search_topics)) })
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        androidx.compose.material3.FilledTonalButton(onClick = { toggleEditMode() },
                                            modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)) {
                                            Text(stringResource(if (editMode) R.string.common_done else R.string.common_edit))
                                        }
                                    }
                                    if (!isSearching) SyllabusMagazineListHeader(
                                        title = stringResource(R.string.planner_your_subjects),
                                        onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                        addContentDescription = "Add subject",
                                        showAdd = editMode,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                                    )
                                }

                                if (isSearching && searchResults.isEmpty && localSubjects.isNotEmpty()) {
                                    item { Text(stringResource(R.string.planner_warm_no_matches), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
                                }
                                if (localSubjects.isEmpty() && !shouldShowFullImport) {
                                    item {
                                        SyllabusMagazineEmptyNote(
                                            text = stringResource(R.string.planner_no_subjects_syllabus),
                                            actionLabel = stringResource(R.string.planner_add_subject_plus),
                                            onAction = { dialogState = SyllabusDialogState.AddSubject },
                                        )
                                    }
                                } else {
                                    items(filteredSubjects, key = { it.id }) { subject ->
                                        val isSelected = isWide && subject.id == selectedSubjectId
                                        PlanHairline()
                                        SyllabusMagazineSubjectRow(
                                            subject = subject,
                                            onClick = { onSelect(subject.id) },
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
                                            editMode = editMode,
                                            onMoveUp = { moveSubject(subject.id, -1) },
                                            onMoveDown = { moveSubject(subject.id, 1) },
                                            onDragEnd = { saveSubjectOrder() },
                                            modifier = Modifier.animateItem().then(
                                                if (isSelected) {
                                                    Modifier.background(
                                                        com.safarparmar.app.ui.theme.SafarSemanticColors.brandPurple().copy(alpha = 0.12f),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                                    )
                                                } else Modifier
                                            ),
                                        )
                                    }
                                }
                                if (isSearching) {
                                    items(searchResults.chapters, key = { "chapter_hit_${it.chapter.id}" }) { hit ->
                                        PlanHairline()
                                        SyllabusSearchResultRow(
                                            title = hit.chapter.name,
                                            path = hit.subject.name,
                                            done = false,
                                            trailing = null,
                                            trailingIsLate = false,
                                            onClick = { openChapter(hit.subject.id, hit.chapter.id) },
                                        )
                                    }
                                    if (searchResults.topics.isNotEmpty()) {
                                        item(key = "topic_hits_heading") {
                                            Text(stringResource(R.string.planner_search_topics_heading), style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                                        }
                                    }
                                    items(searchResults.topics, key = { "topic_hit_${it.topic?.id}" }) { hit ->
                                        val topic = hit.topic ?: return@items
                                        PlanHairline()
                                        SyllabusSearchResultRow(
                                            title = topic.name,
                                            path = "${hit.subject.name} · ${hit.chapter.name}",
                                            done = topic.status == TopicStatus.DONE,
                                            trailing = syllabusTopicDateLabel(topic),
                                            trailingIsLate = topic.isLate(),
                                            onClick = { openChapter(hit.subject.id, hit.chapter.id, topic.id) },
                                        )
                                    }
                                    if (searchResults.topicsTruncated) {
                                        item(key = "topic_hits_more") {
                                            Text(stringResource(R.string.planner_search_more, searchResults.topics.size), style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme provides isDarkTheme,
        ) {
            Column(Modifier.fillMaxSize()) {
                if (useTopNavigation) {
                    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp)) {
                        PlannerTopNavigation(PlannerSection.SYLLABUS, onPlannerSectionSelect)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().then(
                    if (useTopNavigation) Modifier.navigationBarsPadding() else Modifier,
                )) {
                    if (isWide) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Subjects List
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                SubjectsPane(
                                    selectedSubjectId = activeSubjectId,
                                    onSelect = { activeSubjectId = it }
                                )
                            }

                            // Subtle Pane Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(PlannerFlatColors.BorderSoft)
                            )

                            // Right Pane: Selected Subject Chapters & Topics
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                val currentSubject = activeSubjectId?.let { id -> localSubjects.find { it.id == id } }
                                if (currentSubject != null) {
                                    ChaptersPane(subject = currentSubject, showBackButton = false)
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground()),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        com.composables.ui.components.Text(
                                            stringResource(R.string.planner_no_subjects_syllabus),
                                            fontSize = 15.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                                ChaptersPane(subject = subject, showBackButton = true)
                            } else {
                                SubjectsPane(
                                    selectedSubjectId = null,
                                    onSelect = { activeSubjectId = it }
                                )
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
                        Text(stringResource(R.string.common_ok), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { topicForDatePicker = null }) {
                        Text(stringResource(R.string.common_cancel))
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
                val body = stringResource(R.string.planner_delete_chapter_impact, impact.topicCount, impact.scheduledTopicCount)
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
                    stringResource(R.string.planner_delete_topic_named, ds.topic.name),
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
                PlannerDialog(
                    onDismissRequest = { dialogState = SyllabusDialogState.Closed },
                    title = stringResource(R.string.planner_name_already_used),
                    text = { PlannerDialogText(ds.message) },
                    dismissButton = {
                        PlannerDialogTextAction(stringResource(R.string.common_cancel)) { dialogState = SyllabusDialogState.Closed }
                    },
                    confirmButton = {
                        PlannerDialogAction(text = stringResource(R.string.planner_add_anyway)) {
                            ds.onConfirm(); dialogState = SyllabusDialogState.Closed
                        }
                    },
                )
            }
            SyllabusDialogState.Closed -> {}
        }

        if (showOrderChangedPrompt) {
            PlannerDialog(
                onDismissRequest = { showOrderChangedPrompt = false },
                title = stringResource(R.string.planner_order_changed_title),
                text = { PlannerDialogText(stringResource(R.string.planner_order_changed_body)) },
                dismissButton = {
                    PlannerDialogTextAction(stringResource(R.string.common_not_now)) { showOrderChangedPrompt = false }
                },
                confirmButton = {
                    PlannerDialogAction(text = stringResource(R.string.planner_ready_update_dates)) {
                        showOrderChangedPrompt = false
                        buildSchedule()
                    }
                },
            )
        }

        if (showNewDatesConfirm) {
            PlannerDialog(
                onDismissRequest = { showNewDatesConfirm = false },
                title = stringResource(R.string.planner_make_new_dates_question),
                text = {
                    PlannerDialogText(
                        "This will give new dates to your unfinished topics. Today's work will stay the same."
                    )
                },
                dismissButton = {
                    PlannerDialogTextAction(stringResource(R.string.common_not_now)) { showNewDatesConfirm = false }
                },
                confirmButton = {
                    PlannerDialogAction(text = stringResource(R.string.planner_make_new_dates)) {
                        showNewDatesConfirm = false
                        buildSchedule()
                    }
                },
            )
        }

        if (showPlanSettings && selectedPlan != null) {
            PlanSettingsSheet(
                plan = selectedPlan,
                actions = actions,
                onExport = null,
                onReset = {},
                onDismiss = {
                    showPlanSettings = false
                    settingsFocusExamDate = false
                    settingsFocusDailyGoal = false
                },
                onExamDateChanged = { showReschedule = true },
                focusDailyGoal = settingsFocusDailyGoal,
                focusExamDate = settingsFocusExamDate,
                preferredStrategy = state.preferredStudyStrategy,
            )
        }

        if (showReschedule) {
            RescheduleFlowSheet(
                subjects = rawSubjects,
                onRebuildNow = { strategy, priority, orderMode ->
                    actions.rescheduleAfterExamDateChange(strategy, prioritySubjectNames = priority, priorityOrderMode = orderMode)
                    showReschedule = false
                },
                onReorderFirst = { strategy, priority, orderMode ->
                    // Already on Syllabus: arm the rebuild and switch reordering on.
                    actions.armRebuild(strategy, prioritySubjectNames = priority, priorityOrderMode = orderMode)
                    syllabusQuery = ""
                    editMode = true
                    showReschedule = false
                },
                onDismiss = { showReschedule = false },
            )
        }
    }
}
