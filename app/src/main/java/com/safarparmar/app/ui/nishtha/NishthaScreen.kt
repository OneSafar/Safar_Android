package com.safarparmar.app.ui.nishtha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.safarparmar.app.ui.theme.isLightBackground


enum class NishthaTab(val labelRes: Int, val icon: ImageVector) {
    CHECK_IN(R.string.nishtha_tab_checkin, Icons.Default.Favorite),
    JOURNAL(R.string.nishtha_tab_journal, Icons.Default.Book),
    GOALS(R.string.nishtha_tab_goals, Icons.Default.Flag),
    STREAKS(R.string.nishtha_tab_streaks, Icons.Default.LocalFireDepartment),
    ANALYTICS(R.string.nishtha_tab_analytics, Icons.Default.BarChart),
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NishthaScreen(
    currentRoute: String = Routes.NISHTHA,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    initialTab: Int = 0,
    analyticsInitialSection: String = "overview",
    viewModel: NishthaViewModel = hiltViewModel(),
) {
    // Legacy tab-4 entries also display the independent Analytics shell.
    if (initialTab == NishthaTab.ANALYTICS.ordinal) {
        com.safarparmar.app.ui.nishtha.analytics.AnalyticsScreen(
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            initialSection = analyticsInitialSection,
        )
        return
    }
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

    LaunchedEffect(initialTab) {
        val target = NishthaTab.entries.getOrElse(initialTab) { NishthaTab.CHECK_IN }
        tabBackStack.select(target)
    }

    LaunchedEffect(analyticsInitialSection) {
        if (analyticsInitialSection.isNotBlank()) {
            analyticsSection = analyticsInitialSection
        }
    }

    val nishthaNavigate: (String) -> Unit = { route ->
        val routeBase = route.substringBefore("?")
        val tabArg = if (route.contains("tab=")) route.substringAfter("tab=").substringBefore("&").toIntOrNull() else null
        when {
            routeBase == Routes.NISHTHA && tabArg == 4 -> {
                onNavigate(Routes.normalizeFeatureRoute(route))
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
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                // Keep every Nishtha tab on the same warm canvas as Exam
                // Planner Home. Cards and sheets remain elevated surfaces.
                containerColor = SafarSemanticColors.plannerBackground(),
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NishthaBottomBar(
                        selected = selectedTab,
                        onSelect = { tab ->
                            if (tab == NishthaTab.JOURNAL) journalOpenCount++
                            tabBackStack.select(tab)
                        },
                    )
                },
            ) { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                ) {
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                            label = "NishthaTabTransition",
                            modifier = Modifier.fillMaxSize(),
                        ) { targetTab ->
                            when (targetTab) {
                                NishthaTab.CHECK_IN -> CheckInScreen()
                                NishthaTab.JOURNAL -> JournalScreen(openSheetOnLoad = journalOpenCount > 0)
                                NishthaTab.GOALS -> GoalsScreen(onNavigate = nishthaNavigate)
                                NishthaTab.STREAKS -> StreaksScreen()
                                NishthaTab.ANALYTICS -> NishthaAnalyticsScreen(
                                    onNavigate = nishthaNavigate,
                                    initialSection = analyticsSection,
                                )
                            }
                        }
                    }
                }
            }

            // Tour overlay disabled
            TourManager(
                dataStore = viewModel.dataStore,
                steps = nishthaTourSteps(),
                section = "nishtha",
                askOnFirstVisit = false,
                onTourStateReady = { tourState = it },
            )
        }
    }
}

/**
 * Modern Motion Bottom Navigation Bar with hardware-accelerated graphicsLayer slide,
 * per-tab accent transitions, and bouncy spring micro-interactions.
 */
@Composable
private fun NishthaBottomBar(
    selected: NishthaTab,
    onSelect: (NishthaTab) -> Unit,
) {
    val tabs = remember { NishthaTab.entries.filter { it != NishthaTab.ANALYTICS } }
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()
    val isDark = !isLight
    val haptic = LocalHapticFeedback.current

    val isAnalyticsSelected = selected == NishthaTab.ANALYTICS
    val selectedIndex = tabs.indexOf(selected).takeIf { it >= 0 } ?: 0

    // High-performance spring slide animation for the sliding indicator pill
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nishthaMotionTabSlide",
    )

    val currentAccent = nishthaTabAccent(tabs.getOrElse(selectedIndex) { NishthaTab.CHECK_IN }, isDark)
    val animatedAccent by animateColorAsState(
        targetValue = currentAccent,
        animationSpec = tween(250),
        label = "nishthaMotionAccent",
    )

    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                shape = barShape,
            ),
        color = if (isDark) Color(0xFF161618) else Color.White,
        shape = barShape,
        shadowElevation = 0.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / tabs.size
            val density = LocalDensity.current

            // ── Sliding Pill Indicator (Evaluated in draw phase via graphicsLayer) ──
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (isAnalyticsSelected) 0f else 1f
                        translationX = with(density) { (itemWidth * animatedIndex).toPx() }
                    }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(animatedAccent.copy(alpha = if (isDark) 0.18f else 0.12f))
                    .border(
                        width = 1.dp,
                        color = animatedAccent.copy(alpha = if (isDark) 0.35f else 0.25f),
                        shape = RoundedCornerShape(14.dp),
                    ),
            )

            // ── Interactive Tab Items with Motion Scale & Bounce ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selected == tab
                    val tabAccent = nishthaTabAccent(tab, isDark)

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "nishthaTabIconScale_${tab.name}",
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            tabAccent
                        } else {
                            if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                        },
                        animationSpec = tween(200),
                        label = "nishthaTabContentColor_${tab.name}",
                    )
                    val label = stringResource(tab.labelRes)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(tab)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Per-tab accents — same role as PlannerTabAccent on Exam Planner. */
private fun nishthaTabAccent(tab: NishthaTab, isDark: Boolean): Color = when (tab) {
    NishthaTab.CHECK_IN -> if (isDark) Color(0xFFFF6482) else Color(0xFFE11D48)
    NishthaTab.JOURNAL -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    NishthaTab.GOALS -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    NishthaTab.STREAKS -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    NishthaTab.ANALYTICS -> if (isDark) Color(0xFFC084FC) else Color(0xFF581C87)
}
