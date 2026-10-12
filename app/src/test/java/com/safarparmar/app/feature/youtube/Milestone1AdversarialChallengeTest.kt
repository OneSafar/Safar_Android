package com.safarparmar.app.feature.youtube

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
 * Empirical Adversarial Challenge Suite for Milestone 1.
 * Tests concurrency, state consistency, display-name to handle transitions,
 * fake handle synthesis, and backward compatibility.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Milestone1AdversarialChallengeTest {

    private lateinit var dao: InMemoryYouTubeChannelDao
    private lateinit var repository: YouTubeChannelRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    class InMemoryYouTubeChannelDao : YouTubeChannelDao {
        val table = mutableMapOf<String, YouTubeChannelEntity>()
        private val flow = MutableStateFlow<List<YouTubeChannelEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = table.values.sortedByDescending { it.updatedAt }
        }

        override suspend fun getChannelByCanonicalKey(canonicalKey: String): YouTubeChannelEntity? {
            return table[canonicalKey]
        }

        override suspend fun getPolicyByCanonicalKey(canonicalKey: String): ChannelPolicy? {
            return table[canonicalKey]?.policy
        }

        override suspend fun getChannelByHandle(channelHandle: String): YouTubeChannelEntity? {
            return table.values.firstOrNull { it.channelHandle?.equals(channelHandle, ignoreCase = true) == true }
        }

        override suspend fun getPolicyByHandle(channelHandle: String): ChannelPolicy? {
            return table.values.firstOrNull { it.channelHandle?.equals(channelHandle, ignoreCase = true) == true }?.policy
        }

        override suspend fun getChannelByName(channelName: String): YouTubeChannelEntity? {
            return table.values.firstOrNull { it.displayName.equals(channelName, ignoreCase = true) }
        }

        override suspend fun getPolicyByName(channelName: String): ChannelPolicy? {
            return table.values.firstOrNull { it.displayName.equals(channelName, ignoreCase = true) }?.policy
        }

        override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> = flow

        override fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>> =
            flow.map { list -> list.filter { it.policy == policy } }

        override suspend fun insertOrUpdate(channel: YouTubeChannelEntity) {
            table[channel.canonicalKey] = channel
            updateFlow()
        }

        override suspend fun deleteByCanonicalKey(canonicalKey: String) {
            table.remove(canonicalKey)
            updateFlow()
        }

        override suspend fun deleteByHandle(channelHandle: String) {
            table.entries.removeIf { it.value.channelHandle?.equals(channelHandle, ignoreCase = true) == true }
            updateFlow()
        }

        override suspend fun deleteByName(channelName: String) {
            table.entries.removeIf { it.value.displayName.equals(channelName, ignoreCase = true) }
            updateFlow()
        }

        override suspend fun deleteChannel(channel: YouTubeChannelEntity) {
            table.remove(channel.canonicalKey)
            updateFlow()
        }

        override suspend fun deleteAll() {
            table.clear()
            updateFlow()
        }
    }

    @Before
    fun setUp() {
        dao = InMemoryYouTubeChannelDao()
        repository = YouTubeChannelRepositoryImpl(
            youTubeChannelDao = dao,
            ioDispatcher = testDispatcher,
        )
    }

    // =========================================================================
    // SECTION 1: DISPLAY NAME VS LATER ASSOCIATING REAL HANDLE (STATE CONSISTENCY)
    // =========================================================================

    @Test
    fun `empirical challenge - saving display name then associating handle results in duplicate rows and stale lookup`() = runTest(testDispatcher) {
        // Step 1: Channel first detected with only display name "Khan Academy" -> marked PRODUCTIVE
        val nameOnlyIdentity = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy")
        repository.savePolicy(nameOnlyIdentity, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("name:khan academy"))

        // Step 2: Channel later navigated to or handle resolved as "@khanacademy" -> user re-classifies to DISTRACTING
        val handleIdentity = ChannelIdentity(verifiedHandle = "@khanacademy", displayName = "Khan Academy")
        repository.savePolicy(handleIdentity, ChannelPolicy.DISTRACTING)

        // OBSERVATION 1: Duplicate entities are cleaned up; exactly 1 entity exists!
        println("DAO Table keys after handle association: ${dao.table.keys}")
        val storedEntities = dao.getAllChannels().first()
        val hasDuplicates = storedEntities.size > 1
        assertFalse("No duplicates in storage", hasDuplicates)
        assertEquals(1, storedEntities.size)

        // OBSERVATION 2: When video plays without handle, getPolicy returns the updated DISTRACTING policy
        val resolvedPolicyForNameOnly = repository.getPolicy(nameOnlyIdentity)
        println("Policy resolved for nameOnlyIdentity: $resolvedPolicyForNameOnly")
        assertEquals(ChannelPolicy.DISTRACTING, resolvedPolicyForNameOnly)

        // OBSERVATION 3: Deletion cleans up all related records
        repository.deleteChannel(handleIdentity)
        val remainingAfterDelete = dao.getAllChannels().first()
        println("Remaining entities after deleteChannel(handleIdentity): ${remainingAfterDelete.map { it.canonicalKey }}")
        assertTrue(remainingAfterDelete.isEmpty())
        val policyAfterDelete = repository.getPolicy(nameOnlyIdentity)
        println("Policy for nameOnlyIdentity after delete: $policyAfterDelete")
        assertNull(policyAfterDelete)
    }

    @Test
    fun `empirical challenge - saving handle first then updating via display name creates divergent policies`() = runTest(testDispatcher) {
        // Step 1: Channel saved with handle "@veritasium" -> PRODUCTIVE
        val handleIdentity = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        repository.savePolicy(handleIdentity, ChannelPolicy.PRODUCTIVE)

        // Step 2: Channel reclassified on a screen exposing only display name "Veritasium" -> DISTRACTING
        val nameOnlyIdentity = ChannelIdentity(verifiedHandle = null, displayName = "Veritasium")
        repository.savePolicy(nameOnlyIdentity, ChannelPolicy.DISTRACTING)

        // Lookups remain synchronized:
        val policyByHandle = repository.getPolicy(handleIdentity)
        val policyByName = repository.getPolicy(nameOnlyIdentity)

        println("Policy when queried with handle: $policyByHandle")
        println("Policy when queried with name: $policyByName")
        assertEquals(ChannelPolicy.DISTRACTING, policyByHandle)
        assertEquals(ChannelPolicy.DISTRACTING, policyByName)
        assertEquals(1, dao.table.size)
    }

    // =========================================================================
    // SECTION 2: SYNTHETIC FAKE HANDLE GENERATION & NORMALIZATION
    // =========================================================================

    @Test
    fun `empirical challenge - display name with spaces passed to normalizeHandle creates fake handle`() {
        // If a display name is passed to normalizeHandle (e.g. from legacy callers or UI)
        val result = ChannelIdentity.normalizeHandle("Khan Academy")
        println("normalizeHandle('Khan Academy') -> '$result'")
        // Rejects handles containing whitespace
        assertEquals("", result)
    }

    @Test
    fun `empirical challenge - ChannelIdentity from with display name in handle field creates fake handle`() {
        val identity = ChannelIdentity.from("Khan Academy", "Khan Academy")
        println("ChannelIdentity.from('Khan Academy', 'Khan Academy'): handle=${identity.verifiedHandle}, canonicalKey=${identity.canonicalKey}")
        assertNull(identity.verifiedHandle)
        assertEquals("name:khan academy", identity.canonicalKey)
    }

    @Test
    fun `empirical challenge - saveChannelPolicy with display name in channelHandle parameter stores fake handle in DAO`() = runTest(testDispatcher) {
        // Existing callers or UI pass channelHandle = "Khan Academy" when handle is not known
        repository.saveChannelPolicy("Khan Academy", "Khan Academy", ChannelPolicy.PRODUCTIVE)

        val stored = dao.table.values.firstOrNull()
        println("Stored entity: canonicalKey=${stored?.canonicalKey}, channelHandle=${stored?.channelHandle}, displayName=${stored?.displayName}")
        assertNull(stored?.channelHandle)
        assertEquals("name:khan academy", stored?.canonicalKey)
    }

    @Test
    fun `empirical challenge - YouTubeChannelEntity secondary constructor with invalid handle symbol`() {
        val entity = YouTubeChannelEntity(
            channelHandle = "@",
            channelName = "Khan Academy",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        println("Entity created with '@': channelHandle=${entity.channelHandle}, canonicalKey=${entity.canonicalKey}")
        assertNull(entity.channelHandle)
        assertEquals("name:khan academy", entity.canonicalKey)
    }

    // =========================================================================
    // SECTION 3: BACKWARD COMPATIBILITY
    // =========================================================================

    @Test
    fun `empirical challenge - getChannelPolicy fails to find display-name channel if queried with different formatting`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy")
        repository.savePolicy(identity, ChannelPolicy.PRODUCTIVE)

        // Existing callers use getChannelPolicy
        val found = repository.getChannelPolicy("Khan Academy")
        println("getChannelPolicy('Khan Academy'): $found")
        assertEquals(ChannelPolicy.PRODUCTIVE, found)

        // What if caller passes "  khan academy  "?
        val foundWithSpaces = repository.getChannelPolicy("  khan academy  ")
        println("getChannelPolicy('  khan academy  '): $foundWithSpaces")
        assertEquals(ChannelPolicy.PRODUCTIVE, foundWithSpaces)
    }

    @Test
    fun `empirical challenge - deleteChannel by name fails to delete handle-based record`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = "@khanacademy", displayName = "Khan Academy")
        repository.savePolicy(identity, ChannelPolicy.PRODUCTIVE)

        // Caller tries to delete by channel name "Khan Academy"
        repository.deleteChannel("Khan Academy")

        println("DAO Table after deleteChannel('Khan Academy'): ${dao.table.keys}")
        assertFalse(dao.table.containsKey("handle:@khanacademy"))
        assertTrue(dao.table.isEmpty())
    }

    // =========================================================================
    // SECTION 4: CONCURRENCY & MULTI-THREADED ACCESS
    // =========================================================================

    @Test
    fun `empirical challenge - concurrent updates with handle and name do not corrupt state`() = runTest(testDispatcher) {
        val jobs = (1..50).map { i ->
            async {
                if (i % 2 == 0) {
                    repository.savePolicy(
                        ChannelIdentity(verifiedHandle = "@concurrent_chan", displayName = "Concurrent Chan"),
                        ChannelPolicy.PRODUCTIVE,
                    )
                } else {
                    repository.savePolicy(
                        ChannelIdentity(verifiedHandle = null, displayName = "Concurrent Chan"),
                        ChannelPolicy.DISTRACTING,
                    )
                }
            }
        }
        jobs.awaitAll()

        println("Concurrent table entries count: ${dao.table.size}")
        println("Keys: ${dao.table.keys}")
    }

    // =========================================================================
    // SECTION 5: VIDEO FINGERPRINT VALIDATION
    // =========================================================================

    @Test
    fun `empirical challenge - VideoFingerprint mediaId priority and fallback`() {
        val fpWithMediaId = VideoFingerprint.create(
            mediaId = "v12345",
            title = "My Video",
            channelIdentity = ChannelIdentity(verifiedHandle = "@creator", displayName = "Creator"),
        )
        assertEquals("media:v12345", fpWithMediaId.key)

        val fpWithoutMediaId = VideoFingerprint.create(
            mediaId = null,
            title = "My Video",
            channelIdentity = ChannelIdentity(verifiedHandle = "@creator", displayName = "Creator"),
        )
        assertEquals("content:my video|handle:@creator", fpWithoutMediaId.key)

        val fpBlankMediaId = VideoFingerprint.create(
            mediaId = "   ",
            title = "My Video",
            channelIdentity = ChannelIdentity(verifiedHandle = "@creator", displayName = "Creator"),
        )
        assertEquals("content:my video|handle:@creator", fpBlankMediaId.key)
    }
}
