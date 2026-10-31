package com.safarparmar.app.ui.dhyan

import com.safarparmar.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DhyanTechniquesTest {

    @Test
    fun techniquesList_containsExactlyFiveTechniquesWithCorrectTimings() {
        assertEquals(5, techniques.size)

        val diaphragmatic = techniques[0]
        assertEquals(R.string.dhyan_technique_diaphragmatic, diaphragmatic.nameRes)
        assertEquals(4, diaphragmatic.inhale)
        assertEquals(0, diaphragmatic.hold)
        assertEquals(6, diaphragmatic.exhale)
        assertEquals(0, diaphragmatic.holdAfter)
        assertEquals("4-6", diaphragmatic.pattern)

        val pursedLip = techniques[1]
        assertEquals(R.string.dhyan_technique_pursed_lip, pursedLip.nameRes)
        assertEquals(2, pursedLip.inhale)
        assertEquals(0, pursedLip.hold)
        assertEquals(4, pursedLip.exhale)
        assertEquals(0, pursedLip.holdAfter)
        assertEquals("2-4", pursedLip.pattern)

        val box = techniques[2]
        assertEquals(R.string.dhyan_technique_box, box.nameRes)
        assertEquals(4, box.inhale)
        assertEquals(4, box.hold)
        assertEquals(4, box.exhale)
        assertEquals(4, box.holdAfter)
        assertEquals("4-4-4-4", box.pattern)

        val fourSevenEight = techniques[3]
        assertEquals(R.string.dhyan_technique_478, fourSevenEight.nameRes)
        assertEquals(4, fourSevenEight.inhale)
        assertEquals(7, fourSevenEight.hold)
        assertEquals(8, fourSevenEight.exhale)
        assertEquals(0, fourSevenEight.holdAfter)
        assertEquals("4-7-8", fourSevenEight.pattern)

        val sixSevenEight = techniques[4]
        assertEquals(R.string.dhyan_technique_678, sixSevenEight.nameRes)
        assertEquals(6, sixSevenEight.inhale)
        assertEquals(7, sixSevenEight.hold)
        assertEquals(8, sixSevenEight.exhale)
        assertEquals(0, sixSevenEight.holdAfter)
        assertEquals("6-7-8", sixSevenEight.pattern)
    }

    @Test
    fun techniqueSessionId_mapsEachTechniqueToExpectedVisualizerSessionId() {
        assertEquals("1", techniqueSessionId(techniques[0]))
        assertEquals("2", techniqueSessionId(techniques[1]))
        assertEquals("3", techniqueSessionId(techniques[2]))
        assertEquals("4", techniqueSessionId(techniques[3]))
        assertEquals("5", techniqueSessionId(techniques[4]))
        assertEquals("1", techniqueSessionId(null))
    }

    @Test
    fun techniqueSessionId_handlesDifferentInstanceWithSameNameRes() {
        val copiedTechnique = techniques[2].copy(audioUrl = "https://custom.audio/file.mp3")
        assertEquals("3", techniqueSessionId(copiedTechnique))
    }

    @Test
    fun breathCycle_matchesTechniqueTimingContract() {
        techniques.forEach { technique ->
            val cycle = BreathCycle(
                inhale = technique.inhale,
                holdIn = technique.hold,
                exhale = technique.exhale,
                holdOut = technique.holdAfter,
            )
            assertTrue("Inhale duration must be > 0", cycle.inhale > 0)
            assertTrue("Exhale duration must be > 0", cycle.exhale > 0)
            assertTrue("Hold duration must be >= 0", cycle.holdIn >= 0)
            assertTrue("HoldOut duration must be >= 0", cycle.holdOut >= 0)
        }
    }
}
