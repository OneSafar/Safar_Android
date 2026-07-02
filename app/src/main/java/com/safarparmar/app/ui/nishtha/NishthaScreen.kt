package com.safarparmar.app.ui.nishtha

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safarparmar.app.R
import com.safarparmar.app.ui.butterfly.ButterflyTourState
import com.safarparmar.app.ui.components.rememberFeatureTabBackStack
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.analytics.NishthaAnalyticsScreen
import com.safarparmar.app.ui.nishtha.checkin.CheckInScreen
import com.safarparmar.app.ui.nishtha.goals.GoalsScreen
import com.safarparmar.app.ui.nishtha.journal.JournalScreen
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.nishthaTourSteps

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
    onToggleDarkTheme: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    initialTab: Int = 0,
    analyticsInitialSection: String = "overview",
    viewModel: NishthaViewModel = hiltViewModel(),
) {
    val initialNishthaTab = NishthaTab.entries.getOrElse(initialTab) { NishthaTab.CHECK_IN }
    val tabBackStack = rememberFeatureTabBackStack(
        initialTab = initialNishthaTab,
        rootTab = NishthaTab.CHECK_IN,
    )
    val selectedTab = tabBackStack.currentTab
    var journalOpenCount by remember { mutableStateOf(0) }
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }
    var analyticsSection by remember { mutableStateOf(analyticsInitialSection) }
    val nishthaNavigate: (String) -> Unit = { route ->
        if (route.substringBefore("?") == Routes.NISHTHA_ANALYTICS) {
            analyticsSection = Uri.decode(route.substringAfter("section=", "overview"))
            tabBackStack.select(NishthaTab.ANALYTICS)
        } else {
            onNavigate(route)
        }
    }

    BackHandler(enabled = tabBackStack.hasHistory) {
        tabBackStack.goBack()
    }

    LaunchedEffect(tourState?.isVisible, tourState?.currentStepIndex) {
        val state = tourState ?: return@LaunchedEffect
        if (!state.isVisible) return@LaunchedEffect
        when (state.currentStepIndex) {
            0, 1 -> tabBackStack.select(NishthaTab.CHECK_IN)
            2 -> tabBackStack.select(NishthaTab.JOURNAL)
            3 -> tabBackStack.select(NishthaTab.GOALS)
            4 -> tabBackStack.select(NishthaTab.STREAKS)
            5 -> tabBackStack.select(NishthaTab.ANALYTICS)
        }
    }

    SafarDrawerScaffold(
        title = stringResource(R.string.module_nishtha),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        topBarActions = {
            IconButton(onClick = { tourState?.start() }) {
                Image(
                    painter = painterResource(R.drawable.ic_butterfly_tour),
                    contentDescription = "Guide",
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                        NishthaTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    if (tab == NishthaTab.JOURNAL) journalOpenCount++
                                    tabBackStack.select(tab)
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
                        NishthaTab.GOALS     -> GoalsScreen(onNavigate = nishthaNavigate)
                        NishthaTab.STREAKS   -> StreaksScreen()
                        NishthaTab.ANALYTICS -> NishthaAnalyticsScreen(onNavigate = nishthaNavigate, initialSection = analyticsSection)
                    }
                }
            }

            // Tour overlay — asks on first visit; guide icon re-triggers any time
            TourManager(
                dataStore       = viewModel.dataStore,
                steps           = nishthaTourSteps,
                section         = "nishtha",
                askOnFirstVisit = true,
                onTourStateReady = { tourState = it },
            )
        }
    }
}
