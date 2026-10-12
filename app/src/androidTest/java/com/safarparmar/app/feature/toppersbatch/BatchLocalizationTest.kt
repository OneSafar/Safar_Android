package com.safarparmar.app.feature.toppersbatch

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.*
import com.composables.ui.theme.LocalColorScheme
import com.composables.ui.theme.ColorScheme
import java.lang.reflect.Proxy
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

@RunWith(Parameterized::class)
class BatchLocalizationTest(private val tag: String, private val wide: Boolean) {
    @get:Rule val compose = createComposeRule()
    private fun strings(language: String = tag): BatchStrings {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        return BatchStrings(context.createConfigurationContext(configuration).resources)
    }

    @Test fun resourcesFormatsAndEnglishOverridePolicy() {
        val local = strings()
        val fallback = local.text(R.string.toppers_batch_continue_learning)
        val content = BatchOverview(content = BatchTrackerContent(mapOf("todayTitle" to "Remote English label")))
        assertEquals(if (tag == "en") "Remote English label" else fallback, local.copy(content, "todayTitle", fallback))
        assertEquals(fallback, local.copy(BatchOverview(), "todayTitle", fallback))
        assertEquals("My personal English", local.subject(BatchSubject(key = "english", name = "My personal English")))
        assertEquals("2026-05-06", calendarDate("2026-05-06"))
        val formatted = local.date("2026-05-06")
        assertTrue(formatted, formatted.contains(if (tag == "hi") "मई" else "May"))
        assertEquals(if (tag == "hi") "hi" else "en", local.locale.language)
        for (count in listOf(0, 1, 5)) {
            val text = local.quantity(R.plurals.toppers_batch_count_lectures_finished, count, count)
            assertTrue(text, text.contains(count.toString()))
            assertFalse(text, text.contains("%1\$s"))
        }
        val retained = BatchNotice(R.string.toppers_batch_lecture_planned_for_count, BatchDateArgument("2026-05-06"))
        assertNotEquals(strings("en").notice(retained), strings("hi").notice(retained))
        assertNotEquals(strings("hi").notice(retained), strings("hi-Latn").notice(retained))
    }

