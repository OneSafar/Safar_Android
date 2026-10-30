package com.safarparmar.app.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class SafarAdaptiveStateTest {
    private fun window(width: Int, height: Int, tablet: Boolean = true) = SafarAdaptiveState(
        safarWindowWidthClass(width.dp), width.dp, height.dp, tablet, width > height,
    )

    @Test fun widthBoundariesFollowTheWindow() {
        assertEquals(SafarWindowWidthClass.Compact, safarWindowWidthClass(599.dp))
        assertEquals(SafarWindowWidthClass.Medium, safarWindowWidthClass(600.dp))
        assertEquals(SafarWindowWidthClass.Medium, safarWindowWidthClass(839.dp))
        assertEquals(SafarWindowWidthClass.Expanded, safarWindowWidthClass(840.dp))
        assertFalse(window(390, 800).isWideScreen)
        assertTrue(window(600, 900, false).isWideScreen)
    }

    @Test fun navigationPreservesPortraitAndShortWindowFallbacks() {
        assertFalse(window(390, 800).usePlannerTopNavigation)
        assertFalse(window(800, 1280).usePlannerTopNavigation)
        assertFalse(window(839, 600).usePlannerTopNavigation)
        assertTrue(window(840, 600).usePlannerTopNavigation)
        assertFalse(window(1000, 479).usePlannerTopNavigation)
        assertTrue(window(1000, 480, false).usePlannerTopNavigation)
    }

    @Test fun paneFitAccountsForRailAndHeight() {
        val tablet = window(1024, 800)
        assertTrue(tablet.usePlannerTopNavigation)
        assertTrue(tablet.plannerPanesFit((1024 - 104).dp, 600.dp))
        assertFalse(tablet.plannerPanesFit((1024 - 255).dp, 600.dp))
        assertFalse(tablet.plannerPanesFit(839.dp, 600.dp))
        assertTrue(tablet.plannerPanesFit(840.dp, 360.dp))
        assertFalse(tablet.plannerPanesFit(840.dp, 359.dp))
        assertFalse(tablet.plannerPanesFit(759.dp, 600.dp, 760.dp))
        assertTrue(tablet.plannerPanesFit(760.dp, 600.dp, 760.dp))
        assertFalse(window(390, 800).plannerPanesFit(1200.dp, 700.dp))
    }
}
