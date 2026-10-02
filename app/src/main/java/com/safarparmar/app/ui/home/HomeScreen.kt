package com.safarparmar.app.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
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
    ToolCard(R.string.module_nishtha, R.drawable.tool_nistha, Routes.nishthaRoot()),
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
) {
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
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
                                )
                            )
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
        val buttonColor = currentSlide.uiColor
        val buttonTextColor = currentSlide.accentColor

        val descriptionTextColor = if (isDarkTheme) Color.White else buttonColor
        val baseBgColor = MaterialTheme.colorScheme.background
        val currentAccent = currentSlide.accentColor
        val dynamicGradient = remember(baseBgColor, currentAccent) {
            Brush.verticalGradient(
                colors = listOf(
                    currentAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.30f),
                    baseBgColor.copy(alpha = if (isDarkTheme) 0.55f else 0.65f),
                    baseBgColor.copy(alpha = if (isDarkTheme) 0.85f else 0.92f),
                )
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(baseBgColor)
        ) {
            val slideRouteBase = slides[currentPage].route.substringBefore("?")
            val bgImageRes = if (isDarkTheme) {
                when (slideRouteBase) {
                    Routes.EKAGRA -> R.drawable.ekagra_dark
                    Routes.MEHFIL -> R.drawable.dark_mehfil
                    Routes.DHYAN -> R.drawable.dark_dhyan
                    Routes.STUDY_PLANNER -> R.drawable.study_planner_dark
                    else -> R.drawable.bg_home_dark
                }
            } else {
                when (slideRouteBase) {
                    Routes.EKAGRA -> R.drawable.ekagra_light
                    Routes.MEHFIL -> R.drawable.light_mehfil
                    Routes.DHYAN -> R.drawable.dhyan_liight
                    Routes.STUDY_PLANNER -> R.drawable.study_planner_light
                    else -> R.drawable.bg_home_light
                }
            }

            // ── Background image carousel (untouched) ──────────────
            Crossfade(
                targetState = bgImageRes,
                animationSpec = tween(durationMillis = 800),
                label = "bg_image_fade"
            ) { targetRes ->
                Image(
                    painter = painterResource(id = targetRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dynamicGradient)
            )

            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompactHeight = screenHeight < 760.dp
            val isNarrow = screenWidth < 380.dp

            val topBarTint = if (isDarkTheme) Color.White else Color(0xFF1E293B)

            // ── Seamless Top Bar Overlay on Carousel ──────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                ComposablesIconButton(
                    onClick = { openDrawerAction?.invoke() },
                    style = ButtonStyle.Ghost,
                    modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.nav_open_menu),
                        tint = topBarTint,
                        modifier = Modifier.size(26.4.dp),
                    )
                }

                androidx.compose.material3.Text(
                    text = stringResource(R.string.nav_home),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 17.6.sp,
                    fontWeight = FontWeight.Bold,
                    color = topBarTint,
                    textAlign = TextAlign.Center,
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VideoPlaylistEntryPoint(
                        dataStore = dataStore,
                        tint = topBarTint,
                        isDarkTheme = isDarkTheme,
                        showTooltip = true,
                        modifier = Modifier.size(48.dp),
                    )
                    ComposablesIconButton(
                        onClick = { showAnnouncementsSheet = true },
                        style = ButtonStyle.Ghost,
                    ) {
                        BadgedBox(
                            badge = {
                                if (notificationBellState.unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White,
                                        modifier = Modifier.offset(x = 11.dp, y = (-6).dp),
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = if (notificationBellState.unreadCount > 9) "9+" else notificationBellState.unreadCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.home_notifications_updates),
                                tint = topBarTint,
                                modifier = Modifier.size(26.4.dp),
                            )
                        }
                    }
                }
            }

            // ── Text overlay: Module label + headline + description + dots ──
            val topOffset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + (maxHeight * 0.15f)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topOffset, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .clickable { onNavigate(currentSlide.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 10.dp)
            ) {
                // Module category pill
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(durationMillis = 600),
                    label = "pill_fade"
                ) { page ->
                    val slide = slides[page]
                    val pillBg = if (isDarkTheme) {
                        slide.accentColor.copy(alpha = 0.25f)
                    } else {
                        slide.uiColor.copy(alpha = 0.18f)
                    }
                    val pillTextColor = if (isDarkTheme) slide.accentColor else slide.uiColor
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pillBg,
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) slide.accentColor.copy(alpha = 0.4f)
                                else slide.uiColor.copy(alpha = 0.3f)
                            )
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(slide.titleRes).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 2.sp,
                                color = pillTextColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Headline
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(durationMillis = 800),
                    label = "headline_fade"
                ) { page ->
                    val slide = slides[page]
                    val glowColor = if (isDarkTheme) {
                        slide.accentColor
                    } else {
                        slide.accentColor.copy(alpha = 0.8f)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(slide.headlineRes),
                            fontFamily = LoraFontFamily,
                            fontSize = if (isCompactHeight) 22.sp else 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = descriptionTextColor,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                shadow = Shadow(
                                    color = glowColor,
                                    offset = Offset(0f, 0f),
                                    blurRadius = 16f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            lineHeight = if (isCompactHeight) 26.sp else 30.sp
                        )
                        // Body description
                        androidx.compose.material3.Text(
                            text = stringResource(slide.bodyRes),
                            fontSize = if (isCompactHeight) 12.sp else 13.sp,
                            color = descriptionTextColor.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp,
                            style = MaterialTheme.typography.bodySmall.copy(
                                shadow = Shadow(
                                    color = glowColor.copy(alpha = 0.3f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 8f
                                )
                            ),
                        )
                    }
                }

                // Animated page indicator dots
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    slides.indices.forEach { index ->
                        val isSelected = index == currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 7.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dot_w_$index"
                        )
                        val dotAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.4f,
                            animationSpec = tween(400),
                            label = "dot_alpha_$index"
                        )
                        val dotColor = if (isSelected) {
                            if (isDarkTheme) currentSlide.accentColor else currentSlide.uiColor
                        } else {
                            descriptionTextColor
                        }
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor.copy(alpha = dotAlpha))
                                .clickable { currentPage = index }
                        )
                    }
                }
            }

            // ── Bottom panel: Tools card + Dashboard CTA ──────────────
            val bottomPanelOffset = (screenHeight * if (isCompactHeight) 0.025f else 0.04f).coerceIn(20.dp, 56.dp)
            val toolHorizontalPadding = if (isNarrow) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        bottom = padding.calculateBottomPadding() + bottomPanelOffset,
                        top = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 10.dp else 14.dp),
            ) {
                // ── Frosted glass tools card ──────────────────────
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = toolHorizontalPadding),
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
                                        tint = if (isDarkTheme) currentSlide.accentColor else currentSlide.uiColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "MODULES & TOOLS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp,
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
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        ComposablesIcon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
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
                                            val isActive = slides[currentPage].route.substringBefore("?") == tool.route.substringBefore("?")
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
                                                    modifier = Modifier.fillMaxWidth(0.96f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Dashboard CTA button ──────────────────────────
                Button(
                    onClick = { onNavigate(Routes.DASHBOARD) },
                    style = ButtonStyle.Primary,
                    buttonSize = ButtonSize.Regular,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isNarrow) 20.dp else 28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ComposablesIcon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN FULL DASHBOARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ComposablesIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
private fun SafarWelcomeDialog(
    userName: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
) {
    val isLight = !isDarkTheme
    val titleColor = if (isLight) Color(0xFF581C87) else Color(0xFFC084FC)
    val headlineColor = if (isLight) Color(0xFF1F2937) else Color(0xFFF9FAFB)
    val bodyColor = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)
    val shape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            shape = shape,
            color = if (isLight) Color.White else Color(0xFF1E1F25),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF2D2F36)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = if (userName.isNotBlank()) {
                        stringResource(R.string.home_welcome_hello_name, userName)
                    } else {
                        stringResource(R.string.home_welcome_hello)
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_welcome_intro),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = headlineColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_welcome_message),
                        fontSize = 12.5.sp,
                        color = bodyColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.5.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_welcome_journey),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    onClick = onDismiss,
                    style = ButtonStyle.Primary,
                    buttonSize = ButtonSize.Regular,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.home_welcome_start),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
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
) {
    val context = LocalContext.current
    fun openPlaylist() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.VISUAL_GUIDANCE_PLAYLIST_URL))
        runCatching { context.startActivity(intent) }
    }
    com.composables.ui.components.Tooltip(
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
                tint = tint, modifier = Modifier.size(24.dp),
            )
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
    val cardShape = RoundedCornerShape(22.dp)

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
        val labelColor = if (isActive) {
            Theme[colors][primaryColor]
        } else {
            Theme[colors][onPanelColor]
        }
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
