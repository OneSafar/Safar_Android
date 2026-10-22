package com.safarparmar.app.ui.ekagra

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.FeatureTutorialState
import com.safarparmar.app.ui.tutorial.FeatureTutorialStep

internal enum class EkagraTutorialScene(val targetId: String, val titleRes: Int, val messageRes: Int, val instructionRes: Int) {
    SETTINGS_ENTRY("ekagra.settingsEntry", R.string.ekagra_short_settings_title, R.string.ekagra_short_settings_body, R.string.ekagra_short_settings_instruction),
    PERSONALIZE_ENTRY("ekagra.personalizeEntry", R.string.ekagra_short_personalize_title, R.string.ekagra_short_personalize_body, R.string.ekagra_short_personalize_instruction),
    KAVACH_SETTINGS("ekagra.kavachSettings", R.string.ekagra_short_kavach_title, R.string.ekagra_short_kavach_body, R.string.ekagra_short_kavach_instruction),
    CIRCLE_JOIN("ekagra.circleJoin", R.string.ekagra_short_join_title, R.string.ekagra_short_join_body, R.string.ekagra_short_join_instruction),
    CIRCLE_CREATE("ekagra.circleCreate", R.string.ekagra_short_create_title, R.string.ekagra_short_create_body, R.string.ekagra_short_create_instruction),
    MODE("ekagra.mode", R.string.ekagra_tutorial_mode_title, R.string.ekagra_tutorial_mode_body, R.string.ekagra_tutorial_mode_instruction),
    FOCUS_DURATION("ekagra.focusDuration", R.string.ekagra_tutorial_focus_duration_title, R.string.ekagra_tutorial_focus_duration_body, R.string.ekagra_tutorial_focus_duration_instruction),
    BREAK_DURATION("ekagra.breakDuration", R.string.ekagra_tutorial_break_duration_title, R.string.ekagra_tutorial_break_duration_body, R.string.ekagra_tutorial_break_duration_instruction),
    START("ekagra.play", R.string.ekagra_tutorial_start_title, R.string.ekagra_tutorial_start_body, R.string.ekagra_tutorial_start_instruction),
    RUNNING("ekagra.controls", R.string.ekagra_tutorial_running_title, R.string.ekagra_tutorial_running_body, R.string.ekagra_tutorial_running_instruction),
    PAUSED("ekagra.play", R.string.ekagra_tutorial_paused_title, R.string.ekagra_tutorial_paused_body, R.string.ekagra_tutorial_paused_instruction),
    BREAK_RUNNING("ekagra.end", R.string.ekagra_tutorial_break_running_title, R.string.ekagra_tutorial_break_running_body, R.string.ekagra_tutorial_break_running_instruction),
    RESUMED("ekagra.break", R.string.ekagra_tutorial_resumed_title, R.string.ekagra_tutorial_resumed_body, R.string.ekagra_tutorial_resumed_instruction),
    END("ekagra.end", R.string.ekagra_tutorial_end_title, R.string.ekagra_tutorial_end_body, R.string.ekagra_tutorial_end_instruction),
    END_CONFIRM("ekagra.endConfirm", R.string.ekagra_tutorial_end_confirm_title, R.string.ekagra_tutorial_end_confirm_body, R.string.ekagra_tutorial_end_confirm_instruction),
    SAVE("ekagra.saveDetails", R.string.ekagra_tutorial_save_title, R.string.ekagra_tutorial_save_body, R.string.ekagra_tutorial_save_instruction),
    SAVE_CONFIRM("ekagra.confirmSave", R.string.ekagra_tutorial_save_confirm_title, R.string.ekagra_tutorial_save_confirm_body, R.string.ekagra_tutorial_save_confirm_instruction),
    SAVE_GOAL_CONFIRM("ekagra.confirmSave", R.string.ekagra_tutorial_save_confirm_title, R.string.ekagra_tutorial_save_confirm_body, R.string.ekagra_tutorial_save_confirm_instruction),
    SAVE_TOPIC_CONFIRM("ekagra.confirmSave", R.string.ekagra_tutorial_save_confirm_title, R.string.ekagra_tutorial_save_confirm_body, R.string.ekagra_tutorial_save_confirm_instruction),
    HISTORY("ekagra.historySummary", R.string.ekagra_tutorial_history_title, R.string.ekagra_tutorial_history_body, R.string.ekagra_tutorial_history_instruction),
    STOPWATCH("ekagra.mode", R.string.ekagra_tutorial_stopwatch_title, R.string.ekagra_tutorial_stopwatch_body, R.string.ekagra_tutorial_stopwatch_instruction),
    POMODORO_ENTRY("ekagra.pomodoroEntry", R.string.ekagra_tutorial_pomodoro_entry_title, R.string.ekagra_tutorial_pomodoro_entry_body, R.string.ekagra_tutorial_pomodoro_entry_instruction),
    POMODORO_TRADITIONAL("ekagra.pomodoroSettings", R.string.ekagra_tutorial_pomodoro_traditional_title, R.string.ekagra_tutorial_pomodoro_traditional_body, R.string.ekagra_tutorial_pomodoro_traditional_instruction),
    POMODORO_CUSTOM("ekagra.pomodoroSettings", R.string.ekagra_tutorial_pomodoro_custom_title, R.string.ekagra_tutorial_pomodoro_custom_body, R.string.ekagra_tutorial_pomodoro_custom_instruction),
    POMODORO_RUNNING("ekagra.controls", R.string.ekagra_tutorial_pomodoro_running_title, R.string.ekagra_tutorial_pomodoro_running_body, R.string.ekagra_tutorial_pomodoro_running_instruction),
    KAVACH_READY("ekagra.kavach", R.string.ekagra_tutorial_kavach_ready_title, R.string.ekagra_tutorial_kavach_ready_body, R.string.ekagra_tutorial_kavach_ready_instruction),
    KAVACH_APPS("ekagra.blockedApps", R.string.ekagra_tutorial_kavach_apps_title, R.string.ekagra_tutorial_kavach_apps_body, R.string.ekagra_tutorial_kavach_apps_instruction),
    KAVACH_ACTIVE("ekagra.kavach", R.string.ekagra_tutorial_kavach_active_title, R.string.ekagra_tutorial_kavach_active_body, R.string.ekagra_tutorial_kavach_active_instruction),
    YOUTUBE("ekagra.youtube", R.string.ekagra_tutorial_youtube_title, R.string.ekagra_tutorial_youtube_body, R.string.ekagra_tutorial_youtube_instruction),
    MUSIC("ekagra.music", R.string.ekagra_tutorial_music_title, R.string.ekagra_tutorial_music_body, R.string.ekagra_tutorial_music_instruction),
    THEME("ekagra.theme", R.string.ekagra_tutorial_theme_title, R.string.ekagra_tutorial_theme_body, R.string.ekagra_tutorial_theme_instruction),
    MUTED("ekagra.mute", R.string.ekagra_tutorial_muted_title, R.string.ekagra_tutorial_muted_body, R.string.ekagra_tutorial_muted_instruction),
    ALERTS("ekagra.alerts", R.string.ekagra_tutorial_alerts_title, R.string.ekagra_tutorial_alerts_body, R.string.ekagra_tutorial_alerts_instruction),
    AUTO_BREAK("ekagra.autoBreak", R.string.ekagra_tutorial_auto_break_title, R.string.ekagra_tutorial_auto_break_body, R.string.ekagra_tutorial_auto_break_instruction),
    FLOATING_MENU("ekagra.floatingMenu", R.string.ekagra_tutorial_floating_menu_title, R.string.ekagra_tutorial_floating_menu_body, R.string.ekagra_tutorial_floating_menu_instruction),
    FLOATING_PREVIEW("ekagra.floatingPreview", R.string.ekagra_tutorial_floating_preview_title, R.string.ekagra_tutorial_floating_preview_body, R.string.ekagra_tutorial_floating_preview_instruction),
    SAVE_GOAL("ekagra.goalChoice", R.string.ekagra_tutorial_save_goal_title, R.string.ekagra_tutorial_save_goal_body, R.string.ekagra_tutorial_save_goal_instruction),
    SAVE_GOAL_DONE("ekagra.goalChoice", R.string.ekagra_tutorial_save_goal_done_title, R.string.ekagra_tutorial_save_goal_done_body, R.string.ekagra_tutorial_save_goal_done_instruction),
    SAVE_TOPIC("ekagra.topicChoice", R.string.ekagra_tutorial_save_topic_title, R.string.ekagra_tutorial_save_topic_body, R.string.ekagra_tutorial_save_topic_instruction),
    SAVE_TOPIC_DONE("ekagra.topicChoice", R.string.ekagra_tutorial_save_topic_done_title, R.string.ekagra_tutorial_save_topic_done_body, R.string.ekagra_tutorial_save_topic_done_instruction),
    HISTORY_CATEGORIES("ekagra.historyCategories", R.string.ekagra_tutorial_history_categories_title, R.string.ekagra_tutorial_history_categories_body, R.string.ekagra_tutorial_history_categories_instruction),
    HISTORY_FILTERS("ekagra.historyFilters", R.string.ekagra_tutorial_history_filters_title, R.string.ekagra_tutorial_history_filters_body, R.string.ekagra_tutorial_history_filters_instruction),
    CIRCLES("ekagra.circles", R.string.ekagra_tutorial_circles_title, R.string.ekagra_tutorial_circles_body, R.string.ekagra_tutorial_circles_instruction),
    CIRCLE_CHOICES("ekagra.circleChoices", R.string.ekagra_tutorial_circle_choices_title, R.string.ekagra_tutorial_circle_choices_body, R.string.ekagra_tutorial_circle_choices_instruction),
}

