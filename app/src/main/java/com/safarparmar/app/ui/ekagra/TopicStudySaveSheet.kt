package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal sealed interface TopicStudySheetState {
    data object ReadyToSave : TopicStudySheetState
    data object Saving : TopicStudySheetState
    data object Saved : TopicStudySheetState
    data object SavedOnPhone : TopicStudySheetState
    data object MarkingDone : TopicStudySheetState
    data class TopicError(val message: String) : TopicStudySheetState
}

internal fun topicStudyActualSeconds(pending: PendingEndedEkagraSession): Int =
    if (pending.mode.equals("stopwatch", ignoreCase = true)) {
        pending.secondsLeft
    } else {
        pending.totalSeconds - pending.secondsLeft
    }.coerceAtLeast(0)

internal fun formatTopicStudyTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "$minutes min $seconds sec"
        minutes > 0 -> "$minutes min"
        else -> "$seconds sec"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicStudySaveSheet(
    sheetState: SheetState,
    pending: PendingEndedEkagraSession,
    state: TopicStudySheetState,
    selectedTheme: VisualTheme,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNotYet: () -> Unit,
    onFinished: () -> Unit,
    onDiscard: () -> Unit,
) {
    val timeText = formatTopicStudyTime(topicStudyActualSeconds(pending))
    // Mirrors the backdrop branch in EkagraScreen: a gradient always wins over
    // the video, so a theme only reads as "video" when it has no gradient.
    val isVideoTheme = selectedTheme.gradientColors == null && selectedTheme.videoUrl.isNotBlank()
    val ink = rememberEkagraInk(
        onCanvas = isVideoTheme,
        theme = selectedTheme,
        isDarkTheme = isDarkTheme,
    )
    val container = if (isVideoTheme) Color(0xF2131718) else MaterialTheme.colorScheme.surface
    val accent = selectedTheme.accent
    val canDismiss = state == TopicStudySheetState.Saved ||
        state == TopicStudySheetState.SavedOnPhone

    ModalBottomSheet(
        onDismissRequest = { if (canDismiss) onDismiss() },
        sheetState = sheetState,
        containerColor = container,
        contentColor = ink.primaryText,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
        ),
        dragHandle = { BottomSheetDefaults.DragHandle(color = ink.hairline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EkagraEyebrow("Study finished", ink.mutedText)
            EkagraDisplayTitle(
                pending.topicTitle ?: "Exam Planner topic",
                ink.primaryText,
            )
            Text(
                timeText,
                fontFamily = EkagraSerif,
                fontSize = 36.sp,
                color = accent,
                textAlign = TextAlign.Center,
            )
            Text(
                "Study time",
                fontSize = 12.sp,
                color = ink.mutedText,
            )
            EkagraHairline(ink.hairline)

            when (state) {
                TopicStudySheetState.ReadyToSave -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EkagraGhostAction(
                            label = "Discard",
                            ink = ink,
                            onClick = onDiscard,
                            modifier = Modifier.weight(1f),
                        )
                        EkagraPrimaryAction(
                            label = "Save",
                            accent = accent,
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                TopicStudySheetState.Saving -> {
                    CircularProgressIndicator()
                    Text("Saving study time…", color = ink.secondaryText)
                }

                TopicStudySheetState.Saved,
                TopicStudySheetState.SavedOnPhone,
                is TopicStudySheetState.TopicError -> {
                    Text(
                        if (state == TopicStudySheetState.SavedOnPhone) {
                            "✓ Study time saved\nIf internet is off, progress will update later."
                        } else {
                            "✓ Study time saved"
                        },
                        color = accent,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Did you finish this topic?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ink.primaryText,
                    )
                    if (state is TopicStudySheetState.TopicError) {
                        Text(
                            "Study time is saved.\n${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        EkagraGhostAction(
                            label = "Not yet",
                            ink = ink,
                            onClick = onNotYet,
                            modifier = Modifier.weight(1f),
                        )
                        EkagraPrimaryAction(
                            label = if (state is TopicStudySheetState.TopicError) "Try again" else "Yes",
                            accent = accent,
                            onClick = onFinished,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                TopicStudySheetState.MarkingDone -> {
                    Text(
                        "✓ Study time saved",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                    CircularProgressIndicator(color = accent)
                    Text("Marking topic as done…", color = ink.secondaryText)
                }
            }
        }
    }
}
