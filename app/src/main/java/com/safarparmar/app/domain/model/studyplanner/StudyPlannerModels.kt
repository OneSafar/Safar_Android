package com.safarparmar.app.domain.model.studyplanner

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

typealias CalendarMap = Map<String, List<CalendarTopicItem>>

enum class TopicStatus(val wireValue: String, val label: String) {
    @SerializedName("todo")
    TODO("todo", "Todo"),

    @SerializedName("in_progress")
    IN_PROGRESS("in_progress", "In progress"),

    @SerializedName("done")
    DONE("done", "Done"),

    @SerializedName("revision_needed")
    REVISION_NEEDED("revision_needed", "To Revise"),
}

@Immutable
data class StudyPlannerFeatureFlags(
    val isPremium: Boolean = false,
    val unlockedAt: String? = null,
)

@Immutable
data class StudyTopic(
    val id: String = "",
    val name: String = "",
    val status: TopicStatus = TopicStatus.TODO,
    val plannedDate: String? = null,
    val completedDate: String? = null,
    val missedAt: String? = null,
    val missedReason: String? = null,
    val originalPlannedDate: String? = null,
    val notes: String? = null,
    val pinned: Boolean? = null,
    val revisionMarkedAt: String? = null,
    val revisionReminderDates: List<String> = emptyList(),
    /**
     * Dates of spaced-revision sessions the user has actually completed. Combined
     * with [revisionReminderDates] (the remaining sessions) this is the full
     * original schedule, so the UI can show "3 of 5 done" and which sessions are left.
     */
    val revisionCompletedDates: List<String> = emptyList(),
    val revisionScheduleType: String? = null,
)

@Immutable
data class StudyChapter(
    val id: String = "",
    val name: String = "",
    val topics: List<StudyTopic> = emptyList(),
)

@Immutable
data class StudySubject(
    val id: String = "",
    val name: String = "",
    val color: String = "#0ea5e9",
    val weeklyTarget: Int? = null,
    val monthlyTarget: Int? = null,
    val chapters: List<StudyChapter> = emptyList(),
)

@Immutable
data class DailyTodo(
    val id: String = "",
    val name: String = "",
)

@Immutable
data class PlanProgress(
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val inProgressTopics: Int = 0,
    val revisionTopics: Int = 0,
    val completionPercent: Int = 0,
    val remainingPercent: Int = 100,
    val plannerProgressPercent: Int = 0,
    val dailyTodoProgressPercent: Int = 0,
    val overallProgressPercent: Int = 0,
    val bySubject: List<SubjectProgress> = emptyList(),
)

@Immutable
data class SubjectProgress(
    val subjectId: String = "",
    val subjectName: String = "",
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val completionPercent: Int = 0,
    val byChapter: List<ChapterProgress> = emptyList(),
)

@Immutable
data class ChapterProgress(
    val chapterId: String = "",
    val chapterName: String = "",
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val completionPercent: Int = 0,
)

@Immutable
data class StudyPlan(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val examType: String? = null,
    val examDate: String? = null,
    val description: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int> = emptyList(),
    val offDates: List<String> = emptyList(),
    val autoRollover: Boolean? = null,
    val dailyTodos: List<DailyTodo>? = emptyList(),
    val dailyTodoLogs: Map<String, List<String>>? = emptyMap(),
    val subjects: List<StudySubject> = emptyList(),
    val features: StudyPlannerFeatureFlags = StudyPlannerFeatureFlags(),
    val templateId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val progress: PlanProgress? = null,
    val subjectCount: Int? = null,
    val completionPercent: Int? = null,
    val totalTopics: Int? = null,
    val rolloverDigest: PlannerRolloverDigest? = null,
    val undoToken: String? = null,
    val restoreSnapshots: List<PlannerPlanSnapshot> = emptyList(),
    val closedStudyDays: List<String> = emptyList(),
)

@Immutable
data class PlannerRolloverDigest(
    val movedCount: Int = 0,
    val fromDates: List<String> = emptyList(),
    val undoToken: String = "",
)

@Immutable
data class PlannerPlanSnapshot(
    val id: String = "",
    val createdAt: String = "",
    val reason: String = "",
    val label: String = "",
)

@Immutable
data class RolloverUndoResult(
    val message: String? = null,
    val plan: StudyPlan? = null,
)

@Immutable
data class CalendarTopicItem(
    val topicId: String = "",
    val topicName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectColor: String = "#0ea5e9",
    val status: TopicStatus = TopicStatus.TODO,
    val revisionReminderDates: List<String> = emptyList(),
    val revisionScheduleType: String? = null,
)

@Immutable
data class HeatmapPoint(
    val date: String = "",
    val count: Int = 0,
)

@Immutable
data class PlannerAnalytics(
    val progress: PlanProgress = PlanProgress(),
    val heatmap: List<HeatmapPoint> = emptyList(),
)

