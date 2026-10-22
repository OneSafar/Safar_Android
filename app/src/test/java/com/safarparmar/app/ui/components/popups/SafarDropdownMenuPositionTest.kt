package com.safarparmar.app.ui.components.popups

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafarDropdownMenuPositionTest {
    private val window = IntSize(1000, 1000)

    @Test fun `LTR menu follows its original physical anchor`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(0.85f))
        assertEquals(IntOffset(150, 240), provider.calculatePosition(
            IntRect(150, 200, 200, 240), window, LayoutDirection.Ltr, IntSize(200, 100),
        ))
    }

    @Test fun `RTL aligns start to anchor right without converting physical bounds twice`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(0.85f))
        assertEquals(IntOffset(300, 240), provider.calculatePosition(
            IntRect(400, 200, 500, 240), window, LayoutDirection.Rtl, IntSize(200, 100),
        ))
    }

    @Test fun `dp offsets use app density and mirror horizontally in RTL`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset(10.dp, 6.dp), Density(2f))
        val anchor = IntRect(400, 300, 500, 340)
        assertEquals(IntOffset(420, 352), provider.calculatePosition(anchor, window, LayoutDirection.Ltr, IntSize(200, 100)))
        assertEquals(IntOffset(280, 352), provider.calculatePosition(anchor, window, LayoutDirection.Rtl, IntSize(200, 100)))
    }

    @Test fun `right edge falls back to anchor end`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
        assertEquals(IntOffset(750, 240), provider.calculatePosition(
            IntRect(900, 200, 950, 240), window, LayoutDirection.Ltr, IntSize(200, 100),
        ))
    }

    @Test fun `bottom edge opens above anchor`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
        assertEquals(IntOffset(150, 700), provider.calculatePosition(
            IntRect(150, 900, 200, 940), window, LayoutDirection.Ltr, IntSize(200, 200),
        ))
    }

    @Test fun `oversized tall menu uses centered vertical fallback without invalid clamp`() {
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(1f))
        assertEquals(IntOffset(150, 25), provider.calculatePosition(
            IntRect(150, 500, 200, 540), window, LayoutDirection.Ltr, IntSize(200, 950),
        ))
    }

    @Test fun `centered app canvas keeps a menu inside its own viewport`() {
        val appBounds = IntRect(320, 0, 680, 1000)
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(1f), appBounds)
        val position = provider.calculatePosition(
            IntRect(650, 200, 675, 240), window, LayoutDirection.Ltr, IntSize(300, 200),
        )
        assertEquals(IntOffset(375, 240), position)
        assertTrue(position.x >= appBounds.left)
        assertTrue(position.x + 300 <= appBounds.right)
    }

    @Test fun `both failed anchor candidates fall back to available app edge`() {
        val provider = SafarDropdownMenuPositionProvider(
            DpOffset(200.dp, 0.dp), Density(1f), IntRect(320, 0, 680, 1000),
        )
        assertEquals(IntOffset(380, 240), provider.calculatePosition(
            IntRect(650, 200, 675, 240), window, LayoutDirection.Ltr, IntSize(300, 200),
        ))
    }

    @Test fun `actual bounds callback drives animation pivot from anchor intersection`() {
        val anchor = IntRect(150, 200, 200, 240)
        var deliveredAnchor: IntRect? = null
        var deliveredMenu: IntRect? = null
        val provider = SafarDropdownMenuPositionProvider(DpOffset.Zero, Density(1f)) { parent, menu ->
            deliveredAnchor = parent
            deliveredMenu = menu
        }
        provider.calculatePosition(anchor, window, LayoutDirection.Ltr, IntSize(200, 100))
        assertEquals(anchor, deliveredAnchor)
        assertEquals(IntRect(150, 240, 350, 340), deliveredMenu)
        assertEquals(TransformOrigin(0.125f, 0f), safarMenuTransformOrigin(anchor, checkNotNull(deliveredMenu)))
    }

    @Test fun `menu above anchor animates from bottom edge`() {
        assertEquals(TransformOrigin(0.125f, 1f), safarMenuTransformOrigin(
            IntRect(150, 900, 200, 940), IntRect(150, 700, 350, 900),
        ))
    }
}
