package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

enum class BatchSection(@androidx.annotation.StringRes val titleRes: Int) {
    TODAY(R.string.toppers_batch_today), COURSES(R.string.toppers_batch_library), PROGRESS(R.string.toppers_batch_progress), CALENDAR(R.string.toppers_batch_calendar),
}
enum class LectureTab(@androidx.annotation.StringRes val titleRes: Int) { LECTURES(R.string.toppers_batch_lectures), OLDER(R.string.toppers_batch_backlog), REVISION(R.string.toppers_batch_revision) }
enum class BatchGate { LOADING, READY, PREMIUM, UNAVAILABLE, ERROR }

data class BatchUiState(
    val studyMode: BatchStudyMode = BatchStudyMode.OFFICIAL,
    val studyDay: String = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString(),
    val lecturePages: Map<String, Int> = emptyMap(),
    val libraryQuery: String = "",
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val gate: BatchGate = BatchGate.LOADING,
    val overview: BatchOverview? = null,
    val today: BatchToday? = null,
    val calendar: BatchCalendar? = null,
    val section: BatchSection = BatchSection.TODAY,
    val subjectReturnSection: BatchSection = BatchSection.COURSES,
    val subjectReturnTodayId: String? = null,
    val selectedSubjectId: String? = null,
    val todaySubjectId: String? = null,
    val focusedLectureId: String? = null,
    val lectureTab: LectureTab = LectureTab.LECTURES,
    val month: String = YearMonth.now(ZoneId.of("Asia/Kolkata")).toString(),
    val selectedDay: String = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString(),
    val busyIds: Set<String> = emptySet(),
    val error: BatchNotice? = null,
    val message: BatchNotice? = null,
    val celebratingId: String? = null,
    val completionFeedback: BatchCompletionFeedback? = null,
)

@HiltViewModel
class ToppersBatchViewModel @Inject constructor(private val repository: ToppersBatchRepository) : ViewModel() {
    private val _state = MutableStateFlow(BatchUiState())
    private val subjectColourRevision = AtomicLong()
    val state = _state.asStateFlow()

    private var preferredStudyMode: BatchStudyMode? = null
    private val modeWrites = Mutex()
    private val loads = Mutex()

    init { open() }

    fun open() = viewModelScope.launch {
        if (!loads.tryLock()) return@launch
        try {
            preferredStudyMode = repository.studyMode()
            val cached = repository.cachedOverview()
            _state.update { it.copy(gate = if (cached == null) BatchGate.LOADING else BatchGate.READY,
                overview = cached, today = cached?.todayEvents(indiaDay()),
                studyMode = preferredStudyMode ?: cached?.defaultStudyMode() ?: BatchStudyMode.OFFICIAL, error = null) }
            val before = _state.value.overview
            val colourRevision = subjectColourRevision.get()
            when (val result = repository.open()) {
                is Resource.Success -> if (_state.value.busyIds.isEmpty() && _state.value.overview === before && subjectColourRevision.get() == colourRevision) {
                    _state.update { it.copy(gate = BatchGate.READY, overview = result.data, today = result.data.todayEvents(indiaDay()),
                        studyMode = preferredStudyMode ?: result.data.defaultStudyMode(), error = null) }
                }
                is Resource.Error -> {
                    if (result.errorCode in setOf("PREMIUM_REQUIRED", "PLANNER_V2_NOT_AVAILABLE", "PLANNER_V2_NOT_ENABLED") || result.code == 401) {
                        repository.clearCache()
                        _state.update { it.copy(overview = null, today = null) }
                        applyError(result)
                    } else if (cached != null) _state.update { it.copy(error = batchErrorNotice(result)) }
                    else applyError(result)
                }
                else -> Unit
            }
        } finally { loads.unlock() }
    }

