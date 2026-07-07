package com.safarparmar.app.ui.studyplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.safarparmar.app.data.remote.api.AutoDistributeRequest
import com.safarparmar.app.data.remote.api.BatchTopicUpdateItem
import com.safarparmar.app.data.remote.api.BatchTopicUpdateRequest
import com.safarparmar.app.data.remote.api.BulkTopicItemRequest
import com.safarparmar.app.data.remote.api.BulkTopicsRequest
import com.safarparmar.app.data.remote.api.ChapterRequest
import com.safarparmar.app.data.remote.api.CreateFromTemplateRequest
import com.safarparmar.app.data.remote.api.CreatePlanRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusSubjectRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusChapterRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusTopicRequest
import com.safarparmar.app.data.remote.api.ReorderSyllabusRequest
import com.safarparmar.app.data.remote.api.RolloverUndoRequest
import com.safarparmar.app.data.remote.api.DeleteUndoRequest
import com.safarparmar.app.data.remote.api.StructureSyllabusRequest
import com.safarparmar.app.data.remote.api.SubjectRequest
import com.safarparmar.app.data.remote.api.TopicPatchRequest
import com.safarparmar.app.data.remote.api.TopicRequest
import com.safarparmar.app.ui.studyplanner.analytics.StudyPlannerAnalytics
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.Achievement
import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.content.Context
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.ui.studyplanner.logic.countBulkSubjectsChapters
import com.safarparmar.app.ui.studyplanner.logic.countBulkSubjectsTopics
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics
import com.safarparmar.app.ui.studyplanner.logic.parseBulkSubjectsFromTxt
import com.safarparmar.app.ui.studyplanner.logic.parseBulkSyllabus
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.ui.studyplanner.templates.getLocalExamTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SubjectUiModel(
    val id: String,
    val name: String,
    val color: String,
    val chapterCount: Int,
    val topicCount: Int,
    val completionPercentage: Int
)

data class ChapterUiModel(
    val id: String,
    val name: String,
    val topicCount: Int,
    val completionPercentage: Int,
    val status: TopicStatus
)

data class TopicUiModel(
    val id: String,
    val name: String,
    val status: TopicStatus,
    val plannedDate: String? = null
)

object StudyPlannerOnboardingSteps {
    const val BUILD_SCHEDULE = "build_schedule"
    const val REVIEW_CALENDAR = "review_calendar"
    const val FIRST_TOPIC_DONE = "first_topic_done"
}

data class StudyPlannerUiState(
    val plans: List<StudyPlan> = emptyList(),
    val templates: List<ExamTemplateSummary> = emptyList(),
    val selectedPlan: StudyPlan? = null,
    val calendar: CalendarMap = emptyMap(),
    val analytics: PlannerAnalytics? = null,
    val section: PlannerSection = PlannerSection.PLAN,
    val loading: Boolean = false,
    val mutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val rolloverUndoToken: String? = null,
    val deleteUndoToken: String? = null,
    /** What the current deleteUndoToken would restore, e.g. "Topic swap" — used to
     *  phrase the confirmation after Undo is tapped ("Topic swap undone") instead of
     *  a generic message that doesn't match what was actually undone. */
    val lastUndoableActionLabel: String? = null,
    /** Per-plan scheduling mode: "flex" (allow over-goal days) or "strict". */
    val planningMode: String = "flex",
    /** "How do you like to study" default: "interleaved" (mix subjects) or "sequential"
     *  (deep focus). Chosen during plan creation, reused as the Build Planner default. */
    val preferredStudyStrategy: String = "interleaved",
    val onboardingSkipped: Boolean = false,
    val selectedSubjectId: String? = null,
    val selectedChapterId: String? = null,
    val rawSyllabusText: String = "",
    val structuredPreview: com.safarparmar.app.data.remote.api.StructuredSyllabusPreview? = null,
    val isStructuringSyllabus: Boolean = false,
    val isImportingStructuredSyllabus: Boolean = false,
    val structureError: String? = null,
    val structuredImportError: String? = null,
    val structuredImportSuccessMessage: String? = null,
    val pendingOpenAiImport: Boolean = false,
    /** Set when a plan was just created with "Manual mode" (user wants to pick their own
     *  subject order). The Plan tab shows a one-time reorder sheet while this is true,
     *  then clears it via [PlannerActions.clearPendingManualSubjectOrder]. */
    val pendingManualSubjectOrder: Boolean = false,
    val isImporting: Boolean = false,
    val importStatus: String? = null,
    val importError: String? = null,
    val importResultSummary: String? = null,
    val hydrateWarning: String? = null,
    val onboardingCompletedSteps: Set<String> = emptySet(),
    val plannerAchievements: List<Achievement> = emptyList(),
    val activePlanTab: StudyPlannerTab = StudyPlannerTab.TODAY,
)

