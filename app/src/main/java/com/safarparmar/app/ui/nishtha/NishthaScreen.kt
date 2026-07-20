package com.safarparmar.app.ui.nishtha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.home.VideoPlaylistEntryPoint

enum class NishthaTab(val labelRes: Int, val icon: ImageVector) {
    CHECK_IN (R.string.nishtha_tab_checkin,   Icons.Default.Favorite),
    JOURNAL  (R.string.nishtha_tab_journal,   Icons.Default.Book),
    GOALS    (R.string.nishtha_tab_goals,     Icons.Default.Flag),
    STREAKS  (R.string.nishtha_tab_streaks,   Icons.Default.LocalFireDepartment),
    ANALYTICS(R.string.nishtha_tab_analytics, Icons.Default.BarChart),
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
    // Shared bottom-nav back model: Back from any tab returns to Check-in (start)
    // in a single press, then the NavController takes over (→ Home). Matches the
    // behaviour used by Ekagra / Mehfil / Courses.
    val tabBackStack = rememberFeatureTabBackStack(
        initialTab = initialNishthaTab,
        rootTab = NishthaTab.CHECK_IN,
    )
    val selectedTab = tabBackStack.currentTab
    var journalOpenCount by remember { mutableStateOf(0) }
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }
    var analyticsSection by remember { mutableStateOf(analyticsInitialSection) }

    val nishthaNavigate: (String) -> Unit = { route ->
        val routeBase = route.substringBefore("?")
        val tabArg = if (route.contains("tab=")) route.substringAfter("tab=").substringBefore("&").toIntOrNull() else null
        when {
            routeBase == Routes.NISHTHA && tabArg == 4 -> {
                // Intercept analytics navigation — switch the tab in-place instead of
                // pushing a new nav entry (prevents back-stack pollution)
                analyticsSection = android.net.Uri.decode(route.substringAfter("section=", "overview"))
                tabBackStack.select(NishthaTab.ANALYTICS)
            }
            routeBase == Routes.NISHTHA && tabArg != null -> {
                // Handle other tab index navigations (from deep links / notifications)
                val target = NishthaTab.entries.getOrElse(tabArg) { NishthaTab.CHECK_IN }
                tabBackStack.select(target)
            }
            else -> onNavigate(route)
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
        containerColor = SafarSemanticColors.plannerBackground(),
        topBarActions = {
            VideoPlaylistEntryPoint(
                dataStore = viewModel.dataStore,
                tint = MaterialTheme.colorScheme.onSurface,
            )
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
                // Keep every Nishtha tab on the same warm canvas as Exam
                // Planner Home. Cards and sheets remain elevated surfaces.
                containerColor = SafarSemanticColors.plannerBackground(),
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
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                            label = "NishthaTabTransition",
                            modifier = Modifier.fillMaxSize(),
                        ) { targetTab ->
                            when (targetTab) {
                                NishthaTab.CHECK_IN  -> CheckInScreen()
                                NishthaTab.JOURNAL   -> JournalScreen(openSheetOnLoad = journalOpenCount > 0)
                                NishthaTab.GOALS     -> GoalsScreen(onNavigate = nishthaNavigate)
                                NishthaTab.STREAKS   -> StreaksScreen()
                                NishthaTab.ANALYTICS -> NishthaAnalyticsScreen(onNavigate = nishthaNavigate, initialSection = analyticsSection)
                            }
                        }
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
