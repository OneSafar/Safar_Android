package com.safarparmar.app.ui.tutorial

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class TutorialMenuArtworkTest {
    @Test fun `all features receive one of four approved geometry arrangements`() {
        val styles = TitliFeature.entries.groupBy { it.menuArtwork() }
        assertEquals(TutorialMenuArtwork.entries.toSet(), styles.keys)
        assertTrue(styles.values.all { it.size >= 2 })
    }
    @Test fun `soft geometry text buttons and badges have readable light and dark contrast`() {
        fun contrast(a: Color, b: Color): Float {
            val x = a.luminance(); val y = b.luminance()
            return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
        }
        listOf(false, true).forEach { dark ->
            val c = tutorialVisualColors(dark)
            assertTrue(contrast(c.ink, c.canvas) >= 4.5f)
            assertTrue(contrast(c.ink, c.card) >= 4.5f)
            assertTrue(contrast(c.muted, c.card) >= 4.5f)
            assertTrue(contrast(c.onAction, c.action) >= 4.5f)
            assertTrue(contrast(c.ink, c.peach) >= 3f)
            assertTrue(contrast(c.ink, c.mint) >= 3f)
            val art = tutorialArtPalette(dark)
            assertEquals(5, setOf(art.coral, art.yellow, art.lilac, art.sage, art.pink).size)
            assertTrue(listOf(art.coral, art.yellow, art.lilac, art.sage, art.pink).all { it.alpha == 1f })
        }
    }
}
