package com.safar.app.ui.splash

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Auth : SplashDestination()
    object Home : SplashDestination()
}

data class SplashUiState(val destination: SplashDestination? = null, val videoEnded: Boolean = false)

@HiltViewModel
class SplashViewModel @Inject constructor(private val dataStore: SafarDataStore) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    fun onVideoEnded() { _uiState.update { it.copy(videoEnded = true) } }

    fun onStartSafar() {
        viewModelScope.launch {
            val isLoggedIn = dataStore.isLoggedIn.first()
            _uiState.value = SplashUiState(
                destination = if (isLoggedIn) SplashDestination.Home else SplashDestination.Auth,
                videoEnded = true
            )
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var videoReady by remember { mutableStateOf(false) }
    var useFallback by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            is SplashDestination.Home -> onNavigateToHome()
            is SplashDestination.Auth -> onNavigateToAuth()
            null -> Unit
        }
    }

    LaunchedEffect(Unit) {
        delay(2000L)
        if (!videoReady) useFallback = true
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("file:///android_asset/splash.mp4")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) videoReady = true
                    if (state == Player.STATE_ENDED) viewModel.onVideoEnded()
                }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!useFallback) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(600), label = "fade")
            val scale = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Box(
                modifier = Modifier.fillMaxSize().alpha(alpha).background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
                    Text(stringResource(R.string.splash_title), style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
                    Spacer(Modifier.height(32.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                }
            }
        }

        // Start Safar button — appears only after video ends, no auto-navigate
        AnimatedVisibility(
            visible = uiState.videoEnded,
            enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)
        ) {
            StartSafarButton(onClick = { viewModel.onStartSafar() })
        }
    }
}

@Composable
private fun StartSafarButton(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    // Pill button only — no circle wrapper
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(primary, primary.copy(alpha = 0.80f))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Start your Safar",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = glow),
                letterSpacing = 0.3.sp
            )
            Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = glow))
        }
    }
}
