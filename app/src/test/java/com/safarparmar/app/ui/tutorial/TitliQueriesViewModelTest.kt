package com.safarparmar.app.ui.tutorial

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TitliQueriesViewModelTest {
    private val models = mutableListOf<TitliQueriesViewModel>()
    private val api = mockk<TitliApi>()
    private val store = mockk<SafarDataStore>()
    private val context = mockk<Context>()
    private val owner = MutableStateFlow<String?>("student")
    private val signedIn = MutableStateFlow(true)
    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { store.userId } returns owner
        every { store.isLoggedIn } returns signedIn
        every { context.getString(any()) } returns "Try again"
        coEvery { api.mine() } returns TitliQueriesResponse()
    }
    @After fun cleanup() {
        runBlocking { models.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } }
        Dispatchers.resetMain()
    }
    private fun model() = TitliQueriesViewModel(api, store, context).also(models::add)
    @Test fun failureKeepsDraftAndRetryUsesSameRequestId() = runTest {
        val requests = mutableListOf<TitliSubmitRequest>()
        coEvery { api.submit(any()) } coAnswers {
            requests += firstArg<TitliSubmitRequest>()
            if (requests.size == 1) error("Network failed")
            TitliSubmitResponse(TitliQueryDto("query", requests.last().message))
        }
        val model = model(); runCurrent(); model.edit("How do I use Ekagra?"); model.submit(); runCurrent()
        assertEquals("How do I use Ekagra?", model.state.value.draft)
        assertNotNull(model.state.value.error)
        model.submit(); runCurrent()
        assertEquals(requests[0].requestId, requests[1].requestId)
        assertTrue(model.state.value.sent); assertEquals("", model.state.value.draft)
        assertEquals(1, model.state.value.queries.size)
    }
    @Test fun lateResponseCannotAppearInAnotherAccount() = runTest {
        val response = CompletableDeferred<TitliSubmitResponse>()
        coEvery { api.submit(any()) } coAnswers { response.await() }
        val model = model(); runCurrent(); model.edit("Private query"); model.submit(); runCurrent()
        owner.value = "another-student"; runCurrent()
        response.complete(TitliSubmitResponse(TitliQueryDto("private", "Private query"))); runCurrent()
        assertTrue(model.state.value.queries.isEmpty()); assertEquals("", model.state.value.draft)
        assertFalse(model.state.value.sent)
    }
    @Test fun signedOutAndShortQueriesCannotSubmit() = runTest {
        val model = model(); runCurrent(); model.edit("a"); model.submit(); runCurrent()
        signedIn.value = false; runCurrent(); model.edit("A real question"); model.submit(); runCurrent()
        coVerify(exactly = 0) { api.submit(any()) }
        assertFalse(model.state.value.signedIn)
    }
    @Test fun duplicateTapsSendOnlyOnce() = runTest {
        val response = CompletableDeferred<TitliSubmitResponse>()
        coEvery { api.submit(any()) } coAnswers { response.await() }
        val model = model(); runCurrent(); model.edit("A real question"); model.submit(); model.submit(); runCurrent()
        coVerify(exactly = 1) { api.submit(any()) }
        response.complete(TitliSubmitResponse(TitliQueryDto("q", "A real question"))); runCurrent()
    }
    @Test fun anOlderInboxReadCannotEraseANewlySubmittedQuery() = runTest {
        val inbox = CompletableDeferred<TitliQueriesResponse>()
        coEvery { api.mine() } coAnswers { inbox.await() }
        coEvery { api.submit(any()) } returns TitliSubmitResponse(TitliQueryDto("new", "Help me use Ekagra"))
        val model = model(); runCurrent(); model.edit("Help me use Ekagra"); model.submit(); runCurrent()
        inbox.complete(TitliQueriesResponse()); runCurrent()
        assertEquals(listOf("new"), model.state.value.queries.map { it.id })
        assertFalse(model.state.value.loading)
    }
    @Test fun bothExistingSubmitAndNewInboxDatesParse() {
        val gson = Gson()
        assertEquals("today", gson.fromJson("""{"id":"q","message":"Help","createdAt":"today"}""", TitliQueryDto::class.java).createdAt)
        val query = gson.fromJson("""{"id":"q","message":"Help","created_at":"today","reply":{"message":"Use Start.","created_at":"later"}}""", TitliQueryDto::class.java)
        assertEquals("today", query.createdAt); assertEquals("Use Start.", query.reply!!.message)
    }
}
