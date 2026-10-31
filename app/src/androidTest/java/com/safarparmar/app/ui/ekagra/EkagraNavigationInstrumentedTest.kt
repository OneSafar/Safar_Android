package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EkagraNavigationInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyActionRespondsWhilePreviousIndicatorAnimationIsStillRunning() {
        compose.mainClock.autoAdvance = false
        var selected by mutableStateOf(EkagraNavTab.TIMER)
        val clicks = mutableListOf<EkagraNavTab>()
        compose.setContent {
            SafarTheme {
                EkagraBottomNav(selected, { selected = it; clicks += it }, true, false)
            }
        }
        // Give recomposition one frame per tap; never wait for the indicator to finish.
        listOf(EkagraNavTab.DURATION, EkagraNavTab.HISTORY, EkagraNavTab.MUSIC, EkagraNavTab.THEME, EkagraNavTab.TIMER).forEach { tab ->
            compose.onNodeWithTag("ekagra.nav.${tab.name}").performClick()
            compose.runOnIdle { assertEquals("Callback must run before the animation", tab, selected) }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("ekagra.nav.${tab.name}").assertIsSelected()
        }
        compose.runOnIdle {
            assertEquals(5, clicks.size)
            assertEquals(EkagraNavTab.TIMER, selected)
        }
        compose.onNodeWithTag("ekagra.nav.TIMER").performClick()
        compose.runOnIdle { assertEquals("Reselecting an active tab is a no-op", 5, clicks.size) }
        compose.mainClock.advanceTimeBy(500)
        capture("ekagra-navigation-light.png")
    }

    @Test fun switchingContentDisposesOldScreenWithoutWaitingForAnimation() {
        compose.mainClock.autoAdvance = false
        var selected by mutableStateOf(EkagraNavTab.TIMER)
        val mounted = mutableSetOf<EkagraNavTab>()
        compose.setContent {
            SafarTheme {
                EkagraTabContent(selected) { tab ->
                    DisposableEffect(tab) { mounted += tab; onDispose { mounted -= tab } }
                    Text(tab.name, Modifier.testTag("content.${tab.name}"))
                }
            }
        }
        listOf(EkagraNavTab.DURATION, EkagraNavTab.HISTORY, EkagraNavTab.TIMER).forEach { tab ->
            compose.runOnIdle { selected = tab }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("content.${tab.name}").assertIsDisplayed()
            compose.runOnIdle { assertEquals(setOf(tab), mounted) }
        }
    }

    @Test fun reducedMotionLargeTextAndRtlKeepAllActionsReachable() {
        var selected by mutableStateOf(EkagraNavTab.TIMER)
        compose.setContent {
            SafarTheme(darkTheme = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalMotionPolicy provides MotionPolicy(animationsEnabled = false),
                    LocalDensity provides Density(density.density, 1.6f),
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    EkagraBottomNav(selected, { selected = it }, true, true)
                }
            }
        }
        EkagraNavTab.entries.forEach { tab ->
            compose.onNodeWithTag("ekagra.nav.${tab.name}").assertIsDisplayed().assertHasClickAction().performClick().assertIsSelected()
            val bounds = compose.onNodeWithTag("ekagra.nav.${tab.name}").fetchSemanticsNode().boundsInRoot
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            compose.runOnIdle { assertTrue(bounds.width >= 48 * density); assertTrue(bounds.height >= 48 * density) }
        }
        capture("ekagra-navigation-dark-rtl.png")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
