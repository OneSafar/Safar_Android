package com.safarparmar.app.ui.studyplanner.analytics

import android.util.Log
import com.safarparmar.app.BuildConfig

object StudyPlannerAnalytics {
    const val STUDY_PLANNER_OPENED = "study_planner_opened"
    const val PLAN_CREATED_CUSTOM = "plan_created_custom"
    const val PLAN_CREATED_TEMPLATE = "plan_created_template"
    const val SYLLABUS_IMPORT_STARTED = "syllabus_import_started"
    const val SYLLABUS_IMPORT_SUCCEEDED = "syllabus_import_succeeded"
    const val SYLLABUS_IMPORT_FAILED = "syllabus_import_failed"
    const val TOPIC_COMPLETED = "topic_completed"
    const val DASHBOARD_TODAY_CARD_VIEWED = "dashboard_today_card_viewed"
    const val DASHBOARD_TODAY_CARD_CLICKED = "dashboard_today_card_clicked"
    const val PLANNER_NOTIFICATION_OPENED = "planner_notification_opened"
    const val PREMIUM_GATE_VIEWED = "premium_gate_viewed"
    const val SYLLABUS_AI_STRUCTURE_STARTED = "syllabus_ai_structure_started"
    const val SYLLABUS_AI_STRUCTURE_SUCCEEDED = "syllabus_ai_structure_succeeded"
    const val SYLLABUS_AI_STRUCTURE_FAILED = "syllabus_ai_structure_failed"
    const val SYLLABUS_AI_PREVIEW_EDITED = "syllabus_ai_preview_edited"
    const val SYLLABUS_AI_PREVIEW_CONFIRMED = "syllabus_ai_preview_confirmed"
    const val SYLLABUS_AI_IMPORT_STARTED = "syllabus_ai_import_started"
    const val SYLLABUS_AI_IMPORT_SUCCEEDED = "syllabus_ai_import_succeeded"
    const val SYLLABUS_AI_IMPORT_FAILED = "syllabus_ai_import_failed"
    const val SYLLABUS_MANUAL_FORMAT_USED = "syllabus_manual_format_used"
    const val SYLLABUS_AI_RATE_LIMITED = "syllabus_ai_rate_limited"
    const val SYLLABUS_AI_TOPIC_LIMIT_EXCEEDED = "syllabus_ai_topic_limit_exceeded"

    fun track(event: String, properties: Map<String, String> = emptyMap()) {
        runCatching {
            if (BuildConfig.DEBUG) {
                Log.d("SAFAR_PLANNER_ANALYTICS", "$event $properties")
            }
        }
    }
}
