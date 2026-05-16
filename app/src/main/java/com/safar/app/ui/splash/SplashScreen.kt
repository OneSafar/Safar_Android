package com.safar.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.R
import com.safar.app.ui.theme.AuthLoginButtonTokens
import com.safar.app.ui.theme.Orange500
import com.safar.app.ui.theme.Slate300
import com.safar.app.ui.theme.Slate700
import com.safar.app.ui.theme.LoraFontFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.drawWithContent
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
    isDarkTheme: Boolean,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            is SplashDestination.Home -> onNavigateToHome()
            is SplashDestination.Auth -> onNavigateToAuth()
            null -> Unit
        }
    }

    var isLogoAnimating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoAnimating = true
        // Industry-grade choreography: start entry animations while logo is finishing
        delay(1300L) 
        viewModel.onVideoEnded()
    }
    
    // Atmospheric Radial Gradient
    val backgroundBrush = if (isDarkTheme) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF131A26), Color.Black), // Deep Slate/Navy to Black
            center = Offset(0.5f, 0.4f),
            radius = 2000f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFCF9F2), Color(0xFFF8F6F2)), // Faint Cream to BgLight
            center = Offset(0.5f, 0.4f),
            radius = 2000f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.4f))
        SafarLogoAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .weight(4.8f)
        )

        val letterSpacingAnim by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isLogoAnimating) 0.8f else 4f,
            animationSpec = tween(1575, easing = FastOutSlowInEasing),
            label = "letterSpacingAnim"
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = isLogoAnimating,
            enter = fadeIn(tween(1575)) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(1575, easing = FastOutSlowInEasing)
            ) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(1575, easing = FastOutSlowInEasing)
            ),
        ) {
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = letterSpacingAnim.sp,
                    color = if (isDarkTheme) Color.White else Color(0xFF4A5568),
                    fontSize = 15.4.sp,
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.videoEnded,
                enter = fadeIn(tween(1000, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing)
                ),
            ) {
                StartSafarButton(onClick = { viewModel.onStartSafar() })
            }
        }
        Spacer(Modifier.weight(0.55f))
    }
}

@Composable
private fun StartSafarButton(onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.LinearEasing, delayMillis = 500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.35f),
            Color.Transparent
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 400f, 400f)
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(56.dp)
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .drawWithContent {
                drawContent()
                drawRect(brush = shimmerBrush)
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
        ),
    ) {
        Text(
            text = stringResource(R.string.splash_start_safar).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
            ),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(20.dp),
        )
    }
}
