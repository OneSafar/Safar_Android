package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// SM-2 inspired intervals — Day 1 anchors the first review to "did I actually understand?"
// then exponentially grows. Confirmed by Ebbinghaus forgetting-curve research.
private data class RevisionInterval(val days: Int, val label: String, val caption: String)

private val SPACED_INTERVALS = listOf(
    RevisionInterval(1,  "Day 1",   "First recall — highest risk of forgetting"),
    RevisionInterval(3,  "Day 3",   "Short-term consolidation"),
    RevisionInterval(7,  "Week 1",  "Moving to long-term memory"),
    RevisionInterval(14, "Week 2",  "Long-term consolidation"),
    RevisionInterval(30, "Month 1", "Final anchor — memory is durable now"),
)

/**
 * Computes spaced revision dates starting from [fromDate], optionally clamped
 * to not exceed [examDate]. Returns an empty list only when the exam date is
 * already past the first interval.
 */
fun computeSpacedRevisionDates(
    fromDate: LocalDate = LocalDate.now(),
    examDate: LocalDate? = null,
): List<String> = SPACED_INTERVALS
    .map { fromDate.plusDays(it.days.toLong()) }
    .filter { examDate == null || it <= examDate }
    .map { it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScheduleSheet(
    topicName: String,
    examDate: String?,
    onRevisionScheduled: (dates: List<String>, scheduleType: String) -> Unit,
    onDismiss: () -> Unit,
    isAlreadyRevisionNeeded: Boolean = false,
    onCancelRevision: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }

    val examLocalDate = remember(examDate) {
        examDate?.takeIf { it.length >= 10 }?.let {
            runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
        }
    }
    val spacedDates = remember(today, examLocalDate) {
        computeSpacedRevisionDates(today, examLocalDate)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    val tomorrowMillis = remember {
        today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = tomorrowMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val picked = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val minDate = today.plusDays(1)
                return picked >= minDate && (examLocalDate == null || picked <= examLocalDate)
            }
        },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = PlannerFlatColors.BgCream,
    ) {
        if (showDatePicker) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlanEyebrow("Custom revision")
                Text(
                    text = "Select revision date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextDark,
                )
                PlanHairline()

                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val picked = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC).toLocalDate().toString()
                                onRevisionScheduled(listOf(picked), "custom")
                                onDismiss()
                            }
                            showDatePicker = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Set Date", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    PlanEyebrow("Revision plan")
                    Text(
                        "Schedule Revision",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.TextDark,
                    )
                    Text(
                        text = topicName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlannerFlatColors.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                PlanHairline()

                // Option 1: Spaced Revision
                SpacedRevisionOptionCard(
                    dates = spacedDates,
                    today = today,
                    onSchedule = { selectedDates ->
                        onRevisionScheduled(selectedDates, "spaced")
                        onDismiss()
                    },
                )

                // Option 2: Custom Date
                CustomDateOptionCard(onClick = { showDatePicker = true })

                if (isAlreadyRevisionNeeded && onCancelRevision != null) {
                    PlanHairline()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .clickable {
                                onCancelRevision()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Remove revision schedule",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacedRevisionOptionCard(
    dates: List<String>,
    today: LocalDate,
    onSchedule: (dates: List<String>) -> Unit,
) {
    val accent = PlannerFlatColors.PrimaryAccent
    val hasSlots = dates.isNotEmpty()
    var selectedCount by remember(dates) { mutableStateOf(dates.size.coerceAtMost(5).coerceAtLeast(1)) }
    val selectedDates = remember(dates, selectedCount) { dates.take(selectedCount.coerceIn(1, dates.size.coerceAtMost(5).coerceAtLeast(1))) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PlanHairline()
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Spaced revision",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.TextDark,
                    )
                    Text(
                        if (hasSlots) "Short reviews that help you remember for longer"
                        else "No review slots fit before your exam",
                        style = MaterialTheme.typography.bodySmall,
                        color = PlannerFlatColors.TextMuted,
                    )
                }
            }

            if (hasSlots) {
                val maxRevisions = dates.size.coerceAtMost(5)
                Text(
                    text = "How many reviews?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (count in 1..maxRevisions) {
                        val isSelected = count == selectedCount
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.background(accent)
                                    else Modifier.border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
                                )
                                .clickable { selectedCount = count },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$count",
                                color = if (isSelected) Color.White else PlannerFlatColors.TextDark,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Text(
                    text = "Last review: ${selectedPreviewLastDate(selectedDates)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PlannerFlatColors.TextMuted,
                )

                Column {
                    selectedDates.forEachIndexed { index, dateStr ->
                        if (index > 0) PlanHairline(alpha = 0.6f)
                        SpacedRevisionDateRow(index = index, dateStr = dateStr)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, accent, RoundedCornerShape(8.dp))
                        .clickable { onSchedule(selectedDates) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Schedule ${selectedDates.size} revision session${if (selectedDates.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                    }
                }
            }
        }
        PlanHairline()
    }
}

@Composable
private fun SpacedRevisionDateRow(
    index: Int,
    dateStr: String,
) {
    val interval = SPACED_INTERVALS.getOrNull(index)
    val localDate = runCatching { LocalDate.parse(dateStr) }.getOrNull()
    val formatted = localDate
        ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        ?: dateStr
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextMuted,
            )
        }
        Text(
            text = interval?.label ?: "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelMedium,
            color = PlannerFlatColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun selectedPreviewLastDate(dates: List<String>): String {
    val last = dates.lastOrNull() ?: return "-"
    val localDate = runCatching { LocalDate.parse(last) }.getOrNull()
    return localDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: last
}

@Composable
private fun CustomDateOptionCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = PlannerFlatColors.PrimaryAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Pick a custom date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextDark,
            )
            Text(
                "Choose one revision day yourself",
                style = MaterialTheme.typography.bodySmall,
                color = PlannerFlatColors.TextMuted,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
