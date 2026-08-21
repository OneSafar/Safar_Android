package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundAppTrackerTest {

    @Test
    fun `keeps foreground package when a later query has no new events`() {
        val tracker = ForegroundAppTracker()

        tracker.onForeground("com.example.blocked")

        assertEquals("com.example.blocked", tracker.currentPackage)
    }

    @Test
    fun `switching apps replaces the cached foreground package`() {
        val tracker = ForegroundAppTracker()

        tracker.onForeground("com.example.first")
        tracker.onForeground("com.example.second")

        assertEquals("com.example.second", tracker.currentPackage)
    }

    @Test
    fun `background event only clears the matching foreground package`() {
        val tracker = ForegroundAppTracker()
        tracker.onForeground("com.example.current")

        tracker.onBackground("com.example.older")
        assertEquals("com.example.current", tracker.currentPackage)

        tracker.onBackground("com.example.current")
        assertNull(tracker.currentPackage)
    }
}
