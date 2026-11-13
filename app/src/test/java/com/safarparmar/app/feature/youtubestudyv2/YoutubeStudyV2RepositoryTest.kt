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

    @Test fun `exact channel ID identifies an allowed class without handle or display`() = runTest {
        val id = "UC-teacher"
        coEvery { dao.isAllowed(id) } returns true
        val result = repository.evaluate(null, null, id)
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, result.decision)
        assertEquals(id, result.channelId)
    }

    @Test fun `handle candidate derived from display name matches allowed handle alias`() = runTest {
        val channelId = "accessibility:handle:@veritasium"
        coEvery { dao.channelIdForHandle("@veritasium") } returns channelId
        coEvery { dao.isAllowed(channelId) } returns true

        // Runtime only has display name "Veritasium" (no handle exposed by YouTube in portrait)
        val result = repository.evaluate(null, "Veritasium")
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, result.decision)
        assertEquals(channelId, result.channelId)
    }

    @Test fun `display candidate derived from handle matches allowed display alias`() = runTest {
        val channelId = "accessibility:display:veritasium"
        coEvery { dao.channelIdsForDisplay("veritasium") } returns listOf(channelId)
        coEvery { dao.isAllowed(channelId) } returns true

        // Runtime only has handle "@veritasium" in landscape HUD
        val result = repository.evaluate("@veritasium", null)
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, result.decision)
        assertEquals(channelId, result.channelId)
    }

    @Test fun `allowed handle teaches the paired display name for later name-only reads`() = runTest {
        val channelId = "accessibility:handle:@ezval789"
        coEvery { dao.channelIdsForHandle("@ezval789") } returns listOf(channelId)
        coEvery { dao.channelIdsForDisplay("ezval") } returns emptyList()
        coEvery { dao.isAllowed(channelId) } returns true

        val result = repository.evaluate("@EZVAL789", "EZVAL")

        assertEquals(YoutubeV2RuntimeDecision.ALLOW, result.decision)
        coVerify {
            dao.upsertAliases(listOf(YoutubeV2AliasEntity("display", "ezval", channelId, "EZVAL")))
        }
    }

    @Test fun `name-only read matches once the paired name was learned`() = runTest {
        val channelId = "accessibility:handle:@ezval789"
        coEvery { dao.channelIdsForDisplay("ezval") } returns listOf(channelId)
        coEvery { dao.isAllowed(channelId) } returns true

        assertEquals(YoutubeV2RuntimeDecision.ALLOW, repository.evaluate(null, "EZVAL").decision)
    }

    @Test fun `allowed unique display name teaches the paired handle`() = runTest {
        val channelId = "accessibility:display:physics wallah - alakh pandey"
        coEvery { dao.channelIdsForHandle("@physicswallah") } returns emptyList()
        coEvery { dao.channelIdForHandle("@physicswallah") } returns null
        coEvery { dao.channelIdsForDisplay("physics wallah - alakh pandey") } returns listOf(channelId)
        coEvery { dao.isAllowed(channelId) } returns true

        repository.evaluate("@PhysicsWallah", "Physics Wallah - Alakh Pandey")

        coVerify {
            dao.upsertAliases(listOf(YoutubeV2AliasEntity("handle", "@physicswallah", channelId, "@physicswallah")))
        }
    }

    @Test fun `display name already owned by another channel is not learned`() = runTest {
        val allowed = "accessibility:handle:@ezval789"
        coEvery { dao.channelIdsForHandle("@ezval789") } returns listOf(allowed)
        coEvery { dao.channelIdsForDisplay("ezval") } returns listOf("accessibility:handle:@otherezval")
        coEvery { dao.isAllowed(allowed) } returns true

        repository.evaluate("@EZVAL789", "EZVAL")

        coVerify(exactly = 0) { dao.upsertAliases(any()) }
    }

    @Test fun `blocked channel teaches nothing`() = runTest {
        coEvery { dao.channelIdsForHandle("@randomchannel") } returns emptyList()
        coEvery { dao.channelIdForHandle("@randomchannel") } returns null
        coEvery { dao.channelIdsForDisplay("random") } returns emptyList()

        val result = repository.evaluate("@randomchannel", "Random")

        assertEquals(YoutubeV2RuntimeDecision.BLOCK, result.decision)
        coVerify(exactly = 0) { dao.upsertAliases(any()) }
    }
}
