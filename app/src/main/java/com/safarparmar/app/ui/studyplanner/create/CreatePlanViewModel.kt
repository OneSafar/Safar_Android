package com.safarparmar.app.ui.studyplanner.create

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.remote.api.ImportSyllabusChapterRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusSubjectRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusTopicRequest
import com.safarparmar.app.data.remote.api.ChapterRatingRequest
import com.safarparmar.app.data.remote.api.PlanPreviewRequest
import com.safarparmar.app.data.remote.api.PlanPreviewResult
import com.safarparmar.app.data.remote.api.StructureSyllabusRequest
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview
import com.safarparmar.app.data.remote.api.TemplateExtraChapterRequest
import com.safarparmar.app.data.remote.api.TemplateExtraTopicRequest
import com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import com.safarparmar.app.data.remote.api.TopicPatchRequest
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class CreatePlanStep {
    ChoosePath,
    TemplatePicker,
    ManualTopicTree,
    PasteSyllabus,
    PlanSettings,
    ChapterRating,
    DeepFocusOrder,
    MixedBagSubjectPicker,
    BuildingPreview,
    Preview,
    DailyTopics,
}

enum class PlanSource { Template, Manual, Paste }

/** A chapter addressed on the template drill-down screens — either one that shipped
 *  with the template ([Original], addressed by its index for exclusion/extra-topic
 *  keys) or one the user added via the "+" ([Custom], addressed by local id). */
sealed interface TemplateChapterRef {
    data class Original(val index: Int) : TemplateChapterRef
    data class Custom(val localId: String) : TemplateChapterRef
}

/** A chapter in the "Deep Focus" reorder outline, with the topic names inside it in
 *  their current order. */
@Immutable
data class DeepFocusOutlineChapter(val name: String, val topicNames: List<String>)

/** A subject in the "Deep Focus" reorder outline — subjects, chapters, and topics are
 *  all orderable here, matching the live Syllabus screen's own drag-reorder exactly. */
@Immutable
data class DeepFocusOutlineSubject(val name: String, val chapters: List<DeepFocusOutlineChapter>)

/** A client-only, throwaway-if-abandoned topic. [localId] exists purely for
 *  Compose list keys / add-remove addressing — discarded once /preview
 *  returns real server ids. */
@Immutable
data class DraftTopic(val localId: String = UUID.randomUUID().toString(), val name: String)

@Immutable
data class DraftChapter(
    val localId: String = UUID.randomUUID().toString(),
    val name: String,
    val topics: List<DraftTopic> = emptyList(),
)

@Immutable
data class DraftSubject(
    val localId: String = UUID.randomUUID().toString(),
    val name: String,
    val chapters: List<DraftChapter> = emptyList(),
)

@Immutable
data class CreatePlanUiState(
    val step: CreatePlanStep = CreatePlanStep.ChoosePath,
    val source: PlanSource? = null,

    // Shared plan settings
    val title: String = "",
    val examType: String = "",
    val examDate: String = "",
    val offDays: Set<Int> = emptySet(),
    val strategy: String = "interleaved",
    /**
     * The study style the user actually picked. Previously this was reverse-
     * engineered from (strategy, overloadMode) — which meant Balanced and Mixed
     * Bag were told apart only by a hidden overload policy, and picking a style
     * silently also chose whether days could exceed the daily goal. Style now
     * means one thing: the order topics are studied in.
     */
    val studyStyle: String = "balanced",
    /**
     * Set only by [scheduleAnyway] — the user has seen that their syllabus does
     * not fit and has explicitly chosen to let days run past the daily goal so
     * everything gets a date. Reset whenever they go back to settings, so the
     * honest strict schedule is always what a fresh build produces.
     */
    val allowOverload: Boolean = false,
    /** False once the student picks "Same every day" — sent to the server, which
     *  then strips every weight so each day holds exactly [dailyGoal] topics. */
    val weightedPlanning: Boolean = true,
    val dailyGoal: String = "3",

    // Template path
    val templates: List<ExamTemplateSummary> = emptyList(),
    val loadingTemplates: Boolean = false,
    val templatesError: String? = null,
    val selectedTemplateId: String? = null,
    val templateDetail: ExamTemplate? = null,
    val loadingTemplateDetail: Boolean = false,
    /** [subjectIndex, chapterIndex, topicIndex] tuples the user unchecked. */
    val excludedTopicKeys: Set<Triple<Int, Int, Int>> = emptySet(),
    /** Original template subjects and chapters removed in the template editor. The
     *  backend exclusion contract is topic-based, so removal methods also add every
     *  descendant topic to [excludedTopicKeys]. */
    val excludedTemplateSubjectIndices: Set<Int> = emptySet(),
    val excludedTemplateChapterKeys: Set<Pair<Int, Int>> = emptySet(),
    /** Chapters the user appended to a template subject via the drill-down "+",
     *  keyed by the subject's index in [templateDetail]. */
    val templateExtraChapters: Map<Int, List<DraftChapter>> = emptyMap(),
    /** Topics the user appended to an existing template chapter via the drill-down
     *  "+", keyed by (subjectIndex, chapterIndex) in [templateDetail]. */
    val templateExtraTopics: Map<Pair<Int, Int>, List<DraftTopic>> = emptyMap(),
    /** Subjects the user appended to the template via the drill-down "+" on the subject list. */
    val templateExtraSubjects: List<DraftSubject> = emptyList(),
    /** Drill-down position within the template tree: null = subject list,
     *  [drillSubjectIndex] set = chapter list, both set = topic list. */
    val drillSubjectIndex: Int? = null,
    val drillChapter: TemplateChapterRef? = null,

    // Manual path — pure client-side, no backend calls until preview
    val manualSubjects: List<DraftSubject> = emptyList(),
    val manualValidationError: String? = null,

    // Paste path
    val pasteText: String = "",
    val isStructuring: Boolean = false,
    val structureError: String? = null,
    val structuredPreview: StructuredSyllabusPreview? = null,

    // Preview / confirm
    val isBuildingPreview: Boolean = false,
    val previewResult: PlanPreviewResult? = null,
    val previewError: String? = null,
    val isConfirming: Boolean = false,
    val confirmedPlanId: String? = null,
    val premiumRequired: Boolean = false,
    val error: String? = null,

    // Deep Focus drag-reorder — subject order, chapter order per subject (keyed by
    // subject name), and topic order per chapter (keyed by subject name + chapter
    // name, since chapter names aren't globally unique). Seeded from whichever
    // source is active the first time the user opens the reorder screen, then
    // preserved verbatim. [deepFocusDrillSubjectIndex] tracks which subject's
    // chapter list the user has drilled into on that screen (null = subject list).
    val deepFocusSubjectOrder: List<String>? = null,
    val deepFocusChapterOrder: Map<String, List<String>> = emptyMap(),
    val deepFocusTopicOrder: Map<Pair<String, String>, List<String>> = emptyMap(),
    val deepFocusDrillSubjectIndex: Int? = null,

    // Mixed Bag's "hardest subjects" — the 2-3 subjects scheduled exclusively
    // before every other subject. Ordered: the pick order drives the schedule
    // when [mixedBagOrderMode] is "sequential". Null means the user hasn't been
    // asked yet; an empty list means they explicitly skipped the split.
    val mixedBagPrioritySubjects: List<String>? = null,
    /** "sequential" (finish each chosen subject in pick order) or "balanced". */
    val mixedBagOrderMode: String = "sequential",

    // Optional "Rate your chapters" step (manual/paste sources only — templates
    // arrive pre-weighted). (subjectName, chapterName) -> "easy"|"normal"|"tough".
    val chapterRatings: Map<Pair<String, String>, String> = emptyMap(),
)

