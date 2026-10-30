package com.safarparmar.app.ui.studyplanner.create

import org.junit.Assert.*
import org.junit.Test

class PlannerCreationPhasesTest {
    @Test fun `every existing conditional step has an understandable phase`() {
        assertEquals(0, creationPhase(CreatePlanStep.ManualTopicTree))
        assertEquals(0, creationPhase(CreatePlanStep.SavedSyllabusPicker))
        assertEquals(1, creationPhase(CreatePlanStep.DeepFocusOrder))
        assertEquals(1, creationPhase(CreatePlanStep.MixedBagSubjectPicker))
        assertEquals(1, creationPhase(CreatePlanStep.ChapterRating))
        assertEquals(2, creationPhase(CreatePlanStep.Preview))
        assertEquals(2, creationPhase(CreatePlanStep.DailyTopics))
        assertTrue(CreatePlanStep.entries.all { creationPhase(it) in 0..2 })
    }
}
