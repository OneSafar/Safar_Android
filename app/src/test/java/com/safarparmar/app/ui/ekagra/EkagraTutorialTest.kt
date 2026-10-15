package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.ui.tutorial.FeatureTutorialState
import org.junit.Assert.*
import org.junit.Test

class EkagraTutorialTest {
    @Test fun fourShortGuidesCoverTheMainTasks() {
        assertEquals(4, EkagraTutorialLesson.entries.size)
        assertTrue(EkagraTutorialLesson.entries.all { it.scenes.size <= 5 })
        assertEquals(listOf(EkagraTutorialScene.MODE, EkagraTutorialScene.SETTINGS_ENTRY,
            EkagraTutorialScene.PERSONALIZE_ENTRY, EkagraTutorialScene.START, EkagraTutorialScene.RUNNING),
            EkagraTutorialLesson.CORE.scenes.toList())
        assertEquals(4, EkagraTutorialLesson.PROTECTION.scenes.size)
        assertTrue(EkagraTutorialLesson.TOGETHER.scenes.toList().containsAll(listOf(EkagraTutorialScene.CIRCLE_JOIN, EkagraTutorialScene.CIRCLE_CREATE)))
    }

    @Test fun onlyIdleReadyTimersWithoutPendingFlowsAreEligible() {
        for (ready in listOf(false, true)) for (active in listOf(false, true)) for (pending in listOf(false, true)) {
            assertEquals(ready && !active && !pending, ekagraTutorialEligible(ready, active, pending))
        }
    }

    @Test fun everyLessonHasStableUniqueStepsAndCanGoBackAndReplay() {
        EkagraTutorialLesson.entries.forEach { lesson ->
            val state = FeatureTutorialState(lesson.steps())
            assertEquals(lesson.scenes.size, state.steps.size)
            assertEquals(state.steps.size, state.steps.map { it.id }.distinct().size)
            state.previous()
            assertEquals(0, state.index)
            repeat(state.steps.size - 1) { state.next() }
            assertTrue(state.isLast)
            state.next()
            assertEquals(state.steps.lastIndex, state.index)
            state.previous()
            assertFalse(state.isLast)
            state.restart()
            assertEquals(0, state.index)
        }
    }

    @Test fun completingAndSkippingHaveDifferentBookkeeping() {
        val model = EkagraTutorialViewModel()
        model.start(EkagraTutorialLesson.CORE)
        model.state!!.next()
        model.close()
        assertNull(model.completedLesson)
        assertFalse(model.isOpen)
        model.start(EkagraTutorialLesson.CORE)
        assertEquals(0, model.state!!.index)
        model.complete()
        assertTrue(model.isOpen)
        assertNull(model.state)
        assertEquals(EkagraTutorialLesson.CORE, model.completedLesson)
        model.acknowledgeCompletion()
        assertNull(model.completedLesson)
    }

    @Test fun processRecreationStartsClosedInsteadOfResurrectingPractice() {
        val old = EkagraTutorialViewModel()
        old.start(EkagraTutorialLesson.CORE)
        old.state!!.next()
        val recreated = EkagraTutorialViewModel()
        assertFalse(recreated.isOpen)
        assertNull(recreated.state)
        assertNull(recreated.completedLesson)
    }

    @Test fun retainedViewModelStoreKeepsPracticeStepAcrossOwnerRecreation() {
        val store = androidx.lifecycle.ViewModelStore()
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = EkagraTutorialViewModel() as T
        }
        val original = androidx.lifecycle.ViewModelProvider(store, factory)[EkagraTutorialViewModel::class.java]
        original.start(EkagraTutorialLesson.CORE)
        original.state!!.next()
        val retained = androidx.lifecycle.ViewModelProvider(store, factory)[EkagraTutorialViewModel::class.java]
        assertSame(original, retained)
        assertTrue(retained.isOpen)
        assertEquals(1, retained.state!!.index)
        store.clear()
    }

    @Test fun timerSnapshotsRepresentStartPauseBreakAndReturn() {
        val start = EkagraTutorialDemoState(EkagraTutorialScene.START)
        assertFalse(start.running)
        assertFalse(start.hasProgress)
        assertEquals(1500, start.seconds)
        val running = EkagraTutorialDemoState(EkagraTutorialScene.RUNNING)
        assertTrue(running.running)
        assertTrue(running.hasProgress)
        assertEquals(780, running.seconds)
        val paused = EkagraTutorialDemoState(EkagraTutorialScene.PAUSED)
        assertFalse(paused.running)
        assertTrue(paused.hasProgress)
        assertEquals(running.seconds, paused.seconds)
        val rest = EkagraTutorialDemoState(EkagraTutorialScene.BREAK_RUNNING)
        assertEquals(TimerMode.BREAK, rest.mode)
        assertEquals(300, rest.seconds)
        val resumed = EkagraTutorialDemoState(EkagraTutorialScene.RESUMED)
        assertEquals(TimerMode.FOCUS, resumed.mode)
        assertEquals(paused.seconds, resumed.seconds)
        assertFalse(start.kavachEnabled)
        assertTrue(EkagraTutorialDemoState(EkagraTutorialScene.KAVACH_ACTIVE).kavachEnabled)
    }
}
