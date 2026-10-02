package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.SafarSemanticColors

@Composable
fun AnalyticsScreen(
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    initialSection: String = "overview",
) {
    SafarDrawerScaffold(
        title = stringResource(R.string.nishtha_tab_analytics),
        subtitle = stringResource(R.string.app_name),
        currentRoute = Routes.ANALYTICS,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = SafarSemanticColors.plannerBackground(),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NishthaAnalyticsScreen(
                onNavigate = onNavigate,
                initialSection = initialSection,
            )
        }
    }
}