@Immutable
data class ExamTemplateSummary(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String? = null,
    val recommendedDailyGoal: Int? = null,
    val subjectCount: Int? = null,
    val topicCount: Int? = null,
)

@Immutable
data class ExamTemplate(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val recommendedDailyGoal: Int? = null,
    val subjects: List<TemplateSubject> = emptyList(),
)

@Immutable
data class TemplateSubject(
    val name: String = "",
    val color: String = "#0ea5e9",
    val chapters: List<TemplateChapter> = emptyList(),
)

@Immutable
data class TemplateChapter(
    val name: String = "",
    val topics: List<String> = emptyList(),
)

@Immutable
data class AutoDistributeResult(
    val message: String? = null,
    val assigned: Int = 0,
    val skipped: Int = 0,
    val plan: StudyPlan? = null,
)

@Immutable
data class UpgradePlannerResult(
    val message: String? = null,
    val plan: StudyPlan? = null,
)

enum class PlannerSection(val label: String) {
    YOUR_EXAMS("Plan"),
    SYLLABUS("Syllabus"),
    CALENDAR("Calendar"),
    PLAN("Home"),
    INSIGHTS("Progress"),
    REVISION("Revision"),
}

data class PlannerFeatureRegistryItem(
    val id: String,
    val label: String,
    val endpoint: String,
    val androidEntryPoints: List<String>,
    val destructive: Boolean = false,
)

val plannerFeatureRegistry = listOf(
    PlannerFeatureRegistryItem("list_plans", "Fetch plans", "GET /api/plans", listOf("StudyPlansScreen.Load")),
    PlannerFeatureRegistryItem("create_plan", "Create custom plan", "POST /api/plans", listOf("QuickStart.CustomPlan.Create")),
    PlannerFeatureRegistryItem("create_plan_from_template", "Create plan from template", "POST /api/plans/from-template", listOf("QuickStart.TemplateConfig.Generate")),
    PlannerFeatureRegistryItem("delete_plan", "Delete plan", "DELETE /api/plans/:planId", listOf("StudyPlansScreen.CardMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("plan_detail", "Open plan", "GET /api/plans/:planId", listOf("PlannerHome.Load")),
    PlannerFeatureRegistryItem("update_plan", "Update plan settings", "PATCH /api/plans/:planId", listOf("PlanTab.Save")),
    PlannerFeatureRegistryItem("calendar", "Load calendar", "GET /api/plans/:planId/calendar", listOf("TodayTab.Load", "CalendarTab.Load")),
    PlannerFeatureRegistryItem("analytics", "Load insights", "GET /api/plans/:planId/analytics", listOf("InsightsTab.Load")),
    PlannerFeatureRegistryItem("auto_schedule", "Create planner calendar", "POST /api/plans/:planId/auto-distribute", listOf("TodayTab.BuildSchedule", "PlanTab.CreatePlannerCalendar")),
    PlannerFeatureRegistryItem("add_subject", "Add subject", "POST /api/plans/:planId/subjects", listOf("SyllabusTab.AddSubject")),
    PlannerFeatureRegistryItem("rename_subject", "Rename subject", "PATCH /api/plans/:planId/subjects/:subjectId", listOf("SyllabusTab.SubjectMenu.Rename")),
    PlannerFeatureRegistryItem("delete_subject", "Delete subject", "DELETE /api/plans/:planId/subjects/:subjectId", listOf("SyllabusTab.SubjectMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("add_chapter", "Add chapter", "POST /api/plans/:planId/subjects/:subjectId/chapters", listOf("SyllabusTab.AddChapter")),
    PlannerFeatureRegistryItem("rename_chapter", "Rename chapter", "PATCH /api/plans/:planId/subjects/:subjectId/chapters/:chapterId", listOf("SyllabusTab.ChapterMenu.Rename")),
    PlannerFeatureRegistryItem("delete_chapter", "Delete chapter", "DELETE /api/plans/:planId/subjects/:subjectId/chapters/:chapterId", listOf("SyllabusTab.ChapterMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("add_topic", "Add topic", "POST /api/plans/:planId/subjects/:subjectId/chapters/:chapterId/topics", listOf("SyllabusTab.AddTopic")),
    PlannerFeatureRegistryItem("update_topic", "Update topic", "PATCH /api/plans/:planId/topics/:topicId", listOf("TopicSheet.Save", "TodayTab.StatusButtons", "CalendarTab.StatusButtons")),
    PlannerFeatureRegistryItem("delete_topic", "Delete topic", "DELETE /api/plans/:planId/topics/:topicId", listOf("TopicSheet.Delete"), destructive = true),
    PlannerFeatureRegistryItem("bulk_add", "Bulk add topics", "POST /api/plans/:planId/subjects/.../topics", listOf("SyllabusTab.BulkAdd")),
    PlannerFeatureRegistryItem("file_import", "Import syllabus file", "POST /api/syllabus/import", listOf("BulkAddSheet.ImportFile")),
)
