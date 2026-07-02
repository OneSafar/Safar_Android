package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes

@Composable
fun FocusShieldStandaloneScreen(
    currentRoute: String = Routes.FOCUS_SHIELD,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.shieldState.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val scheme = MaterialTheme.colorScheme
    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()
    val needsRequiredPermissions =
        !state.hasUsageStats || (accessibilityRequired && !state.hasAccessibilityService)

    if (needsRequiredPermissions) {
        KavachOnboardingScreen(
            onFinished = {},
            onBack = onBack,
            viewModel = viewModel,
        )
        return
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
            IconButton(
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
            state = state,
            accent = accent,
            onToggleEnabled = viewModel::setEnabled,
            onToggleAlwaysOn = viewModel::setAlwaysOn,
            onToggleStrictMode = viewModel::setStrictMode,
            onToggleEmergencyUnlock = viewModel::setAllowEmergencyUnlock,
            onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
            onRefreshPermissions = viewModel::refreshPermissions,
            onMaybeLater = onBack,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
        )
    }
}
