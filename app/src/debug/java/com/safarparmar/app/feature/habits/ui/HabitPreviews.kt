package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.safarparmar.app.feature.habits.data.*
import java.time.DayOfWeek
import java.time.LocalDate

private val previewToday = LocalDate.of(2026, 9, 14)
private val previewHabit = HabitEntity(
    id = 17,
    name = "Read 15 pages before going to bed",
    targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
)
private val previewRecords = listOf(
    HabitWithCompletions(
        habit = previewHabit,
        completionByDate = (1..30)
            .map { LocalDate.of(2026, 9, it) }
            .filter { it.dayOfWeek in previewHabit.targetDays }
            .associate { d -> d to (d.dayOfMonth in setOf(2, 4, 7, 9, 11, 14, 16)) }
    )
)

@Composable
private fun PreviewTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHabitDarkTheme provides dark) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            Surface(Modifier.fillMaxSize(), color = HabitColors.Background) { content() }
        }
    }
}

@Preview(widthDp = 320, heightDp = 720)
@Composable fun HabitMonthly320Preview() = PreviewTheme {
    MonthlyView(previewToday.withDayOfMonth(1), previewRecords, { _, _, _ -> }, {}, {}, today = previewToday)
}
@Preview(widthDp = 375, heightDp = 720)
@Composable fun HabitWeekly375Preview() = PreviewTheme {
    WeeklyView(previewToday, previewRecords, { _, _, _ -> }, {}, {}, today = previewToday)
}
@Preview(widthDp = 414, heightDp = 720)
@Composable fun HabitToday414Preview() = PreviewTheme {
    TodayView(
        date = previewToday,
        habits = listOf(HabitWithCompletion(previewHabit, previewToday, true)),
        allHabits = listOf(previewHabit),
        onToggle = { _, _ -> }
    )
}
@Preview(widthDp = 768, heightDp = 900)
@Composable fun HabitMonthly768Preview() = PreviewTheme {
    MonthlyView(previewToday.withDayOfMonth(1), previewRecords, { _, _, _ -> }, {}, {}, today = previewToday)
}
@Preview(widthDp = 320, heightDp = 900, fontScale = 2f)
@Composable fun HabitLargeTextDarkPreview() = PreviewTheme(true) {
    MonthlyView(previewToday.withDayOfMonth(1), previewRecords, { _, _, _ -> }, {}, {}, today = previewToday)
}
@Preview(widthDp = 375, heightDp = 800)
@Composable fun HabitEditPreview() = PreviewTheme {
    // onArchive and onDelete added to match updated signature
    EditHabitBottomSheet(previewHabit, {}, { }, onArchive = { }, onDelete = { })
}
