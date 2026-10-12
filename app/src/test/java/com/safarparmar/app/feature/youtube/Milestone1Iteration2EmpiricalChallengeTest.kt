package com.safarparmar.app.feature.youtube

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
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
 * Empirical Challenger Suite (Iteration 2) for Milestone 1.
 * Adversarially tests:
 * 1. Handle association reconciliation (bidirectional, deduplication, whitespace in names, multiple channels).
 * 2. Handle normalization & whitespace rejection (fullwidth at, bare at, internal spaces, invalid symbols, unicode).
 * 3. Deletion completeness (zero zombies, string vs identity overloads, canonical keys).
 * 4. VideoFingerprint edge cases (null/undefined string representations, whitespace, substrings, fallback).
 */
class Milestone1Iteration2EmpiricalChallengeTest {

    private lateinit var dao: StressTestDao
    private lateinit var repository: YouTubeChannelRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    class StressTestDao : YouTubeChannelDao {
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
        dao = StressTestDao()
        repository = YouTubeChannelRepositoryImpl(
            youTubeChannelDao = dao,
            ioDispatcher = testDispatcher,
        )
    }

    // =========================================================================
    // 1. HANDLE ASSOCIATION RECONCILIATION STRESS TESTS
    // =========================================================================

    @Test
    fun `reconciliation - display name with dirty whitespace is cleanly purged upon handle association`() = runTest(testDispatcher) {
        // Initially saved with erratic whitespace in display name
        val nameOnly = ChannelIdentity(verifiedHandle = null, displayName = "  Khan \t  Academy  ")
        repository.savePolicy(nameOnly, ChannelPolicy.DISTRACTING)

        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("name:khan academy"))
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(nameOnly))

        // Subsequently associated with verified handle and clean display name
        val handleId = ChannelIdentity(verifiedHandle = "@khanacademy", displayName = "Khan Academy")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        // Storage MUST have exactly 1 entity, obsolete name-only row MUST be purged
        assertEquals(1, dao.table.size)
        assertFalse(dao.table.containsKey("name:khan academy"))
        assertTrue(dao.table.containsKey("handle:@khanacademy"))

        // Lookups with exact, lower, and upper case, and dirty whitespace MUST return the reconciled PRODUCTIVE policy
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(handleId))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(nameOnly))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ChannelIdentity(null, "khan academy")))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ChannelIdentity(null, "KHAN ACADEMY")))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ChannelIdentity(null, "  Khan   Academy  ")))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(ChannelIdentity(null, "\tKhan\tAcademy\n")))
    }

    @Test
    fun `reconciliation - handle saved first then updated via display-name updates handle record without creating duplicate`() = runTest(testDispatcher) {
        val handleId = ChannelIdentity(verifiedHandle = "@3blue1brown", displayName = "3Blue1Brown")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@3blue1brown"))

        // User updates policy on a watch screen where only display name is available
        val nameId = ChannelIdentity(verifiedHandle = null, displayName = "3Blue1Brown")
        repository.savePolicy(nameId, ChannelPolicy.DISTRACTING)

        // Storage MUST still have exactly 1 record, updated in-place on the handle entity
        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@3blue1brown"))
        assertEquals(ChannelPolicy.DISTRACTING, dao.table["handle:@3blue1brown"]?.policy)

        // Both lookups resolve to DISTRACTING
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(handleId))
        assertEquals(ChannelPolicy.DISTRACTING, repository.getPolicy(nameId))
    }

    @Test
    fun `reconciliation - saving multiple distinct channels does not cross-pollute or delete unrelated records`() = runTest(testDispatcher) {
        val ch1 = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        val ch2 = ChannelIdentity(verifiedHandle = null, displayName = "Veritasium Clips")
        val ch3 = ChannelIdentity(verifiedHandle = "@minutephysics", displayName = "MinutePhysics")

        repository.savePolicy(ch1, ChannelPolicy.PRODUCTIVE)
        repository.savePolicy(ch2, ChannelPolicy.DISTRACTING)
        repository.savePolicy(ch3, ChannelPolicy.PRODUCTIVE)

        assertEquals(3, dao.table.size)

        // Associate handle for ch2
        val ch2WithHandle = ChannelIdentity(verifiedHandle = "@veritasiumclips", displayName = "Veritasium Clips")
        repository.savePolicy(ch2WithHandle, ChannelPolicy.DISTRACTING)

        assertEquals(3, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@veritasium"))
        assertTrue(dao.table.containsKey("handle:@veritasiumclips"))
        assertTrue(dao.table.containsKey("handle:@minutephysics"))
        assertFalse(dao.table.containsKey("name:veritasium clips"))
    }

    @Test
    fun `reconciliation - empty display name when saving handle does not throw or purge wrong records`() = runTest(testDispatcher) {
        val handleOnly = ChannelIdentity(verifiedHandle = "@solitary", displayName = "")
        repository.savePolicy(handleOnly, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)
        assertTrue(dao.table.containsKey("handle:@solitary"))
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(handleOnly))
    }

    // =========================================================================
    // 2. HANDLE NORMALIZATION & WHITESPACE REJECTION STRESS TESTS
    // =========================================================================

    @Test
    fun `handle normalization - all whitespace variants are strictly rejected`() {
        val whitespaceHandles = listOf(
            "Khan Academy",
            "@Khan Academy",
            "  @Khan Academy  ",
            "@Khan\tAcademy",
            "@Khan\nAcademy",
            "@Khan\r\nAcademy",
            " @ ",
            " \t ",
            "   ",
        )
        for (raw in whitespaceHandles) {
            val normalized = ChannelIdentity.normalizeHandle(raw)
            assertEquals("Must reject whitespace in '$raw'", "", normalized)
        }
    }

    @Test
    fun `handle normalization - bare at and multiple at symbols are rejected or normalized safely`() {
        assertEquals("", ChannelIdentity.normalizeHandle("@"))
        assertEquals("", ChannelIdentity.normalizeHandle("@@"))
        assertEquals("", ChannelIdentity.normalizeHandle("@@@"))
        assertEquals("", ChannelIdentity.normalizeHandle("   @   "))
        assertEquals("", ChannelIdentity.normalizeHandle("＠")) // Fullwidth
        assertEquals("", ChannelIdentity.normalizeHandle("＠＠"))
        assertEquals("", ChannelIdentity.normalizeHandle("@＠"))

        // Multiple leading @ before a valid handle is safely stripped down to single @
        assertEquals("@veritasium", ChannelIdentity.normalizeHandle("@@veritasium"))
        assertEquals("@veritasium", ChannelIdentity.normalizeHandle("＠veritasium"))
        assertEquals("@veritasium", ChannelIdentity.normalizeHandle("＠＠veritasium"))
        assertEquals("@veritasium", ChannelIdentity.normalizeHandle("@＠veritasium"))
    }

    @Test
    fun `handle normalization - invalid symbols and punctuation are rejected`() {
        val invalidHandles = listOf(
            "@user!name",
            "@user#123",
            "@user\$money",
            "@user%percent",
            "@user^power",
            "@user&and",
            "@user*star",
            "@user(parens)",
            "@user+plus",
            "@user=equal",
            "@user<lt>",
            "@user>gt",
            "@user,comma",
            "@user/slash",
            "@user?query",
            "@user:colon",
            "@user;semi",
            "@user'quote",
            "@user\"double",
            "@user[bracket]",
            "@user]bracket",
            "@user{brace}",
            "@user}brace",
            "@user|pipe",
            "@user\\backslash",
            "@user~tilde",
            "@user`backtick",
            "@channel🚀",
            "@channel👍",
        )
        for (raw in invalidHandles) {
            val normalized = ChannelIdentity.normalizeHandle(raw)
            assertEquals("Must reject invalid characters in '$raw'", "", normalized)
        }
    }

    @Test
    fun `handle normalization - valid YouTube handle characters are accepted and lowercased`() {
        val validCases = mapOf(
            "@Veritasium" to "@veritasium",
            "veritasium" to "@veritasium",
            "@My.Channel_123-Official" to "@my.channel_123-official",
            "user.name-123_abc" to "@user.name-123_abc",
            "@a" to "@a",
            "@123" to "@123",
        )
        for ((raw, expected) in validCases) {
            assertEquals(expected, ChannelIdentity.normalizeHandle(raw))
        }
    }

    @Test
    fun `handle identity - hasHandle is false for any invalid or bare at handle`() {
        assertFalse(ChannelIdentity(verifiedHandle = "@", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "@@", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "  @  ", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "＠", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "Khan Academy", displayName = "Khan Academy").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "@invalid#char", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = "", displayName = "Test").hasHandle)
        assertFalse(ChannelIdentity(verifiedHandle = null, displayName = "Test").hasHandle)

        assertTrue(ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Test").hasHandle)
        assertTrue(ChannelIdentity(verifiedHandle = "veritasium", displayName = "Test").hasHandle)
    }

    @Test
    fun `entity constructor - secondary constructor safely normalizes handle and drops invalid ones`() {
        val entityWithBareAt = YouTubeChannelEntity(
            channelHandle = "@",
            channelName = "Khan Academy",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        assertNull(entityWithBareAt.channelHandle)
        assertEquals("name:khan academy", entityWithBareAt.canonicalKey)

        val entityWithSpaces = YouTubeChannelEntity(
            channelHandle = "  Khan Academy  ",
            channelName = "Khan Academy",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        assertNull(entityWithSpaces.channelHandle)
        assertEquals("name:khan academy", entityWithSpaces.canonicalKey)

        val entityWithValidHandle = YouTubeChannelEntity(
            channelHandle = "Veritasium",
            channelName = "Veritasium",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        assertEquals("@veritasium", entityWithValidHandle.channelHandle)
        assertEquals("handle:@veritasium", entityWithValidHandle.canonicalKey)
    }

    // =========================================================================
    // 3. DELETION COMPLETENESS STRESS TESTS
    // =========================================================================

    @Test
    fun `deletion - deleteChannel with identity leaves zero zombie records across both handle and name lookups`() = runTest(testDispatcher) {
        val nameId = ChannelIdentity(null, "Veritasium")
        val handleId = ChannelIdentity("@veritasium", "Veritasium")

        repository.savePolicy(nameId, ChannelPolicy.PRODUCTIVE)
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)

        // Delete using handle identity
        repository.deleteChannel(handleId)

        assertEquals("Table must be completely empty after delete", 0, dao.table.size)
        assertNull(repository.getPolicy(handleId))
        assertNull(repository.getPolicy(nameId))
        assertNull(repository.getChannelPolicy("Veritasium"))
        assertNull(repository.getChannelPolicy("@veritasium"))
    }

    @Test
    fun `deletion - deleteChannel with channel name string purges handle-based record`() = runTest(testDispatcher) {
        val handleId = ChannelIdentity("@kurzgesagt", "Kurzgesagt – In a Nutshell")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)

        // Deleting by display name string
        repository.deleteChannel("Kurzgesagt – In a Nutshell")

        assertEquals(0, dao.table.size)
        assertNull(repository.getPolicy(handleId))
        assertNull(repository.getChannelPolicy("@kurzgesagt"))
    }

    @Test
    fun `deletion - deleteChannel with canonical key string purges entity directly`() = runTest(testDispatcher) {
        val handleId = ChannelIdentity("@numberphile", "Numberphile")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)

        // Deleting directly by canonical key
        repository.deleteChannel("handle:@numberphile")

        assertEquals(0, dao.table.size)
        assertNull(repository.getPolicy(handleId))
    }

    @Test
    fun `deletion - deleteChannel on non-existent channel does not fail or alter other records`() = runTest(testDispatcher) {
        val realId = ChannelIdentity("@realchannel", "Real Channel")
        repository.savePolicy(realId, ChannelPolicy.PRODUCTIVE)

        repository.deleteChannel("Non Existent Channel")
        repository.deleteChannel("@nonexistent")
        repository.deleteChannel(ChannelIdentity("@nonexistent", "Non Existent"))

        assertEquals(1, dao.table.size)
        assertEquals(ChannelPolicy.PRODUCTIVE, repository.getPolicy(realId))
    }

    @Test
    fun `deletion - deleteChannel with dirty whitespace in display name purges entity cleanly`() = runTest(testDispatcher) {
        val handleId = ChannelIdentity("@veritasium", "  Veritasium  ")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        assertEquals(1, dao.table.size)

        // Delete using identity with erratic tabs/whitespace in display name
        repository.deleteChannel(ChannelIdentity(null, " \t Veritasium \n "))

        assertEquals("Entity must be deleted despite erratic whitespace in delete argument", 0, dao.table.size)
        assertNull(repository.getPolicy(handleId))
    }

    @Test
    fun `retrieval - getChannel and getChannelByIdentity match despite whitespace variations`() = runTest(testDispatcher) {
        val handleId = ChannelIdentity("@kurzgesagt", "Kurzgesagt – In a Nutshell")
        repository.savePolicy(handleId, ChannelPolicy.PRODUCTIVE)

        val retrievedByHandle = repository.getChannel("@kurzgesagt")
        assertNotNull(retrievedByHandle)
        assertEquals("Kurzgesagt – In a Nutshell", retrievedByHandle?.displayName)

        val retrievedByDirtyIdentity = repository.getChannelByIdentity(
            ChannelIdentity(null, "  Kurzgesagt \t  –   In   a   Nutshell  ")
        )
        assertNotNull("Must resolve channel by identity even with uncollapsed whitespace", retrievedByDirtyIdentity)
        assertEquals("handle:@kurzgesagt", retrievedByDirtyIdentity?.canonicalKey)
    }

    // =========================================================================
    // 4. VIDEO FINGERPRINT VALIDATION STRESS TESTS
    // =========================================================================

    @Test
    fun `fingerprint - string null and undefined in all case variations fall back to content key`() {
        val nullVariants = listOf("null", "Null", "NULL", "  null  ", "\tnull\n")
        val undefinedVariants = listOf("undefined", "Undefined", "UNDEFINED", "  undefined  ", "\tundefined\n")
        val blankVariants = listOf("", "   ", "\t\n", null)

        val dummyIdentity = ChannelIdentity(verifiedHandle = "@science", displayName = "Science")

        for (variant in nullVariants + undefinedVariants + blankVariants) {
            val fpFromConstructor = VideoFingerprint(
                mediaId = variant,
                normalizedTitle = "quantum computing",
                channelCanonicalKey = dummyIdentity.canonicalKey,
            )
            assertEquals(
                "Variant '$variant' must fall back to content key",
                "content:quantum computing|handle:@science",
                fpFromConstructor.key,
            )

            val fpFromFactory = VideoFingerprint.create(
                mediaId = variant,
                title = "Quantum Computing",
                channelIdentity = dummyIdentity,
            )
            assertNull("Factory must clean variant '$variant' to null", fpFromFactory.mediaId)
            assertEquals(
                "Factory with '$variant' must fall back to content key",
                "content:quantum computing|handle:@science",
                fpFromFactory.key,
            )
            assertTrue(fpFromFactory.isValid)
        }
    }

    @Test
    fun `fingerprint - mediaId containing null as substring is preserved as genuine mediaId`() {
        val legitimateIds = listOf("null_video_123", "video_null", "annullment_doc", "not_undefined")
        for (id in legitimateIds) {
            val fp = VideoFingerprint.create(
                mediaId = id,
                title = "Title",
                channelIdentity = ChannelIdentity(verifiedHandle = "@creator", displayName = "Creator"),
            )
            assertEquals("Genuine mediaId '$id' must be preserved", "media:$id", fp.key)
            assertEquals(id, fp.mediaId)
        }
    }
}
