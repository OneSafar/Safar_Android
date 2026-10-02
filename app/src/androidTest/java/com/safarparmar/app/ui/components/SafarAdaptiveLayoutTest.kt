package com.safarparmar.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.composables.ui.components.Text
import com.safarparmar.app.R
import com.safarparmar.app.ui.profile.AccountStatusRow
import com.safarparmar.app.ui.profile.ActionsRow
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SafarAdaptiveLayoutTest(private val width: Int, private val scale: Float, private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                SafarTheme(darkTheme = dark) {
                    Column(Modifier.requiredWidth(width.dp).verticalScroll(rememberScrollState())) { content() }
                }
            }
        }
    }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun assertReadable(text: String, singleLine: Boolean = false) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Missing text layout for $text", results.isNotEmpty())
        results.forEach {
            // Unstyled Text can shrink its measured width after paragraph layout;
            // compare actual glyph-line extents rather than paragraph width.
            for (line in 0 until it.lineCount) {
                assertFalse("Ellipsized text: $text", it.isLineEllipsized(line))
                assertTrue("Clipped line: $text", it.getLineRight(line) - it.getLineLeft(line) <= it.size.width + 1)
                assertTrue("Clipped height: $text", it.getLineBottom(line) <= it.size.height + 1)
            }
            if (singleLine) assertEquals("Button label squeezed: $text", 1, it.lineCount)
        }
    }

    @Test fun profileActionsKeepLabelsReadableAndClicksWork() {
        var deleted = false
        render { ActionsRow({}, { deleted = true }) }
        assertReadable(label(R.string.profile_logout), singleLine = true)
        assertReadable(label(R.string.profile_delete_account), singleLine = true)
        val logout = compose.onNodeWithText(label(R.string.profile_logout)).fetchSemanticsNode().boundsInRoot
        val delete = compose.onNodeWithText(label(R.string.profile_delete_account)).fetchSemanticsNode().boundsInRoot
        assertFalse("Actions overlap", logout.overlaps(delete))
        compose.onNodeWithText(label(R.string.profile_delete_account)).performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }

    @Test fun subscriptionDetailsAreNotSqueezedByAction() {
        render { AccountStatusRow(true) }
        assertReadable(label(R.string.profile_safar_premium))
        assertReadable(label(R.string.profile_premium_active))
        assertReadable(label(R.string.profile_manage_plan), singleLine = true)
    }

    @Test fun bottomBarReservesSpaceAtEveryFontSize() {
        render {
            SafarContentWithBottomBar(Modifier.height(420.dp), bottomBar = {
                Text("YouTube Focus", modifier = Modifier.testTag("bar"))
            }) { modifier -> Box(modifier.testTag("content")) }
        }
        val content = compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        val bar = compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        assertTrue("Bottom bar covers content", content.bottom <= bar.top)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0}, scale={1}, dark={2}")
        fun sizes() = listOf(arrayOf<Any>(320, 1f, false), arrayOf<Any>(320, 1.5f, true),
            arrayOf<Any>(360, 2f, false), arrayOf<Any>(600, 1f, true))
    }
}
