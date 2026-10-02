package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class ToppersBatchRepository @Inject constructor(private val api: ToppersBatchApi) {
    suspend fun status() = safeApiCall { api.status() }
    suspend fun activate() = safeApiCall { api.activate() }.mapSuccess(BatchOverview::officialOnly)
    suspend fun overview() = safeApiCall { api.overview() }.mapSuccess(BatchOverview::officialOnly)
    suspend fun today(date: String) = safeApiCall { api.today(date) }.mapSuccess(BatchToday::officialOnly)
    suspend fun calendar(month: String, offsetMinutes: Int) =
        safeApiCall { api.calendar(month, offsetMinutes) }.mapSuccess(BatchCalendar::officialOnly)
    suspend fun action(id: String, action: String, body: Map<String, Any> = emptyMap()) =
        safeApiCall { api.lectureAction(id, action, body) }
    suspend fun removeAction(id: String, action: String) = safeApiCall { api.removeLectureAction(id, action) }
    suspend fun completeRevision(id: String, sessionIndex: Int) =
        safeApiCall { api.completeRevision(id, mapOf("sessionIndex" to sessionIndex)) }
    suspend fun editLecture(id: String, body: Map<String, Any?>) = safeApiCall { api.editLecture(id, jsonBody(body)) }
    suspend fun removeLecture(id: String) = safeApiCall { api.removeLecture(id) }
    suspend fun restoreLecture(id: String) = safeApiCall { api.restoreLecture(id) }
    suspend fun editSubject(id: String, body: Map<String, Any?>) = safeApiCall { api.editSubject(id, jsonBody(body)) }
    suspend fun removeSubject(id: String) = safeApiCall { api.removeSubject(id) }
    suspend fun restoreSubject(id: String) = safeApiCall { api.restoreSubject(id) }
}

private val officialKeys = setOf("english", "mathematics", "reasoning", "gk")
private fun BatchSubject.isOfficial() = key in officialKeys
private fun BatchLecture.isOfficial() = subjectKey in officialKeys

/** Keep legacy personal-course data on the server, but never mix it into this batch tracker. */
internal fun BatchOverview.officialOnly(): BatchOverview {
    val officialSubjects = subjects.filter(BatchSubject::isOfficial)
    val rows = progress.bySubject.filter { row -> officialSubjects.any { it.id == row.subjectId } }
    val activeIds = officialSubjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val activeRows = rows.filter { it.subjectId in activeIds }
    return copy(
        subjects = officialSubjects,
        removedSubjects = removedSubjects.filter(BatchSubject::isOfficial),
        removedLectures = removedLectures.filter(BatchLecture::isOfficial),
        lectures = lectures.filter(BatchLecture::isOfficial),
        watchList = watchList.filter { item -> officialSubjects.any { it.id == item.subjectId } },
        chapters = chapters.filter { chapter -> officialSubjects.any { it.id == chapter.subjectId } },
        progress = BatchProgress(
            completed = activeRows.sumOf { it.completed }, total = activeRows.sumOf { it.total },
            backlog = activeRows.sumOf { it.backlog }, behind = activeRows.sumOf { it.behind }, bySubject = rows,
        ),
    )
}

internal fun BatchToday.officialOnly() = copy(
    lectures = lectures.filter(BatchLecture::isOfficial),
    backlogPick = backlogPick?.takeIf(BatchLecture::isOfficial),
)

internal fun BatchCalendar.officialOnly() = copy(
    days = days.mapValues { (_, rows) -> rows.filter(BatchLecture::isOfficial) }.filterValues { it.isNotEmpty() },
    classes = classes.mapValues { (_, rows) -> rows.filter(BatchLecture::isOfficial) }.filterValues { it.isNotEmpty() },
)

private fun <T, R> Resource<T>.mapSuccess(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, code, errorCode)
    is Resource.Loading -> Resource.Loading()
}

private fun jsonBody(values: Map<String, Any?>): RequestBody = JSONObject().apply {
    values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

/** Recompute the visible totals locally so a tick and undo update without waiting for a refetch. */
fun BatchOverview.withLecture(updated: BatchLecture, today: String): BatchOverview {
    if (lectures.none { it.id == updated.id }) return this
    val next = lectures.map { if (it.id == updated.id) updated else it }
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val visible = next.filter { it.subjectId in active }
    val watch = subjects.filter { it.enabled }.map { subject ->
        val own = next.filter { it.subjectId == subject.id }
            .sortedWith(compareBy<BatchLecture> { it.order }.thenBy { it.lectureNumber })
        val furthestDone = own.indexOfLast { it.completedAt != null }
        BatchSubjectWatch(subject.id, own.firstOrNull { it.completedAt == null }?.id,
            own.filterIndexed { index, lecture -> lecture.completedAt == null &&
                (index < furthestDone || (lecture.backlogAddedAt != null && lecture.backlogResolvedAt == null))
            }.map { it.id })
    }
    val backlogIds = watch.flatMapTo(mutableSetOf()) { it.backlogLectureIds }
    val rows = subjects.map { subject ->
        val own = next.filter { it.subjectId == subject.id }
        SubjectProgress(
            subjectId = subject.id, completed = own.count { it.completedAt != null }, total = own.size,
            backlog = own.count { it.id in backlogIds },
            behind = own.count { it.completedAt == null && it.scheduledFor != null && it.scheduledFor < today },
            batchAt = own.count { it.scheduledFor != null && it.scheduledFor <= today },
        )
    }
    return copy(lectures = next, watchList = watch, progress = BatchProgress(
        completed = visible.count { it.completedAt != null }, total = visible.size,
        backlog = visible.count { it.id in backlogIds },
        behind = visible.count { it.completedAt == null && it.scheduledFor != null && it.scheduledFor < today },
        bySubject = rows,
    ))
}
