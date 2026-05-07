package com.safar.app.domain.model.studyplanner

import com.google.gson.annotations.SerializedName

typealias CalendarMap = Map<String, List<CalendarTopicItem>>

enum class TopicStatus(val wireValue: String, val label: String) {
    @SerializedName("todo")
    TODO("todo", "Todo"),

    @SerializedName("in_progress")
    IN_PROGRESS("in_progress", "In Progress"),

    @SerializedName("done")
    DONE("done", "Done"),

    @SerializedName("revision_needed")
    REVISION_NEEDED("revision_needed", "Revision"),
}

data class StudyPlannerFeatureFlags(
    val isPremium: Boolean = false,
    val unlockedAt: String? = null,
)

data class StudyTopic(
    val id: String = "",
    val name: String = "",
    val status: TopicStatus = TopicStatus.TODO,
    val plannedDate: String? = null,
    val completedDate: String? = null,
    val notes: String? = null,
)

data class StudyChapter(
    val id: String = "",
    val name: String = "",
    val topics: List<StudyTopic> = emptyList(),
)

data class StudySubject(
    val id: String = "",
    val name: String = "",
    val color: String = "#0ea5e9",
    val weeklyTarget: Int? = null,
    val monthlyTarget: Int? = null,
    val chapters: List<StudyChapter> = emptyList(),
)

data class PlanProgress(
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val inProgressTopics: Int = 0,
    val revisionTopics: Int = 0,
    val completionPercent: Int = 0,
    val remainingPercent: Int = 100,
    val bySubject: List<SubjectProgress> = emptyList(),
)

data class SubjectProgress(
    val subjectId: String = "",
    val subjectName: String = "",
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val completionPercent: Int = 0,
    val byChapter: List<ChapterProgress> = emptyList(),
)

data class ChapterProgress(
    val chapterId: String = "",
    val chapterName: String = "",
    val totalTopics: Int = 0,
    val doneTopics: Int = 0,
    val completionPercent: Int = 0,
)

data class StudyPlan(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val examType: String? = null,
    val examDate: String? = null,
    val description: String? = null,
    val dailyGoal: Int? = null,
    val offDays: List<Int> = emptyList(),
    val subjects: List<StudySubject> = emptyList(),
    val features: StudyPlannerFeatureFlags = StudyPlannerFeatureFlags(),
    val templateId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val progress: PlanProgress? = null,
    val subjectCount: Int? = null,
    val completionPercent: Int? = null,
    val totalTopics: Int? = null,
)

data class CalendarTopicItem(
    val topicId: String = "",
    val topicName: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectColor: String = "#0ea5e9",
    val status: TopicStatus = TopicStatus.TODO,
)

data class HeatmapPoint(
    val date: String = "",
    val count: Int = 0,
)

data class PlannerAnalytics(
    val progress: PlanProgress = PlanProgress(),
    val heatmap: List<HeatmapPoint> = emptyList(),
)

data class ExamTemplateSummary(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val recommendedDailyGoal: Int? = null,
    val subjectCount: Int? = null,
    val topicCount: Int? = null,
)

data class ExamTemplate(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val recommendedDailyGoal: Int? = null,
    val subjects: List<TemplateSubject> = emptyList(),
)

data class TemplateSubject(
    val name: String = "",
    val color: String = "#0ea5e9",
    val chapters: List<TemplateChapter> = emptyList(),
)

data class TemplateChapter(
    val name: String = "",
    val topics: List<String> = emptyList(),
)

data class AutoDistributeResult(
    val message: String? = null,
    val assigned: Int = 0,
    val skipped: Int = 0,
    val plan: StudyPlan? = null,
)

data class UpgradePlannerResult(
    val message: String? = null,
    val plan: StudyPlan? = null,
)

enum class PlannerSection(val label: String) {
    TODAY("Today"),
    SYLLABUS("Syllabus"),
    CALENDAR("Calendar"),
    PLAN("Plan"),
    INSIGHTS("Insights"),
}

enum class PremiumReason(val title: String, val description: String) {
    TOPIC_LIMIT("Premium limit reached", "Free plans support a limited number of topics. Unlock Premium for larger syllabi."),
    AUTO_SCHEDULE("Auto-schedule is Premium", "Build or rebuild your planner calendar in one tap with Premium."),
    BULK_ADD("Bulk Add is Premium", "Import many syllabus topics at once with Premium."),
    RESCHEDULE("Reschedule is Premium", "Move or rebuild future study dates with Premium."),
    TEMPLATE("Template is Premium", "Unlock the full exam template library with Premium."),
}