    private fun applyError(error: Resource.Error<*>) {
        val gate = when (error.errorCode) {
            "PREMIUM_REQUIRED" -> BatchGate.PREMIUM
            "PLANNER_V2_NOT_AVAILABLE" -> BatchGate.UNAVAILABLE
            else -> BatchGate.ERROR
        }
        _state.update { it.copy(gate = gate, error = batchErrorNotice(error)) }
    }

    fun refresh() = viewModelScope.launch { reload() }
    fun refreshReleaseSchedule() = viewModelScope.launch {
        updateStudyDay()
        if (_state.value.gate == BatchGate.READY && _state.value.busyIds.isEmpty() && !loads.isLocked) reload()
    }

    private fun updateStudyDay() {
        val day = indiaDay()
        _state.update { current -> if (current.studyDay == day) current else current.copy(
            studyDay = day, today = current.overview?.todayEvents(day),
        ) }
    }

    private suspend fun reload() = loads.withLock {
        updateStudyDay()
        val before = _state.value.overview
        val colourRevision = subjectColourRevision.get()
        when (val result = repository.overview()) {
            is Resource.Success -> if (_state.value.busyIds.isEmpty() && _state.value.overview === before && subjectColourRevision.get() == colourRevision) {
                _state.update { old ->
                    val subjectExists = result.data.subjects.any { it.id == old.selectedSubjectId }
                    old.copy(overview = result.data, today = result.data.todayEvents(indiaDay()),
                        selectedSubjectId = old.selectedSubjectId.takeIf { subjectExists },
                        todaySubjectId = old.todaySubjectId?.takeIf { id -> result.data.subjects.any { it.id == id } }, error = null)
                }
            }
            is Resource.Error -> {
                if (result.code == 401 || result.errorCode in setOf("PREMIUM_REQUIRED", "PLANNER_V2_NOT_AVAILABLE", "PLANNER_V2_NOT_ENABLED")) {
                    repository.clearCache()
                    _state.update { it.copy(overview = null, today = null) }
                    applyError(result)
                } else _state.update { it.copy(error = batchErrorNotice(result)) }
            }
            else -> Unit
        }
    }

    fun selectStudyMode(mode: BatchStudyMode) {
        preferredStudyMode = mode
        _state.update { it.copy(studyMode = mode, todaySubjectId = null, focusedLectureId = null, error = null, message = null) }
        viewModelScope.launch { modeWrites.withLock { repository.saveStudyMode(_state.value.studyMode) } }
    }

