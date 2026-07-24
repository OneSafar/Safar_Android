package com.safarparmar.app.data.remote.api

import com.safarparmar.app.domain.model.studyplanner.AutoDistributeResult
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.RolloverUndoResult
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.RevisionCompletion
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

    @POST("plans/preview")
    suspend fun previewPlan(@Body request: PlanPreviewRequest): Response<PlanPreviewResult>

    @POST("plans/confirm")
    suspend fun confirmPlan(@Body request: PlanConfirmRequest): Response<StudyPlan>

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

    @POST("plans/{planId}/finish-day")
    suspend fun finishDay(
        @Path("planId") planId: String,
        @Body request: FinishDayRequest,
    ): Response<FinishDayResult>

    @POST("plans/{planId}/finish-day/undo")
    suspend fun undoFinishDay(
        @Path("planId") planId: String,
        @Body request: UndoFinishDayRequest,
    ): Response<FinishDayResult>

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

    /** Moves exactly one saved revision appointment. */
    @PATCH("plans/{planId}/topics/{topicId}/revision-date")
    suspend fun changeRevisionDate(
        @Path("planId") planId: String,
        @Path("topicId") topicId: String,
        @Body request: RevisionDateChangeRequest,
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
    val dailyTodos: List<com.safarparmar.app.domain.model.studyplanner.DailyTodo>? = null,
    val dailyTodoLogs: Map<String, List<String>>? = null,
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
    val preserveFromDate: Boolean = false,
    val includeRevisionNeeded: Boolean = false,
    val overloadMode: String? = null,
    val strategy: String? = null,
    /** For strategy "priority_split" ("Mixed Bag"): the chosen subject names, in
     *  pick order. They are scheduled exclusively before every other subject.
     *  Ignored for other strategies. */
    val prioritySubjectNames: List<String>? = null,
    /** "sequential" | "balanced" — ordering within the priority phase. */
    val priorityOrderMode: String? = null,
    /** Restricts the run to these topics; everything else keeps its dates.
     *  Used by "Move all to my free days" so recovering missed work never
     *  disturbs the topics the student hasn't reached yet. */
    val onlyTopicIds: List<String>? = null,
)

data class ReorderSyllabusRequest(
    val subjectIds: List<String>? = null,
    val chapterIdsBySubjectId: Map<String, List<String>>? = null,
    val topicIdsByChapterId: Map<String, List<String>>? = null,
)

/**
 * Draft → preview → confirm plan creation. `source` picks which of the other
 * fields are required: "template" needs [templateId] (+ optional
 * [excludeTopicKeys], a list of [subjectIndex, chapterIndex, topicIndex]
 * tuples into the template's own array order — template topics have no
 * stable id server-side, so exclusion is positional); "manual"/"paste" need
 * [subjects]. Nothing is saved as a real, visible plan until confirmPlan().
 */
