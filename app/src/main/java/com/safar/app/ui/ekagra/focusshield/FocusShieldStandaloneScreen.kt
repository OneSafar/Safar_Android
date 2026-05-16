package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.R
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes

@Composable
fun FocusShieldStandaloneScreen(
    currentRoute: String = Routes.FOCUS_SHIELD,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.shieldState.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_focus_shield),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
    ) { padding ->
        FocusShieldSettingsContent(
            state = state,
            accent = accent,
            onToggleEnabled = viewModel::setEnabled,
            onToggleStrictMode = viewModel::setStrictMode,
            onToggleEmergencyUnlock = viewModel::setAllowEmergencyUnlock,
            onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
        )
    }
}