    @Test fun screensDialogsAndLanguageSwitchKeepExistingState() {
        val day = ToppersBatchViewModel.indiaDay()
        val initial = todayPlanFixture().copy(studyPlan = BatchStudyPlan(startDate = day, targetDate = LocalDate.parse(day).plusDays(30).toString()),
            course = BatchCourse(officialStartDate = day))
        val api = Proxy.newProxyInstance(ToppersBatchApi::class.java.classLoader, arrayOf(ToppersBatchApi::class.java)) { _, method, _ ->
            if (method.name == "overview") Response.success(initial) else error(method.name)
        } as ToppersBatchApi
        lateinit var page: MutableState<Int>
        lateinit var language: MutableState<String>
        lateinit var vm: ToppersBatchViewModel
        val local = strings()
        compose.setContent {
            val base = LocalContext.current
            val registryOwner = requireNotNull(LocalActivityResultRegistryOwner.current)
            page = remember { mutableStateOf(0) }
            language = remember { mutableStateOf(tag) }
            vm = remember { ToppersBatchViewModel(ToppersBatchRepository(api)) }
            val configuration = remember(language.value) { Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.value)) } }
            val context = remember(configuration) { android.view.ContextThemeWrapper(base, 0).apply { applyOverrideConfiguration(configuration) } }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner, LocalContext provides context, LocalConfiguration provides configuration,
                LocalDensity provides Density(if (wide) 1f else LocalDensity.current.density, if (wide) 1.6f else 1f)) {
                SafarTheme(darkTheme = wide) {
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.batchTrackerColors(wide)) {
                        CompositionLocalProvider(LocalColorScheme provides if (wide) ColorScheme.Dark else ColorScheme.Light,
                            LocalSafarComposablesAccent provides MaterialTheme.colorScheme.primary) {
                            SafarComposablesTheme {
                                val state = BatchUiState(gate = BatchGate.READY, overview = initial, studyDay = day,
                                    studyMode = BatchStudyMode.PERSONAL, selectedSubjectId = "english", month = day.take(7), selectedDay = day)
                                Surface(Modifier.requiredWidth(if (wide) 768.dp else 320.dp).fillMaxHeight().testTag("localized-screen")) {
                                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                        when (page.value) {
                                            0 -> ToppersBatchDashboard(state, vm)
                                            1 -> BatchWeeklyAgenda(state, vm)
                                            2 -> BatchStudentCalendar(state, vm)
                                            3 -> {
                                                BatchReleaseProgressPanel(state, vm)
                                                CompletionTrendChart(initial.lectures, LocalDate.parse(day))
                                                RecentActivityCalendar(initial.lectures, LocalDate.parse(day))
                                            }
                                        }
                                    }
                                }
                                when (page.value) {
                                    4 -> StudyPlanDialog(initial.lectures.first(), state, vm) {}
                                    5 -> PriorProgressDialog(state, vm) {}
                                    6 -> ReschedulePlansDialog(state, vm) {}
                                    7 -> LectureDetailsDialog(initial.lectures.first(), initial.subjects.first(), state, vm) {}
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(local.text(R.string.toppers_batch_your_plan_today)).performScrollTo().assertIsDisplayed()
        capture("today")
        assertNoTextOverflow()
        // Change configuration without constructing a new ViewModel or losing overview.
        val changedTag = if (tag == "hi") "hi-Latn" else "hi"
        val before = vm
        compose.runOnIdle { language.value = changedTag }
        compose.onNodeWithText(strings(changedTag).text(R.string.toppers_batch_your_plan_today)).performScrollTo().assertIsDisplayed()
        assertSame(before, vm)
        compose.runOnIdle { language.value = tag; page.value = 1 }
        compose.onNodeWithContentDescription(local.text(R.string.toppers_batch_choose_subject_count, local.subject(initial.subjects.first()))).assertExists()
        capture("library")
        assertNoTextOverflow()
        compose.onNodeWithText("Nouns").assertExists() // official catalogue content stays as supplied
        compose.runOnIdle { page.value = 2 }
        compose.onNodeWithTag("calendar-today").assertExists()
        compose.onNodeWithText(local.text(R.string.toppers_batch_next_classes)).assertExists()
        capture("calendar")
        assertNoTextOverflow()
        compose.runOnIdle { page.value = 3 }
        compose.onNodeWithText(local.text(R.string.toppers_batch_your_progress_vs_batch)).assertExists()
        capture("progress")
        assertNoTextOverflow()
        compose.runOnIdle { page.value = 4 }
        compose.onNodeWithText(local.text(R.string.toppers_batch_watch_date)).assertExists()
        compose.onNodeWithText(local.text(R.string.toppers_batch_add_reminder_optional)).assertExists()
        capture("watch-plan")
        assertNoTextOverflow()
        compose.runOnIdle { page.value = 5 }
        compose.onNodeWithText(local.text(R.string.toppers_batch_previously_watched)).assertExists()
        compose.onNodeWithContentDescription(local.text(R.string.toppers_batch_about_previously_watched_lectures)).assertExists()
        capture("prior-progress")
        assertNoTextOverflow()
        compose.runOnIdle { page.value = 6 }
        compose.onNodeWithText(local.text(R.string.toppers_batch_reschedule_missed_plans)).assertExists()
        capture("reschedule")
        assertNoTextOverflow()
        compose.runOnIdle { page.value = 7 }
        compose.onNodeWithContentDescription(local.text(R.string.toppers_batch_close_lecture_details)).assertExists()
        compose.onAllNodesWithText("Nouns").onFirst().assertExists()
        capture("lecture-details")
        assertNoTextOverflow()
    }

    private fun assertNoTextOverflow() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
        for (index in nodes.fetchSemanticsNodes().indices) {
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            for (layout in layouts) {
                assertFalse("$tag/$wide: ${layout.layoutInput.text}", layout.multiParagraph.didExceedMaxLines)
                for (line in 0 until layout.lineCount) assertFalse("$tag/$wide: ${layout.layoutInput.text}", layout.isLineEllipsized(line))
            }
        }
    }

    private fun capture(stage: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for (command in listOf("mkdir -p /sdcard/Download/toppers-translations",
            "screencap -p /sdcard/Download/toppers-translations/translation-$tag-wide$wide-$stage.png")) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}/wide-dark-large={1}")
        fun cases() = listOf("en", "hi", "hi-Latn").flatMap { tag -> listOf(false, true).map { arrayOf<Any>(tag, it) } }
    }
}
