package com.safarparmar.app.feature.youtube

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Milestone 1 Challenger (Iteration 3) Empirical Adversarial Challenge Suite.
 *
 * Adversarially challenges:
 * 1. Dirty whitespace handling (tabs, multiple spaces, mixed whitespace, newlines, carriage returns)
 *    across all display name lookups, updates, handle associations, and deletions.
 * 2. Strict whitespace equality vs case-insensitive matching semantics (matching SQLite COLLATE NOCASE behavior).
 * 3. Lifecycle state transitions: Name-only -> Handle association -> Policy update via Name -> Deletion via Name.
 * 4. Disambiguation across similar channel names with dirty whitespace.
 * 5. Boundary conditions: All-whitespace strings, bare @-handles, empty names.
 */
class Milestone1Iteration3EmpiricalChallengeTest {

    private lateinit var dao: EmpiricalChallengeDao
    private lateinit var repository: YouTubeChannelRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    class EmpiricalChallengeDao : YouTubeChannelDao {
        val table = mutableMapOf<String, YouTubeChannelEntity>()
        private val flow = MutableStateFlow<List<YouTubeChannelEntity>>(emptyList())

        private fun syncFlow() {
            flow.value = table.values.sortedByDescending { it.updatedAt }
        }

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

        override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> = flow

        override fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>> =
            flow.map { list -> list.filter { it.policy == policy } }

        override suspend fun insertOrUpdate(channel: YouTubeChannelEntity) {
            table[channel.canonicalKey] = channel
            syncFlow()
        }

        override suspend fun deleteByCanonicalKey(canonicalKey: String) {
            table.remove(canonicalKey)
            syncFlow()
        }

        override suspend fun deleteByHandle(channelHandle: String) {
            table.entries.removeIf { it.value.channelHandle?.equals(channelHandle, ignoreCase = true) == true }
            syncFlow()
        }

        override suspend fun deleteByName(channelName: String) {
            table.entries.removeIf { it.value.displayName.equals(channelName, ignoreCase = true) }
            syncFlow()
        }

        override suspend fun deleteChannel(channel: YouTubeChannelEntity) {
            table.remove(channel.canonicalKey)
            syncFlow()
        }

        override suspend fun deleteAll() {
            table.clear()
            syncFlow()
        }
    }

    @Before
    fun setUp() {
        dao = EmpiricalChallengeDao()
        repository = YouTubeChannelRepositoryImpl(
            youTubeChannelDao = dao,
            ioDispatcher = testDispatcher,
        )
    }

    // =========================================================================
    // 1. DIRTY WHITESPACE HANDLING IN DISPLAY NAME STORAGE & CANONICAL KEY
    // =========================================================================

