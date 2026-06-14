package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.safarparmar.app.domain.model.GoalSubtask
import com.safarparmar.app.ui.components.GoalRowSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Review what was completed on a specific day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showHistoryDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())), fontSize = 13.sp)
                }
                if (selectedDate != today) {
                    TextButton(onClick = { selectedDate = today }) { Text("Reset", fontSize = 12.sp) }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing found for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { GoalItem(it, onComplete = {}, onEdit = {}, onDelete = {}, onRepeat = {}) }
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
