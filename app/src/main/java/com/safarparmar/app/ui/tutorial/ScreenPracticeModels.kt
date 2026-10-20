package com.safarparmar.app.ui.tutorial

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.safarparmar.app.R

internal enum class PracticeScene(val target: String, val title: Int, val instruction: Int, val body: Int) {
    BATCH_SUBJECT("batch.subject", R.string.practice_batch_subject_title, R.string.practice_batch_subject_instruction, R.string.practice_batch_subject_body),
    BATCH_LECTURE("batch.lecture", R.string.practice_batch_lecture_title, R.string.practice_batch_lecture_instruction, R.string.practice_batch_lecture_body),
    BATCH_DONE("batch.done", R.string.practice_batch_done_title, R.string.practice_batch_done_instruction, R.string.practice_batch_done_body),
    BATCH_REVISION("batch.revision", R.string.practice_batch_revision_title, R.string.practice_batch_revision_instruction, R.string.practice_batch_revision_body),
    BATCH_MODES("batch.modes", R.string.practice_batch_modes_title, R.string.practice_batch_modes_instruction, R.string.practice_batch_modes_body),
    BATCH_CALENDAR("batch.calendar", R.string.practice_batch_calendar_title, R.string.practice_batch_calendar_instruction, R.string.practice_batch_calendar_body),
    BATCH_PROGRESS("batch.progress", R.string.practice_batch_progress_title, R.string.practice_batch_progress_instruction, R.string.practice_batch_progress_body),
    HABIT_ADD("habit.add", R.string.practice_habit_add_title, R.string.practice_habit_add_instruction, R.string.practice_habit_add_body),
    HABIT_NAME("habits.name", R.string.practice_habit_name_title, R.string.practice_habit_name_instruction, R.string.practice_habit_name_body),
    HABIT_REPEAT("habits.repeat", R.string.practice_habit_repeat_title, R.string.practice_habit_repeat_instruction, R.string.practice_habit_repeat_body),
    HABIT_CHECK("habit.check", R.string.practice_habit_check_title, R.string.practice_habit_check_instruction, R.string.practice_habit_check_body),
    HABIT_WEEK("habit.week", R.string.practice_habit_week_title, R.string.practice_habit_week_instruction, R.string.practice_habit_week_body),
    HABIT_MONTH("habit.month", R.string.practice_habit_month_title, R.string.practice_habit_month_instruction, R.string.practice_habit_month_body),
    HABIT_EDIT("habit.edit", R.string.practice_habit_edit_title, R.string.practice_habit_edit_instruction, R.string.practice_habit_edit_body),
    MEHFIL_SPACE("mehfil.space", R.string.practice_mehfil_space_title, R.string.practice_mehfil_space_instruction, R.string.practice_mehfil_space_body),
    MEHFIL_POST("mehfil.post", R.string.practice_mehfil_post_title, R.string.practice_mehfil_post_instruction, R.string.practice_mehfil_post_body),
    MEHFIL_WRITE("mehfil.write", R.string.practice_mehfil_write_title, R.string.practice_mehfil_write_instruction, R.string.practice_mehfil_write_body),
    MEHFIL_SHARE("mehfil.share", R.string.practice_mehfil_share_title, R.string.practice_mehfil_share_instruction, R.string.practice_mehfil_share_body),
    MEHFIL_REACT("mehfil.react", R.string.practice_mehfil_react_title, R.string.practice_mehfil_react_instruction, R.string.practice_mehfil_react_body),
    MEHFIL_SAVED("mehfil.saved", R.string.practice_mehfil_saved_title, R.string.practice_mehfil_saved_instruction, R.string.practice_mehfil_saved_body),
    MEHFIL_CONNECTIONS("mehfil.connections", R.string.practice_mehfil_connections_title, R.string.practice_mehfil_connections_instruction, R.string.practice_mehfil_connections_body),
    DHYAN_LENGTH("dhyan.length", R.string.practice_dhyan_length_title, R.string.practice_dhyan_length_instruction, R.string.practice_dhyan_length_body),
    DHYAN_START("dhyan.start", R.string.practice_dhyan_start_title, R.string.practice_dhyan_start_instruction, R.string.practice_dhyan_start_body),
    DHYAN_PAUSE("dhyan.pause", R.string.practice_dhyan_pause_title, R.string.practice_dhyan_pause_instruction, R.string.practice_dhyan_pause_body),
    DHYAN_RESET("dhyan.reset", R.string.practice_dhyan_reset_title, R.string.practice_dhyan_reset_instruction, R.string.practice_dhyan_reset_body),
    DHYAN_GUIDANCE("dhyan.guidance", R.string.practice_dhyan_guidance_title, R.string.practice_dhyan_guidance_instruction, R.string.practice_dhyan_guidance_body),
    DHYAN_SOUND("dhyan.sound", R.string.practice_dhyan_sound_title, R.string.practice_dhyan_sound_instruction, R.string.practice_dhyan_sound_body),
    DHYAN_VOLUME("dhyan.volume", R.string.practice_dhyan_volume_title, R.string.practice_dhyan_volume_instruction, R.string.practice_dhyan_volume_body),
    CIRCLE_PUBLIC("circle.public", R.string.practice_next_circle_public_title, R.string.practice_next_circle_public_instruction, R.string.practice_next_circle_public_body),
    CIRCLE_JOIN_CODE("circle.joinCode", R.string.practice_next_circle_join_code_title, R.string.practice_next_circle_join_code_instruction, R.string.practice_next_circle_join_code_body),
    CIRCLE_CODE("circle.code", R.string.practice_next_circle_code_title, R.string.practice_next_circle_code_instruction, R.string.practice_next_circle_code_body),
    CIRCLE_JOINED("circle.joined", R.string.practice_next_circle_joined_title, R.string.practice_next_circle_joined_instruction, R.string.practice_next_circle_joined_body),
    CIRCLE_CREATE("circle.create", R.string.practice_next_circle_create_title, R.string.practice_next_circle_create_instruction, R.string.practice_next_circle_create_body),
    CIRCLE_PRIVACY("circle.privacy", R.string.practice_next_circle_privacy_title, R.string.practice_next_circle_privacy_instruction, R.string.practice_next_circle_privacy_body),
    CIRCLE_MEMBERS("circle.members", R.string.practice_next_circle_members_title, R.string.practice_next_circle_members_instruction, R.string.practice_next_circle_members_body),
    CIRCLE_FOCUS("circle.focus", R.string.practice_next_circle_focus_title, R.string.practice_next_circle_focus_instruction, R.string.practice_next_circle_focus_body),
    CHECKIN_MOOD("checkin.mood", R.string.practice_next_checkin_mood_title, R.string.practice_next_checkin_mood_instruction, R.string.practice_next_checkin_mood_body),
    CHECKIN_DETAILS("checkin.details", R.string.practice_next_checkin_details_title, R.string.practice_next_checkin_details_instruction, R.string.practice_next_checkin_details_body),
    CHECKIN_SAVE("checkin.save", R.string.practice_next_checkin_save_title, R.string.practice_next_checkin_save_instruction, R.string.practice_next_checkin_save_body),
    CHECKIN_HISTORY("checkin.history", R.string.practice_next_checkin_history_title, R.string.practice_next_checkin_history_instruction, R.string.practice_next_checkin_history_body),
    JOURNAL_PROMPT("journal.prompt", R.string.practice_next_journal_prompt_title, R.string.practice_next_journal_prompt_instruction, R.string.practice_next_journal_prompt_body),
    JOURNAL_WRITE("journal.write", R.string.practice_next_journal_write_title, R.string.practice_next_journal_write_instruction, R.string.practice_next_journal_write_body),
    JOURNAL_HISTORY("journal.history", R.string.practice_next_journal_history_title, R.string.practice_next_journal_history_instruction, R.string.practice_next_journal_history_body),
    STREAKS_COUNTS("streaks.counts", R.string.practice_next_streaks_counts_title, R.string.practice_next_streaks_counts_instruction, R.string.practice_next_streaks_counts_body),
    STREAKS_CALENDAR("streaks.calendar", R.string.practice_next_streaks_calendar_title, R.string.practice_next_streaks_calendar_instruction, R.string.practice_next_streaks_calendar_body),
    STREAKS_TREND("streaks.trend", R.string.practice_next_streaks_trend_title, R.string.practice_next_streaks_trend_instruction, R.string.practice_next_streaks_trend_body),
    ANALYTICS_OVERVIEW("analytics.sections", R.string.practice_next_analytics_overview_title, R.string.practice_next_analytics_overview_instruction, R.string.practice_next_analytics_overview_body),
    ANALYTICS_GOALS("analytics.sections", R.string.practice_next_analytics_goals_title, R.string.practice_next_analytics_goals_instruction, R.string.practice_next_analytics_goals_body),
    ANALYTICS_FOCUS("analytics.sections", R.string.practice_next_analytics_focus_title, R.string.practice_next_analytics_focus_instruction, R.string.practice_next_analytics_focus_body),
    ANALYTICS_MONTH("analytics.month", R.string.practice_next_analytics_month_title, R.string.practice_next_analytics_month_instruction, R.string.practice_next_analytics_month_body),
    PLANNER_EXAMS("planner.exams", R.string.practice_next_planner_exams_title, R.string.practice_next_planner_exams_instruction, R.string.practice_next_planner_exams_body),
    PLANNER_HOME("planner.home", R.string.practice_next_planner_home_title, R.string.practice_next_planner_home_instruction, R.string.practice_next_planner_home_body),
    PLANNER_SYLLABUS("planner.syllabus", R.string.practice_next_planner_syllabus_title, R.string.practice_next_planner_syllabus_instruction, R.string.practice_next_planner_syllabus_body),
    PLANNER_CALENDAR("planner.calendar", R.string.practice_next_planner_calendar_title, R.string.practice_next_planner_calendar_instruction, R.string.practice_next_planner_calendar_body),
    PLANNER_PROGRESS("planner.progress", R.string.practice_next_planner_progress_title, R.string.practice_next_planner_progress_instruction, R.string.practice_next_planner_progress_body),

}

