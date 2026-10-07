package com.safarparmar.app.feature.toppersbatch

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.ui.theme.SafarTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarBusyDayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manyCompletionsStayInsideTheirDateAndRemainInTheAgenda() {
        val day = "2026-10-05"
        val initial = todayPlanFixture()
        val completed = initial.subjects.flatMap { subject -> (1..12).map { number ->
            initial.lectures.first { it.subjectId == subject.id }.copy(
                id = "${subject.id}-$number", lectureNumber = number,
                completedAt = "2026-10-05T09:00:00Z", scheduledFor = null)
        } }
        val overview = initial.copy(lectures = completed)
        var width by mutableStateOf(320)
        var scale by mutableStateOf(1f)
        var selected by mutableStateOf("2026-10-06")
        compose.setContent {
            SafarTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.batchTrackerColors(false)) {
                        Surface(Modifier.requiredWidth(width.dp).height(1400.dp).testTag("busy-calendar")) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                BatchStudentCalendarContent(BatchUiState(overview = overview, month = "2026-10", selectedDay = selected),
                                    onDay = { selected = it }, onMonth = {}, onToday = {}, onJump = {}, onLecture = {}, today = "2026-10-06")
                            }
                        }
                    }
                }
            }
        }
        for (size in listOf(320, 375, 414)) for (font in listOf(1f, 1.6f)) {
            compose.runOnIdle { width = size; scale = font }
            val tile = compose.onNodeWithTag("calendar-date-$day").fetchSemanticsNode().boundsInRoot
            for (subject in initial.subjects) {
                val markers = compose.onAllNodesWithTag("calendar-marker-$day-${subject.id}", useUnmergedTree = true).fetchSemanticsNodes()
                assertEquals("Repeated lectures share one subject marker", 1, markers.size)
                val dot = markers.single().boundsInRoot
                assertTrue("Marker must stay inside its date", dot.left >= tile.left && dot.right <= tile.right && dot.top >= tile.top && dot.bottom <= tile.bottom)
            }
        }
        compose.onNodeWithTag("calendar-date-$day").performClick()
        compose.runOnIdle {
            assertEquals(day, selected)
            assertEquals(48, overview.calendarEvents().count { it.date == day && it.kind == BatchEventKind.DONE })
        }
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "calendar-busy-day-fixed.png")
        file.outputStream().use { compose.onNodeWithTag("calendar-month").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
