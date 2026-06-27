package com.safarparmar.app.ui.dashboard

import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import java.time.LocalDate
import java.time.ZoneId

enum class DashboardStudyPlanStatus {
    NO_ACTIVE_PLAN,
    NO_TOPICS_TODAY,
    HAS_TOPICS,
}

data class DashboardStudyPlanTopic(
    val id: String,
    val title: String,
    val subtitle: String,
    val done: Boolean,
)

data class DashboardStudyPlanState(
    val status: DashboardStudyPlanStatus = DashboardStudyPlanStatus.NO_ACTIVE_PLAN,
    val planId: String? = null,
    val planTitle: String = "",
    val daysLeftText: String = "Set exam date",
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val allTopics: List<DashboardStudyPlanTopic> = emptyList(),
    val visibleTopics: List<DashboardStudyPlanTopic> = emptyList(),
    val moreCount: Int = 0,
    val errorMessage: String? = null,
)

fun buildDashboardStudyPlanState(
    plan: StudyPlan?,
    calendar: CalendarMap,
    today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    errorMessage: String? = null,
): DashboardStudyPlanState {
    if (plan == null) return DashboardStudyPlanState(errorMessage = errorMessage)

    val todayIso = today.toString()
    val topicsToday = calendar[todayIso].orEmpty()
    val total = topicsToday.size
    val done = topicsToday.count { it.status == TopicStatus.DONE }
    val allTopics = topicsToday.map {
        DashboardStudyPlanTopic(
            id = it.topicId,
            title = it.topicName,
            subtitle = listOf(it.subjectName, it.chapterName).filter { part -> part.isNotBlank() }.joinToString(" / "),
            done = it.status == TopicStatus.DONE,
        )
    }
    val visible = allTopics.take(3)

    return DashboardStudyPlanState(
        status = if (topicsToday.isEmpty()) DashboardStudyPlanStatus.NO_TOPICS_TODAY else DashboardStudyPlanStatus.HAS_TOPICS,
        planId = plan.id,
        planTitle = plan.title.ifBlank { "Study Plan" },
        daysLeftText = dashboardDaysLeftText(plan.examDate, today),
        doneCount = done,
        totalCount = total,
        allTopics = allTopics,
        visibleTopics = visible,
        moreCount = (total - visible.size).coerceAtLeast(0),
        errorMessage = errorMessage,
    )
}

fun dashboardDaysLeftText(examDateIso: String?, today: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
    val days = daysUntil(examDateIso, today) ?: return "Set exam date"
    return when {
        days < 0 -> "Exam passed"
        days == 0L -> "Exam today!"
        days == 1L -> "1 day left"
        else -> "$days days left"
    }
}
