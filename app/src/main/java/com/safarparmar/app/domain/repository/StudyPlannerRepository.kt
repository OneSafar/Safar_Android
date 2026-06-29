package com.safarparmar.app.domain.repository

import com.safarparmar.app.data.remote.api.AutoDistributeRequest
import com.safarparmar.app.data.remote.api.ChapterRequest
import com.safarparmar.app.data.remote.api.CreateFromTemplateRequest
import com.safarparmar.app.data.remote.api.CreatePlanRequest
import com.safarparmar.app.data.remote.api.SubjectRequest
import com.safarparmar.app.data.remote.api.BulkImportResponse
import com.safarparmar.app.data.remote.api.BulkTopicsRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusRequest
import com.safarparmar.app.data.remote.api.StructureSyllabusRequest
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview
import com.safarparmar.app.data.remote.api.TopicPatchRequest
import com.safarparmar.app.data.remote.api.TopicRequest
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.UpgradePlannerResult
import com.safarparmar.app.util.Resource
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
    suspend fun bulkTopics(
        planId: String,
        subjectId: String,
        chapterId: String,
        request: BulkTopicsRequest,
    ): Resource<StudyPlan>
    suspend fun importSyllabus(planId: String, request: ImportSyllabusRequest): Resource<StudyPlan>
    suspend fun updateTopic(planId: String, topicId: String, request: TopicPatchRequest): Resource<StudyPlan>
    suspend fun deleteTopic(planId: String, topicId: String): Resource<StudyPlan>
    suspend fun bulkImportSyllabus(planId: String, text: String): Resource<BulkImportResponse>
    suspend fun importManualSyllabus(planId: String, text: String, mode: String): Resource<StudyPlan>
    suspend fun structureSyllabusPreview(request: StructureSyllabusRequest): Resource<StructuredSyllabusPreview>
    suspend fun applySyllabusAi(planId: String, preview: StructuredSyllabusPreview): Resource<StudyPlan>
}
