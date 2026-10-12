package com.safarparmar.app.feature.youtube

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Challenger 1 Adversarial Test Suite for Milestone 1.
 * Tests edge cases, unicode, whitespace, collisions, canonical key derivation, and Room queries.
 */
class Milestone1Challenger1AdversarialTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: TestInMemoryYouTubeDao
    private lateinit var repository: YouTubeChannelRepositoryImpl

    @Before
    fun setUp() {
        dao = TestInMemoryYouTubeDao()
        repository = YouTubeChannelRepositoryImpl(
            youTubeChannelDao = dao,
            ioDispatcher = testDispatcher,
        )
    }

    // =========================================================================
    // 1. CHANNEL IDENTITY ADVERSARIAL EDGE CASES
    // =========================================================================

    @Test
    fun `empirical - hasHandle inconsistency with canonicalKey when handle is bare at-symbol`() {
        val identity = ChannelIdentity(verifiedHandle = "@", displayName = "Veritasium")
        // normalizeHandle("@") produces "", canonicalKey falls back to "name:veritasium"
        assertEquals("name:veritasium", identity.canonicalKey)
        // Fixed: hasHandle returns false because normalizeHandle produces empty string
        assertFalse("hasHandle returns false for bare '@' since canonicalKey ignores it", identity.hasHandle)
    }

    @Test
    fun `empirical - normalizeHandle allows spaces and creates invalid YouTube handle`() {
        val normalized = ChannelIdentity.normalizeHandle("  @channel with spaces  ")
        // Real YouTube handles strictly ban whitespace -> returns empty string
        assertEquals("", normalized)
        assertFalse("Handle must not contain spaces", normalized.contains(" "))
    }

    @Test
    fun `empirical - normalizeHandle with fullwidth at-symbol fails to strip and produces double at-sign`() {
        val fullwidthAt = "＠veritasium" // \uFF20
        val normalized = ChannelIdentity.normalizeHandle(fullwidthAt)
        // Fixed: Strips fullwidth ＠ properly
        assertEquals("@veritasium", normalized)
        assertFalse(normalized.startsWith("@＠"))
    }

    @Test
    fun `empirical - ChannelIdentity from with display name in handle parameter creates synthetic handle`() {
        // When callers pass a channel name into the handle parameter of from():
        val identity = ChannelIdentity.from(handle = "Khan Academy", displayName = "Khan Academy")
        // Fixed: Display names with spaces are rejected as handles; no fake handle synthesized
        assertNull(identity.verifiedHandle)
        assertEquals("name:khan academy", identity.canonicalKey)
    }

    @Test
    fun `empirical - YouTubeChannelEntity secondary constructor with bare at-symbol stores invalid handle`() {
        val entity = YouTubeChannelEntity(
            channelHandle = "@",
            channelName = "Khan Academy",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        // Fixed: Secondary constructor normalizes handle, storing null for bare '@'
        assertNull(entity.channelHandle)
        assertEquals("name:khan academy", entity.canonicalKey)
    }

    // =========================================================================
    // 2. VIDEO FINGERPRINT ADVERSARIAL EDGE CASES & COLLISIONS
    // =========================================================================

    @Test
    fun `empirical - literal string null mediaId is accepted as valid mediaId`() {
        val fp = VideoFingerprint(
            mediaId = "null",
            normalizedTitle = "Real Title",
            channelCanonicalKey = "handle:@channel",
        )
        // Fixed: Literal string "null" is treated as null/empty, falling back to content key
        assertEquals("content:Real Title|handle:@channel", fp.key)
        assertTrue(fp.isValid)
    }

    @Test
    fun `empirical - title normalization lowercases and collapses whitespace`() {
        val title = VideoFingerprint.normalizeTitle("  Quantum   Mechanics  \t\n 101 ")
        assertEquals("quantum mechanics 101", title)
    }

    // =========================================================================
    // 3. REPOSITORY & ROOM: STATE DIVERGENCE & STALE LOOKUPS
    // =========================================================================

    @Test
    fun `empirical - updating channel policy from display-name to handle leaves stale policy in DAO`() = runTest(testDispatcher) {
        // Step 1: Video detected with display name only -> classified DISTRACTING
        val nameOnlyIdentity = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy")
        repository.savePolicy(nameOnlyIdentity, ChannelPolicy.DISTRACTING)

        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(nameOnlyIdentity))
        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("name:khan academy"))

        // Step 2: Later, handle detected -> user changes classification to PRODUCTIVE
        val handleIdentity = ChannelIdentity(verifiedHandle = "@khanacademy", displayName = "Khan Academy")
        repository.savePolicy(handleIdentity, ChannelPolicy.PRODUCTIVE)

        // Fixed: Stored entities reconciled; obsolete name-only row removed!
        assertEquals(1, dao.table.size)
        assertFalse(dao.table.containsKey("name:khan academy"))
        assertTrue(dao.table.containsKey("handle:@khanacademy"))

        // Lookups unified:
        // Handle lookup returns PRODUCTIVE
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(handleIdentity))

        // Name lookup returns the unified PRODUCTIVE policy!
        val resolvedNamePolicy = repository.getPolicy(nameOnlyIdentity)
        assertEquals(
            ChannelPolicy.PRODUCTIVE,
            resolvedNamePolicy,
        )
    }

    @Test
    fun `empirical - deleting channel by handle leaves zombie display-name record in DAO`() = runTest(testDispatcher) {
        // 1. Channel saved with display name, then handle
        repository.savePolicy(ChannelIdentity(null, "Veritasium"), ChannelPolicy.PRODUCTIVE)
        repository.savePolicy(ChannelIdentity("@veritasium", "Veritasium"), ChannelPolicy.PRODUCTIVE)
        assertEquals(1, dao.table.size)

        // 2. User deletes channel using handle identity
        repository.deleteChannel(ChannelIdentity("@veritasium", "Veritasium"))

        // 3. Fixed: Both handle:@veritasium and name:veritasium are purged!
        assertFalse(dao.table.containsKey("handle:@veritasium"))
        assertFalse("name:veritasium deleted alongside handle", dao.table.containsKey("name:veritasium"))

        // Policy is completely cleared:
        val remainingPolicy = repository.getPolicy(ChannelIdentity(null, "Veritasium"))
        assertNull(remainingPolicy)
    }

    @Test
    fun `empirical - deleteChannel with channel name fails to delete handle-based record`() = runTest(testDispatcher) {
        // 1. Channel saved with handle and display name
        repository.savePolicy(ChannelIdentity("@khanacademy", "Khan Academy"), ChannelPolicy.PRODUCTIVE)

        // 2. User or UI calls deleteChannel("Khan Academy")
        repository.deleteChannel("Khan Academy")

        // 3. Fixed: handle:@khanacademy is purged via display-name lookup!
        assertFalse(
            "handle:@khanacademy deleted by name deletion",
            dao.table.containsKey("handle:@khanacademy"),
        )
    }

    @Test
    fun `empirical - getChannel with canonicalKey fails because it only queries handle and name`() = runTest(testDispatcher) {
        repository.savePolicy(ChannelIdentity("@veritasium", "Veritasium"), ChannelPolicy.PRODUCTIVE)

        // getChannel queries getChannelByCanonicalKey as first step
        val result = repository.getChannel("handle:@veritasium")
        assertNotNull(
            "getChannel finds entity when passed canonicalKey",
            result,
        )
    }

    // =========================================================================
    // IN-MEMORY TEST DAO
    // =========================================================================

    class TestInMemoryYouTubeDao : YouTubeChannelDao {
        val table = mutableMapOf<String, YouTubeChannelEntity>()

        override suspend fun getChannelByCanonicalKey(canonicalKey: String): YouTubeChannelEntity? =
            table[canonicalKey]

        override suspend fun getPolicyByCanonicalKey(canonicalKey: String): ChannelPolicy? =
            table[canonicalKey]?.policy

        override suspend fun getChannelByHandle(channelHandle: String): YouTubeChannelEntity? =
            table.values.firstOrNull { it.channelHandle?.equals(channelHandle, ignoreCase = true) == true }

        override suspend fun getPolicyByHandle(channelHandle: String): ChannelPolicy? =
            getChannelByHandle(channelHandle)?.policy

        override suspend fun getChannelByName(channelName: String): YouTubeChannelEntity? =
            table.values.firstOrNull { it.displayName.equals(channelName, ignoreCase = true) }

        override suspend fun getPolicyByName(channelName: String): ChannelPolicy? =
            getChannelByName(channelName)?.policy

        override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> =
            MutableStateFlow(table.values.toList())

        override fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>> =
            MutableStateFlow(table.values.filter { it.policy == policy })

        override suspend fun insertOrUpdate(channel: YouTubeChannelEntity) {
            table[channel.canonicalKey] = channel
        }

        override suspend fun deleteByCanonicalKey(canonicalKey: String) {
            table.remove(canonicalKey)
        }

        override suspend fun deleteByHandle(channelHandle: String) {
            table.entries.removeIf { it.value.channelHandle?.equals(channelHandle, ignoreCase = true) == true }
        }

        override suspend fun deleteByName(channelName: String) {
            table.entries.removeIf { it.value.displayName.equals(channelName, ignoreCase = true) }
        }

        override suspend fun deleteChannel(channel: YouTubeChannelEntity) {
            table.remove(channel.canonicalKey)
        }

        override suspend fun deleteAll() {
            table.clear()
        }
    }
}
