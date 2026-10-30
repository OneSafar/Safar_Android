package com.safarparmar.app.ui.studyplanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.*
import com.safarparmar.app.ui.studyplanner.components.PlannerWarmClarityTheme
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerWarmClarity
import com.safarparmar.app.ui.studyplanner.create.steps.PlanSettingsStep
import com.safarparmar.app.ui.studyplanner.logic.PlannerInsightSubjectRow
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.plan.PlanHomeTaskRow
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
import com.safarparmar.app.ui.studyplanner.screens.CalendarDayChip
import com.safarparmar.app.ui.studyplanner.screens.SubjectProgressChart
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

/** Presentation regressions use local samples/callbacks; no real services or repositories. */
class PlannerWarmClarityInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private fun actions(calls: MutableList<String>): PlannerActions = Proxy.newProxyInstance(
        PlannerActions::class.java.classLoader, arrayOf(PlannerActions::class.java),
    ) { _, method, _ -> calls += method.name; if (method.returnType == Boolean::class.javaPrimitiveType) false else null } as PlannerActions

    @Test fun focusAndCompletionInvokeOnlyTheirPresentationCallbacks() {
        var focus = 0; var completion: Boolean? = null
        val topic = StudyTopic(id = "t", name = "Percentage")
        val chapter = StudyChapter(id = "c", name = "Arithmetic", topics = listOf(topic))
        val subject = StudySubject(id = "s", name = "Maths", chapters = listOf(chapter))
        compose.setContent { SafarTheme { PlannerWarmClarityTheme(false) {
            PlanHomeTaskRow(TopicRef(subject, chapter, topic), { completion = it }, onFocus = { focus++ })
        } } }
        compose.onNodeWithText("Focus").assertIsDisplayed().performClick()
        compose.onNode(isToggleable()).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, focus); assertEquals(true, completion) }
    }

    @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
    @Test fun completedTopicsBeyondFifteenRemainReachableWithoutWrites() {
        val calls = mutableListOf<String>()
        val topics = (1..20).map { StudyTopic(id = "t$it", name = "Completed topic $it", status = TopicStatus.DONE) }
        val plan = StudyPlan(id = "sample", title = "Sample exam", subjects = listOf(StudySubject(id = "s", name = "Maths", chapters = listOf(StudyChapter(id = "c", name = "Chapter", topics = topics)))))
        compose.setContent { SafarTheme { PlannerWarmClarityTheme(false) {
            PlanTabScreen(plan = plan, actions = actions(calls), activeTab = StudyPlannerTab.COMPLETED, onNavigate = {})
        } } }
        compose.onNodeWithText("Completed topic 20").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertTrue(calls.isEmpty()) }
    }

    @Test fun everySubjectIsVisibleAndCanShowItsDetails() {
        val subjects = (1..6).map { PlannerInsightSubjectRow("s$it", "Subject $it", "#542347", it * 10, 10, 1, 2) }
        compose.setContent { SafarTheme { PlannerWarmClarityTheme(false) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { SubjectProgressChart(subjects) }
        } } }
        compose.onNodeWithText("Subject 6").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("10 left · 1 missed · 2 to revise").performScrollTo().assertIsDisplayed()
    }

    @Test fun calendarAnnouncesWorkloadAndRestDaysAndRetainsClick() {
        var clicks = 0
        compose.setContent { SafarTheme { PlannerWarmClarityTheme(false) {
            CalendarDayChip(dateIso = "2026-10-23", items = listOf(CalendarTopicItem(topicId = "t")), selected = true,
                isToday = false, isOff = true, onClick = { clicks++ }, modifier = Modifier.size(56.dp))
        } } }
        compose.onNode(hasContentDescription("1 topics planned", substring = true)).assertIsDisplayed().performClick()
        compose.onNode(hasContentDescription("Rest", substring = true)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun changingStudyStyleDoesNotAutomaticallyNavigate() {
        var style by mutableStateOf("balanced"); var navigation = 0
        compose.setContent { SafarTheme { PlannerWarmClarityTheme(false) {
            PlanSettingsStep(examDate = "2026-12-18", onExamDateChange = {}, offDays = emptySet(), onToggleOffDay = {},
                studyStyle = style, onStudyStyleChange = { style = it }, dailyGoal = "6", onDailyGoalChange = {},
                topicCount = 20, subjectCount = 3, error = null, premiumRequired = false, onBuildPlan = {},
                onOpenDeepFocusOrder = { navigation++ }, onOpenMixedBagPicker = { navigation++ })
        } } }
        compose.onNodeWithText("Deep Focus").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("deep_focus", style); assertEquals(0, navigation) }
        compose.onNodeWithText("Customize order").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, navigation) }
    }

    @Test fun themeDoesNotLeakIntoOtherFeatures() {
        var before: Boolean? = null; var inside: Boolean? = null; var after: Boolean? = null
        compose.setContent { SafarTheme {
            before = LocalPlannerWarmClarity.current
            PlannerWarmClarityTheme(false) { inside = LocalPlannerWarmClarity.current }
            after = LocalPlannerWarmClarity.current
        } }
        compose.runOnIdle { assertEquals(false, before); assertEquals(true, inside); assertEquals(false, after) }
    }
}
