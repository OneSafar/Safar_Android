package com.safarparmar.app.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.safarparmar.app.ui.ekagra.EkagraDisplayTitle
import com.safarparmar.app.ui.ekagra.EkagraEyebrow
import com.safarparmar.app.ui.ekagra.EkagraHairline
import com.safarparmar.app.ui.ekagra.EkagraTextTabs
import com.safarparmar.app.ui.ekagra.rememberEkagraInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryPanel(
    selectedTrackId: String,
    onTrackSelect: (AudioTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)

    var selectedCategory by remember { mutableStateOf<AudioCategory?>(null) }
    var previewingTrackId by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }

    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePreview() {
        val player = mediaPlayer.value
        mediaPlayer.value = null
        previewingTrackId = null
        player?.let {
            kotlin.concurrent.thread {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
        }
    }

    fun playPreview(track: AudioTrack) {
        if (track.id == "none-track") {
            return
        }
        if (previewingTrackId == track.id) {
            // Stop preview if tapping the same track
            releasePreview()
            return
        }
        releasePreview()
        previewingTrackId = track.id

        if (!isMuted) {
            try {
                mediaPlayer.value = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    if (track.isLocal && track.localResId != null) {
                        setDataSource(context, Uri.parse("android.resource://${context.packageName}/${track.localResId}"))
                    } else {
                        setDataSource(track.url)
                    }
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnCompletionListener { releasePreview() }
                    setOnErrorListener { _, _, _ ->
                        releasePreview()
                        true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                releasePreview()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { releasePreview() }
    }

    LaunchedEffect(isMuted) {
        if (isMuted && previewingTrackId != null) {
            releasePreview()
        }
    }

    val categories = remember { listOf<AudioCategory?>(null) + AudioCategory.entries }

    val filteredTracks = remember(selectedCategory) {
        if (selectedCategory == null) AudioLibrary.TRACKS
        else AudioLibrary.TRACKS.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = ink.hairline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp)
        ) {
            // Header — Ekagra typography
            Spacer(Modifier.height(4.dp))
            EkagraEyebrow("Music", ink.secondaryText)
            Spacer(Modifier.height(4.dp))
            EkagraDisplayTitle("Audio library", ink.primaryText)
            Spacer(Modifier.height(18.dp))

            // Categories using EkagraTextTabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                EkagraTextTabs(
                    items = categories,
                    selected = selectedCategory,
                    accent = scheme.primary,
                    ink = ink,
                    label = { it?.displayName ?: "All" },
                    onSelect = { selectedCategory = it }
                )
            }

            Spacer(Modifier.height(16.dp))
            EkagraHairline(ink.hairline)
            Spacer(Modifier.height(8.dp))

            // Track List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    val isSelected = track.id == selectedTrackId
                    val isPreviewing = track.id == previewingTrackId

                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTrackSelect(track)
                                    releasePreview()
                                    onDismiss()
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Play/Preview button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) scheme.primary.copy(alpha = 0.15f) else ink.trackFaint)
                                    .clickable { playPreview(track) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPreviewing) {
                                    EqualizerAnimation(color = scheme.primary)
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isSelected) scheme.primary else ink.secondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Track info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = ink.primaryText
                                )
                                if (track.description != null) {
                                    Text(
                                        text = track.description,
                                        fontSize = 12.sp,
                                        color = ink.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Selected indicator — accent dot + checkmark
                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(scheme.primary)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = scheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        EkagraHairline(ink.hairline.copy(alpha = ink.hairline.alpha * 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerAnimation(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        listOf(300, 400, 500, 350).forEachIndexed { index, delayMillis ->
            val infiniteTransition = rememberInfiniteTransition(label = "eq_$index")
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = delayMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eq_height_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
