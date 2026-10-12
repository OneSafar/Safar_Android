package com.safarparmar.app.feature.toppersbatch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.safarparmar.app.ui.theme.SafarTheme

/** Preview data is debug-only, never used by the account-backed tracker. */
internal fun calendarPreviewState(): BatchUiState {
    val subjects = listOf("mathematics" to "Maths", "english" to "English", "reasoning" to "Reasoning", "gk" to "GK")
        .map { (key, name) -> BatchSubject(key, key, name) }
    val lectures = subjects.flatMap { subject -> (1..3).map { number ->
        BatchLecture(id = "${subject.key}-$number", subjectId = subject.id, subjectKey = subject.key,
            lectureNumber = number, order = number, displayTopic = if (number == 1) "Reading and understanding long passages" else "Practice questions with worked examples",
            scheduledFor = "2026-10-0${number + 3}", classTime = "09:00",
            completedAt = if (number == 1) "2026-10-04T10:00:00Z" else null,
            revisionSessions = if (number == 1) listOf(ReviewSession("2026-10-05")) else emptyList())
    } }
    return BatchUiState(month = "2026-10", selectedDay = "2026-10-04", overview = BatchOverview(
        subjects = subjects, lectures = lectures, course = BatchCourse(officialStartDate = "2026-09-28"),
        studyPlan = BatchStudyPlan("2026-10-01", "2027-03-28")))
}

@Preview(name = "Phone", widthDp = 320, heightDp = 1100, showBackground = true)
@Preview(name = "Foldable", widthDp = 600, heightDp = 1100, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 1100, showBackground = true)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 1100, showBackground = true)
@Preview(name = "Large text", widthDp = 320, heightDp = 1500, fontScale = 2f, showBackground = true)
@Preview(name = "Dark", widthDp = 375, heightDp = 1100, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CalendarPreview() {
    SafarTheme {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, LocalConfiguration.current.fontScale)) {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.batchTrackerColors(dark)) {
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        BatchStudentCalendarContent(calendarPreviewState(), {}, {}, {}, {}, {}, today = "2026-10-04")
                    }
                }
            }
        }
    }
}
