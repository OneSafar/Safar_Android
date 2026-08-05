package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.KavachCategoryFilter
import com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity
import com.safarparmar.app.feature.kavachanalytics.domain.secondsFor
import com.safarparmar.app.feature.kavachanalytics.ui.KavachPeriod
import com.safarparmar.app.feature.kavachanalytics.ui.KavachAnalyticsFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KavachPeriodTest {

    // A Wednesday, so week boundaries are visible in both directions.
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun `daily period is a single day`() {
        val period = KavachPeriod.of(KavachGranularity.DAILY, 0, today)
        assertEquals(today, period.start)
        assertEquals(today, period.end)
    }

    @Test
    fun `paging back steps one day at a time`() {
        val period = KavachPeriod.of(KavachGranularity.DAILY, -3, today)
        assertEquals(LocalDate.of(2026, 8, 2), period.start)
        assertEquals(LocalDate.of(2026, 8, 2), period.end)
    }

    @Test
    fun `the current week runs Monday to today, not Monday to Sunday`() {
        val period = KavachPeriod.of(KavachGranularity.WEEKLY, 0, today)
        assertEquals(LocalDate.of(2026, 8, 3), period.start) // Monday
        // Clamped to today: an unfinished week must not read as a week where
        // usage collapsed on Thursday.
        assertEquals(today, period.end)
    }

    @Test
    fun `a past week is the full Monday to Sunday span`() {
        val period = KavachPeriod.of(KavachGranularity.WEEKLY, -1, today)
        assertEquals(LocalDate.of(2026, 7, 27), period.start)
        assertEquals(LocalDate.of(2026, 8, 2), period.end)
    }

    @Test
    fun `the current month is clamped to today and a past month is whole`() {
        val current = KavachPeriod.of(KavachGranularity.MONTHLY, 0, today)
        assertEquals(LocalDate.of(2026, 8, 1), current.start)
        assertEquals(today, current.end)

        val previous = KavachPeriod.of(KavachGranularity.MONTHLY, -1, today)
        assertEquals(LocalDate.of(2026, 7, 1), previous.start)
        assertEquals(LocalDate.of(2026, 7, 31), previous.end)
    }

    @Test
    fun `paging forward past today is refused`() {
        val period = KavachPeriod.of(KavachGranularity.DAILY, 3, today)
        assertEquals(today, period.start)
    }

    @Test
    fun `previous period is the same granularity one step back`() {
        val week = KavachPeriod.of(KavachGranularity.WEEKLY, 0, today)
        val prior = week.previous(today)
        assertEquals(LocalDate.of(2026, 7, 27), prior.start)
        assertEquals(LocalDate.of(2026, 8, 2), prior.end)
    }
}

class KavachCategoryFilterTest {

    private val totals = CategoryTotals(
        productiveSeconds = 100,
        distractingSeconds = 200,
        neutralSeconds = 30,
        unclassifiedSeconds = 70,
    )

    @Test
    fun `Others covers neutral and uncategorised together`() {
        assertTrue(KavachCategoryFilter.OTHERS.matches(AppCategory.NEUTRAL))
        assertTrue(KavachCategoryFilter.OTHERS.matches(AppCategory.UNCLASSIFIED))
        assertFalse(KavachCategoryFilter.OTHERS.matches(AppCategory.PRODUCTIVE))
        assertFalse(KavachCategoryFilter.OTHERS.matches(AppCategory.DISTRACTING))
    }

    @Test
    fun `All apps matches everything`() {
        AppCategory.entries.forEach {
            assertTrue(KavachCategoryFilter.ALL.matches(it))
        }
    }

    @Test
    fun `the four chips add up to the whole day with nothing double counted`() {
        val sum = KavachCategoryFilter.DISTRACTING.let { totals.secondsFor(it) } +
            totals.secondsFor(KavachCategoryFilter.PRODUCTIVE) +
            totals.secondsFor(KavachCategoryFilter.OTHERS)

        assertEquals(totals.totalSeconds, sum)
        assertEquals(totals.totalSeconds, totals.secondsFor(KavachCategoryFilter.ALL))
    }

    @Test
    fun `Others keeps uncategorised time visible rather than dropping it`() {
        // 30 neutral + 70 uncategorised — the uncategorised half must not vanish
        // just because the two share a chip.
        assertEquals(100, totals.secondsFor(KavachCategoryFilter.OTHERS))
    }
}

class KavachAxisTest {

    private fun mins(n: Int) = n * 60

    @Test
    fun `default daily baseline is 10 hours`() {
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(mins(41)))
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(mins(9)))
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(mins(300)))
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(mins(600)))
    }

    @Test
    fun `usage exceeding 10 hours scales up in even hour increments`() {
        assertEquals(mins(720), KavachAnalyticsFormat.niceAxisMax(mins(660))) // 11h -> 12h
        assertEquals(mins(840), KavachAnalyticsFormat.niceAxisMax(mins(780))) // 13h -> 14h
    }

    @Test
    fun `the midpoint tick is always a whole number of hours`() {
        listOf(41, 90, 200, 400, 700, 1_400).forEach { minutes ->
            val axis = KavachAnalyticsFormat.niceAxisMax(mins(minutes))
            assertEquals(
                "axis $axis should halve cleanly into whole hours",
                0,
                (axis / 2) % 3600,
            )
        }
    }

    @Test
    fun `an empty period still gets the 10 hour baseline`() {
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(0))
        assertEquals(mins(600), KavachAnalyticsFormat.niceAxisMax(-5))
    }

    @Test
    fun `axis labels read cleanly as 10h 5h 0`() {
        assertEquals("0", KavachAnalyticsFormat.axisValue(0))
        assertEquals("5h", KavachAnalyticsFormat.axisValue(mins(300)))
        assertEquals("10h", KavachAnalyticsFormat.axisValue(mins(600)))
    }
}
