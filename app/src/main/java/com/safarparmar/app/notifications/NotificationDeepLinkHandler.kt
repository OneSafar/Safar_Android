package com.safarparmar.app.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.safarparmar.app.MainActivity
import com.safarparmar.app.ui.navigation.Routes
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
        val trimmed = deepLink?.trim().orEmpty()
        if (!trimmed.startsWith("safar://")) return Routes.HOME

        val parts = trimmed
            .removePrefix("safar://")
            .trimStart('/')
            .split('/')
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return Routes.HOME

        val host = parts[0]
        val segments = parts.drop(1)
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
            "studyplanner" -> Routes.STUDY_PLANNER
            "live" -> when (firstSegment) {
                "session" -> segments.getOrNull(1)?.let { sessionId ->
                    "live/session/${decodePathSegment(sessionId)}"
                } ?: Routes.LIVE_SESSIONS_ROOT
                else -> Routes.LIVE_SESSIONS_ROOT
            }
            else -> Routes.HOME
        }
    }

    private fun decodePathSegment(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
