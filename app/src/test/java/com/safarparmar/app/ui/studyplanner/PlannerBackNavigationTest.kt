package com.safarparmar.app.ui.studyplanner

import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import org.junit.Assert.*
import org.junit.Test

class PlannerBackNavigationTest {
    @Test fun openingExamPlannerThenBackExitsWithoutVisitingHome() {
        assertNull(plannerBackSection(PlannerSection.YOUR_EXAMS))
    }
    @Test fun eachPlannerTabReturnsToExamListThenExits() {
        PlannerSection.values().filter { it != PlannerSection.YOUR_EXAMS }.forEach { tab ->
            val destination = plannerBackSection(tab)
            assertEquals(PlannerSection.YOUR_EXAMS, destination)
            assertNull(plannerBackSection(destination!!))
        }
    }
}
