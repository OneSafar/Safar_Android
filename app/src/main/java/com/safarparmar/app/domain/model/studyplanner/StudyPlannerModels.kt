package com.safarparmar.app.domain.model.studyplanner

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

typealias CalendarMap = Map<String, List<CalendarTopicItem>>

/**
 * A topic is either not done, done, or waiting to be revised.
 *
 * "in_progress" was removed along with partial completion — nothing set it once
 * the drag-to-fill slider went away, and a third "started" state the student
 * could never reach was just noise. Topics stored with that status before the
 * change deserialise to [TODO] via the `alternate` below, so old plans keep
 * working without a data migration.
 */
enum class TopicStatus(val wireValue: String, val label: String) {
    @SerializedName(value = "todo", alternate = ["in_progress"])
    TODO("todo", "Not done"),

    @SerializedName("done")
    DONE("done", "Done"),

    @SerializedName("revision_needed")
    REVISION_NEEDED("revision_needed", "To Revise"),
}

/** Effort size of a topic. Missing on the wire = MEDIUM. Points: 1 / 2 / 4. */
enum class TopicSize(val wireValue: String, val points: Int, val label: String) {
    @SerializedName("small")
    SMALL("small", 1, "Small"),

    @SerializedName("medium")
    MEDIUM("medium", 2, "Medium"),

    @SerializedName("big")
    BIG("big", 4, "Big"),
}

/** Chapter-level effort rating; topics without their own size inherit it. */
enum class ChapterDifficulty(val wireValue: String, val label: String) {
    @SerializedName("easy")
    EASY("easy", "Easy"),

    @SerializedName("normal")
    NORMAL("normal", "Normal"),

    @SerializedName("tough")
    TOUGH("tough", "Tough"),
}

/**
 * Mirrors the server's effectiveTopicSize (plan.model.ts): the topic's own
 * size, else the chapter's rating mapped (tough→big, easy→small,
 * normal→medium), else medium. Keep the two implementations identical —
 * the server owns scheduling; this is display-only math.
 */
fun StudyTopic.effectiveSize(chapter: StudyChapter? = null): TopicSize =
    size ?: when (chapter?.difficulty) {
        ChapterDifficulty.TOUGH -> TopicSize.BIG
        ChapterDifficulty.EASY -> TopicSize.SMALL
        else -> TopicSize.MEDIUM
    }

/** Effort points for a topic: small=1, medium=2, big=4. */
fun StudyTopic.effortPoints(chapter: StudyChapter? = null): Int = effectiveSize(chapter).points

/**
 * Completion as a percentage. Partial completion (drag-to-fill) is HIDDEN for
 * this release: this deliberately ignores any stored [progressPercent] and
 * reports pure done/not-done, so a stray partial value written during testing
 * can never leak into progress rings, weighted rollups or the syllabus. The
 * field is kept on the model so partial progress can be re-enabled next update
 * by restoring the `progressPercent?.coerceIn(0, 100) ?:` prefix here.
 */
fun StudyTopic.progressPercentValue(): Int =
    if (status == TopicStatus.DONE) 100 else 0

/** Remaining effort: a half-done big topic counts as 2 points, not 4. */
fun StudyTopic.remainingPoints(chapter: StudyChapter? = null): Float =
    effortPoints(chapter) * (100 - progressPercentValue()) / 100f

/** Converts internal points to the user-facing "topics" number (1 topic = 2 points). */
fun pointsToTopicEquivalents(points: Float): Float = points / 2f

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
    // Older plans can omit this field completely.
    val revisionCompletedDates: List<String>? = emptyList(),
    /**
     * Links each planned revision day to the day the student actually finished
     * it. These can differ when late work is completed on a later day.
     */
    // Nullable for plans saved before this field existed. Callers use orEmpty()
    // so old server data can open Progress without crashing.
    val revisionCompletionLog: List<RevisionCompletion>? = emptyList(),
    val revisionScheduleType: String? = null,
    /** Effort size; null = inherit from chapter difficulty (default medium). */
    val size: TopicSize? = null,
    /** Partial completion 0–100; null = (done ? 100 : 0). */
    val progressPercent: Int? = null,
)

@Immutable
data class RevisionCompletion(
    val sessionDate: String = "",
    val completedDate: String = "",
)

@Immutable
data class StudyChapter(
    val id: String = "",
    val name: String = "",
    val difficulty: ChapterDifficulty? = null,
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
    /** Null on plans made before this existed — treated as weighted so nothing
     *  disappears for existing users. False = "Same every day", where rating a
     *  chapter would silently break the equal-days promise. */
    val weightedPlanning: Boolean? = null,
    // Tolerates a decimal from the server; see LenientIntDeserializer.
    @JsonAdapter(com.safarparmar.app.data.remote.LenientIntDeserializer::class)
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
    /** Effective size, already resolved server-side (topic's own, else its
     *  chapter's rating, else medium). Null only from older servers. */
    val size: TopicSize? = null,
    val progressPercent: Int? = null,
) {
    /** Effort points for this scheduled item; medium (2) when the server sent no size. */
    val points: Int get() = (size ?: TopicSize.MEDIUM).points
}

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

/**
 * A topic inside a bundled exam template. The server sends either a bare name
 * ("Kinematics") for unweighted templates, or a hand-weighted object
 * ({"name": "Kinematics", "size": "big"}) for the ones that carry effort sizes.
 * Both forms are parsed by TemplateTopicDeserializer — do NOT change this to a
 * plain String again, it will crash on weighted templates.
 */
@Immutable
data class TemplateTopic(
    val name: String = "",
    val size: TopicSize? = null,
)

@Immutable
data class TemplateChapter(
    val name: String = "",
    val topics: List<TemplateTopic> = emptyList(),
) {
    /**
     * The chapter's effort rating implied by its topics' sizes — templates are
     * hand-weighted a whole chapter at a time, so this is what pre-fills the
     * "Rate your chapters" step. Null when the template carries no weights.
     */
    val impliedDifficulty: ChapterDifficulty?
        get() {
            val sizes = topics.mapNotNull { it.size }
            if (sizes.isEmpty() || sizes.size != topics.size) return null
            return when {
                sizes.all { it == TopicSize.BIG } -> ChapterDifficulty.TOUGH
                sizes.all { it == TopicSize.SMALL } -> ChapterDifficulty.EASY
                sizes.all { it == TopicSize.MEDIUM } -> ChapterDifficulty.NORMAL
                else -> null
            }
        }
}

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
