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
import com.safarparmar.app.data.remote.api.FinishDayRequest
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
import com.safarparmar.app.data.remote.api.UndoFinishDayRequest
import com.safarparmar.app.domain.model.Achievement
import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.RevisionCompletion
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
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import com.safarparmar.app.ui.studyplanner.templates.getLocalExamTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
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

private fun StudyPlan.withPlannedDates(datesByTopicId: Map<String, String?>): StudyPlan = copy(
    subjects = subjects.map { subject ->
        subject.copy(
            chapters = subject.chapters.map { chapter ->
                chapter.copy(
                    topics = chapter.topics.map { topic ->
                        if (topic.id in datesByTopicId) {
                            topic.copy(plannedDate = datesByTopicId[topic.id])
                        } else {
                            topic
                        }
                    },
                )
            },
        )
    },
)

data class FinishDayUndoState(
    val undoToken: String,
)

data class StudyPlannerUiState(
    val plans: List<StudyPlan> = emptyList(),
    val templates: List<ExamTemplateSummary> = emptyList(),
    val selectedPlan: StudyPlan? = null,
    val calendar: CalendarMap = emptyMap(),
    val analytics: PlannerAnalytics? = null,
    val section: PlannerSection = PlannerSection.YOUR_EXAMS,
    val loading: Boolean = false,
    val mutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** The current snackbar's action should open the unscheduled-topics sheet. */
    val messageOpensUnscheduled: Boolean = false,
    val rolloverUndoToken: String? = null,
    val deleteUndoToken: String? = null,
    val finishDayUndo: FinishDayUndoState? = null,
    /** What the current deleteUndoToken would restore, e.g. "Topic swap" — used to
     *  phrase the confirmation after Undo is tapped ("Topic swap undone") instead of
     *  a generic message that doesn't match what was actually undone. */
    val lastUndoableActionLabel: String? = null,
    /** Per-plan scheduling mode: "flex" (allow over-goal days) or "strict". */
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
    /** Set when Insights navigates the user to Calendar to resolve overdue/unplanned
     *  topics. CalendarTab opens the Missed Topics sheet automatically while this is
     *  true, then clears it via [PlannerActions.clearPendingOpenMissedTopics]. */
    val pendingOpenMissedTopics: Boolean = false,
    val pendingOpenUnscheduledTopics: Boolean = false,
    val isImporting: Boolean = false,
    val importStatus: String? = null,
    val importError: String? = null,
    val importResultSummary: String? = null,
    val hydrateWarning: String? = null,
    val onboardingCompletedSteps: Set<String> = emptySet(),
    val plannerAchievements: List<Achievement> = emptyList(),
    val activePlanTab: StudyPlannerTab = StudyPlannerTab.TODAY,
    /** The in-planner destination to restore when a contextual drill-in is closed. */
    val backDestination: PlannerBackDestination? = null,
    /** Set when the user changed the exam date and chose to reorder the syllabus
     *  before rebuilding; consumed by the Syllabus "Build" button. */
    val pendingRebuild: PendingRebuild? = null,
    /** Chapter ratings saved since the last schedule rebuild. */
    val pendingRatingChapterIds: Set<String> = emptySet(),
    /** Dismissal only hides the prompt; a later rating change shows it again. */
    val ratingRebuildPromptDismissed: Boolean = false,
)

data class PlannerBackDestination(
    val section: PlannerSection,
    val planTab: StudyPlannerTab,
)

/** A rebuild the user armed (via the re-plan flow) but deferred so they can
 *  reorder the syllabus first. */
data class PendingRebuild(
    val strategy: String,
    /** Mixed Bag's chosen subjects, in pick order. */
    val prioritySubjectNames: List<String> = emptyList(),
    /** "sequential" | "balanced" ordering within the priority phase. */
    val priorityOrderMode: String? = null,
)

