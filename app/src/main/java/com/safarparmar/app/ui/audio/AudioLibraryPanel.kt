package com.safarparmar.app.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryPanel(
    selectedTrackId: String,
    onTrackSelect: (AudioTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

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

    val filteredTracks = remember(selectedCategory) {
        if (selectedCategory == null) AudioLibrary.TRACKS
        else AudioLibrary.TRACKS.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Categories
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else AudioCategory.values().indexOf(selectedCategory) + 1,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    text = { Text("All") }
                )
                AudioCategory.values().forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = { Text(category.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Track List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    val isSelected = track.id == selectedTrackId
                    val isPreviewing = track.id == previewingTrackId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) scheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable {
                                onTrackSelect(track)
                                releasePreview()
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play/Preview button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) scheme.primary else scheme.surfaceVariant)
                                .clickable { playPreview(track) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPreviewing) {
                                EqualizerAnimation(color = if (isSelected) scheme.onPrimary else scheme.primary)
                            } else {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Track info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) scheme.primary else scheme.onSurface
                            )
                            if (track.description != null) {
                                Text(
                                    text = track.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Selected indicator
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = scheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
