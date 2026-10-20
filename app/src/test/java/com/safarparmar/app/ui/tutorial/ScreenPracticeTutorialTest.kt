package com.safarparmar.app.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test

class ScreenPracticeTutorialTest {
    @Test fun tenFeaturesHaveShortIndependentLessons() {
        val features = TitliFeature.entries.toSet() - setOf(TitliFeature.EKAGRA, TitliFeature.GOALS, TitliFeature.YOUTUBE)
        assertEquals(features, PracticeLesson.entries.map { it.feature }.toSet())
        features.forEach { feature ->
            val lessons = PracticeLesson.forFeature(feature)
            assertTrue(lessons.size in 1..2)
            assertTrue(lessons.all { it.scenes.size in 3..5 })
        }
        assertEquals(PracticeScene.entries.toSet(), PracticeLesson.entries.flatMap { it.scenes.toList() }.toSet())
        assertEquals(listOf(PracticeScene.PLANNER_EXAMS, PracticeScene.PLANNER_HOME, PracticeScene.PLANNER_SYLLABUS,
            PracticeScene.PLANNER_CALENDAR, PracticeScene.PLANNER_PROGRESS), PracticeLesson.PLANNER_CORE.scenes.toList())
    }

    @Test fun everyLessonCanMoveBothWaysAndReplayFromTheBeginning() {
        PracticeLesson.entries.forEach { lesson ->
            val model = ScreenPracticeViewModel().apply { start(lesson) }
            val state = model.state!!
            assertEquals(lesson.steps().size, state.steps.map { it.id }.distinct().size)
            state.previous(); assertEquals(0, state.index)
            lesson.scenes.forEachIndexed { index, scene ->
                assertEquals(scene.name, state.currentStep.id)
                assertEquals(scene.target, state.currentStep.targetId)
                if (index > 0) { state.previous(); assertEquals(index - 1, state.index); state.next() }
                state.next()
            }
            assertTrue(state.isLast)
            model.close(); model.start(lesson)
            assertEquals(0, model.state!!.index)
            assertNull(model.completedLesson)
        }
    }

    @Test fun completionAndSkipAreSeparateAndHelpNeverCompletesALesson() {
        val model = ScreenPracticeViewModel()
        PracticeLesson.entries.forEach { lesson ->
            model.start(lesson); model.state!!.next(); model.open()
            assertNull(model.state); assertNull(model.completedLesson)
            model.start(lesson); model.close()
            assertFalse(model.isOpen); assertNull(model.completedLesson)
            model.start(lesson); model.complete()
            assertTrue(model.isOpen); assertNull(model.state); assertEquals(lesson, model.completedLesson)
            model.acknowledgeCompletion(); assertNull(model.completedLesson)
        }
    }

    @Test fun rotationRetainsStepWhileNewProcessStartsWithPracticeClosed() {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ScreenPracticeViewModel() as T
        }
        val before = ViewModelProvider(store, factory)[ScreenPracticeViewModel::class.java]
        before.start(PracticeLesson.DHYAN_CORE); before.state!!.next()
        val after = ViewModelProvider(store, factory)[ScreenPracticeViewModel::class.java]
        assertSame(before, after); assertEquals(1, after.state!!.index)
        assertFalse(ScreenPracticeViewModel().isOpen)
        store.clear()
    }
}
