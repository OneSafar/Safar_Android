package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeatureTutorialScalingInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun practiceWindowMatchesFeatureDpAndSpAcrossScalingChanges() {
        var displayDensity by mutableFloatStateOf(1.6f)
        var fontScale by mutableFloatStateOf(1.15f)
        var windowDensity: Density? = null
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(displayDensity, fontScale)) {
                    Column {
                        Box(Modifier.size(80.dp).testTag("feature.control"))
                        Text("Focus session", fontSize = 20.sp, modifier = Modifier.testTag("feature.text"))
                    }
                    FeatureTutorialPracticeWindow(
                        titleRes = R.string.ekagra_tutorial_learn,
                        practiceLabelRes = R.string.ekagra_tutorial_practice,
                        isDarkTheme = false, onExit = {},
                        practiceTag = "scaling.practice", labelTag = "scaling.label", closeTag = "scaling.close",
                    ) {
                        val density = LocalDensity.current
                        SideEffect { windowDensity = density }
                        Box(Modifier.size(80.dp).testTag("practice.control"))
                        Text("Focus session", fontSize = 20.sp, modifier = Modifier.testTag("practice.text"))
                    }
                }
            }
        }
        listOf(1.6f to 1.15f, 2.4f to 1.4f).forEach { (density, font) ->
            compose.runOnIdle { displayDensity = density; fontScale = font }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(density, windowDensity!!.density, 0.001f)
                assertEquals(font, windowDensity!!.fontScale, 0.001f)
            }
            listOf("control", "text").forEach { name ->
                val feature = compose.onNodeWithTag("feature.$name").fetchSemanticsNode().boundsInWindow
                val practice = compose.onNodeWithTag("practice.$name").fetchSemanticsNode().boundsInWindow
                assertEquals("$name width", feature.width, practice.width, 1f)
                assertEquals("$name height", feature.height, practice.height, 1f)
            }
        }
    }
}
