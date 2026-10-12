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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.composables.ui.theme.LocalColorScheme
import com.composables.ui.theme.ColorScheme
import java.io.File
import java.lang.reflect.Proxy
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

@RunWith(Parameterized::class)
class TodayPlanLayoutTest(private val width: Int, private val scale: Float, private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyPopulatedAndCompletedPlansFit() {
        val day = ToppersBatchViewModel.indiaDay()
        val initial = todayPlanFixture()
        val api = Proxy.newProxyInstance(ToppersBatchApi::class.java.classLoader, arrayOf(ToppersBatchApi::class.java)) { _, method, _ ->
            if (method.name == "overview") Response.success(initial) else error(method.name)
        } as ToppersBatchApi
        lateinit var current: MutableState<BatchUiState>
        compose.setContent {
            val vm = remember { ToppersBatchViewModel(ToppersBatchRepository(api)) }
            current = remember { mutableStateOf(BatchUiState(studyMode = BatchStudyMode.PERSONAL, gate = BatchGate.READY, overview = initial, today = initial.todayEvents(day))) }
            SafarTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.batchTrackerColors(dark)) {
                      CompositionLocalProvider(
                        LocalColorScheme provides if (dark) ColorScheme.Dark else ColorScheme.Light,
                        LocalSafarComposablesAccent provides MaterialTheme.colorScheme.primary,
                      ) { SafarComposablesTheme {
                        Surface(Modifier.requiredWidth(width.dp).height(1400.dp).testTag("today-layout")) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                ToppersBatchDashboard(current.value, vm)
                            }
                        }
                      } }
                    }
                }
            }
        }
        for (studyMode in BatchStudyMode.entries) {
            for (mode in listOf("empty", "populated", "completed")) {
                compose.runOnIdle {
                    val overview = if (mode == "empty") initial else initial.withLecture(initial.lectures.first().copy(
                        studyPlannedFor = day, completedAt = if (mode == "completed") Instant.now().toString() else null), day)
                    current.value = current.value.copy(studyMode = studyMode, overview = overview, today = overview.todayEvents(day))
                }
                compose.onNodeWithText(batchTestText(R.string.toppers_batch_your_plan_today)).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(batchTestText(R.string.toppers_batch_overall_batch_progress)).performScrollTo().assertIsDisplayed()
                for (choice in BatchStudyMode.entries) {
                    compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_about_count, batchTestText(choice.titleRes))).assertExists()
                }
                for (subject in current.value.overview!!.subjects) {
                    val progress = current.value.overview!!.progress.bySubject.firstOrNull { it.subjectId == subject.id }
                    compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_open_count_count_of_count_lectures_done,
                        subject.name, progress?.completed ?: 0, progress?.total ?: 0)).assertExists()
                }
                capture("${studyMode.name}-$mode")
                assertNoTextOverflow()
                if (studyMode == BatchStudyMode.PERSONAL) {
                    compose.onNodeWithText(batchTestText(R.string.toppers_batch_parmar_s_schedule)).assertDoesNotExist()
                    val add = batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), if (mode == "empty") 1 else 2)
                    if (compose.onAllNodesWithContentDescription(add).fetchSemanticsNodes().isEmpty())
                        compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
                    compose.onNodeWithContentDescription(add).performScrollTo().assertIsDisplayed()
                } else {
                    compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).assertDoesNotExist()
                    compose.onNodeWithText(batchTestText(R.string.toppers_batch_next_official_classes)).performScrollTo().assertIsDisplayed()
                }
                assertNoTextOverflow()
            }
        }
    }

    private fun assertNoTextOverflow() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
        for (index in nodes.fetchSemanticsNodes().indices) {
            val results = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            results.forEach { result ->
                val message = "Overflow at ${width}dp/${scale}x: ${result.layoutInput.text.text}; paragraph=${result.multiParagraph.width}x${result.multiParagraph.height}; box=${result.size}; constraints=${result.layoutInput.constraints}"
                // Paragraph metrics are fractional pixels; the measured IntSize can round down.
                assertFalse(message, result.multiParagraph.didExceedMaxLines)
                assertTrue(message, result.multiParagraph.height <= result.size.height + 1f)
                for (line in 0 until result.lineCount) {
                    assertFalse(message, result.isLineEllipsized(line))
                    assertTrue(message, result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1f)
                }
            }
        }
    }

    private fun capture(mode: String) {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "today-layout-$mode-${width}dp-${scale}x-dark$dark.png")
        file.outputStream().use { compose.onNodeWithTag("today-layout").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}dp/{1}x/dark={2}")
        fun sizes() = listOf(320, 375, 414, 768).flatMap { width ->
            listOf(false, true).flatMap { dark -> listOf(1f, 1.6f).map { scale -> arrayOf<Any>(width, scale, dark) } }
        }
    }
}