data class PlannerFeatureRegistryItem(
    val id: String,
    val label: String,
    val endpoint: String,
    val androidEntryPoints: List<String>,
    val premiumReason: PremiumReason? = null,
    val destructive: Boolean = false,
)

val plannerFeatureRegistry = listOf(
    PlannerFeatureRegistryItem("list_plans", "Fetch plans", "GET /api/plans", listOf("StudyPlansScreen.Load")),
    PlannerFeatureRegistryItem("create_plan", "Create custom plan", "POST /api/plans", listOf("QuickStart.CustomPlan.Create")),
    PlannerFeatureRegistryItem("create_plan_from_template", "Create plan from template", "POST /api/plans/from-template", listOf("QuickStart.TemplateConfig.Generate"), PremiumReason.TEMPLATE),
    PlannerFeatureRegistryItem("delete_plan", "Delete plan", "DELETE /api/plans/:planId", listOf("StudyPlansScreen.CardMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("plan_detail", "Open plan", "GET /api/plans/:planId", listOf("PlannerHome.Load")),
    PlannerFeatureRegistryItem("update_plan", "Update plan settings", "PATCH /api/plans/:planId", listOf("PlanTab.Save")),
    PlannerFeatureRegistryItem("calendar", "Load calendar", "GET /api/plans/:planId/calendar", listOf("TodayTab.Load", "CalendarTab.Load")),
    PlannerFeatureRegistryItem("analytics", "Load insights", "GET /api/plans/:planId/analytics", listOf("InsightsTab.Load")),
    PlannerFeatureRegistryItem("auto_schedule", "Create planner calendar", "POST /api/plans/:planId/auto-distribute", listOf("TodayTab.BuildSchedule", "PlanTab.CreatePlannerCalendar"), PremiumReason.AUTO_SCHEDULE),
    PlannerFeatureRegistryItem("add_subject", "Add subject", "POST /api/plans/:planId/subjects", listOf("SyllabusTab.AddSubject")),
    PlannerFeatureRegistryItem("rename_subject", "Rename subject", "PATCH /api/plans/:planId/subjects/:subjectId", listOf("SyllabusTab.SubjectMenu.Rename")),
    PlannerFeatureRegistryItem("delete_subject", "Delete subject", "DELETE /api/plans/:planId/subjects/:subjectId", listOf("SyllabusTab.SubjectMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("add_chapter", "Add chapter", "POST /api/plans/:planId/subjects/:subjectId/chapters", listOf("SyllabusTab.AddChapter")),
    PlannerFeatureRegistryItem("rename_chapter", "Rename chapter", "PATCH /api/plans/:planId/subjects/:subjectId/chapters/:chapterId", listOf("SyllabusTab.ChapterMenu.Rename")),
    PlannerFeatureRegistryItem("delete_chapter", "Delete chapter", "DELETE /api/plans/:planId/subjects/:subjectId/chapters/:chapterId", listOf("SyllabusTab.ChapterMenu.Delete"), destructive = true),
    PlannerFeatureRegistryItem("add_topic", "Add topic", "POST /api/plans/:planId/subjects/:subjectId/chapters/:chapterId/topics", listOf("SyllabusTab.AddTopic")),
    PlannerFeatureRegistryItem("update_topic", "Update topic", "PATCH /api/plans/:planId/topics/:topicId", listOf("TopicSheet.Save", "TodayTab.StatusButtons", "CalendarTab.StatusButtons")),
    PlannerFeatureRegistryItem("delete_topic", "Delete topic", "DELETE /api/plans/:planId/topics/:topicId", listOf("TopicSheet.Delete"), destructive = true),
    PlannerFeatureRegistryItem("bulk_add", "Bulk add topics", "POST /api/plans/:planId/subjects/.../topics", listOf("SyllabusTab.BulkAdd"), PremiumReason.BULK_ADD),
    PlannerFeatureRegistryItem("file_import", "Import syllabus file", "POST /api/syllabus/import", listOf("BulkAddSheet.ImportFile"), PremiumReason.BULK_ADD),
    PlannerFeatureRegistryItem("upgrade", "Unlock Premium", "POST /api/plans/:planId/upgrade", listOf("PremiumGateSheet.Unlock")),
)
