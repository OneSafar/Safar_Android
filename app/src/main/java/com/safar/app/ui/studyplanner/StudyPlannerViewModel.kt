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
import android.content.Context
import com.safar.app.notifications.SafarNotificationManager
import com.safar.app.notifications.SafarNotificationChannels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.util.Locale
import javax.inject.Inject

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
    val premiumReason: PremiumReason? = null,
    val onboardingSkipped: Boolean = false,
    val syllabusImportDraft: String = "",
    val syllabusImportFileName: String? = null,
)

@HiltViewModel
class StudyPlannerViewModel @Inject constructor(
    private val repo: StudyPlannerRepository,
    @ApplicationContext private val context: Context,
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

    fun clearSyllabusImportDraft() {
        _uiState.update { it.copy(syllabusImportDraft = "", syllabusImportFileName = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
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
        _uiState.update { it.copy(loading = true, error = null, section = PlannerSection.PLAN) }
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
        _uiState.update { it.copy(selectedPlan = null, calendar = emptyMap(), analytics = null, section = PlannerSection.PLAN) }
        refreshPlans()
    }

    fun createPlan(title: String, examType: String?, examDate: String?, dailyGoal: Int, offDays: List<Int>) = viewModelScope.launch {
        mutatePlanList {
            repo.createPlan(CreatePlanRequest(title = title, examType = examType, examDate = examDate, dailyGoal = dailyGoal, offDays = offDays))
        }
    }

    fun createFromTemplate(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>) = viewModelScope.launch {
        tryCreatePlanFromTemplateWithLocalFallback(templateId, title, examDate, dailyGoal, offDays)
    }

    fun createFromTemplateOrLocal(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>) = viewModelScope.launch {
        val serverHas = _uiState.value.templates.any { it.id == templateId }
        if (serverHas) {
            tryCreatePlanFromTemplateWithLocalFallback(templateId, title, examDate, dailyGoal, offDays)
        } else {
            createPlanFromLocalTemplate(templateId, title, examDate, dailyGoal, offDays)
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
    fun updateTopic(topicId: String, status: TopicStatus? = null, name: String? = null, plannedDate: String? = null, notes: String? = null) {
        val wasDone = _uiState.value.calendar.values.flatten().find { it.topicId == topicId }?.status == TopicStatus.DONE
        mutateSelected(refreshCalendar = true, refreshAnalytics = true, onSuccess = {
            if (status == TopicStatus.DONE && !wasDone) {
                checkDailyMilestones()
            }
        }) { planId -> repo.updateTopic(planId, topicId, TopicPatchRequest(name = name, status = status, plannedDate = plannedDate, notes = notes)) }
    }
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

    fun moveTopicsToDate(topicIds: List<String>, date: String) {
        if (topicIds.isEmpty()) return
        batchTopicDates(topicIds, date, "Topics moved")
    }

    fun clearTopicDates(topicIds: List<String>) {
        if (topicIds.isEmpty()) return
        batchTopicDates(topicIds, null, "Day cleared")
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
    }

    /** Full syllabus (`-` / `_` / `>`) before manual subjects — matches web bulk TXT import. */
    fun importFullSyllabusFromTxt(text: String) {
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
            _uiState.update { it.copy(mutating = true, error = null) }
            var plan = _uiState.value.selectedPlan ?: return@launch
            var colorIdx = 0
            var totalTopicCount = 0
            var totalChapterCount = 0
            for (group in groups) {
                val subjectKey = group.subjectName.lowercase(Locale.US)
                var subject = plan.subjects.find { it.name.lowercase(Locale.US) == subjectKey }
                if (subject == null) {
                    val color = bulkSubjectPalette[colorIdx % bulkSubjectPalette.size]
                    colorIdx++
                    when (val sr = repo.addSubject(planId, SubjectRequest(name = group.subjectName, color = color))) {
                        is Resource.Success -> {
                            plan = sr.data
                            subject = plan.subjects.find { it.name.lowercase(Locale.US) == subjectKey }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(mutating = false, error = sr.message) }
                            return@launch
                        }
                        is Resource.Loading -> Unit
                    }
                }
                val subjectId = subject?.id ?: continue
                for (ch in group.chapters) {
                    val chapterKey = ch.chapterName.lowercase(Locale.US)
                    var chapter = plan.subjects.find { it.id == subjectId }
                        ?.chapters
                        ?.find { it.name.lowercase(Locale.US) == chapterKey }
                    if (chapter == null) {
                        when (val cr = repo.addChapter(planId, subjectId, ChapterRequest(ch.chapterName))) {
                            is Resource.Success -> {
                                plan = cr.data
                                chapter = plan.subjects.find { it.id == subjectId }
                                    ?.chapters
                                    ?.find { it.name.lowercase(Locale.US) == chapterKey }
                            }
                            is Resource.Error -> {
                                _uiState.update { it.copy(mutating = false, error = cr.message) }
                                return@launch
                            }
                            is Resource.Loading -> Unit
                        }
                    }
                    val chapterId = chapter?.id ?: continue
                    totalChapterCount++
                    for (topicName in ch.topics) {
                        when (val tr = repo.addTopic(planId, subjectId, chapterId, TopicRequest(topicName))) {
                            is Resource.Success -> {
                                plan = tr.data
                                totalTopicCount++
                            }
                            is Resource.Error -> {
                                _uiState.update { it.copy(mutating = false, error = tr.message) }
                                return@launch
                            }
                            is Resource.Loading -> Unit
                        }
                    }
                }
            }
            val message = if (totalTopicCount > 0) {
                "Imported $totalTopicCount topics across $totalChapterCount chapters"
            } else {
                "Imported $totalChapterCount empty chapters"
            }
            reloadSelected(message)
        }
    }

    fun importSyllabusFile(file: MultipartBody.Part, fileName: String?) = viewModelScope.launch {
        _uiState.update { it.copy(mutating = true, error = null) }
        when (val r = repo.importSyllabusFile(file)) {
            is Resource.Success -> {
                val draft = r.data.trim()
                _uiState.update {
                    it.copy(
                        mutating = false,
                        syllabusImportDraft = draft,
                        syllabusImportFileName = fileName,
                        message = if (draft.isNotBlank()) "Imported syllabus from ${fileName ?: "file"}." else "Syllabus import finished.",
                    )
                }
            }
            is Resource.Error -> _uiState.update { it.copy(mutating = false, error = r.message, syllabusImportFileName = fileName) }
            is Resource.Loading -> Unit
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

    private fun mutateSelected(refreshCalendar: Boolean = false, refreshAnalytics: Boolean = false, onSuccess: (suspend () -> Unit)? = null, call: suspend (String) -> Resource<StudyPlan>) {
        viewModelScope.launch {
            val planId = _uiState.value.selectedPlan?.id ?: return@launch
            _uiState.update { it.copy(mutating = true, error = null) }
            when (val r = call(planId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedPlan = r.data, mutating = false, message = "Saved") }
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
                    )
                }
                hydratePlanFromServerBestEffort(r.data.id)
            }
            is Resource.Error -> {
                if (r.code == 429 && getLocalExamTemplate(templateId) != null) {
                    createPlanFromLocalTemplate(
                        templateId,
                        title,
                        examDate,
                        dailyGoal,
                        offDays,
                        successMessage = "Plan created from saved template (server busy — try again online later).",
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

    private suspend fun createPlanFromLocalTemplate(
        templateId: String,
        title: String,
        examDate: String?,
        dailyGoal: Int,
        offDays: List<Int>,
        successMessage: String = "Plan created",
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

        val planId = plan.id
        for (subject in template.subjects) {
            val subjectPlan = when (val sr = repo.addSubject(planId, SubjectRequest(name = subject.name, color = subject.color))) {
                is Resource.Success -> sr.data
                is Resource.Error -> {
                    _uiState.update { it.copy(mutating = false, error = sr.message) }
                    return
                }
                is Resource.Loading -> return
            }

            val subjectId = subjectPlan.subjects.find { it.name == subject.name }?.id ?: continue
            for (chapter in subject.chapters) {
                val chapterPlan = when (val cr = repo.addChapter(planId, subjectId, ChapterRequest(chapter.name))) {
                    is Resource.Success -> cr.data
                    is Resource.Error -> {
                        _uiState.update { it.copy(mutating = false, error = cr.message) }
                        return
                    }
                    is Resource.Loading -> return
                }

                val chapterId = chapterPlan.subjects.find { it.id == subjectId }
                    ?.chapters?.find { it.name == chapter.name }?.id ?: continue
                for (topic in chapter.topics) {
                    when (val tr = repo.addTopic(planId, subjectId, chapterId, TopicRequest(topic))) {
                        is Resource.Success -> Unit
                        is Resource.Error -> {
                            _uiState.update { it.copy(mutating = false, error = tr.message) }
                            return
                        }
                        is Resource.Loading -> return
                    }
                }
            }
        }

        _uiState.update { it.copy(mutating = false, message = successMessage) }
        refreshPlans()
        openPlan(planId)
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

    private suspend fun checkDailyMilestones() {
        val today = todayKey()
        val calendar = _uiState.value.calendar
        val todayTopics = calendar[today] ?: return
        
        val total = todayTopics.size
        if (total == 0) return
        
        val doneCount = todayTopics.count { it.status == TopicStatus.DONE }
        val half = (total + 1) / 2
        
        val planTitle = _uiState.value.selectedPlan?.title ?: "Study Plan"
        val notificationManager = SafarNotificationManager(context)
        
        if (doneCount == total || doneCount == half) {
            notificationManager.show(
                title = planTitle,
                body = "You've got this, keep going !",
                channelId = SafarNotificationChannels.STUDY_REMINDERS,
                deepLink = "safar://studyplanner"
            )
        }
    }
}
