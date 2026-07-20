package com.safarparmar.app.ui.studyplanner.create

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.remote.api.ImportSyllabusChapterRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusSubjectRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusTopicRequest
import com.safarparmar.app.data.remote.api.PlanPreviewRequest
import com.safarparmar.app.data.remote.api.PlanPreviewResult
import com.safarparmar.app.data.remote.api.StructureSyllabusRequest
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview
import com.safarparmar.app.data.remote.api.TemplateExtraChapterRequest
import com.safarparmar.app.data.remote.api.TemplateExtraTopicRequest
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.DailyTodo
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
    val overloadMode: String = "flex",
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

    // Mixed Bag "difficult subjects" split — null means the user hasn't been asked
    // (or the picker hasn't run) yet; an empty list means they explicitly skipped
    // it; a non-empty list is the 2-3 subject names that should get topics every
    // day, with the rest rotating in on alternate days.
    val mixedBagPrioritySubjects: List<String>? = null,
)

@HiltViewModel
class CreatePlanViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState = _uiState.asStateFlow()
    private var templateDetailJob: Job? = null
    private val previewRenameVersions = mutableMapOf<String, Int>()

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
     * The 3 study-style cards map onto the 2 real backend knobs
     * (`strategy`: interleaved|sequential, `overloadMode`: flex|strict):
     * Deep Focus = sequential order, one subject at a time.
     * Mixed Bag = interleaved, subjects mixed across the week.
     * Balanced = interleaved + a strict daily cap, so the day-to-day load stays even
     * rather than letting some days stretch past the goal.
     */
    fun setStudyStyle(style: String) = _uiState.update {
        when (style) {
            "deep_focus" -> it.copy(strategy = "sequential", overloadMode = "flex")
            "balanced" -> it.copy(strategy = "interleaved", overloadMode = "strict")
            else -> it.copy(strategy = "interleaved", overloadMode = "flex") // mixed_bag
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
                            .map { (_, name) -> name } + state.templateExtraTopics[si to ci].orEmpty().map { it.name }
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
    fun currentSubjectNames(): List<String> = currentOutline().map { it.name }

    fun openMixedBagPicker() = _uiState.update { it.copy(step = CreatePlanStep.MixedBagSubjectPicker) }

    /** Confirms the chosen "difficult" subjects (topics from these land every day;
     *  the rest rotate in one-at-a-time on alternate days) and returns to settings. */
    fun setMixedBagPrioritySubjects(names: List<String>) {
        _uiState.update { it.copy(mixedBagPrioritySubjects = names, step = CreatePlanStep.PlanSettings) }
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
        val effectiveStrategy = if (state.strategy == "interleaved" && prioritySubjects != null) {
            "priority_split"
        } else {
            state.strategy
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
                    subjectOrder = subjectOrder,
                    chapterOrder = chapterOrder,
                    topicOrder = topicOrder,
                    prioritySubjects = prioritySubjects,
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = effectiveStrategy,
                    overloadMode = state.overloadMode,
                )
            }
            PlanSource.Manual -> PlanPreviewRequest(
                source = "manual",
                title = state.title.ifBlank { null },
                subjects = state.manualSubjects.toImportRequest(),
                subjectOrder = subjectOrder,
                chapterOrder = chapterOrder,
                topicOrder = topicOrder,
                prioritySubjects = prioritySubjects,
                examDate = state.examDate.ifBlank { null },
                dailyGoal = state.dailyGoal.toIntOrNull(),
                offDays = state.offDays.toList(),
                strategy = effectiveStrategy,
                overloadMode = state.overloadMode,
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
                    subjectOrder = subjectOrder,
                    chapterOrder = chapterOrder,
                    topicOrder = topicOrder,
                    prioritySubjects = prioritySubjects,
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = effectiveStrategy,
                    overloadMode = state.overloadMode,
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

    /** Discards the draft plan created by [buildPreview] without confirming it —
     *  called when the user backs out of Preview/BuildingPreview, so drafts don't
     *  accumulate silently. */
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

    fun renamePreviewTopic(topicId: String, newName: String) {
        val currentPreview = _uiState.value.previewResult ?: return
        val draftId = currentPreview.draftId
        val oldName = currentPreview.calendarPreview.values.asSequence().flatten()
            .firstOrNull { it.topicId == topicId }?.topicName ?: return
        val version = (previewRenameVersions[topicId] ?: 0) + 1
        previewRenameVersions[topicId] = version

        // Optimistically update the UI state
        val updatedCalendar = currentPreview.calendarPreview.mapValues { (_, topics) ->
            topics.map { if (it.topicId == topicId) it.copy(topicName = newName) else it }
        }
        val updatedPreview = currentPreview.copy(calendarPreview = updatedCalendar)
        _uiState.update { it.copy(previewResult = updatedPreview) }

        // Call the backend to update the topic in the draft plan
        viewModelScope.launch {
            val patch = com.safarparmar.app.data.remote.api.TopicPatchRequest(name = newName)
            when (val result = repo.updateTopic(draftId, topicId, patch)) {
                is Resource.Success -> Unit // Already updated optimistically
                is Resource.Error -> {
                    if (previewRenameVersions[topicId] == version) {
                        _uiState.update { state ->
                            val preview = state.previewResult ?: return@update state
                            state.copy(
                                previewResult = preview.copy(
                                    calendarPreview = preview.calendarPreview.mapValues { (_, topics) ->
                                        topics.map { if (it.topicId == topicId) it.copy(topicName = oldName) else it }
                                    },
                                ),
                                error = result.message,
                            )
                        }
                    }
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