data class PlanPreviewRequest(
    val source: String,
    val title: String? = null,
    val templateId: String? = null,
    val excludeTopicKeys: List<List<Int>>? = null,
    /** New chapters the user appended to a template subject (drill-down "+" on the
     *  chapters screen), each carrying its own topics — merged server-side before
     *  [excludeTopicKeys] filtering runs. */
    val extraChapters: List<TemplateExtraChapterRequest>? = null,
    /** New topics the user appended to an existing template chapter (drill-down "+"
     *  on the topics screen). [chapterIndex] refers to the chapter's original,
     *  pre-addition index within the subject. */
    val extraTopics: List<TemplateExtraTopicRequest>? = null,
    /** "Deep Focus" drag-reorder, applied server-side after the subject/chapter tree
     *  is materialized from whichever source was used — keyed by name, not index,
     *  since it's captured against the tree as the user last saw it. */
    val subjectOrder: List<String>? = null,
    val chapterOrder: Map<String, List<String>>? = null,
    /** Topic order within each chapter: subjectName -> chapterName -> topic names. */
    val topicOrder: Map<String, Map<String, List<String>>>? = null,
    /** "Mixed Bag" split-focus: the 2-3 subject names the user flagged as most
     *  difficult. When present with `strategy: "interleaved"`, the server upgrades
     *  scheduling to "priority_split" — these subjects get topics every study day,
     *  the rest rotate in one at a time on alternate days. */
    val prioritySubjects: List<String>? = null,
    /** How the Mixed Bag priority phase is ordered: "sequential" finishes each
     *  chosen subject in pick order; "balanced" interleaves them. Either way the
     *  remaining subjects only start once the chosen ones run out. */
    val priorityOrderMode: String? = null,
    val subjects: List<ImportSyllabusSubjectRequest>? = null,
    val examDate: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int> = emptyList(),
    val strategy: String? = null,
    val overloadMode: String? = null,
    /** Create-time chapter difficulty ratings (manual/paste sources), applied
     *  server-side before scheduling so the first schedule is already weighted. */
    val chapterRatings: List<ChapterRatingRequest>? = null,
    /** false = "Same every day": server strips all weights so each day holds
     *  exactly [dailyGoal] topics. Omitted/true = weighted planning. */
    val weightedPlanning: Boolean? = null,
)

data class ChapterRatingRequest(
    val subject: String,
    val chapter: String,
    /** "easy" | "normal" | "tough" */
    val difficulty: String,
)

data class TemplateExtraChapterRequest(
    val subjectIndex: Int,
    val name: String,
    val topics: List<String>,
)

data class TemplateExtraTopicRequest(
    val subjectIndex: Int,
    val chapterIndex: Int,
    val name: String,
)

data class PlanPreviewResult(
    val draftId: String,
    val status: String,
    val title: String? = null,
    val examDate: String? = null,
    val dailyGoal: Int? = null,
    val summary: PlanPreviewSummary,
    val calendarPreview: CalendarMap = emptyMap(),
    val warnings: List<String> = emptyList(),
)

data class PlanPreviewSummary(
    val subjectCount: Int,
    val totalTopics: Int,
    val scheduleAssigned: Int,
    val scheduleSkipped: Int,
    val requiredPerDay: Int? = null,
    val daysUntilExam: Int? = null,
)

data class PlanConfirmRequest(
    val draftId: String,
)

data class SubjectRequest(
    val name: String,
    val color: String? = null,
    val weeklyTarget: Int? = null,
    val monthlyTarget: Int? = null,
)

data class ChapterRequest(
    val name: String? = null,
    /** "easy" | "normal" | "tough"; "" clears the rating server-side. */
    val difficulty: String? = null,
)

data class TopicRequest(
    val name: String,
    val plannedDate: String? = null,
    val notes: String? = null,
    /** "small" | "medium" | "big" */
    val size: String? = null,
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
    /** "small" | "medium" | "big" — preserved so a weighted template keeps its
     *  effort sizes when materialized through the syllabus-import path. */
    val size: String? = null,
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
    val revisionCompletedDates: List<String>? = null,
    val revisionCompletionLog: List<RevisionCompletion>? = null,
    val revisionScheduleType: String? = null,
    /** "small" | "medium" | "big" */
    val size: String? = null,
    /** Partial completion 0–100 (Stage 3 drag-to-fill). */
    val progressPercent: Int? = null,
)

data class RevisionDateChangeRequest(
    val oldDate: String,
    val newDate: String,
)

data class BatchTopicUpdateItem(
    val topicId: String,
    val patch: TopicPatchRequest,
)

data class BatchTopicUpdateRequest(
    val updates: List<BatchTopicUpdateItem>,
)

data class FinishDayRequest(val dateKey: String)
data class UndoFinishDayRequest(val undoToken: String)
data class FinishDayResult(
    val plan: StudyPlan,
    val undoToken: String? = null,
    val movedCount: Int = 0,
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
    val mode: String = "merge",
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
