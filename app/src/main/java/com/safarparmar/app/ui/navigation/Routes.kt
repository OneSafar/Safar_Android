package com.safarparmar.app.ui.navigation

object Routes {
    const val SPLASH  = "splash"
    const val AUTH    = "auth"
    const val HOME      = "home"
    const val DASHBOARD = "dashboard"
    const val PROFILE   = "profile"
    const val SETTINGS  = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val NISHTHA           = "nishtha"
    // Single Nishtha destination. `tab` selects the bottom-nav tab (4 = Analytics),
    // `section` picks the analytics sub-section. Both optional so plain "nishtha",
    // "nishtha?tab=N", and analytics deep-links all resolve to the SAME destination
    // (one ViewModel, no duplicate back-stack entries).
    const val NISHTHA_ROUTE     = "nishtha?tab={tab}&section={section}"
    const val EKAGRA = "ekagra"
    // Single Ekagra destination — goal-linked, topic-linked, and plain launches all
    // resolve here (one ViewModel / TimerService binding).
    const val EKAGRA_ROUTE = "ekagra?goalId={goalId}&goalTitle={goalTitle}&view={view}&topicId={topicId}&topicTitle={topicTitle}&planId={planId}"
    const val STUDY_PLANNER = "study_planner"
    const val STUDY_PLANNER_ROUTE = "study_planner?planId={planId}&showDailyTodoSetup={showDailyTodoSetup}&openTab={openTab}"
    const val CREATE_PLAN = "study_planner/create"
    const val MEHFIL = "mehfil"
    const val DM_CHAT = "mehfil/dm_chat"
    const val DHYAN  = "dhyan"
    const val COURSES = "dhyan_courses"
    const val APP_PICKER = "ekagra/app_picker"
    const val KAVACH_ABOUT = "kavach/about"
    const val KAVACH_SESSION_SUMMARY = "kavach/session_summary"
    const val LAUNCH_USAGE_QUESTIONNAIRE = "launch_usage_questionnaire"
    const val FOCUS_SHIELD = "focus_shield"
    const val LIVE_SESSIONS_ROOT = "live/sessions"
    const val LIVE_SESSIONS = "live/sessions?courseId={courseId}"
    const val LIVE_SESSION = "live/session/{sessionId}"
    const val ADMIN_NOTIFICATIONS = "admin/notifications"
    const val PREMIUM = "premium"

    // Syllabus route — single unified accordion-tree screen (subjects/chapters/topics expand in place)
    const val ROUTE_SYLLABUS_SUBJECTS = "syllabus/subjects/{planId}"

    fun nishthaTab(tab: Int): String = "nishtha?tab=$tab"

    /** Deep-link target that opens a specific plan straight on its Revision tab. */
    fun studyPlannerRevision(planId: String): String =
        "study_planner?planId=${android.net.Uri.encode(planId)}&showDailyTodoSetup=false&openTab=revision"

    fun ekagraForGoal(goalId: String, goalTitle: String): String =
        "ekagra?goalId=${android.net.Uri.encode(goalId)}&goalTitle=${android.net.Uri.encode(goalTitle)}"

    fun ekagraForTopic(topicId: String, topicTitle: String, planId: String): String =
        "ekagra?topicId=${android.net.Uri.encode(topicId)}&topicTitle=${android.net.Uri.encode(topicTitle)}&planId=${android.net.Uri.encode(planId)}"

    // Analytics is Nishtha tab index 4; resolves to the single NISHTHA_ROUTE.
    fun nishthaAnalytics(section: String = "overview"): String =
        "nishtha?tab=4&section=${android.net.Uri.encode(section)}"

    fun ekagraAnalytics(): String = nishthaAnalytics("ekagra")

    fun liveSessions(courseId: String? = null): String =
        if (courseId.isNullOrBlank()) LIVE_SESSIONS_ROOT
        else "live/sessions?courseId=${android.net.Uri.encode(courseId)}"

    fun liveSession(sessionId: String): String =
        "live/session/${android.net.Uri.encode(sessionId)}"
}
