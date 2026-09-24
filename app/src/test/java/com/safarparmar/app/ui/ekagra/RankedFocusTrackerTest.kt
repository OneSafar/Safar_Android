package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.RankedFocusRequest
import com.safarparmar.app.data.remote.dto.RankedFocusResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RankedFocusTrackerTest {
    @Test fun `expired attendance is shown as personal time only`() = runTest {
        val api = mockk<FocusApi>()
        coEvery { api.updateRankedFocus(any()) } returns Response.success(RankedFocusResponse("token", false, 16200, true))
        val tracker = RankedFocusTracker(api, backgroundScope)
        assertEquals(RankedFocusStatus.ConnectionNeeded, tracker.status.value)
        tracker.update("local-one", true)
        runCurrent()
        assertEquals(RankedFocusStatus.AttendanceExpired, tracker.status.value)
    }

    @Test fun `heartbeats cannot silently confirm attendance`() = runTest {
        val calls = mutableListOf<RankedFocusRequest>()
        val api = mockk<FocusApi>()
        coEvery { api.updateRankedFocus(capture(calls)) } returns Response.success(RankedFocusResponse("token", false, 30))
        val tracker = RankedFocusTracker(api, backgroundScope)
        tracker.update("local-one", true)
        tracker.update("local-one", false)
        tracker.update("local-one", true)
        runCurrent()
        assertEquals(3, calls.size)
        assertTrue(calls.all { it.confirm == null })
        assertEquals(listOf(true, false, true), calls.map { it.running })
    }

    @Test fun `only explicit confirmation sends server checkpoint`() = runTest {
        val calls = mutableListOf<RankedFocusRequest>()
        val api = mockk<FocusApi>()
        coEvery { api.updateRankedFocus(capture(calls)) } returns Response.success(RankedFocusResponse("token", false, 5400))
        val tracker = RankedFocusTracker(api, backgroundScope)
        tracker.update("local-one", true)
        tracker.update("local-one", true, confirm = true)
        runCurrent()
        assertEquals(RankedFocusStatus.RankedTime(90), tracker.status.value)
        assertEquals("token", calls.last().confirm)
        assertNull(calls.first().confirm)
    }

    @Test fun `restored session obtains token before explicit confirmation`() = runTest {
        val calls = mutableListOf<RankedFocusRequest>()
        val api = mockk<FocusApi>()
        coEvery { api.updateRankedFocus(capture(calls)) } returns Response.success(RankedFocusResponse("restored", false, 5400))
        val tracker = RankedFocusTracker(api, backgroundScope)
        tracker.update("local-one", true, confirm = true)
        runCurrent()
        assertEquals(2, calls.size)
        assertNull(calls.first().confirm)
        assertEquals("restored", calls.last().confirm)
    }

    @Test fun `offline failure does not replay confirmation on next heartbeat`() = runTest {
        val calls = mutableListOf<RankedFocusRequest>()
        val api = mockk<FocusApi>()
        coEvery { api.updateRankedFocus(capture(calls)) } throws java.io.IOException("offline")
        val tracker = RankedFocusTracker(api, backgroundScope)
        tracker.update("local-one", true, confirm = true)
        runCurrent()
        assertEquals(RankedFocusStatus.Offline, tracker.status.value)
        coEvery { api.updateRankedFocus(capture(calls)) } returns Response.success(RankedFocusResponse("token", false, 0))
        tracker.update("local-one", true)
        runCurrent()
        assertNull(calls.last().confirm)
    }
}
