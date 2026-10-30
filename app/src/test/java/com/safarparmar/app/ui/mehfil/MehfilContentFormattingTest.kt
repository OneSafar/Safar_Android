package com.safarparmar.app.ui.mehfil

import org.junit.Assert.assertEquals
import org.junit.Test

class MehfilContentFormattingTest {
    @Test
    fun separatesAnExplicitHeadingWithoutLosingBodyParagraphs() {
        assertEquals(
            "Revision question" to "First paragraph.\n\nSecond paragraph.",
            splitMehfilContent("Revision question\n\nFirst paragraph.\n\nSecond paragraph."),
        )
    }

    @Test
    fun keepsAnOrdinaryPostAsBodyCopy() {
        assertEquals(null to "An ordinary post without a heading.", splitMehfilContent("An ordinary post without a heading."))
    }

    @Test
    fun doesNotTurnALongWrappedParagraphIntoAHeading() {
        val body = "A long paragraph. ".repeat(20) + "\nAnother paragraph."
        assertEquals(null to body, splitMehfilContent(body))
    }
    @Test
    fun composerHeadingAndBodySurviveTheContentApiRoundTrip() {
        val encoded = composeMehfilContent("  My heading  ", "  First paragraph.\n\nSecond paragraph.  ")
        assertEquals("My heading" to "First paragraph.\n\nSecond paragraph.", splitMehfilContent(encoded))
    }

    @Test
    fun composerLimitsHeadingToTheLengthTheFeedRecognizes() {
        val encoded = composeMehfilContent("x".repeat(200), "Body")
        assertEquals("x".repeat(180) to "Body", splitMehfilContent(encoded))
    }
}