@HiltViewModel
class StudyPlannerViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
    private val homeRepository: HomeRepository,
    val dataStore: com.safarparmar.app.data.local.SafarDataStore,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), PlannerActions {
    private val _uiState = MutableStateFlow(StudyPlannerUiState())
    val uiState = _uiState.asStateFlow()
    private val firedDailyMilestones = mutableSetOf<String>()


    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId = _selectedSubjectId.asStateFlow()

    private val _selectedChapterId = MutableStateFlow<String?>(null)
    val selectedChapterId = _selectedChapterId.asStateFlow()

    val subjects: kotlinx.coroutines.flow.StateFlow<List<SubjectUiModel>> = _uiState.map { state ->
        state.selectedPlan?.subjects?.map { s ->
            val totalTopics = s.chapters.sumOf { it.topics.size }
            val doneTopics = s.chapters.sumOf { ch -> ch.topics.count { it.status == TopicStatus.DONE } }
            val completion = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0
            SubjectUiModel(s.id, s.name, s.color, s.chapters.size, totalTopics, completion)
        } ?: emptyList()
    }.distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    val chapters: kotlinx.coroutines.flow.StateFlow<List<ChapterUiModel>> = combine(_uiState, _selectedSubjectId) { state, subjectId ->
        val subject = state.selectedPlan?.subjects?.find { it.id == subjectId }
        subject?.chapters?.map { ch ->
            val totalTopics = ch.topics.size
            val doneTopics = ch.topics.count { it.status == TopicStatus.DONE }
            val completion = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0

            val status = when {
                totalTopics == 0 -> TopicStatus.TODO
                doneTopics == totalTopics -> TopicStatus.DONE
                else -> TopicStatus.TODO
            }

            ChapterUiModel(ch.id, ch.name, totalTopics, completion, status)
        } ?: emptyList()
    }.distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    val topics: kotlinx.coroutines.flow.StateFlow<List<TopicUiModel>> = combine(_uiState, _selectedSubjectId, _selectedChapterId) { state, subjectId, chapterId ->
        val subject = state.selectedPlan?.subjects?.find { it.id == subjectId }
        val chapter = subject?.chapters?.find { it.id == chapterId }
        chapter?.topics?.map { t ->
            TopicUiModel(t.id, t.name, t.status, t.plannedDate)
        } ?: emptyList()
    }.distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    init {
        refreshPlans()
        loadTemplates()
        val planId = savedStateHandle.get<String>("planId")
        if (!planId.isNullOrBlank()) {
            openPlan(planId)
        } else {
            viewModelScope.launch {
                val strategy = dataStore.plannerPreferredStrategy(null).first()
                _uiState.update { it.copy(preferredStudyStrategy = strategy) }
            }
        }
    }

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun selectChapter(chapterId: String) {
        _selectedChapterId.value = chapterId
        _uiState.update { it.copy(selectedChapterId = chapterId) }
    }

    override fun setSection(section: PlannerSection) {
        _uiState.update { it.copy(section = section) }
        if (section == PlannerSection.CALENDAR) {
            val plan = _uiState.value.selectedPlan
            val hasSchedule = plan?.flattenTopics()?.any { !it.topic.plannedDate.isNullOrBlank() } == true
            if (hasSchedule) markOnboardingStepDone(StudyPlannerOnboardingSteps.REVIEW_CALENDAR)
        }
    }

    override fun setPlanTab(tab: StudyPlannerTab) {
        _uiState.update { it.copy(activePlanTab = tab) }
    }

    /**
     * Handles a back-press inside the Study Planner feature.
     *
     * Back-press hierarchy:
     *   [Any sub-section] → previous section (or PLAN if stack empty)
     *   → [Plan is open, section == PLAN or YOUR_EXAMS] → close plan (go to exam list)
     *   → [No plan open / exam list] → return false so NavController goes to Home.
     */
    override fun navigateBack(): Boolean {
        val hasPlan = _uiState.value.selectedPlan != null
        if (!hasPlan) {
            // Nothing internal to consume — let the NavController handle it.
            return false
        }

        // If the user is not on the PLAN tab, navigating back takes them to PLAN.
        if (_uiState.value.section != PlannerSection.PLAN) {
            _uiState.update { it.copy(section = PlannerSection.PLAN) }
            return true
        }

        // If already on PLAN tab, close the plan to return to YOUR_EXAMS.
        closePlan()
        return true
    }

    override fun clearTransient() {
        _uiState.update {
            it.copy(
                error = null,
                message = null,
                hydrateWarning = null,
                rolloverUndoToken = null,
                deleteUndoToken = null,
                lastUndoableActionLabel = null,
            )
        }
    }

    override fun setPlanningMode(mode: String) {
        val normalized = if (mode == "strict") "strict" else "flex"
        val planId = _uiState.value.selectedPlan?.id ?: return
        _uiState.update { it.copy(planningMode = normalized) }
        viewModelScope.launch { dataStore.setPlannerPlanningMode(planId, normalized) }
    }

    override fun setPreferredStudyStrategy(strategy: String) {
        val normalized = if (strategy == "sequential") "sequential" else "interleaved"
        _uiState.update { it.copy(preferredStudyStrategy = normalized) }
        viewModelScope.launch { dataStore.setPlannerPreferredStrategy(_uiState.value.selectedPlan?.id, normalized) }
    }

    override fun undoDelete() {
        val state = _uiState.value
        val planId = state.selectedPlan?.id ?: return
        val undoToken = state.deleteUndoToken ?: return
        val undoneLabel = state.lastUndoableActionLabel ?: "Change"
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, deleteUndoToken = null, lastUndoableActionLabel = null) }
            when (val r = repo.undoDelete(planId, DeleteUndoRequest(undoToken))) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data.plan ?: it.selectedPlan,
                            mutating = false,
                            message = "$undoneLabel undone",
                        )
                    }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun undoRollover() {
        val state = _uiState.value
        val planId = state.selectedPlan?.id ?: return
        val undoToken = state.rolloverUndoToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, rolloverUndoToken = null) }
            when (val r = repo.undoRollover(planId, RolloverUndoRequest(undoToken))) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data.plan ?: it.selectedPlan,
                            mutating = false,
                            message = "Missed topics restored",
                        )
                    }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    override fun refreshPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val r = repo.listPlans()) {
                is Resource.Success -> _uiState.update { it.copy(plans = r.data, loading = false) }
                is Resource.Error -> _uiState.update { it.copy(error = r.message, loading = false) }
                is Resource.Loading -> Unit
            }
        }
        refreshPlannerAchievements()
    }

    fun loadTemplates() = viewModelScope.launch {
        when (val r = repo.getTemplates()) {
            is Resource.Success -> _uiState.update { it.copy(templates = r.data) }
            is Resource.Error -> Unit
            is Resource.Loading -> Unit
        }
    }

    override fun openPlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, section = PlannerSection.PLAN) }
            when (val r = repo.getPlan(planId)) {
                is Resource.Success -> {
                    val digest = r.data.rolloverDigest
                    val rolloverMessage = digest
                        ?.takeIf { it.movedCount > 0 && it.undoToken.isNotBlank() }
                        ?.let {
                            "Moved ${it.movedCount} missed ${if (it.movedCount == 1) "topic" else "topics"} forward"
                        }
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data,
                            loading = false,
                            message = rolloverMessage,
                            rolloverUndoToken = digest?.undoToken?.takeIf { token -> rolloverMessage != null && token.isNotBlank() },
                        )
                    }
                    refreshOnboardingProgress(planId)
                    refreshPlanningMode(planId)
                    refreshPreferredStudyStrategy(planId)
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                    refreshPlannerAchievements()
                }
                is Resource.Error -> _uiState.update { it.copy(error = r.message, loading = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun refreshPlanningMode(planId: String) = viewModelScope.launch {
        val mode = dataStore.plannerPlanningMode(planId).first()
        _uiState.update { state ->
            if (state.selectedPlan?.id == planId) state.copy(planningMode = mode) else state
        }
    }

    private fun refreshPreferredStudyStrategy(planId: String) = viewModelScope.launch {
        // Per-plan value if this plan already recorded one, else the last global choice —
        // so opening a plan created before a preference was ever picked still shows it.
        val perPlan = dataStore.plannerPreferredStrategy(planId).first()
        val strategy = if (perPlan == "interleaved") dataStore.plannerPreferredStrategy(null).first() else perPlan
        _uiState.update { state ->
            if (state.selectedPlan?.id == planId) state.copy(preferredStudyStrategy = strategy) else state
        }
    }

    override fun closePlan() {
        _uiState.update {
            it.copy(
                selectedPlan = null,
                calendar = emptyMap(),
                analytics = null,
                section = PlannerSection.PLAN,
                onboardingCompletedSteps = emptySet(),
            )
        }
        refreshPlans()
    }

    override fun createPlan(
        title: String,
        examType: String?,
        examDate: String?,
        dailyGoal: Int,
        offDays: List<Int>,
        syllabusText: String?,
        openAiImport: Boolean,
    ) {
        val requiredExamDate = examDate?.take(10)?.takeIf { it.isNotBlank() }
        if (requiredExamDate == null) {
            _uiState.update { it.copy(error = "Exam date is required to create a planner.") }
            return
        }
        viewModelScope.launch {
            mutatePlanList(openAiImport = openAiImport) {
                repo.createPlan(CreatePlanRequest(title = title, examType = examType, examDate = requiredExamDate, dailyGoal = dailyGoal, offDays = offDays))
            }
        }
    }

    override fun createFromTemplate(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>, manualSubjectOrder: Boolean) {
        val requiredExamDate = examDate?.take(10)?.takeIf { it.isNotBlank() }
        if (requiredExamDate == null) {
            _uiState.update { it.copy(error = "Exam date is required to create a planner.") }
            return
        }
        viewModelScope.launch {
            tryCreatePlanFromTemplateWithLocalFallback(templateId, title, requiredExamDate, dailyGoal, offDays, manualSubjectOrder)
        }
    }

    override fun clearPendingManualSubjectOrder() {
        _uiState.update { it.copy(pendingManualSubjectOrder = false) }
    }

    override fun createFromTemplateOrLocal(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>) {
        val requiredExamDate = examDate?.take(10)?.takeIf { it.isNotBlank() }
        if (requiredExamDate == null) {
            _uiState.update { it.copy(error = "Exam date is required to create a planner.") }
            return
        }
        viewModelScope.launch {
            // Always try the fast server-side POST /plans/from-template first
            // (creates entire plan in one request instead of ~175 sequential calls).
            // The server independently validates template existence, so a client-side
            // pre-check against loadTemplates() is unnecessary and was causing the
            // slow local fallback when templates hadn't loaded yet.
            tryCreatePlanFromTemplateWithLocalFallback(templateId, title, requiredExamDate, dailyGoal, offDays)
        }
    }

    override fun deletePlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true) }
            when (val r = repo.deletePlan(planId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(mutating = false, selectedPlan = null, message = "Plan deleted") }
                    refreshPlans()
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun updatePlan(request: UpdatePlanRequest) = mutateSelected { planId -> repo.updatePlan(planId, request) }
    override fun addSubject(name: String) = mutateSelected { planId -> repo.addSubject(planId, SubjectRequest(name = name, color = "#0ea5e9")) }
    override fun renameSubject(subjectId: String, name: String) = mutateSelected { planId -> repo.renameSubject(planId, subjectId, SubjectRequest(name = name)) }
    override fun deleteSubject(subjectId: String) = mutateSelected(
        successMessage = "Subject deleted",
        undoLabel = "Subject deletion",
    ) { planId -> repo.deleteSubject(planId, subjectId) }
    override fun addChapter(subjectId: String, name: String) = mutateSelected { planId -> repo.addChapter(planId, subjectId, ChapterRequest(name)) }
    override fun renameChapter(subjectId: String, chapterId: String, name: String) = mutateSelected { planId -> repo.renameChapter(planId, subjectId, chapterId, ChapterRequest(name)) }
    override fun deleteChapter(subjectId: String, chapterId: String) = mutateSelected(
        successMessage = "Chapter deleted",
        undoLabel = "Chapter deletion",
    ) { planId -> repo.deleteChapter(planId, subjectId, chapterId) }
    override fun addTopic(subjectId: String, chapterId: String, name: String) =
        mutateSelected(onSuccess = { refreshPlannerAchievements() }) { planId ->
            repo.addTopic(planId, subjectId, chapterId, TopicRequest(name))
        }

    override fun addCustomTopicToToday(name: String) {
        val cleaned = name.trim()
        if (cleaned.length < 2) {
            _uiState.update { it.copy(error = "Topic name must be at least 2 characters") }
            return
        }
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true, error = null) }

            // 1. Ensure the "Extra Topics" subject exists.
            var plan = _uiState.value.selectedPlan
            var subject = plan?.subjects?.firstOrNull { it.name.equals(EXTRA_TOPICS_SUBJECT, ignoreCase = true) }
            if (subject == null) {
                when (val r = repo.addSubject(planId, SubjectRequest(name = EXTRA_TOPICS_SUBJECT, color = "#64748B"))) {
                    is Resource.Success -> { plan = r.data; subject = r.data.subjects.firstOrNull { it.name.equals(EXTRA_TOPICS_SUBJECT, ignoreCase = true) } }
                    is Resource.Error -> { _uiState.update { it.copy(mutating = false, error = r.message) }; return@launch }
                    is Resource.Loading -> Unit
                }
            }
            val subjectId = subject?.id
            if (subjectId.isNullOrBlank()) { _uiState.update { it.copy(mutating = false, error = "Could not create Extra Topics section") }; return@launch }

            // 2. Ensure the "Custom" chapter exists inside it.
            var chapter = subject?.chapters?.firstOrNull { it.name.equals(EXTRA_TOPICS_CHAPTER, ignoreCase = true) }
            if (chapter == null) {
                when (val r = repo.addChapter(planId, subjectId, ChapterRequest(EXTRA_TOPICS_CHAPTER))) {
                    is Resource.Success -> {
                        plan = r.data
                        chapter = r.data.subjects.firstOrNull { it.id == subjectId }?.chapters?.firstOrNull { it.name.equals(EXTRA_TOPICS_CHAPTER, ignoreCase = true) }
                    }
                    is Resource.Error -> { _uiState.update { it.copy(mutating = false, error = r.message) }; return@launch }
                    is Resource.Loading -> Unit
                }
            }
            val chapterId = chapter?.id
            if (chapterId.isNullOrBlank()) { _uiState.update { it.copy(mutating = false, error = "Could not create Custom chapter") }; return@launch }

            // 3. Add the topic pinned to today so a rebuild never moves it and
            //    future days keep their original daily-goal count.
            when (val r = repo.addTopic(planId, subjectId, chapterId, TopicRequest(name = cleaned, plannedDate = todayKey()))) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedPlan = r.data, mutating = false, message = "Added to today") }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                    refreshPlannerAchievements()
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }
    override fun updateTopic(topicId: String, status: TopicStatus?, name: String?, plannedDate: String?, notes: String?, pinned: Boolean?) {
        val state = _uiState.value
        val planId = state.selectedPlan?.id ?: return
        val today = todayKey()
        val todayTopics = state.calendar[today].orEmpty()
        val topicWasToday = todayTopics.any { it.topicId == topicId }
        val wasDone = state.calendar.values.flatten().find { it.topicId == topicId }?.status == TopicStatus.DONE
        val beforeDoneCount = todayTopics.count { it.status == TopicStatus.DONE }
        val statusMessage = when (status) {
            TopicStatus.DONE -> "Marked done"
            TopicStatus.TODO -> "Marked as to-do"
            TopicStatus.REVISION_NEEDED -> "Scheduled for revision"
            else -> "Saved"
        }
        mutateSelected(refreshCalendar = true, refreshAnalytics = true, successMessage = statusMessage, onSuccess = {
            if (topicWasToday && status == TopicStatus.DONE && !wasDone) {
                checkDailyMilestones(planId, beforeDoneCount)
            }
            if (status == TopicStatus.DONE && !wasDone) {
                markOnboardingStepDone(StudyPlannerOnboardingSteps.FIRST_TOPIC_DONE)
            }
            refreshPlannerAchievements()
        }) { planId -> repo.updateTopic(planId, topicId, TopicPatchRequest(name = name, status = status, plannedDate = plannedDate, notes = notes, pinned = pinned, clientDateKey = today)) }
    }
    override fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true, error = null) }
            when (val r = repo.deleteTopic(planId, topicId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data,
                            mutating = false,
                            message = "Topic deleted",
                            // The delete route returns an undoToken; surfacing it lets
                            // the screen offer an "Undo" action on the snackbar.
                            deleteUndoToken = r.data.undoToken?.takeIf { token -> token.isNotBlank() },
                            lastUndoableActionLabel = "Topic deletion",
                        )
                    }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun markForRevision(
        topicId: String,
        revisionDates: List<String>,
        revisionScheduleType: String?,
    ) {
        val today = todayKey()
        val firstDate = revisionDates.firstOrNull()
        mutateSelected(refreshCalendar = true) { planId ->
            repo.updateTopic(
                planId,
                topicId,
                TopicPatchRequest(
                    status = TopicStatus.REVISION_NEEDED,
                    plannedDate = firstDate,
                    pinned = firstDate != null,
                    revisionMarkedAt = today,
                    revisionReminderDates = revisionDates,
                    revisionScheduleType = revisionScheduleType,
                    clientDateKey = today,
                ),
            )
        }
    }

    override fun cancelRevision(topicId: String) {
        val today = todayKey()
        mutateSelected(refreshCalendar = true) { planId ->
            repo.updateTopic(
                planId,
                topicId,
                TopicPatchRequest(
                    status = TopicStatus.DONE,
                    plannedDate = "",
                    revisionMarkedAt = null,
                    revisionReminderDates = emptyList(),
                    revisionScheduleType = null,
                    clientDateKey = today,
                )
            )
        }
    }

    override fun autoDistribute(lockExisting: Boolean, overloadMode: String?, strategy: String?) {
        if (_uiState.value.selectedPlan?.examDate.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Set an exam date before building the planner.") }
            return
        }
        val resolvedMode = overloadMode ?: _uiState.value.planningMode
        val resolvedStrategy = strategy?.takeIf { it == "interleaved" || it == "sequential" }
        mutateAuto {
            repo.autoDistribute(
                it,
                AutoDistributeRequest(
                    fromDate = todayKey(),
                    includeRevisionNeeded = false,
                    lockExistingDates = lockExisting,
                    overloadMode = resolvedMode,
                    strategy = resolvedStrategy,
                ),
            )
        }
    }

    override fun reorderSyllabus(
        subjectIds: List<String>?,
        chapterIdsBySubjectId: Map<String, List<String>>?,
        topicIdsByChapterId: Map<String, List<String>>?,
    ) {
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            val request = ReorderSyllabusRequest(
                subjectIds = subjectIds,
                chapterIdsBySubjectId = chapterIdsBySubjectId,
                topicIdsByChapterId = topicIdsByChapterId,
            )
            // Reordering a chapter then immediately reordering a topic inside it (or
            // any two reorders fired close together) can lose the server's optimistic
            // version-lock race and come back as a 409 even though nothing was
            // actually wrong — same fix as mutateSelected: resend once after a short
            // delay, by which point the earlier write has committed.
            var result = repo.reorderSyllabus(planId, request)
            if (result is Resource.Error && result.code == 409) {
                delay(300)
                result = repo.reorderSyllabus(planId, request)
            }
            when (val r = result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        mutating = false,
                        selectedPlan = r.data,
                        message = "Syllabus order updated",
                        deleteUndoToken = r.data.undoToken?.takeIf { it.isNotBlank() },
                        lastUndoableActionLabel = "Syllabus order",
                    )
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun clearPendingAiImport() {
        _uiState.update { it.copy(pendingOpenAiImport = false) }
    }

    override fun markOnboardingStepDone(step: String) {
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            dataStore.setStudyPlannerOnboardingStepDone(planId, step, true)
            refreshOnboardingProgress(planId)
        }
    }

    override fun clearFutureDates() {
        val today = todayKey()
        // Normalize to a bare date-key before comparing: a full ISO timestamp
        // (legacy plannedDate values written before server-side canonicalization)
        // would otherwise always lexicographically sort >= a bare date-key,
        // incorrectly treating every such topic as "future".
        val refs = _uiState.value.selectedPlan?.flattenTopics().orEmpty()
            .filter { (it.topic.plannedDate?.take(10) ?: "") >= today }
        batchTopicDates(refs.map { it.topic.id }, null, "Future dates cleared")
    }

    override fun moveTopicsToDate(topicIds: List<String>, date: String) {
        if (topicIds.isEmpty()) return
        batchTopicDates(topicIds, date, "Topics moved")
    }

    override fun clearTopicDates(topicIds: List<String>) {
        if (topicIds.isEmpty()) return
        batchTopicDates(topicIds, null, "Day cleared")
    }

    override fun resetPlan() {
        val refs = _uiState.value.selectedPlan?.flattenTopics().orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true) }
            refs.forEach { repo.updateTopic(_uiState.value.selectedPlan?.id.orEmpty(), it.topic.id, TopicPatchRequest(status = TopicStatus.TODO, plannedDate = "", notes = it.topic.notes)) }
            reloadSelected("Plan reset")
        }
    }

    override fun bulkAdd(subjectId: String, chapterId: String, text: String) {
        val topics = parseBulkSyllabus(text).flatMap { it.second }.filter { it.isNotBlank() }
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true) }
            topics.forEach { repo.addTopic(planId, subjectId, chapterId, TopicRequest(it)) }
            reloadSelected("${topics.size} topics imported")
        }
    }

    private companion object {
        val bulkSubjectPalette = listOf("#0ea5e9", "#9333ea", "#16a34a", "#ef4444", "#f59e0b", "#0f766e")
        val STUDY_PLANNER_ACHIEVEMENT_IDS = listOf("SP001", "SP002", "T011", "T012")
        const val EXTRA_TOPICS_SUBJECT = "Extra Topics"
        const val EXTRA_TOPICS_CHAPTER = "Custom"
    }

    override fun importFullSyllabusFromTxt(text: String, mode: String) {
        val parsed = parseBulkSubjectsFromTxt(text)
        val groups = parsed.getOrElse { e ->
            _uiState.update { it.copy(error = e.message ?: "Invalid syllabus text") }
            return
        }
        if (groups.isEmpty()) {
            _uiState.update { it.copy(error = "No syllabus content to import") }
            return
        }
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            val resolvedMode = mode.trim().lowercase(Locale.US).takeIf { it == "replace" || it == "merge" } ?: "merge"
            _uiState.update { it.copy(mutating = true, error = null, importError = null) }
            val totalTopicCount = countBulkSubjectsTopics(groups)
            val totalChapterCount = countBulkSubjectsChapters(groups)
            val message = if (totalTopicCount > 0) {
                if (resolvedMode == "replace") {
                    "Replaced syllabus with $totalTopicCount topics across $totalChapterCount chapters"
                } else {
                    "Added new syllabus items. Duplicates were skipped."
                }
            } else {
                if (resolvedMode == "replace") "Replaced syllabus" else "Syllabus updated"
            }
            when (val result = repo.importManualSyllabus(planId, text, resolvedMode)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            mutating = false,
                            selectedPlan = result.data,
                            message = message,
                            importResultSummary = message,
                        )
                    }
                    reloadSelected(message)
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            mutating = false,
                            error = result.message,
                            importError = result.message,
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun structureSyllabusPreview(rawText: String, language: String?) {
        val plan = _uiState.value.selectedPlan
        viewModelScope.launch {
            _uiState.update { it.copy(isStructuringSyllabus = true, structureError = null, structuredPreview = null) }
            val request = StructureSyllabusRequest(
                rawText = rawText,
                examType = plan?.examType,
                planTitle = plan?.title,
                language = language
            )
            when (val r = repo.structureSyllabusPreview(request)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isStructuringSyllabus = false, structuredPreview = r.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isStructuringSyllabus = false, structureError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun updateStructuredPreview(preview: com.safarparmar.app.data.remote.api.StructuredSyllabusPreview?) {
        _uiState.update { it.copy(structuredPreview = preview) }
    }

    override fun importStructuredSyllabus(mode: String) {
        val preview = _uiState.value.structuredPreview ?: return
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingStructuredSyllabus = true, structuredImportError = null, structuredImportSuccessMessage = null) }
            when (val r = repo.applySyllabusAi(planId, preview, mode)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isImportingStructuredSyllabus = false,
                            selectedPlan = r.data,
                            structuredPreview = null,
                            structuredImportSuccessMessage = if (mode == "replace") "Syllabus replaced successfully!" else "Syllabus merged successfully!"
                        )
                    }
                    reloadSelected("Syllabus imported")
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isImportingStructuredSyllabus = false, structuredImportError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun batchTopicDates(topicIds: List<String>, date: String?, message: String) = viewModelScope.launch {
        val planId = _uiState.value.selectedPlan?.id ?: return@launch
        if (topicIds.isEmpty()) return@launch
        _uiState.update { it.copy(mutating = true, error = null) }
        // One atomic write for the whole batch instead of one PATCH per topic —
        // a failure partway through the old sequential loop could leave some
        // topics moved and others not, with no way to tell which.
        val request = BatchTopicUpdateRequest(
            updates = topicIds.map {
                BatchTopicUpdateItem(topicId = it, patch = TopicPatchRequest(plannedDate = date ?: ""))
            },
        )
        when (val r = repo.batchUpdateTopics(planId, request)) {
            is Resource.Success -> {
                _uiState.update {
                    it.copy(
                        selectedPlan = r.data,
                        mutating = false,
                        message = message,
                        // 2+ topics changed at once means the server took a snapshot —
                        // surfacing its undoToken lets the screen offer "Undo".
                        deleteUndoToken = r.data.undoToken?.takeIf { token -> token.isNotBlank() },
                        lastUndoableActionLabel = message,
                    )
                }
                refreshCalendar(planId)
                refreshAnalytics(planId)
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
    }

    override fun swapTopicDates(firstTopicId: String, secondTopicId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val plan = state.selectedPlan ?: return@launch
            val planId = plan.id
            val refsById = plan.flattenTopics().associateBy { it.topic.id }
            val first = refsById[firstTopicId]?.topic ?: return@launch
            val second = refsById[secondTopicId]?.topic ?: return@launch
            val firstDate = first.plannedDate?.takeIf { it.isNotBlank() } ?: return@launch
            val secondDate = second.plannedDate?.takeIf { it.isNotBlank() } ?: return@launch
            if (firstDate.take(10) == secondDate.take(10)) {
                _uiState.update { it.copy(error = "Both topics are already planned for the same day.") }
                return@launch
            }
            _uiState.update { it.copy(mutating = true, error = null) }
            // A single atomic write — both topics' dates are exchanged in one
            // server-side read-modify-write, so there's no intermediate state for
            // a failure to strand (the old 3-request dance needed manual rollback
            // because it wrote each topic separately). The server also snapshots
            // the plan beforehand, so an accidental swap can be undone.
            val request = BatchTopicUpdateRequest(
                updates = listOf(
                    BatchTopicUpdateItem(topicId = firstTopicId, patch = TopicPatchRequest(plannedDate = secondDate)),
                    BatchTopicUpdateItem(topicId = secondTopicId, patch = TopicPatchRequest(plannedDate = firstDate)),
                ),
            )
            when (val r = repo.batchUpdateTopics(planId, request)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data,
                            mutating = false,
                            message = "Topics swapped",
                            deleteUndoToken = r.data.undoToken?.takeIf { token -> token.isNotBlank() },
                            lastUndoableActionLabel = "Topic swap",
                        )
                    }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun replaceTopicToday(currentTopicId: String, replacementTopicId: String, todayDate: String) {
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true) }
            when (val clearCurrent = repo.updateTopic(planId, currentTopicId, TopicPatchRequest(plannedDate = ""))) {
                is Resource.Error -> {
                    _uiState.update { it.copy(mutating = false, error = clearCurrent.message) }
                    return@launch
                }
                is Resource.Loading -> Unit
                is Resource.Success -> Unit
            }
            when (val moveReplacement = repo.updateTopic(planId, replacementTopicId, TopicPatchRequest(plannedDate = todayDate))) {
                is Resource.Error -> {
                    repo.updateTopic(planId, currentTopicId, TopicPatchRequest(plannedDate = todayDate))
                    _uiState.update { it.copy(mutating = false, error = moveReplacement.message) }
                    return@launch
                }
                is Resource.Loading -> Unit
                is Resource.Success -> Unit
            }
            reloadSelected("Topic replaced")
        }
    }

    private fun mutateAuto(call: suspend (String) -> Resource<AutoDistributeResult>) = viewModelScope.launch {
        val planId = _uiState.value.selectedPlan?.id ?: return@launch
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = call(planId)) {
            is Resource.Success -> {
                dataStore.setStudyPlannerOnboardingStepDone(planId, StudyPlannerOnboardingSteps.BUILD_SCHEDULE, true)
                val totalTopicsCount = _uiState.value.selectedPlan?.subjects?.sumOf { subject ->
                    subject.chapters.sumOf { it.topics.size }
                } ?: 0
                val msg = when {
                    r.data.assigned == 0 && r.data.skipped == 0 -> {
                        if (totalTopicsCount == 0) {
                            "Please create at least one topic in each subject to build a plan."
                        } else {
                            "All topics are already scheduled!"
                        }
                    }
                    r.data.skipped == 0 -> "All ${r.data.assigned} topics scheduled successfully!"
                    else -> "Scheduled ${r.data.assigned} topics. ${r.data.skipped} skipped due to limited time."
                }
                _uiState.update { it.copy(mutating = false, selectedPlan = r.data.plan ?: it.selectedPlan, message = msg) }
                refreshOnboardingProgress(planId)
                reloadSelected()
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
    }

    private fun refreshOnboardingProgress(planId: String) = viewModelScope.launch {
        val completed = dataStore.studyPlannerOnboardingCompletedSteps(planId).first()
        _uiState.update { state ->
            if (state.selectedPlan?.id == planId) {
                state.copy(onboardingCompletedSteps = completed)
            } else {
                state
            }
        }
    }

    private fun mutateSelected(
        refreshCalendar: Boolean = false,
        refreshAnalytics: Boolean = false,
        successMessage: String = "Saved",
        // Label shown as "<undoLabel> undone" if this call's response carries an
        // undoToken (e.g. delete-subject/chapter). Ignored when there's no token.
        undoLabel: String? = null,
        onSuccess: (suspend () -> Unit)? = null,
        call: suspend (String) -> Resource<StudyPlan>,
    ) {
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true, error = null) }
            // The server uses optimistic version-locking: it reads the plan, computes
            // the change, then writes back only if nobody else wrote in between. Two
            // requests fired close together (e.g. adding a topic right after adding its
            // chapter, before the first write has settled) can lose that race and get a
            // 409 "modified by another request" response even though nothing was
            // actually wrong. The retry below just re-sends the exact same mutation —
            // by the time it lands, the earlier write has committed, so the server's
            // fresh read no longer conflicts with anything.
            var result = call(planId)
            if (result is Resource.Error && result.code == 409) {
                delay(300)
                result = call(planId)
            }
            when (val r = result) {
                is Resource.Success -> {
                    val undoToken = r.data.undoToken?.takeIf { it.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            selectedPlan = r.data,
                            mutating = false,
                            message = successMessage,
                            deleteUndoToken = undoToken,
                            lastUndoableActionLabel = undoLabel.takeIf { undoToken != null },
                        )
                    }
                    if (refreshCalendar) refreshCalendar(planId)
                    if (refreshAnalytics) refreshAnalytics(planId)
                    onSuccess?.invoke()
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private suspend fun tryCreatePlanFromTemplateWithLocalFallback(
        templateId: String,
        title: String,
        examDate: String?,
        dailyGoal: Int,
        offDays: List<Int>,
        manualSubjectOrder: Boolean = false,
    ) {
        val request = CreateFromTemplateRequest(
            templateId = templateId,
            title = title,
            examDate = examDate,
            dailyGoal = dailyGoal,
            offDays = offDays,
            autoDistribute = false,
        )
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = repo.createPlanFromTemplate(request)) {
            is Resource.Success -> {
                // The from-template response already contains the fully-hydrated plan
                // (subjects → chapters → topics). Use it directly instead of refetching
                // via openPlan(), which fires getPlan + calendar + analytics back-to-back
                // and can trip a rate limit on shared mobile IPs — that was making the
                // template appear "not imported" on Android even though it succeeded.
                _uiState.update {
                    it.copy(
                        mutating = false,
                        loading = false,
                        selectedPlan = r.data,
                        section = PlannerSection.PLAN,
                        message = "Plan created",
                        pendingManualSubjectOrder = manualSubjectOrder && r.data.subjects.isNotEmpty(),
                    )
                }
                hydratePlanFromServerBestEffort(r.data.id)
                refreshPlannerAchievements()
            }
            is Resource.Error -> {
                if (getLocalExamTemplate(templateId) != null) {
                    createPlanFromLocalTemplate(
                        templateId,
                        title,
                        examDate,
                        dailyGoal,
                        offDays,
                        successMessage = "Plan created from saved template.",
                        manualSubjectOrder = manualSubjectOrder,
                    )
                } else {
                    _uiState.update { it.copy(mutating = false, error = r.message) }
                }
            }
            is Resource.Loading -> Unit
        }
    }

    /**
     * Background hydration after a template plan is created. Any failure here
     * (rate limits, transient network) is non-fatal: the optimistic plan from
     * the from-template response is already showing, so we just skip the
     * calendar/analytics until the next manual refresh.
     */
    private fun hydratePlanFromServerBestEffort(planId: String) = viewModelScope.launch {
        when (val plansResult = repo.listPlans()) {
            is Resource.Success -> _uiState.update { it.copy(plans = plansResult.data) }
            else -> Unit
        }
        when (val calendar = repo.getCalendar(planId)) {
            is Resource.Success -> _uiState.update { it.copy(calendar = calendar.data) }
            else -> Unit
        }
        when (val analytics = repo.getAnalytics(planId)) {
            is Resource.Success -> _uiState.update { it.copy(analytics = analytics.data) }
            else -> Unit
        }
    }

    private suspend fun mutatePlanList(openAiImport: Boolean = false, call: suspend () -> Resource<StudyPlan>) {
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = call()) {
            is Resource.Success -> {
                _uiState.update { it.copy(mutating = false, message = "Plan created") }
                refreshPlans()
                _uiState.update {
                    it.copy(
                        selectedPlan = r.data,
                        section = if (openAiImport) PlannerSection.SYLLABUS else PlannerSection.YOUR_EXAMS,
                        pendingOpenAiImport = openAiImport,
                    )
                }
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
    }

    private suspend fun createPlanFromLocalTemplate(
        templateId: String,
        title: String,
        examDate: String?,
        dailyGoal: Int,
        offDays: List<Int>,
        successMessage: String = "Plan created",
        manualSubjectOrder: Boolean = false,
    ) {
        val template = getLocalExamTemplate(templateId)
        if (template == null) {
            _uiState.update { it.copy(mutating = false, error = "Template data missing") }
            return
        }

        _uiState.update { it.copy(mutating = true, error = null) }
        val plan = when (val pr = repo.createPlan(CreatePlanRequest(title = title, examType = template.name, examDate = examDate, dailyGoal = dailyGoal, offDays = offDays))) {
            is Resource.Success -> pr.data
            is Resource.Error -> {
                _uiState.update { it.copy(mutating = false, error = pr.message) }
                return
            }
            is Resource.Loading -> return
        }

        // Map the static template structure to an ImportSyllabusRequest
        val subjectsRequest = template.subjects.map { subject ->
            ImportSyllabusSubjectRequest(
                name = subject.name,
                chapters = subject.chapters.map { chapter ->
                    ImportSyllabusChapterRequest(
                        name = chapter.name,
                        topics = chapter.topics.map { topicName ->
                            ImportSyllabusTopicRequest(name = topicName)
                        }
                    )
                }
            )
        }
        val importRequest = ImportSyllabusRequest(
            subjects = subjectsRequest,
            mode = "replace"
        )

        val importResult = repo.importSyllabus(plan.id, importRequest)
        val finalPlan = when (importResult) {
            is Resource.Success -> importResult.data
            is Resource.Error -> {
                _uiState.update { it.copy(mutating = false, error = importResult.message) }
                return
            }
            is Resource.Loading -> return
        }

        _uiState.update {
            it.copy(
                mutating = false,
                selectedPlan = finalPlan,
                message = successMessage,
                section = PlannerSection.PLAN,
                pendingManualSubjectOrder = manualSubjectOrder && finalPlan.subjects.isNotEmpty(),
            )
        }
        StudyPlannerAnalytics.track(StudyPlannerAnalytics.PLAN_CREATED_TEMPLATE)
        refreshPlans()
        refreshPlannerAchievements()
    }

    private suspend fun refreshCalendar(planId: String) {
        when (val r = repo.getCalendar(planId)) {
            is Resource.Success -> _uiState.update { it.copy(calendar = r.data) }
            is Resource.Error -> Unit
            is Resource.Loading -> Unit
        }
    }

    private suspend fun refreshAnalytics(planId: String) {
        when (val r = repo.getAnalytics(planId)) {
            is Resource.Success -> _uiState.update { it.copy(analytics = r.data) }
            is Resource.Error -> Unit
            is Resource.Loading -> Unit
        }
    }

    private suspend fun reloadSelected(message: String? = null) {
        val planId = _uiState.value.selectedPlan?.id ?: return
        when (val r = repo.getPlan(planId)) {
            is Resource.Success -> _uiState.update { it.copy(selectedPlan = r.data, mutating = false, message = message ?: it.message) }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
        refreshCalendar(planId)
        refreshAnalytics(planId)
        refreshPlannerAchievements()
    }

    private fun refreshPlannerAchievements() {
        viewModelScope.launch {
            when (val r = homeRepository.getAchievements()) {
                is Resource.Success -> {
                    val order = STUDY_PLANNER_ACHIEVEMENT_IDS.withIndex().associate { it.value to it.index }
                    _uiState.update {
                        it.copy(
                            plannerAchievements = r.data
                                .filter { achievement -> achievement.id in STUDY_PLANNER_ACHIEVEMENT_IDS }
                                .sortedBy { achievement -> order[achievement.id] ?: Int.MAX_VALUE }
                        )
                    }
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    private suspend fun checkDailyMilestones(planId: String, beforeDoneCount: Int) {
        val today = todayKey()
        val calendar = _uiState.value.calendar
        val todayTopics = calendar[today] ?: return
        
        val total = todayTopics.size
        if (total == 0) return
        
        val doneCount = todayTopics.count { it.status == TopicStatus.DONE }
        val half = (total + 1) / 2
        val milestone = when {
            beforeDoneCount < half && doneCount >= half -> "half"
            beforeDoneCount < total && doneCount >= total -> "complete"
            else -> return
        }
        val key = "$planId:$today:$milestone"
        if (!firedDailyMilestones.add(key)) return
        
        val planTitle = _uiState.value.selectedPlan?.title ?: "Study Plan"
        val notificationManager = SafarNotificationManager(context)
        
        notificationManager.show(
            title = planTitle,
            body = "You've got this, keep going !",
            channelId = SafarNotificationChannels.STUDY_REMINDERS,
            deepLink = "safar://studyplanner"
        )
    }
}
