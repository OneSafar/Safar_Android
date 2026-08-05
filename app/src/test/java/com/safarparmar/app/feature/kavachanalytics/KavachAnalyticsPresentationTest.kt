package com.safarparmar.app.feature.kavachanalytics

import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import com.safarparmar.app.feature.kavachanalytics.ui.KavachAnalyticsFormat
import com.safarparmar.app.feature.kavachanalytics.ui.buildEditableApps
import com.safarparmar.app.ui.ekagra.focusshield.BlockedAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class KavachAnalyticsPresentationTest {

    @Test
    fun `durations read the way a student would say them`() {
        assertEquals("0m", KavachAnalyticsFormat.duration(0))
        assertEquals("<1m", KavachAnalyticsFormat.duration(30))
        assertEquals("5m", KavachAnalyticsFormat.duration(300))
        assertEquals("1h", KavachAnalyticsFormat.duration(3600))
        assertEquals("2h 5m", KavachAnalyticsFormat.duration(7500))
    }

    @Test
    fun `an ended-early session is never labelled a failure`() {
        assertEquals("Completed", KavachAnalyticsFormat.outcomeLabel(KavachSessionOutcome.COMPLETED))
        assertEquals("Ended early", KavachAnalyticsFormat.outcomeLabel(KavachSessionOutcome.ENDED_EARLY))
        assertEquals("Interrupted", KavachAnalyticsFormat.outcomeLabel(KavachSessionOutcome.INTERRUPTED))
        assertEquals("In progress", KavachAnalyticsFormat.outcomeLabel(null))
    }

    @Test
    fun `percentages never divide by zero`() {
        assertEquals(0, KavachAnalyticsFormat.percent(3, 0))
        assertEquals(50, KavachAnalyticsFormat.percent(1, 2))
        assertEquals(100, KavachAnalyticsFormat.percent(5, 5))
    }

    @Test
    fun `category totals add and combine without losing unclassified time`() {
        val totals = CategoryTotals()
            .add(AppCategory.PRODUCTIVE, 100)
            .add(AppCategory.DISTRACTING, 50)
            .add(AppCategory.UNCLASSIFIED, 25)

        assertEquals(175, totals.totalSeconds)
        assertEquals(25, totals.unclassifiedSeconds)

        val combined = totals + CategoryTotals(neutralSeconds = 10)
        assertEquals(185, combined.totalSeconds)
    }

    @Test
    fun `wire values round-trip and unknown ones fall back to unclassified`() {
        assertEquals(AppCategory.DISTRACTING, AppCategory.fromWire("distracting"))
        assertEquals(AppCategory.UNCLASSIFIED, AppCategory.fromWire("who-knows"))
        assertEquals(AppCategory.UNCLASSIFIED, AppCategory.fromWire(null))
        assertEquals("ended_early", KavachSessionOutcome.ENDED_EARLY.wire)
        assertEquals(null, KavachSessionOutcome.fromWire("failed"))
    }

    // ── App category editor list ─────────────────────────────────────────────

    private fun app(pkg: String, label: String) =
        BlockedAppInfo(packageName = pkg, appName = label, icon = null)

    @Test
    fun `uncategorised apps are surfaced first`() {
        val apps = buildEditableApps(
            installed = listOf(app("com.a", "Alpha"), app("com.z", "Zeta"), app("com.m", "Mid")),
            overrides = mapOf(
                "com.a" to AppCategory.PRODUCTIVE,
                "com.m" to AppCategory.DISTRACTING,
            ),
            query = "",
        )

        assertEquals(listOf("com.z", "com.a", "com.m"), apps.map { it.packageName })
        assertEquals(AppCategory.UNCLASSIFIED, apps.first().category)
    }

    @Test
    fun `search filters by label case-insensitively`() {
        val apps = buildEditableApps(
            installed = listOf(app("com.a", "Instagram"), app("com.b", "Notion")),
            overrides = emptyMap(),
            query = "  inSTA ",
        )

        assertEquals(listOf("com.a"), apps.map { it.packageName })
    }
}