internal enum class EkagraTutorialLesson(val titleRes: Int, vararg val scenes: EkagraTutorialScene) {
    CORE(R.string.ekagra_tutorial_core, EkagraTutorialScene.MODE, EkagraTutorialScene.SETTINGS_ENTRY,
        EkagraTutorialScene.PERSONALIZE_ENTRY, EkagraTutorialScene.START, EkagraTutorialScene.RUNNING),
    PROTECTION(R.string.ekagra_tutorial_protection, EkagraTutorialScene.KAVACH_SETTINGS,
        EkagraTutorialScene.KAVACH_APPS, EkagraTutorialScene.KAVACH_READY, EkagraTutorialScene.KAVACH_ACTIVE),
    TOGETHER(R.string.ekagra_tutorial_together, EkagraTutorialScene.CIRCLES, EkagraTutorialScene.CIRCLE_JOIN,
        EkagraTutorialScene.CIRCLE_CREATE, EkagraTutorialScene.CIRCLE_CHOICES),
    ORGANIZE(R.string.ekagra_tutorial_organize, EkagraTutorialScene.SAVE,
        EkagraTutorialScene.SAVE_CONFIRM, EkagraTutorialScene.HISTORY);

    fun steps() = scenes.map { FeatureTutorialStep(it.name, it.targetId, it.titleRes, it.messageRes, it.instructionRes) }
}

