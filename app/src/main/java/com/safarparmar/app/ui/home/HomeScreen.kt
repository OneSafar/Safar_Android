package com.safarparmar.app.ui.home

import com.safarparmar.app.ui.theme.BgDark
import android.content.Intent
import android.net.Uri
import com.composables.ui.components.TooltipPanel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.components.popups.SafarDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.IconButton as ComposablesIconButton
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onBackgroundColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.notifications.NotificationPermissionRequest
import com.safarparmar.app.performance.adaptiveBlur
import com.safarparmar.app.ui.adaptive.rememberSafarAdaptiveState
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.SafarBackdropBlurRadiusPx
import com.safarparmar.app.ui.studyplanner.components.SafarGlassDialogHost
import com.safarparmar.app.ui.studyplanner.components.rememberPlannerBackdropBlur
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.util.YoutubeUrls
import com.safarparmar.app.util.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class HomeSlide(
    val titleRes: Int,
    val headlineRes: Int,
    val bodyRes: Int,
    val bgImageUrl: String,
    val route: String,
    val accentColor: Color,
    val uiColor: Color,
)

private data class ToolCard(
    val labelRes: Int,
    val imageRes: Int,
    val route: String,
)

private val slides = listOf(
    HomeSlide(
        R.string.module_ekagra,
        R.string.home_slide_ekagra_headline,
        R.string.home_slide_ekagra_body,
        "img_ekagara.webp",
        Routes.EKAGRA,
        Color(0xFFAAC7FF),
        Color(0xFF0A305F)
    ),
    HomeSlide(
        R.string.module_nishtha,
        R.string.home_slide_nishtha_headline,
        R.string.home_slide_nishtha_body,
        "img_nishtha.webp",
        Routes.nishthaRoot(),
        Color(0xFFA9D0B3),
        Color(0xFF143723)
    ),
    HomeSlide(
        R.string.module_mehfil,
        R.string.home_slide_mehfil_headline,
        R.string.home_slide_mehfil_body,
        "img_mehefil.webp",
        Routes.MEHFIL,
        Color(0xFFFFB5A0),
        Color(0xFF561F0F)
    ),
    HomeSlide(
        R.string.module_dhyan,
        R.string.home_slide_dhyan_headline,
        R.string.home_slide_dhyan_body,
        "img_dhyan.webp",
        Routes.DHYAN,
        Color(0xFFDDBCE0),
        Color(0xFF3F2844)
    ),
    HomeSlide(
        R.string.module_study_planner,
        R.string.home_slide_planner_headline,
        R.string.home_slide_planner_body,
        "study_planner_light.webp",
        Routes.STUDY_PLANNER,
        Color(0xFFC8D3A5),
        Color(0xFF2D3615)
    ),
)


private val toolCards = listOf(
    ToolCard(R.string.module_ekagra, R.drawable.tool_ekagra, Routes.EKAGRA),
    ToolCard(R.string.module_nishtha, R.drawable.home_nishtha_book, Routes.nishthaRoot()),
    ToolCard(R.string.module_mehfil, R.drawable.tool_mehfil, Routes.MEHFIL),
    ToolCard(R.string.module_study_planner, R.drawable.tool_study_planner, Routes.STUDY_PLANNER),
    ToolCard(R.string.module_dhyan, R.drawable.tool_dhyan, Routes.DHYAN),
    ToolCard(R.string.nav_study_circle, R.drawable.tool_circle, Routes.STUDY_CIRCLES),
    ToolCard(R.string.nav_focus_shield, R.drawable.tool_kavach, Routes.FOCUS_SHIELD),
    ToolCard(R.string.nav_leaderboard, R.drawable.tool_leaderboard, Routes.LEADERBOARD),
)

