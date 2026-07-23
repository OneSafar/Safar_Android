package com.safarparmar.app.ui.nishtha.goals

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.GoalLinkedSession
import com.safarparmar.app.ui.ekagra.EkagraViewModel
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.util.IstDateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTab(goals: List<Goal>) {
    val completed = goals.filter { it.completed }
    val today = LocalDate.now(IstDateUtils.zone)
    var selectedDate by remember { mutableStateOf(today) }
    var showHistoryDatePicker by remember { mutableStateOf(false) }
    val historyDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val filtered = completed.filter { goal ->
        val d = goal.completedAt?.take(10) ?: goal.scheduledDate?.take(10)
        d == selectedDate.toString()
    }

    val ekagraViewModel = hiltViewModel<EkagraViewModel>()
    val linkedSessions by ekagraViewModel.linkedSessions.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { ekagraViewModel.loadLinkedSessions() }
    val linkedSessionsForDate = remember(linkedSessions, selectedDate) {
        linkedSessions.filter {
            val d = (it.endedAt ?: it.startedAt)?.let { ts ->
                runCatching { Instant.parse(ts).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
            }
            d == selectedDate
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var highlightedGoalId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(highlightedGoalId) {
        if (highlightedGoalId != null) {
            kotlinx.coroutines.delay(1600)
            highlightedGoalId = null
        }
    }

    if (showHistoryDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showHistoryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    historyDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showHistoryDatePicker = false
                }) { Text("OK", color = GoalsFlatColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDatePicker = false }) {
                    Text("Cancel", color = GoalsFlatColors.Muted)
                }
            },
        ) {
            DatePicker(state = historyDatePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalsFlatColors.Bg),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Archive",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                color = GoalsFlatColors.Scheduled,
            )
            Text(
                "Review what was completed on a specific day.",
                fontSize = 12.sp,
                color = GoalsFlatColors.Muted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(12.dp))
                        .clickable { showHistoryDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GoalsFlatColors.Scheduled,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoalsFlatColors.Text,
                        )
                    }
                }
                if (selectedDate != today) {
                    Text(
                        "Reset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoalsFlatColors.Scheduled,
                        modifier = Modifier
                            .clickable { selectedDate = today }
                            .padding(8.dp),
                    )
                }
            }
        }
        PlanHairline(modifier = Modifier.padding(horizontal = 20.dp))

        if (filtered.isEmpty() && linkedSessionsForDate.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing found for this date.", color = GoalsFlatColors.Muted)
            }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            itemsIndexed(filtered, key = { _, goal -> goal.id }) { index, goal ->
                val isHighlighted = highlightedGoalId == goal.id
                val highlightColor by animateColorAsState(
                    targetValue = if (isHighlighted) GoalsFlatColors.Scheduled.copy(alpha = 0.12f) else Color.Transparent,
                    label = "goalHighlight",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(highlightColor),
                ) {
                    GoalItem(goal, onComplete = {}, onEdit = {}, onDelete = {}, onRepeat = {})
                }
                if (index < filtered.lastIndex) PlanHairline(alpha = 0.5f)
            }
            if (linkedSessionsForDate.isNotEmpty()) {
                item(key = "linked_sessions_header") {
                    Spacer(Modifier.height(12.dp))
                    PlanHairline()
                    Text(
                        "LINKED EKAGRA SESSIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = GoalsFlatColors.Ekagra,
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(linkedSessionsForDate, key = { _, s -> "linked_${s.id}" }) { index, session ->
                    LinkedEkagraSessionRow(
                        session = session,
                        onViewGoal = {
                            if (!session.goalExists) {
                                Toast.makeText(context, "This goal has been deleted.", Toast.LENGTH_SHORT).show()
                                return@LinkedEkagraSessionRow
                            }
                            val targetIndex = filtered.indexOfFirst { it.id == session.goalId }
                            if (targetIndex == -1) {
                                Toast.makeText(context, "Goal not found for the selected date.", Toast.LENGTH_SHORT).show()
                                return@LinkedEkagraSessionRow
                            }
                            highlightedGoalId = session.goalId
                            coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
                        },
                    )
                    if (index < linkedSessionsForDate.lastIndex) PlanHairline(alpha = 0.45f)
                }
            }
        }
    }
}

@Composable
private fun LinkedEkagraSessionRow(
    session: GoalLinkedSession,
    onViewGoal: () -> Unit,
) {
    val start = session.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val end = session.endedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
    val timeRange = if (start != null && end != null) {
        "${timeFormatter.format(start)} – ${timeFormatter.format(end)}"
    } else {
        null
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    session.goalTitle ?: if (session.goalExists) "Untitled goal" else "Deleted goal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoalsFlatColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                FlatBadge("Ekagra", GoalsFlatColors.Ekagra)
            }
            Text(
                buildString {
                    if (session.timerMode?.equals("stopwatch", ignoreCase = true) == true) append("Stopwatch · ")
                    val mins = session.durationSeconds / 60
                    val secs = session.durationSeconds % 60
                    append(if (secs > 0) "${mins}m ${secs}s" else "${mins}m")
                    if (timeRange != null) append(" · $timeRange")
                },
                fontSize = 12.sp,
                color = GoalsFlatColors.Muted,
            )
        }
        Text(
            "View Goal",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoalsFlatColors.Scheduled,
            modifier = Modifier
                .clickable(onClick = onViewGoal)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun GoalTimeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color),
        )
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GoalsFlatColors.Text)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 11.sp, color = GoalsFlatColors.Muted)
            }
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
    }
}
