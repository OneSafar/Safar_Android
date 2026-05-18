package com.safar.app.ui.dhyan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.util.YoutubeUrls

@Composable
fun DhyanLatestVideoCard(
    videoUrl: String,
    onOpenModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = stringResource(R.string.dhyan_latest_video_title),
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            DhyanYoutubeThumbnail(
                videoUrl = videoUrl,
                onClick = onOpenModal,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                showPlayOverlay = true,
            )
            Text(
                text = stringResource(R.string.dhyan_latest_video_preview),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DhyanYoutubeThumbnailLink(
    videoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DhyanYoutubeThumbnail(
            videoUrl = videoUrl,
            onClick = onClick,
            modifier = Modifier
                .size(width = 96.dp, height = 54.dp),
            showPlayOverlay = true,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.dhyan_latest_video_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.dhyan_youtube_channel_hint),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun DhyanYoutubeThumbnail(
    videoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPlayOverlay: Boolean,
) {
    val videoId = remember(videoUrl) { YoutubeUrls.extractVideoId(videoUrl) }
    val thumbnails = remember(videoId) {
        videoId?.let { YoutubeUrls.thumbnailUrls(it) } ?: emptyList()
    }
    var thumbIndex by remember(videoId) { mutableIntStateOf(0) }
    val thumbUrl = thumbnails.getOrElse(thumbIndex) { "" }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (showPlayOverlay) 0.dp else 10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbUrl.isNotBlank()) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = stringResource(R.string.dhyan_latest_video_thumbnail_cd),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (thumbIndex < thumbnails.lastIndex) {
                        thumbIndex += 1
                    }
                },
            )
        }
        if (showPlayOverlay) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(8.dp),
            )
        }
    }
}

@Composable
fun DhyanYoutubePromotionDialog(
    videoUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val videoId = remember(videoUrl) { YoutubeUrls.extractVideoId(videoUrl) }
    val watchUrl = remember(videoId, videoUrl) {
        videoId?.let { YoutubeUrls.watchUrl(it) } ?: videoUrl
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clickable { openUrl(watchUrl) },
                        contentAlignment = Alignment.Center,
                    ) {
                        DhyanYoutubeThumbnail(
                            videoUrl = videoUrl,
                            onClick = { openUrl(watchUrl) },
                            modifier = Modifier.fillMaxSize(),
                            showPlayOverlay = false,
                        )
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White)
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFE62117),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.dhyan_watch_on_youtube),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dhyan_youtube_modal_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.dhyan_youtube_modal_body),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = { openUrl(YoutubeUrls.SAFAR_CHANNEL_URL) }) {
                            Text(
                                text = stringResource(R.string.dhyan_youtube_channel_cta),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