@Composable
fun HomeScreen(
    currentRoute: String = Routes.HOME,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    dataStore: SafarDataStore? = null,
    notificationBellViewModel: NotificationBellViewModel = hiltViewModel(),
    dashboardViewModel: com.safarparmar.app.ui.dashboard.DashboardViewModel = hiltViewModel(),
) {
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by (dataStore?.isLoggedIn ?: kotlinx.coroutines.flow.MutableStateFlow(true))
        .collectAsStateWithLifecycle(initialValue = true)
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) onNavigateToAuth()
    }

    val scope = rememberCoroutineScope()
    val homeWelcomeSeenFlow = remember(dataStore) {
        dataStore?.homeWelcomeSeen?.map<Boolean, Boolean?> { it } ?: flowOf(true)
    }
    val homeWelcomeSeen by homeWelcomeSeenFlow.collectAsStateWithLifecycle(initialValue = null)
    val userName by remember(dataStore) {
        dataStore?.userName ?: MutableStateFlow(null)
    }.collectAsStateWithLifecycle(initialValue = null)

    // Keep first-run prompts sequential: SAFAR welcome first, then Android notifications.
    if (homeWelcomeSeen == true) {
        NotificationPermissionRequest()
    }

    var currentPage by remember { mutableIntStateOf((0 until slides.size).random()) }

    LaunchedEffect(currentPage) {
        delay(4000L)
        var next = currentPage
        while (next == currentPage) {
            next = (0 until slides.size).random()
        }
        currentPage = next
    }

    var showAnnouncementsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val notificationBellState by notificationBellViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(showAnnouncementsSheet) {
        if (showAnnouncementsSheet) notificationBellViewModel.load()
    }

    if (showAnnouncementsSheet) {
        AnnouncementsBottomSheet(
            items = notificationBellState.items,
            isLoading = notificationBellState.isLoading,
            onDismissRequest = { showAnnouncementsSheet = false },
            onMarkAllAsRead = notificationBellViewModel::markAllRead,
            onMarkAsRead = notificationBellViewModel::markAsRead,
            onDismissAnnouncement = notificationBellViewModel::dismiss,
            onAnnouncementAction = { item ->
                notificationBellViewModel.markAsRead(item.id)
                showAnnouncementsSheet = false
                val deepLink = item.deepLink
                if (!deepLink.isNullOrBlank()) {
                    val uri = Uri.parse(deepLink)
                    if (com.safarparmar.app.notifications.NotificationDeepLinkHandler.isExternalWebLink(uri)) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    } else {
                        onNavigate(com.safarparmar.app.notifications.NotificationDeepLinkHandler.routeFor(deepLink))
                    }
                } else {
                    val marketIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${context.packageName}"),
                    )
                    runCatching { context.startActivity(marketIntent) }
                        .onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
                                    )
                                )
                            }
                        }
                }
            },
        )
    }

    var openDrawerAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_home),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        showTopBar = false,
        onDrawerControllerReady = { openDrawerAction = it },
    ) { padding ->
        val currentSlide = slides[currentPage]
        val baseBgColor = if (isDarkTheme) BgDark else Color(0xFFFFF5F0)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(baseBgColor)
        ) {
            val adaptiveState = rememberSafarAdaptiveState()
            val isWide = adaptiveState.isWideScreen

            if (isWide) {
                // ══════════════════════════════════════════════════════
                // ── TABLET / WIDE: Reference Dashboard Layout ────────
                // ══════════════════════════════════════════════════════
                HomeScreenTabletDashboard(
                    dashboardUiState = dashboardUiState,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigate,
                    onToggleGoal = dashboardViewModel::toggleGoal,
                    onRefresh = { dashboardViewModel.onEvent(com.safarparmar.app.ui.dashboard.DashboardEvent.Refresh) },
                    userName = userName,
                    userAvatar = dashboardUiState.userAvatar,
                    unreadNotifications = notificationBellState.unreadCount,
                    onOpenAnnouncements = { showAnnouncementsSheet = true },
                    dataStore = dataStore,
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                )
            } else {
                PlumPhoneHome(
                    isDarkTheme = isDarkTheme,
                    slideTitle = stringResource(currentSlide.titleRes),
                    slideHeadline = stringResource(currentSlide.headlineRes),
                    slideCount = slides.size, currentPage = currentPage,
                    onPage = { currentPage = it }, onHero = { onNavigate(currentSlide.route) },
                    onNavigate = onNavigate, onMenu = { openDrawerAction?.invoke() },
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                    actions = {
                        com.safarparmar.app.ui.tutorial.TitliGuideButton(artworkSize = 42.dp)
                        VideoPlaylistEntryPoint(dataStore = dataStore, tint = if (isDarkTheme) Color(0xFFFFF0F6) else Color(0xFF39132E),
                            isDarkTheme = isDarkTheme, showTooltip = true, modifier = Modifier.size(48.dp), iconSize = 30.dp)
                        ComposablesIconButton(onClick = { showAnnouncementsSheet = true }, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Notifications, stringResource(R.string.home_notifications_updates), modifier = Modifier.size(32.dp), tint = if (isDarkTheme) Color(0xFFFFF0F6) else Color(0xFF39132E))
                                if (notificationBellState.unreadCount > 0) Badge(
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = 1.dp),
                                    containerColor = Color(0xFFB3261E), contentColor = Color.White) {
                                    androidx.compose.material3.Text(if (notificationBellState.unreadCount > 9) "9+" else notificationBellState.unreadCount.toString(), fontSize = 9.sp, lineHeight = 11.sp)
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    if (homeWelcomeSeen == false) {
        SafarWelcomeDialog(
            userName = userName.orEmpty(),
            isDarkTheme = isDarkTheme,
            onDismiss = {
                if (dataStore != null) {
                    scope.launch { dataStore.setHomeWelcomeSeen(true) }
                }
            },
        )
    }
}

@Composable
private fun SafarWelcomeDialog(userName: String, isDarkTheme: Boolean, onDismiss: () -> Unit) {
    val paper = if (isDarkTheme) Color(0xFF342735) else Color(0xFFFFF9F4)
    val ink = if (isDarkTheme) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val muted = if (isDarkTheme) Color(0xFFCFBECB) else Color(0xFF75616F)
    val plum = if (isDarkTheme) Color(0xFFF0BEDC) else Color(0xFF491438)
    CompositionLocalProvider(com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides plum) {
        com.safarparmar.app.ui.theme.SafarComposablesTheme {
            com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
                visible = true, onDismissRequest = {},
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                paneTitle = stringResource(R.string.home_welcome_hello), shape = RoundedCornerShape(28.dp),
                backgroundColor = paper, contentColor = ink, contentPadding = PaddingValues(24.dp),
            ) {
                Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Image(painterResource(R.drawable.home_welcome_book), contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 165.dp), contentScale = ContentScale.Fit)
                    Text(if (userName.isNotBlank()) stringResource(R.string.home_welcome_hello_name, userName)
                        else stringResource(R.string.home_welcome_hello), color = ink, fontFamily = PoppinsFontFamily,
                        fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.home_welcome_intro), color = ink, fontSize = 15.sp,
                        lineHeight = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.home_welcome_message), color = muted, fontSize = 13.sp,
                        lineHeight = 20.sp, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.home_welcome_journey), color = plum, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Button(onClick = onDismiss, style = ButtonStyle.Ghost, shape = CircleShape,
                        contentColor = if (isDarkTheme) paper else Color.White,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).background(plum, CircleShape)) {
                        Text(stringResource(R.string.home_welcome_start), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        ComposablesIcon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlaylistEntryPoint(
    dataStore: SafarDataStore?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    showTooltip: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val context = LocalContext.current
    fun openPlaylist() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.VISUAL_GUIDANCE_PLAYLIST_URL))
        runCatching { context.startActivity(intent) }
    }
    com.safarparmar.app.ui.components.popups.SafarComposablesTooltip(
        enabled = showTooltip,
        side = com.composables.ui.components.TooltipSide.Bottom,
        alignment = com.composables.ui.components.TooltipAlignment.End,
        sideOffset = 8.dp,
        longPressShowDurationMillis = 3500L,
        panel = {
            TooltipPanel(
                modifier = Modifier.widthIn(max = 260.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                com.composables.ui.components.Text(
                    stringResource(R.string.home_video_help),
                    fontSize = 13.sp, lineHeight = 18.sp,
                )
            }
        },
    ) {
        ComposablesIconButton(onClick = ::openPlaylist, style = ButtonStyle.Ghost,
            modifier = modifier.size(48.dp)) {
            com.composables.ui.components.Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.home_watch_video_guide),
                tint = tint, modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun HomeFrostedToolsCard(
    isDarkTheme: Boolean,
    isCompactHeight: Boolean,
    isNarrow: Boolean,
    buttonColor: Color,
    currentSlideRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassShape = RoundedCornerShape(20.dp)
    val glassBaseColor = if (isDarkTheme) {
        Color(0xFF14171E).copy(alpha = 0.62f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.72f)
    }
    val glassBorderBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
        } else {
            listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))
        }
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Blur backdrop
        Box(
            modifier = Modifier
                .matchParentSize()
                .adaptiveBlur(16.dp)
                .clip(glassShape)
                .background(
                    color = if (isDarkTheme) Color(0xFF14171E).copy(alpha = 0.48f)
                    else Color(0xFFFFFFFF).copy(alpha = 0.52f),
                    shape = glassShape,
                )
        )

        // Card content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDarkTheme) 10.dp else 4.dp,
                    shape = glassShape,
                    spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.4f else 0.1f),
                    ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.3f else 0.06f),
                )
                .clip(glassShape)
                .background(glassBaseColor)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = if (isDarkTheme) 0.10f else 0.06f),
                            Color.Transparent,
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = glassBorderBrush,
                    shape = glassShape
                )
                .padding(
                    horizontal = if (isNarrow) 10.dp else 14.dp,
                    vertical = if (isCompactHeight) 12.dp else 16.dp
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 4.dp else 6.dp),
            ) {
                // Header row with Composables UI Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ComposablesIcon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = buttonColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Your tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 0.sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.8f)
                        )
                    }
                    // "Explore" mini button
                    Button(
                        onClick = { onNavigate(Routes.DASHBOARD) },
                        style = ButtonStyle.Ghost,
                        buttonSize = ButtonSize.Small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Explore",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color(0xFFB9E4C9) else Color(0xFF123F28),
                                fontWeight = FontWeight.SemiBold,
                            )
                            ComposablesIcon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFB9E4C9) else Color(0xFF123F28),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Composables UI separator
                HorizontalSeparator(
                    modifier = Modifier.padding(vertical = if (isCompactHeight) 2.dp else 4.dp)
                )

                // Tools grid
                val rows = toolCards.chunked(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp),
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 6.dp else 8.dp)
                        ) {
                            rowItems.forEach { tool ->
                                val isActive = currentSlideRoute.substringBefore("?") == tool.route.substringBefore("?")
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ToolImageCard(
                                        tool = tool,
                                        isActive = isActive,
                                        isDarkTheme = isDarkTheme,
                                        activeBorderColor = buttonColor,
                                        onClick = { onNavigate(tool.route) },
                                        modifier = Modifier.fillMaxWidth(0.90f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolImageCard(
    tool: ToolCard,
    isActive: Boolean,
    isDarkTheme: Boolean,
    activeBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier.bounceClick {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val resolvedBorderColor = if (isActive) activeBorderColor else Theme[colors][borderColor]
            val borderWidth = if (isActive) 2.dp else 1.dp

            // The actual card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cardShape)
                    .background(Theme[colors][panelColor])
                    .border(borderWidth, resolvedBorderColor, cardShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(tool.imageRes).build(),
                    contentDescription = stringResource(tool.labelRes),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Text below the image
        val labelColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF102039)
        Text(
            stringResource(tool.labelRes),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