    fun select(section: BatchSection) {
        _state.update { it.copy(section = section, selectedSubjectId = null, todaySubjectId = null, subjectReturnTodayId = null, error = null, message = null) }
        when (section) {
            BatchSection.TODAY -> viewModelScope.launch { refreshToday() }
            else -> Unit
        }
    }
    fun selectSubject(id: String, tab: LectureTab = LectureTab.LECTURES) =
        _state.update { it.copy(subjectReturnSection = if (it.selectedSubjectId != null) it.subjectReturnSection else it.section,
            subjectReturnTodayId = if (it.selectedSubjectId != null) it.subjectReturnTodayId else it.todaySubjectId,
            section = BatchSection.COURSES, selectedSubjectId = id, todaySubjectId = null,
            lectureTab = tab, error = null) }
    fun focusTodaySubject(id: String, lectureId: String? = null) =
        _state.update { current -> current.copy(section = BatchSection.TODAY, todaySubjectId = id, selectedSubjectId = null,
            focusedLectureId = lectureId ?: current.overview?.nextStudyLecture(id, current.studyMode, indiaDay())?.id,
            completionFeedback = null, error = null) }
    fun continueTodayLecture(subjectId: String) = _state.update { current -> current.copy(
        focusedLectureId = current.overview?.nextStudyLecture(subjectId, current.studyMode, indiaDay())?.id,
        completionFeedback = null, celebratingId = null,
    ) }
    fun backFromSubject() = _state.update { current ->
        if (current.todaySubjectId != null) current.copy(todaySubjectId = null)
        else current.copy(section = current.subjectReturnSection, todaySubjectId = current.subjectReturnTodayId,
            selectedSubjectId = null, subjectReturnTodayId = null, lectureTab = LectureTab.LECTURES)
    }
    fun backToCourses() = _state.update { it.copy(selectedSubjectId = null, lectureTab = LectureTab.LECTURES) }
    fun searchLibrary(query: String) = _state.update { it.copy(libraryQuery = query, lecturePages = it.lecturePages - "library-results") }
    fun filterLibrary(filter: LibraryFilter) = _state.update { it.copy(libraryFilter = filter, lecturePages = it.lecturePages - "library-results") }
    fun selectLecturePage(key: String, page: Int) = _state.update { it.copy(lecturePages = it.lecturePages + (key to page.coerceAtLeast(0))) }
    fun selectLectureTab(tab: LectureTab) = _state.update { it.copy(lectureTab = tab) }
    fun goToCalendarDay(date: String) {
        val valid = calendarDate(date) ?: return
        _state.update { it.copy(month = valid.take(7), selectedDay = valid) }
    }
    fun selectDay(date: String) = _state.update { it.copy(selectedDay = date) }
    fun changeMonth(delta: Long) {
        val month = runCatching { YearMonth.parse(_state.value.month).plusMonths(delta) }.getOrNull() ?: return
        _state.update { it.copy(month = month.toString(), selectedDay = month.atDay(1).toString()) }
    }
    fun currentMonth() {
        val today = indiaDay()
        _state.update { it.copy(month = today.take(7), selectedDay = today) }
    }
    fun clearNotice() = _state.update { it.copy(error = null, message = null, completionFeedback = null) }

    private fun refreshToday() {
        _state.update { it.copy(today = it.overview?.todayEvents(indiaDay())) }
    }
    private suspend fun refreshCalendar() {
        val month = _state.value.month
        // JavaScript's getTimezoneOffset() is the negative of ZoneOffset.totalSeconds.
        val offset = -ZoneId.of("Asia/Kolkata").rules.getOffset(Instant.now()).totalSeconds / 60
        when (val result = repository.calendar(month, offset)) {
            is Resource.Success -> if (_state.value.month == month) _state.update { it.copy(calendar = result.data) }
            is Resource.Error -> _state.update { it.copy(error = batchErrorNotice(result)) }
            else -> Unit
        }
    }

    fun undoCompletion(feedback: BatchCompletionFeedback) {
        val current = _state.value
        if (current.completionFeedback?.event != feedback.event) return
        val lecture = current.overview?.lectures?.firstOrNull { it.id == feedback.lectureId } ?: return
        if (lecture.completedAt == null || lecture.id in current.busyIds) return
        toggleDone(lecture)
    }

