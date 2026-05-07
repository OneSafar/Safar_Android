package com.safar.app.data.repository

import com.safar.app.data.remote.api.AutoDistributeRequest
import com.safar.app.data.remote.api.ChapterRequest
import com.safar.app.data.remote.api.CreateFromTemplateRequest
import com.safar.app.data.remote.api.CreatePlanRequest
import com.safar.app.data.remote.api.PlannerApi
import com.safar.app.data.remote.api.SubjectRequest
import com.safar.app.data.remote.api.TopicPatchRequest
import com.safar.app.data.remote.api.TopicRequest
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.domain.model.studyplanner.AutoDistributeResult
import com.safar.app.domain.model.studyplanner.CalendarMap
import com.safar.app.domain.model.studyplanner.ExamTemplate
import com.safar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.UpgradePlannerResult
import com.safar.app.domain.repository.StudyPlannerRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody

@Singleton
class StudyPlannerRepositoryImpl @Inject constructor(
    private val api: PlannerApi,
) : StudyPlannerRepository {
    override suspend fun listPlans(): Resource<List<StudyPlan>> = safeApiCall { api.listPlans() }
    override suspend fun createPlan(request: CreatePlanRequest): Resource<StudyPlan> = safeApiCall { api.createPlan(request) }
    override suspend fun deletePlan(planId: String): Resource<Unit> = safeApiCall { api.deletePlan(planId) }.map { }
    override suspend fun getTemplates(): Resource<List<ExamTemplateSummary>> = safeApiCall { api.getTemplates() }
    override suspend fun getTemplateDetail(templateId: String): Resource<ExamTemplate> = safeApiCall { api.getTemplateDetail(templateId) }
    override suspend fun createPlanFromTemplate(request: CreateFromTemplateRequest): Resource<StudyPlan> = safeApiCall { api.createPlanFromTemplate(request) }
    override suspend fun getPlan(planId: String): Resource<StudyPlan> = safeApiCall { api.getPlan(planId) }
    override suspend fun updatePlan(planId: String, request: UpdatePlanRequest): Resource<StudyPlan> = safeApiCall { api.updatePlan(planId, request) }
    override suspend fun upgradePlan(planId: String): Resource<UpgradePlannerResult> = safeApiCall { api.upgradePlan(planId) }
    override suspend fun getCalendar(planId: String): Resource<CalendarMap> = safeApiCall { api.getCalendar(planId) }
    override suspend fun getAnalytics(planId: String): Resource<PlannerAnalytics> = safeApiCall { api.getAnalytics(planId) }
    override suspend fun autoDistribute(planId: String, request: AutoDistributeRequest): Resource<AutoDistributeResult> = safeApiCall { api.autoDistribute(planId, request) }
    override suspend fun addSubject(planId: String, request: SubjectRequest): Resource<StudyPlan> = safeApiCall { api.addSubject(planId, request) }
    override suspend fun renameSubject(planId: String, subjectId: String, request: SubjectRequest): Resource<StudyPlan> = safeApiCall { api.renameSubject(planId, subjectId, request) }
    override suspend fun deleteSubject(planId: String, subjectId: String): Resource<StudyPlan> = safeApiCall { api.deleteSubject(planId, subjectId) }
    override suspend fun addChapter(planId: String, subjectId: String, request: ChapterRequest): Resource<StudyPlan> = safeApiCall { api.addChapter(planId, subjectId, request) }
    override suspend fun renameChapter(planId: String, subjectId: String, chapterId: String, request: ChapterRequest): Resource<StudyPlan> = safeApiCall { api.renameChapter(planId, subjectId, chapterId, request) }
    override suspend fun deleteChapter(planId: String, subjectId: String, chapterId: String): Resource<StudyPlan> = safeApiCall { api.deleteChapter(planId, subjectId, chapterId) }
    override suspend fun addTopic(planId: String, subjectId: String, chapterId: String, request: TopicRequest): Resource<StudyPlan> = safeApiCall { api.addTopic(planId, subjectId, chapterId, request) }
    override suspend fun updateTopic(planId: String, topicId: String, request: TopicPatchRequest): Resource<StudyPlan> = safeApiCall { api.updateTopic(planId, topicId, request) }
    override suspend fun deleteTopic(planId: String, topicId: String): Resource<StudyPlan> = safeApiCall { api.deleteTopic(planId, topicId) }
    override suspend fun importSyllabusFile(file: MultipartBody.Part): Resource<String> = safeApiCall { api.importSyllabusFile(file) }.map { it.syllabusCode.orEmpty() }
}

private inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, code)
    is Resource.Loading -> Resource.Loading()
}
