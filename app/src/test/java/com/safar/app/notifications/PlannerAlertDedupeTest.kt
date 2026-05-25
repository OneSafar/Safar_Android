package com.safar.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class PlannerAlertDedupeTest {

    @Test
    fun `overdue alert key is stable per plan and date`() {
        val date = LocalDate.of(2026, 5, 20)

        assertEquals(
            "alert_plan1_overdue_2026-05-20",
            PlannerAlertDedupe.overdueKey("plan1", date),
        )
        assertEquals(
            PlannerAlertDedupe.overdueKey("plan1", date),
            PlannerAlertDedupe.overdueKey("plan1", date),
        )
    }

    @Test
    fun `exam countdown key is sent once per milestone`() {
        assertEquals("alert_plan1_exam_7d", PlannerAlertDedupe.examCountdownKey("plan1", 7))
        assertNotEquals(
            PlannerAlertDedupe.examCountdownKey("plan1", 7),
            PlannerAlertDedupe.examCountdownKey("plan1", 1),
        )
    }

    @Test
    fun `pace warning key changes by week`() {
        val weekOne = PlannerAlertDedupe.paceWarningKey("plan1", LocalDate.of(2026, 5, 20))
        val weekTwo = PlannerAlertDedupe.paceWarningKey("plan1", LocalDate.of(2026, 5, 27))

        assertNotEquals(weekOne, weekTwo)
    }
}
