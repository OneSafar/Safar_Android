package com.safarparmar.app.feature.youtubestudyv2

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeStudyV2RepositoryTest {
    private val database = mockk<YoutubeStudyV2Database>(relaxed = true)
    private val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
    private val repository = YoutubeStudyV2Repository(database, dao)

    @Test
    fun `same channel is blocked after local allowlist changes`() = runTest {
        val channelId = "accessibility:handle:@parmarssc"
        coEvery { dao.channelIdForHandle("@parmarssc") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns true
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, repository.decide("@parmarssc", null))
        coEvery { dao.isAllowed(channelId) } returns false
        assertEquals(YoutubeV2RuntimeDecision.BLOCK, repository.decide("@parmarssc", null))
    }

    @Test
    fun `unknown runtime handle blocks immediately`() = runTest {
        coEvery { dao.channelIdForHandle("@unknownchannel") } returns null

        val decision = repository.decide("@UnknownChannel", null)

        assertEquals(YoutubeV2RuntimeDecision.BLOCK, decision)
    }

    @Test
    fun `manually verified productive handle is allowed using local data only`() = runTest {
        val channelId = "accessibility:handle:@safarparmar"
        coEvery { dao.channelIdForHandle("@safarparmar") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns true

        val decision = repository.decide("@SAFARPARMAR", null)

        assertEquals(YoutubeV2RuntimeDecision.ALLOW, decision)
    }

    @Test
    fun `known distracting handle blocks using local data only`() = runTest {
        val channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        coEvery { dao.channelIdForHandle("@googledevelopers") } returns channelId
        coEvery { dao.classificationForChannelId(channelId) } returns YoutubeV2ClassificationEntity(channelId, "distracting", 1L)

        val evaluation = repository.evaluate("@GoogleDevelopers", null)

        assertEquals(YoutubeV2RuntimeDecision.BLOCK, evaluation.decision)
        assertEquals(YoutubeChannelClassification.DISTRACTING, evaluation.classification)
        assertEquals(channelId, evaluation.channelId)
    }
    @Test fun `exact normalized display name is allowed`() = runTest {
        val id = "accessibility:display:parmar ssc"
        coEvery { dao.channelIdsForDisplay("parmar ssc") } returns listOf(id)
        coEvery { dao.isAllowed(id) } returns true
        assertEquals(id, repository.evaluate(null, "  PARMAR   SSC ").channelId)
    }

    @Test fun `different display name remains blocked`() = runTest {
        coEvery { dao.channelIdsForDisplay("parmar ssc classes") } returns emptyList()
        assertEquals(YoutubeV2RuntimeDecision.BLOCK, repository.decide(null, "Parmar SSC Classes"))
    }

    @Test fun `local allow rule does not expire`() = runTest {
        val id = "accessibility:handle:@example"
        coEvery { dao.channelIdForHandle("@example") } returns id
        coEvery { dao.isAllowed(id) } returns true
        assertEquals(id, repository.evaluate("@example", null).channelId)
    }

    @Test fun `resolved canonical ID uses its existing category`() = runTest {
        val id = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        coEvery { dao.classificationForChannelId(id) } returns YoutubeV2ClassificationEntity(id, "productive", 1)
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, repository.evaluateChannelId(id).decision)
        coEvery { dao.classificationForChannelId(id) } returns null
        assertEquals(YoutubeChannelClassification.OTHERS, repository.evaluateChannelId(id).classification)
    }

}
