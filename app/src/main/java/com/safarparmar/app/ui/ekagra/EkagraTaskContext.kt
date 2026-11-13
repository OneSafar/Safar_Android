package com.safarparmar.app.ui.ekagra

/** Planner/goal attribution is owned by a running session, including after navigation. */
internal data class EkagraTaskContext(
    val goalId: String? = null,
    val goalTitle: String? = null,
    val topicId: String? = null,
    val planId: String? = null,
    val topicTitle: String? = null,
    val taskTitle: String? = null,
) {
    val hasLink: Boolean get() = !goalId.isNullOrBlank() || !topicId.isNullOrBlank()

    fun hasSameLink(other: EkagraTaskContext): Boolean =
        goalId == other.goalId && topicId == other.topicId && planId == other.planId
}

internal fun resolveEkagraTaskContext(
    requested: EkagraTaskContext,
    running: EkagraTaskContext?,
): EkagraTaskContext = running ?: requested

/** Natural completion already saved the time; the sheet only needs a completion choice. */
internal fun PendingEkagraSessionSave.toEndedPlannerSession(): PendingEndedEkagraSession {
    val actualSeconds = (actualDurationSeconds ?: actualDurationMinutes * 60).coerceAtLeast(0)
    val stopwatch = mode.equals("stopwatch", ignoreCase = true)
    val plannedSeconds = (plannedDurationMinutes * 60).coerceAtLeast(actualSeconds)
    return PendingEndedEkagraSession(
        sessionId = clientSessionId,
        totalSeconds = if (stopwatch) actualSeconds else plannedSeconds,
        secondsLeft = if (stopwatch) actualSeconds else plannedSeconds - actualSeconds,
        mode = mode,
        startedAt = startedAt,
        endedAt = endedAt,
        topicId = topicId,
        planId = planId,
        topicTitle = topicTitle,
    )
}
