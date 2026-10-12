package com.safarparmar.app.feature.toppersbatch

import androidx.activity.compose.setContent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import com.safarparmar.app.MainActivity
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class ToppersBatchOpeningTest {
    private fun open(overview: BatchOverview, dark: Boolean = false) {
        val api = Proxy.newProxyInstance(ToppersBatchApi::class.java.classLoader,
            arrayOf(ToppersBatchApi::class.java)) { _, method, _ ->
            when (method.name) {
                "status" -> Response.success(BatchStatus(available = true, enabled = true))
                "overview" -> Response.success(overview)
                "today" -> Response.success(BatchToday())
                else -> error("Unexpected request: ${method.name}")
            }
        } as ToppersBatchApi
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
                activity.setContent {
                    SafarTheme(darkTheme = dark) {
                        ToppersBatchScreen(dark, {}, {}, {}, vm)
                    }
                }
            }
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
            var opened = false
            while (!opened && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.waitForIdleSync()
                opened = containsText(instrumentation.uiAutomation.rootInActiveWindow, batchTestText(com.safarparmar.app.R.string.toppers_batch_official_classes_today)) ||
                         containsText(instrumentation.uiAutomation.rootInActiveWindow, batchTestText(com.safarparmar.app.R.string.toppers_batch_your_plan_today))
                if (!opened) Thread.sleep(100)
            }
            assertTrue("The planner should render its dashboard", opened)
        }
    }

    private fun containsText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.text?.toString()?.contains(text) == true) return true
        if (node.contentDescription?.toString()?.contains(text) == true) return true
        return (0 until node.childCount).any { containsText(node.getChild(it), text) }
    }

    @Test fun opensWithNoLectures() = open(BatchOverview())

    @Test fun opensWithOfficialSyllabus() = open(syllabus())

    @Test fun opensInDarkMode() = open(syllabus(), dark = true)

    private fun syllabus() = BatchOverview(
        subjects = listOf(BatchSubject(id = "english", key = "english", name = "English")),
        lectures = listOf(BatchLecture(id = "lecture-1", subjectId = "english", subjectKey = "english",
            displayTopic = "Nouns", lectureNumber = 1)),
        progress = BatchProgress(bySubject = listOf(SubjectProgress("english", total = 1))),
        watchList = listOf(BatchSubjectWatch("english", "lecture-1")),
    )
}
