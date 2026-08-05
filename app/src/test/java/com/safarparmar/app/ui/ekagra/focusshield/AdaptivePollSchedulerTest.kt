package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Always On polls for the student's whole waking day, so how often it asks the OS
 * what is in front is the difference between a useful blocker and a battery
 * complaint. These pin down that it stays responsive when it matters and backs
 * off when it doesn't.
 */
class AdaptivePollSchedulerTest {

    private fun scheduler() = AdaptivePollScheduler()

    @Test
    fun `starts fast`() {
        assertEquals(AdaptivePollScheduler.FAST_MS, scheduler().intervalMs)
    }

    @Test
    fun `stays fast while the foreground app keeps changing`() {
        val poller = scheduler()
        listOf("com.a", "com.b", "com.c", "com.d").forEach { pkg ->
            assertEquals(
                AdaptivePollScheduler.FAST_MS,
                poller.onSample(pkg, isBlockedApp = false),
            )
        }
    }

    @Test
    fun `backs off once the same app has been in front for a while`() {
        val poller = scheduler()
        repeat(4) { poller.onSample("com.a", isBlockedApp = false) }
        // Still attentive immediately after the switch.
        assertEquals(AdaptivePollScheduler.FAST_MS, poller.intervalMs)

        repeat(20) { poller.onSample("com.a", isBlockedApp = false) }
        assertTrue(
            "expected backoff, still at ${poller.intervalMs}",
            poller.intervalMs > AdaptivePollScheduler.FAST_MS,
        )
    }

    @Test
    fun `never backs off past the slow ceiling`() {
        val poller = scheduler()
        repeat(500) { poller.onSample("com.a", isBlockedApp = false) }
        assertEquals(AdaptivePollScheduler.SLOW_MS, poller.intervalMs)
    }

    @Test
    fun `snaps back to fast the moment the app changes`() {
        val poller = scheduler()
        repeat(500) { poller.onSample("com.a", isBlockedApp = false) }
        assertEquals(AdaptivePollScheduler.SLOW_MS, poller.intervalMs)

        // The student just opened something else — this is exactly when a blocked
        // app might be appearing, so responsiveness has to come straight back.
        assertEquals(
            AdaptivePollScheduler.FAST_MS,
            poller.onSample("com.instagram.android", isBlockedApp = false),
        )
    }

    @Test
    fun `never backs off while a blocked app is in front`() {
        val poller = scheduler()
        repeat(100) {
            assertEquals(
                AdaptivePollScheduler.FAST_MS,
                poller.onSample("com.instagram.android", isBlockedApp = true),
            )
        }
    }

    @Test
    fun `an unknown foreground app is treated as a change, not as stability`() {
        val poller = scheduler()
        repeat(500) { poller.onSample("com.a", isBlockedApp = false) }
        assertEquals(AdaptivePollScheduler.FAST_MS, poller.onSample(null, isBlockedApp = false))
    }

    @Test
    fun `reset returns it to the attentive state`() {
        val poller = scheduler()
        repeat(500) { poller.onSample("com.a", isBlockedApp = false) }
        poller.reset()
        assertEquals(AdaptivePollScheduler.FAST_MS, poller.intervalMs)
        // And a sample of the same package now counts as a fresh observation.
        assertEquals(AdaptivePollScheduler.FAST_MS, poller.onSample("com.a", isBlockedApp = false))
    }

    @Test
    fun `an idle hour costs far fewer polls than fixed fast polling would`() {
        val poller = scheduler()
        var elapsed = 0L
        var polls = 0
        val hourMs = 60 * 60 * 1000L
        while (elapsed < hourMs) {
            elapsed += poller.onSample("com.a", isBlockedApp = false)
            polls++
        }
        val fixedFastPolls = hourMs / AdaptivePollScheduler.FAST_MS
        assertTrue(
            "adaptive $polls should be far below fixed $fixedFastPolls",
            polls < fixedFastPolls / 4,
        )
    }
}
