package com.safar.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.theme.LoraFontFamily
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

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            is SplashDestination.Home -> onNavigateToHome()
            is SplashDestination.Auth -> onNavigateToAuth()
            null -> Unit
        }
    }

    LaunchedEffect(Unit) {
        delay(1500L)
        viewModel.onVideoEnded()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.safar_static_splash),
            contentDescription = "SAFAR loading image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Start SAFAR button appears only after video ends; navigation waits for click.
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
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    
    // Text glow animation
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    // Shine / Shimmer animation
    val shineX by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shine_x"
    )

    val buttonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF3DAC78), Color(0xFF073B3A))
    )

    val shineBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.0f),
        ),
        start = Offset(shineX, 0f),
        end = Offset(shineX + 100f, 200f)
    )

    Box(
        modifier = Modifier
            .width(240.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(buttonGradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shine layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shineBrush)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.splash_start_safar),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = glow),
                    letterSpacing = 0.5.sp
                )
            )
            Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = glow))
        }
    }
}
