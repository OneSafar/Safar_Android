package com.safarparmar.app.ui.tutorial

import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.feature.habits.ui.LocalHabitDarkTheme
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScreenPracticeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    /** No Hilt owner, service or repository is supplied to any of the 55 native practice scenes. */
    @Test fun everyNativeTargetMeasuresAndSupportsForwardBackwardAndReplay() {
        var lesson by mutableStateOf(PracticeLesson.BATCH_CORE)
        var state by mutableStateOf(FeatureTutorialState(lesson.steps()))
        var completed = 0
        compose.setContent { SafarTheme {
            CompositionLocalProvider(LocalPlannerIsDarkTheme provides false, LocalHabitDarkTheme provides false,
                LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                FeatureTutorialHost(state, { completed++ }, {}, modifier = Modifier) { step ->
                    ScreenPracticeStage(lesson.feature, PracticeScene.valueOf(step.id), false)
                }
            }
        } }
        PracticeLesson.entries.forEach { nextLesson ->
            compose.runOnIdle { lesson = nextLesson; state = FeatureTutorialState(nextLesson.steps()) }
            nextLesson.scenes.forEachIndexed { index, scene ->
                awaitTarget(scene)
                if (scene in setOf(PracticeScene.BATCH_DONE, PracticeScene.HABIT_REPEAT,
                    PracticeScene.MEHFIL_SHARE, PracticeScene.DHYAN_PAUSE, PracticeScene.CIRCLE_PRIVACY, PracticeScene.CHECKIN_SAVE, PracticeScene.JOURNAL_WRITE, PracticeScene.STREAKS_CALENDAR, PracticeScene.ANALYTICS_MONTH, PracticeScene.PLANNER_HOME, PracticeScene.PLANNER_CALENDAR)) {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    val file = java.io.File(context.cacheDir, "titli-${scene.name}.png")
                    file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                }
                if (index == 1) {
                    compose.onNodeWithTag("tutorial.back").performScrollTo().performClick(); awaitTarget(nextLesson.scenes.first())
                    compose.onNodeWithTag("tutorial.next").performScrollTo().performClick(); awaitTarget(scene)
                }
                compose.onNodeWithTag("tutorial.next").performScrollTo().assertIsEnabled().performClick()
            }
            compose.runOnIdle { state.restart() }; awaitTarget(nextLesson.scenes.first())
        }
        compose.runOnIdle { assertEquals(PracticeLesson.entries.size, completed) }
    }

    @Test fun darkLargeTextUsesSameHostAndInlineFormsRemainInItsWindow() {
        val model = ScreenPracticeViewModel().apply { start(PracticeLesson.HABITS_CORE) }
        compose.setContent { SafarTheme(darkTheme = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f),
                LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                ScreenPracticeTutorial(TitliFeature.HABITS, true, model)
            }
        } }
        awaitTarget(PracticeScene.HABIT_ADD)
        compose.onNodeWithTag("tutorial.next").performScrollTo().performClick(); awaitTarget(PracticeScene.HABIT_NAME)
        compose.onNodeWithTag("tutorial.info").performScrollTo().performClick()
        compose.onNodeWithTag("tutorial.details").assertIsDisplayed()
        compose.onNodeWithTag("tutorial.info.close").performClick()
        compose.onNodeWithTag("tutorial.skip").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen); assertNull(model.completedLesson) }
    }

    @Test fun everyDarkSceneHandlesLargeTextWithoutLiveDependencies() {
        var lesson by mutableStateOf(PracticeLesson.BATCH_CORE)
        var state by mutableStateOf(FeatureTutorialState(lesson.steps().take(1)))
        compose.setContent { SafarTheme(darkTheme = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f),
                LocalPlannerIsDarkTheme provides true, LocalHabitDarkTheme provides true,
                LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FeatureTutorialHost(state, {}, {}) { step ->
                        ScreenPracticeStage(lesson.feature, PracticeScene.valueOf(step.id), true)
                    }
                }
            }
        } }
        PracticeLesson.entries.forEach { nextLesson ->
            nextLesson.scenes.forEach { scene ->
                compose.runOnIdle {
                    lesson = nextLesson
                    state = FeatureTutorialState(nextLesson.steps().filter { it.id == scene.name })
                }
                awaitTarget(scene)
                compose.onNodeWithTag("tutorial.instruction", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                if (scene in setOf(PracticeScene.BATCH_DONE, PracticeScene.HABIT_REPEAT,
                    PracticeScene.MEHFIL_SHARE, PracticeScene.DHYAN_PAUSE, PracticeScene.CIRCLE_PRIVACY, PracticeScene.CHECKIN_SAVE, PracticeScene.JOURNAL_WRITE, PracticeScene.STREAKS_CALENDAR, PracticeScene.ANALYTICS_MONTH, PracticeScene.PLANNER_HOME, PracticeScene.PLANNER_CALENDAR)) {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    val file = java.io.File(context.cacheDir, "titli-dark-${scene.name}.png")
                    file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }

    @Test fun batchUsesTrackerBackgroundAndReadableNativeInkInBothThemes() {
        var dark by mutableStateOf(true)
        var expectedInk = Color.Unspecified
        compose.setContent { SafarTheme(darkTheme = dark) {
            expectedInk = MaterialTheme.colorScheme.onSurface
            Surface {
                ScreenPracticeStage(TitliFeature.TOPPERS_BATCH, PracticeScene.BATCH_DONE, dark)
            }
        } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(true, false).forEach { mode ->
            compose.runOnIdle { dark = mode }
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.practice_sample_lecture))
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(expectedInk, layouts.single().layoutInput.style.color)
            val actual = compose.onRoot().captureToImage().toPixelMap()[2, 2]
            val expected = if (mode) Color(0xFF19151A) else Color(0xFFFAF7F2)
            assertEquals(expected.red, actual.red, .01f)
            assertEquals(expected.green, actual.green, .01f)
            assertEquals(expected.blue, actual.blue, .01f)
        }
    }

    @Test fun allInstructionsAndDetailsHaveHindiAndHinglishCopy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun resources(locale: String): android.content.res.Resources {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocales(android.os.LocaleList.forLanguageTags(locale))
            return context.createConfigurationContext(config).resources
        }
        val english = resources("en")
        listOf("hi", "hi-Latn").forEach { language ->
            val translated = resources(language)
            PracticeScene.entries.forEach { scene ->
                assertNotEquals(english.getString(scene.instruction), translated.getString(scene.instruction))
                assertNotEquals(english.getString(scene.body), translated.getString(scene.body))
            }
        }
    }

    @Test fun plannerSpeechBubbleNeverMovesTheNativeBottomBar() {
        val model = ScreenPracticeViewModel().apply { start(PracticeLesson.PLANNER_CORE) }
        compose.setContent { SafarTheme {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                ScreenPracticeTutorial(TitliFeature.EXAM_PLANNER, false, model)
            }
        } }
        var bottom: Float? = null
        PracticeLesson.PLANNER_CORE.scenes.forEachIndexed { index, scene ->
            awaitTarget(scene)
            val position = compose.onNodeWithText("Plan", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom
            bottom?.let { assertEquals(it, position, 1f) }
            bottom = position
            if (index < PracticeLesson.PLANNER_CORE.scenes.lastIndex) compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
        }
        compose.onNodeWithTag("tutorial.info").performScrollTo().performClick()
        compose.onNodeWithTag("tutorial.details").assertIsDisplayed()
        compose.onNodeWithTag("tutorial.info.close").performClick()
        compose.onNodeWithTag("tutorial.skip").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen); assertNull(model.completedLesson) }
    }

    private fun awaitTarget(scene: PracticeScene) {
        compose.waitUntil("Target ready: ${scene.name}", 6_000) {
            compose.onAllNodesWithTag("tutorial.stage").fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.StateDescription) == scene.name
            }
        }
        compose.onNodeWithTag("tutorial.next").assertIsEnabled()
    }
}
