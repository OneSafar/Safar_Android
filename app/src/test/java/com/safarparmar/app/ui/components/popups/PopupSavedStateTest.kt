package com.safarparmar.app.ui.components.popups

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PopupSavedStateTest {
    @Test fun `slot draft survives independent window composition replacements within one dpi bucket`() = runTest {
        val recomposer = Recomposer(coroutineContext)
        val parent = Composition(NoUiApplier(), recomposer)
        lateinit var holder: SaveableStateHolder
        lateinit var draft: MutableState<String>
        parent.setContent { holder = rememberSaveableStateHolder() }
        fun window(density: Float): Composition = Composition(NoUiApplier(), recomposer).apply {
            setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density, 1.15f),
                    LocalPopupSlotStateHolder provides holder,
                ) {
                    SafarPopupContent { draft = rememberSaveable { mutableStateOf("initial") } }
                }
            }
        }
        try {
            window(1.6f).also { draft.value = "typed inside dialog"; it.dispose() }
            window(1.6002f).also { assertEquals("typed inside dialog", draft.value); it.dispose() }
            window(2.4f).also { assertEquals("typed inside dialog", draft.value); it.dispose() }
        } finally {
            parent.dispose()
            recomposer.cancel()
        }
    }

    @Test fun `window and slot drafts survive density replacement after providers unregister`() {
        val state = PopupSavedState()
        val registry = state.attach { true }
        val outer = registry.registerProvider("outer") { "edited outer draft" }
        val slot = registry.registerProvider("slot") { "edited dialog draft" }

        state.detach(registry)
        outer.unregister()
        slot.unregister()

        val replacement = state.attach { true }
        assertEquals("edited outer draft", replacement.consumeRestored("outer"))
        assertEquals("edited dialog draft", replacement.consumeRestored("slot"))
        assertNull(replacement.consumeRestored("slot"))
    }

    @Test fun `activity saved state captures both live compositions`() {
        val state = PopupSavedState()
        val registry = state.attach { true }
        registry.registerProvider("outer") { "outer" }
        registry.registerProvider("slot") { "slot" }
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any) = true
        }
        val saved = with(PopupSavedState.Saver) { scope.save(state) }
        val restored = requireNotNull(PopupSavedState.Saver.restore(requireNotNull(saved)))
        val next = restored.attach { true }
        assertEquals("outer", next.consumeRestored("outer"))
        assertEquals("slot", next.consumeRestored("slot"))
    }

    @Test fun `an old window disposal cannot overwrite a replacement window draft`() {
        val state = PopupSavedState()
        val first = state.attach { true }
        first.registerProvider("slot") { "old" }
        state.detach(first)
        val second = state.attach { true }
        second.consumeRestored("slot")
        second.registerProvider("slot") { "new" }
        state.detach(first)
        state.detach(second)
        assertEquals("new", state.attach { true }.consumeRestored("slot"))
    }

    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = error("No UI nodes in this JVM test")
        override fun insertBottomUp(index: Int, instance: Unit) = error("No UI nodes in this JVM test")
        override fun move(from: Int, to: Int, count: Int) = error("No UI nodes in this JVM test")
        override fun remove(index: Int, count: Int) = error("No UI nodes in this JVM test")
        override fun onClear() = Unit
    }
}
