package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R
import com.safarparmar.app.util.Resource
import org.junit.Assert.*
import org.junit.Test

class BatchNoticeTest {
    @Test fun codeBasedConflictsDoNotDependOnServerLanguage() {
        assertEquals(BatchNotice(R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again),
            batchErrorNotice(Resource.Error<Unit>("Changed on another device", 409, "PLAN_CONFLICT")))
    }

    @Test fun partialSaveWarningKeepsItsDistinctMeaning() {
        val message = "Some plans changed during saving and were left untouched. Other selected plans were saved. Refresh to review them."
        val notice = batchErrorNotice(Resource.Error<Unit>(message, 409, "PLAN_CONFLICT"))
        assertEquals(R.string.toppers_batch_some_plans_changed_during_saving_and_were_left_untouched_other_se, notice.resource)
        assertNotEquals(R.string.toppers_batch_your_plan_has_changed_refresh_and_try_again, notice.resource)
    }

    @Test fun legacyCompatibilityErrorsStayActionable() {
        assertEquals(BatchNotice(R.string.toppers_batch_reminders_are_unavailable_turn_off_remind_me_to_save_the_date),
            batchErrorNotice(Resource.Error<Unit>("Reminders are unavailable. Turn off Remind me to save the date.")))
        assertEquals(BatchNotice(R.string.toppers_batch_this_option_is_not_available),
            batchErrorNotice(Resource.Error<Unit>("This option is not available", 404)))
    }

    @Test fun unexpectedErrorsDoNotExposeServerOrExceptionText() {
        val raw = "Unexpected storage exception details"
        assertEquals(BatchNotice(R.string.toppers_batch_request_failed),
            batchErrorNotice(Resource.Error<Unit>(raw, 500)))
        assertEquals(BatchNotice(R.string.toppers_batch_something_went_wrong_please_try_again),
            batchErrorNotice(Resource.Error<Unit>(raw)))
    }

    @Test fun noticesRetainUnformattedDatesAndSubjects() {
        val date = BatchDateArgument("2026-10-06")
        val subject = BatchSubjectArgument(BatchSubject(key = "english", name = "English"))
        assertEquals(listOf(date), BatchNotice(R.string.toppers_batch_lecture_planned_for_count, date).arguments)
        assertEquals(listOf(subject), BatchNotice(R.string.toppers_batch_count_restored, subject).arguments)
    }
}
