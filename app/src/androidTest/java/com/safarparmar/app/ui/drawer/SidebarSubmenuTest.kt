package com.safarparmar.app.ui.drawer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.ui.navigation.Routes
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SidebarSubmenuTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)

    @Test fun studyOpensInPlaceAndFeatureSelectionNavigatesAndClosesDrawer() {
        val navigated = mutableListOf<String>()
        var closed = 0
        compose.setContent {
            MaterialTheme {
                SafarDrawer(Routes.HOME, false, false, false,
                    onNavigate = { navigated += it }, onToggleDarkTheme = {}, onCloseDrawer = { closed++ })
            }
        }
        val study = sidebarAreas().first { it.route == Routes.STUDY_HUB }
        val anchor = compose.onNodeWithText(label(study.title))
        val anchorY = anchor.fetchSemanticsNode().boundsInRoot.center.y
        anchor.performClick()
        val option = compose.onNodeWithText(label(study.items.first().labelRes)).assertIsDisplayed()
        assertTrue("Upper category must open downward", option.fetchSemanticsNode().boundsInRoot.center.y > anchorY)
        compose.runOnIdle {
            assertEquals(emptyList<String>(), navigated)
            assertEquals(0, closed)
        }
        val feature = study.items.first { it.route == Routes.EKAGRA }
        compose.onNodeWithText(label(feature.labelRes)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(label(feature.labelRes)).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf(Routes.EKAGRA), navigated)
            assertEquals(1, closed)
        }
    }

    @Test fun backDismissesCategoryWithoutNavigatingOrClosingDrawer() {
        var navigations = 0
        var closed = 0
        compose.setContent {
            MaterialTheme {
                SafarDrawer(Routes.HOME, false, false, true,
                    onNavigate = { navigations++ }, onToggleDarkTheme = {}, onCloseDrawer = { closed++ })
            }
        }
        sidebarAreas().filter { it.items.isNotEmpty() }.forEach { area ->
            val anchor = compose.onNodeWithText(label(area.title)).performScrollTo()
            val anchorY = anchor.fetchSemanticsNode().boundsInRoot.center.y
            anchor.performClick()
            val option = compose.onNodeWithText(label(area.items.first().labelRes)).assertIsDisplayed()
            if (area.route == Routes.PROGRESS_HUB) {
                assertTrue("Lower category must open upward", option.fetchSemanticsNode().boundsInRoot.center.y < anchorY)
            }
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            compose.waitForIdle()
            compose.onNodeWithText(label(area.items.first().labelRes)).assertDoesNotExist()
        }
        compose.runOnIdle {
            assertEquals(0, navigations)
            assertEquals(0, closed)
        }
    }

    @Test fun compactDarkRailOpensStudyOptionsAndNavigatesDirectly() {
        val navigated = mutableListOf<String>()
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) {
                    SafarNavigationRail(Routes.HOME, true, { navigated += it }, {},
                        isExpanded = false, onExpandedChange = {})
                }
            }
        }
        val study = sidebarAreas().first { it.route == Routes.STUDY_HUB }
        compose.onNodeWithText(label(study.title)).performClick()
        val feature = study.items.first { it.route == Routes.HABIT_TRACKER }
        compose.onNodeWithText(label(feature.labelRes)).assertIsDisplayed()
        compose.onNodeWithText(label(feature.labelRes)).performClick()
        compose.runOnIdle { assertEquals(listOf(Routes.HABIT_TRACKER), navigated) }
    }
}
