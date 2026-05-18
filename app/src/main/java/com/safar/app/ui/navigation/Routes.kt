package com.safar.app.ui.navigation

object Routes {
    const val SPLASH  = "splash"
    const val AUTH    = "auth"
    const val HOME      = "home"
    const val DASHBOARD = "dashboard"
    const val PROFILE   = "profile"
    const val SETTINGS  = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val NISHTHA           = "nishtha"
    const val NISHTHA_CHECKIN   = "nishtha/checkin"
    const val NISHTHA_JOURNAL   = "nishtha/journal"
    const val NISHTHA_GOALS     = "nishtha/goals"
    const val NISHTHA_STREAKS   = "nishtha/streaks"
    const val NISHTHA_ANALYTICS = "nishtha/analytics"
    const val NISHTHA_ANALYTICS_SECTION = "nishtha/analytics?section={section}"
    const val EKAGRA = "ekagra"
    const val EKAGRA_LINKED = "ekagra?goalId={goalId}&goalTitle={goalTitle}&view={view}"
    const val STUDY_PLANNER = "study_planner"
    const val MEHFIL = "mehfil"
    const val DM_CHAT = "mehfil/dm_chat"
    const val DHYAN  = "dhyan"
    const val APP_PICKER = "ekagra/app_picker"
    const val KAVACH_ABOUT = "kavach/about"
    const val KAVACH_SESSION_SUMMARY = "kavach/session_summary"
    const val LAUNCH_USAGE_QUESTIONNAIRE = "launch_usage_questionnaire"
    const val FOCUS_SHIELD = "focus_shield"

    // Syllabus Drill-Down Routes
    const val ROUTE_SYLLABUS_SUBJECTS = "syllabus/subjects/{planId}"
    const val ROUTE_SYLLABUS_CHAPTERS = "syllabus/chapters/{planId}/{subjectId}"
    const val ROUTE_SYLLABUS_TOPICS   = "syllabus/topics/{planId}/{subjectId}/{chapterId}"

    fun ekagraForGoal(goalId: String, goalTitle: String): String =
        "ekagra?goalId=${android.net.Uri.encode(goalId)}&goalTitle=${android.net.Uri.encode(goalTitle)}"

    fun nishthaAnalytics(section: String = "overview"): String =
        "nishtha/analytics?section=${android.net.Uri.encode(section)}"

    fun ekagraAnalytics(): String = nishthaAnalytics("focus")
}
