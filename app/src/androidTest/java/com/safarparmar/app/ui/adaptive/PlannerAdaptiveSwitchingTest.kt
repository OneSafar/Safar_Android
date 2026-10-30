package com.safarparmar.app.ui.adaptive

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.ui.studyplanner.screens.PlannerBottomBar
import com.safarparmar.app.ui.studyplanner.screens.PlannerTopNavigation
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Reconfigure the live provider without remounting the composition or using a network/account. */
class PlannerAdaptiveSwitchingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactExpandedCompactReactsWithoutLosingDestinationOrDraft() {
        val config = mutableStateOf(configuration(390, 800, 390))
        var selected by mutableStateOf(PlannerSection.CALENDAR)
        var observed: SafarAdaptiveState? = null
        compose.setContent {
            CompositionLocalProvider(LocalConfiguration provides config.value) {
                ProvideSafarAdaptive {
                    val adaptive = LocalSafarAdaptiveState.current
                    SideEffect { observed = adaptive }
                    SafarTheme {
                        var draft by rememberSaveable { mutableStateOf("Unfinished topic") }
                        Column(Modifier.width(390.dp).fillMaxHeight()) {
                            if (adaptive.usePlannerTopNavigation) {
                                Box(Modifier.testTag("top")) { PlannerTopNavigation(selected) { selected = it } }
                            }
                            Text(selected.name, Modifier.testTag("destination"))
                            androidx.compose.material3.TextField(draft, { draft = it }, Modifier.testTag("draft"))
                            if (!adaptive.usePlannerTopNavigation) {
                                Box(Modifier.testTag("bottom")) { PlannerBottomBar(selected) { selected = it } }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("bottom").assertExists()
        compose.onNodeWithTag("top").assertDoesNotExist()
        compose.onNodeWithTag("draft").performTextReplacement("Retain this edit")
        compose.runOnIdle { config.value = configuration(1280, 800, 800) }
        compose.onNodeWithTag("top").assertExists()
        compose.onNodeWithTag("bottom").assertDoesNotExist()
        compose.onNodeWithTag("draft").assertTextContains("Retain this edit")
        compose.onNodeWithTag("destination").assertTextEquals("CALENDAR")
        compose.runOnIdle { assertTrue(observed!!.isWideScreen) }
        // A narrow window may still be hosted by tablet hardware.
        compose.runOnIdle { config.value = configuration(390, 800, 800) }
        compose.onNodeWithTag("bottom").assertExists()
        compose.onNodeWithTag("top").assertDoesNotExist()
        compose.onNodeWithTag("draft").assertTextContains("Retain this edit")
        compose.onNodeWithTag("destination").assertTextEquals("CALENDAR")
        compose.runOnIdle { assertFalse(observed!!.isWideScreen) }
    }

    private fun configuration(width: Int, height: Int, smallest: Int) = Configuration().apply {
        screenWidthDp = width
        screenHeightDp = height
        smallestScreenWidthDp = smallest
        orientation = if (width > height) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
    }
}
