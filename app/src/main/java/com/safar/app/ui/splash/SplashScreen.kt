package com.safar.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.style.TextAlign
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
    var isTaglineVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoAnimating = true
        delay(150L)
        isTaglineVisible = true
        delay(1650L) 
        viewModel.onStartSafar()
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
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SafarLogoAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = isTaglineVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + 
                    slideInVertically(
                        initialOffsetY = { 20 },
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ),
        ) {
            Text(
                text = "Your Marks Matter, But So Does Your Mind",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF2D3748),
                    fontSize = 17.sp,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}
