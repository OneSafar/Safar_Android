package com.safarparmar.app.ui.dhyan

import org.junit.Assert.assertEquals
import org.junit.Test

class DhyanCentreNavigationTest {
    @Test fun personalPracticeHasNoLiveOrCourseTabs() {
        assertEquals(listOf(DhyanTab.DHYAN), dhyanTabsFor(DhyanTab.DHYAN))
    }

    @Test fun courseAndLiveEntryPointsShareOnlyTheLiveCentreTabs() {
        val tabs = listOf(DhyanTab.LIVE, DhyanTab.COURSES)
        assertEquals(tabs, dhyanTabsFor(DhyanTab.LIVE))
        assertEquals(tabs, dhyanTabsFor(DhyanTab.COURSES))
    }
}
