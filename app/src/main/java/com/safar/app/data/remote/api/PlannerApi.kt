package com.safar.app.data.remote.api

import com.safar.app.domain.model.studyplanner.AutoDistributeResult
import com.safar.app.domain.model.studyplanner.CalendarMap
import com.safar.app.domain.model.studyplanner.ExamTemplate
import com.safar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safar.app.domain.model.studyplanner.PlannerAnalytics
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.domain.model.studyplanner.UpgradePlannerResult
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PlannerApi {
    @GET("plans")
    suspend fun listPlans(): Response<List<StudyPlan>>

    @POST("plans")
    suspend fun createPlan(@Body request: CreatePlanRequest): Response<StudyPlan>

    @DELETE("plans/{planId}")
    suspend fun deletePlan(@Path("planId") planId: String): Response<BasicPlannerResponse>

    // Catalog of bundled exam templates is essentially immutable per app
    // version. The X-Cache-Max-Age header is consumed by an OkHttp network
    // interceptor (see NetworkModule) which rewrites the response with a
    // matching `Cache-Control: max-age=...` so OkHttp's on-disk cache can
    // serve repeat reads. Re-opening the "Create plan" screen during the
    // same session becomes free, and cold opens after a recent visit avoid
    // a network round-trip.
    @Headers("X-Cache-Max-Age: 300")
    @GET("plans/templates")
    suspend fun getTemplates(): Response<List<ExamTemplateSummary>>

    @Headers("X-Cache-Max-Age: 300")
    @GET("plans/templates/{templateId}")
    suspend fun getTemplateDetail(@Path("templateId") templateId: String): Response<ExamTemplate>

    @POST("plans/from-template")
    suspend fun createPlanFromTemplate(@Body request: CreateFromTemplateRequest): Response<StudyPlan>

    @GET("plans/{planId}")
    suspend fun getPlan(@Path("planId") planId: String): Response<StudyPlan>

    @PATCH("plans/{planId}")
    suspend fun updatePlan(
        @Path("planId") planId: String,
        @Body request: UpdatePlanRequest,
    ): Response<StudyPlan>

    @POST("plans/{planId}/upgrade")
    suspend fun upgradePlan(@Path("planId") planId: String): Response<UpgradePlannerResult>

    @GET("plans/{planId}/calendar")
    suspend fun getCalendar(@Path("planId") planId: String): Response<CalendarMap>

    @GET("plans/{planId}/analytics")
    suspend fun getAnalytics(@Path("planId") planId: String): Response<PlannerAnalytics>

    @POST("plans/{planId}/auto-distribute")
    suspend fun autoDistribute(
        @Path("planId") planId: String,
        @Body request: AutoDistributeRequest,
    ): Response<AutoDistributeResult>

    @POST("plans/{planId}/subjects")
    suspend fun addSubject(
        @Path("planId") planId: String,
        @Body request: SubjectRequest,
    ): Response<StudyPlan>

    @PATCH("plans/{planId}/subjects/{subjectId}")
    suspend fun renameSubject(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Body request: SubjectRequest,
    ): Response<StudyPlan>

    @DELETE("plans/{planId}/subjects/{subjectId}")
    suspend fun deleteSubject(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
    ): Response<StudyPlan>

    @POST("plans/{planId}/subjects/{subjectId}/chapters")
    suspend fun addChapter(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Body request: ChapterRequest,
    ): Response<StudyPlan>

    @PATCH("plans/{planId}/subjects/{subjectId}/chapters/{chapterId}")
    suspend fun renameChapter(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Path("chapterId") chapterId: String,
        @Body request: ChapterRequest,
    ): Response<StudyPlan>

    @DELETE("plans/{planId}/subjects/{subjectId}/chapters/{chapterId}")
    suspend fun deleteChapter(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Path("chapterId") chapterId: String,
    ): Response<StudyPlan>

    @POST("plans/{planId}/subjects/{subjectId}/chapters/{chapterId}/topics")
    suspend fun addTopic(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Path("chapterId") chapterId: String,
        @Body request: TopicRequest,
    ): Response<StudyPlan>

    @PATCH("plans/{planId}/topics/{topicId}")
    suspend fun updateTopic(
        @Path("planId") planId: String,
        @Path("topicId") topicId: String,
        @Body request: TopicPatchRequest,
    ): Response<StudyPlan>

    @DELETE("plans/{planId}/topics/{topicId}")
    suspend fun deleteTopic(
        @Path("planId") planId: String,
        @Path("topicId") topicId: String,
    ): Response<StudyPlan>

    // The syllabus import goes through the VPS Express backend, which forwards to the
    // Railway-hosted Python agent (PyMuPDF extraction + Groq Llama-4-Scout). The full
    // round trip can easily exceed 60s on a cold start, so we tag this call so the
    // OkHttp interceptor in NetworkModule bumps connect/read/write timeouts to 180s.
    @Multipart
    @Headers("X-Timeout-Seconds: 180")
    @POST("syllabus/import")
    suspend fun importSyllabusFile(@Part file: MultipartBody.Part): Response<SyllabusImportResponse>
}

data class BasicPlannerResponse(
    val success: Boolean? = null,
    val ok: Boolean? = null,
    val message: String? = null,
)

data class CreatePlanRequest(
    val title: String,
    val examType: String? = null,
    val examDate: String? = null,
    val description: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int> = emptyList(),
)

data class CreateFromTemplateRequest(
    val templateId: String,
    val title: String? = null,
    val examDate: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int> = emptyList(),
    val autoDistribute: Boolean = false,
)

data class UpdatePlanRequest(
    val title: String? = null,
    val examType: String? = null,
    val examDate: String? = null,
    val description: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int>? = null,
)

data class AutoDistributeRequest(
    val fromDate: String? = null,
    val lockExistingDates: Boolean = true,
    val includeRevisionNeeded: Boolean = false,
)

data class SubjectRequest(
    val name: String,
    val color: String? = null,
    val weeklyTarget: Int? = null,
    val monthlyTarget: Int? = null,
)

data class ChapterRequest(val name: String)

data class TopicRequest(
    val name: String,
    val plannedDate: String? = null,
    val notes: String? = null,
)

data class TopicPatchRequest(
    val name: String? = null,
    val status: TopicStatus? = null,
    val plannedDate: String? = null,
    val notes: String? = null,
)

data class SyllabusImportResponse(
    val success: Boolean? = null,
    val syllabusCode: String? = null,
    val message: String? = null,
    val errors: List<String>? = null,
    val detail: String? = null,
    val error: String? = null,
)
