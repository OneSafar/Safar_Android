package com.safarparmar.app.ui.home

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.performance.MotionPolicy
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeBackgroundCarouselTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backgroundSpansTheScreenAndAdvancesWithAnimationsDisabled() {
        compose.setContent {
            SafarTheme(darkTheme = false) {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1f),
                    LocalMotionPolicy provides MotionPolicy(animationsEnabled = false)) {
                    PlumPhoneHome(false, {}, {},
                        Modifier.requiredSize(375.dp, 900.dp).testTag("home"), actions = {})
                }
            }
        }
        compose.onNodeWithTag("home-module-0").assertIsDisplayed()
        val root = compose.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        val background = compose.onNodeWithTag("home-module-background").fetchSemanticsNode().boundsInRoot
        val hero = compose.onNodeWithTag("home-module-carousel").fetchSemanticsNode().boundsInRoot
        assertEquals(root.width, background.width, 1f)
        assertTrue("Background must extend behind the header", background.top < hero.top)
        assertTrue("Background must extend beyond the hero", background.height > hero.height * 2)
        compose.waitUntil(timeoutMillis = 7_000) {
            compose.onAllNodesWithTag("home-module-1").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home-module-1").assertExists()
    }
}
