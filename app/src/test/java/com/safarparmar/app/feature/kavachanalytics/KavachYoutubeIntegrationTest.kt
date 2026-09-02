package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.data.youtubeCategoryTotals
import com.safarparmar.app.feature.kavachanalytics.data.KavachDailyAggregator
import com.safarparmar.app.feature.kavachanalytics.data.DatedUsageInterval
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeViewingIntervalEntity
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.KavachCategoryFilter
import com.safarparmar.app.ui.ekagra.focusshield.allowsKavachQuickUnlock
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Session
import com.safarparmar.app.feature.youtubestudyv2.YoutubeV2Observation
import com.safarparmar.app.feature.youtubestudyv2.YoutubeV2ContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class KavachYoutubeIntegrationTest {
    @Test
    fun `off-mode time keeps every normal app category`() {
        AppCategory.entries.forEach { category ->
            assertEquals(CategoryTotals().add(category, 600), youtubeCategoryTotals(600, 0, 0, 0, 0, category))
        }
    }

    @Test
    fun `same day can contain off-mode time browsing and distracting unlock`() {
        assertEquals(CategoryTotals(360, 180, 0, 60),
            youtubeCategoryTotals(600, 120, 180, 0, 60, AppCategory.PRODUCTIVE))
    }

    @Test
    fun `existing app filters show only their share of YouTube time`() {
        val row = AppUsageRow("com.google.android.youtube", "YouTube", AppCategory.PRODUCTIVE,
            900, 300, 0, 0, CategoryTotals(600, 240, 0, 60), CategoryTotals(120, 180))
        assertEquals(900, row.secondsFor(KavachCategoryFilter.ALL, false))
        assertEquals(600, row.secondsFor(KavachCategoryFilter.PRODUCTIVE, false))
        assertEquals(240, row.secondsFor(KavachCategoryFilter.DISTRACTING, false))
        assertEquals(60, row.secondsFor(KavachCategoryFilter.OTHERS, false))
        assertEquals(180, row.secondsFor(KavachCategoryFilter.DISTRACTING, true))
        assertEquals("Mixed activity", row.usageLabel(KavachCategoryFilter.ALL, false))
    }

    @Test
    fun `session uses observed content within its time boundaries only`() {
        val usage = listOf(DatedUsageInterval("com.google.android.youtube", "2026-09-02", 0, 600_000))
        val observed = listOf(
            YoutubeViewingIntervalEntity("p", 0, 120_000, "2026-09-02", "UC1", "productive", false),
            YoutubeViewingIntervalEntity("d", 120_000, 300_000, "2026-09-02", "UC2", "distracting", false),
            YoutubeViewingIntervalEntity("o", 300_000, 360_000, "2026-09-02", null, "unidentified", false),
        )
        val full = KavachDailyAggregator.sessionCategoryTotals(usage, 0L..600_000L, { AppCategory.PRODUCTIVE }, observed)
        assertEquals(360, full[AppCategory.PRODUCTIVE])
        assertEquals(180, full[AppCategory.DISTRACTING])
        assertEquals(60, full[AppCategory.UNCLASSIFIED])
        assertEquals(600, full.values.sum())
        val partial = KavachDailyAggregator.sessionCategoryTotals(usage, 120_000L..240_000L, { AppCategory.PRODUCTIVE }, observed)
        assertEquals(120, partial[AppCategory.DISTRACTING])
        assertEquals(120, partial.values.sum())
    }

    @Test
    fun `normal unlock exempts both app and media gate but strict never does`() {
        assertTrue(allowsKavachQuickUnlock(false, true, false))
        assertFalse(allowsKavachQuickUnlock(true, true, false))
        assertFalse(allowsKavachQuickUnlock(false, false, false))
        assertFalse(allowsKavachQuickUnlock(false, true, true))
    }

    @Test
    fun `same video is evaluated again when tracking stopped`() {
        val session = YoutubeStudyV2Session()
        val video = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true, "Lesson", "@teacher")
        session.onVideoTap(0L)
        assertTrue(session.acceptStable(video, 500L))
        assertTrue(session.isAlreadyEvaluated(video.stableKey, video.stableKey, true))
        assertFalse(session.isAlreadyEvaluated(video.stableKey, video.stableKey, false))
    }

    @Test
    fun `youtube replaces raw app category without double counting`() {
        val totals = youtubeCategoryTotals(
            rawYoutubeSeconds = 1_800,
            productiveSeconds = 900,
            distractingSeconds = 180,
            shortsSeconds = 120,
            unidentifiedSeconds = 0,
        )

        assertEquals(900, totals.productiveSeconds)
        assertEquals(300, totals.distractingSeconds)
        assertEquals(600, totals.unclassifiedSeconds)
        assertEquals(1_800, totals.totalSeconds)
    }

    @Test
    fun `accessibility measurement is retained when usage stats is incomplete`() {
        val totals = youtubeCategoryTotals(0, 60, 30, 10, 0)

        assertEquals(60, totals.productiveSeconds)
        assertEquals(40, totals.distractingSeconds)
        assertEquals(100, totals.totalSeconds)
    }
}
