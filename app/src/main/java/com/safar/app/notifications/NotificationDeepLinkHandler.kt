package com.safar.app.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.safar.app.MainActivity
import com.safar.app.ui.navigation.Routes

object NotificationDeepLinkHandler {
    const val EXTRA_ROUTE = "notification_route"
    const val EXTRA_DEEP_LINK = "notification_deep_link"

    fun activityIntent(context: Context, deepLink: String?): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK, deepLink)
            putExtra(EXTRA_ROUTE, routeFor(deepLink))
            data = deepLink?.let { runCatching { Uri.parse(it) }.getOrNull() }
        }

    fun routeFor(deepLink: String?): String {
        val uri = deepLink?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return Routes.HOME
        if (uri.scheme != "safar") return Routes.HOME

        val host = uri.host.orEmpty()
        val segments = uri.pathSegments
        val firstSegment = segments.firstOrNull().orEmpty()

        return when (host) {
            "ekagra" -> Routes.EKAGRA
            "dashboard" -> Routes.DASHBOARD
            "nishtha" -> when (firstSegment) {
                "goals" -> Routes.NISHTHA_GOALS
                "streaks" -> Routes.NISHTHA_STREAKS
                else -> Routes.NISHTHA
            }
            "streaks" -> Routes.NISHTHA_STREAKS
            "goals" -> Routes.NISHTHA_GOALS
            "mehfil" -> Routes.MEHFIL
            "profile" -> Routes.PROFILE
            "course" -> Routes.NISHTHA
            else -> Routes.HOME
        }
    }
}
