package com.safarparmar.app.ui.ekagra

/** A missing grant is retried on a new session, never repeatedly on pause/resume. */
internal data class EkagraBackgroundAccessPromptState(
    val promptedSessionId: String? = null,
    val visible: Boolean = false,
    val waitingForSettings: Boolean = false,
) {
    fun sessionChanged(sessionId: String?, runningStudy: Boolean, granted: Boolean): EkagraBackgroundAccessPromptState {
        if (granted || !runningStudy) return copy(visible = false, waitingForSettings = false)
        if (sessionId.isNullOrBlank() || sessionId == promptedSessionId) return this
        return copy(promptedSessionId = sessionId, visible = true, waitingForSettings = false)
    }

    fun openSettings() = copy(visible = false, waitingForSettings = true)
    fun settingsReturned(granted: Boolean): EkagraBackgroundAccessPromptState = when {
        granted -> dismiss()
        waitingForSettings -> copy(visible = true, waitingForSettings = false)
        else -> this
    }
    fun dismiss() = copy(visible = false, waitingForSettings = false)
}
