package com.safarparmar.app.ui.nishtha.goals

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isVisibleInGoals
import com.safarparmar.app.util.isGoalCompleted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTab(
    goals: List<Goal>,
    isSaving: Boolean,
    onReopen: (Goal) -> Unit,
) {
    val today = LocalDate.now(IstDateUtils.zone)
    var selectedDate by remember { mutableStateOf(today) }
    var showHistoryDatePicker by remember { mutableStateOf(false) }
    val historyDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val ekagraViewModel = hiltViewModel<EkagraViewModel>()
    val linkedSessions by ekagraViewModel.linkedSessions.collectAsStateWithLifecycle()
    LaunchedEffect(goals) { ekagraViewModel.loadLinkedSessions() }
    val sessionsByGoal = remember(linkedSessions) {
        linkedSessions.distinctBy { it.id }.groupBy { it.goalId }
    }
    // A linked session is durable evidence that this is a real goal completed
    // through Ekagra. Keep it in Goal History even when an older API response
    // labels its source as `ekagra` without the newer completedViaFocus flag.
    val completed = remember(goals, sessionsByGoal) {
        goals.filter { goal ->
            goal.isGoalCompleted() && (goal.isVisibleInGoals() || sessionsByGoal.containsKey(goal.id))
        }
    }
    val filtered = completed.filter { goal ->
        goal.assignedDateKey() == selectedDate.toString()
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
                "Goal history",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                color = GoalsFlatColors.Scheduled,
            )
            Text(
                "Each goal appears once, with the focus sessions that contributed to it.",
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

        if (filtered.isEmpty()) {
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GoalsFlatColors.Primary, RoundedCornerShape(16.dp))
                    .background(GoalsFlatColors.Primary.copy(alpha = 0.03f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("No completed goals for this date.", color = GoalsFlatColors.Muted)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered, key = { it.id }) { goal ->
                val sessions = sessionsByGoal[goal.id].orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(16.dp))
                        .background(GoalsFlatColors.Primary.copy(alpha = 0.03f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    GoalItem(
                        goal = goal,
                        onComplete = {},
                        onReopen = if (isSaving) null else ({ onReopen(goal) }),
                        onEdit = {},
                        onDelete = {},
                        completedViaEkagra = goal.completedViaFocus || sessions.isNotEmpty(),
                    )
                    if (sessions.isNotEmpty()) {
                        GoalFocusContribution(sessions)
                    }
                }
            }
        }
    }
}

/** Session details belong to the goal above; never repeat its title or completion badge. */
@Composable
private fun GoalFocusContribution(sessions: List<GoalLinkedSession>) {
    var expanded by remember { mutableStateOf(false) }
    val totalSeconds = sessions.sumOf { linkedFocusSeconds(it) }
    val summary = "${sessions.size} focus ${if (sessions.size == 1) "session" else "sessions"} · ${focusContributionDuration(totalSeconds)}"
    PlanHairline(alpha = 0.5f)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        if (sessions.size > 1) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    "$summary · ${if (expanded) "Hide" else "Details"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoalsFlatColors.Muted,
                )
            }
        } else {
            Text(summary, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GoalsFlatColors.Muted)
        }
        if (sessions.size == 1 || expanded) {
            sessions.sortedBy { it.startedAt }.forEach { session ->
                val start = session.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val end = session.endedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                val dateTime = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()).withZone(IstDateUtils.zone)
                val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(IstDateUtils.zone)
                val timing = when {
                    start != null && end != null -> {
                        val endFormat = if (start.atZone(IstDateUtils.zone).toLocalDate() == end.atZone(IstDateUtils.zone).toLocalDate()) time else dateTime
                        "${dateTime.format(start)} – ${endFormat.format(end)}"
                    }
                    start != null -> dateTime.format(start)
                    end != null -> dateTime.format(end)
                    else -> "Time unavailable"
                }
                Text(
                    buildString {
                        append(timing)
                        if (sessions.size > 1) append(" · ${focusContributionDuration(linkedFocusSeconds(session))}")
                        if (session.timerMode.equals("stopwatch", ignoreCase = true)) append(" · Stopwatch")
                    },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = GoalsFlatColors.Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Text(
            "Study time credited to this goal · also in Ekagra history",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = GoalsFlatColors.Muted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun linkedFocusSeconds(session: GoalLinkedSession): Long =
    if (session.durationSeconds > 0) session.durationSeconds.toLong()
    else session.durationMinutes.coerceAtLeast(0) * 60L

private fun focusContributionDuration(seconds: Long): String = when {
    seconds < 60 -> "$seconds sec"
    seconds % 60 == 0L -> "${seconds / 60} min"
    else -> "${seconds / 60} min ${seconds % 60} sec"
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
