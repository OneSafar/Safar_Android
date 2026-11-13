package com.safarparmar.app.ui.nishtha.checkin

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.ui.platform.LocalResources

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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.composables.ui.components.Text
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import com.safarparmar.app.ui.nishtha.NishthaWarmTheme
import com.safarparmar.app.ui.nishtha.nishthaWarmColors
import com.safarparmar.app.ui.nishtha.journal.JournalMockupFont
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
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.ui.nishtha.NishthaUiState
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import kotlinx.coroutines.flow.drop
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
fun CheckInScreen(
    viewModel: NishthaViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.loadTab(com.safarparmar.app.ui.nishtha.NishthaTab.CHECK_IN) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var draftBusy by remember { mutableStateOf(false) }
    com.safarparmar.app.ui.tutorial.ScreenPracticeEntry(com.safarparmar.app.ui.tutorial.TitliFeature.CHECK_IN,
        !isLight, eligible = !state.isLoadingMoods && !state.isCheckingIn && !draftBusy)
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        CheckInScreenContent(
            uiState = state,
            isLight = isLight,
            onClearSuccess = { viewModel.onEvent(com.safarparmar.app.ui.nishtha.NishthaEvent.ClearCheckInSuccess) },
            onCreateMood = { mood, intensity, notes -> viewModel.createMood(mood, intensity, notes) },
            onDraftChange = { draftBusy = it },
            onOpenCalmPractice = { practice ->
                onNavigate(com.safarparmar.app.ui.navigation.Routes.calmCentreWithPractice(practice?.name))
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CheckInScreenContent(
    uiState: NishthaUiState, isLight: Boolean,
    onClearSuccess: () -> Unit = {}, onCreateMood: (String, Int, String?) -> Unit = { _, _, _ -> },
    onDraftChange: (Boolean) -> Unit = {}, initialMoodIndex: Int? = null, initialHistory: Boolean = false,
    onOpenCalmPractice: (com.safarparmar.app.ui.calmcentre.CalmPractice?) -> Unit = {},
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

    var selectedMood by remember { mutableStateOf(initialMoodIndex?.let(moodOptions::getOrNull)) }
    LaunchedEffect(selectedMood) { onDraftChange(selectedMood != null) }
    val selectedMoodLabel = selectedMood?.let { stringResource(it.labelRes) }
    var intensity by remember { mutableFloatStateOf(0.5f) }
    var note by remember { mutableStateOf("") }
    var causedBy by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var showHistory by remember { mutableStateOf(initialHistory) }
    // Keep stored tag values stable; only the visible labels follow the app language.
    val moodTags = remember {
        listOf(
            "Work" to R.string.checkin_reason_work,
            "Family" to R.string.checkin_reason_family,
            "Sleep" to R.string.checkin_reason_sleep,
            "Health" to R.string.checkin_reason_health,
            "Relationship" to R.string.checkin_reason_relationship,
            "Finance" to R.string.checkin_reason_finance,
            "Study" to R.string.checkin_reason_study,
            "Other" to R.string.checkin_reason_other,
        )
    }

    val context = LocalContext.current
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(uiState.checkInSuccess) {
        if (uiState.checkInSuccess) {
            Toast.makeText(context, localizedResources.getString(com.safarparmar.app.R.string.nishtha_checkin_saved), Toast.LENGTH_SHORT).show()
            selectedMood = null
            intensity = 0.5f
            note = ""
            causedBy = ""
            selectedTags = setOf()
            onClearSuccess()
        }
    }

    LaunchedEffect(uiState.checkInError) {
        if (uiState.checkInError != null) {
            Toast.makeText(context, uiState.checkInError, Toast.LENGTH_SHORT).show()
        }
    }

    val c = nishthaWarmColors(isLight)
    NishthaWarmTheme(isLight) {
        Box(Modifier.fillMaxSize().background(c.canvas), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().verticalScroll(scrollState).imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // The long localized title and History action stack when space is tight.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val historyButton: @Composable () -> Unit = {
                        com.composables.ui.components.Button(onClick = { focusManager.clearFocus(); showHistory = !showHistory },
                            style = com.composables.ui.components.ButtonStyle.Outlined, shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            com.composables.ui.components.Icon(if (showHistory) Icons.Default.EditNote else Icons.Default.History,
                                contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
                            Text(stringResource(if (showHistory) R.string.checkin_do_checkin else R.string.checkin_history),
                                color = c.ink, fontFamily = JournalMockupFont, fontSize = 12.sp)
                        }
                    }
                    val heading: @Composable () -> Unit = {
                        Text(stringResource(R.string.checkin_title_today), color = c.ink, fontFamily = JournalMockupFont,
                            fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 37.sp)
                    }
                    if (maxWidth >= 560.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(Modifier.weight(1f)) { heading() }
                            historyButton()
                        }
                    } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        heading()
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { historyButton() }
                    }
                }
                if (!showHistory) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.paper).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LilacCheckInLabel(stringResource(R.string.checkin_select_mood), c.ink)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                        ) {
                            moodOptions.chunked(3).forEachIndexed { rowIndex, rowMoods ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().then(if (rowIndex == 0) Modifier.tutorialTarget("checkin.mood") else Modifier),
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

                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.lilac)
                        .border(1.dp, c.paper, RoundedCornerShape(24.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnimatedVisibility(visible = selectedMood != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LilacCheckInLabel(stringResource(R.string.checkin_intensity), c.ink)
                                    Text("${(intensity * 5).toInt().coerceIn(1, 5)}/5", color = c.muted, fontFamily = JournalMockupFont, fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.checkin_low), color = c.muted, fontSize = 12.sp)
                                    val lowColor = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    val highColor = selectedMood?.color ?: c.action
                                    SlimSlider(value = intensity, onValueChange = { intensity = it },
                                        modifier = Modifier.weight(1f).tutorialTarget("checkin.details").padding(horizontal = 12.dp),
                                        activeColor = lerp(lowColor, highColor, intensity),
                                        activeBrush = Brush.horizontalGradient(listOf(lowColor, highColor)),
                                        inactiveColor = c.line, stretchBrushToFullTrack = true)
                                    Text(stringResource(R.string.checkin_high), color = c.muted, fontSize = 12.sp)
                                }

                                val recommendedPractice = remember(selectedMood) {
                                    when (selectedMood?.labelRes) {
                                        R.string.mood_anxious -> com.safarparmar.app.ui.calmcentre.CalmPractice.GROUND
                                        R.string.mood_sad -> com.safarparmar.app.ui.calmcentre.CalmPractice.KINDNESS
                                        R.string.mood_tired -> com.safarparmar.app.ui.calmcentre.CalmPractice.REST
                                        R.string.mood_angry -> com.safarparmar.app.ui.calmcentre.CalmPractice.BREATHE
                                        else -> com.safarparmar.app.ui.calmcentre.CalmPractice.BREATHE
                                    }
                                }
                                CheckInCalmCard(
                                    practice = recommendedPractice,
                                    isLight = isLight,
                                    onClick = { onOpenCalmPractice(recommendedPractice) },
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LilacCheckInLabel(stringResource(R.string.checkin_note_hint), c.ink)
                            LilacCheckInNotes(note, { note = it }, !uiState.isCheckingIn, isLight)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LilacCheckInLabel(stringResource(R.string.checkin_due_to), c.ink)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                moodTags.forEach { (tag, labelRes) ->
                                    val selected = tag in selectedTags
                                    com.composables.ui.components.Button(
                                        onClick = { selectedTags = if (selected) selectedTags - tag else selectedTags + tag },
                                        style = if (selected) com.composables.ui.components.ButtonStyle.Primary else com.composables.ui.components.ButtonStyle.Outlined,
                                        shape = RoundedCornerShape(14.dp), enabled = !uiState.isCheckingIn,
                                        modifier = Modifier.background(if (selected) Color.Transparent else c.paper, RoundedCornerShape(14.dp))
                                            .semantics { this.selected = selected },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text(stringResource(labelRes), color = if (selected) c.onAction else c.ink,
                                            fontFamily = JournalMockupFont, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                        uiState.checkInError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                        com.composables.ui.components.Button(
                            onClick = {
                                focusManager.clearFocus()
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
                                    onCreateMood(
                                        label,
                                        (intensity * 5).toInt().coerceIn(1, 5),
                                        fullNote.ifBlank { null },
                                    )
                                }
                            },
                            enabled = selectedMood != null && !uiState.isCheckingIn,
                            style = com.composables.ui.components.ButtonStyle.Primary, shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).tutorialTarget("checkin.save")) {
                            if (uiState.isCheckingIn) com.safarparmar.app.ui.components.SafarCircularProgressIndicator(
                                modifier = Modifier.size(16.dp), color = c.onAction, strokeWidth = 2.dp)
                            Text(stringResource(if (uiState.isCheckingIn) R.string.checkin_saving else R.string.checkin_save),
                                color = c.onAction, fontFamily = JournalMockupFont, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.paper).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LilacCheckInLabel(stringResource(R.string.checkin_history_title), c.ink)
                        if (uiState.isLoadingMoods) com.safarparmar.app.ui.components.SafarCircularProgressIndicator()
                        else if (uiState.moods.isEmpty()) Text(stringResource(R.string.checkin_no_history), color = c.muted, fontSize = 13.sp)
                        else uiState.moods.take(5).forEachIndexed { index, mood ->
                            Box(Modifier.tutorialTarget("checkin.history")) { HistoryMoodRow(mood) }
                            if (index < uiState.moods.take(5).lastIndex) androidx.compose.material3.HorizontalDivider(color = c.line)
                        }
                        CheckInCalmHistoryCard(
                            isLight = isLight,
                            onClick = { onOpenCalmPractice(null) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LilacCheckInLabel(label: String, color: Color) {
    Text(label, color = color, fontFamily = JournalMockupFont, fontSize = 17.sp,
        lineHeight = 25.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun LilacCheckInNotes(value: String, onValueChange: (String) -> Unit, enabled: Boolean, light: Boolean) {
    val c = nishthaWarmColors(light)
    val field = androidx.compose.foundation.text.input.rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    LaunchedEffect(field) {
        androidx.compose.runtime.snapshotFlow { field.text.toString() }.drop(1).collect { callback(it) }
    }
    LaunchedEffect(value) {
        if (field.text.toString() != value) field.edit { replace(0, length, value) }
    }
    com.composables.ui.components.TextField(state = field, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
        accessibilityLabel = stringResource(R.string.checkin_note_hint),
        placeholder = { Text(stringResource(R.string.checkin_add_note), color = c.muted, fontFamily = JournalMockupFont) },
        lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine(minHeightInLines = 2),
        shape = RoundedCornerShape(14.dp), backgroundColor = c.paper, contentColor = c.ink,
        borderColor = c.line, fontFamily = JournalMockupFont, fontSize = 14.sp,
        contentPadding = PaddingValues(14.dp))
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

private data class ParsedMoodDetails(
    val userNote: String?,
    val reasons: List<String>,
    val causedByText: String?,
)

private fun parseMoodNotes(rawNotes: String?): ParsedMoodDetails {
    if (rawNotes.isNullOrBlank()) return ParsedMoodDetails(null, emptyList(), null)

    var text = rawNotes.trim()
    var extractedDueTo: String? = null
    var extractedCausedBy: String? = null
    var extractedTags: List<String> = emptyList()

    // 1. Check for "Tags: ..." line
    val tagsMatch = Regex("""(?i)Tags:\s*([^\n]+)""").find(text)
    if (tagsMatch != null) {
        extractedTags = tagsMatch.groupValues[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
        text = text.replace(tagsMatch.value, "").trim()
    }

    // 2. Check for "Caused by: ..." line
    val causedMatch = Regex("""(?i)Caused by:\s*([^\n]+)""").find(text)
    if (causedMatch != null) {
        extractedCausedBy = causedMatch.groupValues[1].trim()
        text = text.replace(causedMatch.value, "").trim()
    }

    // 3. Check for "Due to: ..." or "Due to" (case-insensitive)
    val dueToMatch = Regex("""(?i)Due to:\s*(.+)""", RegexOption.DOT_MATCHES_ALL).find(text)
    if (dueToMatch != null) {
        extractedDueTo = dueToMatch.groupValues[1].trim()
        text = text.substring(0, dueToMatch.range.first).trim()
    }

    // Parse all reasons into a combined list of tag strings
    val allReasons = mutableListOf<String>()
    if (!extractedDueTo.isNullOrBlank()) {
        allReasons.addAll(extractedDueTo.split(",").map { it.trim() }.filter { it.isNotEmpty() })
    }
    allReasons.addAll(extractedTags)

    val cleanNote = text.replace(Regex("""\n+"""), " ").trim().ifBlank { null }
    val cleanCausedBy = extractedCausedBy?.ifBlank { null }

    return ParsedMoodDetails(
        userNote = cleanNote,
        reasons = allReasons.distinct(),
        causedByText = cleanCausedBy
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryMoodRow(mood: Mood) {
    val details = remember(mood.notes) { parseMoodNotes(mood.notes) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    // Standout reason color scheme: Rich Purple / Lilac accents
    val reasonBadgeBg = if (!isLight) Color(0xFF311E52) else Color(0xFFF3E8FF)
    val reasonTextTint = if (!isLight) Color(0xFFD8B4FE) else Color(0xFF7C3AED)
    val reasonLabelColor = if (!isLight) Color(0xFFC084FC) else Color(0xFF6B21A8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Top Row: Emoji + Mood Name & Intensity + Timestamp ──────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PlannerFlatColors.BorderSoft.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(moodEmoji(mood.mood), fontSize = 22.sp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mood.mood.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PlannerFlatColors.TextDark,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PlannerFlatColors.BorderSoft.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = stringResource(R.string.checkin_intensity_value, mood.intensity),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = mood.timestamp.take(10),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PlannerFlatColors.TextMuted,
            )
        }

        // ── User's Custom Note (if any) ─────────────────────────────────────
        if (!details.userNote.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PlannerFlatColors.BorderSoft.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp),
            ) {
                Text(
                    text = details.userNote,
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextDark,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // ── Caused By / Explanation (if any) ─────────────────────────────────
        if (!details.causedByText.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(start = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = reasonLabelColor,
                )
                Text(
                    text = details.causedByText,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── "Due to" Reasons / Tags (Highlighted in distinct reason color!) ───
        if (details.reasons.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Due to:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = reasonLabelColor,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details.reasons.forEach { reason ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = reasonBadgeBg,
                            border = BorderStroke(0.8.dp, reasonTextTint.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = reason,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = reasonTextTint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
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

@Composable
private fun CheckInCalmCard(
    practice: com.safarparmar.app.ui.calmcentre.CalmPractice,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isLight) Color(0xFFFBF6FE) else Color(0xFF2C2235)
    val border = if (isLight) Color(0xFFE9DCF5) else Color(0xFF4A3855)
    val ink = if (isLight) Color(0xFF2E1738) else Color(0xFFF4ECF7)
    val muted = if (isLight) Color(0xFF755E80) else Color(0xFFBCA9C2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFEFE2FB) else Color(0xFF402E4E)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = com.safarparmar.app.ui.theme.RoyalPurple,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MOMENT OF CALM",
                        color = muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = practice.title,
                        color = ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "2 mins",
                    color = com.safarparmar.app.ui.theme.RoyalPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = practice.description,
                color = muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.checkin_calm_recommendation_action),
                    color = com.safarparmar.app.ui.theme.RoyalPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CheckInCalmHistoryCard(
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isLight) Color(0xFFF9F5FD) else Color(0xFF261D2F)
    val border = if (isLight) Color(0xFFECE3F5) else Color(0xFF3F304A)
    val ink = if (isLight) Color(0xFF2E1738) else Color(0xFFF4ECF7)
    val muted = if (isLight) Color(0xFF755E80) else Color(0xFFBCA9C2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isLight) Color(0xFFEFE2FB) else Color(0xFF3A2848)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = com.safarparmar.app.ui.theme.RoyalPurple,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_calm_centre),
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Take a 2-min pause to center your mind",
                    color = muted,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "Open →",
                color = com.safarparmar.app.ui.theme.RoyalPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
