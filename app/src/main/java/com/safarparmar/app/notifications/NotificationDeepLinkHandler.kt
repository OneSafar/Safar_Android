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

    fun activityIntent(
        context: Context,
        deepLink: String?,
    ): Intent {
        val uri = deepLink?.trim()?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (isExternalWebLink(uri)) {
            return Intent(Intent.ACTION_VIEW, uri)
        }

        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK, deepLink)
            putExtra(EXTRA_ROUTE, routeFor(deepLink))
            data = uri
        }
    }

    fun isExternalWebLink(uri: Uri?): Boolean =
        uri?.scheme.equals("https", ignoreCase = true) && !uri?.host.isNullOrBlank()

    fun isExternalWebLink(urlStr: String?): Boolean {
        val trimmed = urlStr?.trim().orEmpty()
        return trimmed.startsWith("https://", ignoreCase = true)
    }

    fun routeFor(deepLink: String?): String {
        val trimmed = deepLink?.trim().orEmpty()
        if (!trimmed.startsWith("safar://")) return Routes.HOME

        // Query params (e.g. planId/tab) are used by some hosts and stripped by the
        // path-only split below, so parse them up front.
        val queryUri = runCatching { Uri.parse(trimmed) }.getOrNull()

        val parts = trimmed
            .substringBefore('?')
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
                else        -> Routes.nishthaRoot()
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
            "youtube_study_mode" -> if (firstSegment == "analytics") Routes.YOUTUBE_STUDY_ANALYTICS else Routes.YOUTUBE_STUDY_MODE
            "course" -> Routes.nishthaRoot()
            "studyplanner", "study_planner" -> {
                val planId = queryUri?.getQueryParameter("planId").orEmpty()
                val tab = queryUri?.getQueryParameter("tab").orEmpty()
                if (tab.equals("revision", ignoreCase = true) && planId.isNotBlank()) {
                    Routes.studyPlannerRevision(planId)
                } else {
                    Routes.STUDY_PLANNER
                }
            }
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
            "study_circles" -> when {
                firstSegment.isNotBlank() -> Routes.studyCircleDetail(decodePathSegment(firstSegment))
                else -> Routes.STUDY_CIRCLES
            }
            else -> Routes.HOME
        }
    }

    private fun decodePathSegment(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
