package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.alpha
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun EkagraVideoBackground(videoUrl: String, modifier: Modifier = Modifier) {
    if (videoUrl.isBlank()) { Box(modifier.background(Color.Black)); return }
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels
            val textureView = android.view.TextureView(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
            }
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    try {
                        val mp = MediaPlayer().apply {
                            setSurface(android.view.Surface(p0))
                            setDataSource(ctx, Uri.parse(videoUrl))
                            isLooping = true; setVolume(0f, 0f)
                            setOnPreparedListener { player ->
                                val vW = screenW.toFloat(); val vH = screenH.toFloat()
                                val vidW = player.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = player.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH
                                val videoAspect = vidW / vidH; val viewAspect = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect) Pair(videoAspect / viewAspect, 1f) else Pair(1f, viewAspect / videoAspect)
                                textureView.scaleX = scaleX; textureView.scaleY = scaleY
                                textureView.pivotX = vW / 2f; textureView.pivotY = vH / 2f
                                player.start()
                            }
                            setOnErrorListener { _, what, extra -> Log.e("VIDEO_DEBUG", "Error: $what $extra"); true }
                            prepareAsync()
                        }
                        textureView.setTag(R.id.ekagra_player_tag, mp)
                    } catch (e: Exception) { Log.e("VIDEO_DEBUG", "Video init failed", e) }
                }
                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) = Unit
                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    val mp = textureView.getTag(R.id.ekagra_player_tag) as? MediaPlayer
                    textureView.setTag(R.id.ekagra_player_tag, null)
                    try { mp?.stop() } catch (_: Exception) {}
                    try { mp?.release() } catch (_: Exception) {}
                    return true
                }
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit
            }
            android.widget.FrameLayout(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
                setTag(R.id.ekagra_video_url_tag, videoUrl)
                addView(textureView)
            }
        },
        update = { frame ->
            val currentUrl = frame.getTag(R.id.ekagra_video_url_tag) as? String
            if (currentUrl == videoUrl) return@AndroidView
            frame.setTag(R.id.ekagra_video_url_tag, videoUrl)
            val tv = frame.getChildAt(0) as? android.view.TextureView ?: return@AndroidView
            val st = tv.surfaceTexture ?: return@AndroidView
            val oldPlayer = tv.getTag(R.id.ekagra_player_tag) as? MediaPlayer
            tv.setTag(R.id.ekagra_player_tag, null)
            val ctx = frame.context
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels
            Thread {
                runCatching { oldPlayer?.stop() }; runCatching { oldPlayer?.release() }
                try {
                    val newPlayer = MediaPlayer().apply {
                        setSurface(android.view.Surface(st))
                        setDataSource(ctx, Uri.parse(videoUrl))
                        isLooping = true; setVolume(0f, 0f)
                        setOnPreparedListener { mp ->
                            Handler(Looper.getMainLooper()).post {
                                val vW = screenW.toFloat(); val vH = screenH.toFloat()
                                val vidW = mp.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = mp.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH
                                val videoAspect = vidW / vidH; val viewAspect = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect) Pair(videoAspect / viewAspect, 1f) else Pair(1f, viewAspect / videoAspect)
                                tv.scaleX = scaleX; tv.scaleY = scaleY
                                tv.pivotX = vW / 2f; tv.pivotY = vH / 2f
                                mp.start()
                            }
                        }
                        setOnErrorListener { _, _, _ -> true }
                        prepareAsync()
                    }
                    tv.setTag(R.id.ekagra_player_tag, newPlayer)
                } catch (_: Exception) {}
            }.start()
        },
        modifier = modifier,
    )
}

// ─── Theme / music data (unchanged) ────────────────────────────────────────────
