package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.studyplanner.*
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.*
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.tutorial.*
import java.time.LocalDate

/** Five bottom-tab overviews using native presentation controls and an isolated sample plan. */
@Composable
internal fun PlannerPracticeStage(scene: PracticeScene, dark: Boolean) {
    val today = LocalDate.now()
    val subject = StudySubject(id = "sample-subject", name = stringResource(R.string.practice_next_sample_subject), chapters = listOf(
        StudyChapter(id = "sample-chapter", name = stringResource(R.string.practice_next_sample_chapter), topics = listOf(
            StudyTopic(id = "sample-topic", name = stringResource(R.string.practice_next_sample_topic), plannedDate = today.toString()),
            StudyTopic(id = "sample-done", name = stringResource(R.string.practice_next_sample_chapter), status = TopicStatus.DONE),
            StudyTopic(id = "sample-3", name = stringResource(R.string.practice_next_sample_subject)),
            StudyTopic(id = "sample-4", name = stringResource(R.string.practice_next_sample_prompt))))))
    val plan = StudyPlan(id = "practice", title = stringResource(R.string.practice_next_sample_plan), examDate = today.plusDays(30).toString(), dailyGoal = 3, subjects = listOf(subject))
    val selected = when (scene) {
        PracticeScene.PLANNER_EXAMS -> PlannerSection.YOUR_EXAMS
        PracticeScene.PLANNER_SYLLABUS -> PlannerSection.SYLLABUS
        PracticeScene.PLANNER_CALENDAR -> PlannerSection.CALENDAR
        PracticeScene.PLANNER_PROGRESS -> PlannerSection.INSIGHTS
        else -> PlannerSection.PLAN
    }
    Column(Modifier.fillMaxSize().background(if (scene == PracticeScene.PLANNER_HOME) PlannerFlatColors.BgCream else com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground())) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (scene) {
                PracticeScene.PLANNER_EXAMS -> Column( verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.tutorialTarget(scene.target)) { PlannerEntryAction(stringResource(R.string.planner_new_plan), stringResource(R.string.practice_next_planner_new_subtitle), Icons.Default.Add, true, {}) }
                    PlannerTargetExamRow(plan = plan, planNumber = 1, isActive = true, isLight = !dark, onOpen = {}, onRename = {}, onDelete = {})
                }
                PracticeScene.PLANNER_HOME -> {
                    PlanHomeHeader(plan.title, {})
                    PlanHomeHero(plan, PlanProgress(totalTopics = 4, doneTopics = 1, completionPercent = 25))
                    Column {
                        PlanHomeTabs(StudyPlannerTab.TODAY, {})
                        Box(Modifier.tutorialTarget(scene.target)) { PlanHomeDailyTodoRow(1, 3, false, {}, expandedContent = {}) }
                        PlanHomeTaskRow(TopicRef(subject, subject.chapters.first(), subject.chapters.first().topics.first()), {})
                        PlanHomeAddActions({}, {})
                    }
                }
                PracticeScene.PLANNER_SYLLABUS -> {
                    SyllabusMagazineHeader(plan.title, 25, 1, 1, 4, 30, 1, {})
                    Column(Modifier.tutorialTarget(scene.target)) {
                        SyllabusMagazineListHeader(stringResource(R.string.practice_next_sample_subject), {})
                        SyllabusMagazineSubjectRow(subject, {}, {}, {}, {}, {}, false, {}, {}, {})
                    }
                }
                PracticeScene.PLANNER_CALENDAR -> {
                    CalendarPlainHeader(plan, !dark)
                    Box {
                        val month = java.time.YearMonth.from(today)
                        PlannerMonthGrid(month, monthCalendarSlots(month).chunked(7), androidx.compose.ui.platform.LocalLocale.current.platformLocale, today.toString(), today.toString(), plan, !dark,
                            dayItems = { emptyList() }, onPrevious = {}, onNext = {}, onDay = {})
                    }
                }
                PracticeScene.PLANNER_PROGRESS -> {
                    InsightsOverallProgressRedesign(25, 33, 1, 4, !dark)
                    Box(Modifier.tutorialTarget(scene.target)) { InsightsMetricSquares(30, 3, !dark) }
                }
                else -> Unit
            }
        }
        PlannerBottomBar(selected, {})
    }
}
