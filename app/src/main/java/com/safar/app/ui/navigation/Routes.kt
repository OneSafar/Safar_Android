package com.safar.app.ui.navigation

object Routes {
    const val SPLASH  = "splash"
    const val AUTH    = "auth"
    const val HOME      = "home"
    const val DASHBOARD = "dashboard"
    const val PROFILE   = "profile"
    const val ACHIEVEMENTS = "achievements"
    const val NISHTHA           = "nishtha"
    const val NISHTHA_CHECKIN   = "nishtha/checkin"
    const val NISHTHA_JOURNAL   = "nishtha/journal"
    const val NISHTHA_GOALS     = "nishtha/goals"
    const val NISHTHA_STREAKS   = "nishtha/streaks"
    const val NISHTHA_ANALYTICS = "nishtha/analytics"
    const val EKAGRA = "ekagra"
    const val EKAGRA_LINKED = "ekagra?goalId={goalId}&goalTitle={goalTitle}&view={view}"
    const val MEHFIL = "mehfil"
    const val DM_CHAT = "mehfil/dm_chat"
    const val DHYAN  = "dhyan"
    const val APP_PICKER = "ekagra/app_picker"

    fun ekagraForGoal(goalId: String, goalTitle: String): String =
        "ekagra?goalId=${android.net.Uri.encode(goalId)}&goalTitle=${android.net.Uri.encode(goalTitle)}"

    fun ekagraAnalytics(): String = "ekagra?view=analytics"
}