    @Test
    fun `challenge - display name with mixed whitespace stores clean display name and canonical key`() = runTest(testDispatcher) {
        val dirtyNames = listOf(
            "  Khan \t  Academy  ",
            "\t\n  Khan   \t   Academy   \r\n",
            "Khan\tAcademy",
            "  Khan \r\n Academy  ",
            "   Khan     Academy   ",
        )

        for (dirtyName in dirtyNames) {
            dao.deleteAll()
            val identity = ChannelIdentity(verifiedHandle = null, displayName = dirtyName)
            assertEquals("name:khan academy", identity.canonicalKey)

            repository.savePolicy(identity, ChannelPolicy.DISTRACTING)

            assertEquals(1, dao.table.size)
            val stored = dao.table["name:khan academy"]
            assertNotNull("Stored entity must exist under canonical key 'name:khan academy'", stored)
            assertEquals("Khan Academy", stored?.displayName)
            assertNull("Must not create fake handle", stored?.channelHandle)
            assertEquals(ChannelPolicy.DISTRACTING, stored?.policy)

            // Lookup via getPolicy with various dirty versions
            assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(identity))
            assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ChannelIdentity(null, "Khan Academy")))
            assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ChannelIdentity(null, "khan academy")))
            assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ChannelIdentity(null, "\nKhan\tAcademy\n")))
        }
    }

    // =========================================================================
    // 2. COMPLETE LIFECYCLE: NAME-ONLY -> HANDLE ASSOCIATION -> UPDATE -> DELETE
    // =========================================================================

    @Test
    fun `challenge - full lifecycle with extreme dirty whitespace across all operations`() = runTest(testDispatcher) {
        // Step 1: Initial creation with dirty display name only
        val initialDirtyName = " \t \r\n  Kurzgesagt \t – \n In   a   Nutshell \r "
        val initialIdentity = ChannelIdentity(verifiedHandle = null, displayName = initialDirtyName)
        repository.savePolicy(initialIdentity, ChannelPolicy.DISTRACTING)

        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("name:kurzgesagt – in a nutshell"))
        val initialStored = dao.table["name:kurzgesagt – in a nutshell"]
        assertEquals("Kurzgesagt – In a Nutshell", initialStored?.displayName)
        assertEquals(ChannelPolicy.DISTRACTING, initialStored?.policy)

        // Step 2: Handle association with another variant of dirty whitespace
        val handleAssociation = ChannelIdentity(
            verifiedHandle = "  @Kurzgesagt  ",
            displayName = "  Kurzgesagt    –    In    a    Nutshell  ",
        )
        repository.savePolicy(handleAssociation, ChannelPolicy.PRODUCTIVE)

        // Storage MUST have exactly 1 record, old name-only key MUST be purged
        assertEquals("Storage must contain exactly 1 entity after handle association", 1, dao.table.size)
        assertFalse(
            "Obsolete name-only canonical key must be purged",
            dao.table.containsKey("name:kurzgesagt – in a nutshell"),
        )
        assertTrue(
            "Handle canonical key must exist",
            dao.table.containsKey("handle:@kurzgesagt"),
        )
        val handleStored = dao.table["handle:@kurzgesagt"]
        assertEquals("@kurzgesagt", handleStored?.channelHandle)
        assertEquals("Kurzgesagt – In a Nutshell", handleStored?.displayName)
        assertEquals(ChannelPolicy.PRODUCTIVE, handleStored?.policy)

        // Step 3: Adversarially query through all retrieval methods using extreme dirty whitespace
        val dirtyQueries = listOf(
            "Kurzgesagt – In a Nutshell",
            "kurzgesagt – in a nutshell",
            "  Kurzgesagt \t – \t In   a   Nutshell  ",
            "\n\tKurzgesagt\t–\tIn\ta\tNutshell\n\r",
            "  @kurzgesagt  ",
            "@Kurzgesagt",
        )

        for (q in dirtyQueries) {
            val queryIdentity = if (q.contains("@")) ChannelIdentity(q, "") else ChannelIdentity(null, q)
            assertEquals("getPolicy must match query '$q'", ChannelPolicy.PRODUCTIVE, repository.getPolicy(queryIdentity))
            assertEquals("getChannelPolicy must match query '$q'", ChannelPolicy.PRODUCTIVE, repository.getChannelPolicy(q))
            val ch = repository.getChannel(q)
            assertNotNull("getChannel must find entity for query '$q'", ch)
            assertEquals("handle:@kurzgesagt", ch?.canonicalKey)
        }

        // Step 4: Reclassify on a screen with only dirty display name
        val updateIdentity = ChannelIdentity(null, " \t Kurzgesagt \n – \t In a Nutshell ")
        repository.savePolicy(updateIdentity, ChannelPolicy.DISTRACTING)

        // Must still have exactly 1 entity, updated on the handle row
        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@kurzgesagt"))
        assertFalse(dao.table.containsKey("name:kurzgesagt – in a nutshell"))
        assertEquals(ChannelPolicy.DISTRACTING, dao.table["handle:@kurzgesagt"]?.policy)

        // Step 5: Deletion using dirty display name
        repository.deleteChannel(ChannelIdentity(null, "  \r\n Kurzgesagt \t – \n In   a   Nutshell \t "))

        // Table must be completely purged with zero zombies
        assertEquals("Table must be completely empty after deleteChannel", 0, dao.table.size)
        for (q in dirtyQueries) {
            val queryIdentity = if (q.contains("@")) ChannelIdentity(q, "") else ChannelIdentity(null, q)
            assertNull("getPolicy must be null after deletion for '$q'", repository.getPolicy(queryIdentity))
            assertNull("getChannelPolicy must be null after deletion for '$q'", repository.getChannelPolicy(q))
            assertNull("getChannel must be null after deletion for '$q'", repository.getChannel(q))
        }
    }

    // =========================================================================
    // 3. STRING OVERLOAD OF deleteChannel WITH DIRTY WHITESPACE
    // =========================================================================

    @Test
    fun `challenge - string overload deleteChannel with dirty display name purges handle entity`() = runTest(testDispatcher) {
        val handleIdentity = ChannelIdentity("@veritasium", "Veritasium")
        repository.savePolicy(handleIdentity, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)

        // Delete using raw string with erratic tabs, spaces, newlines
        repository.deleteChannel("  \t\n  Veritasium  \r\n  ")

        assertEquals("Entity must be deleted by string name with dirty whitespace", 0, dao.table.size)
        assertNull(repository.getPolicy(handleIdentity))
        assertNull(repository.getChannelPolicy("@veritasium"))
        assertNull(repository.getChannelPolicy("Veritasium"))
    }

    // =========================================================================
    // 4. MULTI-CHANNEL DISAMBIGUATION WITH DIRTY WHITESPACE
    // =========================================================================

    @Test
    fun `challenge - multiple channels with overlapping name prefixes do not falsely collide`() = runTest(testDispatcher) {
        val ch1 = ChannelIdentity("@minutephysics", " \t MinutePhysics \n ")
        val ch2 = ChannelIdentity("@minuteearth", "   MinuteEarth   ")
        val ch3 = ChannelIdentity(null, " \t MinutePhysics \t Extras \n ")

        repository.savePolicy(ch1, ChannelPolicy.PRODUCTIVE)
        repository.savePolicy(ch2, ChannelPolicy.DISTRACTING)
        repository.savePolicy(ch3, ChannelPolicy.DISTRACTING)

        assertEquals(3, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@minutephysics"))
        assertTrue(dao.table.containsKey("handle:@minuteearth"))
        assertTrue(dao.table.containsKey("name:minutephysics extras"))

        // Lookup each with varying whitespace
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ChannelIdentity(null, "MinutePhysics")))
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ChannelIdentity(null, "MinuteEarth")))
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ChannelIdentity(null, "  MinutePhysics   Extras  ")))

        // Associate handle for ch3
        val ch3WithHandle = ChannelIdentity("@minutephysicsextras", "MinutePhysics Extras")
        repository.savePolicy(ch3WithHandle, ChannelPolicy.PRODUCTIVE)

        assertEquals(3, dao.table.size)
        assertFalse(dao.table.containsKey("name:minutephysics extras"))
        assertTrue(dao.table.containsKey("handle:@minutephysicsextras"))

        // Delete ch1 via dirty display name
        repository.deleteChannel("  \t MinutePhysics \r\n ")

        assertEquals(2, dao.table.size)
        assertNull(repository.getPolicy(ch1))
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(ch2))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ch3WithHandle))
    }

    // =========================================================================
    // 5. saveChannelPolicy WITH DIRTY WHITESPACE AND LEGACY PARAMETER SWAPPING
    // =========================================================================

    @Test
    fun `challenge - saveChannelPolicy with display name in handle parameter normalizes whitespace safely`() = runTest(testDispatcher) {
        // When legacy caller passes display name with dirty whitespace into channelHandle:
        repository.saveChannelPolicy(
            channelHandle = "  Khan \t  Academy  ",
            channelName = "",
            policy = ChannelPolicy.PRODUCTIVE,
        )

        assertEquals(1, dao.table.size)
        val entity = dao.table["name:khan academy"]
        assertNotNull("Must save under name:khan academy", entity)
        assertNull("Must not create fake handle for display name with whitespace", entity?.channelHandle)
        assertEquals("Khan Academy", entity?.displayName)
    }

    // =========================================================================
    // 6. BLANK AND WHITESPACE-ONLY INPUTS
    // =========================================================================

    @Test
    fun `challenge - all-whitespace strings do not corrupt storage or throw exceptions`() = runTest(testDispatcher) {
        val whitespaceVariants = listOf("", "   ", "\t\t", "\n\r", "  \t \n  ")

        for (ws in whitespaceVariants) {
            val blankIdentity = ChannelIdentity(verifiedHandle = null, displayName = ws)
            repository.savePolicy(blankIdentity, ChannelPolicy.PRODUCTIVE)
            assertEquals("Saving blank identity must be a no-op", 0, dao.table.size)

            assertNull("getPolicy for blank identity must return null", repository.getPolicy(blankIdentity))
            assertNull("getChannelPolicy for blank string must return null", repository.getChannelPolicy(ws))
            assertNull("getChannel for blank string must return null", repository.getChannel(ws))

            // deleteChannel with blank must be clean no-op
            repository.deleteChannel(blankIdentity)
            repository.deleteChannel(ws)
            assertEquals(0, dao.table.size)
        }
    }

    // =========================================================================
    // 7. YOUTUBE CHANNEL ENTITY CONSTRUCTORS & FACTORIES
    // =========================================================================

    @Test
    fun `challenge - entity constructors and factory methods collapse dirty whitespace`() {
        // Secondary constructor
        val entityFromSec = YouTubeChannelEntity(
            channelHandle = "  @3blue1brown  ",
            channelName = " \t 3Blue \t 1Brown \r\n ",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        assertEquals("handle:@3blue1brown", entityFromSec.canonicalKey)
        assertEquals("@3blue1brown", entityFromSec.channelHandle)
        assertEquals("3Blue 1Brown", entityFromSec.displayName)

        // fromIdentity
        val identity = ChannelIdentity(
            verifiedHandle = "@numberphile",
            displayName = "  Numberphile \t Videos  ",
        )
        val entityFromFactory = YouTubeChannelEntity.fromIdentity(identity, ChannelPolicy.PRODUCTIVE)
        assertEquals("handle:@numberphile", entityFromFactory.canonicalKey)
        assertEquals("Numberphile Videos", entityFromFactory.displayName)

        // computeCanonicalKey
        val nameKey = YouTubeChannelEntity.computeCanonicalKey(null, "  \t Khan \n Academy \t ")
        assertEquals("name:khan academy", nameKey)

        val handleKey = YouTubeChannelEntity.computeCanonicalKey("  @khanacademy  ", " \t Khan \n Academy \t ")
        assertEquals("handle:@khanacademy", handleKey)
    }

    // =========================================================================
    // 8. CONCURRENT ACCESS WITH DIRTY WHITESPACE
    // =========================================================================

    @Test
    fun `challenge - concurrent operations with mixed dirty whitespace do not corrupt repository state`() = runTest(testDispatcher) {
        val jobs = (1..40).map { i ->
            async {
                val dirtyName = " \t Channel \t $i \n "
                val identity = if (i % 2 == 0) {
                    ChannelIdentity("@channel_$i", dirtyName)
                } else {
                    ChannelIdentity(null, dirtyName)
                }
                repository.savePolicy(identity, ChannelPolicy.PRODUCTIVE)
                val retrieved = repository.getPolicy(identity)
                assertEquals(ChannelPolicy.PRODUCTIVE, retrieved)
            }
        }
        jobs.awaitAll()

        assertEquals(40, dao.table.size)
        val allChannels = dao.getAllChannels().first()
        assertEquals(40, allChannels.size)
        for (ch in allChannels) {
            assertFalse("Display name must not have consecutive whitespace", ch.displayName.contains("  "))
            assertFalse("Display name must not have tabs", ch.displayName.contains("\t"))
            assertFalse("Display name must not have newlines", ch.displayName.contains("\n"))
        }
    }
}
