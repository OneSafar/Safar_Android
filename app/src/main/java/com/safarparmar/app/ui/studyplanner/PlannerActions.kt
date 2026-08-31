package com.safarparmar.app.ui.studyplanner

import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.TopicStatus

enum class StudyPlannerTab {
    TODAY,
    OVERDUE,
    UPCOMING,
    REVISION,
    COMPLETED,
    DAILY_TODO
}

data class TopicSchedulingResult(
    val added: Int,
    val notAdded: Int,
    val failed: Boolean = false,
)

interface PlannerActions {
    fun setSection(section: PlannerSection)
    fun setPlanTab(tab: StudyPlannerTab)
    /** Opens the revision list and records the current planner location for Back. */
    fun openRevisionTopics()
    /** Navigates to Calendar and opens the Missed Topics sheet, e.g. from an
     *  Insights card about overdue/unplanned topics. */
    fun openMissedTopics()
    fun clearPendingOpenMissedTopics()
    fun openUnscheduledTopics()
    fun clearPendingOpenUnscheduledTopics()
    /**
     * Handles an internal back-press.
     * Returns true  → the ViewModel consumed it (moved to previous section or closed the plan).
     * Returns false → no internal state to pop; caller should let the NavController handle back.
     */
    fun navigateBack(): Boolean
    fun clearTransient()
    fun undoRollover()
    fun undoDelete()
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
    fun createFromTemplate(templateId: String, title: String, examDate: String?, dailyGoal: Int, offDays: List<Int>, manualSubjectOrder: Boolean = false)
    /** Clears the one-time "show the subject-order sheet" flag set after a Manual-mode plan creation. */
    fun clearPendingManualSubjectOrder()
    fun deletePlan(planId: String)
    fun saveCurrentSyllabusForReuse()
    fun renamePlan(planId: String, title: String)
    fun updatePlan(request: UpdatePlanRequest)
    fun addSubject(name: String)
    /** Adds several subjects at once (e.g. from a comma-separated "Add Subject" entry),
     *  each assigned a distinct palette colour, in the order given. */
    fun addSubjects(names: List<String>)
    fun renameSubject(subjectId: String, name: String)
    fun deleteSubject(subjectId: String)
    fun addChapter(subjectId: String, name: String)
    /** Adds several chapters at once (e.g. from a comma-separated "Add Chapter" entry),
     *  landing them at the front of the subject in the order given. */
    fun addChapters(subjectId: String, names: List<String>)
    fun renameChapter(subjectId: String, chapterId: String, name: String)
    /** Rates a chapter's effort ("easy" | "normal" | "tough"); null clears it. */
    fun rateChapter(subjectId: String, chapterId: String, difficulty: String?)
    /** Hides the rating rebuild prompt until another chapter rating changes. */
    fun dismissRatingRebuildPrompt()
    /** Rebuilds future dates after rating changes while preserving today's topics. */
    fun rebuildAfterRatingChanges()
    fun deleteChapter(subjectId: String, chapterId: String)
    /** Adds a single topic, then moves it to the front of the chapter's topic list
     *  so the user sees it immediately without scrolling. */
    fun addTopic(subjectId: String, chapterId: String, name: String)
    /** Adds several topics at once (e.g. from a comma-separated "Add Topic" entry),
     *  landing them at the front of the chapter in the order given. */
    fun addTopics(subjectId: String, chapterId: String, names: List<String>)
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
        size: String? = null,
    )
    /** Moves the revision appointment shown on the calendar. This is separate
     * from moving a topic's first-study date. */
    fun changeRevisionDate(topicId: String, oldDate: String, newDate: String)
    fun batchMarkTopicsDone(topicIds: List<String>)
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
    /** Marks one scheduled revision appointment complete, while keeping later
     * appointments in the same revision schedule available to the student. */
    fun completeRevisionForDate(topicId: String, date: String)
    /** Completes a specific spaced-revision session (by its scheduled date),
     * recording it in revisionCompletedDates and advancing to the next session. */
    fun completeRevisionSession(topicId: String, sessionDate: String)
    /** Undoes a specific completed spaced-revision session, returning it to the
     * remaining/upcoming list. */
    fun uncompleteRevisionSession(topicId: String, sessionDate: String)
    fun cancelRevision(topicId: String)
    fun deleteTopic(topicId: String)
    /**
     * Gives dates only to [topicIds], leaving every other topic where it is.
     * Normal mode respects the saved daily goal. [fitAll] is used only after
     * the student agrees to study more each day.
     */
    fun rescheduleMissedTopics(
        topicIds: List<String>,
        fitAll: Boolean = false,
        onResult: (TopicSchedulingResult) -> Unit = {},
    )
    /**
     * [overloadMode] is an escape hatch, not a setting: null (the normal case)
     * means respect the plan's daily goal and report anything that doesn't fit.
     * Passing "flex" lets the engine pack days fuller to make everything fit —
     * reserved for an explicit, informed "schedule it anyway" choice. Study
     * styles deliberately do NOT set it.
     */
    fun autoDistribute(
        lockExisting: Boolean,
        overloadMode: String? = null,
        strategy: String? = null,
        preserveToday: Boolean = false,
    )
    /**
     * Re-plans the schedule after the exam date changed. Reschedules every
     * unfinished, non-pinned topic into the new [today, examDate] window using
     * [strategy] ("interleaved" | "sequential" | "priority_split"). Completed
     * topics and any manually pinned/moved dates are preserved; today's list is
     * kept intact. [prioritySubjectNames] is only used for "priority_split".
     */
    fun rescheduleAfterExamDateChange(
        strategy: String,
        overloadMode: String? = null,
        prioritySubjectNames: List<String> = emptyList(),
        priorityOrderMode: String? = null,
    )
    /**
     * Arms a pending rebuild with the chosen strategy so the user can reorder
     * the syllabus first; the next "Build re-ordered syllabus" tap applies it.
     */
    fun armRebuild(
        strategy: String,
        prioritySubjectNames: List<String> = emptyList(),
        priorityOrderMode: String? = null,
    )
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
    fun finishDay(topicIds: List<String>)
    fun undoFinishDay()
    fun swapTopicDates(firstTopicId: String, secondTopicId: String)
    fun replaceTopicToday(currentTopicId: String, replacementTopicId: String, todayDate: String)
    fun resetPlan()
    fun importFullSyllabusFromTxt(text: String, mode: String = "merge")
    fun structureSyllabusPreview(rawText: String, language: String? = null)
    fun updateStructuredPreview(preview: com.safarparmar.app.data.remote.api.StructuredSyllabusPreview?)
    fun importStructuredSyllabus(mode: String = "merge")
}
