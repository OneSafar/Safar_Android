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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
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

    val animateCarousel = com.safarparmar.app.performance.decorativeMotionEnabled()
    LaunchedEffect(currentPage, animateCarousel) {
        if (!animateCarousel) return@LaunchedEffect
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
        val isLight = !isDarkTheme
        val baseBgColor = if (isDarkTheme) Color(0xFF140F0C) else Color(0xFFFAF7F2)

        Box(
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
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (isDarkTheme) 0.45f else 0.38f
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
                    .background(
                        Brush.verticalGradient(
                            colors = if (isLight) {
                                listOf(
                                    Color(0xFFFAF7F2).copy(alpha = 0.65f),
                                    Color(0xFFF3EDE2).copy(alpha = 0.85f),
                                )
                            } else {
                                listOf(
                                    Color(0xFF140F0C).copy(alpha = 0.75f),
                                    Color(0xFF1E1815).copy(alpha = 0.9f),
                                )
                            }
                        )
                    )
            )

            // Main editorial content inside scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Hero Motivation Banner
                HeroMotivationBanner(
                    currentPage = currentPage,
                    slides = slides,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigate,
                )

                // 2. What's New Section (Header + 2 highlight cards)
                WhatsNewSection(
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigate,
                )

                // 3. Features Section (Header with View More + 3-col top row + 2-col bottom row)
                FeaturesSection(
                    tools = toolCards,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigate,
                )

                // 4. Primary CTA Button: Go to Dashboard
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick()
                        .clickable { onNavigate(Routes.DASHBOARD) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isLight) Color(0xFF2A1810) else Color(0xFF3D2419),
                    border = BorderStroke(1.dp, Color(0xFF563424).copy(alpha = 0.5f)),
                    shadowElevation = if (isLight) 6.dp else 0.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 13.sp,
                            color = Color(0xFFE08A3C),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = "GO TO DASHBOARD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFDFBF7),
                        )
                        Text(
                            text = "✦",
                            fontSize = 13.sp,
                            color = Color(0xFFE08A3C),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
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
                    text = "Hello${if (userName.isNotBlank()) ", $userName" else ""}.",
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
                        text = "Welcome to SAFAR, your space to focus, plan, and grow.",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = headlineColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                    Text(
                        text = "Where it's just you and me, and our little battle of staying consistent.\n\nWe'll celebrate small wins, and we'll sit through the bad days together.\n\nA virtual pat on your back. Smile",
                        fontSize = 12.5.sp,
                        color = bodyColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.5.sp,
                    )
                    Text(
                        text = "Your journey starts here.",
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
                        text = "Let's get started",
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
                contentDescription = "Watch SAFAR video guide",
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
                        text = "Need help with SAFAR?\nWatch our YouTube video.",
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
                            contentDescription = "Do not show this tip again",
                            tint = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO MOTIVATION BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroMotivationBanner(
    currentPage: Int,
    slides: List<HomeSlide>,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme
    val currentSlide = slides[currentPage]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onNavigate(currentSlide.route) },
        shape = RoundedCornerShape(24.dp),
        color = if (isLight) Color(0xFFFAF4EB).copy(alpha = 0.96f) else Color(0xFF221A16).copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            if (isLight) Color(0xFF2A1810).copy(alpha = 0.08f) else Color(0xFFFAF7F2).copy(alpha = 0.1f),
        ),
        shadowElevation = if (isLight) 3.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Concentric watermark / focus rings motif
            Canvas(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.BottomEnd)
            ) {
                val strokeColor = (if (isLight) Color(0xFF2A1810) else Color(0xFFFAF7F2)).copy(alpha = 0.05f)
                drawCircle(color = strokeColor, radius = size.minDimension * 0.48f, style = Stroke(width = 1.5f))
                drawCircle(color = strokeColor, radius = size.minDimension * 0.32f, style = Stroke(width = 1.5f))
                drawCircle(color = strokeColor, radius = size.minDimension * 0.16f, style = Stroke(width = 1.5f))
            }

            Crossfade(
                targetState = currentPage,
                animationSpec = tween(durationMillis = 600),
                label = "hero_crossfade",
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Tag with terracotta dot
                    val categoryText = stringResource(slide.titleRes).uppercase()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(
                                color = if (isLight) Color(0xFF2A1810).copy(alpha = 0.05f) else Color(0xFFFAF7F2).copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFC85A32), CircleShape)
                        )
                        Text(
                            text = categoryText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = if (isLight) Color(0xFF563424) else Color(0xFFE7DEC8),
                        )
                    }

                    // Headline
                    Text(
                        text = slide.headline,
                        fontFamily = LoraFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color(0xFF1A0E0A) else Color(0xFFFAF7F2),
                        lineHeight = 26.sp,
                    )

                    // Subtitle / Description
                    Text(
                        text = slide.body,
                        fontSize = 11.5.sp,
                        color = if (isLight) Color(0xFF563424).copy(alpha = 0.85f) else Color(0xFFE7DEC8).copy(alpha = 0.85f),
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WHAT'S NEW SECTION (Dhyan Live & YouTube Focus)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WhatsNewSection(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFE08A3C),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "WHAT'S NEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp,
                color = if (isLight) Color(0xFF563424) else Color(0xFFE7DEC8),
            )
        }

        // 2-Column Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HighlightItemCard(
                title = "Dhyan Live",
                imageRes = R.drawable.tool_live,
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(Routes.liveSessions(view = "live")) },
                modifier = Modifier.weight(1f),
            )
            HighlightItemCard(
                title = "YouTube Focus",
                imageRes = R.drawable.tool_youtube,
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(Routes.YOUTUBE_STUDY_MODE_V2) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HighlightItemCard(
    title: String,
    imageRes: Int,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme

    Surface(
        modifier = modifier
            .bounceClick()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF221A16).copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isLight) Color(0xFF2A1810).copy(alpha = 0.08f) else Color(0xFFFAF7F2).copy(alpha = 0.1f),
        ),
        shadowElevation = if (isLight) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) Color(0xFF1A0E0A) else Color(0xFFFAF7F2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURES SECTION (3 Vertical + 2 Horizontal Cards)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturesSection(
    tools: List<ToolCard>,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Section Header with View More
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FEATURES",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp,
                color = if (isLight) Color(0xFF563424) else Color(0xFFE7DEC8),
            )
            Text(
                text = "View More",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC85A32),
                modifier = Modifier
                    .clickable { onNavigate(Routes.COURSES) }
                    .padding(vertical = 4.dp, horizontal = 6.dp),
            )
        }

        // Top Row: 3-Column Grid (tools[0], tools[1], tools[2]) -> Ekagra, Nishtha, Mehfil
        if (tools.size >= 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tools.take(3).forEach { tool ->
                    VerticalFeatureCard(
                        tool = tool,
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Bottom Row: 2-Column Grid (tools[3], tools[4]) -> Exam Planner, Dhyan
        if (tools.size >= 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tools.drop(3).take(2).forEach { tool ->
                    HorizontalFeatureCard(
                        tool = tool,
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(tool.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalFeatureCard(
    tool: ToolCard,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme

    Surface(
        modifier = modifier
            .bounceClick()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF221A16).copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isLight) Color(0xFF2A1810).copy(alpha = 0.08f) else Color(0xFFFAF7F2).copy(alpha = 0.1f),
        ),
        shadowElevation = if (isLight) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = tool.imageRes),
                contentDescription = stringResource(tool.labelRes),
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(tool.labelRes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) Color(0xFF1A0E0A) else Color(0xFFFAF7F2),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HorizontalFeatureCard(
    tool: ToolCard,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme

    Surface(
        modifier = modifier
            .bounceClick()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF221A16).copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            if (isLight) Color(0xFF2A1810).copy(alpha = 0.08f) else Color(0xFFFAF7F2).copy(alpha = 0.1f),
        ),
        shadowElevation = if (isLight) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = tool.imageRes),
                contentDescription = stringResource(tool.labelRes),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(tool.labelRes),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) Color(0xFF1A0E0A) else Color(0xFFFAF7F2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