/** Deliberately has no SavedStateHandle: survives rotation, starts closed after process death. */
internal class EkagraTutorialViewModel : ViewModel() {
    var isOpen by mutableStateOf(false)
        private set
    var lesson by mutableStateOf<EkagraTutorialLesson?>(null)
        private set
    var state by mutableStateOf<FeatureTutorialState?>(null)
        private set
    var completedLesson by mutableStateOf<EkagraTutorialLesson?>(null)
        private set

    fun open() { isOpen = true; lesson = null; state = null }
    fun start(value: EkagraTutorialLesson) {
        isOpen = true
        lesson = value
        state = FeatureTutorialState(value.steps())
    }
    fun complete() { completedLesson = lesson; lesson = null; state = null }
    fun acknowledgeCompletion() { completedLesson = null }
    fun close() { isOpen = false; lesson = null; state = null }
}

/** Pure demo snapshots make Back and replay deterministic; no timer or repository dependencies. */
internal data class EkagraTutorialDemoState(val scene: EkagraTutorialScene) {
    val mode: TimerMode get() = when (scene) {
        EkagraTutorialScene.STOPWATCH -> TimerMode.STOPWATCH
        EkagraTutorialScene.BREAK_RUNNING -> TimerMode.BREAK
        EkagraTutorialScene.POMODORO_RUNNING -> TimerMode.POMODORO
        else -> TimerMode.FOCUS
    }
    val running get() = scene in setOf(EkagraTutorialScene.RUNNING, EkagraTutorialScene.BREAK_RUNNING,
        EkagraTutorialScene.RESUMED, EkagraTutorialScene.KAVACH_ACTIVE, EkagraTutorialScene.POMODORO_RUNNING)
    val hasProgress get() = running || scene in setOf(EkagraTutorialScene.PAUSED, EkagraTutorialScene.END)
    val seconds get() = when {
        mode == TimerMode.STOPWATCH -> 0
        mode == TimerMode.BREAK -> 5 * 60
        hasProgress -> 13 * 60
        else -> 25 * 60
    }
    val kavachEnabled get() = scene in setOf(EkagraTutorialScene.KAVACH_READY, EkagraTutorialScene.KAVACH_ACTIVE)
}

internal fun ekagraTutorialEligible(serviceReady: Boolean, sessionActive: Boolean, flowPending: Boolean): Boolean =
    serviceReady && !sessionActive && !flowPending

/** Scenes that present the real timer canvas rather than a feature panel. */
internal val EkagraTutorialScene.usesTimerCanvas: Boolean
    get() = this in setOf(
        EkagraTutorialScene.MODE, EkagraTutorialScene.SETTINGS_ENTRY, EkagraTutorialScene.PERSONALIZE_ENTRY, EkagraTutorialScene.START, EkagraTutorialScene.RUNNING,
        EkagraTutorialScene.PAUSED, EkagraTutorialScene.BREAK_RUNNING, EkagraTutorialScene.RESUMED,
        EkagraTutorialScene.END, EkagraTutorialScene.STOPWATCH, EkagraTutorialScene.POMODORO_RUNNING,
        EkagraTutorialScene.KAVACH_READY, EkagraTutorialScene.KAVACH_ACTIVE, EkagraTutorialScene.YOUTUBE,
        EkagraTutorialScene.CIRCLES,
    )