@HiltViewModel
class CreatePlanViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState = _uiState.asStateFlow()
    private var templateDetailJob: Job? = null

    fun clearError() = _uiState.update { it.copy(error = null, premiumRequired = false) }

    // ── Step navigation ──────────────────────────────────────────────

    fun goToStep(step: CreatePlanStep) = _uiState.update { it.copy(step = step) }

    fun chooseSource(source: PlanSource) {
        _uiState.update { it.copy(source = source) }
        when (source) {
            PlanSource.Template -> {
                goToStep(CreatePlanStep.TemplatePicker)
                if (_uiState.value.templates.isEmpty()) loadTemplates()
            }
            PlanSource.Manual -> goToStep(CreatePlanStep.ManualTopicTree)
            PlanSource.Paste -> goToStep(CreatePlanStep.PasteSyllabus)
        }
    }

    // ── Template path ────────────────────────────────────────────────

    private fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingTemplates = true, templatesError = null) }
            when (val r = repo.getTemplates()) {
                is Resource.Success -> _uiState.update {
                    it.copy(loadingTemplates = false, templates = r.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(loadingTemplates = false, templatesError = r.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun selectTemplate(templateId: String) {
        templateDetailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTemplateId = templateId,
                templateDetail = null,
                excludedTopicKeys = emptySet(),
                excludedTemplateSubjectIndices = emptySet(),
                excludedTemplateChapterKeys = emptySet(),
                templateExtraChapters = emptyMap(),
                templateExtraTopics = emptyMap(),
                templateExtraSubjects = emptyList(),
                drillSubjectIndex = null,
                drillChapter = null,
            )
        }
        templateDetailJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingTemplateDetail = true) }
            when (val r = repo.getTemplateDetail(templateId)) {
                is Resource.Success -> _uiState.update { state ->
                    if (state.selectedTemplateId != templateId) state else state.copy(
                        loadingTemplateDetail = false,
                        templateDetail = r.data,
                        title = state.title.ifBlank { r.data.name },
                        examType = state.examType.ifBlank { r.data.name },
                        dailyGoal = r.data.recommendedDailyGoal?.toString() ?: state.dailyGoal,
                        // Seed the rating chips from the template's own weights as
                        // soon as the detail lands. Without this, a detail that
                        // arrives after the user has already reached the rating
                        // step leaves every chip blank (openChapterRating only
                        // seeds once, at open). User edits still win via `+`.
                        chapterRatings = impliedTemplateRatings(r.data) + state.chapterRatings,
                    )
                }
                is Resource.Error -> _uiState.update { state ->
                    if (state.selectedTemplateId != templateId) state
                    else state.copy(loadingTemplateDetail = false, error = r.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearSelectedTemplate() {
        templateDetailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTemplateId = null,
                templateDetail = null,
                loadingTemplateDetail = false,
                excludedTopicKeys = emptySet(),
                excludedTemplateSubjectIndices = emptySet(),
                excludedTemplateChapterKeys = emptySet(),
                templateExtraChapters = emptyMap(),
                templateExtraTopics = emptyMap(),
                templateExtraSubjects = emptyList(),
                drillSubjectIndex = null,
                drillChapter = null,
            )
        }
    }

    fun drillIntoSubject(subjectIndex: Int) = _uiState.update { it.copy(drillSubjectIndex = subjectIndex, drillChapter = null) }
    fun drillIntoChapter(chapterRef: TemplateChapterRef) = _uiState.update { it.copy(drillChapter = chapterRef) }

    /** Pops one level of the template drill-down; used by both the top-bar back
     *  arrow and the hardware back button so they stay in sync. Returns true if a
     *  level was popped, false if already at the subject list (caller should then
     *  fall back to leaving the template entirely). */
    fun drillBack(): Boolean {
        val state = _uiState.value
        return when {
            state.drillChapter != null -> {
                _uiState.update { it.copy(drillChapter = null) }
                true
            }
            state.drillSubjectIndex != null -> {
                _uiState.update { it.copy(drillSubjectIndex = null) }
                true
            }
            else -> false
        }
    }

    fun toggleExcludedTopic(subjectIndex: Int, chapterIndex: Int, topicIndex: Int) {
        val key = Triple(subjectIndex, chapterIndex, topicIndex)
        _uiState.update { state ->
            val next = state.excludedTopicKeys.toMutableSet()
            if (!next.add(key)) next.remove(key)
            state.copy(excludedTopicKeys = next)
        }
    }

    fun removeOriginalTemplateSubject(subjectIndex: Int) {
        _uiState.update { state ->
            val subject = state.templateDetail?.subjects?.getOrNull(subjectIndex) ?: return@update state
            val topicKeys = subject.chapters.flatMapIndexed { chapterIndex, chapter ->
                chapter.topics.indices.map { topicIndex -> Triple(subjectIndex, chapterIndex, topicIndex) }
            }
            state.copy(
                excludedTemplateSubjectIndices = state.excludedTemplateSubjectIndices + subjectIndex,
                excludedTemplateChapterKeys = state.excludedTemplateChapterKeys +
                    subject.chapters.indices.map { subjectIndex to it },
                excludedTopicKeys = state.excludedTopicKeys + topicKeys,
                templateExtraChapters = state.templateExtraChapters - subjectIndex,
                templateExtraTopics = state.templateExtraTopics.filterKeys { it.first != subjectIndex },
            )
        }
    }

    fun removeOriginalTemplateChapter(subjectIndex: Int, chapterIndex: Int) {
        _uiState.update { state ->
            val chapter = state.templateDetail?.subjects?.getOrNull(subjectIndex)
                ?.chapters?.getOrNull(chapterIndex) ?: return@update state
            val topicKeys = chapter.topics.indices.map { topicIndex ->
                Triple(subjectIndex, chapterIndex, topicIndex)
            }
            val chapterKey = subjectIndex to chapterIndex
            state.copy(
                excludedTemplateChapterKeys = state.excludedTemplateChapterKeys + chapterKey,
                excludedTopicKeys = state.excludedTopicKeys + topicKeys,
                templateExtraTopics = state.templateExtraTopics - chapterKey,
            )
        }
    }

    /** Appends a brand-new chapter (drill-down "+" on a template subject's chapter list). */
    fun addTemplateChapter(subjectIndex: Int, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val existing = state.templateExtraChapters[subjectIndex].orEmpty()
            state.copy(
                templateExtraChapters = state.templateExtraChapters + (subjectIndex to (existing + DraftChapter(name = name.trim()))),
            )
        }
    }

    fun removeTemplateChapter(subjectIndex: Int, chapterLocalId: String) {
        _uiState.update { state ->
            val existing = state.templateExtraChapters[subjectIndex].orEmpty()
            state.copy(
                templateExtraChapters = state.templateExtraChapters + (subjectIndex to existing.filterNot { it.localId == chapterLocalId }),
            )
        }
    }

    /** Appends a topic to a brand-new chapter (added via [addTemplateChapter]). */
    fun addTemplateTopicToNewChapter(subjectIndex: Int, chapterLocalId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val existing = state.templateExtraChapters[subjectIndex].orEmpty()
            state.copy(
                templateExtraChapters = state.templateExtraChapters + (
                    subjectIndex to existing.map { chapter ->
                        if (chapter.localId != chapterLocalId) chapter
                        else chapter.copy(topics = chapter.topics + DraftTopic(name = name.trim()))
                    }
                ),
            )
        }
    }

    fun removeTemplateTopicFromNewChapter(subjectIndex: Int, chapterLocalId: String, topicLocalId: String) {
        _uiState.update { state ->
            val existing = state.templateExtraChapters[subjectIndex].orEmpty()
            state.copy(
                templateExtraChapters = state.templateExtraChapters + (
                    subjectIndex to existing.map { chapter ->
                        if (chapter.localId != chapterLocalId) chapter
                        else chapter.copy(topics = chapter.topics.filterNot { it.localId == topicLocalId })
                    }
                ),
            )
        }
    }

    /** Appends a topic to an existing template chapter (drill-down "+" on the topics screen). */
    fun addTemplateTopic(subjectIndex: Int, chapterIndex: Int, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val key = subjectIndex to chapterIndex
            val existing = state.templateExtraTopics[key].orEmpty()
            state.copy(templateExtraTopics = state.templateExtraTopics + (key to (existing + DraftTopic(name = name.trim()))))
        }
    }

    fun removeTemplateTopic(subjectIndex: Int, chapterIndex: Int, topicLocalId: String) {
        _uiState.update { state ->
            val key = subjectIndex to chapterIndex
            val existing = state.templateExtraTopics[key].orEmpty()
            state.copy(templateExtraTopics = state.templateExtraTopics + (key to existing.filterNot { it.localId == topicLocalId }))
        }
    }

    // ── Template path (custom subjects added by user) ───────────────────

    fun addTemplateSubject(name: String) {
        if (name.isBlank()) return
        _uiState.update { it.copy(templateExtraSubjects = it.templateExtraSubjects + DraftSubject(name = name.trim())) }
    }

    fun removeTemplateSubject(subjectLocalId: String) {
        _uiState.update { it.copy(templateExtraSubjects = it.templateExtraSubjects.filterNot { s -> s.localId == subjectLocalId }) }
    }

    fun addTemplateSubjectChapter(subjectLocalId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                templateExtraSubjects = state.templateExtraSubjects.map { subject ->
                    if (subject.localId != subjectLocalId) subject
                    else subject.copy(chapters = subject.chapters + DraftChapter(name = name.trim()))
                },
            )
        }
    }

    fun removeTemplateSubjectChapter(subjectLocalId: String, chapterLocalId: String) {
        _uiState.update { state ->
            state.copy(
                templateExtraSubjects = state.templateExtraSubjects.map { subject ->
                    if (subject.localId != subjectLocalId) subject
                    else subject.copy(chapters = subject.chapters.filterNot { it.localId == chapterLocalId })
                },
            )
        }
    }

    fun addTemplateSubjectTopic(subjectLocalId: String, chapterLocalId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                templateExtraSubjects = state.templateExtraSubjects.map { subject ->
                    if (subject.localId != subjectLocalId) subject
                    else subject.copy(
                        chapters = subject.chapters.map { chapter ->
                            if (chapter.localId != chapterLocalId) chapter
                            else chapter.copy(topics = chapter.topics + DraftTopic(name = name.trim()))
                        },
                    )
                },
            )
        }
    }

    fun removeTemplateSubjectTopic(subjectLocalId: String, chapterLocalId: String, topicLocalId: String) {
        _uiState.update { state ->
            state.copy(
                templateExtraSubjects = state.templateExtraSubjects.map { subject ->
                    if (subject.localId != subjectLocalId) subject
                    else subject.copy(
                        chapters = subject.chapters.map { chapter ->
                            if (chapter.localId != chapterLocalId) chapter
                            else chapter.copy(topics = chapter.topics.filterNot { it.localId == topicLocalId })
                        },
                    )
                },
            )
        }
    }

    // ── Manual path (pure client-side tree editing) ─────────────────

    fun addManualSubject(name: String) {
        if (name.isBlank()) return
        _uiState.update {
            it.copy(
                manualSubjects = it.manualSubjects + DraftSubject(name = name.trim()),
                manualValidationError = null,
            )
        }
    }

    fun removeManualSubject(subjectId: String) {
        _uiState.update {
            it.copy(
                manualSubjects = it.manualSubjects.filterNot { s -> s.localId == subjectId },
                manualValidationError = null,
            )
        }
    }

    fun addManualChapter(subjectId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                manualSubjects = state.manualSubjects.map { subject ->
                    if (subject.localId != subjectId) subject
                    else subject.copy(chapters = subject.chapters + DraftChapter(name = name.trim()))
                },
                manualValidationError = null,
            )
        }
    }

    fun removeManualChapter(subjectId: String, chapterId: String) {
        _uiState.update { state ->
            state.copy(
                manualSubjects = state.manualSubjects.map { subject ->
                    if (subject.localId != subjectId) subject
                    else subject.copy(chapters = subject.chapters.filterNot { it.localId == chapterId })
                },
                manualValidationError = null,
            )
        }
    }

    fun addManualTopic(subjectId: String, chapterId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                manualSubjects = state.manualSubjects.map { subject ->
                    if (subject.localId != subjectId) subject
                    else subject.copy(
                        chapters = subject.chapters.map { chapter ->
                            if (chapter.localId != chapterId) chapter
                            else chapter.copy(topics = chapter.topics + DraftTopic(name = name.trim()))
                        },
                    )
                },
                manualValidationError = null,
            )
        }
    }

    fun removeManualTopic(subjectId: String, chapterId: String, topicId: String) {
        _uiState.update { state ->
            state.copy(
                manualSubjects = state.manualSubjects.map { subject ->
                    if (subject.localId != subjectId) subject
                    else subject.copy(
                        chapters = subject.chapters.map { chapter ->
                            if (chapter.localId != chapterId) chapter
                            else chapter.copy(topics = chapter.topics.filterNot { it.localId == topicId })
                        },
                    )
                },
                manualValidationError = null,
            )
        }
    }

    fun continueFromManual() {
        val validationError = validateManualSubjects(_uiState.value.manualSubjects)
        if (validationError != null) {
            _uiState.update { it.copy(manualValidationError = validationError) }
            return
        }
        _uiState.update { it.copy(manualValidationError = null, step = CreatePlanStep.PlanSettings) }
    }

    // ── Paste path ───────────────────────────────────────────────────

    fun setPasteText(text: String) = _uiState.update { it.copy(pasteText = text) }
    fun clearStructureError() = _uiState.update { it.copy(structureError = null) }

    fun structurePaste() {
        val rawText = _uiState.value.pasteText
        if (rawText.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStructuring = true, structureError = null, structuredPreview = null) }
            val request = StructureSyllabusRequest(
                rawText = rawText,
                examType = _uiState.value.examType.ifBlank { null },
                planTitle = _uiState.value.title.ifBlank { null },
            )
            when (val r = repo.structureSyllabusPreview(request)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isStructuring = false, structuredPreview = r.data)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isStructuring = false, structureError = r.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Shared settings ──────────────────────────────────────────────

    fun setTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun setExamType(examType: String) = _uiState.update { it.copy(examType = examType) }
    fun setExamDate(examDate: String) = _uiState.update { it.copy(examDate = examDate) }
    fun setOffDays(offDays: Set<Int>) = _uiState.update { it.copy(offDays = offDays) }
    fun setStrategy(strategy: String) = _uiState.update { it.copy(strategy = strategy) }

    /**
     * The 3 study-style cards choose an ORDER, nothing else:
     *   Deep Focus — one subject at a time, in syllabus order (sequential).
     *   Mixed Bag  — your hardest subjects first, then the rest (priority_split,
     *                resolved in buildPreview once subjects have been picked).
     *   Balanced   — an even mix of every subject each day (interleaved).
     * All three respect the daily goal; nothing here decides whether days may
     * run over it.
     */
    fun setStudyStyle(style: String) = _uiState.update {
        when (style) {
            // Leaving Mixed Bag must drop its chosen subjects. buildPreview
            // upgrades "interleaved" to "priority_split" whenever they are
            // non-empty, so a user who tried Mixed Bag and then switched to
            // Balanced was silently still getting Mixed Bag scheduling.
            "deep_focus" -> it.copy(
                studyStyle = "deep_focus",
                strategy = "sequential",
                mixedBagPrioritySubjects = null,
            )
            "mixed_bag" -> it.copy(studyStyle = "mixed_bag", strategy = "interleaved")
            else -> it.copy(
                studyStyle = "balanced",
                strategy = "interleaved",
                mixedBagPrioritySubjects = null,
            )
        }
    }
    fun setDailyGoal(dailyGoal: String) = _uiState.update { it.copy(dailyGoal = dailyGoal.filter(Char::isDigit).take(2)) }

    // ── Deep Focus reorder ───────────────────────────────────────────

    /** The current subject/chapter/topic outline for whichever source is active,
     *  with empty chapters/subjects dropped — mirrors exactly what ends up in the
     *  built plan (template exclusions/extras applied, manual/paste as-is). */
    private fun currentOutline(): List<DeepFocusOutlineSubject> {
        val state = _uiState.value
        return when (state.source) {
            PlanSource.Template -> {
                val excluded = state.excludedTopicKeys
                val template = state.templateDetail ?: return emptyList()
                val originalAndExtraChapterSubjects = template.subjects.mapIndexedNotNull { si, subject ->
                    val originalChapters = subject.chapters.mapIndexedNotNull { ci, chapter ->
                        val topicNames = chapter.topics.withIndex()
                            .filter { (ti, _) -> Triple(si, ci, ti) !in excluded }
                            .map { (_, topic) -> topic.name } + state.templateExtraTopics[si to ci].orEmpty().map { it.name }
                        if (topicNames.isEmpty()) null else DeepFocusOutlineChapter(chapter.name, topicNames)
                    }
                    val extraChapters = state.templateExtraChapters[si].orEmpty()
                        .filter { it.topics.isNotEmpty() }
                        .map { DeepFocusOutlineChapter(it.name, it.topics.map { t -> t.name }) }
                    val chapters = originalChapters + extraChapters
                    if (chapters.isEmpty()) null else DeepFocusOutlineSubject(subject.name, chapters)
                }
                
                val extraSubjects = state.templateExtraSubjects.mapNotNull { subject ->
                    val chapters = subject.chapters.filter { it.topics.isNotEmpty() }
                        .map { DeepFocusOutlineChapter(it.name, it.topics.map { t -> t.name }) }
                    if (chapters.isEmpty()) null else DeepFocusOutlineSubject(subject.name, chapters)
                }
                
                originalAndExtraChapterSubjects + extraSubjects
            }
            PlanSource.Manual -> state.manualSubjects.mapNotNull { subject ->
                val chapters = subject.chapters.filter { it.topics.isNotEmpty() }
                    .map { DeepFocusOutlineChapter(it.name, it.topics.map { t -> t.name }) }
                if (chapters.isEmpty()) null else DeepFocusOutlineSubject(subject.name, chapters)
            }
            PlanSource.Paste -> state.structuredPreview?.subjects?.mapNotNull { subject ->
                val chapters = subject.chapters.filter { it.topics.isNotEmpty() }
                    .map { DeepFocusOutlineChapter(it.name, it.topics) }
                if (chapters.isEmpty()) null else DeepFocusOutlineSubject(subject.name, chapters)
            } ?: emptyList()
            null -> emptyList()
        }
    }

    /** Reconciles a freshly computed outline against whatever order the user
     *  already set: entries that still exist keep their position, brand-new
     *  ones are appended, removed ones are dropped — so revisiting the reorder
     *  screen after tweaking exclusions never silently discards the user's work
     *  or shows stale subjects/chapters/topics. */
    private fun mergeOutlineOrder(
        current: List<DeepFocusOutlineSubject>,
        prevSubjectOrder: List<String>?,
        prevChapterOrder: Map<String, List<String>>,
        prevTopicOrder: Map<Pair<String, String>, List<String>>,
    ): List<DeepFocusOutlineSubject> {
        val bySubjectName = current.associateBy { it.name }
        val orderedSubjectNames = (prevSubjectOrder.orEmpty().filter { it in bySubjectName }) +
            current.map { it.name }.filter { it !in prevSubjectOrder.orEmpty() }
        return orderedSubjectNames.distinct().map { subjectName ->
            val subject = bySubjectName.getValue(subjectName)
            val byChapterName = subject.chapters.associateBy { it.name }
            val prevChapters = prevChapterOrder[subjectName]
            val orderedChapterNames = if (prevChapters != null) {
                val currentSet = byChapterName.keys
                prevChapters.filter { it in currentSet } + subject.chapters.map { it.name }.filter { it !in prevChapters }
            } else {
                subject.chapters.map { it.name }
            }
            val chapters = orderedChapterNames.distinct().map { chapterName ->
                val chapter = byChapterName.getValue(chapterName)
                val prevTopics = prevTopicOrder[subjectName to chapterName]
                val topicNames = if (prevTopics != null) {
                    val currentTopicSet = chapter.topicNames.toSet()
                    prevTopics.filter { it in currentTopicSet } + chapter.topicNames.filter { it !in prevTopics }
                } else {
                    chapter.topicNames
                }
                DeepFocusOutlineChapter(chapterName, topicNames)
            }
            DeepFocusOutlineSubject(subjectName, chapters)
        }
    }

    /** Opens the Deep Focus reorder screen, seeding/reconciling its order from
     *  whatever subjects/chapters/topics are currently active, and resetting the
     *  drill-down position back to the subject list. */
    fun openDeepFocusOrder() {
        // Nothing to arrange yet (e.g. a template whose detail is still loading).
        // Opening anyway would store an EMPTY order, and deepFocusOutline() would
        // then keep returning empty even after the syllabus arrived — a blank
        // reorder screen the user could not recover from without backing out.
        if (currentOutline().isEmpty()) return
        _uiState.update { state ->
            val merged = mergeOutlineOrder(
                currentOutline(),
                state.deepFocusSubjectOrder,
                state.deepFocusChapterOrder,
                state.deepFocusTopicOrder,
            )
            state.copy(
                step = CreatePlanStep.DeepFocusOrder,
                deepFocusSubjectOrder = merged.map { it.name },
                deepFocusChapterOrder = merged.associate { it.name to it.chapters.map { c -> c.name } },
                deepFocusTopicOrder = merged.flatMap { s -> s.chapters.map { c -> (s.name to c.name) to c.topicNames } }.toMap(),
                deepFocusDrillSubjectIndex = null,
            )
        }
    }

    fun deepFocusOutline(): List<DeepFocusOutlineSubject> {
        val state = _uiState.value
        val order = state.deepFocusSubjectOrder ?: return currentOutline()
        return order.mapNotNull { subjectName ->
            val chapterNames = state.deepFocusChapterOrder[subjectName] ?: return@mapNotNull null
            val chapters = chapterNames.mapNotNull { chapterName ->
                state.deepFocusTopicOrder[subjectName to chapterName]?.let { DeepFocusOutlineChapter(chapterName, it) }
            }
            DeepFocusOutlineSubject(subjectName, chapters)
        }
    }

    fun drillIntoDeepFocusSubject(index: Int) = _uiState.update { it.copy(deepFocusDrillSubjectIndex = index) }

    /** Pops the drill-down back to the subject list. Returns true if there was a
     *  level to pop, false if already at the subject list (caller should then fall
     *  back to leaving the whole reorder screen). */
    fun drillBackDeepFocus(): Boolean {
        val state = _uiState.value
        return if (state.deepFocusDrillSubjectIndex != null) {
            _uiState.update { it.copy(deepFocusDrillSubjectIndex = null) }
            true
        } else {
            false
        }
    }

    fun moveDeepFocusSubject(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val order = state.deepFocusSubjectOrder ?: return@update state
            if (fromIndex !in order.indices || toIndex !in order.indices) return@update state
            state.copy(deepFocusSubjectOrder = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) })
        }
    }

    fun moveDeepFocusChapter(subjectName: String, fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val chapters = state.deepFocusChapterOrder[subjectName] ?: return@update state
            if (fromIndex !in chapters.indices || toIndex !in chapters.indices) return@update state
            val reordered = chapters.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            state.copy(deepFocusChapterOrder = state.deepFocusChapterOrder + (subjectName to reordered))
        }
    }

    fun moveDeepFocusTopic(subjectName: String, chapterName: String, fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val key = subjectName to chapterName
            val topics = state.deepFocusTopicOrder[key] ?: return@update state
            if (fromIndex !in topics.indices || toIndex !in topics.indices) return@update state
            val reordered = topics.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            state.copy(deepFocusTopicOrder = state.deepFocusTopicOrder + (key to reordered))
        }
    }

    // ── Mixed Bag "difficult subjects" picker ────────────────────────

    /** Subject names for whichever source is active — used to populate the "choose
     *  your 2-3 most difficult subjects" picker. */
    fun currentSubjectNames(): List<String> = orderedOutline().map { it.name }

    /**
     * [currentOutline] in the order the plan will ACTUALLY be built in.
     *
     * buildPreview sends deepFocusSubjectOrder / deepFocusChapterOrder to the
     * server, so once a student has dragged their syllabus the real schedule
     * follows that order — but every screen reading currentOutline() kept
     * showing the raw source order instead, so "Rate your chapters" and the
     * Mixed Bag picker listed subjects in an order the plan would never use.
     *
     * Sorted rather than rebuilt from the order maps: anything missing from
     * them (a subject added after the reorder screen was last opened) keeps its
     * place at the end instead of vanishing. This mirrors the server's own
     * sortByNameOrder, so client and schedule agree.
     */
    private fun orderedOutline(): List<DeepFocusOutlineSubject> {
        val state = _uiState.value
        val outline = currentOutline()
        val subjectRank = state.deepFocusSubjectOrder
            ?.withIndex()
            ?.associate { (index, name) -> name to index }
            .orEmpty()

        // Mixed Bag schedules the chosen "hardest" subjects exclusively first, so
        // that is the order the plan really runs in — the rating screen should
        // show the same, not the syllabus order those subjects came from.
        val priorityRank = state.mixedBagPrioritySubjects
            ?.withIndex()
            ?.associate { (index, name) -> name to index }
            .orEmpty()

        return outline
            .sortedWith(
                compareBy<DeepFocusOutlineSubject> { priorityRank[it.name] ?: Int.MAX_VALUE }
                    .thenBy { subjectRank[it.name] ?: Int.MAX_VALUE },
            )
            .map { subject ->
                val chapterRank = state.deepFocusChapterOrder[subject.name]
                    ?.withIndex()
                    ?.associate { (index, name) -> name to index }
                    ?: return@map subject
                subject.copy(
                    chapters = subject.chapters.sortedBy { chapterRank[it.name] ?: Int.MAX_VALUE },
                )
            }
    }

    fun openMixedBagPicker() = _uiState.update { it.copy(step = CreatePlanStep.MixedBagSubjectPicker) }

    /** Confirms the chosen "hardest" subjects — these are scheduled exclusively
     *  first, in [orderMode], and the remaining subjects only start once they run
     *  out — then returns to settings. */
    fun setMixedBagPrioritySubjects(names: List<String>, orderMode: String = "sequential") {
        _uiState.update {
            it.copy(
                mixedBagPrioritySubjects = names,
                mixedBagOrderMode = if (orderMode == "balanced") "balanced" else "sequential",
                step = CreatePlanStep.PlanSettings,
            )
        }
    }

    /** The alternate-day split is optional — skipping just falls back to the plain
     *  interleaved mix with no subject getting special daily treatment. */
    fun skipMixedBagSplit() {
        _uiState.update { it.copy(mixedBagPrioritySubjects = emptyList(), step = CreatePlanStep.PlanSettings) }
    }

    /** Total topic count already gathered for whichever source is active — used
     *  by PlanSettingsStep to show a "recommended daily goal" hint before preview. */
    fun currentTopicCount(): Int {
        val state = _uiState.value
        return when (state.source) {
            PlanSource.Template -> {
                val excluded = state.excludedTopicKeys
                val original = state.templateDetail?.subjects?.withIndex()?.sumOf { (si, subject) ->
                    subject.chapters.withIndex().sumOf { (ci, chapter) ->
                        chapter.topics.withIndex().count { (ti, _) -> Triple(si, ci, ti) !in excluded }
                    }
                } ?: 0
                val extraTopicsOnExisting = state.templateExtraTopics.values.sumOf { it.size }
                val extraChapterTopics = state.templateExtraChapters.values.sumOf { chapters -> chapters.sumOf { it.topics.size } }
                original + extraTopicsOnExisting + extraChapterTopics
            }
            PlanSource.Manual -> state.manualSubjects.sumOf { s -> s.chapters.sumOf { it.topics.size } }
            PlanSource.Paste -> state.structuredPreview?.subjects?.sumOf { s -> s.chapters.sumOf { it.topics.size } } ?: 0
            null -> 0
        }
    }

    // ── "Rate your chapters" step (manual/paste only) ────────────────

    /** Whether the optional chapter-rating step applies to the active source. */
    /**
     * Every source gets the rating step, templates included. Only 3 of the 20
     * bundled templates ship hand-weighted, so gating this to manual/paste left
     * SSC/UPSC/Railways students with no way to weight their syllabus until
     * *after* the plan was built — forcing them to rate chapters in the Syllabus
     * screen and rebuild, throwing away the schedule they just approved.
     */
    fun canRateChapters(): Boolean = _uiState.value.source != null

    /**
     * Opens the rating step, pre-filling it from whatever weights the source
     * already carries. A JEE student sees Physics chapters already marked Tough
     * and adjusts what they disagree with, instead of starting from blank.
     * Manual/paste syllabi carry no weights, so they start empty.
     */
    fun openChapterRating() = _uiState.update { state ->
        val outline = orderedOutline()
        val templateImplied = impliedTemplateRatings(state.templateDetail)
        val merged = templateImplied + state.chapterRatings
        val completeRatings = outline.flatMap { subject ->
            subject.chapters.map { chapter ->
                val key = subject.name to chapter.name
                key to (merged[key] ?: ChapterDifficulty.NORMAL.wireValue)
            }
        }.toMap()

        state.copy(
            step = CreatePlanStep.ChapterRating,
            weightedPlanning = true,
            chapterRatings = completeRatings,
        )
    }

    /**
     * The (subject, chapter) → difficulty map implied by a template's own
     * hand-weighted topics. Shared by [openChapterRating] and the template-detail
     * load so the rating chips pre-fill regardless of which happens first.
     */
    private fun impliedTemplateRatings(
        template: ExamTemplate?,
    ): Map<Pair<String, String>, String> =
        template
            ?.subjects
            ?.flatMap { subject ->
                subject.chapters.mapNotNull { chapter ->
                    chapter.impliedDifficulty?.let { difficulty ->
                        (subject.name to chapter.name) to difficulty.wireValue
                    }
                }
            }
            ?.toMap()
            .orEmpty()

    /** Subject/chapter outline shown by the rating step. */
    fun ratingOutline(): List<DeepFocusOutlineSubject> = orderedOutline()

    /**
     * Routes the Plan Settings "continue" through the optional chapter-rating
     * step for manual/paste sources; template plans arrive pre-weighted and go
     * straight to preview.
     */
    fun continueFromSettings() {
        _uiState.update { it.copy(allowOverload = false) }
        if (canRateChapters() && currentOutline().isNotEmpty()) openChapterRating() else buildPreview()
    }

    fun setChapterRating(subjectName: String, chapterName: String, difficulty: String?) {
        _uiState.update { state ->
            val key = subjectName to chapterName
            val targetDifficulty = difficulty ?: ChapterDifficulty.NORMAL.wireValue
            state.copy(
                chapterRatings = state.chapterRatings + (key to targetDifficulty),
            )
        }
    }

    /**
     * "Same number every day": every chapter is marked Normal, which makes the
     * server drop the template's per-topic sizes, so every topic costs the same.
     * The day budget is goal x 2 and a normal topic is 2, so each day lands on
     * EXACTLY the student's number — no heavier days, nothing to interpret.
     *
     * This is the honest counterpart to the weighted plan: the points system is
     * never shown either way, the student just picks predictable or smart.
     */
    fun buildEvenPlan() {
        // Ratings are dropped entirely rather than all set to "normal": the plan
        // must end up with NO difficulty and NO topic size, so the Syllabus screen
        // can tell it is an equal-days plan and hide the rating chips.
        _uiState.update { it.copy(weightedPlanning = false, chapterRatings = emptyMap()) }
        buildPreview()
    }

    // ── Preview / confirm ────────────────────────────────────────────

    fun buildPreview() {
        val state = _uiState.value
        val source = state.source ?: return
        if (source == PlanSource.Manual) {
            val validationError = validateManualSubjects(state.manualSubjects)
            if (validationError != null) {
                _uiState.update {
                    it.copy(
                        manualValidationError = validationError,
                        previewError = null,
                        step = CreatePlanStep.ManualTopicTree,
                    )
                }
                return
            }
        }
        val subjectOrder = state.deepFocusSubjectOrder
        val chapterOrder = state.deepFocusChapterOrder.ifEmpty { null }
        // Nest (subjectName, chapterName) -> topics into subjectName -> chapterName ->
        // topics for the wire format — a flat Pair-keyed map isn't JSON-friendly.
        val topicOrder = state.deepFocusTopicOrder.entries
            .groupBy({ it.key.first }, { it.key.second to it.value })
            .mapValues { (_, pairs) -> pairs.toMap() }
            .ifEmpty { null }
        // A non-empty priority-subject split upgrades "interleaved" (Mixed Bag) into
        // the dedicated "priority_split" strategy; every other style (deep focus's
        // sequential, balanced's plain interleaved) is unaffected.
        val prioritySubjects = state.mixedBagPrioritySubjects?.takeIf { it.isNotEmpty() }
        // Only meaningful alongside a priority split.
        val priorityOrderMode = prioritySubjects?.let { state.mixedBagOrderMode }
        val effectiveStrategy = if (state.strategy == "interleaved" && prioritySubjects != null) {
            "priority_split"
        } else {
            state.strategy
        }
        val overloadMode = if (state.allowOverload) "flex" else null
        val chapterRatings = state.chapterRatings.takeIf { it.isNotEmpty() }?.map { (key, difficulty) ->
            ChapterRatingRequest(subject = key.first, chapter = key.second, difficulty = difficulty)
        }
        val request = when (source) {
            PlanSource.Template -> {
                val templateId = state.selectedTemplateId ?: return
                PlanPreviewRequest(
                    source = "template",
                    title = state.title.ifBlank { null },
                    templateId = templateId,
                    excludeTopicKeys = state.excludedTopicKeys.map { (s, c, t) -> listOf(s, c, t) },
                    extraChapters = state.templateExtraChapters.flatMap { (subjectIndex, chapters) ->
                        chapters.map { chapter ->
                            TemplateExtraChapterRequest(
                                subjectIndex = subjectIndex,
                                name = chapter.name,
                                topics = chapter.topics.map { it.name },
                            )
                        }
                    },
                    extraTopics = state.templateExtraTopics.flatMap { (key, topics) ->
                        topics.map { topic ->
                            TemplateExtraTopicRequest(subjectIndex = key.first, chapterIndex = key.second, name = topic.name)
                        }
                    },
                    chapterRatings = chapterRatings,
                    weightedPlanning = state.weightedPlanning,
                    subjectOrder = subjectOrder,
                    chapterOrder = chapterOrder,
                    topicOrder = topicOrder,
                    prioritySubjects = prioritySubjects,
                    priorityOrderMode = priorityOrderMode,
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = effectiveStrategy,
                    overloadMode = overloadMode,
                )
            }
            PlanSource.Manual -> PlanPreviewRequest(
                source = "manual",
                title = state.title.ifBlank { null },
                subjects = state.manualSubjects.toImportRequest(),
                chapterRatings = chapterRatings,
                weightedPlanning = state.weightedPlanning,
                subjectOrder = subjectOrder,
                chapterOrder = chapterOrder,
                topicOrder = topicOrder,
                prioritySubjects = prioritySubjects,
                    priorityOrderMode = priorityOrderMode,
                examDate = state.examDate.ifBlank { null },
                dailyGoal = state.dailyGoal.toIntOrNull(),
                offDays = state.offDays.toList(),
                strategy = effectiveStrategy,
                overloadMode = overloadMode,
            )
            PlanSource.Paste -> {
                val preview = state.structuredPreview ?: return
                PlanPreviewRequest(
                    source = "paste",
                    title = state.title.ifBlank { null },
                    subjects = preview.subjects.map { subject ->
                        ImportSyllabusSubjectRequest(
                            name = subject.name,
                            chapters = subject.chapters.map { chapter ->
                                ImportSyllabusChapterRequest(
                                    name = chapter.name,
                                    topics = chapter.topics.map { ImportSyllabusTopicRequest(it) },
                                )
                            },
                        )
                    },
                    chapterRatings = chapterRatings,
                    weightedPlanning = state.weightedPlanning,
                    subjectOrder = subjectOrder,
                    chapterOrder = chapterOrder,
                    topicOrder = topicOrder,
                    prioritySubjects = prioritySubjects,
                    priorityOrderMode = priorityOrderMode,
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = effectiveStrategy,
                    overloadMode = overloadMode,
                )
            }
        }

        goToStep(CreatePlanStep.BuildingPreview)
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildingPreview = true, previewError = null) }
            when (val r = repo.previewPlan(request)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isBuildingPreview = false, previewResult = r.data, step = CreatePlanStep.Preview)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(
                        isBuildingPreview = false,
                        previewError = r.message,
                        premiumRequired = r.errorCode == "PREMIUM_REQUIRED" || r.code == 403,
                        step = CreatePlanStep.PlanSettings,
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * "Schedule it anyway" from the preview's doesn't-fit card: rebuilds the same
     * plan with the daily budget allowed to stretch, so every topic gets a date
     * even though days will run fuller than the goal. This is the ONLY route to
     * flex scheduling — it is an informed choice made once, in front of the real
     * numbers, rather than a setting chosen blind at plan creation.
     *
     * The existing draft is discarded first; [buildPreview] inserts a new one,
     * and without this the abandoned draft would linger server-side.
     */
    fun scheduleAnyway() {
        discardDraft()
        _uiState.update { it.copy(allowOverload = true) }
        buildPreview()
    }

    /** Discards the draft plan created by [buildPreview] without confirming it —
     *  called when the user backs out of Preview/BuildingPreview, so drafts don't
     *  accumulate silently. */
    /**
     * Renames a topic on the draft plan straight from the review screen.
     *
     * The old pen icon was dropped because the row had no room for it; the entry
     * point is now a long-press on the topic. The draft is a real server-side plan,
     * so this PATCHes it and patches the local preview in place — rebuilding the
     * preview would mint a whole new draft and throw away the schedule under review.
     */
    fun renameDraftTopic(topicId: String, newName: String) {
        val cleaned = newName.trim()
        if (cleaned.isBlank()) return
        val state = _uiState.value
        val draftId = state.previewResult?.draftId ?: return
        val current = state.previewResult
        // Optimistic: the calendar the review screen renders is local, so patch it
        // now and let the request settle behind the UI.
        _uiState.update { st ->
            val preview = st.previewResult ?: return@update st
            val patched = preview.calendarPreview.mapValues { (_, items) ->
                items.map { if (it.topicId == topicId) it.copy(topicName = cleaned) else it }
            }
            st.copy(previewResult = preview.copy(calendarPreview = patched))
        }
        viewModelScope.launch {
            val result = repo.updateTopic(draftId, topicId, TopicPatchRequest(name = cleaned))
            if (result is Resource.Error) {
                // Put the old name back rather than showing a rename that never saved.
                _uiState.update { st ->
                    val preview = st.previewResult ?: return@update st
                    val reverted = preview.calendarPreview.mapValues { (_, items) ->
                        items.map { item ->
                            if (item.topicId == topicId) {
                                val old = current.calendarPreview.values.flatten()
                                    .firstOrNull { it.topicId == topicId }?.topicName ?: item.topicName
                                item.copy(topicName = old)
                            } else item
                        }
                    }
                    st.copy(
                        previewResult = preview.copy(calendarPreview = reverted),
                        error = result.message,
                    )
                }
            }
        }
    }

    fun discardDraft() {
        val draftId = _uiState.value.previewResult?.draftId ?: return
        _uiState.update { it.copy(previewResult = null, isConfirming = false) }
        viewModelScope.launch { repo.deletePlan(draftId) }
    }

    fun confirmPlan() {
        val draftId = _uiState.value.previewResult?.draftId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirming = true, error = null) }
            when (val r = repo.confirmPlan(draftId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isConfirming = false, confirmedPlanId = r.data.id)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isConfirming = false, error = r.message ?: "Plan confirmation failed")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /** Saves the optional daily routine on the draft, then confirms it. Keeping both
     * operations here guarantees the ready animation is only shown after the topics
     * have been persisted. */
    fun finishDailyTopics(dailyTodos: List<DailyTodo>) {
        val draftId = _uiState.value.previewResult?.draftId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirming = true, error = null) }
            if (dailyTodos.isNotEmpty()) {
                when (val update = repo.updatePlan(draftId, UpdatePlanRequest(dailyTodos = dailyTodos))) {
                    is Resource.Error -> {
                        _uiState.update { it.copy(isConfirming = false, error = update.message) }
                        return@launch
                    }
                    else -> Unit
                }
            }
            when (val result = repo.confirmPlan(draftId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isConfirming = false, confirmedPlanId = result.data.id)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isConfirming = false, error = result.message ?: "Plan confirmation failed")
                }
                is Resource.Loading -> Unit
            }
        }
    }

}

