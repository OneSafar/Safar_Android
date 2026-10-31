package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertEquals
import org.junit.Test

class EkagraNavigationSelectionTest {
    @Test fun panelsSelectTheirOwnActionAndDismissBackToUnderlyingDestination() {
        listOf(EkagraNavTab.TIMER, EkagraNavTab.DURATION, EkagraNavTab.HISTORY).forEach { destination ->
            assertEquals(destination, ekagraNavigationSelection(destination, musicOpen = false, themeOpen = false))
            assertEquals(EkagraNavTab.MUSIC, ekagraNavigationSelection(destination, musicOpen = true, themeOpen = false))
            assertEquals(EkagraNavTab.THEME, ekagraNavigationSelection(destination, musicOpen = false, themeOpen = true))
            assertEquals(destination, ekagraNavigationSelection(destination, musicOpen = false, themeOpen = false))
        }
    }
}
