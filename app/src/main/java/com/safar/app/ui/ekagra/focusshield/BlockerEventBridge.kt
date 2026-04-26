package com.safar.app.ui.ekagra.focusshield

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight event bus that the AccessibilityService uses to notify the
 * Compose UI that a blocked app was intercepted. Lives as a singleton so
 * both the service (non-Hilt) and the Compose tree can reach it.
 */
object BlockerEventBridge {

    /** Emitted when a blocked app is intercepted. Payload = package name. */
    private val _blockedEvents = MutableSharedFlow<BlockedEvent>(extraBufferCapacity = 8)
    val blockedEvents: SharedFlow<BlockedEvent> = _blockedEvents.asSharedFlow()

    fun emit(event: BlockedEvent) {
        _blockedEvents.tryEmit(event)
    }

    data class BlockedEvent(
        val packageName: String,
        val appName: String,
        val timestamp: Long = System.currentTimeMillis(),
    )
}
