package com.safarparmar.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeToolLabelFitTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completeLabelsFitAtReportedDeviceDensityAndFontScale() {
        val labels = listOf("Ekagra", "Nishtha", "Mehfil", "Exam Planner", "Dhyan", "Study Circle", "KAVACH", "Leaderboard")
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(1.75f, 1.15f)) {
                    // Include inherited letter spacing that the previous measurement omitted.
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.5.sp)) {
                        Column(Modifier.width(((720f / 1.75f - 48f - 24f) / 4f).dp)) {
                            labels.forEach { FittedHomeToolLabel(it, labels, 12.sp, Color.Black) }
                        }
                    }
                }
            }
        }
        val sizes = mutableSetOf<Float>()
        labels.forEach { label ->
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertEquals(1, results.size)
            val layout = results.single()
            assertEquals(label, layout.layoutInput.text.text)
            assertEquals(1, layout.lineCount)
            assertEquals(label.length, layout.getLineEnd(0))
            assertFalse("Overflow: $label", layout.hasVisualOverflow)
            assertFalse(layout.isLineEllipsized(0))
            assertTrue(layout.getLineRight(0) - layout.getLineLeft(0) <= layout.size.width)
            sizes.add(layout.layoutInput.style.fontSize.value)
        }
        assertEquals("All eight labels share a font size", 1, sizes.size)
    }
}
