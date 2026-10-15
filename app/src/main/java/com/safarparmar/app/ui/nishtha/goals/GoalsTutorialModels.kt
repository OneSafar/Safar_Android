package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.tutorial.FeatureTutorialState
import com.safarparmar.app.ui.tutorial.FeatureTutorialStep
import java.time.LocalDate

internal enum class GoalsTutorialScene(val target: String, val title: Int, val instruction: Int, val body: Int) {
    ADD("goals.add", R.string.goals_tutorial_add_title, R.string.goals_tutorial_add_instruction, R.string.goals_tutorial_add_body),
    TITLE("goals.title", R.string.goals_tutorial_title_title, R.string.goals_tutorial_title_instruction, R.string.goals_tutorial_title_body),
    CREATE("goals.create", R.string.goals_tutorial_create_title, R.string.goals_tutorial_create_instruction, R.string.goals_tutorial_create_body),
    TODAY("goals.row", R.string.goals_tutorial_today_title, R.string.goals_tutorial_today_instruction, R.string.goals_tutorial_today_body),
    CHECK("goals.complete", R.string.goals_tutorial_check_title, R.string.goals_tutorial_check_instruction, R.string.goals_tutorial_check_body),
    TIME("goals.studyTime", R.string.goals_tutorial_time_title, R.string.goals_tutorial_time_instruction, R.string.goals_tutorial_time_body),
    DONE("goals.doneActions", R.string.goals_tutorial_done_title, R.string.goals_tutorial_done_instruction, R.string.goals_tutorial_done_body),
    HISTORY("goals.historyGoal", R.string.goals_tutorial_history_title, R.string.goals_tutorial_history_instruction, R.string.goals_tutorial_history_body),
    SCHEDULE("goals.schedule", R.string.goals_tutorial_schedule_title, R.string.goals_tutorial_schedule_instruction, R.string.goals_tutorial_schedule_body),
    DATE("goals.date", R.string.goals_tutorial_date_title, R.string.goals_tutorial_date_instruction, R.string.goals_tutorial_date_body),
    UPCOMING("goals.row", R.string.goals_tutorial_upcoming_title, R.string.goals_tutorial_upcoming_instruction, R.string.goals_tutorial_upcoming_body),
    DAILY("goals.daily", R.string.goals_tutorial_daily_title, R.string.goals_tutorial_daily_instruction, R.string.goals_tutorial_daily_body),
    REPEAT("goals.repeatSelection", R.string.goals_tutorial_repeat_title, R.string.goals_tutorial_repeat_instruction, R.string.goals_tutorial_repeat_body),
    REPEAT_AUTO("goals.repeatAuto", R.string.goals_tutorial_repeat_auto_title, R.string.goals_tutorial_repeat_auto_instruction, R.string.goals_tutorial_repeat_auto_body),
    YESTERDAY("goals.repeatSelection", R.string.goals_tutorial_yesterday_title, R.string.goals_tutorial_yesterday_instruction, R.string.goals_tutorial_yesterday_body),
    MENU("goals.menu", R.string.goals_tutorial_menu_title, R.string.goals_tutorial_menu_instruction, R.string.goals_tutorial_menu_body),
    EDIT("goals.title", R.string.goals_tutorial_edit_title, R.string.goals_tutorial_edit_instruction, R.string.goals_tutorial_edit_body),
    DELETED("goals.restore", R.string.goals_tutorial_deleted_title, R.string.goals_tutorial_deleted_instruction, R.string.goals_tutorial_deleted_body),
    MISSED("goals.row", R.string.goals_tutorial_missed_title, R.string.goals_tutorial_missed_instruction, R.string.goals_tutorial_missed_body),
    MISSED_CHOICE("goals.missedActions", R.string.goals_tutorial_missed_choice_title, R.string.goals_tutorial_missed_choice_instruction, R.string.goals_tutorial_missed_choice_body),
    STATUS("goals.statusGrid", R.string.goals_tutorial_status_title, R.string.goals_tutorial_status_instruction, R.string.goals_tutorial_status_body),
    INSIGHTS("goals.insights", R.string.goals_tutorial_insights_title, R.string.goals_tutorial_insights_instruction, R.string.goals_tutorial_insights_body),
    LINKED_HISTORY("goals.historyGoal", R.string.goals_tutorial_linked_history_title, R.string.goals_tutorial_linked_history_instruction, R.string.goals_tutorial_linked_history_body),
}

