package com.safarparmar.app.feature.youtubestudyv2

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeStudyV2RepositoryTest {
    private val database = mockk<YoutubeStudyV2Database>(relaxed = true)
    private val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
    private val api = mockk<YoutubeStudyV2Api>(relaxed = true)
    private val repository = YoutubeStudyV2Repository(database, dao, api)

    @Test
    fun `unknown runtime handle blocks immediately without waiting for API discovery`() = runTest {
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
    fun `known distracting handle blocks using local data only`() = runTest {
        val channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        coEvery { dao.channelIdForHandle("@googledevelopers") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns false

        val decision = repository.decide("@GoogleDevelopers", null)

        assertEquals(YoutubeV2RuntimeDecision.BLOCK, decision)
        coVerify(exactly = 0) { api.resolve(any()) }
    }

    @Test
    fun `already discovered handle is not registered twice`() = runTest {
        val channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        val entity = YoutubeV2IdentityEntity(
            channelId = channelId,
            handle = "@googledevelopers",
            displayName = "Google Developers",
            thumbnailUrl = null,
            resolvedAtMs = 12345L,
        )
        coEvery { dao.channelIdForHandle("@googledevelopers") } returns channelId
        coEvery { dao.identityForChannelId(channelId) } returns entity

        assertEquals(entity, repository.registerDiscoveredHandle("@GoogleDevelopers").getOrThrow())
        coVerify(exactly = 0) { api.resolve(any()) }
        coVerify(exactly = 0) { api.discover(any()) }
    }

    @Test
    fun `automatic discovery never submits a display name`() = runTest {
        assertEquals(null, repository.registerDiscoveredHandle("Parmar SSC").getOrThrow())
        coVerify(exactly = 0) { api.discover(any()) }
    }
}
