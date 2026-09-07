package com.safarparmar.app.feature.live

import com.safarparmar.app.feature.live.presentation.LiveSessionFilter
import com.safarparmar.app.feature.live.presentation.LiveThemeColors
import com.safarparmar.app.feature.live.presentation.formatNextSessionSubtitle
import com.safarparmar.app.feature.live.presentation.formatRelativeDateAndDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class LiveSessionComponentsTest {

    @Test
    fun `next session subtitle with null or blank start time`() {
        val subtitle = formatNextSessionSubtitle(null, "Quant")
        assertEquals("Next session will be announced soon", subtitle)

        val blankSubtitle = formatNextSessionSubtitle("", "Quant")
        assertEquals("Next session will be announced soon", blankSubtitle)
    }

    @Test
    fun `next session subtitle formats today properly`() {
        val now = Instant.now().toString()
        val subtitle = formatNextSessionSubtitle(now, "Quant")
        assertTrue(subtitle.startsWith("Next session: today,"))
        assertTrue(subtitle.contains("Quant"))
    }

    @Test
    fun `relative date formatting handles today and duration`() {
        val now = Instant.now()
        val start = now.minus(50, ChronoUnit.MINUTES).toString()
        val end = now.toString()

        val label = formatRelativeDateAndDuration(start, end)
        assertTrue(label.startsWith("Today"))
        assertTrue(label.contains("50 min"))
    }

    @Test
    fun `relative date formatting handles yesterday`() {
        val now = Instant.now()
        val start = now.minus(26, ChronoUnit.HOURS).toString()
        val end = now.minus(25, ChronoUnit.HOURS).minus(12, ChronoUnit.MINUTES).toString()

        val label = formatRelativeDateAndDuration(start, end)
        assertTrue(label.startsWith("Yesterday"))
        assertTrue(label.contains("48 min"))
    }

    @Test
    fun `relative date formatting handles multiple days ago`() {
        val now = Instant.now()
        val start = now.minus(50, ChronoUnit.HOURS).toString()
        val end = now.minus(49, ChronoUnit.HOURS).minus(8, ChronoUnit.MINUTES).toString()

        val label = formatRelativeDateAndDuration(start, end)
        assertTrue(label.startsWith("2 days ago"))
        assertTrue(label.contains("52 min"))
    }

    @Test
    fun `filter mappings match backend values`() {
        assertEquals("active", LiveSessionFilter.LIVE.backendStatus)
        assertEquals("ended", LiveSessionFilter.COMPLETED.backendStatus)
    }

    @Test
    fun `theme colors provide distinct light and dark palettes`() {
        assertNotEquals(LiveThemeColors.background(isDark = true), LiveThemeColors.background(isDark = false))
        assertNotEquals(LiveThemeColors.card(isDark = true), LiveThemeColors.card(isDark = false))
        assertNotEquals(LiveThemeColors.textPrimary(isDark = true), LiveThemeColors.textPrimary(isDark = false))
        assertNotEquals(LiveThemeColors.searchBg(isDark = true), LiveThemeColors.searchBg(isDark = false))
    }
}
