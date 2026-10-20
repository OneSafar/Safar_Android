package com.safarparmar.app.ui.components.popups

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SafarPopupEnvironmentInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bridgeDoesNotAddSpacedLayoutChildrenAndIsRemovedOnDismissal() {
        var showBridge by mutableStateOf(false)
        var hostRoot: ViewGroup? = null
        var bridgeAttached = false
        compose.setContent {
            val root = LocalView.current.rootView as ViewGroup
            SideEffect { hostRoot = root }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(20.dp).testTag("before.bridge"))
                if (showBridge) SafarPopupEnvironment {
                    val bridgeView = LocalView.current
                    // LocalView is the AndroidComposeView inside the bridge's
                    // ComposeView, rather than the ComposeView attached to root.
                    SideEffect { bridgeAttached = bridgeView.isAttachedToWindow &&
                        (bridgeView.parent as? androidx.compose.ui.platform.ComposeView)?.parent === root }
                }
                Box(Modifier.size(20.dp).testTag("after.bridge"))
            }
        }
        val originalFirst = compose.onNodeWithTag("before.bridge").fetchSemanticsNode().boundsInWindow
        val originalSecond = compose.onNodeWithTag("after.bridge").fetchSemanticsNode().boundsInWindow
        var originalChildren = 0
        compose.runOnIdle {
            originalChildren = hostRoot!!.childCount
            showBridge = true
        }
        compose.waitForIdle()
        val shownFirst = compose.onNodeWithTag("before.bridge").fetchSemanticsNode().boundsInWindow
        val shownSecond = compose.onNodeWithTag("after.bridge").fetchSemanticsNode().boundsInWindow
        assertEquals(originalFirst, shownFirst)
        assertEquals(originalSecond, shownSecond)
        compose.runOnIdle {
            assertTrue(bridgeAttached)
            assertEquals(originalChildren + 1, hostRoot!!.childCount)
            showBridge = false
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(originalChildren, hostRoot!!.childCount) }
    }

    @Test fun nativeDialogChromeTracksScaleAndKeepsFormStateWithoutChangingHostResources() {
        val hostResources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val originalConfiguration = Configuration(hostResources.configuration)
        val originalDensityDpi = hostResources.displayMetrics.densityDpi
        var displayDensity by mutableFloatStateOf(1.6f)
        var fontScale by mutableFloatStateOf(1.15f)
        var windowDensity: Density? = null
        var exactContentDensity: Density? = null
        var resourceDensityDpi = 0

        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    // The immediate feature scale must override a stale outer
                    // theme marker when the native popup is first launched.
                    LocalSafarPopupDensity provides Density(9f, 2f),
                    LocalDensity provides Density(displayDensity, fontScale),
                ) {
                    SafarPopupEnvironment {
                        var input by rememberSaveable { mutableStateOf("original") }
                        AlertDialog(
                            onDismissRequest = {},
                            modifier = Modifier.width(280.dp).testTag("native.popup.shell"),
                            title = {
                                // This slot intentionally has no content provider: the
                                // native window's own density must already be correct.
                                val density = LocalDensity.current
                                val resources = LocalContext.current.resources
                                SideEffect {
                                    windowDensity = density
                                    resourceDensityDpi = resources.configuration.densityDpi
                                }
                                Text("Popup title")
                            },
                            text = {
                                SafarPopupContent {
                                    var slotInput by rememberSaveable { mutableStateOf("slot draft") }
                                    val contentDensity = LocalDensity.current
                                    SideEffect { exactContentDensity = contentDensity }
                                    Column {
                                        OutlinedTextField(value = input, onValueChange = { input = it },
                                            modifier = Modifier.testTag("native.popup.input"))
                                        OutlinedTextField(value = slotInput, onValueChange = { slotInput = it },
                                            modifier = Modifier.testTag("native.popup.slot.input"))
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = {}) { Text("Keep open") } },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("native.popup.input").performTextReplacement("keep this draft")
        compose.onNodeWithTag("native.popup.slot.input").performTextReplacement("keep slot state")

        listOf(1.6f to 1.15f, 1.6002f to 1.15f, 2.4f to 1.4f).forEach { (density, font) ->
            compose.runOnIdle { displayDensity = density; fontScale = font }
            compose.waitForIdle()
            compose.onNodeWithTag("native.popup.input").assertTextContains("keep this draft")
            compose.onNodeWithTag("native.popup.slot.input").assertTextContains("keep slot state")
            compose.runOnIdle {
                assertEquals(density, windowDensity!!.density, 0.5f / 160f)
                assertEquals(font, windowDensity!!.fontScale, 0.001f)
                assertEquals(density, exactContentDensity!!.density, 0f)
                assertEquals(font, exactContentDensity!!.fontScale, 0f)
                assertEquals((density * 160f).toInt(), resourceDensityDpi)
                assertEquals(originalDensityDpi, hostResources.displayMetrics.densityDpi)
                assertEquals(originalConfiguration.fontScale, hostResources.configuration.fontScale, 0f)
                assertEquals(originalConfiguration.orientation, hostResources.configuration.orientation)
            }
            val shell = compose.onNodeWithTag("native.popup.shell").fetchSemanticsNode().boundsInWindow
            assertEquals(280f * density, shell.width, 2f)
            assertTrue(shell.height > 0f)
        }
    }
}