internal enum class PracticeLesson(val feature: TitliFeature, val title: Int, vararg val scenes: PracticeScene) {
    BATCH_CORE(TitliFeature.TOPPERS_BATCH, R.string.practice_batch_core, PracticeScene.BATCH_SUBJECT, PracticeScene.BATCH_LECTURE, PracticeScene.BATCH_DONE, PracticeScene.BATCH_REVISION),
    BATCH_REVIEW(TitliFeature.TOPPERS_BATCH, R.string.practice_batch_review, PracticeScene.BATCH_MODES, PracticeScene.BATCH_CALENDAR, PracticeScene.BATCH_PROGRESS),
    HABITS_CORE(TitliFeature.HABITS, R.string.practice_habits_core, PracticeScene.HABIT_ADD, PracticeScene.HABIT_NAME, PracticeScene.HABIT_REPEAT, PracticeScene.HABIT_CHECK),
    HABITS_REVIEW(TitliFeature.HABITS, R.string.practice_habits_review, PracticeScene.HABIT_WEEK, PracticeScene.HABIT_MONTH, PracticeScene.HABIT_EDIT),
    MEHFIL_CORE(TitliFeature.MEHFIL, R.string.practice_mehfil_core, PracticeScene.MEHFIL_SPACE, PracticeScene.MEHFIL_POST, PracticeScene.MEHFIL_WRITE, PracticeScene.MEHFIL_SHARE),
    MEHFIL_REVIEW(TitliFeature.MEHFIL, R.string.practice_mehfil_review, PracticeScene.MEHFIL_REACT, PracticeScene.MEHFIL_SAVED, PracticeScene.MEHFIL_CONNECTIONS),
    DHYAN_CORE(TitliFeature.DHYAN, R.string.practice_dhyan_core, PracticeScene.DHYAN_LENGTH, PracticeScene.DHYAN_START, PracticeScene.DHYAN_PAUSE, PracticeScene.DHYAN_RESET),
    DHYAN_REVIEW(TitliFeature.DHYAN, R.string.practice_dhyan_review, PracticeScene.DHYAN_GUIDANCE, PracticeScene.DHYAN_SOUND, PracticeScene.DHYAN_VOLUME),
    CIRCLE_CORE(TitliFeature.STUDY_CIRCLE, R.string.practice_next_circle_core, PracticeScene.CIRCLE_PUBLIC, PracticeScene.CIRCLE_JOIN_CODE, PracticeScene.CIRCLE_CODE, PracticeScene.CIRCLE_JOINED),
    CIRCLE_CREATE(TitliFeature.STUDY_CIRCLE, R.string.practice_next_circle_create_lesson, PracticeScene.CIRCLE_CREATE, PracticeScene.CIRCLE_PRIVACY, PracticeScene.CIRCLE_MEMBERS, PracticeScene.CIRCLE_FOCUS),
    CHECKIN_CORE(TitliFeature.CHECK_IN, R.string.practice_next_checkin_core, PracticeScene.CHECKIN_MOOD, PracticeScene.CHECKIN_DETAILS, PracticeScene.CHECKIN_SAVE, PracticeScene.CHECKIN_HISTORY),
    JOURNAL_CORE(TitliFeature.JOURNAL, R.string.practice_next_journal_core, PracticeScene.JOURNAL_PROMPT, PracticeScene.JOURNAL_WRITE, PracticeScene.JOURNAL_HISTORY),
    STREAKS_CORE(TitliFeature.STREAKS, R.string.practice_next_streaks_core, PracticeScene.STREAKS_COUNTS, PracticeScene.STREAKS_CALENDAR, PracticeScene.STREAKS_TREND),
    ANALYTICS_CORE(TitliFeature.ANALYTICS, R.string.practice_next_analytics_core, PracticeScene.ANALYTICS_OVERVIEW, PracticeScene.ANALYTICS_GOALS, PracticeScene.ANALYTICS_FOCUS, PracticeScene.ANALYTICS_MONTH),
    PLANNER_CORE(TitliFeature.EXAM_PLANNER, R.string.practice_next_planner_core, PracticeScene.PLANNER_EXAMS, PracticeScene.PLANNER_HOME, PracticeScene.PLANNER_SYLLABUS, PracticeScene.PLANNER_CALENDAR, PracticeScene.PLANNER_PROGRESS);
    fun steps() = scenes.map { FeatureTutorialStep(it.name, it.target, it.title, it.body, it.instruction) }
    companion object {
        fun forFeature(feature: TitliFeature) = entries.filter { it.feature == feature }
    }
}

