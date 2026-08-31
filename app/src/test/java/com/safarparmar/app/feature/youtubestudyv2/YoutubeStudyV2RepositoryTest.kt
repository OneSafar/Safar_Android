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
    fun `unknown runtime handle blocks without API discovery or database writes`() = runTest {
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
}
