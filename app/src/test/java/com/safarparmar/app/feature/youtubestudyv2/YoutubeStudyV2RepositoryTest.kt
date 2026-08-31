package com.safarparmar.app.feature.youtubestudyv2

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeStudyV2RepositoryTest {
    private val database = mockk<YoutubeStudyV2Database>(relaxed = true)
    private val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
    private val api = mockk<YoutubeStudyV2Api>(relaxed = true)
    private val preferences = mockk<YoutubeStudyV2Preferences>(relaxed = true) {
        every { allowedCategories } returns MutableStateFlow(setOf("education", "science_tech"))
    }
    private val repository = YoutubeStudyV2Repository(database, dao, api, preferences)

    @Test
    fun `unknown runtime handle with no active categories blocks without API discovery`() = runTest {
        every { preferences.allowedCategories } returns MutableStateFlow(emptySet())
        coEvery { dao.channelIdForHandle("@unknownchannel") } returns null

        val decision = repository.decide("@UnknownChannel", null)

        assertEquals(YoutubeV2RuntimeDecision.BLOCK, decision)
        coVerify(exactly = 0) { api.resolve(any()) }
        coVerify(exactly = 0) { dao.upsertIdentity(any()) }
        coVerify(exactly = 0) { dao.upsertAliases(any()) }
    }

    @Test
    fun `manually verified productive handle is allowed using local data only`() = runTest {
        val channelId = "UCsbT4wZ_FUUpJGtVa4mooow"
        coEvery { dao.channelIdForHandle("@safarparmar") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns true

        val decision = repository.decide("@SAFARPARMAR", null)

        assertEquals(YoutubeV2RuntimeDecision.ALLOW, decision)
        coVerify(exactly = 0) { api.resolve(any()) }
    }

    @Test
    fun `channel belonging to enabled category is allowed even if not individually whitelisted`() = runTest {
        every { preferences.allowedCategories } returns MutableStateFlow(setOf("education", "science_tech"))
        val channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        coEvery { dao.channelIdForHandle("@googledevelopers") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns false
        coEvery { dao.identityForChannelId(channelId) } returns YoutubeV2IdentityEntity(
            channelId = channelId,
            handle = "@googledevelopers",
            displayName = "Google Developers",
            thumbnailUrl = null,
            categories = "science_tech,education",
            resolvedAtMs = 12345L,
        )

        val decision = repository.decide("@GoogleDevelopers", null)

        assertEquals(YoutubeV2RuntimeDecision.ALLOW, decision)
    }
}
