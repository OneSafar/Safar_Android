package com.safarparmar.app.feature.toppersbatch

import androidx.annotation.Keep
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Gson reads these wire models by reflection, including in optimized release builds.
@Keep
data class BatchStatus(val available: Boolean = false, val enabled: Boolean = false)
@Keep
data class BatchSubject(
    val id: String = "", val key: String = "", val name: String = "", val enabled: Boolean = true,
    val color: String? = null, val defaultColor: String? = null, val externalUrl: String? = null, val externalProvider: String? = null,
)
@Keep
data class ReviewSession(val date: String = "", val completedAt: String? = null)
@Keep
data class BatchLecture(
    val legacyLocalId: String? = null, val id: String = "", val subjectId: String = "", val subjectKey: String = "", val chapterId: String? = null,
    val lectureNumber: Int = 0, val order: Int = 0, val originalTopic: String = "", val displayTopic: String = "",
    val section: String? = null, val sourceMonth: String = "", val sourceWeek: String = "",
    val planVersion: Int = 0, val completionDateUnknown: Boolean = false,
    val studyReminderTime: String? = null, val revisionReminderTime: String? = null,
    val completedAt: String? = null, val scheduledFor: String? = null, val classTime: String? = null,
    val liveYoutubeUrl: String? = null, val recordedUrl: String? = null, val classStartsAt: String? = null,
    val releaseState: String? = null, val isAvailable: Boolean? = null,
    val backlogAddedAt: String? = null, val backlogResolvedAt: String? = null,
    val revisionDate: String? = null, val revisionCompletedAt: String? = null,
    val revisionMode: String? = null, val revisionSessions: List<ReviewSession>? = null,
    val legacyRevisionMode: String? = null, val legacyRevisionSessions: List<ReviewSession>? = null,
    val userAdded: Boolean = false,
    val studyPlannedFor: String? = null, val revisionTagged: Boolean = false, val color: String? = null, val liveWindow: BatchLiveWindow? = null,
) {
    fun isLocked(today: String): Boolean {
        if (completedAt != null) return false
        isAvailable?.let { return !it }
        if (releaseState == "locked") return true
        if (releaseState == "released") return false
        return scheduledFor?.take(10)?.let { date ->
            runCatching { java.time.LocalDate.parse(date).isAfter(java.time.LocalDate.parse(today)) }.getOrDefault(false)
        } == true
    }
    val sessions: List<ReviewSession> get() = revisionSessions ?: legacyRevisionSessions.orEmpty()
    fun olderWork(today: String): Boolean = completedAt == null && !isLocked(today) &&
        ((backlogAddedAt != null && backlogResolvedAt == null) || (scheduledFor != null && scheduledFor < today))
    val pendingRevision: Boolean get() = completedAt != null && revisionDate != null && revisionCompletedAt == null
}
@Keep
data class BatchChapter(
    val id: String = "", val subjectId: String = "", val title: String = "",
    val firstLectureNumber: Int = 0, val lastLectureNumber: Int = 0,
    val completedAt: String? = null, val externalChapterUrl: String? = null,
)
@Keep
data class SubjectProgress(
    val subjectId: String = "", val completed: Int = 0, val total: Int = 0,
    val backlog: Int = 0, val behind: Int = 0, val batchAt: Int = 0,
)
@Keep
data class BatchProgress(
    val completed: Int = 0, val total: Int = 0, val backlog: Int = 0, val behind: Int = 0,
    val bySubject: List<SubjectProgress> = emptyList(),
)
@Keep
data class BatchCourse(val id: String = "", val name: String = "", val officialStartDate: String? = null, val academyCourseUrl: String? = null)
@Keep
data class BatchStudyPlan(val startDate: String = "", val targetDate: String? = null, val weeklyGoal: Int = 7)
@Keep
data class BatchSubjectWatch(val subjectId: String = "", val nextLectureId: String? = null,
                             val backlogLectureIds: List<String> = emptyList())
