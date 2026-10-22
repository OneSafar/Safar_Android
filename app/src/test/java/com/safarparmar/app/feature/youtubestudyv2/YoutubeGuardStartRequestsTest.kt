package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.*
import org.junit.Test

class YoutubeGuardStartRequestsTest {
    @Test fun `guard notification requires enabled focus and a connected monitor`() {
        val requests = YoutubeGuardStartRequests()
        val start = requests.requestStart()
        assertTrue(requests.canRun(start, enabled = true, connected = true))
        assertFalse(requests.canRun(start, enabled = false, connected = true))
        assertFalse(requests.canRun(start, enabled = true, connected = false))
    }

    @Test fun `disconnect invalidates an Android start already in flight`() {
        val requests = YoutubeGuardStartRequests()
        val queued = requests.requestStart()
        requests.invalidate()
        assertFalse(requests.canRun(queued, enabled = true, connected = false))
        // Even a later rebind cannot revive the old queued command.
        assertFalse(requests.canRun(queued, enabled = true, connected = true))
        val reconnected = requests.requestStart()
        assertTrue(requests.canRun(reconnected, enabled = true, connected = true))
    }

    @Test fun `newer start supersedes an older command and unsolicited restarts are rejected`() {
        val requests = YoutubeGuardStartRequests()
        val first = requests.requestStart()
        val second = requests.requestStart()
        assertFalse(requests.canRun(first, enabled = true, connected = true))
        assertTrue(requests.canRun(second, enabled = true, connected = true))
        assertFalse(requests.canRun(-1L, enabled = true, connected = true))
    }
}
