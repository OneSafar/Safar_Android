package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeVerifiedVideoTest {
    private val productive = YoutubeChannelClassification.PRODUCTIVE
    private val title = "20 Minutes of BEST Plays"

    private fun video(handle: String? = null, display: String? = null, title: String? = this.title, ad: Boolean = false) =
        YoutubeV2Observation(
            kind = YoutubeV2ContentKind.VIDEO,
            watchScreenConfirmed = true,
            title = title,
            exactHandle = handle,
            displayName = display,
            adPlaying = ad,
        )

    private val verified = YoutubeVerifiedVideo.from(video(handle = "@EZVAL789", display = "EZVAL"))

    @Test fun `same video keeps permission when only the name is visible`() {
        assertTrue(retainsVerifiedPermission(verified, video(display = "EZVAL"), productive))
    }

    @Test fun `same video keeps permission when only the handle is visible`() {
        assertTrue(retainsVerifiedPermission(verified, video(handle = "@ezval789"), productive))
    }

    @Test fun `same video keeps permission when owner evidence is briefly gone`() {
        assertTrue(retainsVerifiedPermission(verified, video(), productive))
    }

    @Test fun `title change is re-evaluated`() {
        assertFalse(retainsVerifiedPermission(verified, video(display = "EZVAL", title = "Next video"), productive))
    }

    @Test fun `contradicting handle is re-evaluated`() {
        assertFalse(retainsVerifiedPermission(verified, video(handle = "@someoneelse"), productive))
    }

    @Test fun `contradicting display name is re-evaluated`() {
        assertFalse(retainsVerifiedPermission(verified, video(display = "Someone Else"), productive))
    }

    @Test fun `non-productive classification never retains`() {
        assertFalse(retainsVerifiedPermission(verified, video(display = "EZVAL"), null))
        assertFalse(retainsVerifiedPermission(verified, video(display = "EZVAL"), YoutubeChannelClassification.DISTRACTING))
    }

    @Test fun `ads and non-video screens are not retained`() {
        assertFalse(retainsVerifiedPermission(verified, video(display = "EZVAL", ad = true), productive))
        assertFalse(retainsVerifiedPermission(verified, video().copy(kind = YoutubeV2ContentKind.SHORTS), productive))
        assertFalse(retainsVerifiedPermission(null, video(display = "EZVAL"), productive))
    }

    @Test fun `evidence seen later is merged without overwriting`() {
        val handleOnly = YoutubeVerifiedVideo.from(video(handle = "@EZVAL789"))!!
        val merged = handleOnly.mergedWith(video(handle = "@other", display = "EZVAL"))
        assertEquals("@ezval789", merged.handle)
        assertEquals("ezval", merged.displayName)
        assertFalse(retainsVerifiedPermission(merged, video(display = "Other Name"), productive))
    }

    @Test fun `video without a title cannot be verified`() {
        assertEquals(null, YoutubeVerifiedVideo.from(video(handle = "@EZVAL789", title = null)))
    }
}
