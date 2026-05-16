package com.safar.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.safar.app.ui.theme.LoraFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.*
import com.safar.app.notifications.NotificationPermissionRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private data class HomeSlide(
    val titleRes: Int,
    val headline: String,
    val body: String,
    val bgImageUrl: String,
    val route: String,
    val accentColor: Color,
)

private data class ToolCard(
    val labelRes: Int,
    val imageRes: Int,
    val route: String,
)

private val slides =
        listOf(
                HomeSlide(
                        R.string.module_ekagra,
                        "Boost Your\nProductivity",
                        "Stay focused with your own Pomodoro\ntimer and track your work sessions",
                        "img_ekagara.webp",
                        Routes.EKAGRA,
                        Sky600
                ),
                HomeSlide(
                        R.string.module_nishtha,
                        "Build Daily\nHabits",
                        "Track consistency, journal, reflect\non your emotional state",
                        "img_nishtha.webp",
                        Routes.NISHTHA,
                        Teal400
                ),
                HomeSlide(
                        R.string.module_mehfil,
                        "Capture Your\nThoughts",
                        "Notes, ideas and reminders\n— All in one place",
                        "img_mehefil.webp",
                        Routes.MEHFIL,
                        Orange500
                ),
                HomeSlide(
                        R.string.module_dhyan,
                        "Find Your\nInner Peace",
                        "Meditation sessions with Parmar sir",
                        "img_dhyan.webp",
                        Routes.DHYAN,
                        Violet600
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
    onLanguageClick: () -> Unit = {},
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

    var currentPage by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            currentPage = (currentPage + 1) % slides.size
        }
    }

    SafarDrawerScaffold(
        title = "Home",
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarContentColor = Color.White,
        emphasizeTopBar = true,
    ) { padding ->
        val ctaPrimary = MaterialTheme.colorScheme.primary
        val ctaOnPrimary = MaterialTheme.colorScheme.onPrimary
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompactHeight = screenHeight < 760.dp
            val isNarrow = screenWidth < 380.dp
            val descriptionFrameHeight = (screenHeight * if (isCompactHeight) 0.235f else 0.2376f)
                .coerceIn(if (isCompactHeight) 164.dp else 176.dp, if (isCompactHeight) 198.dp else 228.dp)
            val descriptionFrameWidth = if (isNarrow) 0.78f else 0.8f
            val frameTextTopOffset = descriptionFrameHeight * if (isCompactHeight) 0.07f else 0.085f
            val frameTextVerticalPadding = if (isCompactHeight) 20.dp else 26.dp
            val headlineSize = if (isCompactHeight) 23.sp else 27.sp
            val headlineLineHeight = if (isCompactHeight) 26.sp else 30.sp
            val bottomPanelOffset = (screenHeight * if (isCompactHeight) 0.065f else 0.09f).coerceIn(52.dp, 104.dp)
            val bottomPanelSpacing = if (isCompactHeight) 12.dp else 16.dp
            val toolHorizontalPadding = if (isNarrow) 14.dp else 20.dp
            val ctaHorizontalPadding = if (isNarrow) 32.dp else 44.dp

            // Full-screen crossfade (no slide effect) with Ken Burns on each slide
            Crossfade(
                targetState = currentPage,
                animationSpec = tween(800),
                modifier = Modifier.fillMaxSize(),
                label = "slide_cf",
            ) { page ->
                val slide = slides[page]
                // Ken Burns: gentler range so portrait assets aren’t over-cropped on phones
                // (slightly zoomed out vs 1f..1.1f; may show thin edges — acceptable per design)
                val startScale = if (page % 2 == 0) 1.0f else 1.06f
                val endScale = if (page % 2 == 0) 1.06f else 1.0f
                val bgScale = remember { Animatable(startScale) }
                LaunchedEffect(Unit) {
                    bgScale.animateTo(endScale, tween(5000, easing = LinearEasing))
                }

                Box(Modifier.fillMaxSize().clickable { onNavigate(slide.route) }) {
                    AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data("file:///android_asset/${slide.bgImageUrl}") .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().scale(bgScale.value),
                    )
                    // Black film overlay to make UI elements distinct
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                    // Dark vignette
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.4f),
                                0.5f to Color.Black.copy(alpha = 0.2f),
                                1.0f to Color.Black.copy(alpha = 0.7f),
                            )
                        )
                    )
                    // Card background replacement
                    val topOffset = padding.calculateTopPadding() + 16.dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset)
                            .fillMaxWidth(descriptionFrameWidth)
                            .height(descriptionFrameHeight)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        // Frosted glass pane inside the frame (drawn behind the frame)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 34.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDarkTheme) Color.Transparent else Color.Black.copy(alpha = 0.5f))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.2f),
                                            Color.White.copy(alpha = 0.1f),
                                        )
                                    )
                                )
                        )

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(if (isDarkTheme) "file:///android_asset/Description_Box_Frame.png" else "file:///android_asset/WEEE.png")
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = frameTextTopOffset)
                                .padding(horizontal = 20.dp, vertical = frameTextVerticalPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                stringResource(slide.titleRes).uppercase(),
                                fontSize = if (isCompactHeight) 9.sp else 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                color = Color.White.copy(alpha = 0.82f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = if (isCompactHeight) 8.dp else 10.dp),
                            )
                            Text(
                                slide.headline,
                                fontFamily = LoraFontFamily,
                                fontSize = headlineSize,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = headlineLineHeight,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
                // 4 tool cards — each card wrapped in a weight Box so scale overflow
                // stays within the allocated slot (no cross-card overlap)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = toolHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    toolCards.forEach { tool ->
                        val isActive = slides[currentPage].route == tool.route
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            ToolImageCard(
                                tool = tool,
                                isActive = isActive,
                                borderColor = ctaPrimary,
                                onClick = { onNavigate(tool.route) },
                                modifier = Modifier.fillMaxWidth(0.902f), // +10% vs 0.82f
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ctaHorizontalPadding)
                        .height(if (isCompactHeight) 48.dp else 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(50))
                            .background(ctaPrimary)
                            .border(1.dp, ctaPrimary.copy(alpha = 0.85f), RoundedCornerShape(50))
                            .semantics { role = Role.Button }
                            .clickable { onNavigate(Routes.DASHBOARD) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "✦   GO TO DASHBOARD   ✦",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = ctaOnPrimary,
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
        modifier = modifier.clickable {
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
        Text(
            stringResource(tool.labelRes),
            fontSize = 13.2.sp, // +20% vs 11.sp
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.White else Color(0xFFD7E4DC),
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(if (isActive) 1.05f else 1f)
        )
    }
}
