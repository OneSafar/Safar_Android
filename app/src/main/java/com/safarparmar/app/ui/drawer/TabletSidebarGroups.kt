package com.safarparmar.app.ui.drawer

import com.safarparmar.app.ui.navigation.Routes

internal data class TabletSidebarGroups(
    val study: List<DrawerItem>,
    val wellbeing: List<DrawerItem>,
    val community: List<DrawerItem>,
    val account: List<DrawerItem>,
)

/** Keep the tablet sidebar in sync with the shared drawer's destinations. */
internal fun tabletSidebarGroups(): TabletSidebarGroups {
    val study = drawerSections.first { it.id == DrawerSectionId.STUDY_PRODUCTIVITY }.items
    val wellbeingRoutes = setOf(Routes.NISHTHA, Routes.DHYAN, Routes.CALM_CENTRE, Routes.LIVE_SESSIONS_ROOT)
    val wellbeingAndCommunity = drawerSections.first { it.id == DrawerSectionId.WELL_BEING_COMMUNITY }.items
    val pinnedRoutes = setOf(Routes.PROFILE, Routes.SETTINGS)
    return TabletSidebarGroups(
        study = study,
        wellbeing = wellbeingAndCommunity.filter { it.route.substringBefore("?") in wellbeingRoutes },
        community = wellbeingAndCommunity.filterNot { it.route.substringBefore("?") in wellbeingRoutes } +
            drawerPinnedBottom.filterNot { it.route.substringBefore("?") in pinnedRoutes },
        account = drawerPinnedBottom.filter { it.route.substringBefore("?") in pinnedRoutes },
    )
}

/** Tapping a rail section opens the sidebar and the section in a single action. */
internal data class SidebarSectionTapResult(val openSidebar: Boolean, val sectionExpanded: Boolean)

internal fun sectionTapResult(sidebarExpanded: Boolean, sectionExpanded: Boolean): SidebarSectionTapResult =
    if (sidebarExpanded) SidebarSectionTapResult(openSidebar = false, sectionExpanded = !sectionExpanded)
    else SidebarSectionTapResult(openSidebar = true, sectionExpanded = true)
