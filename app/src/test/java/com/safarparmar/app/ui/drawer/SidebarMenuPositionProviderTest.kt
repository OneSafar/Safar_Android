package com.safarparmar.app.ui.drawer

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidebarMenuPositionProviderTest {
    @Test fun phoneMenuExtendsPastDrawerButStaysInsideWindow() {
        val position = SidebarMenuPositionProvider(false, 16, 8, 24, 24).calculatePosition(
            IntRect(16, 300, 304, 364), IntSize(400, 800), LayoutDirection.Ltr, IntSize(280, 240),
        )
        assertTrue(position.x + 280 > 320) // Drawer right edge.
        assertEquals(384, position.x + 280) // Window right gutter.
        assertTrue(position.y >= 40 && position.y + 240 <= 760)
    }

    @Test fun tabletRailOpensBesideRailRatherThanInsideIt() {
        val position = SidebarMenuPositionProvider(true, 16, 8, 0, 0).calculatePosition(
            IntRect(8, 220, 96, 300), IntSize(1200, 800), LayoutDirection.Ltr, IntSize(280, 360),
        )
        assertEquals(104, position.x)
    }

    @Test fun rightToLeftMenuExtendsPastLeftEdgeOfDrawer() {
        val position = SidebarMenuPositionProvider(false, 16, 8, 24, 24).calculatePosition(
            IntRect(96, 300, 384, 364), IntSize(400, 800), LayoutDirection.Rtl, IntSize(280, 240),
        )
        assertEquals(16, position.x)
        assertTrue(position.x < 80) // Drawer left edge.
    }

    @Test fun lowerRowsOpenAboveWithoutCrossingSystemInsets() {
        val position = SidebarMenuPositionProvider(false, 16, 8, 24, 24).calculatePosition(
            IntRect(16, 650, 304, 714), IntSize(400, 800), LayoutDirection.Ltr, IntSize(280, 400),
        )
        assertTrue(position.y < 650)
        assertTrue(position.y >= 40 && position.y + 400 <= 760)
    }
}
