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
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.util.bounceClick
import com.safarparmar.app.notifications.NotificationPermissionRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications

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
)


private val toolCards = listOf(
    ToolCard(R.string.module_ekagra, R.drawable.tool_ekagra, Routes.EKAGRA),
    ToolCard(R.string.module_nishtha, R.drawable.tool_nistha, Routes.NISHTHA),
    ToolCard(R.string.module_mehfil, R.drawable.tool_mehfil, Routes.MEHFIL),
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

    // Fetch active notifications currently in the system tray for our app
    val activeNotifications = remember(showBellDialog) {
        if (showBellDialog) {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            try {
                notificationManager?.activeNotifications?.mapNotNull { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getString(android.app.Notification.EXTRA_TITLE)
                    val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
                    if (!title.isNullOrBlank() && !text.isNullOrBlank()) {
                        title to text
                    } else null
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
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
        AlertDialog(
            onDismissRequest = { showBellDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontFamily = LoraFontFamily,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Unseen Notifications Section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Unseen Notifications:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        if (activeNotifications.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "You're all caught up! No unseen alerts.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                activeNotifications.forEach { (title, body) ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = body,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // App Version Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Version",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "v$appVersion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBellDialog = false
                        onNavigate(Routes.SETTINGS)
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBellDialog = false }) {
                    Text("Close")
                }
            }
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
            IconButton(onClick = { showBellDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications & Updates",
                    tint = if (isDarkTheme) Color.White else Color.Black
                )
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
                    else -> R.drawable.bg_home_dark
                }
            } else {
                when (slides[currentPage].route) {
                    Routes.EKAGRA -> R.drawable.ekagra_light
                    Routes.MEHFIL -> R.drawable.light_mehfil
                    Routes.DHYAN -> R.drawable.dhyan_liight
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
                    val rows = toolCards.chunked(2)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(if (isNarrow) 0.82f else 0.78f),
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
