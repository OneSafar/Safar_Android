package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.viewmodel.HabitTab
import com.safarparmar.app.ui.tutorial.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/** Same forms, tabs, rows and grids as HabitScreen; all callbacks are local no-ops. */
@Composable
internal fun HabitPracticeStage(scene: PracticeScene) {
    val today = LocalDate.now(com.safarparmar.app.util.IstDateUtils.zone)
    val habit = HabitEntity(id = -1, name = stringResource(R.string.practice_sample_habit))
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val history = HabitWithCompletions(habit, (0L..27L).associate { today.minusDays(it) to (it % 3 != 1L) })
    Box(Modifier.fillMaxSize().background(HabitColors.Background)) {
        when (scene) {
            PracticeScene.HABIT_NAME, PracticeScene.HABIT_REPEAT -> HabitFormBottomSheet(
                onDismiss = {}, onSubmit = { _, _, _, _ -> }, inlinePreview = true, initialName = habit.name)
            else -> {
                val tab = when (scene) { PracticeScene.HABIT_WEEK -> HabitTab.WEEKLY; PracticeScene.HABIT_MONTH -> HabitTab.MONTHLY; else -> HabitTab.TODAY }
                Column(Modifier.fillMaxSize()) {
                    ModernTabs(tab, {}, modifier = Modifier.fillMaxWidth().padding(16.dp).tutorialTarget(
                        if (scene == PracticeScene.HABIT_WEEK || scene == PracticeScene.HABIT_MONTH) scene.target else "habits.tabs"))
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            HabitTab.WEEKLY -> WeeklyView(weekStart, listOf(history), { _, _, _ -> }, {}, {}, today = today)
                            HabitTab.MONTHLY -> MonthlyView(today.withDayOfMonth(1), listOf(history), { _, _, _ -> }, {}, {}, today = today)
                            else -> TodayView(today, listOf(HabitWithCompletion(habit, today, scene == PracticeScene.HABIT_CHECK)), listOf(habit), { _, _ -> })
                        }
                    }
                }
                ModernPrimaryFab({}, Modifier.align(Alignment.BottomEnd).padding(16.dp).tutorialTarget("habit.add"))
            }
        }
    }
}
