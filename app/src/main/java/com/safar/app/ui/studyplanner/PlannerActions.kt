package com.safar.app.ui.studyplanner

import com.safar.app.data.remote.api.UpdatePlanRequest
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.PremiumReason
import com.safar.app.domain.model.studyplanner.TopicStatus
import okhttp3.MultipartBody

interface PlannerActions {
    fun setSection(section: PlannerSection)
    fun clearTransient()
    fun clearSyllabusImportDraft()
    fun setError(message: String)
    fun showPremium(reason: PremiumReason)
    fun refreshPlans()
    fun openPlan(planId: String)
    fun closePlan()
    fun createPlan(title: String, examType: String?, examDate: String?, dailyGoal: Int, offDays: List<Int>)
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
    fun clearFutureDates()
    fun moveTopicsToDate(topicIds: List<String>, date: String)
    fun clearTopicDates(topicIds: List<String>)
    fun resetPlan()
    fun bulkAdd(subjectId: String, chapterId: String, text: String)
    fun importFullSyllabusFromTxt(text: String)
    fun importSyllabusFile(file: MultipartBody.Part, fileName: String?)
    fun upgradePlan()
}
