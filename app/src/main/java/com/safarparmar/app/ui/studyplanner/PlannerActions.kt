package com.safarparmar.app.ui.studyplanner

import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.TopicStatus

enum class StudyPlannerTab {
    TODAY,
    OVERDUE,
    UPCOMING,
    REVISION,
    COMPLETED
}

interface PlannerActions {
    fun setSection(section: PlannerSection)
    fun setPlanTab(tab: StudyPlannerTab)
    /**
     * Handles an internal back-press.
     * Returns true  → the ViewModel consumed it (moved to previous section or closed the plan).
     * Returns false → no internal state to pop; caller should let the NavController handle back.
     */
    fun navigateBack(): Boolean
    fun clearTransient()
    fun undoRollover()
    fun undoDelete()
    /** Persists the plan's Flexible/Strict scheduling mode ("flex" or "strict"). */
    fun setPlanningMode(mode: String)
    /** Persists the "how do you like to study" default ("interleaved" or "sequential"). */
    fun setPreferredStudyStrategy(strategy: String)
    fun setError(message: String)
    fun refreshPlans()
    fun openPlan(planId: String)
    fun closePlan()
    fun createPlan(
        title: String,
        examType: String?,
        examDate: String?,
        dailyGoal: Int,
        offDays: List<Int>,
        syllabusText: String? = null,
        openAiImport: Boolean = false,
    )
    fun createFromTemplate(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>)
    fun createFromTemplateOrLocal(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>)
    fun deletePlan(planId: String)
    fun updatePlan(request: UpdatePlanRequest)
    fun addSubject(name: String)
    fun renameSubject(subjectId: String, name: String)
    fun deleteSubject(subjectId: String)
    fun addChapter(subjectId: String, name: String)
    fun renameChapter(subjectId: String, chapterId: String, name: String)
    fun deleteChapter(subjectId: String, chapterId: String)
    fun addTopic(subjectId: String, chapterId: String, name: String)
    /**
     * Adds a brand-new, user-typed topic straight into Today's Study Plan. It is
     * filed under an auto-created "Extra Topics" subject/chapter and pinned to
     * today, so it never disrupts the built schedule for future days.
     */
    fun addCustomTopicToToday(name: String)
    fun updateTopic(
        topicId: String,
        status: TopicStatus? = null,
        name: String? = null,
        plannedDate: String? = null,
        notes: String? = null,
        pinned: Boolean? = null,
    )
    /**
     * Sets a topic to REVISION_NEEDED and schedules [revisionDates] on it.
     * The first date becomes the new plannedDate so the topic reappears in
     * Today's feed on that day. All dates are stored as revisionReminderDates
     * on the server for record-keeping.
     */
    fun markForRevision(
        topicId: String,
        revisionDates: List<String>,
        revisionScheduleType: String? = null,
    )
    fun cancelRevision(topicId: String)
    fun deleteTopic(topicId: String)
    fun autoDistribute(lockExisting: Boolean, overloadMode: String? = null, strategy: String? = null)
    fun reorderSyllabus(
        subjectIds: List<String>? = null,
        chapterIdsBySubjectId: Map<String, List<String>>? = null,
        topicIdsByChapterId: Map<String, List<String>>? = null,
    )
    fun clearPendingAiImport()
    fun markOnboardingStepDone(step: String)
    fun clearFutureDates()
    fun moveTopicsToDate(topicIds: List<String>, date: String)
    fun clearTopicDates(topicIds: List<String>)
    fun swapTopicDates(firstTopicId: String, secondTopicId: String)
    fun replaceTopicToday(currentTopicId: String, replacementTopicId: String, todayDate: String)
    fun resetPlan()
    fun bulkAdd(subjectId: String, chapterId: String, text: String)
    fun importFullSyllabusFromTxt(text: String, mode: String = "merge")
    fun structureSyllabusPreview(rawText: String, language: String? = null)
    fun updateStructuredPreview(preview: com.safarparmar.app.data.remote.api.StructuredSyllabusPreview?)
    fun importStructuredSyllabus()
}
