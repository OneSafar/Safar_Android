package com.safarparmar.app.ui.calmcentre

import org.junit.Assert.*
import org.junit.Test

class CalmPracticeTest {
    @Test fun selectableBreathingPatternsUseRealPhaseTimings() {
        val box = CalmBreathTechnique.BOX
        assertEquals("Breathe in", calmBreathFrame(3999, box).title)
        assertEquals("Hold gently", calmBreathFrame(4000, box).title)
        assertEquals(1f, calmBreathFrame(7999, box).expansion, 0.001f)
        assertEquals("Breathe out", calmBreathFrame(8000, box).title)
        assertEquals("Rest softly", calmBreathFrame(12000, box).title)
        assertEquals(0f, calmBreathFrame(15999, box).expansion, 0.001f)
        assertEquals("Breathe in", calmBreathFrame(16000, box).title)
        assertEquals("Breathe in", calmBreathFrame(8000, CalmBreathTechnique.BELLY).title)
        assertEquals("Breathe out", calmBreathFrame(11000, CalmBreathTechnique.LONG_EXHALE).title)
        assertEquals("Breathe in", calmBreathFrame(12000, CalmBreathTechnique.LONG_EXHALE).title)
    }

    @Test fun everyTechniqueHasAContinuousBoundedAnimationAndTextProgress() {
        CalmBreathTechnique.entries.forEach { technique ->
            for (time in 0L..40_000L step 16L) {
                val frame = calmBreathFrame(time, technique)
                assertTrue(frame.expansion in 0f..1f)
                assertTrue(frame.progress in 0f..1f)
                assertTrue(kotlin.math.abs(frame.expansion - calmBreathFrame(time + 16, technique).expansion) < 0.01f)
                if (technique == CalmBreathTechnique.GENTLE) {
                    assertEquals(calmBreathExpansion(time), frame.expansion, 0.00001f)
                }
            }
        }
    }
    @Test fun textFillTracksBreathingAndMoonMovementBoundaries() {
        assertEquals(0.5f, calmCueProgress(CalmPractice.BREATHE, 2000L, 60000L), 0.001f)
        assertEquals(0f, calmCueProgress(CalmPractice.BREATHE, 4000L, 60000L), 0.001f)
        assertEquals(0.5f, calmCueProgress(CalmPractice.BREATHE, 7000L, 60000L), 0.001f)
        assertEquals(0f, calmCueProgress(CalmPractice.REST, 15000L, 60000L), 0.001f)
        assertEquals(1f, calmCueProgress(CalmPractice.REST, 19500L, 60000L), 0.001f)
    }
    @Test fun kindnessCompanionsExchangePlacesAndCloseTheirOrbit() {
        val start = calmCompanionPose(0L, false)
        val halfway = calmCompanionPose(12_000L, false)
        val otherStart = calmCompanionPose(0L, true)
        assertEquals(otherStart.x, halfway.x, 0.001f)
        assertEquals(start, calmCompanionPose(24_000L, false))
        for (t in 0L..24_000L step 16L) {
            val a = calmCompanionPose(t, false)
            val b = calmCompanionPose(t, true)
            assertEquals(1000f, a.x + b.x, 0.001f)
            assertEquals(800f, a.y + b.y, 0.001f)
            assertTrue(a.scale in 0.88f..1.12f)
            assertTrue(kotlin.math.abs(a.x - calmCompanionPose(t + 16L, false).x) < 1f)
        }
    }

    @Test fun windDownMoonFollowsCueStepsWithoutBoundaryJumps() {
        for (duration in listOf(60_000L, 120_000L, 300_000L)) {
            assertEquals(280f, calmRestMoonY(5000L, duration), 0.001f)
            assertEquals(355f, calmRestMoonY(duration / 4 + 5000, duration), 0.001f)
            assertEquals(300f, calmRestMoonY(duration / 2 + 5000, duration), 0.001f)
            assertEquals(425f, calmRestMoonY(duration * 3 / 4 + 5000, duration), 0.001f)
            for (step in 1..3) {
                val boundary = duration * step / 4
                assertEquals(calmRestMoonY(boundary - 1, duration), calmRestMoonY(boundary, duration), 0.001f)
            }
        }
    }

    @Test fun explicitIntentionTakesPriorityOverMood() {
        CalmMood.entries.forEach { mood ->
            assertEquals(CalmPractice.KINDNESS, recommendCalmPractice(mood, CalmIntention.SUPPORT).practice)
            assertEquals(CalmPractice.GROUND, recommendCalmPractice(mood, CalmIntention.FOCUS).practice)
            assertEquals(CalmPractice.REST, recommendCalmPractice(mood, CalmIntention.REST).practice)
        }
    }

    @Test fun resetMeetsTheUsersCurrentMood() {
        val expected = listOf(CalmPractice.BREATHE, CalmPractice.KINDNESS, CalmPractice.GROUND, CalmPractice.REST, CalmPractice.BREATHE)
        CalmMood.entries.zip(expected).forEach { (mood, practice) ->
            assertEquals(practice, recommendCalmPractice(mood, CalmIntention.RESET).practice)
        }
    }

    @Test fun blossomAndInstructionsShareExactFourSixBoundaries() {
        assertEquals("Breathe in", calmCue(CalmPractice.BREATHE, 3999L, 60_000L).title)
        assertEquals("Breathe out", calmCue(CalmPractice.BREATHE, 4000L, 60_000L).title)
        assertEquals("Breathe in", calmCue(CalmPractice.BREATHE, 10_000L, 60_000L).title)
        assertEquals(0f, calmBreathExpansion(0L), 0.00001f)
        assertEquals(1f, calmBreathExpansion(4000L), 0.00001f)
        assertEquals(0f, calmBreathExpansion(10_000L), 0.00001f)
        assertTrue(calmBreathExpansion(2000L) < calmBreathExpansion(3000L))
        assertTrue(calmBreathExpansion(6000L) > calmBreathExpansion(7000L))
    }

    @Test fun breathingCurveIsContinuousAndBoundedAcrossManyCycles() {
        for (t in 0L..30_000L step 16L) {
            val value = calmBreathExpansion(t)
            assertTrue(value in 0f..1f)
            assertTrue(kotlin.math.abs(value - calmBreathExpansion(t + 16L)) < 0.01f)
        }
    }

    @Test fun sensorySequenceUsesAllFiveStepsAtEveryDuration() {
        for (minutes in listOf(1, 2, 5)) {
            val duration = minutes * 60_000L
            val prompts = (0..4).map { calmCue(CalmPractice.GROUND, it * duration / 5, duration).title }
            assertEquals(listOf("Notice five things", "Feel four things", "Listen for three sounds", "Notice two scents", "One small taste"), prompts)
            assertEquals(prompts.last(), calmCue(CalmPractice.GROUND, duration, duration).title)
        }
    }

    @Test fun countdownRoundsUpWithoutShowingNegativeTime() {
        assertEquals("2:00", calmTimeLabel(120_000))
        assertEquals("1:00", calmTimeLabel(59_999))
        assertEquals("0:01", calmTimeLabel(1))
        assertEquals("0:00", calmTimeLabel(0))
        assertEquals("0:00", calmTimeLabel(-1000))
    }
}
