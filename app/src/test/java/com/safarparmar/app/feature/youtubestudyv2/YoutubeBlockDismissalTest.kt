package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeBlockDismissalTest {
    @Test fun `return home suppresses repeated mini player and pip sheets until a new video`() {
        val dismissal = YoutubeBlockDismissal()
        assertFalse(dismissal.suppressFloatingSheet)
        dismissal.onReturnHome()
        repeat(3) { assertTrue(dismissal.suppressFloatingSheet) }
        dismissal.onVideoEntered()
        assertFalse(dismissal.suppressFloatingSheet)
    }
}