    fun toggleDone(lecture: BatchLecture) {
        if (lecture.id in _state.value.busyIds) return
        if (lecture.isLocked(indiaDay())) {
            _state.update { it.copy(message = BatchNotice(R.string.toppers_batch_unlocks_count, BatchDateArgument(lecture.scheduledFor?.take(10)))) }
            return
        }
        val done = lecture.completedAt == null
        val before = _state.value
        val wasBacklog = before.overview?.watchList.orEmpty()
            .any { lecture.id in it.backlogLectureIds }
        _state.update { current -> current.copy(
            overview = current.overview?.withLecture(lecture.copy(completedAt = if (done) Instant.now().toString() else null, completionDateUnknown = false), indiaDay()),
            busyIds = current.busyIds + lecture.id, error = null,
            celebratingId = null, completionFeedback = null, message = null,
        ) }
        viewModelScope.launch {
            val result = repository.action(lecture.id, if (done) "complete" else "uncomplete")
            when (result) {
                is Resource.Success -> _state.update { current ->
                    val updated = current.overview?.withLecture(result.data.lecture, indiaDay())
                    current.copy(
                        overview = updated,
                        celebratingId = if (done) lecture.id else null,
                        completionFeedback = if (done) BatchCompletionFeedback(
                            lecture.id, lecture.displayTopic,
                            batchCompletionMessage(lecture, updated, wasBacklog, indiaDay()),
                            System.nanoTime()
                        ) else null,
                        message = if (done) null else BatchNotice(R.string.toppers_batch_lecture_marked_as_not_done),
                    )
                }
                is Resource.Error -> _state.update { current -> current.copy(
                    overview = current.overview?.withLecture(lecture, indiaDay()), error = batchErrorNotice(result),
                    celebratingId = null, completionFeedback = if (!done) before.completionFeedback else current.completionFeedback,
                ) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - lecture.id) }
            // The class and calendar lists are separate API responses; update them only after save.
            if (result is Resource.Success) {
                if (_state.value.section == BatchSection.TODAY) refreshToday()
            }
            if (done) {
                val feedbackEvent = _state.value.completionFeedback?.takeIf { it.lectureId == lecture.id }?.event
                delay(1800)
                _state.update { if (it.celebratingId == lecture.id) it.copy(celebratingId = null) else it }
                delay(6200)
                _state.update { if (feedbackEvent != null && it.completionFeedback?.event == feedbackEvent) it.copy(completionFeedback = null) else it }
            }
        }
    }

