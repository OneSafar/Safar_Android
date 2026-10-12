package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitFormBottomSheet(
    habit: HabitEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: suspend (String, Set<DayOfWeek>, Boolean) -> Unit,
    onSuccess: (String, Set<DayOfWeek>) -> Unit = { _, _ -> },
    onArchive: (suspend () -> Unit)? = null,
    onDelete: (suspend () -> Unit)? = null
) {
    var name by rememberSaveable(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var dayMask by rememberSaveable(habit?.id) {
        mutableIntStateOf(habit?.targetDays?.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) } ?: 127)
    }
    var specificDays by rememberSaveable(habit?.id) { mutableStateOf(habit?.let { !it.isEveryDay } ?: false) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var daysError by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val selectedDays = DayOfWeek.entries.filter { dayMask and (1 shl (it.value - 1)) != 0 }.toSet()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    fun dismissSafely() {
        if (busy) return
        focus.clearFocus()
        onDismiss()
    }

    fun submit() {
        if (busy) return
        nameError = name.isBlank()
        daysError = specificDays && selectedDays.isEmpty()
        if (nameError || daysError) return

        val savedName = name.trim()
        val savedDays = if (specificDays) selectedDays else DayOfWeek.entries.toSet()
        busy = true
        error = null
        scope.launch {
            try {
                onSubmit(savedName, savedDays, !specificDays)
                onSuccess(savedName, savedDays)
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "Unable to save habit. Please try again."
                busy = false
            }
        }
    }

    fun runArchive() {
        if (busy || onArchive == null) return
        busy = true
        scope.launch {
            try {
                onArchive()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                busy = false
                confirmArchive = false
                error = "Unable to archive habit. Please try again."
            }
        }
    }

    fun runDelete() {
        if (busy || onDelete == null) return
        busy = true
        scope.launch {
            try {
                onDelete()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                busy = false
                confirmDelete = false
                error = "Unable to delete habit. Please try again."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = ::dismissSafely,
        sheetState = sheetState,
        containerColor = HabitColors.Background,
        contentColor = HabitColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 5.dp)
                    .size(width = 50.dp, height = 5.dp)
                    .background(HabitColors.TextSecondary, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (habit == null) androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_new) else "Edit Habit",
                        color = HabitColors.TextPrimary,
                        fontSize = 27.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (habit == null) "Set up a routine and choose which days it repeats." else "Update your habit name and repeat schedule.",
                        color = HabitColors.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                IconButton(onClick = ::dismissSafely, enabled = !busy, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_close), tint = HabitColors.TextPrimary)
                }
            }

            FormSection {
                FormLabel("Habit name")
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        if (name.isNotBlank()) nameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_name_example), color = HabitColors.TextSecondary) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) ({ Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_name_required), color = HabitColors.Error) }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HabitColors.Surface,
                        unfocusedContainerColor = HabitColors.Surface,
                        focusedBorderColor = HabitColors.RoyalPurple,
                        unfocusedBorderColor = HabitColors.OutlineStrong,
                        focusedTextColor = HabitColors.TextPrimary,
                        unfocusedTextColor = HabitColors.TextPrimary,
                        cursorColor = HabitColors.RoyalPurple
                    )
                )
            }

            FormSection(showDivider = false) {
                FormLabel("Repeat schedule")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(HabitColors.Parchment)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScheduleChoice("Every day", selected = !specificDays, enabled = !busy, modifier = Modifier.weight(1f)) {
                        specificDays = false
                        dayMask = 127
                        daysError = false
                    }
                    ScheduleChoice("Specific days", selected = specificDays, enabled = !busy, modifier = Modifier.weight(1f)) {
                        specificDays = true
                        if (dayMask == 127) dayMask = weekdayMask()
                    }
                }

                AnimatedVisibility(visible = specificDays) {
                    Column(Modifier.padding(top = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            MondayFirst.forEach { day ->
                                val selected = day in selectedDays
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (selected) HabitColors.RoyalPurpleBg else HabitColors.Surface)
                                        .border(1.dp, if (selected) HabitColors.RoyalPurple.copy(alpha = .35f) else HabitColors.Outline, RoundedCornerShape(11.dp))
                                        .clickable(enabled = !busy) {
                                            val bit = 1 shl (day.value - 1)
                                            dayMask = dayMask xor bit
                                            daysError = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        color = if (selected) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (daysError) Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_day_required), color = HabitColors.Error, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (specificDays) {
                        if (selectedDays.isEmpty()) androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_day_required)
                        else "Repeats on ${scheduleSummary(selectedDays)}"
                    } else "Repeats every day",
                    color = HabitColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = HabitColors.Error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = ::submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)
            ) {
                Text(if (busy) "Saving…" else if (habit == null) "Create Habit" else "Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (habit != null && (onArchive != null || onDelete != null)) {
                Spacer(Modifier.height(8.dp))
                if (onArchive != null) {
                    TextButton(onClick = { confirmArchive = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp), tint = HabitColors.TextSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_archive), color = HabitColors.TextSecondary, fontSize = 13.sp)
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = HabitColors.Error)
                        Spacer(Modifier.width(5.dp))
                        Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_delete_permanently), color = HabitColors.Error, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_archive_question)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_archive_explanation)) },
            confirmButton = { TextButton(onClick = ::runArchive) { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_archive_action)) } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_cancel)) } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_delete_question)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_delete_explanation)) },
            confirmButton = { TextButton(onClick = ::runDelete) { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_delete), color = HabitColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun FormSection(showDivider: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
    if (showDivider) HorizontalDivider(color = HabitColors.Outline)
}

@Composable
private fun FormLabel(label: String) {
    Text(
        label.uppercase(Locale.getDefault()),
        color = HabitColors.TextTertiary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = .8.sp
    )
}

@Composable
private fun ScheduleChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HabitColors.Surface else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, HabitColors.Outline, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) HabitColors.RoyalPurple else HabitColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun weekdayMask(): Int = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
).fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
