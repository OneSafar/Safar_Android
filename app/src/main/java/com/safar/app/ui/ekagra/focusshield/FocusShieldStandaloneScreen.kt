package com.safar.app.ui.ekagra.focusshield

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    // About Kavach uses the full-screen Stitch layout via navigation.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconTint = if (isDark) Color.White else Color(0xFF64748B)

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_focus_shield),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        emphasizeTopBar = true,
        topBarActions = {
            IconButton(onClick = { onNavigate(Routes.KAVACH_ABOUT) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.kavach_info_content_description),
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) { padding ->
        FocusShieldSettingsContent(
            state = state,
            accent = accent,
            onToggleEnabled = viewModel::setEnabled,
            onToggleStrictMode = viewModel::setStrictMode,
            onToggleEmergencyUnlock = viewModel::setAllowEmergencyUnlock,
            onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
            onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
            onRefreshPermissions = viewModel::refreshPermissions,
            onMaybeLater = { onNavigate(Routes.EKAGRA) },
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
        )
    }
}
