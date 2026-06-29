package com.safarparmar.app.ui.studyplanner

import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.TopicStatus

interface PlannerActions {
    fun setSection(section: PlannerSection)
    /**
     * Handles an internal back-press.
     * Returns true  → the ViewModel consumed it (moved to previous section or closed the plan).
     * Returns false → no internal state to pop; caller should let the NavController handle back.
     */
    fun navigateBack(): Boolean
    fun clearTransient()
    fun setError(message: String)
    fun refreshPlans()
    fun openPlan(planId: String)
    fun closePlan()
    fun createPlan(title: String, examType: String?, examDate: String?, dailyGoal: Int, offDays: List<Int>, syllabusText: String? = null)
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
    fun updateTopic(
        topicId: String,
        status: TopicStatus? = null,
        name: String? = null,
        plannedDate: String? = null,
        notes: String? = null,
    )
    fun deleteTopic(topicId: String)
    fun autoDistribute(includeRevision: Boolean, lockExisting: Boolean)
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
