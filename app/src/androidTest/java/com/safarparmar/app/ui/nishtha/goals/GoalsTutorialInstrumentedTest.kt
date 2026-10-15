package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.theme.SafarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GoalsTutorialInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    /** Rendering every scene without a Hilt owner proves it does not fetch live goals or focus sessions. */
    @Test fun allLessonsMeasureTargetsNavigateAndReplayWithoutLiveDependencies() {
        val model = GoalsTutorialViewModel().apply { start(GoalsTutorialLesson.CORE) }
        compose.setContent { SafarTheme { GoalsTutorialPractice(model) } }
        GoalsTutorialLesson.entries.forEach { lesson ->
            compose.runOnIdle { model.start(lesson) }
            lesson.scenes.forEachIndexed { index, scene ->
                awaitTarget(scene)
                compose.onNodeWithTag("goals.tutorial.label").assertIsDisplayed()
                compose.onNodeWithTag("goals.tutorial.close").assertIsDisplayed()
                if (index == 1) {
                    compose.onNodeWithTag("tutorial.back").performScrollTo().performClick()
                    awaitTarget(lesson.scenes.first())
                    compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
                    awaitTarget(scene)
                }
                if (lesson == GoalsTutorialLesson.CORE && scene in setOf(GoalsTutorialScene.TITLE, GoalsTutorialScene.CHECK, GoalsTutorialScene.DONE, GoalsTutorialScene.HISTORY)) capture(scene)
                compose.onNodeWithTag("tutorial.next").performScrollTo().assertIsEnabled().performClick()
            }
            compose.runOnIdle {
                assertNull(model.state); assertEquals(lesson, model.completedLesson); model.acknowledgeCompletion()
            }
            compose.onNodeWithTag("goals.tutorial.lesson.${lesson.name}").assertExists()
        }
        compose.onNodeWithTag("goals.tutorial.done").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen) }
    }

    @Test fun darkLargeTextGuidanceAndInfoStayAccessible() {
        val model = GoalsTutorialViewModel().apply { start(GoalsTutorialLesson.CORE) }
        compose.setContent { SafarTheme(darkTheme = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) { GoalsTutorialPractice(model) }
        } }
        awaitTarget(GoalsTutorialScene.ADD)
        compose.onNodeWithTag("tutorial.info").performScrollTo().performClick()
        compose.onNodeWithTag("tutorial.details").assertIsDisplayed()
        compose.onNodeWithTag("tutorial.info.close").performClick()
        compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
        awaitTarget(GoalsTutorialScene.TITLE)
        compose.onNodeWithTag("tutorial.skip").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen); assertNull(model.completedLesson) }
    }

    @Test fun invitationAndCompletionBookkeepingAreIndependentOfEkagra() {
        val store = SafarDataStore(InstrumentationRegistry.getInstrumentation().context)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val version = kotlin.random.Random.nextInt(2, Int.MAX_VALUE)
        val ekagraBefore = runBlocking { store.featureTutorialCompletedLessons("ekagra", 1).first() }
        val model = GoalsTutorialViewModel()
        var eligible by mutableStateOf(true)
        compose.setContent { SafarTheme { GoalsTutorialEntry(store, eligible, model, version) } }
        val invite = context.getString(R.string.goals_tutorial_invite)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(invite).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.ekagra_tutorial_later)).performClick()
        compose.waitUntil(5_000) { runBlocking { store.featureTutorialInvitationSeen("goals", version).first() } }
        compose.runOnIdle { eligible = false; model.open() }
        compose.onNodeWithText(context.getString(R.string.goals_tutorial_busy)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ekagra_tutorial_done)).performClick()
        compose.runOnIdle { eligible = true; model.start(GoalsTutorialLesson.CORE) }
        awaitTarget(GoalsTutorialScene.ADD)
        compose.runOnIdle { model.complete() }
        compose.waitUntil(5_000) { runBlocking { "CORE" in store.featureTutorialCompletedLessons("goals", version).first() } }
        assertEquals(ekagraBefore, runBlocking { store.featureTutorialCompletedLessons("ekagra", 1).first() })
        compose.onNodeWithTag("goals.tutorial.close").performClick()
        compose.onNodeWithText(invite).assertDoesNotExist()
    }

    @Test fun allSceneInstructionsAndInfoHaveHindiAndHinglishTranslations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun resources(locale: String): android.content.res.Resources {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocales(android.os.LocaleList.forLanguageTags(locale))
            return context.createConfigurationContext(config).resources
        }
        val english = resources("en")
        listOf("hi", "hi-Latn").forEach { locale ->
            val localized = resources(locale)
            GoalsTutorialScene.entries.forEach { scene ->
                assertNotEquals(english.getString(scene.instruction), localized.getString(scene.instruction))
                assertNotEquals(english.getString(scene.body), localized.getString(scene.body))
            }
        }
    }

    @Test fun lightAndDarkGuidanceBlendsWithTheFeatureCanvasLikeEkagra() {
        val model = GoalsTutorialViewModel().apply { start(GoalsTutorialLesson.CORE) }
        var dark by mutableStateOf(false)
        var expectedCanvas = Color.Unspecified
        var expectedCard = Color.Unspecified
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            SafarTheme(darkTheme = dark) {
                CompositionLocalProvider(com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme provides dark) {
                    expectedCanvas = GoalsFlatColors.Bg
                    expectedCard = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        .compositeOver(Color.Black.copy(alpha = 0.38f).compositeOver(expectedCanvas))
                    GoalsTutorialPractice(model)
                }
            }
        }
        listOf(false, true).forEach { mode ->
            compose.runOnIdle { dark = mode; model.start(GoalsTutorialLesson.CORE) }
            awaitTarget(GoalsTutorialScene.ADD)
            compose.onNodeWithTag("tutorial.instruction", useUnmergedTree = true)
                .assertTextEquals(context.getString(GoalsTutorialScene.ADD.instruction))
            compose.onNodeWithTag("tutorial.details").assertDoesNotExist()
            val root = compose.onNodeWithTag("goals.tutorial.practice").captureToImage().toPixelMap()
            // Safe top padding belongs to the real Goals canvas, not a separate tutorial palette.
            assertColorClose(expectedCanvas, root[root.width / 2, 2])
            val card = compose.onNodeWithTag("tutorial.guidance").captureToImage().toPixelMap()
            // Sample a blank patch above the copy, inside the rounded card and its border.
            assertColorClose(expectedCard, card[card.width / 2, 10])
            capture(GoalsTutorialScene.ADD, if (mode) "dark" else "light")
            compose.onNodeWithTag("tutorial.info").performScrollTo().performClick()
            compose.onNodeWithTag("tutorial.details").assertTextEquals(context.getString(GoalsTutorialScene.ADD.body))
            compose.onNodeWithTag("tutorial.info.close").performClick()
        }
    }

    @Test fun shortCompletionIncludesTimeAndHelpReplaysOnlyFourGuides() {
        val model = GoalsTutorialViewModel().apply {
            start(GoalsTutorialLesson.CORE)
            repeat(GoalsTutorialLesson.CORE.scenes.indexOf(GoalsTutorialScene.DONE)) { state!!.next() }
        }
        compose.setContent { SafarTheme { GoalsTutorialPractice(model) } }
        awaitTarget(GoalsTutorialScene.DONE)
        compose.onNodeWithTag("tutorial.target.goals.studyTime", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("tutorial.target.goals.doneActions", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("tutorial.help").performClick()
        GoalsTutorialLesson.entries.forEach { compose.onNodeWithTag("goals.tutorial.lesson.${it.name}").assertExists() }
        compose.onNodeWithTag("goals.tutorial.lesson.REPEAT").assertDoesNotExist()
        compose.onNodeWithTag("goals.tutorial.lesson.MISSED").assertDoesNotExist()
        compose.onNodeWithTag("goals.tutorial.lesson.SCHEDULE").performClick()
        awaitTarget(GoalsTutorialScene.DATE)
    }

    private fun assertColorClose(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.025f)
        assertEquals(expected.green, actual.green, 0.025f)
        assertEquals(expected.blue, actual.blue, 0.025f)
    }

    private fun awaitTarget(scene: GoalsTutorialScene) {
        android.util.Log.i("GoalsTutorialTest", "Awaiting ${scene.name}")
        compose.waitUntil(12_000) {
            compose.onAllNodes(hasTestTag("tutorial.stage") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, scene.name))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tutorial.target.${scene.target}", useUnmergedTree = true).assertExists()
    }
    private fun capture(scene: GoalsTutorialScene, variant: String? = null) {
        val bitmap = compose.onNodeWithTag("goals.tutorial.practice").captureToImage().asAndroidBitmap()
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "goals-${scene.name.lowercase()}${variant?.let { "-$it" }.orEmpty()}.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GoalsTutorial")
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
