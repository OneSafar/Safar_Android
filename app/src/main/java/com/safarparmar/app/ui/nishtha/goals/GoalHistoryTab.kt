package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.GoalLinkedSession
import com.safarparmar.app.domain.model.GoalSubtask
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.ekagra.EkagraViewModel
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.IstDateUtils
import java.time.Instant
import java.time.LocalTime
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
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
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
            val d = (it.endedAt ?: it.startedAt)?.let { ts -> runCatching { Instant.parse(ts).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull() }
            d == selectedDate
        }
    }

    // "View Goal" pill on a linked session scrolls to and briefly highlights the
    // matching completed-goal entry in the Archive list above.
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
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showHistoryDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showHistoryDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = historyDatePickerState)
        }
    }

    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val historyThemeColor = if (isDark) Color(0xFFC084FC) else Color(0xFF5B21B6)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = historyThemeColor)
            Text("Review what was completed on a specific day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showHistoryDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = historyThemeColor),
                    border = BorderStroke(1.dp, historyThemeColor.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())), fontSize = 13.sp)
                }
                if (selectedDate != today) {
                    TextButton(
                        onClick = { selectedDate = today },
                        colors = ButtonDefaults.textButtonColors(contentColor = historyThemeColor)
                    ) { Text("Reset", fontSize = 12.sp) }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        if (filtered.isEmpty() && linkedSessionsForDate.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing found for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(filtered, key = { _, goal -> goal.id }) { index, goal ->
                val isHighlighted = highlightedGoalId == goal.id
                val highlightColor by animateColorAsState(
                    targetValue = if (isHighlighted) historyThemeColor.copy(alpha = 0.18f) else Color.Transparent,
                    label = "goalHighlight",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(highlightColor)
                ) {
                    GoalItem(goal, onComplete = {}, onEdit = {}, onDelete = {}, onRepeat = {})
                }
            }
            if (linkedSessionsForDate.isNotEmpty()) {
                item(key = "linked_sessions_header") {
                    Text(
                        "Linked Ekagra Sessions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = historyThemeColor,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(linkedSessionsForDate, key = { "linked_${it.id}" }) { session ->
                    LinkedEkagraSessionRow(
                        session = session,
                        themeColor = historyThemeColor,
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
                }
            }
        }
    }
}

@Composable
private fun LinkedEkagraSessionRow(
    session: GoalLinkedSession,
    themeColor: Color,
    onViewGoal: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val start = session.startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val end = session.endedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())
    val timeRange = if (start != null && end != null) {
        "${timeFormatter.format(start)} – ${timeFormatter.format(end)}"
    } else null

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        border = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        session.goalTitle ?: if (session.goalExists) "Untitled goal" else "Deleted goal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // "Ekagra" badge marks this goal completion as sourced from the timer,
                    // distinguishing it from a manually marked-complete goal.
                    Text(
                        "Ekagra",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onTertiaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(scheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
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
                    color = scheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onViewGoal,
                colors = ButtonDefaults.textButtonColors(contentColor = themeColor),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("View Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun GoalTimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.07f)).border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
    }
}
