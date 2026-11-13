package com.safarparmar.app.ui.dhyan

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.ui.tutorial.PracticeScene
import java.io.File
import org.junit.Rule
import org.junit.Test

class DhyanPinkPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun restoredPinkPresentationShowsArtworkAndControls() {
        compose.setContent {
            SafarTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().testTag("dhyan-preview")) {
                    DhyanPracticeStage(PracticeScene.DHYAN_PAUSE, false)
                }
            }
        }
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.dhyan_meditate)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.common_pause)).assertIsDisplayed()
        compose.onNodeWithTag("dhyan-preview").captureToImage().asAndroidBitmap().let { bitmap ->
            File(context.getExternalFilesDir(null), "dhyan-pink-purple.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
