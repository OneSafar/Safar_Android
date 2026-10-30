package com.safarparmar.app.feature.habits.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.safarparmar.app.feature.habits.timetable.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun TimetableSessionEditor(
    initial: TimetableSession,
    isNew: Boolean,
    subjects: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (TimetableSession) -> Unit,
    onDelete: suspend (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Keep the generated identity stable across recompositions and process recreation.
    val id = rememberSaveable { initial.id }
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var subject by rememberSaveable { mutableStateOf(initial.subject) }
    var kind by rememberSaveable { mutableIntStateOf(initial.kind.ordinal) }
    var date by rememberSaveable { mutableStateOf(initial.date) }
    var start by rememberSaveable { mutableIntStateOf(initial.startMinute) }
    var end by rememberSaveable { mutableIntStateOf(initial.endMinute) }
    var repeat by rememberSaveable { mutableIntStateOf(initial.repeat.ordinal) }
    var reminder by rememberSaveable { mutableIntStateOf(initial.reminderMinutes ?: -1) }
    var linked by rememberSaveable { mutableStateOf(initial.trackAsHabit) }
    var color by rememberSaveable { mutableIntStateOf(initial.color) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var subjectMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    BackHandler { if (!busy) onDismiss() }
    fun save() {
        if (busy) return
        error = null
        val session = initial.copy(id = id, title = title.trim(), subject = subject.trim().ifBlank { "General" }, kind = SessionKind.entries[kind],
            date = date, startMinute = start, endMinute = end, repeat = SessionRepeat.entries[repeat], reminderMinutes = reminder.takeIf { it >= 0 }, color = color, trackAsHabit = linked)
        try { session.validate() } catch (e: Exception) { error = e.message; return }
        busy = true
        scope.launch {
            try { onSave(session) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not save session. Try again." }
            finally { busy = false }
        }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HabitColors.RoyalPurple, unfocusedBorderColor = HabitColors.Outline,
        focusedTextColor = HabitColors.TextPrimary, unfocusedTextColor = HabitColors.TextPrimary, cursorColor = HabitColors.RoyalPurple,
        focusedContainerColor = HabitColors.Background, unfocusedContainerColor = HabitColors.Background)
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = HabitColors.TextPrimary) }
            Text(if (isNew) "Add Timetable Activity" else "Edit Timetable Activity", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = HabitColors.TextPrimary)
            FilledIconButton(onClick = ::save, enabled = !busy, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon), colors = IconButtonDefaults.filledIconButtonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)) { Icon(Icons.Rounded.Check, "Save session") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ReferenceTabs(SessionKind.entries.map { it.label }, kind, { kind = it })
            HabitContentPanel {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorLabel("Title")
                OutlinedTextField(title, { if (it.length <= 100) title = it }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                    placeholder = { Text("e.g. Physics – Chapter 5", fontSize = 14.sp) }, shape = RoundedCornerShape(14.dp), colors = fieldColors)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorLabel("Subject")
                Box {
                    OutlinedTextField(subject, { if (it.length <= 60) subject = it }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                        leadingIcon = { Text(SessionKind.entries[kind].symbol, fontSize = 22.sp) },
                        trailingIcon = { IconButton(onClick = { subjectMenu = true }) { Icon(Icons.Rounded.ExpandMore, "Choose subject", tint = HabitColors.TextSecondary) } },
                        shape = RoundedCornerShape(14.dp), colors = fieldColors)
                    DropdownMenu(subjectMenu, { subjectMenu = false }, containerColor = HabitColors.Surface) {
                        (subjects + listOf("General", "Physics", "Mathematics", "Chemistry", "Biology", "History", "English")).distinct().forEach { value ->
                            DropdownMenuItem(text = { Text(value, color = HabitColors.TextPrimary) }, onClick = { subject = value; subjectMenu = false })
                        }
                    }
                }
            }
            EditorField("Date") {
                EditorPicker(LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), !busy) {
                    val current = LocalDate.parse(date)
                    DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, current.year, current.monthValue - 1, current.dayOfMonth).show()
                }
            }
            EditorField("Time") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { EditorPicker(sessionTime(start), !busy, showArrow = false) {
                        TimePickerDialog(context, { _, hour, minute -> start = hour * 60 + minute }, start / 60, start % 60, DateFormat.is24HourFormat(context)).show()
                    } }
                    Text("to", color = HabitColors.TextSecondary, fontSize = 13.sp)
                    Box(Modifier.weight(1f)) { EditorPicker(sessionTime(end), !busy, showArrow = false) {
                        TimePickerDialog(context, { _, hour, minute -> end = hour * 60 + minute }, end / 60, end % 60, DateFormat.is24HourFormat(context)).show()
                    } }
                }
            }
            EditorField("Repeat") {
                Box {
                    EditorPicker(SessionRepeat.entries[repeat].label, !busy) { repeatMenu = true }
                    DropdownMenu(repeatMenu, { repeatMenu = false }, containerColor = HabitColors.Surface) {
                        SessionRepeat.entries.forEach { value -> DropdownMenuItem(text = { Text(value.label, color = HabitColors.TextPrimary) }, onClick = { repeat = value.ordinal; repeatMenu = false }) }
                    }
                }
            }
            EditorField("Reminder") {
                Box {
                    EditorPicker(when (reminder) { -1 -> "None"; 0 -> "At start time"; else -> "$reminder minutes before" }, !busy) { reminderMenu = true }
                    DropdownMenu(reminderMenu, { reminderMenu = false }, containerColor = HabitColors.Surface) {
                        listOf(-1, 0, 5, 10, 15, 30).forEach { value -> DropdownMenuItem(text = { Text(when(value) { -1 -> "None"; 0 -> "At start time"; else -> "$value minutes before" }, color = HabitColors.TextPrimary) }, onClick = {
                            reminder = value; reminderMenu = false
                            if (value >= 0 && !permissionGranted && Build.VERSION.SDK_INT >= 33) requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) }
                    }
                }
            }
            if (reminder >= 0 && !permissionGranted) Text("Notifications are off. Allow them in Android settings to receive reminders.", color = HabitColors.TextSecondary, fontSize = 12.sp)
            EditorField("Color") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SessionPastels.forEachIndexed { index, value ->
                        val selected = color == index
                        val popScale by rememberHabitCompletionPop(selected)
                        Box(
                            Modifier.size(40.dp)
                                .habitClickable(enabled = !busy, scaleDown = HabitMotion.ScaleChip, role = Role.RadioButton) { color = index }
                                .graphicsLayer {
                                    scaleX = popScale
                                    scaleY = popScale
                                }
                                .clip(CircleShape)
                                .background(if (selected) SessionAccents[index] else value)
                                .semantics { contentDescription = listOf("Green", "Rose", "Blue", "Peach", "Lavender")[index]; this.selected = selected },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Also show in Habits", color = HabitColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("One check updates both lists", color = HabitColors.TextSecondary, fontSize = 12.sp)
                }
                Switch(checked = linked, onCheckedChange = { linked = it }, enabled = !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = HabitColors.RoyalPurple, checkedThumbColor = HabitColors.OnRoyalPurple))
            }
            }
            }
            error?.let { Text(it, color = HabitColors.Error, fontSize = 13.sp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Spacer(Modifier.height(18.dp))
            Button(onClick = ::save, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).habitPress(scaleDown = HabitMotion.ScaleButton), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = HabitColors.OnRoyalPurple, strokeWidth = 2.dp) else Text("Save", fontSize = 16.sp)
            }
            if (!isNew) TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally).habitPress(scaleDown = HabitMotion.ScaleButton)) { Text("Delete Activity", color = HabitColors.Error) }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = HabitColors.Surface,
        title = { Text("Delete this timetable activity?", color = HabitColors.TextPrimary) }, text = { Text("This removes its schedule and timetable completion history.", color = HabitColors.TextSecondary) },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false; busy = true
            scope.launch { try { onDelete(id) } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message } finally { busy = false } }
        }) { Text("Delete", color = HabitColors.Error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}

@Composable private fun EditorLabel(label: String) { Text(label, color = HabitColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
@Composable private fun EditorField(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.width(82.dp)) { EditorLabel(label) }; Box(Modifier.weight(1f)) { content() } }
}
@Composable private fun EditorPicker(label: String, enabled: Boolean, showArrow: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.dp, HabitColors.Outline, RoundedCornerShape(13.dp))
        .habitClickable(enabled = enabled, scaleDown = HabitMotion.ScaleCard, onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = HabitColors.TextPrimary, fontSize = 13.sp)
        if (showArrow) Icon(Icons.Rounded.ExpandMore, null, tint = HabitColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}
