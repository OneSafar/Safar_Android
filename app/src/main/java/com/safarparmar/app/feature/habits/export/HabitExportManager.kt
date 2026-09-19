package com.safarparmar.app.feature.habits.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.safarparmar.app.feature.habits.data.HabitPerformance
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportDateRangeOption {
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

@Singleton
class HabitExportManager @Inject constructor() {

    /**
     * Resolves the start and end dates for a given [ExportDateRangeOption].
     */
    fun resolveDateRange(
        option: ExportDateRangeOption,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null,
        today: LocalDate = LocalDate.now(),
        earliestHabitDate: LocalDate = LocalDate.of(2020, 1, 1)
    ): Pair<LocalDate, LocalDate> {
        return when (option) {
            ExportDateRangeOption.THIS_MONTH -> {
                val start = today.withDayOfMonth(1)
                val end = today.withDayOfMonth(today.lengthOfMonth())
                start to end
            }
            ExportDateRangeOption.THIS_YEAR -> {
                val start = LocalDate.of(today.year, 1, 1)
                val end = LocalDate.of(today.year, 12, 31)
                start to end
            }
            ExportDateRangeOption.ALL_TIME -> {
                earliestHabitDate to today
            }
            ExportDateRangeOption.CUSTOM -> {
                val s = customStart ?: today.minusMonths(1)
                val e = customEnd ?: today
                if (s.isAfter(e)) e to s else s to e
            }
        }
    }

    /**
     * Generates an RFC 4180-compliant CSV string containing both the detailed
     * day-by-day logs and the aggregated summary section.
     */
    fun generateCsv(
        habits: List<HabitWithCompletions>,
        startDate: LocalDate,
        endDate: LocalDate,
        performanceList: List<HabitPerformance> = emptyList(),
        selectedHabitIds: Set<Long>? = null
    ): String {
        val filteredHabits = if (selectedHabitIds != null) {
            habits.filter { it.habit.id in selectedHabitIds }
        } else {
            habits
        }

        val filteredPerformance = if (selectedHabitIds != null) {
            performanceList.filter { it.habitId in selectedHabitIds }
        } else {
            performanceList
        }

        val sb = StringBuilder()

        // 1. Detailed Day-by-Day Section
        sb.append("Date,Habit,Scheduled,Completed\r\n")

        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val dateStr = cursor.toString()
            for (hwc in filteredHabits) {
                val isDone = hwc.completionByDate[cursor]
                if (isDone != null) {
                    val habitNameEscaped = escapeCsv(hwc.habit.name)
                    val scheduled = "Yes"
                    val completed = if (isDone) "Yes" else "No"
                    sb.append("$dateStr,$habitNameEscaped,$scheduled,$completed\r\n")
                }
            }
            cursor = cursor.plusDays(1)
        }

        // 2. Summary Section
        if (filteredPerformance.isNotEmpty()) {
            sb.append("\r\n")
            sb.append("Habit Summary\r\n")
            sb.append("Habit,Completion Rate,Current Streak,Best Streak\r\n")
            for (perf in filteredPerformance) {
                val nameEscaped = escapeCsv(perf.habitName)
                val rateFormatted = String.format(Locale.US, "%.0f%%", perf.completionRate * 100f)
                sb.append("$nameEscaped,$rateFormatted,${perf.currentStreak},${perf.bestStreak}\r\n")
            }
        }

        return sb.toString()
    }

    /**
     * Escapes CSV cells according to RFC 4180:
     * Values with commas, quotes, or newlines are wrapped in quotes,
     * and internal quotes are doubled.
     */
    fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Saves CSV content to app cache and opens the Android Share Sheet.
     */
    fun exportAndShare(
        context: Context,
        csvContent: String,
        filenamePrefix: String = "habit_tracker_export"
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val file = File(exportDir, "${filenamePrefix}_$timestamp.csv")
        file.writeText(csvContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Habit Tracker Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Habit Data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return file
    }

    /**
     * Writes CSV string directly to an output stream (e.g. Storage Access Framework Uri).
     */
    fun writeToOutputStream(outputStream: OutputStream, csvContent: String) {
        outputStream.use { os ->
            os.write(csvContent.toByteArray(Charsets.UTF_8))
            os.flush()
        }
    }
}