@HiltViewModel
class StudyPlannerViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
    private val homeRepository: HomeRepository,
    val dataStore: com.safarparmar.app.data.local.SafarDataStore,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), PlannerActions {
    private companion object {
        const val SELECTED_PLAN_ID_KEY = "study_planner_selected_plan_id"
        val bulkSubjectPalette = listOf("#0ea5e9", "#9333ea", "#16a34a", "#ef4444", "#f59e0b", "#0f766e")
        val STUDY_PLANNER_ACHIEVEMENT_IDS = listOf("SP001", "SP002", "T011", "T012")
        const val EXTRA_TOPICS_SUBJECT = "Extra Topics"
        const val EXTRA_TOPICS_CHAPTER = "Custom"
    }

    private val _uiState = MutableStateFlow(StudyPlannerUiState())
    val uiState = _uiState.asStateFlow()
    private val firedDailyMilestones = mutableSetOf<String>()
    /** Prevents a deliberate return to the plan picker from being auto-opened again. */
    private var initialLandingResolved = false


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
            ?: savedStateHandle.get<String>(SELECTED_PLAN_ID_KEY)
        if (!planId.isNullOrBlank()) {
            initialLandingResolved = true
            openPlan(planId)
        } else {
            viewModelScope.launch {
                val strategy = dataStore.plannerPreferredStrategy(null).first()
                _uiState.update { it.copy(preferredStudyStrategy = strategy) }
            }
        }

        // Live-refresh when an Ekagra session marks one of this plan's topics done
        // from the other feature's ViewModel — reload silently so the topic flips
        // to done without waiting for the planner's next cold load.
        viewModelScope.launch {
            PlannerTopicEventBus.topicCompletedFromEkagra.collect { completedPlanId ->
                if (_uiState.value.selectedPlan?.id == completedPlanId) {
                    reloadSelected()
                }
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

    override fun openRevisionTopics() {
        _uiState.update { state ->
            state.copy(
                // Revision is now its own top-level section rather than a hidden
                // sub-tab crammed into Home. Remember where we came from so Back
                // returns to the origin (Calendar / Progress / etc.).
                section = PlannerSection.REVISION,
                backDestination = PlannerBackDestination(
                    section = state.section,
                    planTab = state.activePlanTab,
                ),
            )
        }
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
        val state = _uiState.value
        val hasPlan = state.selectedPlan != null
        if (!hasPlan) {
            // Nothing internal to consume — let the NavController handle it.
            return false
        }

        // Contextual drill-ins (for example Calendar → Revision) return to their
        // origin before the regular tab-level Back behaviour is considered.
        state.backDestination?.let { destination ->
            _uiState.update {
                it.copy(
                    section = destination.section,
                    activePlanTab = destination.planTab,
                    backDestination = null,
                )
            }
            return true
        }

        // If the user is not on the PLAN tab, navigating back takes them to PLAN.
        if (state.section != PlannerSection.PLAN) {
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
                finishDayUndo = null,
                lastUndoableActionLabel = null,
                messageOpensUnscheduled = false,
            )
        }
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
                is Resource.Error -> _uiState.update {
                    it.copy(
                        mutating = false,
                        error = r.message,
                        deleteUndoToken = undoToken,
                        lastUndoableActionLabel = undoneLabel,
                    )
                }
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
                is Resource.Error -> _uiState.update {
                    it.copy(mutating = false, error = r.message, rolloverUndoToken = undoToken)
                }
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
                is Resource.Success -> {
                    _uiState.update { it.copy(plans = r.data, loading = false) }
                    // Existing users should land on their daily plan, rather than the
                    // plan-creation/picker screen. Resolve this only once per feature
                    // visit so closePlan() remains a reliable way to reach the picker.
                    if (!initialLandingResolved) {
                        initialLandingResolved = true
                        if (_uiState.value.selectedPlan == null) {
                            r.data.firstOrNull()?.let { openPlan(it.id) }
                        }
                    }
                }
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
        initialLandingResolved = true
        savedStateHandle[SELECTED_PLAN_ID_KEY] = planId
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    section = it.section,
                    pendingRatingChapterIds = emptySet(),
                    ratingRebuildPromptDismissed = false,
                )
            }
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
        savedStateHandle[SELECTED_PLAN_ID_KEY] = null
        _uiState.update {
            it.copy(
                selectedPlan = null,
                calendar = emptyMap(),
                analytics = null,
                section = PlannerSection.PLAN,
                onboardingCompletedSteps = emptySet(),
                backDestination = null,
                pendingRatingChapterIds = emptySet(),
                ratingRebuildPromptDismissed = false,
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

    override fun openMissedTopics() {
        _uiState.update { it.copy(section = PlannerSection.CALENDAR, pendingOpenMissedTopics = true) }
    }

    override fun clearPendingOpenMissedTopics() {
        _uiState.update { it.copy(pendingOpenMissedTopics = false) }
    }

    override fun openUnscheduledTopics() {
        _uiState.update { it.copy(section = PlannerSection.PLAN, pendingOpenUnscheduledTopics = true) }
    }

    override fun clearPendingOpenUnscheduledTopics() {
        _uiState.update { it.copy(pendingOpenUnscheduledTopics = false) }
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
                    if (_uiState.value.selectedPlan?.id == planId) {
                        savedStateHandle[SELECTED_PLAN_ID_KEY] = null
                    }
                    _uiState.update { it.copy(mutating = false, selectedPlan = null, message = "Plan deleted") }
                    refreshPlans()
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun updatePlan(request: UpdatePlanRequest) = mutateSelected { planId -> repo.updatePlan(planId, request) }
    override fun addSubject(name: String) = addSubjects(listOf(name))

    override fun addSubjects(names: List<String>) {
        val cleanNames = names.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanNames.isEmpty()) return
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            val startIndex = _uiState.value.selectedPlan?.subjects?.size ?: 0
            var latestPlan: StudyPlan? = null
            for ((i, name) in cleanNames.withIndex()) {
                val color = bulkSubjectPalette[(startIndex + i) % bulkSubjectPalette.size]
                when (val r = repo.addSubject(planId, SubjectRequest(name = name, color = color))) {
                    is Resource.Success -> latestPlan = r.data
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                selectedPlan = latestPlan ?: it.selectedPlan,
                                mutating = false,
                                error = "${i} of ${cleanNames.size} subjects were added before the request failed. ${r.message.orEmpty()}".trim(),
                            )
                        }
                        return@launch
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(mutating = false, error = "Adding subjects did not complete.") }
                        return@launch
                    }
                }
            }
            val plan = latestPlan ?: return@launch
            _uiState.update {
                it.copy(
                    selectedPlan = plan,
                    mutating = false,
                    message = if (cleanNames.size > 1) "${cleanNames.size} subjects added" else "Saved",
                )
            }
        }
    }

    override fun renameSubject(subjectId: String, name: String) = mutateSelected { planId -> repo.renameSubject(planId, subjectId, SubjectRequest(name = name)) }
    override fun deleteSubject(subjectId: String) = mutateSelected(
        // Removing a subject drops all its topics, so the calendar and analytics
        // must be refetched or Progress keeps counting the deleted work.
        refreshCalendar = true,
        refreshAnalytics = true,
        successMessage = "Subject deleted",
        undoLabel = "Subject deletion",
    ) { planId -> repo.deleteSubject(planId, subjectId) }
    override fun addChapter(subjectId: String, name: String) = addChapters(subjectId, listOf(name))

    override fun addChapters(subjectId: String, names: List<String>) {
        val cleanNames = names.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanNames.isEmpty()) return
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            var latestPlan: StudyPlan? = null
            for ((index, name) in cleanNames.withIndex()) {
                when (val r = repo.addChapter(planId, subjectId, ChapterRequest(name))) {
                    is Resource.Success -> latestPlan = r.data
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                selectedPlan = latestPlan ?: it.selectedPlan,
                                mutating = false,
                                error = "$index of ${cleanNames.size} chapters were added before the request failed. ${r.message.orEmpty()}".trim(),
                            )
                        }
                        return@launch
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(mutating = false, error = "Adding chapters did not complete.") }
                        return@launch
                    }
                }
            }
            val plan = latestPlan ?: return@launch
            _uiState.update {
                it.copy(
                    selectedPlan = plan,
                    mutating = false,
                    message = if (cleanNames.size > 1) "${cleanNames.size} chapters added" else "Saved",
                )
            }

            // New chapters land at the end server-side — move them to the front of the
            // subject's chapter list so the user sees what they just added without
            // scrolling, then persist that as the subject's real order.
            val subject = plan.subjects.find { it.id == subjectId }
            if (subject != null && subject.chapters.size > cleanNames.size) {
                val ids = subject.chapters.map { it.id }
                val newest = ids.takeLast(cleanNames.size)
                val rest = ids.dropLast(cleanNames.size)
                reorderSyllabus(chapterIdsBySubjectId = mapOf(subjectId to (newest + rest)))
            }
        }
    }

    override fun renameChapter(subjectId: String, chapterId: String, name: String) = mutateSelected { planId -> repo.renameChapter(planId, subjectId, chapterId, ChapterRequest(name)) }
    override fun rateChapter(subjectId: String, chapterId: String, difficulty: String?) = mutateSelected(
        successMessage = "Chapter rated",
        onSuccess = {
            _uiState.update {
                it.copy(
                    pendingRatingChapterIds = it.pendingRatingChapterIds + chapterId,
                    ratingRebuildPromptDismissed = false,
                )
            }
        },
    ) { planId ->
        // "" clears the rating server-side; Gson would drop a null field entirely.
        repo.renameChapter(planId, subjectId, chapterId, ChapterRequest(difficulty = difficulty ?: ""))
    }

    override fun dismissRatingRebuildPrompt() {
        _uiState.update { it.copy(ratingRebuildPromptDismissed = true) }
    }

    override fun rebuildAfterRatingChanges() {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return
        if (plan.examDate.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Set an exam date before rebuilding the planner.") }
            return
        }
        if (state.pendingRatingChapterIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            val result = repo.autoDistribute(
                plan.id,
                AutoDistributeRequest(
                    fromDate = todayKey(),
                    includeRevisionNeeded = false,
                    lockExistingDates = false,
                    preserveFromDate = true,
                    overloadMode = null,
                    strategy = state.preferredStudyStrategy,
                ),
            )
            when (result) {
                is Resource.Success -> {
                    val message = if (result.data.skipped > 0) {
                        "Schedule updated · ${result.data.skipped} ${if (result.data.skipped == 1) "topic is" else "topics are"} unscheduled"
                    } else {
                        "Schedule updated"
                    }
                    _uiState.update {
                        it.copy(
                            selectedPlan = result.data.plan ?: it.selectedPlan,
                            mutating = false,
                            message = message,
                            messageOpensUnscheduled = result.data.skipped > 0,
                            pendingRatingChapterIds = emptySet(),
                            ratingRebuildPromptDismissed = false,
                        )
                    }
                    reloadSelected(message)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }
    override fun deleteChapter(subjectId: String, chapterId: String) = mutateSelected(
        // Same as deleteSubject: the removed chapter's topics must leave the
        // calendar/analytics too, or Progress shows stale counts.
        refreshCalendar = true,
        refreshAnalytics = true,
        successMessage = "Chapter deleted",
        undoLabel = "Chapter deletion",
    ) { planId -> repo.deleteChapter(planId, subjectId, chapterId) }
    override fun addTopic(subjectId: String, chapterId: String, name: String) = addTopics(subjectId, chapterId, listOf(name))

    override fun addTopics(subjectId: String, chapterId: String, names: List<String>) {
        val cleanNames = names.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanNames.isEmpty()) return
        val planId = _uiState.value.selectedPlan?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            val result = repo.bulkTopics(
                planId,
                subjectId,
                chapterId,
                BulkTopicsRequest(cleanNames.map(::BulkTopicItemRequest)),
            )
            val plan = when (result) {
                is Resource.Success -> result.data
                is Resource.Error -> {
                    _uiState.update { it.copy(mutating = false, error = result.message) }
                    return@launch
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(mutating = false) }
                    return@launch
                }
            }
            _uiState.update {
                it.copy(
                    selectedPlan = plan,
                    mutating = false,
                    message = if (cleanNames.size > 1) "${cleanNames.size} topics added" else "Saved",
                )
            }
            refreshPlannerAchievements()

            // New topics land at the end server-side — move them to the front of the
            // chapter's topic list so the user sees what they just added without
            // scrolling, then persist that as the chapter's real order.
            val chapter = plan.subjects.find { it.id == subjectId }?.chapters?.find { it.id == chapterId }
            if (chapter != null && chapter.topics.size > cleanNames.size) {
                val ids = chapter.topics.map { it.id }
                val newest = ids.takeLast(cleanNames.size)
                val rest = ids.dropLast(cleanNames.size)
                reorderSyllabus(topicIdsByChapterId = mapOf(chapterId to (newest + rest)))
            }
        }
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
    override fun updateTopic(topicId: String, status: TopicStatus?, name: String?, plannedDate: String?, notes: String?, pinned: Boolean?, size: String?) {
        val state = _uiState.value
        val planId = state.selectedPlan?.id ?: return
        val today = todayKey()
        val todayTopics = state.calendar[today].orEmpty()
        val topicWasToday = todayTopics.any { it.topicId == topicId }
        val wasDone = state.calendar.values.flatten().find { it.topicId == topicId }?.status == TopicStatus.DONE
        val beforeDoneCount = todayTopics.count { it.status == TopicStatus.DONE }
        val statusMessage = when (status) {
            TopicStatus.DONE -> "Marked done"
            TopicStatus.TODO -> "Marked as not done"
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
        }) { planId -> repo.updateTopic(planId, topicId, TopicPatchRequest(name = name, status = status, plannedDate = plannedDate, notes = notes, pinned = pinned, size = size, clientDateKey = today)) }
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

    private fun findSelectedTopic(topicId: String): StudyTopic? =
        _uiState.value.selectedPlan
            ?.subjects
            ?.asSequence()
            ?.flatMap { it.chapters.asSequence() }
            ?.flatMap { it.topics.asSequence() }
            ?.firstOrNull { it.id == topicId }

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
                    // Fresh cycle: nothing completed yet.
                    revisionCompletedDates = emptyList(),
                    revisionCompletionLog = emptyList(),
                    revisionScheduleType = revisionScheduleType,
                    clientDateKey = today,
                ),
            )
        }
    }

    /**
     * Completes ONE spaced-revision session: moves [sessionDate] from the remaining
     * reminder list into the completed list. When it was the last remaining session
     * the whole topic graduates to DONE. This is what lets a user progress through
     * sessions 2, 3, 4, 5 instead of the first tick ending the whole topic.
     */
    override fun completeRevisionSession(topicId: String, sessionDate: String) {
        val topic = findSelectedTopic(topicId) ?: return
        val target = sessionDate.take(10)
        // Only accept a completion for a date that was actually a remaining session.
        if (topic.revisionReminderDates.none { it.take(10) == target }) return

        val remaining = topic.revisionReminderDates
            .map { it.take(10) }
            .filter { it != target }
            .distinct()
            .sorted()
        val completed = (topic.revisionCompletedDates.orEmpty().map { it.take(10) } + target)
            .distinct()
            .sorted()
        val today = todayKey()
        val completionLog = (
            topic.revisionCompletionLog.orEmpty().filterNot { it.sessionDate.take(10) == target } +
                RevisionCompletion(sessionDate = target, completedDate = today)
            ).sortedBy { it.sessionDate.take(10) }

        mutateSelected(refreshCalendar = true) { planId ->
            if (remaining.isEmpty()) {
                repo.updateTopic(
                    planId,
                    topicId,
                    TopicPatchRequest(
                        status = TopicStatus.DONE,
                        plannedDate = "",
                        revisionReminderDates = emptyList(),
                        // Keep every finished revision so Progress can show the
                        // study work after the final revision is also done.
                        revisionCompletedDates = completed,
                        revisionCompletionLog = completionLog,
                        revisionScheduleType = null,
                        clientDateKey = today,
                    ),
                )
            } else {
                repo.updateTopic(
                    planId,
                    topicId,
                    TopicPatchRequest(
                        status = TopicStatus.REVISION_NEEDED,
                        // Point views at the next appointment, not the one just completed.
                        plannedDate = remaining.first(),
                        pinned = true,
                        revisionReminderDates = remaining,
                        revisionCompletedDates = completed,
                        revisionCompletionLog = completionLog,
                        revisionScheduleType = topic.revisionScheduleType,
                        clientDateKey = today,
                    ),
                )
            }
        }
    }

    /** Undo a mistaken session tick: move [sessionDate] back from completed to remaining. */
    override fun uncompleteRevisionSession(topicId: String, sessionDate: String) {
        val topic = findSelectedTopic(topicId) ?: return
        val target = sessionDate.take(10)
        if (topic.revisionCompletedDates.orEmpty().none { it.take(10) == target }) return

        val completed = topic.revisionCompletedDates.orEmpty()
            .map { it.take(10) }
            .filter { it != target }
            .distinct()
            .sorted()
        val completionLog = topic.revisionCompletionLog.orEmpty()
            .filterNot { it.sessionDate.take(10) == target }
        val remaining = (topic.revisionReminderDates.map { it.take(10) } + target)
            .distinct()
            .sorted()
        val today = todayKey()

        mutateSelected(refreshCalendar = true) { planId ->
            repo.updateTopic(
                planId,
                topicId,
                TopicPatchRequest(
                    status = TopicStatus.REVISION_NEEDED,
                    plannedDate = remaining.first(),
                    pinned = true,
                    revisionReminderDates = remaining,
                    revisionCompletedDates = completed,
                    revisionCompletionLog = completionLog,
                    revisionScheduleType = topic.revisionScheduleType,
                    clientDateKey = today,
                ),
            )
        }
    }

    override fun completeRevisionForDate(topicId: String, date: String) {
        // Today-tab / single-checkbox path: resolve the session that is actually
        // due and complete that one. Prefer an exact match, else the latest session
        // on/before [date] (a due or overdue one), else the earliest remaining. This
        // replaces the old behaviour that only removed [date] if it happened to be
        // in the list — which made ticking on any non-reminder day do nothing.
        val topic = findSelectedTopic(topicId) ?: return
        val target = date.take(10)
        val remaining = topic.revisionReminderDates
            .map { it.take(10) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        if (remaining.isEmpty()) return
        val due = when {
            remaining.contains(target) -> target
            else -> remaining.lastOrNull { it <= target } ?: remaining.first()
        }
        completeRevisionSession(topicId, due)
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
                    revisionCompletedDates = emptyList(),
                    revisionCompletionLog = emptyList(),
                    revisionScheduleType = null,
                    clientDateKey = today,
                )
            )
        }
    }

    override fun rescheduleMissedTopics(
        topicIds: List<String>,
        fitAll: Boolean,
        onResult: (TopicSchedulingResult) -> Unit,
    ) {
        if (topicIds.isEmpty()) return
        val state = _uiState.value
        val planId = state.selectedPlan?.id ?: return
        if (state.selectedPlan?.examDate.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Set an exam date first, then we can fit these back in.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null) }
            val result = repo.autoDistribute(
                planId,
                AutoDistributeRequest(
                    fromDate = todayKey(),
                    // Only the missed topics move; everything already placed on a
                    // future day keeps its date.
                    onlyTopicIds = topicIds,
                    lockExistingDates = false,
                    // Today's list must not change under the student mid-recovery.
                    preserveFromDate = true,
                    includeRevisionNeeded = false,
                    // Null = the server's honest default: respect the daily goal and
                    // report anything that still doesn't fit.
                    overloadMode = if (fitAll) "flex" else null,
                    strategy = state.preferredStudyStrategy,
                ),
            )
            when (result) {
                is Resource.Success -> {
                    val moved = result.data.assigned
                    val skipped = result.data.skipped
                    val message = when {
                        moved == 0 -> "Your study days are already full until the exam."
                        skipped == 0 && moved == 1 -> "Done — 1 topic now has a date."
                        skipped == 0 -> "Done — $moved topics now have dates."
                        else -> "$moved added. $skipped still need more time."
                    }
                    _uiState.update {
                        it.copy(
                            mutating = false,
                            selectedPlan = result.data.plan ?: it.selectedPlan,
                            message = message,
                        )
                    }
                    reloadSelected()
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                    onResult(TopicSchedulingResult(added = moved, notAdded = skipped))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(mutating = false, error = result.message) }
                    onResult(
                        TopicSchedulingResult(
                            added = 0,
                            notAdded = topicIds.size,
                            failed = true,
                        ),
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun autoDistribute(
        lockExisting: Boolean,
        overloadMode: String?,
        strategy: String?,
        preserveToday: Boolean,
    ) {
        if (_uiState.value.selectedPlan?.examDate.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Set an exam date before building the planner.") }
            return
        }
        val resolvedStrategy = strategy?.takeIf { it == "interleaved" || it == "sequential" }
        mutateAuto {
            repo.autoDistribute(
                it,
                AutoDistributeRequest(
                    fromDate = todayKey(),
                    includeRevisionNeeded = false,
                    lockExistingDates = lockExisting,
                    preserveFromDate = preserveToday,
                    overloadMode = overloadMode,
                    strategy = resolvedStrategy,
                ),
            )
        }
    }

    override fun armRebuild(
        strategy: String,
        prioritySubjectNames: List<String>,
        priorityOrderMode: String?,
    ) {
        _uiState.update {
            it.copy(
                pendingRebuild = PendingRebuild(
                    strategy = strategy,
                    prioritySubjectNames = prioritySubjectNames,
                    priorityOrderMode = priorityOrderMode,
                ),
            )
        }
    }

    override fun rescheduleAfterExamDateChange(
        strategy: String,
        overloadMode: String?,
        prioritySubjectNames: List<String>,
        priorityOrderMode: String?,
    ) {
        if (_uiState.value.selectedPlan?.examDate.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Set an exam date before rebuilding the planner.") }
            return
        }
        // "priority_split" needs at least one priority subject to mean anything;
        // otherwise fall back to the balanced interleaved layout (mirrors server).
        val resolvedStrategy = when (strategy) {
            "sequential" -> "sequential"
            "priority_split" -> if (prioritySubjectNames.isNotEmpty()) "priority_split" else "interleaved"
            else -> "interleaved"
        }
        // Keep the plan-tab "how you study" label in sync with the choice.
        setPreferredStudyStrategy(if (resolvedStrategy == "sequential") "sequential" else "interleaved")
        _uiState.update { it.copy(pendingRebuild = null) }
        mutateAuto {
            repo.autoDistribute(
                it,
                AutoDistributeRequest(
                    fromDate = todayKey(),
                    includeRevisionNeeded = false,
                    // Redistribute all unfinished, non-pinned topics into the new
                    // window; the engine skips completed topics and honours pinned
                    // (manually moved) ones automatically.
                    lockExistingDates = false,
                    preserveFromDate = true,
                    overloadMode = overloadMode,
                    strategy = resolvedStrategy,
                    prioritySubjectNames = prioritySubjectNames.takeIf { resolvedStrategy == "priority_split" },
                    priorityOrderMode = priorityOrderMode.takeIf { resolvedStrategy == "priority_split" },
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
            .filter { (it.topic.plannedDate?.take(10) ?: "") > today }
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

    override fun finishDay(topicIds: List<String>) {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return
        val topicIdsSet = topicIds.toSet()
        val originalDates = plan.flattenTopics()
            .mapNotNull { ref ->
                ref.topic.plannedDate
                    ?.takeIf { it.isNotBlank() && ref.topic.id in topicIdsSet }
                    ?.let { ref.topic.id to it }
            }
            .toMap()
        if (originalDates.isEmpty()) return

        _uiState.update {
            it.copy(
                mutating = true,
                error = null,
                finishDayUndo = null,
                deleteUndoToken = null,
                lastUndoableActionLabel = null,
            )
        }

        viewModelScope.launch {
            val dateKey = originalDates.values.first().take(10)
            when (val result = repo.finishDay(plan.id, FinishDayRequest(dateKey))) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = result.data.plan,
                            mutating = false,
                            message = "Done for the day. Remaining topics moved to Missed.",
                            finishDayUndo = result.data.undoToken?.let(::FinishDayUndoState),
                            deleteUndoToken = null,
                            lastUndoableActionLabel = null,
                        )
                    }
                    refreshCalendar(plan.id)
                    refreshAnalytics(plan.id)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        selectedPlan = plan,
                        mutating = false,
                        finishDayUndo = null,
                        error = result.message,
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun undoFinishDay() {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return
        val undo = state.finishDayUndo ?: return
        _uiState.update {
            it.copy(
                mutating = true,
                finishDayUndo = null,
                message = null,
                error = null,
            )
        }

        viewModelScope.launch {
            when (val result = repo.undoFinishDay(plan.id, UndoFinishDayRequest(undo.undoToken))) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = result.data.plan,
                            mutating = false,
                            message = "Today's tasks restored",
                        )
                    }
                    refreshCalendar(plan.id)
                    refreshAnalytics(plan.id)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        selectedPlan = plan,
                        mutating = false,
                        finishDayUndo = undo,
                        error = result.message,
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun resetPlan() {
        val plan = _uiState.value.selectedPlan ?: return
        val refs = plan.flattenTopics()
        if (refs.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true, error = null, message = null) }
            val request = BatchTopicUpdateRequest(
                updates = refs.map { ref ->
                    BatchTopicUpdateItem(
                        topicId = ref.topic.id,
                        patch = TopicPatchRequest(
                            status = TopicStatus.TODO,
                            plannedDate = "",
                            notes = ref.topic.notes,
                        ),
                    )
                },
            )
            when (val result = repo.batchUpdateTopics(plan.id, request)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedPlan = result.data,
                            mutating = false,
                            message = "Plan reset",
                            deleteUndoToken = result.data.undoToken?.takeIf(String::isNotBlank),
                            lastUndoableActionLabel = "Plan reset",
                        )
                    }
                    refreshCalendar(plan.id)
                    refreshAnalytics(plan.id)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(mutating = false, error = result.message ?: "Plan reset failed")
                }
                is Resource.Loading -> Unit
            }
        }
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
            _uiState.update { it.copy(mutating = true, error = null) }
            val request = BatchTopicUpdateRequest(
                listOf(
                    BatchTopicUpdateItem(currentTopicId, TopicPatchRequest(plannedDate = "")),
                    BatchTopicUpdateItem(replacementTopicId, TopicPatchRequest(plannedDate = todayDate)),
                ),
            )
            when (val result = repo.batchUpdateTopics(planId, request)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(selectedPlan = result.data, mutating = false, message = "Topic replaced")
                    }
                    refreshCalendar(planId)
                    refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = result.message) }
                is Resource.Loading -> Unit
            }
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

    override fun batchMarkTopicsDone(topicIds: List<String>) {
        if (topicIds.isEmpty()) return
        val request = BatchTopicUpdateRequest(
            updates = topicIds.map {
                BatchTopicUpdateItem(
                    topicId = it,
                    patch = TopicPatchRequest(status = TopicStatus.DONE, clientDateKey = todayKey())
                )
            },
        )
        mutateSelected(
            refreshCalendar = true,
            refreshAnalytics = true,
            successMessage = "${topicIds.size} topics marked as done",
            undoLabel = "Mark done"
        ) { planId -> repo.batchUpdateTopics(planId, request) }
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
                        topics = chapter.topics.map { topic ->
                            ImportSyllabusTopicRequest(
                                name = topic.name,
                                size = topic.size?.wireValue,
                            )
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
                val cleanup = repo.deletePlan(plan.id)
                val cleanupMessage = if (cleanup is Resource.Error) {
                    " The empty plan could not be removed automatically; it is now visible in Your Exams so you can delete or retry it."
                } else {
                    ""
                }
                _uiState.update {
                    it.copy(mutating = false, error = importResult.message.orEmpty() + cleanupMessage)
                }
                refreshPlans()
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
