package com.safarparmar.app.notifications

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object PlannerAlertDedupe {
    fun overdueKey(planId: String, date: LocalDate): String =
        "alert_${planId}_overdue_${date}"

    fun paceWarningKey(planId: String, date: LocalDate): String =
        "alert_${planId}_pace_warning_${weekKey(date)}"

    fun examCountdownKey(planId: String, daysUntilExam: Long): String =
        "alert_${planId}_exam_${daysUntilExam}d"

    fun revisionReminderKey(planId: String, date: LocalDate): String =
        "alert_${planId}_revision_${date}"

    fun weekKey(date: LocalDate): String {
        val fields = WeekFields.of(Locale.getDefault())
        val week = date.get(fields.weekOfWeekBasedYear())
        val year = date.get(fields.weekBasedYear())
        return "$year-W$week"
    }
}
