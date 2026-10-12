package com.safarparmar.app.ui.drawer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class ViewportDrawerStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun closedDrawerStaysClosedAcrossRepeatedLandscapePortraitDensityChanges() {
        val density = mutableStateOf(2f)
        lateinit var drawer: DrawerState
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density.value)) {
                drawer = rememberViewportDrawerState(resetOnViewportChange = true)
                val scope = rememberCoroutineScope()
                MaterialTheme {
                    ModalNavigationDrawer(
                        drawerState = drawer,
                        gesturesEnabled = false,
                        drawerContent = {
                            Box(Modifier.width(100.dp).fillMaxHeight()) { Text("Sidebar") }
                        },
                    ) {
                        TextButton(onClick = { scope.launch { drawer.open() } }) { Text("Open menu") }
                    }
                }
            }
        }
        compose.waitForIdle()
        val initialDrawer = drawer
        repeat(3) {
            // A previous closed offset of -50px is closer to Open (0) than to
            // the new portrait Closed (-200px), reproducing Material's anchor drift.
            compose.runOnIdle { density.value = 0.5f }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(DrawerValue.Closed, drawer.currentValue) }
            compose.runOnIdle { density.value = 2f }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(DrawerValue.Closed, drawer.currentValue)
                assertEquals(-200f, drawer.currentOffset, 1f)
            }
        }
        compose.runOnIdle { assertNotSame(initialDrawer, drawer) }
        // The explicit menu action must still work with the newly created state.
        compose.onNodeWithText("Open menu").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(DrawerValue.Open, drawer.currentValue) }
    }

    @Test fun otherScreensRetainTheirDrawerStateWhenPolicyIsDisabled() {
        val density = mutableStateOf(2f)
        lateinit var drawer: DrawerState
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density.value)) {
                drawer = rememberViewportDrawerState(resetOnViewportChange = false)
            }
        }
        compose.waitForIdle()
        val initialDrawer = drawer
        compose.runOnIdle { density.value = 0.5f }
        compose.waitForIdle()
        compose.runOnIdle { assertSame(initialDrawer, drawer) }
    }
}
