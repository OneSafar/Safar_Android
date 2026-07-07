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
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreatePlanStep {
    ChoosePath,
    TemplatePicker,
    ManualTopicTree,
    PasteSyllabus,
    PlanSettings,
    BuildingPreview,
    Preview,
}

enum class PlanSource { Template, Manual, Paste }

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

    // Manual path — pure client-side, no backend calls until preview
    val manualSubjects: List<DraftSubject> = emptyList(),

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
)

@HiltViewModel
class CreatePlanViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState = _uiState.asStateFlow()

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
        _uiState.update {
            it.copy(
                selectedTemplateId = templateId,
                templateDetail = null,
                excludedTopicKeys = emptySet(),
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingTemplateDetail = true) }
            when (val r = repo.getTemplateDetail(templateId)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        loadingTemplateDetail = false,
                        templateDetail = r.data,
                        title = it.title.ifBlank { r.data.name },
                        examType = it.examType.ifBlank { r.data.name },
                        dailyGoal = r.data.recommendedDailyGoal?.toString() ?: it.dailyGoal,
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(loadingTemplateDetail = false, error = r.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearSelectedTemplate() {
        _uiState.update { it.copy(selectedTemplateId = null, templateDetail = null, excludedTopicKeys = emptySet()) }
    }

    fun toggleExcludedTopic(subjectIndex: Int, chapterIndex: Int, topicIndex: Int) {
        val key = Triple(subjectIndex, chapterIndex, topicIndex)
        _uiState.update { state ->
            val next = state.excludedTopicKeys.toMutableSet()
            if (!next.add(key)) next.remove(key)
            state.copy(excludedTopicKeys = next)
        }
    }

    // ── Manual path (pure client-side tree editing) ─────────────────

    fun addManualSubject(name: String) {
        if (name.isBlank()) return
        _uiState.update { it.copy(manualSubjects = it.manualSubjects + DraftSubject(name = name.trim())) }
    }

    fun removeManualSubject(subjectId: String) {
        _uiState.update { it.copy(manualSubjects = it.manualSubjects.filterNot { s -> s.localId == subjectId }) }
    }

    fun addManualChapter(subjectId: String, name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            state.copy(
                manualSubjects = state.manualSubjects.map { subject ->
                    if (subject.localId != subjectId) subject
                    else subject.copy(chapters = subject.chapters + DraftChapter(name = name.trim()))
                },
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
            )
        }
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

    /** Total topic count already gathered for whichever source is active — used
     *  by PlanSettingsStep to show a "recommended daily goal" hint before preview. */
    fun currentTopicCount(): Int {
        val state = _uiState.value
        return when (state.source) {
            PlanSource.Template -> {
                val excluded = state.excludedTopicKeys
                state.templateDetail?.subjects?.withIndex()?.sumOf { (si, subject) ->
                    subject.chapters.withIndex().sumOf { (ci, chapter) ->
                        chapter.topics.withIndex().count { (ti, _) -> Triple(si, ci, ti) !in excluded }
                    }
                } ?: 0
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
        val request = when (source) {
            PlanSource.Template -> {
                val templateId = state.selectedTemplateId ?: return
                PlanPreviewRequest(
                    source = "template",
                    title = state.title.ifBlank { null },
                    templateId = templateId,
                    excludeTopicKeys = state.excludedTopicKeys.map { (s, c, t) -> listOf(s, c, t) },
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = state.strategy,
                    overloadMode = state.overloadMode,
                )
            }
            PlanSource.Manual -> PlanPreviewRequest(
                source = "manual",
                title = state.title.ifBlank { null },
                subjects = state.manualSubjects.toImportRequest(),
                examDate = state.examDate.ifBlank { null },
                dailyGoal = state.dailyGoal.toIntOrNull(),
                offDays = state.offDays.toList(),
                strategy = state.strategy,
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
                    examDate = state.examDate.ifBlank { null },
                    dailyGoal = state.dailyGoal.toIntOrNull(),
                    offDays = state.offDays.toList(),
                    strategy = state.strategy,
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
                is Resource.Error -> {
                    if (r.code == 409 || r.errorCode == "CONFLICT") {
                        // Likely already confirmed by an earlier tap that raced this one —
                        // treat as success rather than surfacing a scary error.
                        _uiState.update { it.copy(isConfirming = false, confirmedPlanId = draftId) }
                    } else {
                        _uiState.update { it.copy(isConfirming = false, error = r.message) }
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }
}

private fun List<DraftSubject>.toImportRequest(): List<ImportSyllabusSubjectRequest> = map { subject ->
    ImportSyllabusSubjectRequest(
        name = subject.name,
        chapters = subject.chapters.map { chapter ->
            ImportSyllabusChapterRequest(
                name = chapter.name,
                topics = chapter.topics.map { ImportSyllabusTopicRequest(it.name) },
            )
        },
    )
}
