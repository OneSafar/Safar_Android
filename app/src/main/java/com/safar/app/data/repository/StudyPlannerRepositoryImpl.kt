package com.safar.app.data.repository

import com.safar.app.data.remote.api.AutoDistributeRequest
import com.safar.app.data.remote.api.ChapterRequest
import com.safar.app.data.remote.api.CreateFromTemplateRequest
import com.safar.app.data.remote.api.CreatePlanRequest
import com.safar.app.data.remote.api.PlannerApi
import com.safar.app.data.remote.api.SubjectRequest
import com.safar.app.data.remote.api.SyllabusImportResponse
import com.safar.app.data.remote.api.TopicPatchRequest
import com.safar.app.data.remote.api.TopicRequest
import com.safar.app.data.remote.api.UpdatePlanRequest
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
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
    override suspend fun updateTopic(planId: String, topicId: String, request: TopicPatchRequest): Resource<StudyPlan> = safeApiCall { api.updateTopic(planId, topicId, request) }
    override suspend fun deleteTopic(planId: String, topicId: String): Resource<StudyPlan> = safeApiCall { api.deleteTopic(planId, topicId) }
    /**
     * Mirrors the website `runBulkFileImport` flow in `StudyPlanner.tsx`:
     *  - On HTTP 200 with `success: true` → return the formatted syllabus code.
     *  - On HTTP 422 (agent-side validation failure) → still surface the partial
     *    `syllabusCode` so the user can fix it in the bulk-add editor, exactly like
     *    the website's `setBulkTopicsText(String(payload?.syllabusCode || ""))`.
     *  - On any other error → return a friendly Resource.Error.
     */
    override suspend fun importSyllabusFile(file: MultipartBody.Part): Resource<String> {
        return try {
            val response = api.importSyllabusFile(file)
            val rawBody: SyllabusImportResponse? = response.body() ?: parseErrorBody(response.errorBody()?.string())
            val code = response.code()

            when {
                response.isSuccessful && rawBody?.success != false -> {
                    Resource.Success(rawBody?.syllabusCode.orEmpty())
                }
                code == 422 && !rawBody?.syllabusCode.isNullOrBlank() -> {
                    // Agent produced output but it failed the strict `- _ >` validator.
                    // Hand the draft back so the user can correct it in the textarea.
                    Resource.Success(rawBody!!.syllabusCode.orEmpty())
                }
                else -> {
                    val message = rawBody?.message
                        ?: rawBody?.detail
                        ?: rawBody?.error
                        ?: rawBody?.errors?.joinToString("; ")
                        ?: "Could not import syllabus file."
                    Resource.Error(message, code)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            Resource.Error("The syllabus AI agent is taking longer than expected. Try again in a moment.")
        } catch (e: UnknownHostException) {
            Resource.Error("Could not reach SAFAR. Please check your internet connection.")
        } catch (e: EOFException) {
            Resource.Error("Connection to SAFAR was interrupted. Please try again.")
        } catch (e: IOException) {
            Resource.Error("Could not connect to SAFAR. Please try again.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Syllabus import failed.")
        }
    }

    private fun parseErrorBody(raw: String?): SyllabusImportResponse? {
        if (raw.isNullOrBlank()) return null
        return try {
            gson.fromJson(raw, SyllabusImportResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}

private inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, code)
    is Resource.Loading -> Resource.Loading()
}
