package com.safar.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingPage(val emoji: String, val title: String, val subtitle: String)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: SafarDataStore
) : ViewModel() {
    fun finishOnboarding() {
        viewModelScope.launch { dataStore.setOnboardingDone(true) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage("🧘", "Welcome to Safar", "Your journey of self-discovery and growth starts here."),
        OnboardingPage("💭", "Track Your Emotions", "Check in with your mood daily and understand your emotional patterns."),
        OnboardingPage("🎯", "Achieve Your Goals", "Set meaningful goals, track progress and celebrate wins."),
        OnboardingPage("🤝", "Join the Community", "Connect with others on their journey in Mehfil."),
        OnboardingPage("⏱️", "Stay Focused", "Use Ekagra mode to maintain deep focus on what matters.")
    )
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        HorizontalPager(state = pagerState, modifier = Modifier.weight(6f)) { page ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = pages[page].emoji, style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(32.dp))
                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = pages[page].subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    viewModel.finishOnboarding()
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
        }
        Spacer(Modifier.height(16.dp))
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(onClick = { viewModel.finishOnboarding(); onFinish() }) {
                Text("Skip")
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
