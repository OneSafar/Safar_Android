package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats

/** Server/client identity is authoritative; never match unrelated sessions by duration/title. */
internal fun mergeJournalHistory(server: EkagraAnalyticsStats, local: List<PendingEkagraSessionSave>): EkagraAnalyticsStats {
    val ids = server.focusSessions.flatMap { listOfNotNull(it.id, it.sourceSessionId) }.toSet()
    val additions = local.filter { it.mode !in listOf("short", "long") && it.clientSessionId !in ids && it.serverId !in ids }
        .distinctBy { it.clientSessionId }
        .map { session ->
            EkagraAnalyticsFocusSession(
                id = session.serverId ?: session.clientSessionId, sourceSessionId = session.clientSessionId,
                startedAt = session.startedAt, endedAt = session.endedAt,
                durationMinutes = session.plannedDurationMinutes, actualMinutes = session.actualDurationMinutes,
                actualSeconds = session.actualDurationSeconds ?: session.actualDurationMinutes * 60,
                taskText = session.taskTitle, associatedGoalId = session.goalId, isGoalLinked = session.goalId != null,
                timerMode = session.mode, endReason = session.endReason, pendingSync = session.serverId == null,
            )
        }
    return server.copy(focusSessions = (server.focusSessions + additions).sortedByDescending { it.endedAt },
        totalFocusMinutes = server.totalFocusMinutes + additions.sumOf { it.actualMinutes },
        totalSessions = server.totalSessions + additions.size)
}