/** No domain ViewModels, repositories, players, navigation, or permission APIs. */
internal class ScreenPracticeViewModel : ViewModel() {
    var isOpen by mutableStateOf(false); private set
    var lesson by mutableStateOf<PracticeLesson?>(null); private set
    var state by mutableStateOf<FeatureTutorialState?>(null); private set
    var completedLesson by mutableStateOf<PracticeLesson?>(null); private set
    fun open() { isOpen = true; lesson = null; state = null }
    fun start(value: PracticeLesson) { isOpen = true; lesson = value; state = FeatureTutorialState(value.steps()) }
    fun complete() { completedLesson = lesson; lesson = null; state = null }
    fun acknowledgeCompletion() { completedLesson = null }
    fun close() { isOpen = false; lesson = null; state = null }
}

internal fun TitliFeature.practiceTitle(): Int = when (this) {
    TitliFeature.TOPPERS_BATCH -> R.string.practice_batch
    TitliFeature.HABITS -> R.string.practice_habits
    TitliFeature.MEHFIL -> R.string.practice_mehfil
    TitliFeature.DHYAN -> R.string.practice_dhyan
    TitliFeature.STUDY_CIRCLE -> R.string.practice_next_circle
    TitliFeature.CHECK_IN -> R.string.practice_next_checkin
    TitliFeature.JOURNAL -> R.string.practice_next_journal
    TitliFeature.STREAKS -> R.string.practice_next_streaks
    TitliFeature.ANALYTICS -> R.string.practice_next_analytics
    TitliFeature.EXAM_PLANNER -> R.string.practice_next_planner
    else -> error("No practice catalog for $this")
}
