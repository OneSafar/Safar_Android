package com.safarparmar.app.ui.ekagra.focusshield

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.MainActivity
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class KavachProgressiveUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun disabledScreenHasTwoSwitchesAndYoutubeEnableOpensSetup() {
        val route = AtomicReference<String?>(null)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    SafarTheme {
                        FocusShieldStandaloneScreen(onNavigate = { route.set(it) })
                    }
                }
            }
            await { switches().size == 2 }
            assertFalse(hasText("WHEN IT WORKS"))
            assertFalse(hasText("PROTECTION LEVEL"))
            assertFalse(hasText("Apps to block"))
            assertTrue(switches().none { it.isChecked })
            clickSwitch("YouTube Study Mode")
            await { route.get() == Routes.YOUTUBE_STUDY_MODE_V2 }
        }
    }

    @Test fun turningKavachOffHidesItsConfiguration() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    SafarTheme {
                        var state by remember { mutableStateOf(FocusShieldUiState(isEnabled = true)) }
                        FocusShieldSettingsContent(
                            state = state, accent = KavachDesign.Primary,
                            onToggleEnabled = { state = state.copy(isEnabled = it) },
                            onOpenAppPicker = {}, onGoToEkagra = {}, onOpenOverlaySettings = {},
                            modifier = Modifier.statusBarsPadding(),
                        )
                    }
                }
            }
            await { hasText("WHEN IT WORKS") }
            clickSwitch("KAVACH")
            await { !hasText("WHEN IT WORKS") && switches().size == 2 }
            assertFalse(hasText("PROTECTION LEVEL"))
            assertFalse(hasText("Save"))
        }
    }

    @Test fun legacyYoutubeShortcutStillOpensYoutubeSetup() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    SafarTheme { FocusShieldStandaloneScreen(initialTab = 1) }
                }
            }
            await { hasText("YouTube Focus") }
            assertFalse(hasText("WHEN IT WORKS"))
        }
    }

    private fun nodes(node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): List<AccessibilityNodeInfo> {
        if (node == null) return emptyList()
        return listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    }
    private fun switches() = nodes().filter {
        it.isCheckable && it.contentDescription?.toString() in setOf("KAVACH", "YouTube Study Mode")
    }
    private fun hasText(text: String) = nodes().any { it.text?.toString()?.contains(text) == true }
    private fun clickSwitch(label: String) {
        await { switches().any { it.contentDescription?.toString() == label } }
        val control = switches().single { it.contentDescription?.toString() == label }
        assertTrue(control.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun await(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            Thread.sleep(100)
        }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.FileOutputStream(java.io.File(instrumentation.targetContext.cacheDir, "kavach-test-failure.png")).use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        fail("Screen did not reach the expected state. Nodes: " + nodes().joinToString("\n") {
            "${it.className}: text=${it.text}, description=${it.contentDescription}, checked=${it.isChecked}"
        }.take(6000))
    }
}
