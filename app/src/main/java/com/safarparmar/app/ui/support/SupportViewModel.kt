package com.safarparmar.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class SupportUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val enabled: Boolean = false,
    val ticket: SupportTicketDto? = null,
    val history: List<SupportTicketDto> = emptyList(),
    val loadError: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SupportViewModel @Inject constructor(private val api: SupportApi) : ViewModel() {
    private val _state = MutableStateFlow(SupportUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, message = null, loadError = false)
        runCatching {
            val availability = api.availability()
            val mine = api.mine()
            if (!availability.isSuccessful || !mine.isSuccessful) error("Unable to load support")
            _state.value.copy(loading = false, enabled = availability.body()?.enabled == true, ticket = mine.body()?.ticket, history = mine.body()?.history.orEmpty())
        }.onSuccess { _state.value = it }.onFailure { if (it is CancellationException) throw it; _state.value = _state.value.copy(loading = false, enabled = false, loadError = true, message = "Could not load requests. Please retry.") }
    }

    fun create(problem: String, urgency: String, phone: String, time: String, repeat: Boolean) = viewModelScope.launch {
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true, message = null)
        runCatching {
            val response = api.create(CreateSupportTicketRequest(problem, urgency, phone, time, repeat))
            if (!response.isSuccessful) error(if (response.code() == 409) "You already have an open request" else "Please check your answers and try again")
            response.body()?.ticket ?: error("Empty response")
        }.onSuccess { _state.value = _state.value.copy(saving = false, ticket = it, message = "Request sent") }
            .onFailure { if (it is CancellationException) throw it; _state.value = _state.value.copy(saving = false, message = it.message) }
    }

    fun escalate() = viewModelScope.launch {
        val ticket = _state.value.ticket ?: return@launch
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true)
        runCatching { api.escalate(ticket.id).body()?.ticket ?: error("Could not update request") }
            .onSuccess { _state.value = _state.value.copy(saving = false, ticket = it, message = "Marked urgent") }
            .onFailure { if (it is CancellationException) throw it; _state.value = _state.value.copy(saving = false, message = it.message) }
    }

    fun feedback(ticketId: String, rating: Int, comment: String) = viewModelScope.launch {
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true)
        runCatching { api.feedback(ticketId, SupportFeedbackRequest(rating, comment)).body()?.ticket ?: error("Could not send feedback") }
            .onSuccess { _state.value = _state.value.copy(saving = false, history = _state.value.history.map { row -> if (row.id == it.id) it else row }, message = "Thank you for your feedback") }
            .onFailure { if (it is CancellationException) throw it; _state.value = _state.value.copy(saving = false, message = it.message) }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
