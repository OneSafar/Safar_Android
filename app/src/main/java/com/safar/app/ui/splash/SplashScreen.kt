package com.safar.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStore: SafarDataStore
) : ViewModel() {

    sealed class SplashEvent {
        object GoToOnboarding : SplashEvent()
        object GoToHome : SplashEvent()
    }

    private var _event: SplashEvent? = null
    val event get() = _event

    init {
        viewModelScope.launch {
            delay(1500)
            val isOnboardingDone = dataStore.isOnboardingDone.first()
            val isLoggedIn = dataStore.isLoggedIn.first()
            _event = when {
                !isOnboardingDone -> SplashEvent.GoToOnboarding
                isLoggedIn -> SplashEvent.GoToHome
                else -> SplashEvent.GoToOnboarding
            }
        }
    }
}

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    LaunchedEffect(viewModel) {
        while (viewModel.event == null) delay(100)
        when (viewModel.event) {
            is SplashViewModel.SplashEvent.GoToOnboarding -> onNavigateToOnboarding()
            is SplashViewModel.SplashEvent.GoToHome -> onNavigateToHome()
            null -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Text(
                text = "सफर",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Safar",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )
        }
    }
}
