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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.safarparmar.app.ui.ekagra.EkagraDisplayTitle
import com.safarparmar.app.ui.ekagra.EkagraEyebrow
import com.safarparmar.app.ui.ekagra.EkagraHairline
import com.safarparmar.app.ui.ekagra.EkagraInk
import com.safarparmar.app.ui.ekagra.EkagraPill
import com.safarparmar.app.ui.ekagra.rememberEkagraInk
import com.safarparmar.app.ui.theme.isLightBackground

/** Visual theme for the shared audio library bottom sheet. */
enum class AudioLibraryPanelTheme {
    Default,
    Dhyan,
}

private data class AudioLibraryColors(
    val bg: Color,
    val text: Color,
    val muted: Color,
    val hairline: Color,
    val accent: Color,
)

@Composable
private fun rememberAudioLibraryColors(
    theme: AudioLibraryPanelTheme,
    isDarkTheme: Boolean,
): AudioLibraryColors {
    return when (theme) {
        AudioLibraryPanelTheme.Dhyan -> AudioLibraryColors(
            bg = if (isDarkTheme) Color(0xFF2A1520) else Color(0xFFFFF0F5),
            text = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF1E1B4B),
            muted = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
            hairline = if (isDarkTheme) Color(0xFF5C3040) else Color(0xFFF5C4D8),
            accent = if (isDarkTheme) Color(0xFFE86B96) else Color(0xFFF04880),
        )
        AudioLibraryPanelTheme.Default -> {
            val dark = !MaterialTheme.colorScheme.background.isLightBackground()
            AudioLibraryColors(
                bg = if (dark) Color(0xFF131316) else Color(0xFFFFF9F0),
                text = if (dark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B),
                muted = if (dark) Color(0xFFCBD5E1) else Color(0xFF475569),
                hairline = if (dark) Color(0xFF3F3F46) else Color(0xFFE2DDF0),
                accent = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryPanel(
    selectedTrackId: String,
    onTrackSelect: (AudioTrack) -> Unit,
    onDismiss: () -> Unit,
    theme: AudioLibraryPanelTheme = AudioLibraryPanelTheme.Default,
    isDarkTheme: Boolean = false,
) {
    val context = LocalContext.current
    val colors = rememberAudioLibraryColors(theme = theme, isDarkTheme = isDarkTheme)

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
                        setDataSource(context, MediaFileCache.uriFor(context, track.url))
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

    val ink = remember(colors) {
        EkagraInk(
            primaryText = colors.text,
            secondaryText = colors.muted,
            mutedText = colors.muted,
            hairline = colors.hairline,
            trackFaint = colors.hairline.copy(alpha = 0.3f),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.hairline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            EkagraEyebrow("Music", ink.secondaryText)
            Spacer(Modifier.height(4.dp))
            EkagraDisplayTitle("Audio library", ink.primaryText)
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val selected = selectedCategory == category
                    val label = category?.displayName ?: "All"
                    EkagraPill(
                        label = label,
                        selected = selected,
                        accent = colors.accent,
                        ink = ink,
                        onClick = { selectedCategory = category },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            EkagraHairline(ink.hairline)

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
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            colors.accent.copy(alpha = 0.15f)
                                        } else {
                                            colors.hairline.copy(alpha = 0.45f)
                                        },
                                    )
                                    .clickable { playPreview(track) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isPreviewing) {
                                    EqualizerAnimation(color = colors.accent)
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isSelected) colors.accent else colors.muted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = ink.primaryText,
                                )
                                if (track.description != null) {
                                    Text(
                                        text = track.description,
                                        fontSize = 11.5.sp,
                                        color = ink.mutedText,
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
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        EkagraHairline(ink.hairline.copy(alpha = 0.5f))
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
