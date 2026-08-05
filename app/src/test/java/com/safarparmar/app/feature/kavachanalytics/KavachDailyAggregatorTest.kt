package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.data.AppCategoryDefaults
import com.safarparmar.app.feature.kavachanalytics.data.DailyEventCounts
import com.safarparmar.app.feature.kavachanalytics.data.DatedUsageInterval
import com.safarparmar.app.feature.kavachanalytics.data.KavachDailyAggregator
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class KavachDailyAggregatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val date = "2026-08-04"

    private fun at(time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun interval(pkg: String, from: String, to: String) =
        DatedUsageInterval(pkg, date, at(from), at(to))

    private val categories = mapOf(
        "com.instagram.android" to AppCategory.DISTRACTING,
        "com.notion.id" to AppCategory.PRODUCTIVE,
    )

    private fun aggregate(
        intervals: List<DatedUsageInterval>,
        windows: List<LongRange> = emptyList(),
        counts: DailyEventCounts = DailyEventCounts(),
    ) = KavachDailyAggregator.aggregate(
        localDate = date,
        intervals = intervals,
        kavachWindows = windows,
        eventCounts = counts,
        categoryOf = { categories[it] ?: AppCategory.UNCLASSIFIED },
        labelOf = { it.substringAfterLast('.') },
        nowMs = 1_000L,
    )

    @Test
    fun `all-day and during-Kavach totals stay separate`() {
        val rows = aggregate(
            intervals = listOf(interval("com.instagram.android", "09:00:00", "10:00:00")),
            windows = listOf(at("09:45:00")..at("11:00:00")),
        )

        val instagram = rows.single { it.packageName == "com.instagram.android" }
        assertEquals(3600, instagram.allDaySeconds)
        assertEquals(900, instagram.kavachSeconds)
    }

    @Test
    fun `time outside every session counts only towards the all-day total`() {
        val rows = aggregate(intervals = listOf(interval("com.notion.id", "09:00:00", "09:30:00")))

        val notion = rows.single()
        assertEquals(1800, notion.allDaySeconds)
        assertEquals(0, notion.kavachSeconds)
    }

    @Test
    fun `an app with only a blocked attempt still gets a row`() {
        val rows = aggregate(
            intervals = emptyList(),
            counts = DailyEventCounts(blockedAttempts = mapOf("com.instagram.android" to 4)),
        )

        val instagram = rows.single()
        assertEquals(0, instagram.allDaySeconds)
        assertEquals(4, instagram.blockedAttempts)
    }

    @Test
    fun `unknown apps are reported as unclassified not distracting`() {
        val rows = aggregate(intervals = listOf(interval("com.some.newapp", "09:00:00", "09:10:00")))

        assertEquals(AppCategory.UNCLASSIFIED.wire, rows.single().category)
    }

    @Test
    fun `intervals from another day are ignored`() {
        val rows = aggregate(
            intervals = listOf(
                interval("com.notion.id", "09:00:00", "09:10:00"),
                DatedUsageInterval("com.notion.id", "2026-08-03", at("09:00:00"), at("09:30:00")),
            ),
        )

        assertEquals(600, rows.single().allDaySeconds)
    }

    @Test
    fun `separate visits to the same app add up`() {
        val rows = aggregate(
            intervals = listOf(
                interval("com.notion.id", "09:00:00", "09:10:00"),
                interval("com.notion.id", "11:00:00", "11:05:00"),
            ),
        )

        assertEquals(900, rows.single().allDaySeconds)
    }

    @Test
    fun `session category totals attribute overlap to the right category`() {
        val totals = KavachDailyAggregator.sessionCategoryTotals(
            intervals = listOf(
                interval("com.instagram.android", "10:00:00", "10:05:00"),
                interval("com.notion.id", "10:05:00", "10:35:00"),
                interval("com.some.newapp", "10:35:00", "10:40:00"),
            ),
            window = at("10:00:00")..at("10:40:00"),
            categoryOf = { categories[it] ?: AppCategory.UNCLASSIFIED },
        )

        assertEquals(300, totals[AppCategory.DISTRACTING])
        assertEquals(1800, totals[AppCategory.PRODUCTIVE])
        assertEquals(300, totals[AppCategory.UNCLASSIFIED])
    }

    @Test
    fun `shipped defaults classify known apps and leave everything else unclassified`() {
        assertEquals(AppCategory.DISTRACTING, AppCategoryDefaults.categoryFor("com.instagram.android"))
        assertEquals(AppCategory.PRODUCTIVE, AppCategoryDefaults.categoryFor("com.notion.id"))
        assertEquals(AppCategory.NEUTRAL, AppCategoryDefaults.categoryFor("com.google.android.dialer"))
        assertEquals(AppCategory.UNCLASSIFIED, AppCategoryDefaults.categoryFor("com.unknown.app"))
    }

    @Test
    fun `no default silently marks an unknown package as distracting`() {
        assertTrue(AppCategoryDefaults.DEFAULTS.keys.all { it.isNotBlank() })
        assertNotNull(AppCategoryDefaults.DEFAULTS["com.instagram.android"])
    }
}
