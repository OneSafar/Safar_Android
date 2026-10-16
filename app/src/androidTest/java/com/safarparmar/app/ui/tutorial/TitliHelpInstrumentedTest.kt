package com.safarparmar.app.ui.tutorial

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.*
import com.safarparmar.app.ui.theme.SafarTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class TitliHelpInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun personalizedMenuRoutesAllFeaturesAndOther() {
        val selected = mutableListOf<TitliFeature>(); var queries = 0
        compose.setContent { SafarTheme { TitliHelpMenu("Sam", selected::add, { queries++ }, {}) } }
        compose.onNodeWithText("Hi, Sam! How can I help you?").assertIsDisplayed()
        TitliFeature.entries.forEach { feature -> compose.onNodeWithTag("titli.menu.${feature.name}").performScrollTo().performClick() }
        compose.onNodeWithTag("titli.menu.other").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(TitliFeature.entries.toList(), selected); assertEquals(1, queries) }
    }
    @Test fun selectingFeatureAutomaticallyStartsItsRegisteredGuide() {
        org.junit.Assume.assumeTrue(com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED)
        val store = SafarDataStore(InstrumentationRegistry.getInstrumentation().context)
        val key = "test-titli-menu-${UUID.randomUUID()}"
        runBlocking { store.setTitliIntroductionSeen(key) }
        var destination by mutableStateOf<TitliFeature?>(null); var starts = 0
        compose.setContent { SafarTheme {
            TitliHost(store, onOpenEkagra = { destination = TitliFeature.EKAGRA }, onOpenGoals = {}, introductionKey = key) {
                if (destination != null) RegisterTitliGuide(start = { starts++ }, lessons = {}, feature = destination)
                TitliGuideButton()
            }
        } }
        compose.onNodeWithTag("titli.button").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("titli.menu.EKAGRA").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("titli.menu.EKAGRA").performScrollTo().performClick()
        compose.waitUntil(5000) { starts == 1 }
        compose.runOnIdle { assertEquals(TitliFeature.EKAGRA, destination) }
    }
    @Test fun queryFormValidatesAndDisplaysReplyWithoutNetworkWrites() {
        var state by mutableStateOf(TitliQueriesState(loading = false, signedIn = true,
            queries = listOf(TitliQueryDto("sample", "How do I start?", reply = TitliReplyDto("Tap Start.")))))
        var submissions = 0
        compose.setContent { SafarTheme {
            TitliQueriesContent(state, { state = state.copy(draft = it) }, { submissions++; state = state.copy(draft = "", sent = true) }, {}, {})
        } }
        compose.onNodeWithTag("titli.query.submit").assertIsNotEnabled()
        compose.onNodeWithTag("titli.query.input").performTextInput("How do I use Ekagra?")
        compose.onNodeWithTag("titli.query.submit").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("titli.query.sent").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("titli.query.reply.sample").performScrollTo().assertTextEquals("Tap Start.")
        compose.runOnIdle { assertEquals(1, submissions) }
    }
    @Test fun finalActionSaysOk() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("OK", context.getString(R.string.feature_tutorial_finish))
    }
}
