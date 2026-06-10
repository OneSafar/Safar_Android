package com.safarparmar.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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

    var currentPage by remember { mutableIntStateOf(0) }
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
        topBarContentColor = if (isDarkTheme) Color.White else Color.Black,
        emphasizeTopBar = true,
    ) { padding ->
        val ctaPrimary = MaterialTheme.colorScheme.primary
        val ctaOnPrimary = MaterialTheme.colorScheme.onPrimary

        val currentSlide = slides[currentPage]
        val animateColor by animateColorAsState(
            targetValue = currentSlide.accentColor,
            animationSpec = tween(durationMillis = 1000),
            label = "bg_color"
        )
        val baseBgColor = if (isDarkTheme) Color(0xFF0F1115) else Color(0xFFF8F6F2)
        val dynamicGradient = remember(animateColor, baseBgColor) {
            Brush.verticalGradient(
                colors = listOf(
                    animateColor.copy(alpha = if (isDarkTheme) 0.25f else 0.35f),
                    baseBgColor
                )
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicGradient)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompactHeight = screenHeight < 760.dp
            val isNarrow = screenWidth < 380.dp
            val descriptionFrameHeight = (screenHeight * if (isCompactHeight) 0.235f else 0.2376f)
                .coerceIn(if (isCompactHeight) 164.dp else 176.dp, if (isCompactHeight) 198.dp else 228.dp)
            val descriptionFrameWidth = if (isNarrow) 0.78f else 0.8f
            val frameTextVerticalPadding = if (isCompactHeight) 16.dp else 22.dp
            val headlineSize = if (isCompactHeight) 22.sp else 26.sp
            val headlineLineHeight = if (isCompactHeight) 25.sp else 29.sp
            val bottomPanelOffset = (screenHeight * if (isCompactHeight) 0.065f else 0.09f).coerceIn(52.dp, 104.dp)
            val bottomPanelSpacing = if (isCompactHeight) 12.dp else 16.dp
            val toolHorizontalPadding = if (isNarrow) 14.dp else 20.dp
            val ctaHorizontalPadding = if (isNarrow) 32.dp else 44.dp

            // Description card box container matching Go to Dashboard CTA button color
            val topOffset = padding.calculateTopPadding() + 24.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topOffset)
                    .fillMaxWidth(descriptionFrameWidth)
                    .height(descriptionFrameHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ctaPrimary)
                    .border(2.dp, ctaOnPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .clickable { onNavigate(currentSlide.route) }
            ) {
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(durationMillis = 800),
                    label = "text_fade"
                ) { page ->
                    val slide = slides[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = frameTextVerticalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(slide.titleRes).uppercase(),
                            fontSize = if (isCompactHeight) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = ctaOnPrimary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = if (isCompactHeight) 8.dp else 12.dp)
                        )
                        Text(
                            text = slide.headline,
                            fontFamily = LoraFontFamily,
                            fontSize = headlineSize,
                            fontWeight = FontWeight.SemiBold,
                            color = ctaOnPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = headlineLineHeight,
                            modifier = Modifier.padding(bottom = if (isCompactHeight) 6.dp else 10.dp)
                        )
                        Text(
                            text = slide.body,
                            fontSize = if (isCompactHeight) 11.sp else 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = ctaOnPrimary.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = if (isCompactHeight) 14.sp else 16.sp
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
                                isDarkTheme = isDarkTheme,
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
