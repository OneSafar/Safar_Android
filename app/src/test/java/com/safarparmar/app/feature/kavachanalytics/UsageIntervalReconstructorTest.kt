package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.data.ForegroundTransition
import com.safarparmar.app.feature.kavachanalytics.data.UsageInterval
import com.safarparmar.app.feature.kavachanalytics.data.UsageIntervalReconstructor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageIntervalReconstructorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun at(dateTime: String): Long =
        java.time.LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun fg(pkg: String, at: Long) = ForegroundTransition(pkg, at, movedToForeground = true)
    private fun bg(pkg: String, at: Long) = ForegroundTransition(pkg, at, movedToForeground = false)

    @Test
    fun `closes an interval when the next app comes forward`() {
        val start = at("2026-08-04T10:00:00")
        val switch = at("2026-08-04T10:05:00")
        val end = at("2026-08-04T10:10:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.a", start), fg("com.b", switch)),
            windowStartMs = start,
            windowEndMs = end,
        )

        assertEquals(
            listOf(
                UsageInterval("com.a", start, switch),
                UsageInterval("com.b", switch, end),
            ),
            intervals,
        )
    }

    @Test
    fun `closes a still-open visit at the end of the collection window`() {
        val start = at("2026-08-04T10:00:00")
        val end = at("2026-08-04T10:03:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.a", start)),
            windowStartMs = start,
            windowEndMs = end,
        )

        assertEquals(listOf(UsageInterval("com.a", start, end)), intervals)
    }

    @Test
    fun `a background event ends the visit and no time is invented afterwards`() {
        val start = at("2026-08-04T10:00:00")
        val stop = at("2026-08-04T10:02:00")
        val end = at("2026-08-04T10:30:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.a", start), bg("com.a", stop)),
            windowStartMs = start,
            windowEndMs = end,
        )

        assertEquals(listOf(UsageInterval("com.a", start, stop)), intervals)
    }

    @Test
    fun `repeated foreground events for the same app do not split the visit`() {
        val start = at("2026-08-04T10:00:00")
        val again = at("2026-08-04T10:01:00")
        val end = at("2026-08-04T10:02:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.a", start), fg("com.a", again)),
            windowStartMs = start,
            windowEndMs = end,
        )

        assertEquals(listOf(UsageInterval("com.a", start, end)), intervals)
    }

    @Test
    fun `events before the window are ignored so a collection gap invents no screen time`() {
        val windowStart = at("2026-08-04T10:00:00")
        val windowEnd = at("2026-08-04T10:05:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.a", at("2026-08-04T09:00:00"))),
            windowStartMs = windowStart,
            windowEndMs = windowEnd,
        )

        assertTrue(intervals.isEmpty())
    }

    @Test
    fun `excluded packages never produce intervals`() {
        val start = at("2026-08-04T10:00:00")
        val end = at("2026-08-04T10:05:00")

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = listOf(fg("com.safarparmar.app", start)),
            windowStartMs = start,
            windowEndMs = end,
            isExcluded = { it == "com.safarparmar.app" },
        )

        assertTrue(intervals.isEmpty())
    }

    @Test
    fun `SAFAR launcher system UI and permission screens are excluded`() {
        val excluded = { pkg: String ->
            UsageIntervalReconstructor.isExcluded(
                packageName = pkg,
                ownPackageName = "com.safarparmar.app",
                homePackages = setOf("com.miui.home"),
            )
        }

        assertTrue(excluded("com.safarparmar.app"))
        assertTrue(excluded("com.miui.home"))
        assertTrue(excluded("com.google.android.apps.nexuslauncher"))
        assertTrue(excluded("com.android.systemui"))
        assertTrue(excluded("com.android.settings"))
        assertTrue(excluded("com.google.android.permissioncontroller"))
        assertTrue(excluded(""))
        assertTrue(!excluded("com.instagram.android"))
    }

    // ── Midnight and timezone handling ───────────────────────────────────────

    @Test
    fun `a visit crossing midnight is split so each day is credited correctly`() {
        val interval = UsageInterval("com.a", at("2026-08-04T23:40:00"), at("2026-08-05T00:20:00"))

        val split = UsageIntervalReconstructor.splitByLocalDate(interval, zone)

        assertEquals(2, split.size)
        assertEquals("2026-08-04", split[0].localDate)
        assertEquals(20 * 60_000L, split[0].durationMs)
        assertEquals("2026-08-05", split[1].localDate)
        assertEquals(20 * 60_000L, split[1].durationMs)
    }

    @Test
    fun `a visit spanning several days produces one segment per day`() {
        val interval = UsageInterval("com.a", at("2026-08-04T22:00:00"), at("2026-08-07T02:00:00"))

        val split = UsageIntervalReconstructor.splitByLocalDate(interval, zone)

        assertEquals(
            listOf("2026-08-04", "2026-08-05", "2026-08-06", "2026-08-07"),
            split.map { it.localDate },
        )
    }

    @Test
    fun `a daylight-saving spring-forward day still splits at local midnight`() {
        val newYork = ZoneId.of("America/New_York")
        // 2026-03-08 is the US spring-forward day: that local day is only 23 hours.
        val start = java.time.LocalDateTime.parse("2026-03-07T23:30:00")
            .atZone(newYork).toInstant().toEpochMilli()
        val end = java.time.LocalDateTime.parse("2026-03-09T00:30:00")
            .atZone(newYork).toInstant().toEpochMilli()

        val split = UsageIntervalReconstructor.splitByLocalDate(UsageInterval("com.a", start, end), newYork)

        assertEquals(listOf("2026-03-07", "2026-03-08", "2026-03-09"), split.map { it.localDate })
        // The shortened day is 23 hours of wall clock, not 24.
        val dstDay = split.first { it.localDate == "2026-03-08" }
        assertEquals(23 * 60 * 60_000L, dstDay.durationMs)
        // Boundaries line up with local midnight in the new offset.
        assertEquals(
            LocalDate.of(2026, 3, 9).atStartOfDay(newYork).toInstant().toEpochMilli(),
            dstDay.endMs,
        )
    }

    // ── Kavach overlap ───────────────────────────────────────────────────────

    @Test
    fun `overlap counts only the part of a visit inside a Kavach session`() {
        val interval = UsageIntervalReconstructor
            .splitByLocalDate(UsageInterval("com.a", at("2026-08-04T10:00:00"), at("2026-08-04T10:30:00")), zone)
            .single()
        val window = at("2026-08-04T10:20:00")..at("2026-08-04T11:00:00")

        assertEquals(10 * 60_000L, UsageIntervalReconstructor.overlapMs(interval, listOf(window)))
    }

    @Test
    fun `overlapping session windows are merged so no second is counted twice`() {
        val interval = UsageIntervalReconstructor
            .splitByLocalDate(UsageInterval("com.a", at("2026-08-04T10:00:00"), at("2026-08-04T11:00:00")), zone)
            .single()
        val windows = listOf(
            at("2026-08-04T10:00:00")..at("2026-08-04T10:40:00"),
            at("2026-08-04T10:20:00")..at("2026-08-04T10:50:00"),
        )

        val merged = UsageIntervalReconstructor.mergeWindows(windows)

        assertEquals(1, merged.size)
        assertEquals(50 * 60_000L, UsageIntervalReconstructor.overlapMs(interval, merged))
    }

    @Test
    fun `a visit entirely outside every session contributes no Kavach time`() {
        val interval = UsageIntervalReconstructor
            .splitByLocalDate(UsageInterval("com.a", at("2026-08-04T08:00:00"), at("2026-08-04T08:30:00")), zone)
            .single()
        val window = at("2026-08-04T10:00:00")..at("2026-08-04T11:00:00")

        assertEquals(0L, UsageIntervalReconstructor.overlapMs(interval, listOf(window)))
    }
}
