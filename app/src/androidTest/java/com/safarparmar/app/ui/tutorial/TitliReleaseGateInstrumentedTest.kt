package com.safarparmar.app.ui.tutorial

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.ekagra.EkagraTutorialEntry
import com.safarparmar.app.ui.ekagra.EkagraTutorialViewModel
import com.safarparmar.app.ui.nishtha.goals.GoalsTutorialEntry
import com.safarparmar.app.ui.nishtha.goals.GoalsTutorialViewModel
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

class TitliReleaseGateInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledGuidanceKeepsScreenContentAndHidesEvenExplicitReplayActions() {
        assumeFalse(BuildConfig.TITLI_TUTORIALS_ENABLED)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SafarDataStore(context)
        val ekagra = EkagraTutorialViewModel().apply { open() }
        val goals = GoalsTutorialViewModel().apply { open() }
        compose.setContent {
            SafarTheme {
                TitliHost(store, {}, {}) {
                    Text("Existing feature content")
                    TitliGuideButton()
                    TitliGuideButton(onClick = { error("Hidden guide must not be clickable") })
                    EkagraTutorialEntry(store, eligible = true, isDarkTheme = false, model = ekagra)
                    GoalsTutorialEntry(store, eligible = true, model = goals)
                    TitliFeatureHelp(TitliFeature.YOUTUBE, onDone = {})
                    listOf(TitliFeature.HABITS, TitliFeature.TOPPERS_BATCH, TitliFeature.MEHFIL, TitliFeature.DHYAN)
                        .forEach { ScreenPracticeEntry(it, isDarkTheme = false) }
                }
            }
        }
        compose.onNodeWithText("Existing feature content").assertIsDisplayed()
        compose.onAllNodesWithTag("titli.button").assertCountEquals(0)
        compose.onNodeWithTag("titli.portrait").assertDoesNotExist()
        compose.onNodeWithTag("ekagra.tutorial.practice").assertDoesNotExist()
        compose.onNodeWithTag("goals.tutorial.practice").assertDoesNotExist()
        compose.onNodeWithTag("practice.label").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.ekagra_tutorial_invite)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.goals_tutorial_invite)).assertDoesNotExist()
    }
}
