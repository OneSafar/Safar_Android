package com.safarparmar.app.ui.ekagra

import com.google.gson.Gson
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EkagraTimeAccountingTest {

    @Test
    fun `two completed one-minute pomodoro loops aggregate to two minutes`() {
        val progress = calculateFocusProgress(
            mode = TimerMode.POMODORO,
            currentPeriodTotalSeconds = 60,
            currentPeriodRemainingSeconds = 0,
            pomodoroFocusSeconds = 60,
            targetPomodoroLoops = 2,
            completedPomodoroLoops = 1,
        )

        assertEquals(120, progress.plannedSeconds)
        assertEquals(120, progress.actualSeconds)
    }

    @Test
    fun `completed first loop plus six seconds of second loop equals sixty-six seconds`() {
        val progress = calculateFocusProgress(
            mode = TimerMode.POMODORO,
            currentPeriodTotalSeconds = 60,
            currentPeriodRemainingSeconds = 54,
            pomodoroFocusSeconds = 60,
            targetPomodoroLoops = 2,
            completedPomodoroLoops = 1,
        )

        assertEquals(120, progress.plannedSeconds)
        assertEquals(66, progress.actualSeconds)
    }

    @Test
    fun `history detail uses exact seconds instead of rounded zero minutes`() {
        val pending = EkagraAnalyticsFocusSession(
            id = "six-second-session",
            durationMinutes = 1,
            actualMinutes = 0,
            actualSeconds = 6,
            timerMode = "Timer",
        ).toPendingEndedSession()

        assertEquals(60, pending.totalSeconds)
        assertEquals(54, pending.secondsLeft)
        assertEquals(6, pending.totalSeconds - pending.secondsLeft)
    }

    @Test
    fun `history detail preserves cumulative exact seconds`() {
        val pending = EkagraAnalyticsFocusSession(
            id = "cumulative-session",
            durationMinutes = 2,
            actualMinutes = 1,
            actualSeconds = 66,
            timerMode = "Timer",
        ).toPendingEndedSession()

        assertEquals(120, pending.totalSeconds)
        assertEquals(54, pending.secondsLeft)
        assertEquals(66, pending.totalSeconds - pending.secondsLeft)
    }

    @Test
    fun `save request serializes client id using backend deduplication field`() {
        val json = Gson().toJson(
            SaveEkagraSessionRequest(
                clientSessionId = "session-123",
                mode = "Timer",
                startedAt = "2026-07-10T10:00:00Z",
                plannedDurationMinutes = 2,
                actualDurationMinutes = 1,
                actualDurationSeconds = 66,
            )
        )

        assertTrue(json.contains("\"clientSessionId\":\"session-123\""))
        assertFalse(json.contains("client_session_id"))
    }

    @Test
    fun `elapsed duration omits zero units while preserving seconds`() {
        assertEquals("30s", formatElapsedDuration(30))
        assertEquals("2m", formatElapsedDuration(120))
        assertEquals("2m 5s", formatElapsedDuration(125))
        assertEquals("1h 2m 5s", formatElapsedDuration(3725))
    }

    @Test
    fun `history aggregation can use timestamp seconds for older sessions`() {
        val session = EkagraAnalyticsFocusSession(
            startedAt = "2026-07-10T10:00:00Z",
            endedAt = "2026-07-10T10:00:30Z",
            actualMinutes = 0,
            actualSeconds = 0,
            timerMode = "stopwatch",
        )

        assertEquals(30L, exactElapsedSeconds(session))
    }
}
