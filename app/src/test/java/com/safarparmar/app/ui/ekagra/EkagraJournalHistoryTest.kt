package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import org.junit.Test
import org.junit.Assert.*

class EkagraJournalHistoryTest {
    private val pending = PendingEkagraSessionSave("local-1", "Timer", "2026-09-23T10:00:00Z", "2026-09-23T13:00:00Z", 300, 180, 10800, null, null, taskTitle = "Untitled", shieldEnabled = false)
    @Test fun offlineEntryIsVisibleWithPreciseSeconds() {
        val result = mergeJournalHistory(EkagraAnalyticsStats(), listOf(pending))
        assertEquals(10800, result.focusSessions.single().actualSeconds)
        assertTrue(result.focusSessions.single().pendingSync)
        assertEquals(180, result.totalFocusMinutes)
    }
    @Test fun serverEchoBeforeAcknowledgementDoesNotDoubleCount() {
        val server = EkagraAnalyticsStats(totalFocusMinutes = 180, focusSessions = listOf(EkagraAnalyticsFocusSession(id = "server", sourceSessionId = "local-1", actualSeconds = 10800)))
        val result = mergeJournalHistory(server, listOf(pending))
        assertEquals(1, result.focusSessions.size)
        assertEquals(180, result.totalFocusMinutes)
    }
    @Test fun oldBackendMatchesAcknowledgedServerId() {
        val server = EkagraAnalyticsStats(focusSessions = listOf(EkagraAnalyticsFocusSession(id = "server")))
        assertEquals(1, mergeJournalHistory(server, listOf(pending.copy(serverId = "server"))).focusSessions.size)
    }
    @Test fun breaksDoNotBecomeFocusTime() {
        assertTrue(mergeJournalHistory(EkagraAnalyticsStats(), listOf(pending.copy(mode = "short"))).focusSessions.isEmpty())
    }
}
