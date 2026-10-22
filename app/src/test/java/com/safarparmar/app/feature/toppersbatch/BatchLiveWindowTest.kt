package com.safarparmar.app.feature.toppersbatch

import com.google.gson.Gson
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class BatchLiveWindowTest {
    private val window = BatchLiveWindow("2026-10-05T04:00:00Z", "2026-10-05T06:00:00Z", "https://www.youtube.com/@test-channel")
    @Test fun liveWindowUsesExactStartAndEndBoundaries() {
        assertEquals("upcoming", window.status(Instant.parse("2026-10-05T03:59:59Z")))
        assertEquals("live", window.status(Instant.parse("2026-10-05T04:00:00Z")))
        assertEquals("live", window.status(Instant.parse("2026-10-05T05:59:59Z")))
        assertEquals("ended", window.status(Instant.parse("2026-10-05T06:00:00Z")))
        assertEquals("ended", window.status(Instant.parse("2026-10-06T04:00:00Z")))
    }
    @Test fun academyOnlyNeverEnablesLiveChoice() {
        val academyOnly = window.copy(enabled = false)
        assertEquals("academy-only", academyOnly.status(Instant.parse("2026-10-05T04:15:00Z")))
        val decoded = Gson().fromJson("""{"enabled":false,"startsAt":"2026-10-05T04:00:00Z","endsAt":"2026-10-05T06:00:00Z"}""", BatchLiveWindow::class.java)
        assertEquals("academy-only", decoded.status(Instant.parse("2026-10-05T04:15:00Z")))
    }
    @Test fun missingOrInvalidDurationNeverEnablesLiveChoice() {
        assertEquals("unavailable", window.copy(endsAt = null).status(Instant.parse("2026-10-05T04:15:00Z")))
        assertEquals("unavailable", window.copy(endsAt = window.startsAt).status(Instant.parse("2026-10-05T04:15:00Z")))
    }
    @Test fun readsBackendWindowAndSharedCourseUrlWithoutInventingLectureLinks() {
        val overview = Gson().fromJson("""{"course":{"id":"batch","academyCourseUrl":"https://www.parmaracademy.in/course"},"lectures":[{"id":"gk-1","liveWindow":{"startsAt":"2026-10-05T04:00:00Z","endsAt":"2026-10-05T06:00:00Z","youtubeUrl":"https://www.youtube.com/@test-channel"}}]}""", BatchOverview::class.java)
        assertEquals("https://www.parmaracademy.in/course", overview.course.academyCourseUrl)
        assertEquals(window, overview.lectures.single().liveWindow)
    }
}
