package com.safarparmar.app.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.SafarTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Real layout and route checks for the redesign's responsive breakpoints. */
@RunWith(Parameterized::class)
class RoyalPurpleHomeLayoutTest(private val width: Int, private val scale: Float, private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)

    @Test fun homeRemainsReadableAndRoutesAreConnected() {
        var route: String? = null
        var menu = false
        compose.setContent {
            SafarTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalDensity provides Density(if (width > 700) .85f else 1f, scale)) {
                    PlumPhoneHome(dark, { route = it }, { menu = true },
                        Modifier.requiredSize(width.dp, 900.dp).testTag("home"), autoRotateCarousels = false, actions = {
                            IconButton(onClick = {}) { Icon(Icons.Default.Spa, "Guide") }
                            IconButton(onClick = {}) { Icon(Icons.Default.PlayCircle, "Video guide") }
                            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, "Notifications") }
                        })
                }
            }
        }
        compose.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "royal-home-$width-$scale-$dark.png")
        file.outputStream().use { compose.onNodeWithTag("home").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithContentDescription(label(R.string.nav_open_menu)).performClick()
        compose.runOnIdle { assertTrue(menu) }
        compose.onNodeWithText(label(R.string.royal_home_study)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.royal_home_nav_journal)).assertDoesNotExist()
        homeModuleSlides.forEachIndexed { index, slide ->
            compose.onNodeWithTag("home-module-tab-$index").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(label(slide.description)).assertIsDisplayed()
            compose.onNodeWithTag("home-module-$index").performClick()
            compose.runOnIdle { assertEquals(slide.route, route) }
        }
        val promo = compose.onNodeWithTag("home-promo-carousel")
        listOf(Routes.YOUTUBE_STUDY_MODE_V2, Routes.HABIT_TRACKER, Routes.TIMETABLE).forEachIndexed { index, target ->
            promo.performScrollTo()
            promo.performScrollToIndex(index)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(label(when(index) {
                0 -> R.string.home_banner_youtube_focus_description
                1 -> R.string.home_banner_habit_tracker_description
                else -> R.string.home_banner_timetable_description
            })).performClick()
            compose.runOnIdle { assertEquals(target, route) }
        }
        val destinations = listOf(
            R.string.module_ekagra to Routes.EKAGRA,
            R.string.module_nishtha to Routes.nishthaRoot(),
            R.string.module_mehfil to Routes.MEHFIL,
            R.string.module_study_planner to Routes.STUDY_PLANNER,
            R.string.module_dhyan to Routes.DHYAN,
            R.string.nav_study_circle to Routes.STUDY_CIRCLES,
            R.string.nav_focus_shield to Routes.FOCUS_SHIELD,
            R.string.nav_leaderboard to Routes.LEADERBOARD,
            R.string.royal_home_dashboard to Routes.DASHBOARD,
        )
        destinations.forEach { (id, target) ->
            val node = compose.onAllNodesWithText(label(id)).filter(hasClickAction()).onLast()
            node.performScrollTo()
            if (id != R.string.royal_home_dashboard) {
                compose.onNodeWithTag("home-tool-image-$id", useUnmergedTree = true).assertExists()
            }
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onAllNodesWithText(label(id), useUnmergedTree = true).onLast()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Missing text layout for ${label(id)}", layouts.isNotEmpty())
            layouts.forEach { layout ->
                if (id == R.string.nav_leaderboard) assertEquals("Leaderboard must fit on one line", 1, layout.lineCount)
                // Unstyled Text shrinks its measured width after paragraph layout.
                // Check actual glyph extents, rather than the paragraph overflow flag.
                for (line in 0 until layout.lineCount) {
                    assertFalse("Ellipsized label ${label(id)}", layout.isLineEllipsized(line))
                    assertTrue("Clipped line ${label(id)}", layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1)
                    assertTrue("Clipped height ${label(id)}", layout.getLineBottom(line) <= layout.size.height + 1)
                }
            }
            node.performClick()
            compose.runOnIdle { assertEquals(target, route) }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0}, scale={1}, dark={2}")
        fun configurations() = listOf(arrayOf<Any>(320, 1f, false), arrayOf<Any>(375, 1f, false),
            arrayOf<Any>(414, 1f, false), arrayOf<Any>(414, 1.15f, false), arrayOf<Any>(484, 1.15f, false), arrayOf<Any>(768, 1f, true), arrayOf<Any>(375, 1.45f, true))
    }
}
