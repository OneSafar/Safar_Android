package com.safarparmar.app.data.local

import app.cash.turbine.test
import com.safarparmar.app.data.local.converter.ChannelPolicyConverter
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Empirical and Adversarial Verification Suite for Room Persistence,
 * Type Converters, and Repository Implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubePersistenceAdversarialTest {

    private val converter = ChannelPolicyConverter()
    private lateinit var dao: YouTubeChannelDao
    private lateinit var repository: YouTubeChannelRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = YouTubeChannelRepositoryImpl(
            youTubeChannelDao = dao,
            ioDispatcher = testDispatcher,
        )
    }

    // =========================================================================
    // 1. TYPE CONVERTER VERIFICATION
    // =========================================================================

    @Test
    fun `converter serializes enum to string correctly`() {
        assertEquals("PRODUCTIVE", converter.fromChannelPolicy(ChannelPolicy.PRODUCTIVE))
        assertEquals("DISTRACTING", converter.fromChannelPolicy(ChannelPolicy.DISTRACTING))
        assertNull(converter.fromChannelPolicy(null))
    }

    @Test
    fun `converter deserializes valid strings with trimming`() {
        assertEquals(ChannelPolicy.PRODUCTIVE, converter.toChannelPolicy("PRODUCTIVE"))
        assertEquals(ChannelPolicy.DISTRACTING, converter.toChannelPolicy("DISTRACTING"))
        assertEquals(ChannelPolicy.PRODUCTIVE, converter.toChannelPolicy("  PRODUCTIVE  "))
        assertEquals(ChannelPolicy.DISTRACTING, converter.toChannelPolicy("\tDISTRACTING\n"))
    }

    @Test
    fun `converter returns null for blank or invalid strings`() {
        assertNull(converter.toChannelPolicy(null))
        assertNull(converter.toChannelPolicy(""))
        assertNull(converter.toChannelPolicy("   "))
        assertNull(converter.toChannelPolicy("UNKNOWN_POLICY"))
        assertNull(converter.toChannelPolicy("productive")) // Case sensitive enumValueOf returns null
        assertNull(converter.toChannelPolicy("12345"))
    }

    // =========================================================================
    // 2. HANDLE NORMALIZATION & WHITESPACE TRIMMING
    // =========================================================================

    @Test
    fun `saveChannelPolicy prepends at-sign when handle lacks prefix`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        repository.saveChannelPolicy("veritasium", "Veritasium", ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertEquals("@veritasium", slot.captured.channelHandle)
        assertEquals("Veritasium", slot.captured.channelName)
        assertEquals(ChannelPolicy.PRODUCTIVE, slot.captured.policy)
    }

    @Test
    fun `saveChannelPolicy trims leading and trailing whitespace from handle`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        repository.saveChannelPolicy("  @3blue1brown  ", "3Blue1Brown", ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertEquals("@3blue1brown", slot.captured.channelHandle)
        assertEquals("3Blue1Brown", slot.captured.channelName)
    }

    @Test
    fun `saveChannelPolicy trims whitespace before prepending at-sign`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        repository.saveChannelPolicy("   kurzgesagt   ", "Kurzgesagt", ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertEquals("@kurzgesagt", slot.captured.channelHandle)
    }

    @Test
    fun `saveChannelPolicy saves without fake handle when handle is empty but name is present`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        repository.saveChannelPolicy("", "Khan Academy", ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertNull(slot.captured.channelHandle)
        assertEquals("Khan Academy", slot.captured.displayName)
        assertEquals("name:khan academy", slot.captured.canonicalKey)
        assertEquals(ChannelPolicy.PRODUCTIVE, slot.captured.policy)
    }

    @Test
    fun `saveChannelPolicy ignores save when both handle and name are blank`() = runTest(testDispatcher) {
        repository.saveChannelPolicy("", "", ChannelPolicy.PRODUCTIVE)
        repository.saveChannelPolicy("   ", "   ", ChannelPolicy.DISTRACTING)

        coVerify(exactly = 0) { dao.insertOrUpdate(any()) }
    }

    // =========================================================================
    // 3. ADVERSARIAL: CASING INCONSISTENCY IN PRIMARY KEY PERSISTENCE
    // =========================================================================

    @Test
    fun `verification - repository normalizes handle casing to ensure single primary key and update replacement`() = runTest(testDispatcher) {
        val slots = mutableListOf<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slots)) } returns Unit

        repository.saveChannelPolicy("@Veritasium", "Veritasium", ChannelPolicy.PRODUCTIVE)
        repository.saveChannelPolicy("@veritasium", "Veritasium", ChannelPolicy.DISTRACTING)

        assertEquals(2, slots.size)
        val firstSavedHandle = slots[0].channelHandle
        val secondSavedHandle = slots[1].channelHandle

        assertEquals("@veritasium", firstSavedHandle)
        assertEquals("@veritasium", secondSavedHandle)
        assertEquals(firstSavedHandle, secondSavedHandle)
    }

    @Test
    fun `getChannelPolicy queries both normalized and raw handle with COLLATE NOCASE`() = runTest(testDispatcher) {
        coEvery { dao.getPolicyByHandle("@veritasium") } returns null
        coEvery { dao.getPolicyByHandle("veritasium") } returns null
        coEvery { dao.getChannelByName("veritasium") } returns YouTubeChannelEntity(
            channelHandle = "@veritasium",
            channelName = "veritasium",
            policy = ChannelPolicy.PRODUCTIVE,
        )

        val policy = repository.getChannelPolicy("veritasium")

        assertEquals(ChannelPolicy.PRODUCTIVE, policy)
        coVerify(atLeast = 1) { dao.getPolicyByHandle("@veritasium") }
        coVerify(atLeast = 1) { dao.getChannelByName("veritasium") }
    }

    @Test
    fun `getChannelPolicy returns null for empty or blank input`() = runTest(testDispatcher) {
        assertNull(repository.getChannelPolicy(""))
        assertNull(repository.getChannelPolicy("   "))
        coVerify(exactly = 0) { dao.getPolicyByHandle(any()) }
    }

    // =========================================================================
    // 4. CONCURRENT READS AND WRITES
    // =========================================================================

    @Test
    fun `concurrent reads and writes execute without concurrency exceptions`() = runTest(testDispatcher) {
        val insertedEntities = mutableListOf<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(insertedEntities)) } returns Unit
        coEvery { dao.getPolicyByHandle(any()) } returns ChannelPolicy.PRODUCTIVE

        val writeJobs = (1..50).map { i ->
            async {
                repository.saveChannelPolicy(
                    channelHandle = "@channel_$i",
                    channelName = "Channel $i",
                    policy = if (i % 2 == 0) ChannelPolicy.PRODUCTIVE else ChannelPolicy.DISTRACTING,
                )
            }
        }

        val readJobs = (1..50).map { i ->
            async {
                repository.getChannelPolicy("@channel_$i")
            }
        }

        val results = (writeJobs + readJobs).awaitAll()
        assertEquals(100, results.size)
        assertEquals(50, insertedEntities.size)
    }

    // =========================================================================
    // 5. FLOW EMISSIONS
    // =========================================================================

    @Test
    fun `getAllChannels flow propagates emissions from DAO`() = runTest(testDispatcher) {
        val flowState = MutableStateFlow<List<YouTubeChannelEntity>>(emptyList())
        every { dao.getAllChannels() } returns flowState

        repository.getAllChannels().test {
            assertEquals(emptyList<YouTubeChannelEntity>(), awaitItem())

            val list1 = listOf(
                YouTubeChannelEntity("@c1", "C1", ChannelPolicy.PRODUCTIVE, 100L)
            )
            flowState.value = list1
            assertEquals(list1, awaitItem())

            val list2 = listOf(
                YouTubeChannelEntity("@c1", "C1", ChannelPolicy.PRODUCTIVE, 100L),
                YouTubeChannelEntity("@c2", "C2", ChannelPolicy.DISTRACTING, 200L),
            )
            flowState.value = list2
            assertEquals(list2, awaitItem())
        }
    }

    // =========================================================================
    // 6. DELETE ADVERSARIAL CASES
    // =========================================================================

    @Test
    fun `deleteChannel calls dao deleteByHandle for both normalized and trimmed handles`() = runTest(testDispatcher) {
        repository.deleteChannel("veritasium")

        coVerify { dao.deleteByHandle("@veritasium") }
        coVerify { dao.deleteByHandle("veritasium") }
    }

    @Test
    fun `deleteChannel with empty handle does not call dao`() = runTest(testDispatcher) {
        repository.deleteChannel("")
        repository.deleteChannel("   ")

        coVerify(exactly = 0) { dao.deleteByHandle(any()) }
    }

    // =========================================================================
    // 7. CANONICAL IDENTITY PERSISTENCE (PHASE 14)
    // =========================================================================

    @Test
    fun `savePolicy with verified handle persists canonicalKey and real handle`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        val identity = ChannelIdentity(verifiedHandle = "@Veritasium", displayName = "Veritasium")
        repository.savePolicy(identity, ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertEquals("handle:@veritasium", slot.captured.canonicalKey)
        assertEquals("@veritasium", slot.captured.channelHandle)
        assertEquals("Veritasium", slot.captured.displayName)
        assertEquals(ChannelPolicy.PRODUCTIVE, slot.captured.policy)
    }

    @Test
    fun `savePolicy with display name only persists name canonicalKey without fake handle`() = runTest(testDispatcher) {
        val slot = slot<YouTubeChannelEntity>()
        coEvery { dao.insertOrUpdate(capture(slot)) } returns Unit

        val identity = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy")
        repository.savePolicy(identity, ChannelPolicy.PRODUCTIVE)

        assertTrue(slot.isCaptured)
        assertEquals("name:khan academy", slot.captured.canonicalKey)
        assertNull(slot.captured.channelHandle) // Strictly verified: NO fake @khanacademy handle!
        assertEquals("Khan Academy", slot.captured.displayName)
        assertEquals(ChannelPolicy.PRODUCTIVE, slot.captured.policy)
    }

    @Test
    fun `savePolicy with invalid empty identity does not call dao`() = runTest(testDispatcher) {
        repository.savePolicy(ChannelIdentity.EMPTY, ChannelPolicy.PRODUCTIVE)
        coVerify(exactly = 0) { dao.insertOrUpdate(any()) }
    }

    @Test
    fun `getPolicy with ChannelIdentity queries canonical key first`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        coEvery { dao.getPolicyByCanonicalKey("handle:@veritasium") } returns ChannelPolicy.PRODUCTIVE

        val policy = repository.getPolicy(identity)

        assertEquals(ChannelPolicy.PRODUCTIVE, policy)
        coVerify { dao.getPolicyByCanonicalKey("handle:@veritasium") }
    }

    @Test
    fun `getPolicy with ChannelIdentity falls back to handle when canonical key returns null`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        coEvery { dao.getPolicyByCanonicalKey("handle:@veritasium") } returns null
        coEvery { dao.getChannelByCanonicalKey("handle:@veritasium") } returns null
        coEvery { dao.getPolicyByHandle("@veritasium") } returns ChannelPolicy.DISTRACTING

        val policy = repository.getPolicy(identity)

        assertEquals(ChannelPolicy.DISTRACTING, policy)
        coVerify { dao.getPolicyByCanonicalKey("handle:@veritasium") }
        coVerify { dao.getPolicyByHandle("@veritasium") }
    }

    @Test
    fun `getPolicy with display-name only identity queries canonical name key then name fallback`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy")
        coEvery { dao.getPolicyByCanonicalKey("name:khan academy") } returns null
        coEvery { dao.getChannelByCanonicalKey("name:khan academy") } returns null
        coEvery { dao.getPolicyByName("Khan Academy") } returns ChannelPolicy.PRODUCTIVE

        val policy = repository.getPolicy(identity)

        assertEquals(ChannelPolicy.PRODUCTIVE, policy)
        coVerify { dao.getPolicyByCanonicalKey("name:khan academy") }
        coVerify { dao.getPolicyByName("Khan Academy") }
    }

    @Test
    fun `getChannelByIdentity queries canonical key then returns entity`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        val expectedEntity = YouTubeChannelEntity(
            canonicalKey = "handle:@veritasium",
            channelHandle = "@veritasium",
            displayName = "Veritasium",
            policy = ChannelPolicy.PRODUCTIVE,
        )
        coEvery { dao.getChannelByCanonicalKey("handle:@veritasium") } returns expectedEntity

        val entity = repository.getChannelByIdentity(identity)

        assertEquals(expectedEntity, entity)
        assertEquals(identity, entity?.toIdentity())
    }

    @Test
    fun `deleteChannel with ChannelIdentity deletes by canonicalKey and handle`() = runTest(testDispatcher) {
        val identity = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")

        repository.deleteChannel(identity)

        coVerify { dao.deleteByCanonicalKey("handle:@veritasium") }
        coVerify { dao.deleteByHandle("@veritasium") }
    }
}
