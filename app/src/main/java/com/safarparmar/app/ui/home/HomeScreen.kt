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
    val headline: String,
    val body: String,
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
        "Boost Your\nProductivity",
        "Stay focused with your own Pomodoro\ntimer and track your work sessions",
        "img_ekagara.webp",
        Routes.EKAGRA,
        Color(0xFFAAC7FF),
        Color(0xFF0A305F)
    ),
    HomeSlide(
        R.string.module_nishtha,
        "Build Daily\nHabits",
        "Track consistency, journal, reflect\non your emotional state",
        "img_nishtha.webp",
        Routes.nishthaRoot(),
        Color(0xFFA9D0B3),
        Color(0xFF143723)
    ),
    HomeSlide(
        R.string.module_mehfil,
        "Capture Your\nThoughts",
        "Notes, ideas and reminders\n— All in one place",
        "img_mehefil.webp",
        Routes.MEHFIL,
        Color(0xFFFFB5A0),
        Color(0xFF561F0F)
    ),
    HomeSlide(
        R.string.module_dhyan,
        "Find Your\nInner Peace",
        "Meditation sessions with Parmar sir",
        "img_dhyan.webp",
        Routes.DHYAN,
        Color(0xFFDDBCE0),
        Color(0xFF3F2844)
    ),
    HomeSlide(
        R.string.module_study_planner,
        "Plan Your\nSuccess",
        "Track your syllabus progress, schedule\nyour targets, and achieve your daily goals",
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
        title = "Home",
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
                        contentDescription = "Notifications & Updates",
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
                            text = slide.headline,
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
                // 2x2 grid of tools (cube formation)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = toolHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(toolCards.take(3), toolCards.drop(3))
                    rows.forEach { rowItems ->
                        val rowWidth = if (rowItems.size == 3) {
                            if (isNarrow) 0.82f else 0.78f
                        } else {
                            if (isNarrow) 0.54f else 0.51f
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(rowWidth),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                        modifier = Modifier.fillMaxWidth(0.92f),
                                    )
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
    val titleColor = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Lavender
    val headlineColor = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
    val bodyColor = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
    val accentLine = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    val shape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SafarGlassDialogHost(isDarkTheme = isDarkTheme) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .height(360.dp)
                    .safarFrostedPanel(isLight = isLight, shape = shape, elevation = if (isLight) 18.dp else 10.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
            Text(
                text = "Hello${if (userName.isNotBlank()) ", $userName" else ""}.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = titleColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Welcome to SAFAR, your space to focus, plan, and grow.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = headlineColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Where it's just you and me, and our little battle of staying consistent.\n\nWe'll celebrate small wins, and we'll sit through the bad days together.\n\nA virtual pat on your back.\nSmile",
                    fontSize = 12.sp,
                    color = bodyColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your journey starts here.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentLine,
                    textAlign = TextAlign.Center,
                )
            }
            MacOSPrimaryActionButton(
                text = "Let's get started",
                onClick = onDismiss,
                isLight = isLight,
            )
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
    val glassShape = RoundedCornerShape(14.dp)
    val tipTitleColor = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
    val tipIconTint = if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink

    fun openPlaylist() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.VISUAL_GUIDANCE_PLAYLIST_URL))
        runCatching { context.startActivity(intent) }
    }

    Box(modifier = modifier) {
        IconButton(onClick = ::openPlaylist) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Watch SAFAR video guide",
                tint = tint,
            )
        }

        DropdownMenu(
            expanded = tooltipVisible && !tooltipDismissed,
            onDismissRequest = { tooltipVisible = false },
            shape = glassShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            // Popup is its own window — enable strong compositor blur so the
            // busy home hero text doesn't bleed through the tooltip.
            val blurred = rememberPlannerBackdropBlur(radiusPx = SafarBackdropBlurRadiusPx)

            Row(
                modifier = Modifier
                    .widthIn(max = 268.dp)
                    .clip(glassShape)
                    .background(
                        if (isLight) {
                            Color(0xFFEBEEF3).copy(alpha = if (blurred) 0.78f else 0.94f)
                        } else {
                            Color(0xFF141418).copy(alpha = if (blurred) 0.72f else 0.90f)
                        },
                    )
                    .safarFrostedPanel(
                        isLight = isLight,
                        shape = glassShape,
                        tintAlpha = if (blurred) 0.38f else 0.52f,
                        elevation = if (isLight) 16.dp else 10.dp,
                    )
                    .clickable(onClick = ::openPlaylist)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = tipIconTint,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Need help with SAFAR? Watch our YouTube video.",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
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
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Do not show this tip again",
                        tint = tipTitleColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp),
                    )
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
        targetValue = if (isActive) 10.dp else 7.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "vertical_spacing",
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.bounceClick {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            // Increased by 20% from 2.16 / 1.44 dp
            val borderWidth = if (isActive) 2.6.dp else 1.73.dp

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
            fontSize = 13.2.sp, // +20% vs 11.sp
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
        )
    }
}
