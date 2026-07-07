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
    const val EXTRA_ENTER_TIMER_PIP = "enter_timer_pip"

    fun activityIntent(
        context: Context,
        deepLink: String?,
        enterTimerPip: Boolean = false,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK, deepLink)
            putExtra(EXTRA_ROUTE, routeFor(deepLink))
            putExtra(EXTRA_ENTER_TIMER_PIP, enterTimerPip)
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
            "ekagra" -> when (firstSegment) {
                "app_picker" -> Routes.APP_PICKER
                else -> Routes.EKAGRA
            }
            "dashboard" -> Routes.DASHBOARD
            "nishtha" -> when (firstSegment) {
                "checkin"   -> Routes.nishthaTab(0)
                "journal"   -> Routes.nishthaTab(1)
                "goals"     -> Routes.nishthaTab(2)
                "streaks"   -> Routes.nishthaTab(3)
                "analytics" -> Routes.nishthaTab(4)
                else        -> Routes.NISHTHA
            }
            "streaks" -> Routes.nishthaTab(3)
            "goals"   -> Routes.nishthaTab(2)
            "mehfil" -> when (firstSegment) {
                "dm_chat" -> Routes.DM_CHAT
                else -> Routes.MEHFIL
            }
            "profile" -> Routes.PROFILE
            "settings" -> Routes.SETTINGS
            "achievements" -> Routes.ACHIEVEMENTS
            "dhyan" -> Routes.DHYAN
            "focus_shield" -> Routes.FOCUS_SHIELD
            "course" -> Routes.NISHTHA
            "studyplanner", "study_planner" -> Routes.STUDY_PLANNER
            "admin" -> when (firstSegment) {
                "notifications" -> Routes.ADMIN_NOTIFICATIONS
                else -> Routes.HOME
            }
            "premium" -> Routes.PREMIUM
            "live" -> when (firstSegment) {
                "session" -> segments.getOrNull(1)?.let { sessionId ->
                    "live/session/${decodePathSegment(sessionId)}"
                } ?: Routes.LIVE_SESSIONS_ROOT
                "sessions" -> Routes.LIVE_SESSIONS_ROOT
                else -> Routes.LIVE_SESSIONS_ROOT
            }
            else -> Routes.HOME
        }
    }

    private fun decodePathSegment(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
