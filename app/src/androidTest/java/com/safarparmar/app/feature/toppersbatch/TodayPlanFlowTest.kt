package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONObject
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.MainActivity
import com.safarparmar.app.ui.theme.SafarTheme
import java.io.File
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class TodayPlanFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var vm: ToppersBatchViewModel
    private var server = todayPlanFixture()
    private var failSave = false

    private fun open(largeText: Boolean = false) {
        val api = Proxy.newProxyInstance(ToppersBatchApi::class.java.classLoader, arrayOf(ToppersBatchApi::class.java)) { _, method, args ->
            when (method.name) {
                "overview" -> Response.success(server)
                "editSubject" -> {
                    val id = args!![0] as String
                    val buffer = Buffer()
                    (args[1] as RequestBody).writeTo(buffer)
                    val body = JSONObject(buffer.readUtf8())
                    val updated = server.subjects.first { it.id == id }.copy(color = body.optString("color").takeUnless { it.isEmpty() || it == "null" })
                    server = server.copy(subjects = server.subjects.map { if (it.id == id) updated else it })
                    Response.success(updated)
                }
                "lectureAction" -> {
                    val id = args!![0] as String
                    val action = args[1] as String
                    @Suppress("UNCHECKED_CAST") val body = args[2] as Map<String, Any>
                    val row = server.lectures.first { it.id == id }
                    if (failSave && action == "plan") {
                        failSave = false
                        Response.error<LectureResult>(409, """{"code":"PLAN_CONFLICT","message":"Plan changed. Please try again."}""".toResponseBody("application/json".toMediaType()))
                    } else {
                        val updated = when (action) {
                            "plan" -> row.copy(studyPlannedFor = if (body["remove"] == true) null else (body["dates"] as List<*>).first() as String, planVersion = row.planVersion + 1)
                            "complete" -> row.copy(completedAt = Instant.now().toString())
                            "uncomplete" -> row.copy(completedAt = null)
                            else -> error("Unexpected action $action")
                        }
                        server = server.withLecture(updated, ToppersBatchViewModel.indiaDay())
                        Response.success(LectureResult(updated))
                    }
                }
                else -> error("Unexpected request ${method.name}")
            }
        } as ToppersBatchApi
        compose.activity.runOnUiThread {
            vm = ToppersBatchViewModel(ToppersBatchRepository(api))
            compose.activity.setContent {
                SafarTheme {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, if (largeText) 1.6f else density.fontScale),
                    ) {
                        Box(if (largeText) Modifier.requiredWidth(320.dp) else Modifier) {
                            ToppersBatchScreen(false, {}, {}, {}, vm)
                        }
                    }
                }
            }
        }
        compose.waitUntil(10_000) { ::vm.isInitialized && vm.state.value.gate == BatchGate.READY }
        compose.runOnIdle { vm.selectStudyMode(BatchStudyMode.PERSONAL) }
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_your_plan_today)).assertExists()
    }

    @Test fun addCompleteAndUndoFromToday() {
        open()
        capture("today-plan-empty")
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 1 }
        compose.onNodeWithText(batchTestQuantity(R.plurals.toppers_batch_count_of_count_tasks_done_count_left, 1, 0, 1, 1)).assertExists()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_watch_date)).assertDoesNotExist()
        capture("today-plan-populated")
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_complete_subject_lecture, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.busyIds.isEmpty() }
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_completed_in_your_plan)).assertExists()
        capture("today-plan-completed")
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_completed_in_your_plan)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_undo_subject_lecture, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.busyIds.isEmpty() }
        compose.onNodeWithText(batchTestQuantity(R.plurals.toppers_batch_count_of_count_tasks_done_count_left, 1, 0, 1, 1)).assertExists()
    }

    @Test fun failedSaveRetainsChosenFutureDateAndRetryReturnsToToday() {
        open()
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 1 }
        failSave = true
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_more_actions_for_count, "Nouns")).performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_choose_date)).performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_tomorrow)).performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_save)).performClick()
        compose.onAllNodesWithText(batchTestText(R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again)).onLast().assertIsDisplayed()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_watch_date)).assertExists()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_lecture_details)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_save)).performClick()
        val tomorrow = LocalDate.parse(ToppersBatchViewModel.indiaDay()).plusDays(1).toString()
        compose.waitUntil(10_000) { vm.state.value.overview?.lectures?.first()?.studyPlannedFor == tomorrow }
        assertEquals(tomorrow, vm.state.value.overview!!.lectures.first().studyPlannedFor)
        assertEquals(0, vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total)
        compose.onNodeWithTag("toppers-batch-body").performScrollToIndex(0)
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_lecture_planned_for_count, BatchDateArgument(tomorrow))).assertExists()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_your_plan_today)).assertExists()
    }

    @Test fun completedUnplannedLectureHasNoEmptyWatchDateAndUnfinishedDateIsActionable() {
        val day = ToppersBatchViewModel.indiaDay()
        server = server.withLecture(server.lectures.first().copy(completedAt = Instant.now().toString(),
            revisionDate = day, revisionSessions = listOf(ReviewSession(day))), day)
        open()
        compose.onNodeWithText("Nouns - Lecture 01").performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_completed_on)).assertExists()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_watch_on)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_planned_watch)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_not_set)).assertDoesNotExist()
        compose.onNodeWithText("Your study date").assertDoesNotExist()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_close_lecture_details)).performClick()
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 2)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 2)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview?.lectures?.last()?.studyPlannedFor != null }
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_watch_date)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_your_plan_today)).assertExists()
    }

    @Test fun switchingToOfficialHidesWatchDatesAndRetainsThePersonalPlan() {
        open()
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 1 }
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_official_schedule)).performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_official_classes_today)).assertExists()
        compose.onNodeWithText("Nouns - Lecture 01").performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_watch_on)).assertDoesNotExist()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_planned_watch)).assertDoesNotExist()
        compose.onAllNodesWithText(batchTestText(R.string.toppers_batch_official_schedule)).onLast().assertExists()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_close_lecture_details)).performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_complete_subject_lecture, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.busyIds.isEmpty() }
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_my_own_pace)).performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_completed_in_your_plan)).assertExists()
        assertEquals(1, vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total)
    }

    @Test fun modeHelpSubjectCardsAndRemoveRemainAccessible() {
        open()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_overall_batch_progress)).performScrollTo().assertIsDisplayed()
        for (mode in BatchStudyMode.entries) {
            compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_about_count, batchTestText(mode.titleRes))).performScrollTo().performClick()
            compose.onNodeWithText(batchTestText(R.string.toppers_batch_switching_modes_keeps_your_saved_plans_and_completion_history)).assertIsDisplayed()
            compose.onNodeWithText(batchTestText(R.string.toppers_batch_got_it)).performClick()
        }
        for (subject in server.subjects) {
            val progress = server.progress.bySubject.firstOrNull { it.subjectId == subject.id }
            compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_open_count_count_of_count_lectures_done,
                subject.name, progress?.completed ?: 0, progress?.total ?: 0)).performScrollTo().assertIsDisplayed()
        }
        assertEquals(BatchStudyMode.PERSONAL, vm.state.value.studyMode)
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 1 }
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_more_actions_for_count, "Nouns")).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_remove_subject_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 0 }
        if (compose.onAllNodesWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).fetchSemanticsNodes().isEmpty()) compose.onNodeWithText(batchTestText(R.string.toppers_batch_add_lectures_to_today)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_add_count_lecture_count_to_today, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total == 1 }
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_complete_subject_lecture, batchTestText(R.string.toppers_batch_english), 1)).performScrollTo().performClick()
        compose.onAllNodesWithText(batchTestText(R.string.toppers_batch_undo)).onLast().performClick()
        compose.waitUntil(10_000) { vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).completed == 0 }
        assertEquals(1, vm.state.value.overview!!.todayPlan(ToppersBatchViewModel.indiaDay()).total)
    }

    @Test fun subjectCardsOpenTheirContentsInBothModes() {
        open()
        for (mode in BatchStudyMode.entries) {
            compose.onNodeWithText(batchTestText(mode.titleRes)).performScrollTo().performClick()
            for (subject in server.subjects) {
                val progress = server.progress.bySubject.firstOrNull { it.subjectId == subject.id }
                compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_open_count_count_of_count_lectures_done,
                    subject.name, progress?.completed ?: 0, progress?.total ?: 0)).performScrollTo().performClick()
                compose.onNodeWithText(batchTestText(R.string.toppers_batch_today_count, subject.name)).assertExists()
                compose.onNodeWithText(batchTestText(R.string.toppers_batch_your_progress)).assertExists()
                compose.onNodeWithText(batchTestText(R.string.toppers_batch_view_all_count_lectures, subject.name)).performScrollTo().assertIsDisplayed()
                compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_back_to_today)).performClick()
            }
        }
    }

    @Test fun savedSubjectColourUpdatesBothModesAndSurvivesRefresh() {
        open()
        compose.onNodeWithTag("toppers-batch-body").performScrollToIndex(0)
        capture("restored-today-colours")
        val english = server.subjects.first()
        val blue = "#1D4ED8"
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_more_actions_for_count, english.name)).performScrollTo().performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_change_colour)).performClick()
        compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_count_color, batchTestText(R.string.toppers_batch_blue))).performClick()
        compose.onNodeWithText(batchTestText(R.string.toppers_batch_save)).performClick()
        compose.waitUntil(10_000) { vm.state.value.busyIds.isEmpty() && vm.state.value.overview?.subjects?.first()?.color == blue }
        assertEquals(blue, server.subjects.first().color)
        vm.refresh()
        compose.waitUntil(10_000) { vm.state.value.gate == BatchGate.READY }
        for (mode in BatchStudyMode.entries) {
            compose.onNodeWithText(batchTestText(mode.titleRes)).performScrollTo().performClick()
            val card = compose.onNodeWithContentDescription(batchTestText(R.string.toppers_batch_open_count_count_of_count_lectures_done, english.name, 0, 2)).performScrollTo()
            val bitmap = card.captureToImage().asAndroidBitmap()
            assertEquals(android.graphics.Color.parseColor(blue), bitmap.getPixel(bitmap.width / 2, bitmap.height - 10))
            val overview = vm.state.value.overview!!
            assertEquals(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(blue)), subjectProgressColor(overview.subjects.first()))
            overview.calendarEvents().filter { it.subject?.id == english.id }.forEach {
                assertEquals(blue, it.subject!!.color)
            }
        }
    }

    @Test fun fullTrackerTitleWrapsWithoutTruncation() {
        checkFullTitle(largeText = false)
    }

    @Test fun fullTrackerTitleWrapsWithLargeText() {
        checkFullTitle(largeText = true)
    }

    private fun checkFullTitle(largeText: Boolean) {
        val title = "Parmar's Toppers Batch Tracker"
        server = server.copy(course = server.course.copy(name = title))
        open(largeText)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(title).assertIsDisplayed().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
        ) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse("Title exceeds max lines: $layout", layout.multiParagraph.didExceedMaxLines)
            assertTrue("Title height ${layout.multiParagraph.height} exceeds ${layout.size.height}", layout.multiParagraph.height <= layout.size.height + 1f)
            for (line in 0 until layout.lineCount) {
                assertFalse(layout.isLineEllipsized(line))
                assertTrue(layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f)
            }
            if (largeText) assertTrue("Large text should wrap", layout.lineCount > 1)
        }
        capture(if (largeText) "tracker-full-title-large-text" else "tracker-full-title")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        instrumentation.uiAutomation.takeScreenshot()!!.let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}

internal fun todayPlanFixture(): BatchOverview {
    val subjects = listOf(BatchSubject(id = "english", key = "english", name = "English", defaultColor = "#E11D48"),
        BatchSubject(id = "reasoning", key = "reasoning", name = "Reasoning", defaultColor = "#7C3AED"),
        BatchSubject(id = "mathematics", key = "mathematics", name = "Maths", defaultColor = "#F59E0B"),
        BatchSubject(id = "gk", key = "gk", name = "GK/GS", defaultColor = "#16A34A"))
    val rows = subjects.mapIndexed { index, subject -> BatchLecture(id = "${subject.id}-1", subjectId = subject.id,
        subjectKey = subject.key, lectureNumber = 1, order = 1, isAvailable = true,
        scheduledFor = ToppersBatchViewModel.indiaDay(), displayTopic = listOf("Nouns", "Analogy", "Number system", "History")[index]) } +
        BatchLecture(id = "english-2", subjectId = "english", subjectKey = "english", lectureNumber = 2, order = 2, isAvailable = true, displayTopic = "Pronouns")
    return BatchOverview(subjects = subjects, lectures = rows).withLecture(rows.first(), ToppersBatchViewModel.indiaDay())
}
