package com.safarparmar.app.widget

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.ui.ekagra.PendingEkagraSessionSave
import com.safarparmar.app.ui.ekagra.mergeJournalHistory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SafarWidgetMetricsTest {
    private val today = LocalDate.of(2026, 9, 27)

    @Test fun focusUsesActualSecondsAndIndiaDay() {
        val sessions = listOf(
            EkagraAnalyticsFocusSession(id = "a", endedAt = "2026-09-26T20:00:00Z", actualSeconds = 95, actualMinutes = 2),
            EkagraAnalyticsFocusSession(id = "b", endedAt = "2026-09-27T10:00:00Z", actualMinutes = 10),
            EkagraAnalyticsFocusSession(id = "c", endedAt = "2026-09-26T10:00:00Z", actualSeconds = 900),
            EkagraAnalyticsFocusSession(id = "d", endedAt = "2026-09-27T10:00:00Z", actualSeconds = 300, timerMode = "short"),
        )
        assertEquals(695L, widgetFocusSeconds(sessions, today))
    }

    @Test fun goalCountsExcludeOtherDaysDeletedCancelledAndFreeSessions() {
        fun goal(id: String) = Goal(id = id, scheduledDate = today.toString())
        val goals = listOf(
            goal("done").copy(status = "completed"),
            goal("pending"),
            goal("focus-linked").copy(source = "ekagra", completedViaFocus = true, completed = true),
            goal("free-session").copy(source = "ekagra"),
            goal("deleted").copy(deletedAt = "2026-09-27T10:00:00Z"),
            goal("cancelled").copy(status = "cancelled"),
            goal("tomorrow").copy(scheduledDate = "2026-09-28"),
            goal("yesterday").copy(scheduledDate = "2026-09-26", completedAt = "2026-09-27T10:00:00Z"),
        )
        assertEquals(WidgetGoalCounts(2, 1), widgetGoalCounts(goals, today))
    }

    @Test fun legacyUndatedActiveGoalsStayActionable() {
        assertEquals(WidgetGoalCounts(0, 1), widgetGoalCounts(listOf(Goal()), today))
    }

    @Test fun overlappingProtectionDoesNotDoubleCount() {
        assertEquals(12L, widgetProtectedSeconds(listOf(0L to 10_000L, 5_000L to 12_000L), 0, 20_000))
    }

    @Test fun protectionClipsMidnightAndStopsAtLastHeartbeat() {
        assertEquals(8L, widgetProtectedSeconds(listOf(0L to 12_000L), 4_000, 20_000))
        assertEquals(6L, widgetProtectedSeconds(listOf(0L to 12_000L), 4_000, 10_000))
    }

    @Test fun invalidAndEmptyProtectionWindowsAreZero() {
        assertEquals(0L, widgetProtectedSeconds(listOf(9_000L to 1_000L), 0, 20_000))
        assertEquals(0L, widgetProtectedSeconds(emptyList(), 0, 20_000))
    }

    @Test fun uploadedSessionIsNotAddedAgainFromLocalQueue() {
        val local = PendingEkagraSessionSave(
            clientSessionId = "client-1", mode = "Timer", startedAt = "2026-09-27T08:00:00Z",
            endedAt = "2026-09-27T08:10:00Z", plannedDurationMinutes = 10, actualDurationMinutes = 10,
            actualDurationSeconds = 600, goalId = null, goalTitle = null, taskTitle = "Study", shieldEnabled = false,
        )
        val server = EkagraAnalyticsStats(focusSessions = listOf(EkagraAnalyticsFocusSession(
            id = "server-1", sourceSessionId = "client-1", endedAt = local.endedAt, actualSeconds = 600,
        )))
        assertEquals(600L, widgetFocusSeconds(mergeJournalHistory(server, listOf(local)).focusSessions, today))
        assertEquals(600L, widgetFocusSeconds(mergeJournalHistory(EkagraAnalyticsStats(), listOf(local)).focusSessions, today))
    }

    @Test fun bentoSnapshotSerializesAndMaintainsNewFields() {
        val snapshot = SafarWidgetSnapshot(
            owner = "user_123",
            day = "2026-10-23",
            focusSeconds = 14400L,
            protectedSeconds = 7200L,
            kavachUsed = true,
            completed = 3,
            pending = 1,
            nextGoalTitle = "Solve Electrostatics Module 2",
            streakDays = 21,
            userName = "Shashank",
            updatedAt = 1729680000000L,
            stale = false,
        )
        val json = com.google.gson.Gson().toJson(snapshot)
        val restored = com.google.gson.Gson().fromJson(json, SafarWidgetSnapshot::class.java)

        assertEquals("user_123", restored.owner)
        assertEquals("Solve Electrostatics Module 2", restored.nextGoalTitle)
        assertEquals(21, restored.streakDays)
        assertEquals("Shashank", restored.userName)
        assertEquals(14400L, restored.focusSeconds)
        assertEquals(true, restored.kavachUsed)
    }

    @Test fun legacyJsonDeserializesWithoutNewFieldsGracefully() {
        val legacyJson = """{"owner":"user_old","day":"2026-10-23","focusSeconds":3600,"completed":2,"pending":0}"""
        val restored = com.google.gson.Gson().fromJson(legacyJson, SafarWidgetSnapshot::class.java)

        assertEquals("user_old", restored.owner)
        assertEquals(null, restored.nextGoalTitle)
        assertEquals(null, restored.streakDays)
        assertEquals(null, restored.userName)
        assertEquals(3600L, restored.focusSeconds)
    }
}
