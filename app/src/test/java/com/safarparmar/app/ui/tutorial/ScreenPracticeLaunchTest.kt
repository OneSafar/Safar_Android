package com.safarparmar.app.ui.tutorial

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Exercises real Compose registration, including retained callbacks from the loading frame. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScreenPracticeLaunchTest {
    @Test fun `all practice features launch after loading and still block genuine busy flows`() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val runner = launch(clock, start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        try {
            PracticeLesson.entries.map { it.feature }.distinct().forEach { feature ->
                val controller = TitliController()
                val model = ScreenPracticeViewModel()
                val eligible = mutableStateOf(false)
                var blocked = 0
                val composition = Composition(NoUiApplier(), recomposer)
                try {
                    composition.setContent {
                        CompositionLocalProvider(LocalTitliController provides controller) {
                            RegisterScreenPracticeGuide(feature, eligible.value, model) { blocked++ }
                        }
                    }
                    val initial = controller.guides.values.single()
                    assertFalse(initial.ready())
                    initial.lessons(); assertEquals(1, blocked); assertFalse(model.isOpen)
                    eligible.value = true
                    Snapshot.sendApplyNotifications(); runCurrent(); clock.sendFrame(1L); runCurrent()
                    assertTrue(initial.ready())
                    // The callback retained by the toolbar must read the new eligibility.
                    initial.lessons(); assertTrue("$feature lesson picker", model.isOpen)
                    model.close(); initial.start()
                    assertEquals(PracticeLesson.forFeature(feature).first(), model.lesson)
                    assertEquals(1, blocked)
                    model.close()
                    eligible.value = false
                    Snapshot.sendApplyNotifications(); runCurrent(); clock.sendFrame(2L); runCurrent()
                    assertFalse(initial.ready())
                    initial.start(); assertFalse(model.isOpen); assertEquals(2, blocked)
                } finally { composition.dispose() }
                assertTrue(controller.guides.isEmpty())
            }
        } finally { recomposer.cancel(); runner.cancel() }
    }
    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = error("No UI")
        override fun insertBottomUp(index: Int, instance: Unit) = error("No UI")
        override fun move(from: Int, to: Int, count: Int) = error("No UI")
        override fun remove(index: Int, count: Int) = error("No UI")
        override fun onClear() = Unit
    }
}
