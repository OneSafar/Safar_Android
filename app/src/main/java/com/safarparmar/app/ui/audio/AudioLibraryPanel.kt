package com.safarparmar.app.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground

/** Flat-hairline ink for the shared audio library sheet. */
private object AudioLibraryFlat {
    val Bg: Color @Composable get() {
        val dark = !MaterialTheme.colorScheme.background.isLightBackground()
        return if (dark) Color(0xFF131316) else Color(0xFFFFF9F0)
    }
    val Text: Color @Composable get() {
        val dark = !MaterialTheme.colorScheme.background.isLightBackground()
        return if (dark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    }
    val Muted: Color @Composable get() {
        val dark = !MaterialTheme.colorScheme.background.isLightBackground()
        return if (dark) Color(0xFFCBD5E1) else Color(0xFF475569)
    }
    val Hairline: Color @Composable get() {
        val dark = !MaterialTheme.colorScheme.background.isLightBackground()
        return if (dark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)
    }
    val Accent @Composable get() = MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryPanel(
    selectedTrackId: String,
    onTrackSelect: (AudioTrack) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

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
                            .build(),
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
        containerColor = AudioLibraryFlat.Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AudioLibraryFlat.Hairline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            PlanEyebrow("Music")
            Spacer(Modifier.height(6.dp))
            Text(
                "Audio library",
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = AudioLibraryFlat.Text,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val selected = selectedCategory == category
                    val label = category?.displayName ?: "All"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (selected) {
                                    Modifier.background(AudioLibraryFlat.Accent)
                                } else {
                                    Modifier.border(1.dp, AudioLibraryFlat.Hairline, RoundedCornerShape(10.dp))
                                },
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else AudioLibraryFlat.Muted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            PlanHairline()
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 40.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            AudioLibraryFlat.Accent.copy(alpha = 0.15f)
                                        } else {
                                            AudioLibraryFlat.Hairline.copy(alpha = 0.45f)
                                        },
                                    )
                                    .clickable { playPreview(track) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isPreviewing) {
                                    EqualizerAnimation(color = AudioLibraryFlat.Accent)
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isSelected) AudioLibraryFlat.Accent else AudioLibraryFlat.Muted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = AudioLibraryFlat.Text,
                                )
                                if (track.description != null) {
                                    Text(
                                        text = track.description,
                                        fontSize = 12.sp,
                                        color = AudioLibraryFlat.Muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AudioLibraryFlat.Accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        PlanHairline(alpha = 0.55f)
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
        modifier = Modifier.height(16.dp),
    ) {
        listOf(300, 400, 500, 350).forEachIndexed { index, delayMillis ->
            val infiniteTransition = rememberInfiniteTransition(label = "eq_$index")
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = delayMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "eq_height_$index",
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}