internal enum class GoalsTutorialLesson(val title: Int, vararg val scenes: GoalsTutorialScene) {
    CORE(R.string.goals_tutorial_core, GoalsTutorialScene.ADD, GoalsTutorialScene.TITLE,
        GoalsTutorialScene.CHECK, GoalsTutorialScene.DONE, GoalsTutorialScene.HISTORY),
    SCHEDULE(R.string.goals_tutorial_schedule, GoalsTutorialScene.DATE, GoalsTutorialScene.UPCOMING,
        GoalsTutorialScene.DAILY, GoalsTutorialScene.REPEAT),
    MANAGE(R.string.goals_tutorial_manage, GoalsTutorialScene.MENU, GoalsTutorialScene.EDIT,
        GoalsTutorialScene.MISSED, GoalsTutorialScene.DELETED),
    PROGRESS(R.string.goals_tutorial_progress, GoalsTutorialScene.STATUS, GoalsTutorialScene.INSIGHTS,
        GoalsTutorialScene.LINKED_HISTORY);

    fun steps() = scenes.map { FeatureTutorialStep(it.name, it.target, it.title, it.body, it.instruction) }
}

/** Retained across configuration changes, intentionally closed in a newly created process. */
internal class GoalsTutorialViewModel : ViewModel() {
    var isOpen by mutableStateOf(false); private set
    var state by mutableStateOf<FeatureTutorialState?>(null); private set
    var lesson by mutableStateOf<GoalsTutorialLesson?>(null); private set
    var completedLesson by mutableStateOf<GoalsTutorialLesson?>(null); private set
    fun open() { isOpen = true; state = null; lesson = null }
    fun start(value: GoalsTutorialLesson) { isOpen = true; lesson = value; state = FeatureTutorialState(value.steps()) }
    fun complete() { completedLesson = lesson; lesson = null; state = null }
    fun acknowledgeCompletion() { completedLesson = null }
    fun close() { isOpen = false; state = null; lesson = null }
}

/** No repositories, timer services, or account data are available to demo scenes. */
internal data class GoalsTutorialDemoState(val scene: GoalsTutorialScene, val today: LocalDate) {
    val tab get() = when (scene) {
        GoalsTutorialScene.UPCOMING -> 1
        GoalsTutorialScene.MISSED, GoalsTutorialScene.MISSED_CHOICE -> 2
        GoalsTutorialScene.HISTORY, GoalsTutorialScene.LINKED_HISTORY -> 3
        else -> 0
    }
    val totalToday get() = if (scene == GoalsTutorialScene.ADD || tab == 1 || tab == 2) 0 else 1
    fun sampleGoal(title: String, details: String): Goal {
        val completed = scene in setOf(GoalsTutorialScene.HISTORY, GoalsTutorialScene.LINKED_HISTORY)
        val date = when (scene) {
            GoalsTutorialScene.UPCOMING -> today.plusDays(1)
            GoalsTutorialScene.MISSED, GoalsTutorialScene.MISSED_CHOICE -> today.minusDays(1)
            else -> today
        }
        return Goal(id = "practice-goal", title = title, description = details, scheduledDate = date.toString(),
            goalKind = if (scene == GoalsTutorialScene.UPCOMING) "scheduled" else "today",
            completed = completed, status = if (completed) "completed" else "not_started",
            completedAt = if (completed) today.toString() else null,
            studiedMinutes = if (completed && scene != GoalsTutorialScene.LINKED_HISTORY) 25 else 0,
            completedViaFocus = scene == GoalsTutorialScene.LINKED_HISTORY)
    }
}

internal fun goalsTutorialEligible(loading: Boolean, saving: Boolean, actionPending: Boolean) =
    !loading && !saving && !actionPending
