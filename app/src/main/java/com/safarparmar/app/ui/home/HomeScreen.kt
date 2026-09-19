package com.safarparmar.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.safarparmar.app.ui.theme.LoraFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.util.bounceClick
import com.safarparmar.app.notifications.NotificationPermissionRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import com.safarparmar.app.util.YoutubeUrls
import com.safarparmar.app.performance.adaptiveBlur
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.studyplanner.components.SafarBackdropBlurRadiusPx
import com.safarparmar.app.ui.studyplanner.components.SafarGlassDialogHost
import com.safarparmar.app.ui.studyplanner.components.rememberPlannerBackdropBlur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_home),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        topBarContentColor = if (isDarkTheme) Color.White else Color.Black,
        emphasizeTopBar = true,
        topBarActions = {
            VideoPlaylistEntryPoint(
                dataStore = dataStore,
                tint = if (isDarkTheme) Color.White else Color.Black,
                isDarkTheme = isDarkTheme,
                showTooltip = true,
            )
            IconButton(onClick = { showAnnouncementsSheet = true }) {
                BadgedBox(
                    badge = {
                        if (notificationBellState.unreadCount > 0) {
                            Badge {
                                Text(
                                    text = if (notificationBellState.unreadCount > 9) "9+" else notificationBellState.unreadCount.toString()
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.home_notifications_updates),
                        tint = if (isDarkTheme) Color.White else Color.Black
                    )
                }
            }
        }
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
                    currentAccent.copy(alpha = if (isDarkTheme) 0.25f else 0.35f),
                    baseBgColor.copy(alpha = if (isDarkTheme) 0.6f else 0.7f)
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
            val bottomPanelOffset = (screenHeight * if (isCompactHeight) 0.03f else 0.05f).coerceIn(24.dp, 64.dp)
            val bottomPanelSpacing = if (isCompactHeight) 12.dp else 16.dp
            val toolHorizontalPadding = if (isNarrow) 14.dp else 20.dp
            val ctaHorizontalPadding = if (isNarrow) 32.dp else 44.dp

            // Plain Description text overlay (no box container)
            val topOffset = padding.calculateTopPadding() + 32.dp
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topOffset)
                    .fillMaxWidth(if (isNarrow) 0.85f else 0.9f)
                    .clickable { onNavigate(currentSlide.route) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(durationMillis = 800),
                    label = "text_fade"
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(slide.titleRes).uppercase(),
                            fontSize = if (isCompactHeight) 12.1.sp else 13.2.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = descriptionTextColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                shadow = Shadow(
                                    color = glowColor.copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 12f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = if (isCompactHeight) 4.dp else 6.dp)
                        )
                        Text(
                            text = stringResource(slide.headlineRes),
                            fontFamily = LoraFontFamily,
                            fontSize = if (isCompactHeight) 24.sp else 28.sp,
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
                            lineHeight = if (isCompactHeight) 28.sp else 32.sp
                        )
                    }
                }
            }

            // Bottom overlay: tools + button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = padding.calculateBottomPadding() + bottomPanelOffset, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(bottomPanelSpacing),
            ) {
                // Frosted Glass Container for Tools
                val glassShape = RoundedCornerShape(22.dp)
                val glassBaseColor = if (isDarkTheme) {
                    Color(0xFF14171E).copy(alpha = 0.58f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.65f)
                }
                val glassBorderBrush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.35f))
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = toolHorizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    // Soft Gaussian blur backdrop layer for frosted glass
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .adaptiveBlur(14.dp)
                            .clip(glassShape)
                            .background(
                                color = if (isDarkTheme) Color(0xFF14171E).copy(alpha = 0.50f)
                                else Color(0xFFFFFFFF).copy(alpha = 0.55f),
                                shape = glassShape,
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        buttonColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
                                        Color.Transparent,
                                    )
                                ),
                                shape = glassShape,
                            )
                    )

                    // Frosted Glass Container for Tools
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDarkTheme) 12.dp else 6.dp,
                                shape = glassShape,
                                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.12f),
                                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.08f),
                            )
                            .clip(glassShape)
                            .background(glassBaseColor)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        buttonColor.copy(alpha = if (isDarkTheme) 0.12f else 0.08f),
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
                                horizontal = if (isNarrow) 8.dp else 10.dp,
                                vertical = if (isCompactHeight) 12.dp else 14.dp
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 9.dp else 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rows = toolCards.chunked(4)
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
                                            borderColor = buttonColor,
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

                Button(
                    onClick = { onNavigate(Routes.DASHBOARD) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ctaHorizontalPadding)
                        .height(if (isCompactHeight) 48.dp else 50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    ),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, buttonColor.copy(alpha = 0.85f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "✦   GO TO DASHBOARD   ✦",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = Color.White,
                    ),
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
    val scope = rememberCoroutineScope()
    val dismissalFlow = remember(dataStore) {
        dataStore?.videoGuideTooltipDismissed ?: MutableStateFlow(false)
    }
    val tooltipDismissed by dismissalFlow.collectAsStateWithLifecycle(initialValue = false)
    var tooltipVisible by remember { mutableStateOf(showTooltip) }
    val isLight = !isDarkTheme
    val flatShape = RoundedCornerShape(16.dp)

    val surfaceBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E1F25)
    val borderClr = if (isLight) Color(0xFFE2E8F0) else Color(0xFF2D2F36)
    val tipTitleColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val tipIconTint = Color(0xFFEC4899)

    fun openPlaylist() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.VISUAL_GUIDANCE_PLAYLIST_URL))
        runCatching { context.startActivity(intent) }
    }

    Box(modifier = modifier) {
        IconButton(onClick = ::openPlaylist) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = stringResource(R.string.home_watch_video_guide),
                tint = tint,
            )
        }

        DropdownMenu(
            expanded = tooltipVisible && !tooltipDismissed,
            onDismissRequest = { tooltipVisible = false },
            shape = flatShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable(onClick = ::openPlaylist),
                shape = flatShape,
                color = surfaceBg,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, borderClr),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = tipIconTint,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_video_help),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        color = tipTitleColor,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            tooltipVisible = false
                            if (dataStore != null) {
                                scope.launch { dataStore.setVideoGuideTooltipDismissed(true) }
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.home_video_tip_dismiss),
                            tint = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp),
                        )
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
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "card_scale",
    )
    val verticalSpacing by animateDpAsState(
        targetValue = if (isActive) 7.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "vertical_spacing",
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.bounceClick {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val resolvedBorderColor = if (isActive) borderColor else borderColor.copy(alpha = 0.5f)
            val borderWidth = if (isActive) 2.2.dp else 1.5.dp

            // The actual card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        clip = false
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .border(borderWidth, resolvedBorderColor, RoundedCornerShape(12.dp))
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
        val labelColor = if (isDarkTheme) {
            if (isActive) Color.White else Color(0xFFD7E4DC)
        } else {
            if (isActive) borderColor else Color.Black.copy(alpha = 0.6f)
        }
        Text(
            stringResource(tool.labelRes),
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
        )
    }
}
