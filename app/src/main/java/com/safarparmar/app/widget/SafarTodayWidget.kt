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

class SafarTodayWidget : AppWidgetProvider() {
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
        private const val ACTION_REFRESH = "com.safarparmar.app.widget.REFRESH_TODAY"
        fun hasWidgets(context: Context): Boolean = ids(context).isNotEmpty()
        private fun ids(context: Context) = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SafarTodayWidget::class.java))

        internal fun render(context: Context, value: SafarWidgetSnapshot?) {
            val snapshot = value?.takeIf { it.day == LocalDate.now(widgetZone).toString() }
            val views = RemoteViews(context.packageName, R.layout.widget_safar_today)
            fun open(id: Int, route: String) {
                views.setOnClickPendingIntent(id, PendingIntent.getActivity(context, id,
                    NotificationDeepLinkHandler.activityIntent(context, route),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            open(R.id.widget_root, "safar://home")
            open(R.id.widget_focus, "safar://ekagra")
            open(R.id.widget_kavach, "safar://kavach")
            open(R.id.widget_goals, "safar://goals")
            views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context, 0,
                Intent(context, SafarTodayWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setViewVisibility(R.id.widget_content, if (value == null) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_sign_in, if (value == null) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_kavach, if (snapshot?.kavachUsed == true) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_focus_value, duration(context, snapshot?.focusSeconds))
            views.setTextViewText(R.id.widget_kavach_value, duration(context, snapshot?.protectedSeconds))
            views.setTextViewText(R.id.widget_goal_summary, context.getString(
                R.string.widget_goal_summary, snapshot?.completed?.toString() ?: "—", snapshot?.pending?.toString() ?: "—",
            ))
            val completed = snapshot?.completed ?: 0
            val total = completed + (snapshot?.pending ?: 0)
            views.setImageViewBitmap(R.id.widget_goal_ring, goalRing(context, completed, total))
            views.setTextViewText(R.id.widget_goal_fraction, context.getString(
                R.string.widget_goal_fraction,
                snapshot?.completed?.toString() ?: "—",
                if (snapshot?.completed != null && snapshot.pending != null) total.toString() else "—",
            ))
            val percentage = if (total > 0) java.text.NumberFormat.getPercentInstance(context.resources.configuration.locales[0])
                .format(completed.toDouble() / total) else "—"
            views.setTextViewText(R.id.widget_goal_percent, percentage)
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
            views.setContentDescription(R.id.widget_refresh, context.getString(R.string.widget_refresh) + ". " + status)
            views.setViewVisibility(R.id.widget_status, if (value != null && (snapshot == null || snapshot.stale || snapshot.updatedAt == 0L)) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_status, status)
            AppWidgetManager.getInstance(context).updateAppWidget(ids(context), views)
        }

        /** RemoteViews cannot host a custom View; send only the small ring bitmap. */
        private fun goalRing(context: Context, completed: Int, total: Int): android.graphics.Bitmap {
            val size = 168
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 18f
                strokeCap = android.graphics.Paint.Cap.BUTT
            }
            val bounds = android.graphics.RectF(10f, 10f, 158f, 158f)
            paint.color = context.getColor(if (total > 0) R.color.widget_progress_track else R.color.widget_divider)
            canvas.drawOval(bounds, paint)
            if (total > 0 && completed > 0) {
                paint.color = context.getColor(R.color.widget_accent)
                canvas.drawArc(bounds, -90f, 360f * (completed.toFloat() / total).coerceIn(0f, 1f), false, paint)
            }
            return bitmap
        }

        private fun duration(context: Context, seconds: Long?): String {
            if (seconds == null) return "—"
            val minutes = seconds.coerceAtLeast(0) / 60
            return when {
                seconds in 1..59 -> context.getString(R.string.widget_under_minute)
                minutes < 60 -> context.getString(R.string.widget_minutes, minutes)
                else -> context.getString(R.string.widget_hours_minutes, minutes / 60, minutes % 60)
            }
        }
    }
}
