package com.safarparmar.app.ui.calmcentre

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import com.safarparmar.app.ui.theme.SafarTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class CalmCentreJourneyTest {
    @get:Rule val compose = createComposeRule()

    private fun start(dark: Boolean = false, animated: Boolean = false) {
        compose.setContent {
            SafarTheme(darkTheme = dark) {
                // Keep the shared decoration ticker idle; active practice motion uses the test clock.
                CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(constrained = true, animationsEnabled = animated)) {
                    CalmCentreFlow(dark)
                }
            }
        }
    }

    @Test fun companionsOrbitAndWindDownVisitsEachAnimatedStep() {
        start(animated = true)
        answer(mood = "Feeling low", intention = "Be kinder to myself")
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Begin practice").performClick()
        compose.mainClock.advanceTimeBy(1000)
        capture("kindness-orbit-start")
        compose.mainClock.advanceTimeBy(6000)
        capture("kindness-orbit-depth")
        compose.mainClock.advanceTimeBy(6000)
        capture("kindness-orbit-exchanged")
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("calm-catalogue").performScrollToNode(hasText("Guided moments"))
        compose.onNodeWithTag("calm-guided-shelf").performScrollToNode(hasText("Let the day settle"))
        compose.onNodeWithText("Let the day settle").performClick()
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithText("Arrive, just as you are").assertIsDisplayed()
        capture("rest-step-arrive")
        compose.mainClock.advanceTimeBy(15000)
        compose.onNodeWithText("Unclench a little").assertIsDisplayed()
        capture("rest-step-lower")
        compose.mainClock.advanceTimeBy(15000)
        compose.onNodeWithText("Let thoughts drift").assertIsDisplayed()
        capture("rest-step-rise")
        compose.mainClock.advanceTimeBy(15000)
        compose.onNodeWithText("Nothing more to do").assertIsDisplayed()
        capture("rest-step-settle")
        compose.onNodeWithText("Pause practice").performClick()
        compose.mainClock.advanceTimeBy(15000)
        compose.onNodeWithText("Resume practice").assertIsDisplayed()
        compose.onNodeWithText("A moment to pause").assertIsDisplayed()
    }

    @Test fun catalogueCanBeBrowsedWithoutCheckInAndBoxBreathingHasHolds() {
        start()
        compose.onNodeWithText("Browse all practices").performClick()
        compose.onNodeWithText("Breathe").performClick()
        compose.onNodeWithTag("calm-catalogue").performScrollToNode(hasText("Find your breathing rhythm"))
        compose.onNodeWithTag("calm-breathing-shelf").performScrollToNode(hasText("Box breathing"))
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Box breathing").performClick()
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithText("Hold gently").assertIsDisplayed()
        compose.onNodeWithText("Pause practice").performClick()
        compose.onNodeWithText("Resume practice").assertIsDisplayed()
    }

    private fun answer(mood: String = "A little overwhelmed", intention: String = "Help me reset") {
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText(mood).performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText(intention).performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("1 minute", substring = false).performClick()
        compose.onNodeWithText("Find my practice").performClick()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "calm-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun checkInRecommendsPracticeAndTimerPausesThenCompletes() {
        start()
        capture("check-in-light")
        answer()
        compose.onNodeWithText("Find your rhythm").assertIsDisplayed()
        capture("recommendation-light")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Begin practice").performClick()
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithText("Breathe in").assertIsDisplayed()
        capture("breathing-light")
        compose.mainClock.advanceTimeBy(3500)
        compose.onNodeWithText("Breathe out").assertIsDisplayed()
        compose.onNodeWithText("Pause practice").performClick()
        val pausedTimer = compose.onAllNodes(hasContentDescription("remaining", substring = true)).fetchSemanticsNodes().single().config
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithText("Resume practice").assertIsDisplayed()
        val timerAfterPause = compose.onAllNodes(hasContentDescription("remaining", substring = true)).fetchSemanticsNodes().single().config
        org.junit.Assert.assertEquals(pausedTimer.toString(), timerAfterPause.toString())
        compose.onNodeWithText("Resume practice").performClick()
        compose.mainClock.advanceTimeBy(61_000)
        compose.onNodeWithText("A little more room.").assertIsDisplayed()
        capture("complete-light")
        compose.onNodeWithText("Back to my practices").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Begin practice").assertExists()
    }

    @Test fun kindnessRecommendationAndAlternativesWorkInDarkTheme() {
        start(dark = true)
        answer(mood = "Feeling low", intention = "Be kinder to myself")
        capture("recommendation-dark")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Begin practice").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Make a little room").assertIsDisplayed()
        capture("kindness-dark")
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("calm-catalogue").performScrollToNode(hasText("Guided moments"))
        compose.onNodeWithTag("calm-guided-shelf").performScrollToNode(hasText("Come back to here"))
        compose.onNodeWithText("Come back to here").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Notice five things").assertIsDisplayed()
        capture("grounding-dark")
    }

    @Test fun checkInChoicesRemainReachableAtPhoneAndTabletWidths() {
        val pixels = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.widthPixels
        var targetWidth by mutableFloatStateOf(320f)
        compose.setContent {
            SafarTheme(darkTheme = false) {
                CompositionLocalProvider(
                    LocalMotionPolicy provides MotionPolicy(animationsEnabled = false),
                    LocalDensity provides Density(pixels / targetWidth),
                ) { CalmCentreFlow(false) }
            }
        }
        for (width in listOf(320, 375, 414, 768)) {
            compose.runOnIdle { targetWidth = width.toFloat() }
            compose.onNodeWithText("Doing okay").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Continue").assertIsDisplayed().assertIsEnabled()
            capture("width-$width")
        }
    }
}
