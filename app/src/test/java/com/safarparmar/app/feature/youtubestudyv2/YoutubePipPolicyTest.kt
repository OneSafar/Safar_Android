package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePipPolicyTest {
    @Test fun `distracting playback is blocked again when break expires in PiP`() {
        assertFalse(shouldBlockYoutubePip(true, YoutubeChannelClassification.DISTRACTING))
        assertTrue(shouldBlockYoutubePip(false, YoutubeChannelClassification.DISTRACTING))
    }

    @Test fun `productive playback continues after break expires`() {
        assertFalse(shouldBlockYoutubePip(false, YoutubeChannelClassification.PRODUCTIVE))
    }

    @Test fun `unidentified PiP cannot bypass channel checks`() {
        assertTrue(shouldBlockYoutubePip(false, null))
        assertTrue(shouldBlockYoutubePip(false, YoutubeChannelClassification.OTHERS))
        assertFalse(shouldBlockYoutubePip(true, null))
    }
}
