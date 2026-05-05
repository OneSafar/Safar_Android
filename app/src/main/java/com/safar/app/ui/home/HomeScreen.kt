package com.safar.app.ui.home

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.*
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
    val imageUrl: String,
    val route: String,
    val glowColor: Color,
)

private val slides =
        listOf(
                HomeSlide(
                        R.string.module_ekagra,
                        "Boost Your\nProductivity",
                        "Stay focused with your own Pomodoro\ntimer and track your work sessions",
                        "img_ekagara.jpeg",
                        Routes.EKAGRA,
                        Sky600
                ),
                HomeSlide(
                        R.string.module_nishtha,
                        "Build Daily\nHabits",
                        "Track consistency, journal, reflect\non your emotional state",
                        "img_nishtha.jpeg",
                        Routes.NISHTHA,
                        Teal400
                ),
                HomeSlide(
                        R.string.module_mehfil,
                        "Capture Your\nThoughts",
                        "Notes, ideas and reminders\n— All in one place",
                        "img_mehefil.jpeg",
                        Routes.MEHFIL,
                        Orange500
                ),
                HomeSlide(
                        R.string.module_dhyan,
                        "Find Your\nInner Peace",
                        "Meditation sessions with Parmar sir",
                        "img_dhyan.jpeg",
                        Routes.DHYAN,
                        Violet600
                ),
        )

private val toolCards = listOf(
    ToolCard(R.string.module_ekagra, "https://safar.parmarssc.in/focus-timer.webp", Routes.EKAGRA, Blue600),
    ToolCard(R.string.module_nishtha, "https://safar.parmarssc.in/nishtha-silhouette.webp", Routes.NISHTHA, Color(0xFF84FF00)),
    ToolCard(R.string.module_mehfil, "https://safar.parmarssc.in/mehfil-silhouette.webp", Routes.MEHFIL, Green500),
    ToolCard(R.string.module_dhyan, "https://safar.parmarssc.in/meditation-silhouette.webp", Routes.DHYAN, Violet600),
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
        .collectAsState(initial = true)
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) onNavigateToAuth()
    }

    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            currentPage = (currentPage + 1) % slides.size
        }
    }

    SafarDrawerScaffold(
        title = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarContentColor = Color.White,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // Full-screen crossfade (no slide effect) with Ken Burns on each slide
            Crossfade(
                targetState = currentPage,
                animationSpec = tween(800),
                modifier = Modifier.fillMaxSize(),
                label = "slide_cf",
            ) { page ->
                val slide = slides[page]
                // Alternate direction: even pages zoom in, odd pages zoom out
                val startScale = if (page % 2 == 0) 1f else 1.10f
                val endScale   = if (page % 2 == 0) 1.10f else 1f
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
                    // Glass card
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val screenHeight = configuration.screenHeightDp.dp
                    val topOffset = padding.calculateTopPadding() + 16.dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topOffset)
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(16.dp)) // Clip the entire card including the blurred background
                    ) {
                        // 1. The canonical blurred background layer wrapped so it doesn't expand the parent
                        Box(modifier = Modifier.matchParentSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data("file:///android_asset/${slide.bgImageUrl}").build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .requiredSize(screenWidth, screenHeight)
                                    .align(Alignment.TopCenter)
                                    .offset(y = -topOffset) // Offset perfectly aligns the image with the actual background
                                    .scale(bgScale.value) // Apply the exact same scale animation
                                    .blur(35.dp, BlurredEdgeTreatment.Unbounded)
                            )

                            // 2. The dark vignette to match the background's vignette (since we blur the raw image)
                            Box(
                                Modifier
                                    .requiredSize(screenWidth, screenHeight)
                                    .align(Alignment.TopCenter)
                                    .offset(y = -topOffset)
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Black.copy(alpha = 0.4f),
                                            0.5f to Color.Black.copy(alpha = 0.2f),
                                            1.0f to Color.Black.copy(alpha = 0.7f),
                                        )
                                    )
                            )
                        }
                        val cardTransition = rememberInfiniteTransition(label = "card_shine")
                        val cardShineX by cardTransition.animateFloat(
                            initialValue = -600f,
                            targetValue = 1200f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "card_shine_x"
                        )

                        // Subtler shadow for more translucent look
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(20.dp, BlurredEdgeTreatment.Unbounded)
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f), // Lighter frosty top
                                            slide.accentColor.copy(alpha = 0.15f),
                                            Color(0xFFF0F4F8).copy(alpha = 0.25f) // Cooler frosty bottom
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.8f), // Strong frost highlight
                                            Color.White.copy(alpha = 0.15f),
                                            Color.White.copy(alpha = 0.4f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                        ) {
                            // Animated Gloss layer
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.0f),
                                                Color.White.copy(alpha = 0.2f),
                                                Color.White.copy(alpha = 0.0f),
                                            ),
                                            start = Offset(cardShineX, 0f),
                                            end = Offset(cardShineX + 150f, 300f)
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {

                            Text(
                                stringResource(slide.titleRes).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.5.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            )
                            Text(
                                slide.headline,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 33.sp,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            )
                            Text(
                                slide.body,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

            // Bottom overlay: tools + button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = padding.calculateBottomPadding() + 104.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 4 tool cards — each card wrapped in a weight Box so scale overflow
                // stays within the allocated slot (no cross-card overlap)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                onClick = { onNavigate(tool.route) },
                                modifier = Modifier.fillMaxWidth(0.86f),
                            )
                        }
                    }
                }

                // Login-style gradient button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF3DAC78), Color(0xFF073B3A))))
                        .clickable { onNavigate(Routes.DASHBOARD) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "GO TO DASHBOARD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.White,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "card_scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.6f else 0f,
        animationSpec = tween(350),
        label = "glow_alpha",
    )

    val verticalSpacing by animateDpAsState(
        targetValue = if (isActive) 14.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "vertical_spacing",
    )

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Specific color Aura for active card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.81f)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        clip = false
                    }
                    .blur(20.dp, BlurredEdgeTreatment.Unbounded)
                    .background(tool.glowColor.copy(alpha = glowAlpha), RoundedCornerShape(12.dp))
            )

            // Padded container acting as border/gradient
            val containerBrush = if (isActive) {
                if (tool.route == Routes.NISHTHA) {
                    Brush.linearGradient(listOf(Color(0xFF073B3A), Color(0xFF052F2E)))
                } else {
                    Brush.linearGradient(listOf(tool.glowColor, tool.glowColor.copy(alpha = 0.7f)))
                }
            } else {
                Brush.linearGradient(listOf(tool.glowColor.copy(alpha = 0.35f), tool.glowColor.copy(alpha = 0.15f)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.81f)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        clip = false
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerBrush)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                val innerBorderColor = if (isActive) Color(0xFFBEF264).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                val innerBgColor = Color.White.copy(alpha = 0.1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(innerBgColor)
                        .border(1.dp, innerBorderColor, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = tool.imageUrl,
                        contentDescription = stringResource(tool.labelRes),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Text below the image
        Text(
            stringResource(tool.labelRes),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.White else Color(0xFFCBD5E1),
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(if (isActive) 1.05f else 1f)
        )
    }
}
