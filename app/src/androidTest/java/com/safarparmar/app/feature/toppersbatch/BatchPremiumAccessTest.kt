package com.safarparmar.app.feature.toppersbatch

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.safarparmar.app.ui.theme.SafarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BatchPremiumAccessTest {
    @get:Rule val compose = createComposeRule()

    private fun checkAccess(premium: Boolean) {
        var redirects = 0
        var actions = 0
        compose.setContent {
            SafarTheme {
                CompositionLocalProvider(LocalBatchFeatureAccess provides BatchFeatureAccess(premium, { redirects++ })) {
                    Column {
                        BatchButton(onClick = { actions++ }) { androidx.compose.material3.Text("Add lecture") }
                        BatchOutlinedButton(onClick = { actions++ }) { androidx.compose.material3.Text("Add subject") }
                        BatchTextButton(onClick = { actions++ }) { androidx.compose.material3.Text("Restore") }
                    }
                }
            }
        }
        listOf("Add lecture", "Add subject", "Restore").forEach { compose.onNodeWithText(it).performClick() }
        compose.runOnIdle {
            assertEquals(if (premium) 3 else 0, actions)
            assertEquals(if (premium) 0 else 3, redirects)
        }
    }

    @Test fun freeStudentsReachPremiumWithoutRunningFeatureActions() = checkAccess(false)
    @Test fun premiumStudentsUseFeaturesWithoutAnyRedirect() = checkAccess(true)
}
