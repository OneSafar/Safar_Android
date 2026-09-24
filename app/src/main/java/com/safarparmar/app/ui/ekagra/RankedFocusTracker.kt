package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.RankedFocusRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Events stay ordered. Failed/offline events are never replayed as attendance. */
sealed interface RankedFocusStatus {
    data object ConnectionNeeded : RankedFocusStatus
    data object AttendanceExpired : RankedFocusStatus
    data class RankedTime(val minutes: Int) : RankedFocusStatus
    data object Unavailable : RankedFocusStatus
    data object Offline : RankedFocusStatus
}

internal class RankedFocusTracker(api: FocusApi, scope: CoroutineScope) {
    private data class Event(val id: String, val running: Boolean, val confirm: Boolean, val close: Boolean, val queuedAt: Long)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val checkpoints = mutableMapOf<String, String>()
    private val _status = MutableStateFlow<RankedFocusStatus>(RankedFocusStatus.ConnectionNeeded)
    val status: StateFlow<RankedFocusStatus> = _status

    init {
        scope.launch {
            for (event in events) {
                if (!event.close && System.nanoTime() / 1_000_000 - event.queuedAt > 120_000) continue
                try {
                    // Fetch a current checkpoint after process recreation. This does not
                    // acknowledge attendance; only this explicit confirm event can do so.
                    if (event.confirm && checkpoints[event.id] == null) {
                        val state = api.updateRankedFocus(RankedFocusRequest(event.id, event.running)).body()
                        state?.checkpoint?.let { checkpoints[event.id] = it }
                    }
                    val response = api.updateRankedFocus(RankedFocusRequest(
                        event.id, event.running,
                        confirm = if (event.confirm) checkpoints[event.id] else null,
                        close = event.close,
                    ))
                    val state = response.body()
                    if (response.isSuccessful && state != null) {
                        state.checkpoint?.let { checkpoints[event.id] = it }
                        _status.value = if (state.attendancePaused && !event.close)
                            RankedFocusStatus.AttendanceExpired
                        else RankedFocusStatus.RankedTime(state.rankedSeconds / 60)
                    } else {
                        _status.value = RankedFocusStatus.Unavailable
                    }
                    if (event.close) checkpoints.remove(event.id)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    _status.value = RankedFocusStatus.Offline
                }
            }
        }
    }

    fun update(id: String, running: Boolean, confirm: Boolean = false, close: Boolean = false) {
        events.trySend(Event(id, running, confirm, close, System.nanoTime() / 1_000_000))
    }
}
