package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import com.safarparmar.app.ui.studyplanner.components.PlannerRevisionAccent
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.sp
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
    val examMaxMillis = remember(examLocalDate) {
        examLocalDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
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
    ) {
        if (showDatePicker) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Select Revision Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
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
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Set Date", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Schedule Revision",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = topicName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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
                    OutlinedButton(
                        onClick = {
                            onCancelRevision()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    ) {
                        Text("Remove Revision Schedule", fontWeight = FontWeight.Bold)
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
    val scheme = MaterialTheme.colorScheme
    val hasSlots = dates.isNotEmpty()
    var selectedCount by remember(dates) { mutableStateOf(dates.size.coerceAtMost(5).coerceAtLeast(1)) }
    val selectedDates = remember(dates, selectedCount) { dates.take(selectedCount.coerceIn(1, dates.size.coerceAtMost(5).coerceAtLeast(1))) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (hasSlots) scheme.primaryContainer else scheme.surfaceContainerLow,
        border = BorderStroke(
            1.5.dp,
            if (hasSlots) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(id = com.safarparmar.app.R.drawable.ic_brain),
                    contentDescription = null,
                    tint = if (hasSlots) PlannerRevisionAccent.Spaced else scheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Spaced Revision",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasSlots) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                    Text(
                        if (hasSlots) "Choose how many spaced reviews you want before the exam"
                        else "No review slots fit before your exam",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasSlots) scheme.onPrimaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
                    )
                }
            }

            if (hasSlots) {
                val maxRevisions = dates.size.coerceAtMost(5)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Number of revisions:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) scheme.primary else scheme.onPrimaryContainer.copy(alpha = 0.08f))
                                        .clickable { selectedCount = count },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) scheme.onPrimary else scheme.onPrimaryContainer,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                    val lastDate = selectedPreviewLastDate(selectedDates)
                    Text(
                        text = "Last reminder: $lastDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDates.forEachIndexed { index, dateStr ->
                        SpacedRevisionDateRow(
                            index = index,
                            dateStr = dateStr,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSchedule(selectedDates) },
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Schedule ${selectedDates.size} Revision Session${if (selectedDates.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacedRevisionDateRow(
    index: Int,
    dateStr: String,
    color: Color,
) {
    val interval = SPACED_INTERVALS.getOrNull(index)
    val localDate = runCatching { LocalDate.parse(dateStr) }.getOrNull()
    val formatted = localDate
        ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        ?: dateStr
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.14f),
            contentColor = color,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(
            text = interval?.label ?: "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(56.dp),
        )
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.85f),
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
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Custom Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface,
                )
                Text(
                    "Pick a specific date",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
