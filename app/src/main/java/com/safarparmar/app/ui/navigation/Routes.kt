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
    const val LEADERBOARD = "leaderboard"
    const val STUDY_CIRCLES = "study_circles"
    const val STUDY_CIRCLE_DETAIL = "study_circles/{circleId}"
    const val DM_CHAT = "mehfil/dm_chat"
    /** Full route pattern for DM_CHAT that accepts optional deep-link args. */
    const val DM_CHAT_ROUTE = "mehfil/dm_chat?targetUserId={targetUserId}&targetUserName={targetUserName}&contextPreview={contextPreview}"
    const val DHYAN  = "dhyan"
    const val COURSES = "dhyan_courses"
    const val APP_PICKER = "ekagra/app_picker"
    const val KAVACH_ABOUT = "kavach/about"
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

    fun nishthaTab(tab: Int, section: String = "overview"): String =
        "nishtha?tab=$tab&section=${encodeParam(section)}"

    /** Canonical entry route for drawer / HomeScreen launches. */
    fun nishthaRoot(): String = "nishtha?tab=0&section=overview"

    /** Canonical entry route for drawer / HomeScreen launches. */
    fun studyPlannerRoot(): String = "study_planner?showDailyTodoSetup=false"

    /** Normalize legacy/plain feature routes to their registered destination patterns. */
    fun normalizeFeatureRoute(route: String): String = when {
        route == NISHTHA || (route.substringBefore("?") == NISHTHA && !route.contains("tab=")) ->
            nishthaRoot()
        route.startsWith("nishtha?") && !route.contains("section=") -> {
            val tab = Regex("tab=(\\d+)").find(route)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            nishthaTab(tab, "overview")
        }
        route == STUDY_PLANNER || (route.substringBefore("?") == STUDY_PLANNER && !route.contains("showDailyTodoSetup=")) ->
            studyPlannerRoot()
        else -> route
    }

    private fun encodeParam(value: String): String =
        runCatching { java.net.URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)

    /** Deep-link target that opens a specific plan straight on its Revision tab. */
    fun studyPlannerRevision(planId: String): String =
        "study_planner?planId=${encodeParam(planId)}&showDailyTodoSetup=false&openTab=revision"

    fun ekagraForGoal(goalId: String, goalTitle: String): String =
        "ekagra?goalId=${encodeParam(goalId)}&goalTitle=${encodeParam(goalTitle)}"

    fun ekagraForTopic(topicId: String, topicTitle: String, planId: String): String =
        "ekagra?topicId=${encodeParam(topicId)}&topicTitle=${encodeParam(topicTitle)}&planId=${encodeParam(planId)}"

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

    fun studyCircleDetail(circleId: String): String =
        "study_circles/${android.net.Uri.encode(circleId)}"

    /**
     * Navigate directly into the DM chat screen for a specific user, bypassing the
     * Mehfil hub. Encodes the target's info so the DmChatScreen can fire the connect
     * request itself when no active DmSession already exists.
     */
    fun dmChatDirect(targetUserId: String, targetUserName: String, contextPreview: String): String =
        "mehfil/dm_chat?targetUserId=${encodeParam(targetUserId)}" +
            "&targetUserName=${encodeParam(targetUserName)}" +
            "&contextPreview=${encodeParam(contextPreview)}"
}
