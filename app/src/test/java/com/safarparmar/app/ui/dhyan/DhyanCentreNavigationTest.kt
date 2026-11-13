package com.safarparmar.app.ui.dhyan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DhyanCentreNavigationTest {
    @Test fun personalPracticeSharesOnlyBreathingAndCalmCentreTabs() {
        val personalTabs = listOf(DhyanTab.DHYAN, DhyanTab.CALM_CENTRE)
        assertEquals(personalTabs, dhyanTabsFor(DhyanTab.DHYAN))
        assertEquals(personalTabs, dhyanTabsFor(DhyanTab.CALM_CENTRE))
        assertFalse(dhyanTabsFor(DhyanTab.DHYAN).contains(DhyanTab.LIVE))
        assertFalse(dhyanTabsFor(DhyanTab.DHYAN).contains(DhyanTab.COURSES))
    }

    @Test fun courseAndLiveEntryPointsShareOnlyTheLiveCentreTabs() {
        val tabs = listOf(DhyanTab.LIVE, DhyanTab.COURSES)
        assertEquals(tabs, dhyanTabsFor(DhyanTab.LIVE))
        assertEquals(tabs, dhyanTabsFor(DhyanTab.COURSES))
    }
}
