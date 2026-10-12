package com.safarparmar.app.feature.toppersbatch

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import com.safarparmar.app.R
import com.safarparmar.app.util.Resource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** UI messages retain resource IDs and raw arguments, never a rendered language. */
data class BatchNotice(@param:StringRes val resource: Int, val arguments: List<Any?> = emptyList()) {
    constructor(@StringRes resource: Int, vararg arguments: Any?) : this(resource, arguments.toList())
}
internal data class BatchDateArgument(val date: String?)
internal data class BatchSubjectArgument(val subject: BatchSubject)

/** Scoped to the current Compose configuration; safe to use in event/semantics lambdas. */
internal class BatchStrings(val resources: Resources) {
    private val selectedLocale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ENGLISH
    val isEnglish: Boolean = selectedLocale.language != "hi"
    val locale: Locale = if (selectedLocale.language == "hi" && selectedLocale.script != "Latn") Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")
    fun text(@StringRes id: Int, vararg args: Any?): String = if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args.map(::argument).toTypedArray())
    fun quantity(id: Int, count: Int, vararg args: Any?): String = resources.getQuantityString(id, count, *args.map(::argument).toTypedArray())
    private fun argument(value: Any?): Any = when (value) {
        is BatchDateArgument -> date(value.date)
        is BatchSubjectArgument -> subject(value.subject)
        is BatchNotice -> notice(value)
        null -> ""
        else -> value
    }
    fun notice(value: BatchNotice): String = text(value.resource, *value.arguments.toTypedArray())
    fun date(value: String?, pattern: String = "d MMM yyyy"): String = calendarDate(value)?.let {
        LocalDate.parse(it).format(DateTimeFormatter.ofPattern(pattern, locale))
    } ?: text(R.string.toppers_batch_not_set)
    fun subject(value: BatchSubject): String {
        val standardNames = when (value.key) {
            "english" -> setOf("", "english")
            "mathematics" -> setOf("", "mathematics", "maths", "math")
            "reasoning" -> setOf("", "reasoning")
            "gk" -> setOf("", "gk", "gk/gs", "gk / gs", "general knowledge", "general studies", "gk & gs")
            else -> return value.name
        }
        // Personal edits and catalogue-specific names are content, not UI copy.
        if (value.name.trim().lowercase(Locale.ROOT) !in standardNames || (isEnglish && value.name.isNotBlank())) return value.name
        return text(when (value.key) {
            "english" -> R.string.toppers_batch_english
            "mathematics" -> R.string.toppers_batch_mathematics
            "reasoning" -> R.string.toppers_batch_reasoning
            else -> R.string.toppers_batch_general_knowledge
        })
    }
    fun time(value: String): String = runCatching {
        java.time.LocalTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", locale))
    }.getOrDefault(value)
    fun weekdays() = java.time.DayOfWeek.entries.map { it.getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    fun percent(fraction: Float): String = java.text.NumberFormat.getPercentInstance(locale).apply {
        maximumFractionDigits = if (fraction > 0f && fraction < 0.01f) 1 else 0
    }.format(fraction.toDouble())
    fun copy(overview: BatchOverview?, key: String, fallback: String): String =
        if (isEnglish) overview?.content?.copy?.get(key)?.takeIf { it.isNotBlank() }?.let {
            if (key == "todayTitle" && it == "Today’s Watch List") fallback else it
        } ?: fallback else fallback
}

@Composable
internal fun rememberBatchStrings(): BatchStrings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { BatchStrings(context.resources) }
}

