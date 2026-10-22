package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.time.YearMonth

@RunWith(Parameterized::class)
class BatchCalendarLayoutTest(private val width: Int, private val scale: Float, private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Test fun calendarReflowsAndPreservesActions() {
        var opened = false
        compose.setContent {
            var state by remember { mutableStateOf(calendarPreviewState()) }
            SafarTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.batchTrackerColors(dark)) {
                        Surface(Modifier.requiredWidth(width.dp).height(1400.dp).testTag("calendar-capture")) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                BatchStudentCalendarContent(state,
                                    onDay = { state = state.copy(selectedDay = it) },
                                    onMonth = { delta -> val month = YearMonth.parse(state.month).plusMonths(delta); state = state.copy(month = month.toString(), selectedDay = month.atDay(1).toString()) },
                                    onToday = { state = state.copy(month = "2026-10", selectedDay = "2026-10-04") },
                                    onJump = { state = state.copy(month = it.take(7), selectedDay = it) },
                                    onLecture = { opened = true }, today = "2026-10-04")
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("calendar-today").assertIsDisplayed()
        val dateTile = compose.onNodeWithTag("calendar-date-2026-10-04").assertIsDisplayed().fetchSemanticsNode()
        assertTrue("Date tiles must not stretch horizontally", dateTile.size.width <= 52)
        assertTrue("Date tile height stays compact at every text scale", dateTile.size.height in 56..59)
        compose.onNodeWithTag("calendar-date-2026-10-05").assertIsDisplayed()
        val month = compose.onNodeWithTag("calendar-month").fetchSemanticsNode()
        assertTrue("Month card must not stretch horizontally", month.size.width <= 400)
        assertTrue("Month card must not stretch vertically", month.size.height <= 600)
        compose.onAllNodes(SemanticsMatcher("date tile") { it.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)?.startsWith("calendar-date-") == true }).fetchSemanticsNodes().forEach { tile ->
            assertTrue("Every date must fit inside the calendar", tile.boundsInRoot.left >= month.boundsInRoot.left && tile.boundsInRoot.right <= month.boundsInRoot.right)
        }
        capture("month", "calendar-month")
        assertNoTextOverflow()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_next_month)).performClick()
        compose.onNodeWithText("November").assertExists()
        compose.onNodeWithTag("calendar-today").performClick()
        compose.onNodeWithText("October").assertExists()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_list), useUnmergedTree = true).performClick()
        compose.onNodeWithText("Mon, 5 Oct").performClick()
        compose.onNodeWithTag("calendar-event-REVISION-mathematics-1").assertDoesNotExist()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_expand_count, batchTestText(BatchEventKind.REVISION.titleRes))).performScrollTo().performClick()
        compose.onNodeWithTag("calendar-event-REVISION-mathematics-1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_collapse_count, batchTestText(BatchEventKind.REVISION.titleRes))).performScrollTo().performClick()
        compose.onNodeWithTag("calendar-event-REVISION-mathematics-1").assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_month), useUnmergedTree = true).performClick()
        compose.onNodeWithTag("calendar-event-CLASS-mathematics-2").assertDoesNotExist()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_expand_count, batchTestText(BatchEventKind.CLASS.titleRes))).performScrollTo().performClick()
        compose.onNodeWithTag("calendar-event-CLASS-mathematics-2").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithTag("calendar-next-gk").assertDoesNotExist()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_expand_count, batchTestText(R.string.toppers_batch_next_classes))).performScrollTo().performClick()
        if (compose.onAllNodesWithTag("calendar-next-gk").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("calendar-next-gk").performScrollTo()
        } else {
            compose.onNodeWithText(batchTestText(R.string.toppers_batch_no_more_classes_scheduled_today)).performScrollTo().assertIsDisplayed()
        }
        capture("next-classes")
        assertNoTextOverflow()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_collapse_count, batchTestText(R.string.toppers_batch_next_classes))).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_collapse_count, batchTestText(BatchEventKind.CLASS.titleRes))).performScrollTo().performClick()
        compose.onNodeWithTag("calendar-event-CLASS-mathematics-2").assertDoesNotExist()
        capture("collapsed-sections")
    }

    private fun assertNoTextOverflow() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
        for (index in 0 until nodes.fetchSemanticsNodes().size) {
            val results = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            results.forEach { layout ->
                // Paragraph width can retain its available width after Text measures to its glyphs.
                // Check the actual line bounds, ellipsis and height rather than that loose width.
                val clipped = layout.multiParagraph.didExceedMaxLines ||
                    layout.multiParagraph.height > layout.size.height + 1f ||
                    (0 until layout.lineCount).any { line -> layout.isLineEllipsized(line) ||
                        layout.getLineRight(line) > layout.size.width + 1f || layout.getLineLeft(line) < -1f }
                assertFalse("Text is cut off at ${width}dp / ${scale}x: ${layout.layoutInput.text}", clipped)
            }
        }
    }
    private fun capture(part: String, tag: String = "calendar-capture") {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "calendar-layout").apply { mkdirs() }
        val file = File(directory, "${width}dp-${scale}x-${if (dark) "dark" else "light"}-$part.png")
        file.outputStream().use { compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp/{1}x/dark={2}")
        fun sizes() = listOf(arrayOf<Any>(320, 1f, false), arrayOf<Any>(375, 1f, false), arrayOf<Any>(414, 1f, false),
            arrayOf<Any>(768, 1f, false), arrayOf<Any>(1000, 1f, false), arrayOf<Any>(320, 1.6f, false),
            arrayOf<Any>(320, 2f, false), arrayOf<Any>(375, 1f, true))
    }
}
