package com.safar.app.ui.nishtha

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.R
import com.safar.app.ui.butterfly.ButterflyTourState
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.nishtha.analytics.NishthaAnalyticsScreen
import com.safar.app.ui.nishtha.checkin.CheckInScreen
import com.safar.app.ui.nishtha.goals.GoalsScreen
import com.safar.app.ui.nishtha.journal.JournalScreen
import com.safar.app.ui.tour.TourManager
import com.safar.app.ui.tour.nishthaTourSteps

enum class NishthaTab(val labelRes: Int, val icon: ImageVector) {
    CHECK_IN (R.string.nishtha_tab_checkin,   Icons.Default.Favorite),
    JOURNAL  (R.string.nishtha_tab_journal,   Icons.Default.Book),
    GOALS    (R.string.nishtha_tab_goals,     Icons.Default.Flag),
    STREAKS  (R.string.nishtha_tab_streaks,   Icons.Default.LocalFireDepartment),
    ANALYTICS(R.string.nishtha_tab_analytics, Icons.Default.BarChart),
}

@Composable
fun NishthaScreen(
    currentRoute: String = Routes.NISHTHA,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    initialTab: Int = 0,
    viewModel: NishthaViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(NishthaTab.entries.getOrElse(initialTab) { NishthaTab.CHECK_IN }) }
    var journalOpenCount by remember { mutableStateOf(0) }
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }

    SafarDrawerScaffold(
        title = stringResource(R.string.module_nishtha),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleNightMode,
        onLanguageClick = onLanguageClick,
        topBarActions = {
            IconButton(onClick = { tourState?.start() }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Guide")
            }
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                        NishthaTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    if (tab == NishthaTab.JOURNAL) journalOpenCount++
                                    selectedTab = tab
                                },
                                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                                label = {
                                    Text(
                                        stringResource(tab.labelRes),
                                        fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            top    = padding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        )
                ) {
                    when (selectedTab) {
                        NishthaTab.CHECK_IN  -> CheckInScreen()
                        NishthaTab.JOURNAL   -> JournalScreen(openSheetOnLoad = journalOpenCount > 0)
                        NishthaTab.GOALS     -> GoalsScreen()
                        NishthaTab.STREAKS   -> StreaksScreen()
                        NishthaTab.ANALYTICS -> NishthaAnalyticsScreen(onNavigate = onNavigate)
                    }
                }
            }

            // Tour overlay — asks on first visit; guide icon re-triggers any time
            TourManager(
                dataStore       = viewModel.dataStore,
                steps           = nishthaTourSteps,
                askOnFirstVisit = true,
                onTourStateReady = { tourState = it },
            )
        }
    }
}
