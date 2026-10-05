package com.safarparmar.app.feature.toppersbatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

enum class BatchSection(val title: String) {
    TODAY("Today"), COURSES("Library"), PROGRESS("Progress"), CALENDAR("Calendar"),
}
enum class LectureTab(val title: String) { LECTURES("Lectures"), OLDER("Backlog"), REVISION("Study again") }
enum class BatchGate { LOADING, READY, PREMIUM, UNAVAILABLE, ERROR }

data class BatchUiState(
    val lecturePages: Map<String, Int> = emptyMap(),
    val libraryQuery: String = "",
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val gate: BatchGate = BatchGate.LOADING,
    val overview: BatchOverview? = null,
    val today: BatchToday? = null,
    val calendar: BatchCalendar? = null,
    val section: BatchSection = BatchSection.TODAY,
    val selectedSubjectId: String? = null,
    val todaySubjectId: String? = null,
    val focusedLectureId: String? = null,
    val lectureTab: LectureTab = LectureTab.LECTURES,
    val month: String = YearMonth.now(ZoneId.of("Asia/Kolkata")).toString(),
    val selectedDay: String = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString(),
    val busyIds: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null,
    val celebratingId: String? = null,
    val completionFeedback: BatchCompletionFeedback? = null,
)

@HiltViewModel
class ToppersBatchViewModel @Inject constructor(private val repository: ToppersBatchRepository) : ViewModel() {
    private val _state = MutableStateFlow(BatchUiState())
    val state = _state.asStateFlow()

    init { open() }

    fun open() = viewModelScope.launch {
        _state.update { it.copy(gate = BatchGate.LOADING, error = null) }
        when (val result = repository.status()) {
            is Resource.Success -> {
                if (!result.data.available) { _state.update { it.copy(gate = BatchGate.UNAVAILABLE) }; return@launch }
                val overview = if (result.data.enabled) repository.overview() else repository.activate()
                when (overview) {
                    is Resource.Success -> {
                        _state.update { it.copy(gate = BatchGate.READY, overview = overview.data) }
                        refreshToday()
                    }
                    is Resource.Error -> applyError(overview)
                    else -> Unit
                }
            }
            is Resource.Error -> applyError(result)
            else -> Unit
        }
    }

    private fun applyError(error: Resource.Error<*>) {
        val gate = when (error.errorCode) {
            "PREMIUM_REQUIRED" -> BatchGate.PREMIUM
            "PLANNER_V2_NOT_AVAILABLE" -> BatchGate.UNAVAILABLE
            else -> BatchGate.ERROR
        }
        _state.update { it.copy(gate = gate, error = error.message) }
    }

    fun refresh() = viewModelScope.launch { reload() }
    fun refreshReleaseSchedule() = viewModelScope.launch {
        if (_state.value.gate == BatchGate.READY && _state.value.busyIds.isEmpty()) reload()
    }

    private suspend fun reload() {
        when (val result = repository.overview()) {
            is Resource.Success -> {
                _state.update { old ->
                    val subjectExists = result.data.subjects.any { it.id == old.selectedSubjectId }
                    old.copy(overview = result.data, selectedSubjectId = old.selectedSubjectId.takeIf { subjectExists },
                        todaySubjectId = old.todaySubjectId?.takeIf { id -> result.data.subjects.any { it.id == id } }, error = null)
                }
                when (_state.value.section) {
                    BatchSection.TODAY -> refreshToday()
                    else -> Unit
                }
            }
            is Resource.Error -> _state.update { it.copy(error = result.message) }
            else -> Unit
        }
    }

