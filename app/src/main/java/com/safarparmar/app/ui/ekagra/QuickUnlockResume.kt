package com.safarparmar.app.ui.ekagra

/** An unlock can resume only the session and grace window that paused it. */
internal data class QuickUnlockResume(
    val sessionId: String,
    val graceUntilMs: Long,
    val resumeAtElapsed: Long,
) {
    fun matches(sessionId: String?, graceUntilMs: Long): Boolean =
        this.sessionId == sessionId && this.graceUntilMs == graceUntilMs

    fun isDue(nowElapsed: Long): Boolean = nowElapsed >= resumeAtElapsed
}
