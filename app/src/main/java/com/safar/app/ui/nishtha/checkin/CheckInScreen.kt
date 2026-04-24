package com.safar.app.ui.nishtha.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.R
import com.safar.app.ui.nishtha.NishthaViewModel
import com.safar.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

data class MoodOption(val emoji: String, val labelRes: Int, val color: Color)

// ─── Slim Slider ──────────────────────────────────────────────────────────────
// Minimal 2dp track + small circular notch thumb. No bulk.
@Composable
fun SlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(1f) }
    val thumbSizeDp = 14.dp
    val trackHeightDp = 2.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val fraction =((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(thumbSizeDp)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .pointerInput(valueRange) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.pressed) {
                                change.consume()
                                val fraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + fraction * (valueRange.endInclusive - valueRange.start))
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Inactive track
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeightDp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(1.dp))
                .background(inactiveColor)
        )
        // Active track
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeightDp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(1.dp))
                    .background(activeColor)
            )
        }
        // Thumb — small circular notch
        val thumbOffsetPx = (trackWidthPx - thumbSizePx) * fraction
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }
        Box(
            Modifier
                .size(thumbSizeDp)
                .offset(x = thumbOffsetDp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(activeColor)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    val moodOptions = remember {
        listOf(
            MoodOption("😄", R.string.mood_happy,   Amber500),
            MoodOption("😌", R.string.mood_calm,    Teal500),
            MoodOption("😐", R.string.mood_neutral, Slate500),
            MoodOption("😢", R.string.mood_sad,     Blue500),
            MoodOption("😠", R.string.mood_angry,   Rose500),
            MoodOption("😰", R.string.mood_anxious, Orange500),
            MoodOption("🥱", R.string.mood_tired,   Slate400),
            MoodOption("🤩", R.string.mood_excited, Violet500),
            MoodOption("🌱", R.string.mood_motivated, Emerald500),
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    // Resolve selected mood label in composable scope
    val selectedMoodLabel = selectedMood?.let { stringResource(it.labelRes) }
    var intensity by remember { mutableStateOf(0.5f) }
    var note by remember { mutableStateOf("") }
    var causedBy by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showHistory by remember { mutableStateOf(false) }
    val moodTags = remember { listOf("Work", "Family", "Sleep", "Health", "Relationship", "Finance", "Study", "Other") }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.checkInSuccess) {
        if (uiState.checkInSuccess) {
            Toast.makeText(context, "Check-in saved!", Toast.LENGTH_SHORT).show()
            selectedMood = null
            intensity = 0.5f
            note = ""
            causedBy = ""
            selectedTags = setOf()
            viewModel.onEvent(com.safar.app.ui.nishtha.NishthaEvent.ClearCheckInSuccess)
        }
    }

    LaunchedEffect(uiState.checkInError) {
        if (uiState.checkInError != null) {
            Toast.makeText(context, uiState.checkInError, Toast.LENGTH_SHORT).show()
        }
    }


    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(16.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() }
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.checkin_how_feeling), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.checkin_today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { showHistory = !showHistory }) {
                Icon(if (showHistory) Icons.Default.EditNote else Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (showHistory) stringResource(R.string.checkin_do_checkin) else stringResource(R.string.checkin_history), fontSize = 12.sp)
            }
        }

        AnimatedVisibility(visible = !showHistory) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.checkin_select_mood), style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp, top = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(moodOptions.size) { index ->
                        val mood = moodOptions[index]
                        MoodChip(
                            mood = mood,
                            selected = selectedMood == mood,
                            onClick = {
                                selectedMood = mood
                                scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = selectedMood != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.checkin_intensity), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.checkin_low), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // ── SlimSlider replaces the old M3 Slider ──────────────────────
                            val intensityInt = (intensity * 5).toInt().coerceIn(1, 5)
                            val sliderColor = when (intensityInt) {
                                1, 2 -> Color(0xFF232A8D)
                                3    -> Color(0xFFF08B0F)
                                else -> Color(0xFFED254B)
                            }
                            SlimSlider(
                                value = intensity,
                                onValueChange = { intensity = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                activeColor = sliderColor,
                            )
                            Text(stringResource(R.string.checkin_high), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val intensityInt = (intensity * 5).toInt().coerceIn(1, 5)
                        val numberColor = when (intensityInt) {
                            1, 2 -> Color(0xFF232A8D)
                            3    -> Color(0xFFF08B0F)
                            else -> Color(0xFFED254B)
                        }
                        Text(
                            "$intensityInt/5",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = numberColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.checkin_note_hint)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )

                // ── What caused this mood / Due to ─────────────────────────
                Text("Due to", style = MaterialTheme.typography.titleSmall)
                // Tag chips
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    moodTags.forEach { tag ->
                        val isTagSelected = tag in selectedTags
                        FilterChip(
                            selected = isTagSelected,
                            onClick = {
                                selectedTags = if (isTagSelected) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                }

                if (uiState.checkInError != null) {
                    Text(uiState.checkInError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        selectedMoodLabel?.let { label ->
                            // Encode causedBy and tags into notes
                            val fullNote = buildString {
                                if (note.isNotBlank()) append(note)
                                if (causedBy.isNotBlank()) {
                                    if (isNotEmpty()) append("\n\n")
                                    append("Caused by: $causedBy")
                                }
                                if (selectedTags.isNotEmpty()) {
                                    if (isNotEmpty()) append("\n")
                                    append("Tags: ${selectedTags.joinToString(", ")}")
                                }
                            }
                            viewModel.createMood(label, (intensity * 5).toInt().coerceIn(1, 5), fullNote.ifBlank { null })
                        }
                    },
                    enabled = selectedMood != null && !uiState.isCheckingIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    if (uiState.isCheckingIn) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.checkin_save))
                }
            }
        }

        AnimatedVisibility(visible = showHistory) {
            if (uiState.moods.isEmpty()) {
                Text(stringResource(R.string.checkin_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.checkin_history_title), style = MaterialTheme.typography.titleSmall)
                        uiState.moods.take(5).forEach { mood ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(moodEmoji(mood.mood), fontSize = 24.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(mood.mood.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(stringResource(R.string.checkin_intensity_value, mood.intensity), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(mood.timestamp.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Parse notes field — format: "plain note\n\nCaused by: X\nTags: Y"
                                val notes = mood.notes ?: ""
                                val causedByLine = notes.lines().firstOrNull { it.startsWith("Caused by:") }?.removePrefix("Caused by:")?.trim()
                                val tagsLine = notes.lines().firstOrNull { it.startsWith("Tags:") }?.removePrefix("Tags:")?.trim()
                                val tags = tagsLine?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                // Plain note = everything before "Caused by:" or "Tags:" lines
                                val plainNote = notes.lines()
                                    .filter { !it.startsWith("Caused by:") && !it.startsWith("Tags:") }
                                    .joinToString(" ").trim()
                                if (plainNote.isNotBlank()) {
                                    Text(plainNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp), lineHeight = 16.sp)
                                }
                                if (!causedByLine.isNullOrBlank()) {
                                    Text("💬 $causedByLine", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 32.dp))
                                }
                                if (tags.isNotEmpty()) {
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 32.dp)
                                    ) {
                                        tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Text(tag, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun moodEmoji(mood: String) = when (mood.lowercase()) {
    "happy" -> "😄"; "calm" -> "😌"; "neutral" -> "😐"
    "sad" -> "😢"; "angry" -> "😠"; "anxious" -> "😰"
    "tired" -> "🥱"; "excited" -> "🤩"; "motivated" -> "🌱"
    else -> "😊"
}

@Composable
private fun MoodChip(mood: MoodOption, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(mood.labelRes)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) mood.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                if (selected)
                    Brush.verticalGradient(listOf(mood.color.copy(alpha = 0.22f), mood.color.copy(alpha = 0.07f)))
                else
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(mood.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) mood.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
            )
        }
    }
}