@Keep
data class BatchLectureControl(
    val subjectKey: String = "", val lectureNumber: Int = 0,
    val scheduledDate: String? = null, val classTime: String? = null,
    val releaseState: String = "scheduled", val isAvailable: Boolean = false,
)
@Keep
data class BatchTrackerContent(val copy: Map<String, String> = emptyMap())


@Keep
data class BatchOverview(
    val canUseFeatures: Boolean = true,
    val studyWorkflow: BatchStudyWorkflow = BatchStudyWorkflow(),
    val content: BatchTrackerContent = BatchTrackerContent(),
    val lectureControls: List<BatchLectureControl> = emptyList(), val serverDay: String? = null,
    val studyPlan: BatchStudyPlan? = null, val course: BatchCourse = BatchCourse(), val subjects: List<BatchSubject> = emptyList(),
    val removedSubjects: List<BatchSubject> = emptyList(), val removedLectures: List<BatchLecture> = emptyList(),
    val lectures: List<BatchLecture> = emptyList(), val chapters: List<BatchChapter> = emptyList(),
    val progress: BatchProgress = BatchProgress(), val watchList: List<BatchSubjectWatch> = emptyList(),
)
@Keep
data class BatchToday(val date: String = "", val lectures: List<BatchLecture> = emptyList(), val backlogPick: BatchLecture? = null)
@Keep
data class BatchCalendar(
    val month: String = "", val days: Map<String, List<BatchLecture>> = emptyMap(),
    val classes: Map<String, List<BatchLecture>> = emptyMap(),
)
@Keep
data class LectureResult(val lecture: BatchLecture = BatchLecture(), val chapterCompleted: BatchChapter? = null)
@Keep
data class RemoveResult(val removed: Boolean = false)

/** Same account-backed API used by the web Toppers Batch tracker. */
interface ToppersBatchApi {
    @GET("plans/toppers-batch/v2/status") suspend fun status(): Response<BatchStatus>
    @POST("plans/toppers-batch/v2/activate") suspend fun activate(): Response<BatchOverview>
    @POST("plans/toppers-batch/v2/import-local-progress") suspend fun importLocalProgress(@Body body: RequestBody): Response<BatchOverview>
    @GET("plans/toppers-batch/v2") suspend fun overview(): Response<BatchOverview>
    @PATCH("plans/toppers-batch/v2/study-plan") suspend fun studyPlan(@Body body: RequestBody): Response<BatchOverview>
    @POST("plans/toppers-batch/v2/reschedule") suspend fun reschedule(@Body body: RequestBody): Response<BatchOverview>
    @POST("plans/toppers-batch/v2/subjects/{id}/lectures") suspend fun addLecture(@Path("id") id: String, @Body body: RequestBody): Response<BatchLecture>
    @POST("plans/toppers-batch/v2/prior-progress") suspend fun priorProgress(@Body body: RequestBody): Response<BatchOverview>
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

@Keep
data class BatchLiveWindow(val startsAt: String? = null, val endsAt: String? = null, val youtubeUrl: String? = null, val enabled: Boolean = true) {
    fun status(now: java.time.Instant): String {
        if (!enabled) return "academy-only"
        val start = startsAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return "unavailable"
        val end = endsAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: return "unavailable"
        if (end <= start) return "unavailable"
        return when { now < start -> "upcoming"; now >= end -> "ended"; else -> "live" }
    }
}

@Keep
data class BatchStudyActivity(val lectureId: String = "", val kind: String = "watch", val date: String = "",
    val reminderTime: String? = null, val sessionIndex: Int? = null, val waitingForRelease: Boolean = false)
@Keep
data class BatchStudyWorkflow(val today: List<BatchStudyActivity> = emptyList(), val missed: List<BatchStudyActivity> = emptyList(),
    val backlogReasons: Map<String, String> = emptyMap(), val revisionsCompleted: Int = 0, val completedToday: List<String> = emptyList())
