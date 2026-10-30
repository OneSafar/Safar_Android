package com.safarparmar.app.feature.habits.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel
class TimetableViewModel @Inject constructor(private val repository: TimetableRepository) : ViewModel() {
    private val pending = MutableStateFlow<Map<Triple<String, String, LocalDate>, Boolean>>(emptyMap())
    val sessions = combine(repository.sessions, repository.owner, pending) { records, user, ticks ->
        records.map { session ->
            val updates = ticks.filterKeys { it.first == user && it.second == session.id }
            session.copy(completedDates = updates.entries.fold(session.completedDates) { dates, (key, done) ->
                if (done) dates + key.third.toString() else dates - key.third.toString()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val owner = repository.owner.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val errors = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val messages = errors.receiveAsFlow()
    suspend fun save(session: TimetableSession) = repository.save(session)
    suspend fun delete(id: String) = repository.delete(id)
    fun toggle(id: String, date: LocalDate) {
        val user = owner.value ?: return
        val key = Triple(user, id, date)
        if (key in pending.value) return
        val session = sessions.value.find { it.id == id } ?: return
        val done = !session.isDone(date)
        if (date <= LocalDate.now() && session.occursOn(date)) pending.update { it + (key to done) }
        viewModelScope.launch {
            try {
                repository.toggle(id, date)
                // Keep feedback until the persisted record reaches the screen's flow.
                repository.sessions.first { values -> values.none { it.id == id } || values.find { it.id == id }?.isDone(date) == done }
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { errors.send(e.message ?: "Could not update timetable activity.") }
            finally { pending.update { it - key } }
        }
    }
}
