package com.safarparmar.app.data.remote.api

import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.RolloverUndoResult
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.UpgradePlannerResult
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlannerApi {
    @GET("plans")
    suspend fun listPlans(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<List<StudyPlan>>

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
    suspend fun getPlan(
        @Path("planId") planId: String,
        @Query("today") today: String? = null,
        @Query("timezone") timezone: String? = null,
    ): Response<StudyPlan>

    @PATCH("plans/{planId}")
    suspend fun updatePlan(
        @Path("planId") planId: String,
        @Body request: UpdatePlanRequest,
    ): Response<StudyPlan>

    @POST("plans/{planId}/upgrade")
    suspend fun upgradePlan(@Path("planId") planId: String): Response<UpgradePlannerResult>

    @POST("plans/{planId}/rollover/undo")
    suspend fun undoRollover(
        @Path("planId") planId: String,
        @Body request: RolloverUndoRequest,
    ): Response<RolloverUndoResult>

    @POST("plans/{planId}/undo-delete")
    suspend fun undoDelete(
        @Path("planId") planId: String,
        @Body request: DeleteUndoRequest,
    ): Response<PlanRestoreResult>

    @POST("plans/{planId}/restore")
    suspend fun restorePlan(
        @Path("planId") planId: String,
        @Body request: PlanRestoreRequest,
    ): Response<PlanRestoreResult>

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

    @PUT("plans/{planId}/order")
    suspend fun reorderSyllabus(
        @Path("planId") planId: String,
        @Body request: ReorderSyllabusRequest,
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

    @POST("plans/{planId}/subjects/{subjectId}/chapters/{chapterId}/bulk-topics")
    suspend fun bulkTopics(
        @Path("planId") planId: String,
        @Path("subjectId") subjectId: String,
        @Path("chapterId") chapterId: String,
        @Body request: BulkTopicsRequest,
    ): Response<StudyPlan>

    @POST("plans/{planId}/import-syllabus")
    suspend fun importSyllabus(
        @Path("planId") planId: String,
        @Body request: ImportSyllabusRequest,
    ): Response<StudyPlan>

    @PATCH("plans/{planId}/topics/{topicId}")
    suspend fun updateTopic(
        @Path("planId") planId: String,
        @Path("topicId") topicId: String,
        @Body request: TopicPatchRequest,
    ): Response<StudyPlan>

    /**
     * Atomic multi-topic update: applies every item in one server-side
     * read-modify-write instead of N sequential single-topic PATCH calls, so
     * a failure partway through can't leave the plan half-changed. Used for
     * swap-dates, move-to-date, and clear-day. When more than one topic is
     * updated, the response carries an `undoToken` (see StudyPlan.undoToken)
     * good for one call to [undoDelete].
     */
    @PATCH("plans/{planId}/topics")
    suspend fun batchUpdateTopics(
        @Path("planId") planId: String,
        @Body request: BatchTopicUpdateRequest,
    ): Response<StudyPlan>

    @DELETE("plans/{planId}/topics/{topicId}")
    suspend fun deleteTopic(
        @Path("planId") planId: String,
        @Path("topicId") topicId: String,
    ): Response<StudyPlan>

    @POST("plans/{planId}/syllabus-ai")
    suspend fun applySyllabusAi(
        @Path("planId") planId: String,
        @Body request: SyllabusAiRequest,
    ): Response<SyllabusAiResponse>
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
    val offDates: List<String>? = null,
    val autoRollover: Boolean? = null,
)

data class RolloverUndoRequest(
    val undoToken: String,
)

data class DeleteUndoRequest(
    val undoToken: String,
)

data class PlanRestoreRequest(
    val snapshotId: String,
)

data class PlanRestoreResult(
    val message: String? = null,
    val plan: StudyPlan? = null,
)

data class AutoDistributeRequest(
    val fromDate: String? = null,
    val lockExistingDates: Boolean = true,
    val includeRevisionNeeded: Boolean = false,
    val overloadMode: String? = null,
)

data class ReorderSyllabusRequest(
    val subjectIds: List<String>? = null,
    val chapterIdsBySubjectId: Map<String, List<String>>? = null,
    val topicIdsByChapterId: Map<String, List<String>>? = null,
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

data class BulkTopicsRequest(
    val topics: List<BulkTopicItemRequest>,
)

data class BulkTopicItemRequest(
    val name: String,
    val plannedDate: String? = null,
    val notes: String? = null,
)

data class ImportSyllabusRequest(
    val subjects: List<ImportSyllabusSubjectRequest>,
    val mode: String? = null,
)

data class ImportSyllabusSubjectRequest(
    val name: String,
    val chapters: List<ImportSyllabusChapterRequest>,
)

data class ImportSyllabusChapterRequest(
    val name: String,
    val topics: List<ImportSyllabusTopicRequest>,
)

data class ImportSyllabusTopicRequest(
    val name: String,
)

data class TopicPatchRequest(
    val name: String? = null,
    val status: TopicStatus? = null,
    val plannedDate: String? = null,
    val notes: String? = null,
    val pinned: Boolean? = null,
    // The device's local "today" (YYYY-MM-DD), used server-side to anchor
    // completedDate when status becomes DONE instead of the server's own clock.
    val clientDateKey: String? = null,
    val revisionMarkedAt: String? = null,
    val revisionReminderDates: List<String>? = null,
    val revisionScheduleType: String? = null,
)

data class BatchTopicUpdateItem(
    val topicId: String,
    val patch: TopicPatchRequest,
)

data class BatchTopicUpdateRequest(
    val updates: List<BatchTopicUpdateItem>,
)

data class BulkImportRequest(
    val subjects: List<BulkImportSubjectRequest>,
)

data class BulkImportSubjectRequest(
    val name: String,
    val chapters: List<BulkImportChapterRequest>,
)

data class BulkImportChapterRequest(
    val name: String,
    val topics: List<String>,
)

data class BulkImportResponse(
    val success: Boolean = false,
    val subjects: Int = 0,
    val chapters: Int = 0,
    val topics: Int = 0,
    val subjectsCreated: Int? = null,
    val chaptersCreated: Int? = null,
    val topicsCreated: Int? = null,
    val message: String? = null,
    val errorCode: String? = null,
)

data class StructureSyllabusRequest(
    val rawText: String,
    val examType: String? = null,
    val planTitle: String? = null,
    val language: String? = null,
)

data class StructureSyllabusResponse(
    val success: Boolean = false,
    val subjects: List<StructuredSubject>? = null,
    val warnings: List<String>? = null,
    val stats: SyllabusStats? = null,
    /** Legacy nested shape (unused by current server). */
    val data: StructuredSyllabusPreview? = null,
    val errorCode: String? = null,
    val message: String? = null,
)

fun StructureSyllabusResponse.toPreview(): StructuredSyllabusPreview? {
    data?.let { return it }
    val subjectList = subjects ?: return null
    return StructuredSyllabusPreview(
        subjects = subjectList,
        warnings = warnings.orEmpty(),
        stats = stats ?: SyllabusStats(),
    )
}

data class StructuredSyllabusPreview(
    val subjects: List<StructuredSubject> = emptyList(),
    val warnings: List<String> = emptyList(),
    val stats: SyllabusStats = SyllabusStats(),
)

data class StructuredSubject(
    val name: String,
    val chapters: List<StructuredChapter> = emptyList(),
)

data class StructuredChapter(
    val name: String,
    val topics: List<String> = emptyList(),
)

data class SyllabusStats(
    val subjectCount: Int = 0,
    val chapterCount: Int = 0,
    val topicCount: Int = 0,
)

data class SyllabusAiRequest(
    val aiPreview: SyllabusAiPreview,
)

data class SyllabusAiPreview(
    val subjects: List<SyllabusAiSubject>,
)

data class SyllabusAiSubject(
    val name: String,
    val chapters: List<SyllabusAiChapter> = emptyList(),
)

data class SyllabusAiChapter(
    val name: String,
    val topics: List<String> = emptyList(),
)

fun StructuredSyllabusPreview.toSyllabusAiPreview(): SyllabusAiPreview = SyllabusAiPreview(
    subjects = subjects.map { subject ->
        SyllabusAiSubject(
            name = subject.name,
            chapters = subject.chapters.map { chapter ->
                SyllabusAiChapter(
                    name = chapter.name,
                    topics = chapter.topics.map { it.trim() }.filter { it.isNotBlank() },
                )
            },
        )
    },
)

data class SyllabusAiResponse(
    val success: Boolean = false,
    val plan: StudyPlan? = null,
    val message: String? = null,
)

data class SyllabusImportResponse(
    val success: Boolean? = null,
    val syllabusCode: String? = null,
    val message: String? = null,
    val errors: List<String>? = null,
    val detail: String? = null,
    val error: String? = null,
    val errorCode: String? = null,
)
