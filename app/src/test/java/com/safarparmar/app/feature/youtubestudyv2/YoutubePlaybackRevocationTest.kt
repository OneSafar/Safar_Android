package com.safarparmar.app.feature.youtubestudyv2

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class YoutubePlaybackRevocationTest {
    private val channelId = "accessibility:handle:@teacher"
    private val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
    private val repository = YoutubeStudyV2Repository(mockk(relaxed = true), dao)

    private suspend fun classification(rules: Map<String, YoutubeChannelClassification>) =
        currentYoutubePlaybackClassification(channelId, YoutubeChannelClassification.PRODUCTIVE, rules) {
            repository.evaluateChannelId(it).classification
        }

    @Test fun `deleting a productive channel revokes floating and hidden fullscreen playback`() = runTest {
        coEvery { dao.classificationForChannelId(channelId) } returns null
        val current = classification(emptyMap())
        assertEquals(YoutubeChannelClassification.OTHERS, current)
        assertTrue(shouldBlockYoutubePip(false, current))
        assertFalse(canRetainFullscreenPermission("Lesson", null, current))
    }

    @Test fun `older first snapshot does not revoke a newly saved productive channel`() = runTest {
        coEvery { dao.classificationForChannelId(channelId) } returns
            YoutubeV2ClassificationEntity(channelId, "productive", 100L)
        val current = classification(emptyMap())
        assertEquals(YoutubeChannelClassification.PRODUCTIVE, current)
        assertFalse(shouldBlockYoutubePip(false, current))
        assertTrue(canRetainFullscreenPermission("Lesson", null, current))
    }

    @Test fun `reclassifying a productive channel revokes both playback grants`() = runTest {
        val current = classification(mapOf(channelId to YoutubeChannelClassification.DISTRACTING))
        assertEquals(YoutubeChannelClassification.DISTRACTING, current)
        assertTrue(shouldBlockYoutubePip(false, current))
        assertFalse(canRetainFullscreenPermission("Lesson", null, current))
    }

    @Test fun `an added and verified channel survives HUD timeout but cannot override revoked rules`() = runTest {
        val portrait = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true,
            title = "Lesson", exactHandle = "@teacher", canResumeWatchSession = true)
        val fullscreen = portrait.copy(fullscreen = true, title = null, exactHandle = null,
            canResumeWatchSession = false)
        val transition = YoutubeFullscreenTransition()
        transition.observe(portrait, 100L)
        transition.markVerified(portrait)
        val effective = transition.observe(fullscreen, 60_000L)
        assertEquals(portrait.stableKey, effective.stableKey)

        coEvery { dao.classificationForChannelId(channelId) } returns null
        val current = classification(emptyMap())
        assertFalse(canRetainFullscreenPermission(effective.title, null, current))
        assertTrue(shouldBlockYoutubePip(false, current))
    }
}
