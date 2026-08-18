package com.safarparmar.app.feature.youtubeinsights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.ui.KavachAnalyticsViewModel
import com.safarparmar.app.feature.kavachanalytics.ui.YoutubeInsightsDetailSheet
import com.safarparmar.app.ui.ekagra.focusshield.KavachStitchBackHeader

@Composable
fun YoutubeStudyAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding() // Ensures bottom elements adapt to gesture/3-button nav
    ) {
        KavachStitchBackHeader(
            onBack = onBack,
            title = "YouTube Analytics",
        )
        YoutubeInsightsDetailSheet(
            state = state,
            isLight = isLight,
            onSetProductive = viewModel::setYoutubeChannelProductive,
        )
    }
}
