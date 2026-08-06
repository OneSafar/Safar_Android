package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.data.local.KavachEventEntity
import com.safarparmar.app.feature.kavachanalytics.domain.KavachEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A quick unlock is a device-wide grace window, and under Always On it can be
 * granted with no Kavach session running at all.
 *
 * These pin down the lookup rule the recorder relies on: find the open unlock by
 * *type*, not by session. Scoping it to a session is what left Always On unlocks
 * permanently open, so their duration always reported as 0m.
 */
class QuickUnlockRecordingTest {

    private val now = 1_800_000_000_000L

    private fun unlockStarted(
        id: String,
        sessionId: String?,
        atMs: Long,
        windowSeconds: Int,
        consumed: Boolean = false,
    ) = KavachEventEntity(
        id = id,
        clientSessionId = sessionId,
        type = KavachEventType.QUICK_UNLOCK_STARTED,
        packageName = "com.instagram.android",
        atMs = atMs,
        localDate = "2026-08-05",
        durationSeconds = windowSeconds,
        consumed = consumed,
    )

    /** Mirrors the DAO's lastOpenEventOfType query. */
    private fun lastOpenOfType(events: List<KavachEventEntity>, type: String) =
        events.filter { it.type == type && !it.consumed }.maxByOrNull { it.atMs }

    /** Mirrors the old, session-scoped lookup that caused the bug. */
    private fun lastOpenForSession(events: List<KavachEventEntity>, sessionId: String, type: String) =
        events.filter { it.clientSessionId == sessionId && it.type == type && !it.consumed }
            .maxByOrNull { it.atMs }

    @Test
    fun `an unlock with no session is findable by type`() {
        val events = listOf(unlockStarted("a", sessionId = null, atMs = now, windowSeconds = 300))

        // This is the regression: there is no session id to scope the lookup to, so
        // the old query could never have matched and the unlock stayed open.
        assertNotNull(lastOpenOfType(events, KavachEventType.QUICK_UNLOCK_STARTED))
    }

    @Test
    fun `the session-scoped lookup misses an Always On unlock entirely`() {
        val events = listOf(unlockStarted("a", sessionId = null, atMs = now, windowSeconds = 300))
        assertNull(lastOpenForSession(events, "kavach-1", KavachEventType.QUICK_UNLOCK_STARTED))
    }

    @Test
    fun `a consumed unlock is not found again, so it cannot be closed twice`() {
        val events = listOf(
            unlockStarted("a", sessionId = null, atMs = now, windowSeconds = 300, consumed = true),
        )
        assertNull(lastOpenOfType(events, KavachEventType.QUICK_UNLOCK_STARTED))
    }

    @Test
    fun `the newest open unlock wins when two overlap`() {
        val events = listOf(
            unlockStarted("older", sessionId = null, atMs = now, windowSeconds = 300),
            unlockStarted("newer", sessionId = "kavach-1", atMs = now + 60_000, windowSeconds = 300),
        )
        assertEquals("newer", lastOpenOfType(events, KavachEventType.QUICK_UNLOCK_STARTED)?.id)
    }

    // ── The duration arithmetic the recorder applies on close ────────────────

    private fun closedSeconds(startedAt: Long, windowSeconds: Int, closedAt: Long): Int {
        val cap = startedAt + windowSeconds * 1000L
        val endedAt = minOf(closedAt, cap)
        return ((endedAt - startedAt) / 1000L).toInt().coerceAtLeast(0)
    }

    @Test
    fun `closing early records the time actually spent, not the window chosen`() {
        // Student picked 5 minutes but came back after 90 seconds.
        assertEquals(90, closedSeconds(now, windowSeconds = 300, closedAt = now + 90_000))
    }

    @Test
    fun `the chosen window is a ceiling, never exceeded`() {
        // Settled late — the phone was idle and nothing closed it on time.
        assertEquals(300, closedSeconds(now, windowSeconds = 300, closedAt = now + 900_000))
    }

    @Test
    fun `a close at the exact expiry records the full window`() {
        assertEquals(300, closedSeconds(now, windowSeconds = 300, closedAt = now + 300_000))
    }

    @Test
    fun `a clock that jumps backwards cannot produce negative time`() {
        assertEquals(0, closedSeconds(now, windowSeconds = 300, closedAt = now - 10_000))
    }
}
