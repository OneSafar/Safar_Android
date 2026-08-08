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
    /** App-category editor behind Kavach analytics and Kavach setup. */
    const val KAVACH_APP_CATEGORIES = "kavach/app_categories"
    const val LAUNCH_USAGE_QUESTIONNAIRE = "launch_usage_questionnaire"
    const val FOCUS_SHIELD = "focus_shield"
    const val YOUTUBE_STUDY_MODE = "youtube_study_mode"
    const val YOUTUBE_STUDY_ANALYTICS = "youtube_study_mode/analytics"
    const val LIVE_SESSIONS_ROOT = "live/sessions"
    const val LIVE_SESSIONS = "live/sessions?courseId={courseId}"
    const val LIVE_SESSION = "live/session/{sessionId}"
    const val ADMIN_NOTIFICATIONS = "admin/notifications"
    const val PREMIUM = "premium"

    // Syllabus route — single unified accordion-tree screen (subjects/chapters/topics expand in place)
    const val ROUTE_SYLLABUS_SUBJECTS = "syllabus/subjects/{planId}"

    fun nishthaTab(tab: Int): String = "nishtha?tab=$tab"

    /** Canonical entry route for drawer / HomeScreen launches. */
    fun nishthaRoot(): String = "nishtha?tab=0&section=${android.net.Uri.encode("overview")}"

    /** Canonical entry route for drawer / HomeScreen launches. */
    fun studyPlannerRoot(): String = "study_planner?showDailyTodoSetup=false"

    /** Normalize legacy/plain feature routes to their registered destination patterns. */
    fun normalizeFeatureRoute(route: String): String = when {
        route == NISHTHA || (route.substringBefore("?") == NISHTHA && !route.contains("tab=")) ->
            nishthaRoot()
        route == STUDY_PLANNER || (route.substringBefore("?") == STUDY_PLANNER && !route.contains("showDailyTodoSetup=")) ->
            studyPlannerRoot()
        else -> route
    }

    /** Deep-link target that opens a specific plan straight on its Revision tab. */
    fun studyPlannerRevision(planId: String): String =
        "study_planner?planId=${android.net.Uri.encode(planId)}&showDailyTodoSetup=false&openTab=revision"

    fun ekagraForGoal(goalId: String, goalTitle: String): String =
        "ekagra?goalId=${android.net.Uri.encode(goalId)}&goalTitle=${android.net.Uri.encode(goalTitle)}"

    fun ekagraForTopic(topicId: String, topicTitle: String, planId: String): String =
        "ekagra?topicId=${android.net.Uri.encode(topicId)}&topicTitle=${android.net.Uri.encode(topicTitle)}&planId=${android.net.Uri.encode(planId)}"

    /**
     * Context launches carry newly selected work. They must not restore an old
     * Ekagra back-stack entry with a previous topic/goal in its arguments.
     */
    fun isContextualEkagraLaunch(route: String): Boolean =
        route.substringBefore("?") == EKAGRA &&
            (route.contains("topicId=") || route.contains("goalId="))

    /**
     * True when a Nishtha route asks for a specific tab or analytics section.
     *
     * Nishtha is a single destination whose back-stack state is saved and restored,
     * so a deep link to "tab=4&section=kavach" would otherwise be answered with
     * whatever tab the student last had open — landing them on Check-In instead of
     * the analytics they tapped through to.
     */
    fun isContextualNishthaLaunch(route: String): Boolean {
        if (route.substringBefore("?") != NISHTHA) return false
        val wantsTab = Regex("tab=(\\d+)").find(route)?.groupValues?.get(1)?.toIntOrNull()
        val wantsSection = Regex("section=([^&]+)").find(route)?.groupValues?.get(1)
            ?.let { android.net.Uri.decode(it) }
        return (wantsTab != null && wantsTab != 0) ||
            (wantsSection != null && wantsSection != "overview")
    }

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
