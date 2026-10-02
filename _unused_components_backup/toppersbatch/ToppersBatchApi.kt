package com.safarparmar.app.feature.toppersbatch

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class BatchStatus(val available: Boolean = false, val enabled: Boolean = false)
data class BatchSubject(
    val id: String = "", val key: String = "", val name: String = "", val enabled: Boolean = true,
    val color: String? = null, val externalUrl: String? = null, val externalProvider: String? = null,
)
data class ReviewSession(val date: String = "", val completedAt: String? = null)
data class BatchLecture(
    val id: String = "", val subjectId: String = "", val subjectKey: String = "", val chapterId: String? = null,
    val lectureNumber: Int = 0, val order: Int = 0, val originalTopic: String = "", val displayTopic: String = "",
    val section: String? = null, val sourceMonth: String = "", val sourceWeek: String = "",
    val completedAt: String? = null, val scheduledFor: String? = null, val classTime: String? = null,
    val liveYoutubeUrl: String? = null, val recordedUrl: String? = null,
    val backlogAddedAt: String? = null, val backlogResolvedAt: String? = null,
    val revisionDate: String? = null, val revisionCompletedAt: String? = null,
    val revisionMode: String? = null, val revisionSessions: List<ReviewSession>? = null,
    val legacyRevisionMode: String? = null, val legacyRevisionSessions: List<ReviewSession>? = null,
    val color: String? = null,
) {
    val sessions: List<ReviewSession> get() = revisionSessions ?: legacyRevisionSessions.orEmpty()
    fun olderWork(today: String): Boolean = completedAt == null &&
        ((backlogAddedAt != null && backlogResolvedAt == null) || (scheduledFor != null && scheduledFor < today))
    val pendingRevision: Boolean get() = revisionDate != null && revisionCompletedAt == null
}
data class BatchChapter(
    val id: String = "", val subjectId: String = "", val title: String = "",
    val firstLectureNumber: Int = 0, val lastLectureNumber: Int = 0,
    val completedAt: String? = null, val externalChapterUrl: String? = null,
)
data class SubjectProgress(
    val subjectId: String = "", val completed: Int = 0, val total: Int = 0,
    val backlog: Int = 0, val behind: Int = 0, val batchAt: Int = 0,
)
data class BatchProgress(
    val completed: Int = 0, val total: Int = 0, val backlog: Int = 0, val behind: Int = 0,
    val bySubject: List<SubjectProgress> = emptyList(),
)
data class BatchCourse(val id: String = "", val name: String = "")
data class BatchSubjectWatch(val subjectId: String = "", val nextLectureId: String? = null,
                             val backlogLectureIds: List<String> = emptyList())
data class BatchOverview(
    val course: BatchCourse = BatchCourse(), val subjects: List<BatchSubject> = emptyList(),
    val removedSubjects: List<BatchSubject> = emptyList(), val removedLectures: List<BatchLecture> = emptyList(),
    val lectures: List<BatchLecture> = emptyList(), val chapters: List<BatchChapter> = emptyList(),
    val progress: BatchProgress = BatchProgress(), val watchList: List<BatchSubjectWatch> = emptyList(),
)
data class BatchToday(val date: String = "", val lectures: List<BatchLecture> = emptyList(), val backlogPick: BatchLecture? = null)
data class BatchCalendar(
    val month: String = "", val days: Map<String, List<BatchLecture>> = emptyMap(),
    val classes: Map<String, List<BatchLecture>> = emptyMap(),
)
data class LectureResult(val lecture: BatchLecture = BatchLecture(), val chapterCompleted: BatchChapter? = null)
data class RemoveResult(val removed: Boolean = false)

/** Same account-backed API used by the web Toppers Batch tracker. */
interface ToppersBatchApi {
    @GET("plans/toppers-batch/v2/status") suspend fun status(): Response<BatchStatus>
    @POST("plans/toppers-batch/v2/activate") suspend fun activate(): Response<BatchOverview>
    @GET("plans/toppers-batch/v2") suspend fun overview(): Response<BatchOverview>
    @GET("plans/toppers-batch/v2/today") suspend fun today(@Query("date") date: String): Response<BatchToday>
    @GET("plans/toppers-batch/v2/calendar") suspend fun calendar(
        @Query("month") month: String, @Query("offsetMinutes") offsetMinutes: Int,
    ): Response<BatchCalendar>

    @POST("plans/toppers-batch/v2/lectures/{id}/{action}") suspend fun lectureAction(
        @Path("id") id: String, @Path("action") action: String, @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): Response<LectureResult>
    @DELETE("plans/toppers-batch/v2/lectures/{id}/{action}") suspend fun removeLectureAction(
        @Path("id") id: String, @Path("action") action: String,
    ): Response<LectureResult>
    @POST("plans/toppers-batch/v2/lectures/{id}/revision/complete") suspend fun completeRevision(
        @Path("id") id: String, @Body body: Map<String, Int>,
    ): Response<LectureResult>
    @PATCH("plans/toppers-batch/v2/lectures/{id}") suspend fun editLecture(
        @Path("id") id: String, @Body body: RequestBody,
    ): Response<BatchLecture>
    @DELETE("plans/toppers-batch/v2/lectures/{id}") suspend fun removeLecture(@Path("id") id: String): Response<RemoveResult>
    @POST("plans/toppers-batch/v2/lectures/{id}/restore") suspend fun restoreLecture(@Path("id") id: String): Response<BatchLecture>

    @PATCH("plans/toppers-batch/v2/subjects/{id}") suspend fun editSubject(
        @Path("id") id: String, @Body body: RequestBody,
    ): Response<BatchSubject>
    @DELETE("plans/toppers-batch/v2/subjects/{id}") suspend fun removeSubject(@Path("id") id: String): Response<RemoveResult>
    @POST("plans/toppers-batch/v2/subjects/{id}/restore") suspend fun restoreSubject(@Path("id") id: String): Response<BatchSubject>

}