/** Raw repository messages remain available for legacy-server compatibility checks. */
internal fun batchErrorNotice(error: Resource.Error<*>): BatchNotice {
    val id = when (error.message) {
        "Your account changed. Please reopen the tracker." -> R.string.toppers_batch_your_account_changed_please_reopen_the_tracker
        "This option is not available" -> R.string.toppers_batch_this_option_is_not_available
        "Revision planning is currently unavailable." -> R.string.toppers_batch_revision_planning_is_currently_unavailable
        "Reminders are unavailable. Turn off Remind me to save the date." -> R.string.toppers_batch_reminders_are_unavailable_turn_off_remind_me_to_save_the_date
        "Choose one watch date." -> R.string.toppers_batch_choose_one_watch_date
        "Could not read Toppers Batch data. Please try again." -> R.string.toppers_batch_could_not_read_toppers_batch_data_please_try_again
        "This colour is used by another subject. Choose a different colour." -> R.string.toppers_batch_this_colour_is_used_by_another_subject_choose_a_different_colour
        "Class not started" -> R.string.toppers_batch_class_not_started
        "Could not reach SAFAR. Please check your internet connection." -> R.string.toppers_batch_could_not_reach_safar_please_check_your_internet_connection
        "SAFAR is taking too long to respond. Please try again." -> R.string.toppers_batch_safar_is_taking_too_long_to_respond_please_try_again
        "Connection to SAFAR was interrupted. Please try again." -> R.string.toppers_batch_connection_to_safar_was_interrupted_please_try_again
        "Could not connect to SAFAR. Please try again." -> R.string.toppers_batch_could_not_connect_to_safar_please_try_again
        "Some plans changed during saving and were left untouched. Other selected plans were saved. Refresh to review them." -> R.string.toppers_batch_some_plans_changed_during_saving_and_were_left_untouched_other_se
        "Choose valid dates and a weekly goal from 1 to 100 lectures" -> R.string.toppers_batch_choose_valid_dates_and_a_weekly_goal_from_1_to_100_lectures
        "Your start date cannot be in the future" -> R.string.toppers_batch_your_start_date_cannot_be_in_the_future
        "Target date must be on or after your start date" -> R.string.toppers_batch_target_date_must_be_on_or_after_your_start_date
        "Could not import saved lecture progress" -> R.string.toppers_batch_could_not_import_saved_lecture_progress
        "Choose lectures and a valid past completion date" -> R.string.toppers_batch_choose_lectures_and_a_valid_past_completion_date
        "Choose available lectures from your account" -> R.string.toppers_batch_choose_available_lectures_from_your_account
        "A revision is already planned on one of these dates. Choose another start date." -> R.string.toppers_batch_a_revision_is_already_planned_on_one_of_these_dates_choose_anothe
        "Choose a valid date" -> R.string.toppers_batch_choose_a_valid_date
        "Choose a valid month" -> R.string.toppers_batch_choose_a_valid_month
        "Check the subject options and try again" -> R.string.toppers_batch_check_the_subject_options_and_try_again
        "Enter a lecture name up to 300 characters" -> R.string.toppers_batch_enter_a_lecture_name_up_to_300_characters
        "Add the subject back first." -> R.string.toppers_batch_add_the_subject_back_first
        "Enter a lecture name or choose a color" -> R.string.toppers_batch_enter_a_lecture_name_or_choose_a_color
        "Choose a valid plan and reminder time" -> R.string.toppers_batch_choose_a_valid_plan_and_reminder_time
        "This lecture is already completed. Plan a revision instead." -> R.string.toppers_batch_this_lecture_is_already_completed_plan_a_revision_instead
        "Complete this lecture before planning revision." -> R.string.toppers_batch_complete_this_lecture_before_planning_revision
        "Class not started. Refresh the lecture schedule." -> R.string.toppers_batch_class_not_started_refresh_the_lecture_schedule
        "Parmar class dates are set by the admin." -> R.string.toppers_batch_parmar_class_dates_are_set_by_the_admin
        "Choose one date or five dates" -> R.string.toppers_batch_choose_one_date_or_five_dates
        "Mark the lecture as done before setting a study date" -> R.string.toppers_batch_mark_the_lecture_as_done_before_setting_a_study_date
        "Choose 1 to 5 different study dates" -> R.string.toppers_batch_choose_1_to_5_different_study_dates
        "You can choose up to five study dates in total" -> R.string.toppers_batch_you_can_choose_up_to_five_study_dates_in_total
        "No study date is set for this lecture" -> R.string.toppers_batch_no_study_date_is_set_for_this_lecture
        "You need SAFAR Premium to use this study plan." -> R.string.toppers_batch_you_need_safar_premium_to_use_this_study_plan
        "This study plan is not available for your account yet." -> R.string.toppers_batch_this_study_plan_is_not_available_for_your_account_yet
        "Open your study plan first." -> R.string.toppers_batch_open_your_study_plan_first
        "Could not open your study plan. Please try again." -> R.string.toppers_batch_could_not_open_your_study_plan_please_try_again
        "Subject not found" -> R.string.toppers_batch_this_lecture_or_subject_is_no_longer_available_refresh_your_track
        "Removed subject not found" -> R.string.toppers_batch_this_lecture_or_subject_is_no_longer_available_refresh_your_track
        "Lecture not found" -> R.string.toppers_batch_this_lecture_or_subject_is_no_longer_available_refresh_your_track
        "Removed lecture not found" -> R.string.toppers_batch_this_lecture_or_subject_is_no_longer_available_refresh_your_track
        "Some plans have changed. Refresh and select them again." -> R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again
        "Your plan changed on another screen. Refresh and try again." -> R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again
        "This revision plan has changed. Refresh and try again." -> R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again
        "Choose today or a future date" -> R.string.toppers_batch_choose_today_or_a_future_date
        "Choose one watching date" -> R.string.toppers_batch_choose_one_watch_date
        "Choose a valid personal study date" -> R.string.toppers_batch_choose_a_valid_date
        "Choose a valid next date" -> R.string.toppers_batch_choose_a_valid_date
        "Choose a valid start date" -> R.string.toppers_batch_choose_a_valid_date
        "Choose a valid date to study again" -> R.string.toppers_batch_choose_a_valid_date
        else -> when (error.errorCode) {
            "PREMIUM_REQUIRED" -> R.string.toppers_batch_safar_premium_is_needed_for_toppers_batch
            "PLANNER_V2_NOT_AVAILABLE" -> R.string.toppers_batch_toppers_batch_is_not_available_for_this_account_yet
            "PLANNER_V2_NOT_ENABLED" -> R.string.toppers_batch_open_your_study_plan_first
            "PLAN_CONFLICT" -> R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again
            "LECTURE_NOT_RELEASED" -> R.string.toppers_batch_class_not_started_refresh_the_lecture_schedule
            else -> when (error.code) {
                401 -> R.string.toppers_batch_please_sign_in_again_to_open_your_tracker
                403 -> R.string.toppers_batch_toppers_batch_is_not_available_for_this_account_yet
                404 -> R.string.toppers_batch_this_lecture_or_subject_is_no_longer_available_refresh_your_track
                409 -> R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again
                400, 422 -> R.string.toppers_batch_check_your_selections_and_try_again
                429 -> R.string.toppers_batch_please_wait_a_moment_and_try_again
                in 500..599 -> R.string.toppers_batch_request_failed
                else -> R.string.toppers_batch_something_went_wrong_please_try_again
            }
        }
    }
    return BatchNotice(id)
}
