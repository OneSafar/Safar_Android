package com.safarparmar.app.ui.components.popups

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test

class SafarPopupBoundsTest {
    private val appColumn = IntRect(400, 0, 1000, 1000)

    @Test fun `left aligned popup stays inside centered tablet canvas`() {
        assertEquals(150, popupHorizontalCorrection(250, 240, appColumn))
    }

    @Test fun `right aligned popup stays inside centered tablet canvas`() {
        assertEquals(-140, popupHorizontalCorrection(900, 240, appColumn))
    }

    @Test fun `panel already inside canvas retains its anchor placement`() {
        assertEquals(0, popupHorizontalCorrection(500, 240, appColumn))
    }

    @Test fun `no viewport information leaves library placement alone`() {
        assertEquals(0, popupHorizontalCorrection(900, 240, null))
    }

    @Test fun `oversized transient panel anchors to canvas start without invalid range`() {
        assertEquals(-100, popupHorizontalCorrection(500, 800, appColumn))
    }
}
