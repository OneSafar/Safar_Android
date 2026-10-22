package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeatureTutorialInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private fun step(id: String) = FeatureTutorialStep(id, "control", R.string.ekagra_tutorial_start_title, R.string.ekagra_tutorial_start_body)

    @Test fun unavailableTargetCanRetryAndScrimNeverInvokesUnderlyingActions() {
        var present by mutableStateOf(false)
        var realClicks = 0
        var completions = 0
        val state = FeatureTutorialState(listOf(step("first")))
        compose.setContent {
            MaterialTheme {
                FeatureTutorialHost(state, { completions++ }, {}) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (present) Button(onClick = { realClicks++ }, modifier = Modifier.tutorialTarget("control")) { Text("Control") }
                    }
                }
            }
        }
        awaitStage("unavailable")
        compose.runOnIdle { present = true }
        compose.onNodeWithTag("tutorial.next").performClick()
        awaitStage("first")
        val control = compose.onNodeWithTag("tutorial.target.control", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val stage = compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("tutorial.stage").performTouchInput { click(control.center - stage.topLeft) }
        compose.runOnIdle { assertEquals(0, realClicks) }
        compose.onNodeWithTag("tutorial.next").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun offscreenTargetsScrollIntoViewAndReregisterOnBackAndNext() {
        val state = FeatureTutorialState(listOf(step("first"), step("second")))
        compose.setContent {
            MaterialTheme {
                FeatureTutorialHost(state, {}, {}) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(1600.dp))
                        Button(onClick = {}, modifier = Modifier.tutorialTarget("control")) { Text("Control") }
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
        awaitStage("first")
        compose.onNodeWithTag("tutorial.target.control", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("tutorial.next").performClick()
        awaitStage("second")
        compose.onNodeWithTag("tutorial.back").performClick()
        awaitStage("first")
    }

    @Test fun companionFollowsEachSpotlightStaysOutsideControlAndStopsWithReducedMotion() {
        val state = FeatureTutorialState(listOf(step("top"), step("bottom")))
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                    FeatureTutorialHost(state, {}, {}) { current ->
                        Box(Modifier.fillMaxSize(), contentAlignment = if (current.id == "top") Alignment.TopStart else Alignment.BottomEnd) {
                            Button(onClick = {}, modifier = Modifier.padding(16.dp).tutorialTarget("control")) { Text("Control") }
                        }
                    }
                }
            }
        }
        fun verifyPosition(): androidx.compose.ui.geometry.Rect {
            val companion = compose.onNodeWithTag("titli.companion", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val control = compose.onNodeWithTag("tutorial.target.control", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val stage = compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot
            assertTrue("Companion must stay in practice stage", stage.contains(companion.topLeft) && stage.contains(companion.bottomRight))
            assertTrue("Companion $companion must not cover control $control in stage $stage", !companion.overlaps(control))
            compose.onNodeWithTag("titli.speakingDots", useUnmergedTree = true).assertDoesNotExist()
            return companion
        }
        awaitStage("top")
        compose.waitForIdle()
        val first = verifyPosition()
        compose.mainClock.advanceTimeBy(3000)
        assertEquals(first, verifyPosition())
        compose.onNodeWithTag("tutorial.next").performClick()
        awaitStage("bottom")
        compose.waitForIdle()
        val second = verifyPosition()
        assertTrue("Companion must follow the new spotlight", first != second)
        compose.onNodeWithTag("tutorial.back").performClick()
        awaitStage("top")
        compose.waitForIdle()
        assertEquals(first, verifyPosition())
    }

    @Test fun floatingSpeechNeverResizesFeatureWhenStepsChange() {
        val state = FeatureTutorialState(listOf(step("first"), step("second")))
        compose.setContent { MaterialTheme {
            Box(Modifier.fillMaxSize().testTag("feature.host")) {
                FeatureTutorialHost(state, {}, {}, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.testTag("feature.navigation")) {
                            Button(onClick = {}, modifier = Modifier.tutorialTarget("control")) { Text("Navigation") }
                        }
                    }
                }
            }
        } }
        awaitStage("first")
        val host = compose.onNodeWithTag("feature.host").fetchSemanticsNode().boundsInRoot
        val stage = compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot
        assertEquals(host, stage)
        val navigation = compose.onNodeWithTag("feature.navigation", true).fetchSemanticsNode().boundsInRoot
        assertEquals(stage.bottom, navigation.bottom, 1f)
        compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
        awaitStage("second")
        assertEquals(stage, compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot)
        assertEquals(navigation, compose.onNodeWithTag("feature.navigation", true).fetchSemanticsNode().boundsInRoot)
    }

    @Test fun nextAndBackHoldDuringMeasurementThenTravelDirectlyWithoutResetting() {
        val state = FeatureTutorialState(listOf(step("top"), step("bottom")))
        var present by mutableStateOf(true)
        compose.setContent { MaterialTheme {
            FeatureTutorialHost(state, {}, {}) { current ->
                Box(Modifier.fillMaxSize(), contentAlignment = if (current.id == "top") Alignment.TopStart else Alignment.BottomEnd) {
                    if (present) Button(onClick = {}, modifier = Modifier.padding(16.dp).tutorialTarget("control")) { Text("Control") }
                }
            }
        } }
        awaitStage("top")
        compose.mainClock.autoAdvance = false
        fun speechTop() = compose.onNodeWithTag("titli.speech", true).fetchSemanticsNode().boundsInRoot.top
        fun travel(actionTag: String, downward: Boolean) {
            val origin = speechTop()
            compose.runOnIdle { present = false }
            compose.onNodeWithTag(actionTag).performClick()
            compose.mainClock.advanceTimeBy(128)
            assertEquals("Keep the last position while the next target is absent", origin, speechTop(), 1f)
            compose.runOnIdle { present = true }
            val samples = mutableListOf(origin)
            repeat(36) {
                compose.mainClock.advanceTimeBy(16)
                samples += speechTop()
            }
            val destination = samples.last()
            assertTrue("Step must move to a different target", kotlin.math.abs(destination - origin) > 30f)
            val maximumFrameTravel = kotlin.math.abs(destination - origin) * 0.25f + 2f
            samples.zipWithNext().forEach { (before, after) ->
                assertTrue("No reset or reversed travel: $samples", if (downward) after >= before - 1f else after <= before + 1f)
                assertTrue("No abrupt jump between frames: $samples", kotlin.math.abs(after - before) <= maximumFrameTravel)
            }
        }
        travel("tutorial.next", downward = true)
        travel("tutorial.back", downward = false)
    }

    @Test fun resizingStageRemeasuresDestinationWithoutChangingStep() {
        val state = FeatureTutorialState(listOf(step("first")))
        var stageHeight by mutableStateOf(600.dp)
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                FeatureTutorialHost(state, {}, {}, modifier = Modifier.fillMaxWidth().height(stageHeight)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                        Button(onClick = {}, modifier = Modifier.padding(16.dp).tutorialTarget("control")) { Text("Control") }
                    }
                }
            }
        } }
        awaitStage("first")
        compose.runOnIdle { stageHeight = 400.dp }
        awaitStage("first")
        val speech = compose.onNodeWithTag("titli.speech", true).fetchSemanticsNode().boundsInRoot
        val control = compose.onNodeWithTag("tutorial.target.control", true).fetchSemanticsNode().boundsInRoot
        val stage = compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot
        assertTrue("Remeasured bubble stays inside resized screen", stage.contains(speech.topLeft) && stage.contains(speech.bottomRight))
        assertTrue("Remeasured bubble stays outside resized target", !speech.overlaps(control))
        compose.runOnIdle { assertEquals(0, state.index) }
    }

    private fun awaitStage(value: String) {
        compose.waitUntil(timeoutMillis = 12_000) {
            runCatching {
                compose.onAllNodes(hasTestTag("tutorial.stage") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value))
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }
}
