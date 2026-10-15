package com.safarparmar.app.ui.ekagra

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.theme.SafarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class EkagraTutorialInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    /** No Hilt host, service, repository, or permission launcher is supplied. */
    @Test fun everyLessonMeasuresTargetsCompletesAndReplaysWithoutLiveDependencies() {
        val model = EkagraTutorialViewModel().apply { start(EkagraTutorialLesson.CORE) }
        compose.setContent { SafarTheme { EkagraTutorialPractice(model) } }
        EkagraTutorialLesson.entries.forEach { lesson ->
            compose.runOnIdle { model.start(lesson) }
            lesson.scenes.forEachIndexed { index, scene ->
                awaitTarget(scene)
                compose.onNodeWithTag("tutorial.next").performScrollTo().assertIsEnabled()
                if (index == 1) {
                    compose.onNodeWithTag("tutorial.back").performClick()
                    awaitTarget(lesson.scenes[0])
                    compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
                    awaitTarget(scene)
                }
                if (lesson == EkagraTutorialLesson.CORE && scene in setOf(EkagraTutorialScene.START, EkagraTutorialScene.SAVE, EkagraTutorialScene.HISTORY)) {
                    capturePreview(scene)
                }
                compose.onNodeWithTag("tutorial.next").performScrollTo().performClick()
            }
            compose.runOnIdle {
                assertNull(model.state)
                assertEquals(lesson, model.completedLesson)
                model.acknowledgeCompletion()
            }
            compose.onNodeWithTag("tutorial.lesson.${lesson.name}").assertExists()
        }
        compose.onNodeWithTag("tutorial.done").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen) }
    }

    @Test fun darkThemeLargeTextKeepsGuidanceAndExitReachable() {
        val model = EkagraTutorialViewModel().apply { start(EkagraTutorialLesson.CORE) }
        compose.setContent {
            SafarTheme(darkTheme = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    EkagraTutorialPractice(model, isDarkTheme = true)
                }
            }
        }
        awaitTarget(EkagraTutorialScene.MODE)
        compose.onNodeWithTag("tutorial.next").performScrollTo().assertIsDisplayed().performClick()
        awaitTarget(EkagraTutorialScene.SETTINGS_ENTRY)
        compose.onNodeWithTag("tutorial.skip").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(model.isOpen); assertNull(model.completedLesson) }
    }

    @Test fun tutorialPreferencesAreVersionedAndDoNotChangeTimerSettings() = runBlocking {
        // Write only uniquely namespaced test keys; timer settings are read-only.
        val store = SafarDataStore(InstrumentationRegistry.getInstrumentation().context)
        val feature = "test-ekagra-${UUID.randomUUID()}"
        val durationBefore = store.focusDurationMinutes.first()
        store.setTourDone(feature, true)
        assertFalse(store.featureTutorialInvitationSeen(feature, 1).first())
        store.setFeatureTutorialInvitationSeen(feature, 1)
        store.completeFeatureTutorialLesson(feature, 1, "CORE")
        store.completeFeatureTutorialLesson(feature, 1, "CORE")
        assertTrue(store.featureTutorialInvitationSeen(feature, 1).first())
        assertEquals(setOf("CORE"), store.featureTutorialCompletedLessons(feature, 1).first())
        assertFalse(store.featureTutorialInvitationSeen(feature, 2).first())
        assertTrue(store.featureTutorialCompletedLessons(feature, 2).first().isEmpty())
        assertEquals(durationBefore, store.focusDurationMinutes.first())
    }

    @Test fun invitationIsOfferedOnceAndActiveFlowsBlockManualReplay() {
        val store = SafarDataStore(InstrumentationRegistry.getInstrumentation().context)
        val feature = "test-invitation-${UUID.randomUUID()}"
        val model = EkagraTutorialViewModel()
        var eligible by androidx.compose.runtime.mutableStateOf(true)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val invitation = context.getString(com.safarparmar.app.R.string.ekagra_tutorial_invite)
        compose.setContent { SafarTheme { EkagraTutorialEntry(store, eligible, false, model, progressFeature = feature) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(invitation).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.ekagra_tutorial_later)).performClick()
        compose.waitUntil(5_000) { runBlocking { store.featureTutorialInvitationSeen(feature, 1).first() } }
        compose.runOnIdle { eligible = false; model.open() }
        compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.ekagra_tutorial_busy)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.ekagra_tutorial_done)).performClick()
        compose.runOnIdle { eligible = true }
        compose.onNodeWithText(invitation).assertDoesNotExist()
        compose.runOnIdle { model.open() }
        compose.onNodeWithTag("tutorial.lesson.CORE").assertExists()
        compose.onNodeWithTag("tutorial.close").performClick()
    }

    @Test fun everyLessonHasEnglishHindiAndHinglishCopy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun resources(tag: String): android.content.res.Resources {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocales(android.os.LocaleList.forLanguageTags(tag))
            return context.createConfigurationContext(config).resources
        }
        val english = resources("en")
        listOf("hi", "hi-Latn").forEach { locale ->
            val localized = resources(locale)
            EkagraTutorialScene.entries.forEach { scene ->
                assertTrue(localized.getString(scene.titleRes).isNotBlank())
                assertTrue(localized.getString(scene.instructionRes).isNotBlank())
                assertNotEquals("Missing $locale translation for ${scene.name}",
                    english.getString(scene.messageRes), localized.getString(scene.messageRes))
            }
        }
    }

    @Test fun speechGuidanceHidesDetailsBehindInfoAndPreservesFullScreenNavigation() {
        val model = EkagraTutorialViewModel().apply {
            start(EkagraTutorialLesson.CORE)
            repeat(3) { state!!.next() }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { SafarTheme { EkagraTutorialPractice(model) } }
        awaitTarget(EkagraTutorialScene.START)
        val explanation = context.getString(EkagraTutorialScene.START.messageRes)
        compose.onNodeWithText(explanation).assertDoesNotExist()
        compose.onNodeWithText(context.getString(EkagraTutorialScene.START.instructionRes), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("tutorial.ekagra.navigation", useUnmergedTree = true).assertIsDisplayed()
        val stage = compose.onNodeWithTag("tutorial.stage").fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNodeWithTag("tutorial.ekagra.navigation", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals("Navigation stays at the bottom of the full-size stage", stage.bottom, navigation.bottom, 1f)
        compose.onNodeWithTag("titli.speech").assertIsDisplayed()
        val info = compose.onNodeWithTag("tutorial.info").fetchSemanticsNode().boundsInRoot
        val help = compose.onNodeWithTag("tutorial.help").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithTag("tutorial.close").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(info.center.y, help.center.y, 1f)
        org.junit.Assert.assertEquals(help.center.y, close.center.y, 1f)
        org.junit.Assert.assertTrue(info.right <= help.left && help.right <= close.left)
        val heading = compose.onNodeWithTag("tutorial.title", true).fetchSemanticsNode().boundsInRoot
        val instruction = compose.onNodeWithTag("tutorial.instruction", true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(heading.left, instruction.left, 1f)
        compose.onNodeWithTag("titli.dots", true).assertDoesNotExist()
        compose.onNodeWithTag("titli.speakingDots", true).assertDoesNotExist()
        compose.onNodeWithTag("tutorial.info").performClick()
        compose.onNodeWithTag("tutorial.details").assertTextEquals(explanation).assertIsDisplayed()
        compose.onNodeWithTag("tutorial.info.close").performClick()
        compose.onNodeWithTag("tutorial.details").assertDoesNotExist()
        compose.onNodeWithTag("tutorial.next").performClick()
        awaitTarget(EkagraTutorialScene.RUNNING)
    }

    @Test fun shortGuidesKeepNavigationGroupedAndCircleChoicesReadableInBothModes() {
        val model = EkagraTutorialViewModel()
        var dark by androidx.compose.runtime.mutableStateOf(false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { SafarTheme(darkTheme = dark) { EkagraTutorialPractice(model, isDarkTheme = dark) } }
        listOf(false, true).forEach { mode ->
            compose.runOnIdle { dark = mode }
            listOf(EkagraTutorialScene.SETTINGS_ENTRY, EkagraTutorialScene.PERSONALIZE_ENTRY,
                EkagraTutorialScene.KAVACH_SETTINGS, EkagraTutorialScene.CIRCLE_JOIN,
                EkagraTutorialScene.CIRCLE_CREATE, EkagraTutorialScene.CIRCLE_CHOICES).forEach { scene ->
                compose.runOnIdle {
                    val lesson = EkagraTutorialLesson.entries.first { scene in it.scenes }
                    model.start(lesson)
                    repeat(lesson.scenes.indexOf(scene)) { model.state!!.next() }
                }
                awaitTarget(scene)
                if (scene == EkagraTutorialScene.PERSONALIZE_ENTRY) {
                    compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.ekagra_nav_music), useUnmergedTree = true).assertExists()
                    compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.ekagra_theme_title), useUnmergedTree = true).assertExists()
                }
                if (scene == EkagraTutorialScene.CIRCLE_CHOICES) assertTextContrast(context.getString(com.safarparmar.app.R.string.ekagra_tutorial_sample_circle))
                capturePreview(scene, if (mode) "dark" else "light")
            }
        }
    }

    @Test fun helpReturnsToOnlyFourGuidesAndCanReplay() {
        val model = EkagraTutorialViewModel().apply { start(EkagraTutorialLesson.CORE) }
        compose.setContent { SafarTheme { EkagraTutorialPractice(model) } }
        awaitTarget(EkagraTutorialScene.MODE)
        compose.onNodeWithTag("tutorial.help").performClick()
        EkagraTutorialLesson.entries.forEach { compose.onNodeWithTag("tutorial.lesson.${it.name}").assertExists() }
        compose.onNodeWithTag("tutorial.lesson.PROTECTION").performClick()
        awaitTarget(EkagraTutorialScene.KAVACH_SETTINGS)
    }

    @Test fun timerFollowsAppAppearanceAndSelectedFeatureBackdrop() {
        val model = EkagraTutorialViewModel()
        var dark by androidx.compose.runtime.mutableStateOf(false)
        var theme by androidx.compose.runtime.mutableStateOf(selectableVisualThemes.first())
        compose.setContent {
            SafarTheme(darkTheme = dark) {
                EkagraTutorialPractice(model, isDarkTheme = dark, selectedTheme = theme)
            }
        }
        listOf(false, true).forEach { mode ->
            selectableVisualThemes.take(2).forEach { selected ->
                compose.runOnIdle {
                    dark = mode
                    theme = selected
                    model.start(EkagraTutorialLesson.CORE)
                }
                awaitTarget(EkagraTutorialScene.MODE)
                // Timer labels must use the same canvas ink as the actual feature.
                assertTextContrast("25:00", minimumContrast = 3f)
                capturePreview(EkagraTutorialScene.MODE, "${selected.name.lowercase()}-${if (mode) "dark" else "light"}")
            }
        }
    }

    @Test fun blockedAppsUsesTheRealPickerLayoutWithIsolatedSamplesInBothThemes() {
        val model = EkagraTutorialViewModel()
        var dark by androidx.compose.runtime.mutableStateOf(false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { SafarTheme(darkTheme = dark) { EkagraTutorialPractice(model, isDarkTheme = dark) } }
        listOf(false, true).forEach { mode ->
            compose.runOnIdle {
                dark = mode
                model.start(EkagraTutorialLesson.PROTECTION)
                model.state!!.next()
            }
            awaitTarget(EkagraTutorialScene.KAVACH_APPS)
            listOf(com.safarparmar.app.R.string.kavach_shield_configuration_title,
                com.safarparmar.app.R.string.kavach_search_apps,
                com.safarparmar.app.R.string.kavach_select_distracting,
                com.safarparmar.app.R.string.common_select_all,
                com.safarparmar.app.R.string.common_clear_all,
                com.safarparmar.app.R.string.kavach_save_configuration).forEach { id ->
                compose.onNodeWithText(context.getString(id), useUnmergedTree = true).assertExists()
            }
            assertTextContrast(context.getString(com.safarparmar.app.R.string.ekagra_tutorial_sample_app_one))
            // Even invoking Save directly cannot leave practice or activate real protection.
            compose.onNodeWithText(context.getString(com.safarparmar.app.R.string.kavach_save_configuration), useUnmergedTree = true).performClick()
            compose.runOnIdle { assertEquals(EkagraTutorialScene.KAVACH_APPS.name, model.state!!.currentStep.id) }
            capturePreview(EkagraTutorialScene.KAVACH_APPS, if (mode) "picker-dark" else "picker-light")
            compose.onNodeWithTag("tutorial.next").performClick()
            awaitTarget(EkagraTutorialScene.KAVACH_READY)
        }
    }

    private fun assertTextContrast(label: String, minimumContrast: Float = 4.5f) {
        val pixels = compose.onNodeWithText(label, useUnmergedTree = true).captureToImage().toPixelMap()
        val background = pixels[pixels.width - 1, pixels.height - 1].luminance()
        var highestContrast = 1f
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val foreground = pixels[x, y].luminance()
            val contrast = (maxOf(background, foreground) + 0.05f) / (minOf(background, foreground) + 0.05f)
            highestContrast = maxOf(highestContrast, contrast)
        }
        assertTrue("Unreadable text '$label': contrast $highestContrast", highestContrast >= minimumContrast)
    }

    private fun capturePreview(scene: EkagraTutorialScene, variant: String? = null) {
        val bitmap = compose.onNodeWithTag("tutorial.practice").captureToImage()
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = android.content.ContentValues().apply {
            val suffix = variant?.let { "-$it" } ?: ""
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "tutorial-${scene.name.lowercase()}$suffix.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EkagraTutorial")
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use { bitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun awaitTarget(scene: EkagraTutorialScene) {
        android.util.Log.i("EkagraTutorialTest", "Awaiting ${scene.name}")
        compose.waitUntil(timeoutMillis = 12_000) {
            compose.onAllNodes(hasTestTag("tutorial.stage") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, scene.name))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tutorial.target.${scene.targetId}", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("tutorial.practiceLabel").assertIsDisplayed()
        compose.onNodeWithTag("tutorial.close").assertIsDisplayed()
    }
}
