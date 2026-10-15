package com.safarparmar.app.ui.nishtha.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.safarparmar.app.ui.tutorial.FeatureTutorialState
import com.safarparmar.app.util.isGoalCompleted
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GoalsTutorialTest {
    @Test fun fourGuidesKeepTheMainWalkthroughShort() {
        assertEquals(4, GoalsTutorialLesson.entries.size)
        assertTrue(GoalsTutorialLesson.entries.all { it.scenes.size <= 5 })
        assertEquals(listOf(GoalsTutorialScene.ADD, GoalsTutorialScene.TITLE, GoalsTutorialScene.CHECK,
            GoalsTutorialScene.DONE, GoalsTutorialScene.HISTORY), GoalsTutorialLesson.CORE.scenes.toList())
        assertEquals(listOf(GoalsTutorialScene.DATE, GoalsTutorialScene.UPCOMING, GoalsTutorialScene.DAILY,
            GoalsTutorialScene.REPEAT), GoalsTutorialLesson.SCHEDULE.scenes.toList())
    }

    @Test fun loadingSavingAndPendingActionsBlockPractice() {
        for (loading in listOf(false, true)) for (saving in listOf(false, true)) for (pending in listOf(false, true)) {
            assertEquals(!loading && !saving && !pending, goalsTutorialEligible(loading, saving, pending))
        }
    }
    @Test fun everyLessonSupportsBackAndDeterministicReplay() {
        GoalsTutorialLesson.entries.forEach { lesson ->
            val state = FeatureTutorialState(lesson.steps())
            assertEquals(state.steps.size, state.steps.map { it.id }.distinct().size)
            assertTrue(state.steps.all { it.instructionRes != 0 && it.messageRes != 0 })
            state.previous(); assertEquals(0, state.index)
            repeat(state.steps.size) { state.next() }
            assertTrue(state.isLast)
            state.previous(); assertFalse(state.isLast)
            state.restart(); assertEquals(0, state.index)
        }
    }
    @Test fun skippedLessonsDoNotBecomeCompleted() {
        val model = GoalsTutorialViewModel()
        model.start(GoalsTutorialLesson.CORE); model.state!!.next(); model.close()
        assertFalse(model.isOpen); assertNull(model.completedLesson)
        model.start(GoalsTutorialLesson.CORE); assertEquals(0, model.state!!.index)
        model.complete(); assertTrue(model.isOpen); assertNull(model.state)
        assertEquals(GoalsTutorialLesson.CORE, model.completedLesson)
        model.acknowledgeCompletion(); assertNull(model.completedLesson)
    }
    @Test fun configurationRecreationRetainsStepButNewProcessStartsClosed() {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GoalsTutorialViewModel() as T
        }
        val original = ViewModelProvider(store, factory)[GoalsTutorialViewModel::class.java]
        original.start(GoalsTutorialLesson.CORE); original.state!!.next()
        val retained = ViewModelProvider(store, factory)[GoalsTutorialViewModel::class.java]
        assertSame(original, retained); assertEquals(1, retained.state!!.index)
        assertFalse(GoalsTutorialViewModel().isOpen)
        store.clear()
    }
    @Test fun samplesHaveTheCorrectDatesAndSeparateManualAndFocusTime() {
        val today = LocalDate.of(2026, 10, 14)
        fun sample(scene: GoalsTutorialScene) = GoalsTutorialDemoState(scene, today).sampleGoal("Study", "Read notes")
        assertEquals(0, GoalsTutorialDemoState(GoalsTutorialScene.ADD, today).totalToday)
        assertEquals(0, GoalsTutorialDemoState(GoalsTutorialScene.UPCOMING, today).totalToday)
        assertEquals(1, GoalsTutorialDemoState(GoalsTutorialScene.TODAY, today).totalToday)
        assertEquals(today.toString(), sample(GoalsTutorialScene.TODAY).scheduledDate)
        assertFalse(sample(GoalsTutorialScene.TODAY).isGoalCompleted())
        assertEquals(today.plusDays(1).toString(), sample(GoalsTutorialScene.UPCOMING).scheduledDate)
        assertEquals(today.minusDays(1).toString(), sample(GoalsTutorialScene.MISSED).scheduledDate)
        assertTrue(sample(GoalsTutorialScene.HISTORY).isGoalCompleted())
        assertEquals(25, sample(GoalsTutorialScene.HISTORY).studiedMinutes)
        val focused = sample(GoalsTutorialScene.LINKED_HISTORY)
        assertTrue(focused.completedViaFocus); assertEquals(0, focused.studiedMinutes)
    }
}
