package com.safarparmar.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.safarparmar.app.R
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Safar Bento Hub: Flagship 4x3 / 4x4 Android Home Screen Widget.
 * Unifies today's study focus, Kavach shield protection, priority goal,
 * active streak, and 1-tap quick action shortcuts to all 4 core modules.
 */
class SafarBentoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        SafarWidgetWorker.enqueue(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        SafarWidgetWorker.enqueue(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(ACTION_REFRESH, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            SafarWidgetWorker.enqueue(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.safarparmar.app.widget.REFRESH_BENTO"

        fun hasWidgets(context: Context): Boolean = ids(context).isNotEmpty()

        private fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SafarBentoWidget::class.java))

        internal fun render(context: Context, value: SafarWidgetSnapshot?) {
            val widgetIds = ids(context)
            if (widgetIds.isEmpty()) return

            val snapshot = value?.takeIf { it.day == LocalDate.now(widgetZone).toString() }
            val views = RemoteViews(context.packageName, R.layout.widget_safar_bento)

            fun open(id: Int, route: String) {
                views.setOnClickPendingIntent(
                    id,
                    PendingIntent.getActivity(
                        context,
                        id,
                        NotificationDeepLinkHandler.activityIntent(context, route),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }

            // Interactive Click Targets
            open(R.id.bento_root, "safar://home")
            open(R.id.bento_focus_card, "safar://ekagra")
            open(R.id.bento_kavach_card, "safar://kavach")
            open(R.id.bento_goal_card, "safar://goals")

            // 4-Pill Quick Actions Dock
            open(R.id.bento_action_focus, "safar://ekagra")
            open(R.id.bento_action_kavach, "safar://kavach")
            open(R.id.bento_action_goals, "safar://goals")
            open(R.id.bento_action_planner, "safar://study_planner")

            // Refresh Button
            views.setOnClickPendingIntent(
                R.id.bento_refresh,
                PendingIntent.getBroadcast(
                    context,
                    101,
                    Intent(context, SafarBentoWidget::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            // Auth State
            views.setViewVisibility(R.id.bento_content, if (value == null) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.bento_sign_in, if (value == null) View.VISIBLE else View.GONE)

            // Header Greeting & Title
            val title = if (!snapshot?.userName.isNullOrBlank()) {
                context.getString(R.string.widget_bento_greeting, snapshot.userName)
            } else {
                context.getString(R.string.widget_bento_title)
            }
            views.setTextViewText(R.id.bento_title, title)

            // Streak Badge
            val streak = snapshot?.streakDays ?: 0
            if (streak > 0) {
                views.setViewVisibility(R.id.bento_streak_pill, View.VISIBLE)
                views.setTextViewText(R.id.bento_streak_text, "🔥 " + context.getString(R.string.widget_bento_streak, streak))
            } else {
                views.setViewVisibility(R.id.bento_streak_pill, View.GONE)
            }

            // Focus Hub
            val focusSeconds = snapshot?.focusSeconds
            views.setTextViewText(R.id.bento_focus_time, duration(context, focusSeconds))
            if (focusSeconds != null && focusSeconds > 0) {
                views.setTextViewText(R.id.bento_focus_sub, context.getString(R.string.widget_bento_focus_target, 5))
                val pct = ((focusSeconds.toFloat() / (5 * 3600f)) * 100f).toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.bento_focus_progress, 100, pct, false)
            } else {
                views.setTextViewText(R.id.bento_focus_sub, context.getString(R.string.widget_bento_focus_empty))
                views.setProgressBar(R.id.bento_focus_progress, 100, 0, false)
            }

            // Kavach Shield Hub
            val isShieldOn = snapshot?.kavachUsed == true
            if (isShieldOn) {
                views.setTextViewText(R.id.bento_kavach_status, context.getString(R.string.widget_bento_shield_on))
                views.setTextViewText(
                    R.id.bento_kavach_sub,
                    context.getString(R.string.widget_bento_shield_saved, duration(context, snapshot.protectedSeconds)),
                )
                views.setTextViewText(R.id.bento_kavach_indicator, "🛡️ " + context.getString(R.string.widget_bento_shield_on))
            } else {
                views.setTextViewText(R.id.bento_kavach_status, context.getString(R.string.widget_bento_shield_off))
                views.setTextViewText(R.id.bento_kavach_sub, context.getString(R.string.widget_bento_shield_ready))
                views.setTextViewText(R.id.bento_kavach_indicator, "🛡️ " + context.getString(R.string.widget_bento_shield_off))
            }

            // Priority Goal Card
            val nextGoal = snapshot?.nextGoalTitle
            val completed = snapshot?.completed ?: 0
            val pending = snapshot?.pending ?: 0
            val total = completed + pending

            val goalText = when {
                !nextGoal.isNullOrBlank() -> nextGoal
                total > 0 && pending == 0 -> context.getString(R.string.widget_bento_all_goals_done)
                else -> context.getString(R.string.widget_bento_add_goal)
            }
            views.setTextViewText(R.id.bento_goal_title, goalText)
            views.setTextViewText(R.id.bento_goal_count, if (total > 0) "$completed/$total" else "—")

            // Status / Last Updated Info
            val status = when {
                value == null -> context.getString(R.string.widget_sign_in)
                snapshot?.stale == true -> context.getString(R.string.widget_stale)
                snapshot == null || snapshot.updatedAt == 0L -> context.getString(R.string.widget_loading)
                else -> {
                    val locale = context.resources.configuration.locales[0]
                    val time = Instant.ofEpochMilli(snapshot.updatedAt).atZone(widgetZone)
                        .format(DateTimeFormatter.ofPattern("h:mm a", locale))
                    context.getString(R.string.widget_updated, time)
                }
            }
            views.setContentDescription(R.id.bento_refresh, context.getString(R.string.widget_refresh) + ". " + status)
            views.setViewVisibility(
                R.id.bento_status,
                if (value != null && (snapshot == null || snapshot.stale || snapshot.updatedAt == 0L)) View.VISIBLE else View.GONE,
            )
            views.setTextViewText(R.id.bento_status, status)

            AppWidgetManager.getInstance(context).updateAppWidget(widgetIds, views)
        }

        private fun duration(context: Context, seconds: Long?): String {
            if (seconds == null || seconds <= 0) return "—"
            val minutes = seconds / 60
            return when {
                seconds in 1..59 -> context.getString(R.string.widget_under_minute)
                minutes < 60 -> context.getString(R.string.widget_minutes, minutes)
                else -> context.getString(R.string.widget_hours_minutes, minutes / 60, minutes % 60)
            }
        }
    }
}
