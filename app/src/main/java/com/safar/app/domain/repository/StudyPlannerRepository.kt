package com.safar.app.domain.repository

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
import com.safar.app.domain.model.studyplanner.ExamTemplate
import com.safar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.UpgradePlannerResult
import com.safar.app.util.Resource
import okhttp3.MultipartBody

interface StudyPlannerRepository {
    suspend fun listPlans(): Resource<List<StudyPlan>>
    suspend fun createPlan(request: CreatePlanRequest): Resource<StudyPlan>
    suspend fun deletePlan(planId: String): Resource<Unit>
    suspend fun getTemplates(): Resource<List<ExamTemplateSummary>>
    suspend fun getTemplateDetail(templateId: String): Resource<ExamTemplate>
    suspend fun createPlanFromTemplate(request: CreateFromTemplateRequest): Resource<StudyPlan>
    suspend fun getPlan(planId: String): Resource<StudyPlan>
    suspend fun updatePlan(planId: String, request: UpdatePlanRequest): Resource<StudyPlan>
    suspend fun upgradePlan(planId: String): Resource<UpgradePlannerResult>
    suspend fun getCalendar(planId: String): Resource<CalendarMap>
    suspend fun getAnalytics(planId: String): Resource<PlannerAnalytics>
    suspend fun autoDistribute(planId: String, request: AutoDistributeRequest): Resource<AutoDistributeResult>
    suspend fun addSubject(planId: String, request: SubjectRequest): Resource<StudyPlan>
    suspend fun renameSubject(planId: String, subjectId: String, request: SubjectRequest): Resource<StudyPlan>
    suspend fun deleteSubject(planId: String, subjectId: String): Resource<StudyPlan>
    suspend fun addChapter(planId: String, subjectId: String, request: ChapterRequest): Resource<StudyPlan>
    suspend fun renameChapter(planId: String, subjectId: String, chapterId: String, request: ChapterRequest): Resource<StudyPlan>
    suspend fun deleteChapter(planId: String, subjectId: String, chapterId: String): Resource<StudyPlan>
    suspend fun addTopic(planId: String, subjectId: String, chapterId: String, request: TopicRequest): Resource<StudyPlan>
    suspend fun updateTopic(planId: String, topicId: String, request: TopicPatchRequest): Resource<StudyPlan>
    suspend fun deleteTopic(planId: String, topicId: String): Resource<StudyPlan>
    suspend fun importSyllabusFile(file: MultipartBody.Part): Resource<String>
}
