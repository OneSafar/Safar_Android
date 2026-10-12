package com.safarparmar.app.feature.youtube.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.data.local.entity.ChannelPolicy

import androidx.compose.ui.input.pointer.pointerInput

/**
 * Full-screen Compose overlay for the YouTube content guard.
 * Rendered inside a TYPE_ACCESSIBILITY_OVERLAY window by [YouTubeGuardOverlayManager].
 */
@Composable
fun YouTubeGuardOverlay(state: OverlayState) {
    if (state is OverlayState.Hidden) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF50D131A))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is OverlayState.Verifying -> VerifyingContent(s)
            is OverlayState.Blocked -> BlockedContent(s)
            is OverlayState.ShortsBlocked -> ShortsBlockedContent(s)
            is OverlayState.AskUser -> AskUserContent(s)
            is OverlayState.Hidden -> Unit
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-screens
// ---------------------------------------------------------------------------

@Composable
private fun VerifyingContent(state: OverlayState.Verifying) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            color = Color(0xFF42A5F5),
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
        )
        Text(
            text = "Verifying Content...",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        if (state.channelName.isNotBlank() || state.title.isNotBlank()) {
            val label = when {
                state.channelName.isNotBlank() && state.title.isNotBlank() ->
                    "${state.channelName} • ${state.title}"
                state.channelName.isNotBlank() -> state.channelName
                else -> state.title
            }
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Text(
            text = "Checking channel policy to preserve your focus.\nPlease wait a moment.",
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockedContent(state: OverlayState.Blocked) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = "Blocked",
            tint = Color(0xFFEF5350),
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = "Distracting Content",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = state.channelName.ifBlank { state.channelHandle },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEF5350),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "This channel is classified as distracting.\nStay focused — you can do it! 💪",
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = state.onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text("Go Back", color = Color.White)
        }
    }
}

@Composable
private fun ShortsBlockedContent(state: OverlayState.ShortsBlocked) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = "Shorts Blocked",
            tint = Color(0xFFFF7043),
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = "YouTube Shorts Blocked",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "Shorts are blocked during focus sessions.\nBack to learning! 📚",
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = state.onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text("Exit Shorts", color = Color.White)
        }
    }
}

@Composable
private fun AskUserContent(state: OverlayState.AskUser) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2430)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Unknown channel",
                tint = Color(0xFF42A5F5),
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "New Channel Detected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = state.channelName.ifBlank { state.channelHandle },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Is this channel productive for your studies?",
                fontSize = 14.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // "Distracting" button
                OutlinedButton(
                    onClick = { state.onClassify(state.channelHandle, ChannelPolicy.DISTRACTING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                ) {
                    Text("Distracting", fontWeight = FontWeight.SemiBold)
                }
                // "Productive" button
                Button(
                    onClick = { state.onClassify(state.channelHandle, ChannelPolicy.PRODUCTIVE) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                ) {
                    Text("Productive", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
