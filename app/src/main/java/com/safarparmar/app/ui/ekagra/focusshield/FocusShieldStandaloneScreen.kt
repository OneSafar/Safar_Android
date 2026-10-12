package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Screen
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2ViewModel
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes

@Composable
fun FocusShieldStandaloneScreen(
    currentRoute: String = Routes.FOCUS_SHIELD,
    isDarkTheme: Boolean = false,
    initialTab: Int = 0,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: FocusShieldViewModel = hiltViewModel(),
    youtubeViewModel: YoutubeStudyV2ViewModel = hiltViewModel(),
) {
    // Preserve direct YouTube setup links from Ekagra and older tab routes.
    if (initialTab == 1) {
        YoutubeStudyV2Screen(onBack = onBack, isDarkTheme = isDarkTheme, viewModel = youtubeViewModel)
        return
    }
    val shieldState by viewModel.shieldState.collectAsStateWithLifecycle()
    val youtubeState by youtubeViewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                youtubeViewModel.refreshPermission()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_focus_shield),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        emphasizeTopBar = true,
        topBarActions = {
            com.composables.ui.components.IconButton(style = com.composables.ui.components.ButtonStyle.Ghost,
                onClick = { onNavigate(Routes.KAVACH_ABOUT) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.kavach_info_content_description),
                    tint = scheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
    ) { padding ->
        FocusShieldSettingsContent(
            state = shieldState,
            accent = KavachDesign.Primary,
            youtubeEnabled = youtubeState.enabled,
            onToggleYoutubeStudyMode = { enabled ->
                if (enabled) {
                    // First-time setup enables protection after disclosure and setup.
                    if (youtubeState.setupCompleted) youtubeViewModel.setEnabled(true)
                    onNavigate(Routes.YOUTUBE_STUDY_MODE_V2)
                } else {
                    youtubeViewModel.setEnabled(false)
                }
            },
            onOpenYoutubeSetup = { onNavigate(Routes.YOUTUBE_STUDY_MODE_V2) },
            onToggleEnabled = viewModel::setEnabled,
            onToggleProfile = viewModel::setKavachProfile,
            onToggleStrictMode = viewModel::setStrictMode,
            onToggleSchedule = viewModel::setScheduleEnabled,
            onSetScheduleRange = viewModel::setScheduleRange,
            onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
            onOpenAppCategories = { onNavigate(Routes.KAVACH_APP_CATEGORIES) },
            onOpenAnalytics = { onNavigate(Routes.nishthaAnalytics("kavach")) },
            onGoToEkagra = { onNavigate(Routes.EKAGRA) },
            onOpenOverlaySettings = viewModel::openOverlaySettings,
            onRefreshPermissions = viewModel::refreshPermissions,
            onSetPendingEnableAfterAppSelection = viewModel::setPendingEnableAfterAppSelection,
            onMaybeLater = onBack,
            onSave = onBack,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        )
    }
}