    private fun <T> change(id: String, successText: BatchNotice, request: suspend () -> Resource<T>, onSuccess: () -> Unit = {}) {
        if (id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + id, error = null, message = null) }
        viewModelScope.launch {
            when (val result = request()) {
                is Resource.Success -> {
                    onSuccess()
                    _state.update { it.copy(message = successText) }
                    _state.update { it.copy(busyIds = it.busyIds - id) }
                    reload()
                }
                is Resource.Error -> {
                    // Refresh stale plans without dismissing the student's panel or hiding the error.
                    val refreshed = repository.overview()
                    _state.update { it.copy(overview = (refreshed as? Resource.Success)?.data ?: it.overview, error = batchErrorNotice(result)) }
                }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - id) }
        }
    }

    fun savePlan(lecture: BatchLecture, body: Map<String, Any>, onSuccess: () -> Unit) {
        val date = (body["dates"] as? List<*>)?.firstOrNull() as? String ?: body["date"] as? String
        val message = when {
            body["remove"] == true -> BatchNotice(R.string.toppers_batch_plan_removed)
            body["kind"] == "watch" && date != null -> BatchNotice(R.string.toppers_batch_lecture_planned_for_count, BatchDateArgument(date))
            else -> BatchNotice(R.string.toppers_batch_plan_saved)
        }
        change(lecture.id, message, {
            repository.savePlan(lecture.id, body + ("version" to lecture.planVersion)).also { result ->
                if (result is Resource.Success) _state.update { current ->
                    val updated = current.overview?.withLecture(result.data.lecture, indiaDay())
                    current.copy(overview = updated, today = updated?.todayEvents(indiaDay()))
                }
            }
        }, onSuccess)
    }

    fun addToToday(lecture: BatchLecture) {
        if (_state.value.studyMode != BatchStudyMode.PERSONAL) return
        updateStudyDay()
        val day = indiaDay()
        val current = _state.value.overview?.lectures?.firstOrNull { it.id == lecture.id } ?: return
        if (current.completedAt != null || current.isLocked(day) || current.studyPlannedFor?.let { it >= day } == true) return
        savePlan(current, mapOf("kind" to "watch", "dates" to listOf(day))) {}
    }
    internal fun removeTodayTask(task: TodayPlanTask, onSuccess: () -> Unit = {}) {
        updateStudyDay()
        val current = _state.value.overview?.lectures?.firstOrNull { it.id == task.lecture.id } ?: return
        val day = _state.value.studyDay
        if (_state.value.studyMode != BatchStudyMode.PERSONAL) return
        val stillPlanned = when (task.kind) {
            "watch" -> current.completedAt == null && current.studyPlannedFor == day
            "revision" -> current.completedAt != null && current.sessions.ifEmpty {
                current.revisionDate?.let { listOf(ReviewSession(it, current.revisionCompletedAt)) }.orEmpty()
            }.any { it.date == day && it.completedAt == null }
            else -> false
        }
        if (stillPlanned) savePlan(current, mapOf("kind" to task.kind, "remove" to true), onSuccess)
    }

    fun reschedulePlans(date: String, items: List<Map<String, Any>>, onSuccess: () -> Unit) =
        change("reschedule", BatchNotice(R.string.toppers_batch_plans_rescheduled), { repository.reschedule(date, items) }, onSuccess)
    fun importPriorProgress(ids: List<String>, date: String?, onSuccess: () -> Unit) =
        change("prior-progress", BatchNotice(R.string.toppers_batch_previous_progress_saved), { repository.priorProgress(ids, date) }, onSuccess)

    fun lectureAction(lecture: BatchLecture, action: String, body: Map<String, Any> = emptyMap(), remove: Boolean = false,
                      onSuccess: () -> Unit = {}) {
        if (lecture.isLocked(indiaDay()) && action in setOf("today", "backlog", "revision", "revision/complete", "complete")) return
        val text = when (action) {
            "revision" -> BatchNotice(R.string.toppers_batch_study_dates_saved)
            "revision/complete" -> BatchNotice(R.string.toppers_batch_revision_completed)
            "backlog" -> if (remove) BatchNotice(R.string.toppers_batch_removed_from_backlog) else BatchNotice(R.string.toppers_batch_added_to_backlog)
            "study-date" -> if (remove) BatchNotice(R.string.toppers_batch_personal_study_date_cleared) else BatchNotice(R.string.toppers_batch_personal_study_date_saved)
            "revision-tag" -> if (remove) BatchNotice(R.string.toppers_batch_revision_tag_removed) else BatchNotice(R.string.toppers_batch_revision_tag_added)
            "today" -> if (remove) BatchNotice(R.string.toppers_batch_class_date_cleared) else BatchNotice(R.string.toppers_batch_added_to_today)
            else -> BatchNotice(R.string.toppers_batch_saved)
        }
        if (action == "revision/complete") {
            val index = lecture.sessions.indexOfFirst { it.completedAt == null }
            change(lecture.id, text, { repository.completeRevision(lecture.id, index, lecture.planVersion) }, onSuccess)
        } else if (remove) change(lecture.id, text, { repository.removeAction(lecture.id, action) }, onSuccess)
        else change(lecture.id, text, { repository.action(lecture.id, action, body) }, onSuccess)
    }
    fun editLecture(lecture: BatchLecture, title: String, onSuccess: () -> Unit) =
        change(lecture.id, BatchNotice(R.string.toppers_batch_lecture_name_saved), { repository.editLecture(lecture.id, mapOf("displayTopic" to title.trim())) }, onSuccess)
    fun lectureColor(lecture: BatchLecture, color: String?) {
        if (lecture.id in _state.value.busyIds) return
        val previous = _state.value.overview?.lectures?.firstOrNull { it.id == lecture.id }?.color
        _state.update { it.withLectureColor(lecture.id, color).copy(busyIds = it.busyIds + lecture.id, error = null) }
        viewModelScope.launch {
            when (val result = repository.editLecture(lecture.id, mapOf("color" to color))) {
                is Resource.Success -> Unit // The colour is already visible; no full-library reload.
                is Resource.Error -> _state.update { it.withLectureColor(lecture.id, previous).copy(error = batchErrorNotice(result)) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - lecture.id) }
        }
    }
    fun removeLecture(lecture: BatchLecture, onSuccess: () -> Unit) =
        change(lecture.id, BatchNotice(R.string.toppers_batch_lecture_deleted_you_can_restore_it_in_lectures), { repository.removeLecture(lecture.id) }, onSuccess)
    fun restoreLecture(lecture: BatchLecture) = change(lecture.id, BatchNotice(R.string.toppers_batch_lecture_restored), { repository.restoreLecture(lecture.id) })
    fun subjectColor(subject: BatchSubject, color: String?) {
        val chosen = color ?: subject.defaultColor
        if (_state.value.overview?.subjects.orEmpty().any { it.id != subject.id && (it.color ?: it.defaultColor)?.equals(chosen, ignoreCase = true) == true }) {
            _state.update { it.copy(error = BatchNotice(R.string.toppers_batch_this_colour_is_used_by_another_subject_choose_a_different_colour)) }
            return
        }
        if (subject.id in _state.value.busyIds) return
        val previous = _state.value.overview?.subjects?.firstOrNull { it.id == subject.id }?.color
        fun BatchUiState.colour(value: String?) = copy(overview = overview?.copy(
            subjects = overview.subjects.map { if (it.id == subject.id) it.copy(color = value) else it }))
        subjectColourRevision.incrementAndGet()
        _state.update { it.colour(color).copy(busyIds = it.busyIds + subject.id, error = null) }
        viewModelScope.launch {
            val result = repository.editSubject(subject.id, mapOf("color" to color))
            // StateFlow suppresses equal values, so identity alone cannot detect a completed save.
            subjectColourRevision.incrementAndGet()
            when (result) {
                is Resource.Success -> _state.update { it.colour(result.data.color) }
                is Resource.Error -> _state.update { it.colour(previous).copy(error = batchErrorNotice(result)) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - subject.id) }
        }
    }
    fun subjectLink(subject: BatchSubject, provider: String, url: String, onSuccess: () -> Unit) =
        change(subject.id, BatchNotice(R.string.toppers_batch_course_link_saved), { repository.editSubject(subject.id,
            mapOf("externalProvider" to provider, "externalUrl" to url.trim())) }, onSuccess)
    fun addLecture(subject: BatchSubject, title: String, onSuccess: () -> Unit) =
        change(subject.id, BatchNotice(R.string.toppers_batch_lecture_added), { repository.addLecture(subject.id, title.trim()) }) {
            onSuccess()
            val datedWeeks = _state.value.overview?.lectures.orEmpty().filter { it.subjectId == subject.id }
                .mapNotNull { row -> row.scheduledFor?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
                .map { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.distinct().size
            selectSubject(subject.id)
            selectLecturePage("agenda-${subject.id}", datedWeeks)
        }
    fun removeSubject(subject: BatchSubject, onSuccess: () -> Unit) =
        change(subject.id, BatchNotice(R.string.toppers_batch_count_deleted_restore_it_in_lectures, BatchSubjectArgument(subject)), { repository.removeSubject(subject.id) }, onSuccess)
    fun restoreSubject(subject: BatchSubject) =
        change(subject.id, BatchNotice(R.string.toppers_batch_count_restored, BatchSubjectArgument(subject)), { repository.restoreSubject(subject.id) })
    companion object {
        private val india = ZoneId.of("Asia/Kolkata")
        fun indiaDay(): String = LocalDate.now(india).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

/** Apply only the style property; keep concurrent completion and revision edits intact. */
internal fun BatchUiState.withLectureColor(id: String, color: String?): BatchUiState {
    fun BatchLecture.recolour() = if (this.id == id) copy(color = color) else this
    fun Map<String, List<BatchLecture>>.recolour() = mapValues { (_, rows) -> rows.map { it.recolour() } }
    return copy(
        overview = overview?.copy(lectures = overview.lectures.map { it.recolour() }),
        today = today?.copy(lectures = today.lectures.map { it.recolour() }, backlogPick = today.backlogPick?.recolour()),
        calendar = calendar?.copy(days = calendar.days.recolour(), classes = calendar.classes.recolour()),
    )
}
