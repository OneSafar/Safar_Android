package com.safar.app.ui.studyplanner

import com.safar.app.domain.model.studyplanner.TopicStatus

enum class InsightTrackStatus { ON_TRACK, AT_RISK, BEHIND, AHEAD, NEEDS_DATA }

data class PlannerInsightSummary(
    val completionPercent: Int,
    val remainingTopics: Int,
    val daysUntilExam: Int?,
    val availableStudyDays: Int?,
    val requiredTopicsPerStudyDay: Float?,
    val onTrackStatus: InsightTrackStatus,
    val forecastCompletionDate: String?,
    val daysBuffer: Int?,
    val scheduleCoveragePercent: Int?,
)

data class PlannerInsightDayLoad(
    val date: String,
    val plannedCount: Int,
    val doneCount: Int,
)

data class PlannerInsightWorkload(
    val next14Days: List<PlannerInsightDayLoad>,
    val overloadDays: Int,
    val emptyStudyDays: Int,
    val busiestDay: PlannerInsightDayLoad?,
    val busiestSubjectUpcoming: String?,
)

data class PlannerInsightSubjectRow(
    val subjectId: String,
    val subjectName: String,
    val completionPercent: Int,
    val remainingTopics: Int,
    val overdueTopics: Int,
    val revisionTopics: Int,
)

data class PlannerInsightLaggingChapter(
    val subjectName: String,
    val chapterName: String,
    val remainingTopics: Int,
    val completionPercent: Int,
    val overdueTopics: Int,
)

data class PlannerInsightBacklog(
    val overdueTotal: Int,
    val overdue1to3: Int,
    val overdue4to7: Int,
    val overdue8Plus: Int,
    val unplannedUnfinished: Int,
    val revisionNeeded: Int,
)

data class PlannerInsightConsistency(
    val studyStreak: Int,
    val activeDaysLast14: Int,
    val activeDaysLast30: Int,
    val bestStudyWeekday: String,
    val heatmap: List<HeatmapCell>,
)

data class HeatmapCell(val date: String, val count: Int)

data class PlannerInsights(
    val summary: PlannerInsightSummary,
    val workload: PlannerInsightWorkload,
    val consistency: PlannerInsightConsistency,
    val subjectRows: List<PlannerInsightSubjectRow>,
    val laggingChapters: List<PlannerInsightLaggingChapter>,
    val backlog: PlannerInsightBacklog,
    val recommendations: List<String>,
)
