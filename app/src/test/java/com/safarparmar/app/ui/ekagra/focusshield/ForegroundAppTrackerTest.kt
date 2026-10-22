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

    @Test
    fun `pause from replaced activity does not forget the fullscreen foreground app`() {
        val tracker = ForegroundAppTracker()
        tracker.onForeground("com.google.android.youtube", "WatchActivity")
        tracker.onForeground("com.google.android.youtube", "FullscreenActivity")
        tracker.onBackground("com.google.android.youtube", "WatchActivity")
        assertEquals("com.google.android.youtube", tracker.currentPackage)
        tracker.onBackground("com.google.android.youtube", "FullscreenActivity")
        assertNull(tracker.currentPackage)
    }

    @Test
    fun `blocked app remains tracked after switching between its activities`() {
        val tracker = ForegroundAppTracker()
        tracker.onForeground("com.example.blocked", "Feed")
        tracker.onForeground("com.example.blocked", "Video")
        tracker.onBackground("com.example.blocked", "Feed")
        assertEquals("com.example.blocked", tracker.currentPackage)
        tracker.onForeground("com.example.allowed", "Home")
        tracker.onBackground("com.example.blocked", "Video")
        assertEquals("com.example.allowed", tracker.currentPackage)
    }
}
