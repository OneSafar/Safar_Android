package com.safarparmar.app.data.repository

import com.safarparmar.app.data.remote.api.AutoDistributeRequest
import com.safarparmar.app.data.remote.api.BulkImportResponse
import com.safarparmar.app.data.remote.api.BulkTopicsRequest
import com.safarparmar.app.data.remote.api.ChapterRequest
import com.safarparmar.app.data.remote.api.CreateFromTemplateRequest
import com.safarparmar.app.data.remote.api.CreatePlanRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusChapterRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusSubjectRequest
import com.safarparmar.app.data.remote.api.ImportSyllabusTopicRequest
import com.safarparmar.app.data.remote.api.PlannerApi
import com.safarparmar.app.data.remote.api.StructureSyllabusRequest
import com.safarparmar.app.data.remote.api.StructureSyllabusResponse
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview
import com.safarparmar.app.data.remote.api.SubjectRequest
import com.safarparmar.app.data.remote.api.SyllabusAiRequest
import com.safarparmar.app.data.remote.api.SyllabusApi
import com.safarparmar.app.data.remote.api.SyllabusImportResponse
import com.safarparmar.app.data.remote.api.SyllabusStats
import com.safarparmar.app.data.remote.api.TopicPatchRequest
import com.safarparmar.app.data.remote.api.TopicRequest
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.data.remote.api.toPreview
import com.safarparmar.app.data.remote.api.toSyllabusAiPreview
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.UpgradePlannerResult
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.ui.studyplanner.logic.BulkSubjectParsed
import com.safarparmar.app.ui.studyplanner.logic.countBulkSubjectsChapters
import com.safarparmar.app.ui.studyplanner.logic.countBulkSubjectsTopics
import com.safarparmar.app.ui.studyplanner.logic.parseBulkSubjectsFromTxt
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.parseApiErrorBody
import com.safarparmar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class StudyPlannerRepositoryImpl @Inject constructor(
    private val api: PlannerApi,
    private val syllabusApi: SyllabusApi,
) : StudyPlannerRepository {
    private val gson = Gson()

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
    override suspend fun bulkTopics(
        planId: String,
        subjectId: String,
        chapterId: String,
        request: BulkTopicsRequest,
    ): Resource<StudyPlan> = safeApiCall { api.bulkTopics(planId, subjectId, chapterId, request) }
    override suspend fun importSyllabus(planId: String, request: ImportSyllabusRequest): Resource<StudyPlan> =
        safeApiCall { api.importSyllabus(planId, request) }
    override suspend fun updateTopic(planId: String, topicId: String, request: TopicPatchRequest): Resource<StudyPlan> = safeApiCall { api.updateTopic(planId, topicId, request) }
    override suspend fun deleteTopic(planId: String, topicId: String): Resource<StudyPlan> = safeApiCall { api.deleteTopic(planId, topicId) }

    override suspend fun bulkImportSyllabus(planId: String, text: String): Resource<BulkImportResponse> {
        val parsed = parseBulkSubjectsFromTxt(text).getOrElse {
            return Resource.Error(it.message ?: "Invalid syllabus format.")
        }
        if (parsed.isEmpty()) return Resource.Error("No syllabus content found.")
        val request = buildImportRequestFromParsed(parsed)
        if (request.subjects.isEmpty()) return Resource.Error("No syllabus content found.")

        val subjectCount = request.subjects.size
        val chapterCount = countBulkSubjectsChapters(parsed)
        val topicCount = countBulkSubjectsTopics(parsed)

        return when (val result = importSyllabus(planId, request)) {
            is Resource.Success -> Resource.Success(
                BulkImportResponse(
                    success = true,
                    subjects = subjectCount,
                    chapters = chapterCount,
                    topics = topicCount,
                    subjectsCreated = subjectCount,
                    chaptersCreated = chapterCount,
                    topicsCreated = topicCount,
                    message = "Imported $subjectCount subjects, $chapterCount chapters, $topicCount topics",
                ),
            )
            is Resource.Error -> Resource.Error(result.message, result.code, result.errorCode)
            is Resource.Loading -> Resource.Error("Import interrupted.")
        }
    }

    override suspend fun structureSyllabusPreview(request: StructureSyllabusRequest): Resource<StructuredSyllabusPreview> {
        return try {
            val response = syllabusApi.structureSyllabusPreview(request)
            val body = response.body()
            when {
                response.isSuccessful && body != null -> {
                    val preview = body.toPreview()
                    if (preview != null && (preview.subjects.isNotEmpty() || body.success)) {
                        Resource.Success(preview)
                    } else {
                        Resource.Error(
                            messageForSyllabusError(body.errorCode, body.message ?: "We could not organize this syllabus."),
                            response.code(),
                            body.errorCode,
                        )
                    }
                }
                else -> {
                    val err = parseErrorBody(response.errorBody()?.string(), StructureSyllabusResponse::class.java)
                        ?: parseErrorBody(response.errorBody()?.string(), SyllabusImportResponse::class.java)
                    Resource.Error(
                        messageForSyllabusError(err?.errorCode, err?.message ?: "We could not organize this syllabus."),
                        response.code(),
                        err?.errorCode,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            Resource.Error("Organizing your syllabus is taking longer than expected. Try again.")
        } catch (e: UnknownHostException) {
            Resource.Error("Could not reach SAFAR. Please check your internet connection.")
        } catch (e: IOException) {
            Resource.Error("Could not connect to SAFAR. Please try again.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Syllabus organization failed.")
        }
    }

    override suspend fun applySyllabusAi(planId: String, preview: StructuredSyllabusPreview): Resource<StudyPlan> {
        return when (val r = safeApiCall {
            api.applySyllabusAi(planId, SyllabusAiRequest(aiPreview = preview.toSyllabusAiPreview()))
        }) {
            is Resource.Success -> {
                val plan = r.data.plan
                if (plan != null) Resource.Success(plan)
                else Resource.Error(r.data.message ?: "Syllabus import failed.")
            }
            is Resource.Error -> Resource.Error(r.message, r.code, r.errorCode)
            is Resource.Loading -> Resource.Error("Syllabus import failed.")
        }
    }

    private fun buildImportRequestFromParsed(groups: List<BulkSubjectParsed>): ImportSyllabusRequest {
        val subjects = groups.mapNotNull { subject ->
            val subjectName = subject.subjectName.trim()
            if (subjectName.isBlank()) return@mapNotNull null
            val chapters = subject.chapters.mapNotNull { chapter ->
                val chapterName = chapter.chapterName.trim()
                if (chapterName.isBlank()) return@mapNotNull null
                val topics = chapter.topics
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { ImportSyllabusTopicRequest(name = it) }
                if (topics.isEmpty()) return@mapNotNull null
                ImportSyllabusChapterRequest(name = chapterName, topics = topics)
            }
            if (chapters.isEmpty()) return@mapNotNull null
            ImportSyllabusSubjectRequest(name = subjectName, chapters = chapters)
        }

        return ImportSyllabusRequest(subjects = subjects, mode = "replace")
    }

    private fun parseErrorBody(raw: String?): SyllabusImportResponse? {
        if (raw.isNullOrBlank()) return null
        return try {
            gson.fromJson(raw, SyllabusImportResponse::class.java)
        } catch (e: JsonSyntaxException) {
            val parsed = parseApiErrorBody(raw)
            SyllabusImportResponse(message = parsed.message, error = parsed.error, errorCode = parsed.code)
        }
    }

    private fun <T> parseErrorBody(raw: String?, klass: Class<T>): StructureSyllabusResponse? {
        if (raw.isNullOrBlank()) return null
        return try {
            gson.fromJson(raw, klass) as? StructureSyllabusResponse
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private fun messageForSyllabusError(errorCode: String?, fallback: String): String {
        return when (errorCode) {
            "INPUT_TOO_LARGE" -> "This syllabus is too large. Shorten it and try again."
            "TOPIC_LIMIT_EXCEEDED", "TOPIC_LIMIT" -> "This syllabus exceeds your current topic limit."
            "RATE_LIMITED" -> "You have reached today's AI syllabus limit. Try again later."
            "SYLLABUS_PARSE_FAILED" -> "We could not organize this syllabus. Try editing it or use manual format."
            else -> fallback
        }
    }
}

private inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, code, errorCode)
    is Resource.Loading -> Resource.Loading()
}
