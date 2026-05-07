package com.safar.app.ui.studyplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.remote.api.AutoDistributeRequest
import com.safar.app.data.remote.api.ChapterRequest
import com.safar.app.data.remote.api.CreateFromTemplateRequest
import com.safar.app.data.remote.api.CreatePlanRequest
import com.safar.app.data.remote.api.SubjectRequest
import com.safar.app.data.remote.api.TopicPatchRequest
import com.safar.app.data.remote.api.TopicRequest
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.domain.model.studyplanner.AutoDistributeResult
import com.safar.app.domain.model.studyplanner.CalendarMap
import com.safar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.PremiumReason
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.domain.repository.StudyPlannerRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyPlannerUiState(
    val plans: List<StudyPlan> = emptyList(),
    val templates: List<ExamTemplateSummary> = emptyList(),
    val selectedPlan: StudyPlan? = null,
    val calendar: CalendarMap = emptyMap(),
    val analytics: PlannerAnalytics? = null,
    val section: PlannerSection = PlannerSection.TODAY,
    val loading: Boolean = false,
    val mutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val premiumReason: PremiumReason? = null,
    val onboardingSkipped: Boolean = false,
)

@HiltViewModel
class StudyPlannerViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudyPlannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshPlans()
        loadTemplates()
    }

    fun setSection(section: PlannerSection) {
        _uiState.update { it.copy(section = section) }
    }

    fun clearTransient() {
        _uiState.update { it.copy(error = null, message = null, premiumReason = null) }
    }

    fun showPremium(reason: PremiumReason) {
        _uiState.update { it.copy(premiumReason = reason) }
    }

    fun requirePremium(reason: PremiumReason, action: () -> Unit) {
        if (_uiState.value.selectedPlan?.features?.isPremium == false) showPremium(reason) else action()
    }

    fun refreshPlans() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null) }
        when (val r = repo.listPlans()) {
            is Resource.Success -> _uiState.update { it.copy(plans = r.data, loading = false) }
            is Resource.Error -> _uiState.update { it.copy(error = r.message, loading = false) }
            is Resource.Loading -> Unit
        }
    }

    fun loadTemplates() = viewModelScope.launch {
        when (val r = repo.getTemplates()) {
            is Resource.Success -> _uiState.update { it.copy(templates = r.data) }
            is Resource.Error -> Unit
            is Resource.Loading -> Unit
        }
    }

    fun openPlan(planId: String) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null, section = PlannerSection.TODAY) }
        when (val r = repo.getPlan(planId)) {
            is Resource.Success -> {
                _uiState.update { it.copy(selectedPlan = r.data, loading = false) }
                refreshCalendar(planId)
                refreshAnalytics(planId)
            }
            is Resource.Error -> _uiState.update { it.copy(error = r.message, loading = false) }
            is Resource.Loading -> Unit
        }
    }

    fun closePlan() {
        _uiState.update { it.copy(selectedPlan = null, calendar = emptyMap(), analytics = null, section = PlannerSection.TODAY) }
        refreshPlans()
    }

    fun createPlan(title: String, examType: String?, examDate: String?, dailyGoal: Int, offDays: List<Int>) = viewModelScope.launch {
        mutatePlanList {
            repo.createPlan(CreatePlanRequest(title = title, examType = examType, examDate = examDate, dailyGoal = dailyGoal, offDays = offDays))
        }
    }

    fun createFromTemplate(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>) = viewModelScope.launch {
        mutatePlanList {
            repo.createPlanFromTemplate(CreateFromTemplateRequest(templateId = templateId, title = title, examDate = examDate, dailyGoal = dailyGoal, offDays = offDays, autoDistribute = false))
        }
    }

    fun deletePlan(planId: String) = viewModelScope.launch {
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

    fun updatePlan(request: UpdatePlanRequest) = mutateSelected { planId -> repo.updatePlan(planId, request) }
    fun addSubject(name: String) = mutateSelected { planId -> repo.addSubject(planId, SubjectRequest(name = name, color = "#0ea5e9")) }
    fun renameSubject(subjectId: String, name: String) = mutateSelected { planId -> repo.renameSubject(planId, subjectId, SubjectRequest(name = name)) }
    fun deleteSubject(subjectId: String) = mutateSelected { planId -> repo.deleteSubject(planId, subjectId) }
    fun addChapter(subjectId: String, name: String) = mutateSelected { planId -> repo.addChapter(planId, subjectId, ChapterRequest(name)) }
    fun renameChapter(subjectId: String, chapterId: String, name: String) = mutateSelected { planId -> repo.renameChapter(planId, subjectId, chapterId, ChapterRequest(name)) }
    fun deleteChapter(subjectId: String, chapterId: String) = mutateSelected { planId -> repo.deleteChapter(planId, subjectId, chapterId) }
    fun addTopic(subjectId: String, chapterId: String, name: String) = mutateSelected { planId -> repo.addTopic(planId, subjectId, chapterId, TopicRequest(name)) }
    fun updateTopic(topicId: String, status: TopicStatus? = null, name: String? = null, plannedDate: String? = null, notes: String? = null) =
        mutateSelected(refreshCalendar = true, refreshAnalytics = true) { planId -> repo.updateTopic(planId, topicId, TopicPatchRequest(name = name, status = status, plannedDate = plannedDate, notes = notes)) }
    fun deleteTopic(topicId: String) = mutateSelected(refreshCalendar = true, refreshAnalytics = true) { planId -> repo.deleteTopic(planId, topicId) }

    fun autoDistribute(includeRevision: Boolean, lockExisting: Boolean) {
        requirePremium(PremiumReason.AUTO_SCHEDULE) {
            mutateAuto {
                repo.autoDistribute(it, AutoDistributeRequest(includeRevisionNeeded = includeRevision, lockExistingDates = lockExisting))
            }
        }
    }

    fun clearFutureDates() {
        requirePremium(PremiumReason.RESCHEDULE) {
            val today = todayKey()
            val refs = _uiState.value.selectedPlan?.flattenTopics().orEmpty()
                .filter { (it.topic.plannedDate ?: "") >= today }
            batchTopicDates(refs.map { it.topic.id }, null, "Future dates cleared")
        }
    }

    fun resetPlan() {
        val refs = _uiState.value.selectedPlan?.flattenTopics().orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(mutating = true) }
            refs.forEach { repo.updateTopic(_uiState.value.selectedPlan?.id.orEmpty(), it.topic.id, TopicPatchRequest(status = TopicStatus.TODO, plannedDate = "", notes = it.topic.notes)) }
            reloadSelected("Plan reset")
        }
    }

    fun bulkAdd(subjectId: String, chapterId: String, text: String) {
        requirePremium(PremiumReason.BULK_ADD) {
            val topics = parseBulkSyllabus(text).flatMap { it.second }.filter { it.isNotBlank() }
            viewModelScope.launch {
                val planId = _uiState.value.selectedPlan?.id ?: return@launch
                _uiState.update { it.copy(mutating = true) }
                topics.forEach { repo.addTopic(planId, subjectId, chapterId, TopicRequest(it)) }
                reloadSelected("${topics.size} topics imported")
            }
        }
    }

    fun upgradePlan() = viewModelScope.launch {
        val planId = _uiState.value.selectedPlan?.id ?: return@launch
        _uiState.update { it.copy(mutating = true) }
        when (val r = repo.upgradePlan(planId)) {
            is Resource.Success -> {
                _uiState.update { it.copy(mutating = false, premiumReason = null, selectedPlan = r.data.plan ?: it.selectedPlan, message = r.data.message ?: "Premium unlocked") }
                reloadSelected()
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
    }

    private fun batchTopicDates(topicIds: List<String>, date: String?, message: String) = viewModelScope.launch {
        val planId = _uiState.value.selectedPlan?.id ?: return@launch
        _uiState.update { it.copy(mutating = true) }
        topicIds.forEach { repo.updateTopic(planId, it, TopicPatchRequest(plannedDate = date ?: "")) }
        reloadSelected(message)
    }

    private fun mutateAuto(call: suspend (String) -> Resource<AutoDistributeResult>) = viewModelScope.launch {
        val planId = _uiState.value.selectedPlan?.id ?: return@launch
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = call(planId)) {
            is Resource.Success -> {
                _uiState.update { it.copy(mutating = false, selectedPlan = r.data.plan ?: it.selectedPlan, message = "Assigned ${r.data.assigned}; skipped ${r.data.skipped}") }
                reloadSelected()
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
    }

    private fun mutateSelected(refreshCalendar: Boolean = false, refreshAnalytics: Boolean = false, call: suspend (String) -> Resource<StudyPlan>) {
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true, error = null) }
            when (val r = call(planId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedPlan = r.data, mutating = false, message = "Saved") }
                    if (refreshCalendar) refreshCalendar(planId)
                    if (refreshAnalytics) refreshAnalytics(planId)
                }
                is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private suspend fun mutatePlanList(call: suspend () -> Resource<StudyPlan>) {
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = call()) {
            is Resource.Success -> {
                _uiState.update { it.copy(mutating = false, message = "Plan created") }
                refreshPlans()
                openPlan(r.data.id)
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message) }
            is Resource.Loading -> Unit
        }
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
    }
}
