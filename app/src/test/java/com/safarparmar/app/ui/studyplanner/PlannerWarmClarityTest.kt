package com.safarparmar.app.ui.studyplanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.safarparmar.app.ui.studyplanner.components.plannerWarmColors
import com.safarparmar.app.ui.studyplanner.components.plannerButtonForeground
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerWarmClarityTest {
    @Test fun `essential text actions and semantic labels remain readable in both themes`() {
        fun contrast(a: Color, b: Color): Float = (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)
        listOf(false, true).forEach { dark ->
            val c = plannerWarmColors(dark)
            listOf(c.canvas, c.card, c.peach, c.mint).forEach { surface ->
                assertTrue("Ink on $surface, dark=$dark", contrast(c.ink, surface) >= 4.5f)
            }
            assertTrue(contrast(c.muted, c.card) >= 4.5f)
            assertTrue(contrast(c.muted, c.canvas) >= 4.5f)
            assertTrue(contrast(c.onAction, c.action) >= 4.5f)
            listOf(c.success, c.attention, c.error).forEach { status -> assertTrue(contrast(status, c.card) >= 4.5f) }
        }
    }
    @Test fun `legacy action slots retain readable labels across accent colors`() {
        listOf(false, true).forEach { dark ->
            val c = plannerWarmColors(dark)
            listOf(c.action, c.muted, c.error, c.success, c.attention).forEach { background ->
                val foreground = plannerButtonForeground(background)
                val ratio = (maxOf(foreground.luminance(), background.luminance()) + 0.05f) /
                    (minOf(foreground.luminance(), background.luminance()) + 0.05f)
                assertTrue("Button contrast dark=$dark", ratio >= 4.5f)
            }
        }
    }

}
