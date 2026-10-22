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
    TODAY("Today"), COURSES("Lectures"), PROGRESS("Progress"), CALENDAR("Calendar"),
}
enum class LectureTab(val title: String) { LECTURES("Lectures"), OLDER("Backlog"), REVISION("Study again") }
enum class BatchGate { LOADING, READY, PREMIUM, UNAVAILABLE, ERROR }

data class BatchUiState(
    val gate: BatchGate = BatchGate.LOADING,
    val overview: BatchOverview? = null,
    val today: BatchToday? = null,
    val calendar: BatchCalendar? = null,
    val section: BatchSection = BatchSection.TODAY,
    val selectedSubjectId: String? = null,
    val todaySubjectId: String? = null,
    val lectureTab: LectureTab = LectureTab.LECTURES,
    val month: String = YearMonth.now(ZoneId.of("Asia/Kolkata")).toString(),
    val selectedDay: String = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString(),
    val busyIds: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null,
    val celebratingId: String? = null,
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
                    BatchSection.CALENDAR -> refreshCalendar()
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
            BatchSection.CALENDAR -> viewModelScope.launch { refreshCalendar() }
            else -> Unit
        }
    }
    fun selectSubject(id: String, tab: LectureTab = LectureTab.LECTURES) =
        _state.update { it.copy(section = BatchSection.COURSES, selectedSubjectId = id, todaySubjectId = null,
            lectureTab = tab, error = null) }
    fun focusTodaySubject(id: String) =
        _state.update { it.copy(section = BatchSection.TODAY, todaySubjectId = id, selectedSubjectId = null,
            error = null) }
    fun backFromSubject() = _state.update { current ->
        if (current.todaySubjectId != null) current.copy(todaySubjectId = null)
        else current.copy(selectedSubjectId = null, lectureTab = LectureTab.LECTURES)
    }
    fun backToCourses() = _state.update { it.copy(selectedSubjectId = null, lectureTab = LectureTab.LECTURES) }
    fun selectLectureTab(tab: LectureTab) = _state.update { it.copy(lectureTab = tab) }
    fun selectDay(date: String) = _state.update { it.copy(selectedDay = date) }
    fun changeMonth(delta: Long) {
        val month = runCatching { YearMonth.parse(_state.value.month).plusMonths(delta) }.getOrNull() ?: return
        _state.update { it.copy(month = month.toString(), selectedDay = month.atDay(1).toString()) }
        viewModelScope.launch { refreshCalendar() }
    }
    fun currentMonth() {
        val today = indiaDay()
        _state.update { it.copy(month = today.take(7), selectedDay = today) }
        viewModelScope.launch { refreshCalendar() }
    }
    fun clearNotice() = _state.update { it.copy(error = null, message = null) }

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
        val offset = -ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 60
        when (val result = repository.calendar(month, offset)) {
            is Resource.Success -> if (_state.value.month == month) _state.update { it.copy(calendar = result.data) }
            is Resource.Error -> _state.update { it.copy(error = result.message) }
            else -> Unit
        }
    }

    fun toggleDone(lecture: BatchLecture) {
        if (lecture.id in _state.value.busyIds) return
        val done = lecture.completedAt == null
        _state.update { current -> current.copy(
            overview = current.overview?.withLecture(lecture.copy(completedAt = if (done) Instant.now().toString() else null), indiaDay()),
            busyIds = current.busyIds + lecture.id, error = null,
            celebratingId = if (done) lecture.id else null,
        ) }
        viewModelScope.launch {
            val result = repository.action(lecture.id, if (done) "complete" else "uncomplete")
            when (result) {
                is Resource.Success -> _state.update { current -> current.copy(
                    overview = current.overview?.withLecture(result.data.lecture, indiaDay()),
                    message = if (done) "Lecture done. You can plan to study it again." else "Lecture marked as not done.",
                ) }
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
                if (_state.value.section == BatchSection.CALENDAR) refreshCalendar()
            }
            if (done) {
                delay(650)
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
        val text = when (action) {
            "revision" -> "Study dates saved."
            "revision/complete" -> "Study again marked done."
            "backlog" -> if (remove) "Removed from backlog." else "Added to backlog."
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
    fun lectureColor(lecture: BatchLecture, color: String?) =
        change(lecture.id, "Lecture colour saved.", { repository.editLecture(lecture.id, mapOf("color" to color)) })
    fun removeLecture(lecture: BatchLecture, onSuccess: () -> Unit) =
        change(lecture.id, "Lecture deleted. You can restore it in Lectures.", { repository.removeLecture(lecture.id) }, onSuccess)
    fun restoreLecture(lecture: BatchLecture) = change(lecture.id, "Lecture restored.", { repository.restoreLecture(lecture.id) })
    fun subjectColor(subject: BatchSubject, color: String?) =
        change(subject.id, "Subject colour saved.", { repository.editSubject(subject.id, mapOf("color" to color)) })
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
