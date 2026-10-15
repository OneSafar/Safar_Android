package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import com.safarparmar.app.ui.theme.SafarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class TitliInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstTapIntroducesAndStartsGuideLaterTapsOpenLessonsWithoutAddingChrome() {
        val store = SafarDataStore(InstrumentationRegistry.getInstrumentation().context)
        val key = "test-titli-${UUID.randomUUID()}"
        var starts = 0
        var lists = 0
        compose.setContent { SafarTheme {
            TitliHost(store, {}, {}, introductionKey = key) {
                RegisterTitliGuide(start = { starts++ }, lessons = { lists++ })
                Row(Modifier.testTag("existing.toolbar")) { Text("Existing screen"); TitliGuideButton() }
            }
        } }
        val toolbar = compose.onNodeWithTag("existing.toolbar").fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithTag("titli.button").fetchSemanticsNode().boundsInRoot
        assertTrue(icon.top >= toolbar.top && icon.bottom <= toolbar.bottom)
        compose.onNodeWithTag("titli.button").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("titli.introduction").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("titli.begin").performClick()
        compose.runOnIdle { assertEquals(1, starts); assertEquals(0, lists) }
        compose.waitUntil(5_000) { runBlocking { store.titliIntroductionSeen(key).first() } }
        compose.onNodeWithTag("titli.button").performClick()
        compose.waitUntil(5_000) { lists == 1 }
        compose.onNodeWithTag("titli.introduction").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, starts) }
    }

    @Test fun spotlightCompanionGentlyFloatsWithoutDots() {
        compose.mainClock.autoAdvance = false
        compose.setContent { SafarTheme {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animationsEnabled = true)) {
                Box(Modifier.fillMaxSize()) {
                    TitliSpotlightCompanion(
                        target = { androidx.compose.ui.geometry.Rect(30f, 40f, 160f, 100f) },
                        stageSize = androidx.compose.ui.geometry.Size(600f, 800f),
                    )
                }
            }
        } }
        compose.mainClock.advanceTimeBy(32)
        val firstY = compose.onNodeWithTag("titli.companion", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.advanceTimeBy(650)
        val liftedY = compose.onNodeWithTag("titli.companion", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("Titli should gently float", kotlin.math.abs(liftedY - firstY) > 1f)
        compose.onNodeWithTag("titli.dots", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("titli.speakingDots", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun blackWingsAndPinkGlowRenderInBothThemesWithStaticAccessibleFallback() {
        var dark by mutableStateOf(false)
        compose.setContent { SafarTheme(darkTheme = dark) {
            CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                Box(Modifier.fillMaxSize().testTag("titli.preview")) { TitliIntroduction({}, {}) }
            }
        } }
        listOf(false, true).forEach { mode ->
            compose.runOnIdle { dark = mode }
            val portrait = compose.onNodeWithTag("titli.portrait").captureToImage()
            val pixels = portrait.toPixelMap()
            var black = 0
            var pink = 0
            for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) {
                val color = pixels[x, y]
                if (color.red < 0.08f && color.green < 0.08f && color.blue < 0.08f) black++
                if (color.red > 0.7f && color.green < 0.3f && color.blue > 0.3f) pink++
            }
            assertTrue("Black wings missing", black > 50)
            assertTrue("Pink outline/spots missing", pink > 20)
            compose.onNodeWithTag("titli.begin").assertIsDisplayed()
            val device = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "titli-${if (mode) "dark" else "light"}.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Titli")
            }
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { device.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
