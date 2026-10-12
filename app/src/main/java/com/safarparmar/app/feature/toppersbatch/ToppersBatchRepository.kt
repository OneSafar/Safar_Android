package com.safarparmar.app.feature.toppersbatch

import kotlinx.coroutines.CancellationException
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class ToppersBatchRepository @Inject constructor(private val api: ToppersBatchApi, private val gk: GkLectureStore? = null, private val cache: BatchOverviewCache? = null) {
    private var planActionAvailable = true
    suspend fun studyMode() = cache?.studyMode()
    suspend fun saveStudyMode(mode: BatchStudyMode) { cache?.saveStudyMode(mode) }
    suspend fun status() = safeApiCall { api.status() }
    suspend fun cachedOverview() = cache?.load()
    suspend fun clearCache() { cache?.clear() }
    suspend fun open(): Resource<BatchOverview> {
        val result = overview()
        return if (result is Resource.Error && result.errorCode == "PLANNER_V2_NOT_ENABLED") activate() else result
    }
    private suspend fun fetchOverview(request: suspend () -> retrofit2.Response<BatchOverview>): Resource<BatchOverview> {
        val account = cache?.account()
        val version = cache?.version() ?: 0L
        return safeApiCall(request).mapSuccess {
            check(cache == null || cache.account() == account) { "Your account changed. Please reopen the tracker." }
            val overview = gk?.attach(it.officialOnly()) ?: it.officialOnly()
            cache?.save(account, overview, version)
            overview
        }
    }
    private suspend fun <T> mutation(request: suspend () -> Resource<T>): Resource<T> {
        cache?.clear() // Never replay an older snapshot after a student's edit.
        return request()
    }
    suspend fun activate() = fetchOverview { api.activate() }
    suspend fun overview() = fetchOverview { api.overview() }
    suspend fun studyPlan(plan: BatchStudyPlan) = mutation { fetchOverview { api.studyPlan(jsonBody(mapOf("startDate" to plan.startDate, "targetDate" to plan.targetDate, "weeklyGoal" to plan.weeklyGoal))) } }
    suspend fun reschedule(date: String, items: List<Map<String, Any>>) = mutation { fetchOverview { api.reschedule(jsonBody(mapOf("firstDate" to date, "items" to org.json.JSONArray(items.map { org.json.JSONObject(it) })))) } }
    suspend fun priorProgress(ids: List<String>, date: String?) = mutation { fetchOverview { api.priorProgress(jsonBody(mapOf("lectureIds" to org.json.JSONArray(ids), "completedDate" to date))) } }
    suspend fun today(date: String) = safeApiCall { api.today(date) }.mapSuccess(BatchToday::officialOnly)
    suspend fun calendar(month: String, offsetMinutes: Int) =
        safeApiCall { api.calendar(month, offsetMinutes) }.mapSuccess(BatchCalendar::officialOnly)
    suspend fun action(id: String, action: String, body: Map<String, Any> = emptyMap()) = mutation { safeApiCall { api.lectureAction(id, action, body) } }
    /** Older tracker servers expose study-date but do not yet recognize the plan action. */
    suspend fun savePlan(id: String, body: Map<String, Any>): Resource<LectureResult> {
        if (planActionAvailable) {
            val result = action(id, "plan", body)
            if (result !is Resource.Error || result.code != 404 || result.message != "This option is not available") return result
            planActionAvailable = false
        }
        if (body["kind"] != "watch") return Resource.Error("Revision planning is currently unavailable.")
        if (body["remove"] == true) return removeAction(id, "study-date")
        if (body["reminderTime"] != null) return Resource.Error("Reminders are unavailable. Turn off Remind me to save the date.")
        val dates = body["dates"] as? List<*>
        if (dates != null && dates.size != 1) return Resource.Error("Choose one watch date.")
        val date = (dates?.singleOrNull() ?: body["date"]) as? String
            ?: return Resource.Error("Choose one watch date.")
        return action(id, "study-date", mapOf("date" to date))
    }
    suspend fun removeAction(id: String, action: String) = mutation { safeApiCall { api.removeLectureAction(id, action) } }
    suspend fun completeRevision(id: String, sessionIndex: Int, version: Int) = mutation { safeApiCall { api.completeRevision(id, mapOf("sessionIndex" to sessionIndex.coerceAtLeast(0), "version" to version)) } }
    suspend fun editLecture(id: String, body: Map<String, Any?>) = mutation { safeApiCall { api.editLecture(id, jsonBody(body)) } }
    suspend fun removeLecture(id: String) = mutation { safeApiCall { api.removeLecture(id) } }
    suspend fun restoreLecture(id: String) = mutation { safeApiCall { api.restoreLecture(id) } }
    suspend fun editSubject(id: String, body: Map<String, Any?>): Resource<BatchSubject> = mutation {
        try {
            safeApiCall { api.editSubject(id, jsonBody(body)) }
        } finally {
            // A read started during the save may still carry the old subject colour.
            cache?.clear()
        }
    }
    suspend fun addLecture(id: String, title: String) = mutation { safeApiCall { api.addLecture(id, jsonBody(mapOf("displayTopic" to title))) } }
    suspend fun removeSubject(id: String) = mutation { safeApiCall { api.removeSubject(id) } }
    suspend fun restoreSubject(id: String) = mutation { safeApiCall { api.restoreSubject(id) } }
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

private suspend fun <T, R> Resource<T>.mapSuccess(transform: suspend (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> try {
        Resource.Success(transform(data))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Gson can populate a non-null Kotlin property with an explicit JSON null.
        // Mapping happens after safeApiCall, so malformed data needs its own boundary.
        Resource.Error("Could not read Toppers Batch data. Please try again.")
    }
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
    val (watch, workflow) = copy(lectures = next).projectStudyWorkflow(today)
    val backlogIds = watch.flatMapTo(mutableSetOf()) { it.backlogLectureIds }
    val rows = subjects.map { subject ->
        val own = next.filter { it.subjectId == subject.id }
        SubjectProgress(
            subjectId = subject.id, completed = own.count { it.completedAt != null }, total = own.size,
            backlog = own.count { it.id in backlogIds },
            behind = own.count { it.id in backlogIds },
            batchAt = own.count { it.isAvailable ?: (it.scheduledFor != null && it.scheduledFor <= today) },
        )
    }
    return copy(lectures = next, watchList = watch, studyWorkflow = workflow, progress = BatchProgress(
        completed = visible.count { it.completedAt != null }, total = visible.size,
        backlog = visible.count { it.id in backlogIds },
        behind = visible.count { it.id in backlogIds },
        bySubject = rows,
    ))
}

/** Official events are already in the overview; a second full server load is unnecessary. */
internal fun BatchOverview.todayEvents(date: String): BatchToday {
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    return BatchToday(date, lectures.filter { it.subjectId in active && it.scheduledFor == date },
        lectures.firstOrNull { it.subjectId in active && it.completedAt == null && !it.isLocked(date) && it.scheduledFor?.let { day -> day < date } == true })
}
