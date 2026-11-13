package com.safarparmar.app.feature.youtubestudyv2

import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The add-channel screen sends the @handle and the optional name shown under videos together. */
class YoutubeAddChannelPairingTest {
    private val database = mockk<YoutubeStudyV2Database>(relaxed = true)
    private val dao = mockk<YoutubeStudyV2Dao>(relaxed = true)
    private val repository = YoutubeStudyV2Repository(database, dao)

    @Before fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }
    }

    @After fun tearDown() = unmockkStatic("androidx.room.RoomDatabaseKt")

    @Test fun `handle link plus channel name saves both aliases`() = runTest {
        val result = repository.resolveAndAllow("https://youtube.com/@EZVAL789/videos", "EZVAL")

        assertTrue(result.isSuccess)
        val channelId = "accessibility:handle:@ezval789"
        assertEquals(channelId, result.getOrThrow().channel.channelId)
        assertEquals("EZVAL", result.getOrThrow().channel.displayName)
        coVerify {
            dao.upsertAliases(match { aliases ->
                aliases.any { it.aliasType == "handle" && it.normalizedAlias == "@ezval789" && it.channelId == channelId } &&
                    aliases.any { it.aliasType == "display" && it.normalizedAlias == "ezval" && it.channelId == channelId }
            })
        }
        coVerify { dao.allow(match { it.channelId == channelId }) }
    }

    @Test fun `name-only read matches a channel added with both fields`() = runTest {
        val channelId = "accessibility:handle:@ezval789"
        coEvery { dao.channelIdsForDisplay("ezval") } returns listOf(channelId)
        coEvery { dao.isAllowed(channelId) } returns true
        assertEquals(YoutubeV2RuntimeDecision.ALLOW, repository.evaluate(null, "EZVAL").decision)
    }

    @Test fun `channel id link is rejected with a reason`() = runTest {
        val result = repository.resolveAndAllow("https://www.youtube.com/channel/UCiGyWN6DEbnj2alu7iapuKQ", "EZVAL")
        assertEquals(YoutubeChannelReference.CHANNEL_ID_LINK_MESSAGE, result.exceptionOrNull()?.message)
    }
}