internal fun validateManualSubjects(subjects: List<DraftSubject>): String? {
    if (subjects.isEmpty()) return "Add at least one subject, chapter, and topic."

    fun duplicateName(names: List<String>): String? = names
        .groupBy { it.trim().lowercase() }
        .entries
        .firstOrNull { (normalized, matches) -> normalized.isNotEmpty() && matches.size > 1 }
        ?.value
        ?.first()
        ?.trim()

    duplicateName(subjects.map { it.name })?.let { return "Subject names must be unique: \"$it\" is repeated." }

    subjects.forEach { subject ->
        if (subject.chapters.isEmpty()) return "Add a chapter to ${subject.name}."
        duplicateName(subject.chapters.map { it.name })?.let {
            return "Chapter names in ${subject.name} must be unique: \"$it\" is repeated."
        }
        subject.chapters.forEach { chapter ->
            if (chapter.topics.isEmpty()) return "Add a topic to ${subject.name} › ${chapter.name}."
            duplicateName(chapter.topics.map { it.name })?.let {
                return "Topic names in ${chapter.name} must be unique: \"$it\" is repeated."
            }
        }
    }
    return null
}

internal fun List<DraftSubject>.toImportRequest(): List<ImportSyllabusSubjectRequest> = mapNotNull { subject ->
    val chapters = subject.chapters.mapNotNull { chapter ->
        val topics = chapter.topics.mapNotNull { topic ->
            topic.name.trim().takeIf { it.isNotEmpty() }?.let(::ImportSyllabusTopicRequest)
        }
        if (chapter.name.isBlank() || topics.isEmpty()) null else ImportSyllabusChapterRequest(
            name = chapter.name.trim(),
            topics = topics,
        )
    }
    if (subject.name.isBlank() || chapters.isEmpty()) null else
    ImportSyllabusSubjectRequest(
        name = subject.name.trim(),
        chapters = chapters,
    )
}
