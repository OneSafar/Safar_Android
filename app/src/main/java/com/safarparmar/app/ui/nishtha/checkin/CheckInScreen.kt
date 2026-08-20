package com.safarparmar.app.ui.nishtha.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Mood
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.Amber500
import com.safarparmar.app.ui.theme.Blue500
import com.safarparmar.app.ui.theme.Emerald500
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.Orange500
import com.safarparmar.app.ui.theme.Rose500
import com.safarparmar.app.ui.theme.Slate400
import com.safarparmar.app.ui.theme.Slate500
import com.safarparmar.app.ui.theme.Teal500
import com.safarparmar.app.ui.theme.Violet500
import com.safarparmar.app.ui.theme.isLightBackground
import kotlinx.coroutines.launch

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
    activeBrush: Brush? = null,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    /** When true, [activeBrush] is painted at full track width then clipped to the filled fraction —
     *  so low→high gradient reads correctly as the thumb moves (not compressed into the fill). */
    stretchBrushToFullTrack: Boolean = false,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSizeDp = 14.dp
    val trackHeightDp = 2.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val trackWidthDp = with(density) { trackWidthPx.toDp() }

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
                                val next = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + next * (valueRange.endInclusive - valueRange.start))
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeightDp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(1.dp))
                .background(inactiveColor)
        )
        if (fraction > 0f) {
            if (activeBrush != null && stretchBrushToFullTrack && trackWidthDp > 0.dp) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(trackHeightDp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(1.dp)),
                ) {
                    Box(
                        Modifier
                            .width(trackWidthDp)
                            .height(trackHeightDp)
                            .background(activeBrush),
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(trackHeightDp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(1.dp))
                        .let {
                            if (activeBrush != null) it.background(activeBrush)
                            else it.background(activeColor)
                        },
                )
            }
        }
        val thumbOffsetPx = (trackWidthPx - thumbSizePx) * fraction
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }
        Box(
            Modifier
                .size(thumbSizeDp)
                .offset(x = thumbOffsetDp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(activeColor)
                .border(2.dp, Color.White, CircleShape),
        )
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        CheckInScreenContent(viewModel = viewModel, isLight = isLight)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInScreenContent(
    viewModel: NishthaViewModel,
    isLight: Boolean,
) {
    val moodOptions = remember {
        listOf(
            MoodOption("😄", R.string.mood_happy, Amber500),
            MoodOption("😌", R.string.mood_calm, Teal500),
            MoodOption("😐", R.string.mood_neutral, Slate500),
            MoodOption("😢", R.string.mood_sad, Blue500),
            MoodOption("😠", R.string.mood_angry, Rose500),
            MoodOption("😰", R.string.mood_anxious, Orange500),
            MoodOption("🥱", R.string.mood_tired, Slate400),
            MoodOption("🤩", R.string.mood_excited, Violet500),
            MoodOption("🌱", R.string.mood_motivated, Emerald500),
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    val selectedMoodLabel = selectedMood?.let { stringResource(it.labelRes) }
    var intensity by remember { mutableFloatStateOf(0.5f) }
    var note by remember { mutableStateOf("") }
    var causedBy by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showHistory by remember { mutableStateOf(false) }
    val moodTags = remember { listOf("Work", "Family", "Sleep", "Health", "Relationship", "Finance", "Study", "Other") }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val accent = PlannerFlatColors.PrimaryAccent

    LaunchedEffect(uiState.checkInSuccess) {
        if (uiState.checkInSuccess) {
            Toast.makeText(context, "Check-in saved!", Toast.LENGTH_SHORT).show()
            selectedMood = null
            intensity = 0.5f
            note = ""
            causedBy = ""
            selectedTags = setOf()
            viewModel.onEvent(com.safarparmar.app.ui.nishtha.NishthaEvent.ClearCheckInSuccess)
        }
    }

    LaunchedEffect(uiState.checkInError) {
        if (uiState.checkInError != null) {
            Toast.makeText(context, uiState.checkInError, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlannerFlatColors.BgCream)
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() },
            ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Flat hairline header ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanEyebrow("Nishtha", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                    .clickable { showHistory = !showHistory }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.EditNote else Icons.Default.History,
                        contentDescription = null,
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (showHistory) {
                            stringResource(R.string.checkin_do_checkin)
                        } else {
                            stringResource(R.string.checkin_history)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.TextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.checkin_how_feeling),
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = PlannerFlatColors.TextDark,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.checkin_today),
            fontSize = 13.sp,
            color = PlannerFlatColors.TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        PlanHairline()

        AnimatedVisibility(visible = !showHistory) {
            Column {
                Spacer(Modifier.height(22.dp))
                CheckInSectionLabel(stringResource(R.string.checkin_select_mood))
                Spacer(Modifier.height(14.dp))

                // ── Mood grid — macOS Control Center tiles only ─────────────
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                ) {
                    moodOptions.chunked(3).forEach { rowMoods ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            rowMoods.forEach { mood ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MoodChip(
                                        mood = mood,
                                        selected = selectedMood == mood,
                                        isLight = isLight,
                                        onClick = {
                                            selectedMood = mood
                                            scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedMood != null) {
                    Column {
                        Spacer(Modifier.height(22.dp))
                        PlanHairline(alpha = 0.6f)
                        Spacer(Modifier.height(18.dp))
                        CheckInSectionLabel(stringResource(R.string.checkin_intensity))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.checkin_low),
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                            val highColor = selectedMood?.color ?: accent
                            // Low = muted slate → High = full mood color (clear low→high shift)
                            val lowColor = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B)
                            val currentColor = lerp(lowColor, highColor, intensity)
                            val sliderGradient = remember(lowColor, highColor) {
                                Brush.horizontalGradient(listOf(lowColor, highColor))
                            }
                            SlimSlider(
                                value = intensity,
                                onValueChange = { intensity = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                activeColor = currentColor,
                                activeBrush = sliderGradient,
                                inactiveColor = PlannerFlatColors.BorderSoft,
                                stretchBrushToFullTrack = true,
                            )
                            Text(
                                stringResource(R.string.checkin_high),
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                        val intensityInt = (intensity * 5).toInt().coerceIn(1, 5)
                        val intensityColor = lerp(
                            if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B),
                            selectedMood?.color ?: accent,
                            intensity,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$intensityInt/5",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = intensityColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                PlanHairline(alpha = 0.6f)
                Spacer(Modifier.height(18.dp))
                CheckInSectionLabel(stringResource(R.string.checkin_note_hint))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                        focusedTextColor = PlannerFlatColors.TextDark,
                        unfocusedTextColor = PlannerFlatColors.TextDark,
                        cursorColor = accent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )

                Spacer(Modifier.height(22.dp))
                PlanHairline(alpha = 0.6f)
                Spacer(Modifier.height(18.dp))
                CheckInSectionLabel("Due to")
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    moodTags.forEach { tag ->
                        FlatTagPill(
                            label = tag,
                            selected = tag in selectedTags,
                            accent = accent,
                            onClick = {
                                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                            },
                        )
                    }
                }

                if (uiState.checkInError != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.checkInError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(24.dp))
                PlanHairline()
                Spacer(Modifier.height(18.dp))

                // Flat primary save — not macOS glass
                val saveEnabled = selectedMood != null && !uiState.isCheckingIn
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (saveEnabled) accent else PlannerFlatColors.BorderSoft.copy(alpha = 0.55f),
                        )
                        .clickable(enabled = saveEnabled) {
                            selectedMoodLabel?.let { label ->
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
                                viewModel.createMood(
                                    label,
                                    (intensity * 5).toInt().coerceIn(1, 5),
                                    fullNote.ifBlank { null },
                                )
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiState.isCheckingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(R.string.checkin_save),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showHistory) {
            Column {
                Spacer(Modifier.height(22.dp))
                CheckInSectionLabel(stringResource(R.string.checkin_history_title))
                Spacer(Modifier.height(12.dp))
                PlanHairline()

                if (uiState.moods.isEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.checkin_no_history),
                        color = PlannerFlatColors.TextMuted,
                        fontSize = 13.sp,
                    )
                } else {
                    uiState.moods.take(5).forEachIndexed { index, mood ->
                        HistoryMoodRow(mood = mood)
                        if (index < uiState.moods.take(5).lastIndex) {
                            PlanHairline(alpha = 0.5f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = PlannerFlatColors.TextDark,
    )
}

/** Flat hairline tag — outlined when idle, accent-filled when selected. */
@Composable
private fun FlatTagPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(accent)
                else Modifier.border(1.dp, PlannerFlatColors.BorderSoft, CircleShape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else PlannerFlatColors.TextMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryMoodRow(mood: Mood) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(moodEmoji(mood.mood), fontSize = 24.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    mood.mood.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PlannerFlatColors.TextDark,
                )
                Text(
                    stringResource(R.string.checkin_intensity_value, mood.intensity),
                    fontSize = 11.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            Text(
                mood.timestamp.take(10),
                fontSize = 11.sp,
                color = PlannerFlatColors.TextMuted,
            )
        }

        val notes = mood.notes.orEmpty()
        val causedByLine = notes.lines().firstOrNull { it.startsWith("Caused by:") }?.removePrefix("Caused by:")?.trim()
        val tagsLine = notes.lines().firstOrNull { it.startsWith("Tags:") }?.removePrefix("Tags:")?.trim()
        val tags = tagsLine?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val plainNote = notes.lines()
            .filter { !it.startsWith("Caused by:") && !it.startsWith("Tags:") }
            .joinToString(" ")
            .trim()

        if (plainNote.isNotBlank()) {
            Text(
                plainNote,
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
                modifier = Modifier.padding(start = 34.dp),
                lineHeight = 16.sp,
            )
        }
        if (!causedByLine.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(start = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = PlannerFlatColors.TextMuted,
                )
                Spacer(Modifier.width(4.dp))
                Text(causedByLine, fontSize = 11.sp, color = PlannerFlatColors.TextMuted)
            }
        }
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 34.dp, top = 4.dp),
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(tag, fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
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

/**
 * Mood tile — macOS Control Center recipe (same chrome as Plan-tab exam cards).
 * Body is fully opaque so the cream page never shows through as a Material rectangle.
 * No ripple/indication — glass must stay clean.
 */
@Composable
private fun MoodChip(
    mood: MoodOption,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(mood.labelRes)
    val shape = RoundedCornerShape(20.dp)
    // Opaque Control Center fills — translucent alpha was letting BgCream read as an
    // M3 rectangle inside the glass tile.
    val bodyColor = if (selected) {
        if (isLight) {
            lerp(Color(0xFFF9F9FB), mood.color, 0.18f)
        } else {
            lerp(Color(0xFF2C2C2E), mood.color, 0.28f)
        }
    } else if (isLight) {
        Color(0xFFF9F9FB)
    } else {
        Color(0xFF2C2C2E)
    }
    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            listOf(
                if (selected) mood.color.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                if (selected) mood.color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (selected) mood.color.copy(alpha = 0.6f) else Color(0xFFE5E5EA),
                if (selected) mood.color.copy(alpha = 0.3f) else Color(0xFFD1D1D6),
            ),
        )
    }
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    val subtitleColor = if (selected) {
        mood.color
    } else if (isLight) {
        Color.Black.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) shadowElevation + 2.dp else shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            // Click before clip so any residual indication cannot paint a raw rectangle
            // outside the glass shape; indication is still explicitly null.
            .clip(shape)
            .background(bodyColor, shape)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                brush = borderBrush,
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) mood.color else mood.color.copy(alpha = if (isLight) 0.16f else 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(mood.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
                color = subtitleColor,
                maxLines = 1,
            )
        }
    }
}