    fun select(section: BatchSection) {
        _state.update { it.copy(section = section, selectedSubjectId = null, todaySubjectId = null, error = null, message = null) }
        when (section) {
            BatchSection.TODAY -> viewModelScope.launch { refreshToday() }
            else -> Unit
        }
    }
    fun selectSubject(id: String, tab: LectureTab = LectureTab.LECTURES) =
        _state.update { it.copy(section = BatchSection.COURSES, selectedSubjectId = id, todaySubjectId = null,
            lectureTab = tab, error = null) }
    fun focusTodaySubject(id: String) =
        _state.update { current -> current.copy(section = BatchSection.TODAY, todaySubjectId = id, selectedSubjectId = null,
            focusedLectureId = current.overview?.watchList?.firstOrNull { it.subjectId == id }?.nextLectureId,
            completionFeedback = null, error = null) }
    fun continueTodayLecture(subjectId: String) = _state.update { current -> current.copy(
        focusedLectureId = current.overview?.watchList?.firstOrNull { it.subjectId == subjectId }?.nextLectureId,
        completionFeedback = null, celebratingId = null,
    ) }
    fun backFromSubject() = _state.update { current ->
        if (current.todaySubjectId != null) current.copy(todaySubjectId = null)
        else current.copy(selectedSubjectId = null, lectureTab = LectureTab.LECTURES)
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

    private suspend fun refreshToday() {
        when (val result = repository.today(indiaDay())) {
            is Resource.Success -> _state.update { it.copy(today = result.data) }
            is Resource.Error -> _state.update { it.copy(error = result.message) }
            else -> Unit
        }
    }
    private suspend fun refreshCalendar() {
        val month = _state.value.month
        // JavaScript's getTimezoneOffset() is the negative of ZoneOffset.totalSeconds.
        val offset = -ZoneId.of("Asia/Kolkata").rules.getOffset(Instant.now()).totalSeconds / 60
        when (val result = repository.calendar(month, offset)) {
            is Resource.Success -> if (_state.value.month == month) _state.update { it.copy(calendar = result.data) }
            is Resource.Error -> _state.update { it.copy(error = result.message) }
            else -> Unit
        }
    }

    fun toggleDone(lecture: BatchLecture) {
        if (lecture.id in _state.value.busyIds) return
        if (lecture.isLocked(indiaDay())) {
            _state.update { it.copy(message = "Unlocks ${lecture.scheduledFor?.take(10)}") }
            return
        }
        val done = lecture.completedAt == null
        val before = _state.value
        val wasBacklog = lecture.olderWork(indiaDay()) || before.overview?.watchList.orEmpty()
            .any { lecture.id in it.backlogLectureIds }
        _state.update { current -> current.copy(
            overview = current.overview?.withLecture(lecture.copy(completedAt = if (done) Instant.now().toString() else null), indiaDay()),
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
                        message = if (done) null else "Lecture marked as not done.",
                    )
                }
                is Resource.Error -> _state.update { current -> current.copy(
                    overview = current.overview?.withLecture(lecture, indiaDay()), error = result.message,
                    celebratingId = null,
                ) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - lecture.id) }
            // The class and calendar lists are separate API responses; update them only after save.
            if (result is Resource.Success) {
                if (_state.value.section == BatchSection.TODAY) refreshToday()
            }
            if (done) {
                delay(1800)
                _state.update { if (it.celebratingId == lecture.id) it.copy(celebratingId = null) else it }
            }
        }
    }

    private fun <T> change(id: String, successText: String, request: suspend () -> Resource<T>, onSuccess: () -> Unit = {}) {
        if (id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + id, error = null, message = null) }
        viewModelScope.launch {
            when (val result = request()) {
                is Resource.Success -> {
                    onSuccess()
                    _state.update { it.copy(message = successText) }
                    reload()
                }
                is Resource.Error -> _state.update { it.copy(error = result.message) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - id) }
        }
    }

    fun lectureAction(lecture: BatchLecture, action: String, body: Map<String, Any> = emptyMap(), remove: Boolean = false,
                      onSuccess: () -> Unit = {}) {
        if (lecture.isLocked(indiaDay()) && action in setOf("today", "backlog", "revision", "revision/complete", "complete")) return
        val text = when (action) {
            "revision" -> "Study dates saved."
            "revision/complete" -> "Study again marked done."
            "backlog" -> if (remove) "Removed from backlog." else "Added to backlog."
            "study-date" -> if (remove) "Personal study date cleared." else "Personal study date saved."
            "revision-tag" -> if (remove) "Revision tag removed." else "Revision tag added."
            "today" -> if (remove) "Class date cleared." else "Added to today."
            else -> "Saved."
        }
        if (action == "revision/complete") {
            val index = lecture.sessions.indexOfFirst { it.completedAt == null }
            change(lecture.id, text, { repository.completeRevision(lecture.id, index) }, onSuccess)
        } else if (remove) change(lecture.id, text, { repository.removeAction(lecture.id, action) }, onSuccess)
        else change(lecture.id, text, { repository.action(lecture.id, action, body) }, onSuccess)
    }
    fun editLecture(lecture: BatchLecture, title: String, onSuccess: () -> Unit) =
        change(lecture.id, "Lecture name saved.", { repository.editLecture(lecture.id, mapOf("displayTopic" to title.trim())) }, onSuccess)
    fun lectureColor(lecture: BatchLecture, color: String?) {
        if (lecture.id in _state.value.busyIds) return
        val previous = _state.value.overview?.lectures?.firstOrNull { it.id == lecture.id }?.color
        _state.update { it.withLectureColor(lecture.id, color).copy(busyIds = it.busyIds + lecture.id, error = null) }
        viewModelScope.launch {
            when (val result = repository.editLecture(lecture.id, mapOf("color" to color))) {
                is Resource.Success -> Unit // The colour is already visible; no full-library reload.
                is Resource.Error -> _state.update { it.withLectureColor(lecture.id, previous).copy(error = result.message) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - lecture.id) }
        }
    }
    fun removeLecture(lecture: BatchLecture, onSuccess: () -> Unit) =
        change(lecture.id, "Lecture deleted. You can restore it in Lectures.", { repository.removeLecture(lecture.id) }, onSuccess)
    fun restoreLecture(lecture: BatchLecture) = change(lecture.id, "Lecture restored.", { repository.restoreLecture(lecture.id) })
    fun subjectColor(subject: BatchSubject, color: String?) {
        if (subject.id in _state.value.busyIds) return
        val previous = _state.value.overview?.subjects?.firstOrNull { it.id == subject.id }?.color
        fun BatchUiState.colour(value: String?) = copy(overview = overview?.copy(
            subjects = overview.subjects.map { if (it.id == subject.id) it.copy(color = value) else it }))
        _state.update { it.colour(color).copy(busyIds = it.busyIds + subject.id, error = null) }
        viewModelScope.launch {
            when (val result = repository.editSubject(subject.id, mapOf("color" to color))) {
                is Resource.Success -> Unit
                is Resource.Error -> _state.update { it.colour(previous).copy(error = result.message) }
                else -> Unit
            }
            _state.update { it.copy(busyIds = it.busyIds - subject.id) }
        }
    }
    fun subjectLink(subject: BatchSubject, provider: String, url: String, onSuccess: () -> Unit) =
        change(subject.id, "Course link saved.", { repository.editSubject(subject.id,
            mapOf("externalProvider" to provider, "externalUrl" to url.trim())) }, onSuccess)
    fun removeSubject(subject: BatchSubject, onSuccess: () -> Unit) =
        change(subject.id, "${subject.name} deleted. Restore it in Lectures.", { repository.removeSubject(subject.id) }, onSuccess)
    fun restoreSubject(subject: BatchSubject) =
        change(subject.id, "${subject.name} restored.", { repository.restoreSubject(subject.id) })
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
