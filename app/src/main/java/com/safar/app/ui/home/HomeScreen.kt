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
import androidx.compose.ui.platform.LocalContext
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
    ToolCard(R.string.module_ekagra, "https://safar.parmarssc.in/focus-timer.webp", Routes.EKAGRA, Sky600),
    ToolCard(R.string.module_nishtha, "https://safar.parmarssc.in/nishtha-silhouette.webp", Routes.NISHTHA, Amber400),
    ToolCard(R.string.module_mehfil, "https://safar.parmarssc.in/mehfil-silhouette.webp", Routes.MEHFIL, Green400),
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
                                0.0f to Color.Black.copy(alpha = 0.45f),
                                0.3f to Color.Black.copy(alpha = 0.15f),
                                0.6f to Color.Black.copy(alpha = 0.15f),
                                1.0f to Color.Black.copy(alpha = 0.80f),
                            )
                        )
                    )
                    // Glass card
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = padding.calculateTopPadding() + 12.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.38f),
                                            Color.White.copy(alpha = 0.22f),
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.5f),
                                            Color.White.copy(alpha = 0.1f),
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    stringResource(slide.titleRes).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.5.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    slide.headline,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 38.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    slide.body,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
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
                    .padding(bottom = padding.calculateBottomPadding() + 24.dp, top = 20.dp),
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
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryDark, GradientMidDark)))
                        .clickable { onNavigate(Routes.DASHBOARD) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "GO TO DASHBOARD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
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
        targetValue = if (isActive) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "card_scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.70f else 0f,
        animationSpec = tween(350),
        label = "glow_alpha",
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (isActive) 2.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "border_w",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // Glow halo — blurred colored layer behind the card
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    clip = false
                }
                .blur(26.dp, BlurredEdgeTreatment.Unbounded)
                .background(tool.glowColor.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
        )

        // Card (drawn on top of glow)
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    clip = false
                }
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = borderWidth.dp,
                    color = if (isActive) tool.glowColor else Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AsyncImage(
                model = tool.imageUrl,
                contentDescription = stringResource(tool.labelRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
            )
            Text(
                stringResource(tool.labelRes),
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
