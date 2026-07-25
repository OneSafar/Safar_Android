package com.safarparmar.app.ui.dhyan

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.feature.live.presentation.LiveSessionsScreen
import com.safarparmar.app.ui.components.rememberFeatureTabBackStack
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.util.YoutubeUrls

enum class CoursesHubTab(val label: String) {
    COURSES("Courses"),
    LIVE_CLASSES("Live Classes"),
}

private fun Modifier.coursesGlassPanel(isLight: Boolean): Modifier {
    val body = DhyanFlatColors.glassBody(isLight)
    val borderBrush = if (isLight) {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    }
    val shape = RoundedCornerShape(20.dp)
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    return this
        .shadow(elevation = shadowElevation, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
        .clip(shape)
        .background(body)
        .border(width = 0.5.dp, brush = borderBrush, shape = shape)
}

@Composable
fun DhyanCoursesScreen(
    currentRoute: String = Routes.COURSES,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    initialTab: CoursesHubTab = CoursesHubTab.COURSES,
    liveCourseId: String = "",
    premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = hiltViewModel(),
) {
    val tabBackStack = rememberFeatureTabBackStack(
        initialTab = initialTab,
        rootTab = CoursesHubTab.COURSES,
    )
    val selectedTab = tabBackStack.currentTab
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()

    BackHandler(enabled = tabBackStack.hasHistory) {
        tabBackStack.goBack()
    }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
            SafarDrawerScaffold(
                title = "Courses",
                subtitle = null,
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = onNavigate,
                onToggleDarkTheme = onToggleDarkTheme,
                containerColor = DhyanFlatColors.Bg,
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                    Column(Modifier.fillMaxSize()) {
                        CoursesUnderlineTabs(
                            selected = selectedTab,
                            onSelect = { tabBackStack.select(it) },
                        )
                        PlanHairline()

                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedTab) {
                                CoursesHubTab.COURSES -> CoursesTabContent(isDarkTheme = isDarkTheme)
                                CoursesHubTab.LIVE_CLASSES -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        LiveSessionsScreen(
                                            courseId = liveCourseId,
                                            onBack = { tabBackStack.goBack() },
                                            onOpenSession = { sessionId -> onNavigate(Routes.liveSession(sessionId)) },
                                            showTopBar = false,
                                        )
                                        if (!premiumStatus.isPremium) {
                                            LiveClassesPremiumLockOverlay(
                                                modifier = Modifier.fillMaxSize(),
                                                onUpgradeClick = { onNavigate(Routes.PREMIUM) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoursesUnderlineTabs(
    selected: CoursesHubTab,
    onSelect: (CoursesHubTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        CoursesHubTab.entries.forEach { tab ->
            val isSelected = selected == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(tab) },
                    ),
            ) {
                Text(
                    text = tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) DhyanFlatColors.Text else DhyanFlatColors.Muted,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (isSelected) 48.dp else 0.dp)
                        .background(if (isSelected) DhyanFlatColors.Primary else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun CoursesTabContent(isDarkTheme: Boolean) {
    val context = LocalContext.current
    val isLight = !isDarkTheme
    val onGlass = DhyanFlatColors.onGlassText(isLight)
    val mutedGlass = DhyanFlatColors.onGlassMuted(isLight)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Learn & practice",
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = DhyanFlatColors.Text,
        )
        Spacer(Modifier.height(4.dp))

        // YouTube promo — macOS glass tile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .coursesGlassPanel(isLight)
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.SAFAR_CHANNEL_URL)))
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFF4D4D), Color(0xFFE60000))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Visit SAFAR on YouTube", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onGlass)
                Text(
                    "Meditation guidance, mindful practices, and new videos from SAFAR.",
                    fontSize = 12.sp,
                    color = mutedGlass,
                    lineHeight = 17.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DhyanFlatColors.Primary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Visit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // SAFAR 3.0 course — macOS glass tile
        val courseUrl = "https://www.parmaracademy.in/courses/75-safar-30"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .coursesGlassPanel(isLight)
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(courseUrl)))
                    }
                }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.safar_3_0_meditation),
                    contentDescription = "SAFAR 3.0 Meditation",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    text = "SAFAR 3.0 Meditation Course",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = onGlass,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "Every Morning Join Parmar Sir for refreshing Yoga sessions, Guided meditation and Mind Calming Practices.",
                fontSize = 13.sp,
                color = mutedGlass,
                lineHeight = 19.sp,
            )
            PlanHairline(alpha = 0.55f)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Available",
                    fontSize = 12.sp,
                    color = DhyanFlatColors.Primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Buy now →",
                    fontSize = 12.sp,
                    color = DhyanFlatColors.Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LiveClassesPremiumLockOverlay(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(DhyanFlatColors.Bg.copy(alpha = 0.94f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpgradeClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DhyanFlatColors.PrimarySoft)
                    .border(1.dp, DhyanFlatColors.Primary.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Safar Premium feature",
                    tint = DhyanFlatColors.Primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            PlanEyebrow("Dhyan")
            Text(
                text = "Safar Premium Feature",
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = DhyanFlatColors.Text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Upgrade to unlock Live Classes, interactive meditation sessions, and real-time guidance.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = DhyanFlatColors.Muted,
                lineHeight = 20.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DhyanFlatColors.Primary)
                    .clickable(onClick = onUpgradeClick),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Upgrade to Safar Premium", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
