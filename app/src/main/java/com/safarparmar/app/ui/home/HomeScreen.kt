package com.safarparmar.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import androidx.compose.material.icons.filled.CheckCircle
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.mehfil.formatPostDate
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.util.bounceClick
import com.safarparmar.app.notifications.NotificationPermissionRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle

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
        Routes.NISHTHA,
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
    ToolCard(R.string.module_nishtha, R.drawable.tool_nistha, Routes.NISHTHA),
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

    // Ask for notification permission once — shows a rationale dialog 1.5s after landing on Home
    NotificationPermissionRequest()

    var currentPage by remember { mutableIntStateOf((0 until slides.size).random()) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(currentPage) {
        delay(4000L)
        var next = currentPage
        while (next == currentPage) {
            next = (0 until slides.size).random()
        }
        currentPage = next
    }

    var showBellDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val notificationBellState by notificationBellViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(showBellDialog) {
        if (showBellDialog) notificationBellViewModel.load()
    }

    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.5.1"
        } catch (e: Exception) {
            "1.5.1"
        }
    }

    if (showBellDialog) {
        Dialog(
            onDismissRequest = {
                notificationBellViewModel.markAllRead()
                showBellDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                notificationBellViewModel.markAllRead()
                                showBellDialog = false
                            }
                        )
                )

                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .align(Alignment.CenterEnd),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Notifications",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        notificationBellViewModel.markAllRead()
                                        showBellDialog = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Unread",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Mark all as read",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        notificationBellViewModel.markAllRead()
                                    }
                                )
                            }

                            if (notificationBellState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            } else if (notificationBellState.items.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "You're all caught up! No alerts.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    notificationBellState.items.forEach { item ->
                                        NotificationItemRow(
                                            item = item,
                                            onItemClick = {
                                                showBellDialog = false
                                                notificationBellViewModel.markAllRead()
                                                val deepLink = item.deepLink
                                                if (!deepLink.isNullOrBlank()) {
                                                    val route = com.safarparmar.app.notifications.NotificationDeepLinkHandler.routeFor(deepLink)
                                                    onNavigate(route)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        notificationBellViewModel.markAllRead()
                                        showBellDialog = false
                                        onNavigate(Routes.SETTINGS)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Settings",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "App Version v$appVersion",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    notificationBellViewModel.markAllRead()
                                    showBellDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Close",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
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
            IconButton(onClick = { showBellDialog = true }) {
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
        val descriptionBorderColor = if (isDarkTheme) Color.White else buttonColor

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
            val bgImageRes = if (isDarkTheme) {
                when (slides[currentPage].route) {
                    Routes.EKAGRA -> R.drawable.ekagra_dark
                    Routes.MEHFIL -> R.drawable.dark_mehfil
                    Routes.DHYAN -> R.drawable.dark_dhyan
                    Routes.STUDY_PLANNER -> R.drawable.study_planner_dark
                    else -> R.drawable.bg_home_dark
                }
            } else {
                when (slides[currentPage].route) {
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
                                val isActive = slides[currentPage].route == tool.route
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

@Composable
private fun NotificationItemRow(
    item: NotificationFeedItem,
    onItemClick: () -> Unit
) {
    val isUnread = item.isUnread
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 0.dp),
        border = if (isUnread) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isUnread) {
                    val tag = when (item.source) {
                        NotificationFeedSource.CUSTOM -> "ANNOUNCEMENT"
                    }
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Read",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            NotificationBodyContent(body = item.body)

            if (item.createdAt.isNotBlank()) {
                Text(
                    text = formatPostDate(item.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NotificationBodyContent(body: String) {
    val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val isAnnouncement = body.contains("Important Update:") || lines.size >= 2
    
    if (isAnnouncement) {
        val header = lines.firstOrNull { it.contains("Important Update:", ignoreCase = true) } ?: "Important Update:"
        val titleLine = lines.firstOrNull { !it.contains("Important Update:", ignoreCase = true) } ?: ""
        val remainingText = lines.filter { it != header && it != titleLine }.joinToString("\n")
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_megaphone),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (titleLine.isNotEmpty()) {
                        Text(
                            text = titleLine,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (remainingText.isNotEmpty()) {
                        Text(
                            text = remainingText,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
