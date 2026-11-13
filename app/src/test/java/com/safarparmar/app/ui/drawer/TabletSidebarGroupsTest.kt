package com.safarparmar.app.ui.drawer

import com.safarparmar.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletSidebarGroupsTest {
    @Test
    fun everySharedDestinationIsPresentOnceAndAccountActionsArePinned() {
        val groups = tabletSidebarGroups()
        val grouped = groups.study + groups.wellbeing + groups.community + groups.account
        val expected = drawerSections.flatMap { it.items } + drawerPinnedBottom

        assertEquals(expected.map { it.route }.sorted(), grouped.map { it.route }.sorted())
        assertEquals(setOf(Routes.PROFILE, Routes.SETTINGS), groups.account.map { it.route }.toSet())
        assertTrue(groups.study.any { it.route == Routes.YOUTUBE_STUDY_MODE_V2 })
        assertTrue(groups.wellbeing.any { it.route == Routes.DHYAN })
        assertTrue(groups.community.any { it.route == Routes.MEHFIL })
        assertTrue(groups.community.any { it.route == Routes.ADMIN_NOTIFICATIONS && it.requiresAdmin })
        assertFalse(groups.community.any { it.route in setOf(Routes.PROFILE, Routes.SETTINGS) })
    }
    @Test
    fun sectionTapOpensCollapsedRailOrTogglesAnExpandedSidebarSection() {
        assertEquals(SidebarSectionTapResult(true, true), sectionTapResult(false, false))
        assertEquals(SidebarSectionTapResult(true, true), sectionTapResult(false, true))
        assertEquals(SidebarSectionTapResult(false, true), sectionTapResult(true, false))
        assertEquals(SidebarSectionTapResult(false, false), sectionTapResult(true, true))
    }
}
