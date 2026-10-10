package com.safarparmar.app.ui.support

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.R
import com.safarparmar.app.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
class SupportViewModel @Inject constructor(
    private val api: SupportApi,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(SupportUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, message = null, loadError = false)
        runCatching {
            val availability = api.availability()
            val mine = api.mine()
            if (!availability.isSuccessful || !mine.isSuccessful) error("Unable to load support")
            _state.value.copy(
                loading = false,
                enabled = availability.body()?.enabled == true,
                ticket = mine.body()?.ticket,
                history = mine.body()?.history.orEmpty(),
            )
        }.onSuccess { _state.value = it }
            .onFailure {
                if (it is CancellationException) throw it
                _state.value = _state.value.copy(
                    loading = false,
                    enabled = false,
                    loadError = true,
                    message = context.getString(R.string.support_load_failed),
                )
            }
    }

    fun create(
        problem: String,
        topic: String = "other",
        urgency: String,
        phone: String,
        time: String,
        contactMethod: String = "phone",
        repeat: Boolean = false,
    ) = viewModelScope.launch {
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true, message = null)
        runCatching {
            val response = api.create(
                CreateSupportTicketRequest(
                    problemText = problem,
                    topic = topic,
                    urgency = urgency,
                    callbackPhone = phone,
                    contactPreference = time,
                    contactMethod = contactMethod,
                    isRepeatContact = repeat,
                )
            )
            if (!response.isSuccessful) {
                error(
                    if (response.code() == 409) context.getString(R.string.support_already_open)
                    else context.getString(R.string.support_check_answers)
                )
            }
            response.body()?.ticket ?: error(context.getString(R.string.support_empty_response))
        }.onSuccess {
            _state.value = _state.value.copy(
                saving = false,
                ticket = it,
                message = context.getString(R.string.support_request_sent),
            )
        }.onFailure {
            if (it is CancellationException) throw it
            _state.value = _state.value.copy(saving = false, message = it.message)
        }
    }

    fun escalate() = viewModelScope.launch {
        val ticket = _state.value.ticket ?: return@launch
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true)
        runCatching {
            api.escalate(ticket.id).body()?.ticket
                ?: error(context.getString(R.string.support_update_failed))
        }.onSuccess {
            _state.value = _state.value.copy(
                saving = false,
                ticket = it,
                message = context.getString(R.string.support_marked_urgent),
            )
        }.onFailure {
            if (it is CancellationException) throw it
            _state.value = _state.value.copy(saving = false, message = it.message)
        }
    }

    fun cancel() = viewModelScope.launch {
        val ticket = _state.value.ticket ?: return@launch
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true)
        runCatching {
            api.cancel(ticket.id).body()?.ticket
                ?: error(context.getString(R.string.support_update_failed))
        }.onSuccess { updated ->
            _state.value = _state.value.copy(
                saving = false,
                ticket = null,
                history = listOf(updated) + _state.value.history.filter { it.id != updated.id },
                message = context.getString(R.string.support_request_cancelled),
            )
        }.onFailure {
            if (it is CancellationException) throw it
            _state.value = _state.value.copy(saving = false, message = it.message)
        }
    }

    fun feedback(ticketId: String, rating: Int, comment: String) = viewModelScope.launch {
        if (_state.value.saving) return@launch
        _state.value = _state.value.copy(saving = true)
        runCatching {
            api.feedback(ticketId, SupportFeedbackRequest(rating, comment)).body()?.ticket
                ?: error(context.getString(R.string.support_feedback_failed))
        }.onSuccess { updated ->
            _state.value = _state.value.copy(
                saving = false,
                ticket = if (_state.value.ticket?.id == updated.id) updated else _state.value.ticket,
                history = _state.value.history.map { row -> if (row.id == updated.id) updated else row },
                message = context.getString(R.string.support_feedback_thanks),
            )
        }.onFailure {
            if (it is CancellationException) throw it
            _state.value = _state.value.copy(saving = false, message = it.message)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
