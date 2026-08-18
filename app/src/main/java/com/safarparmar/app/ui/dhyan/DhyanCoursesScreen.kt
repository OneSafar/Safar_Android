package com.safarparmar.app.ui.dhyan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.util.YoutubeUrls

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
) {
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        Box(Modifier.fillMaxSize()) {
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
                    CoursesTabContent(
                        isDarkTheme = isDarkTheme,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoursesTabContent(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val isLight = !isDarkTheme
    val onGlass = DhyanFlatColors.onGlassText(isLight)
    val mutedGlass = DhyanFlatColors.onGlassMuted(isLight)

    var coursesVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        coursesVisible = true
    }

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

        // Live learning belongs to Courses, but keeps its dedicated destination so
        // notifications, course filters, and session-detail deep links still work.
        StaggeredDhyanEntranceBox(index = 0, isVisible = coursesVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .coursesGlassPanel(isLight)
                    .clickable { onNavigate(Routes.LIVE_SESSIONS_ROOT) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(DhyanFlatColors.Primary, Color(0xFF6D28D9)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(27.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.nav_live_sessions),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = onGlass,
                    )
                    Text(
                        text = stringResource(R.string.courses_live_sessions_description),
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
                    Text(
                        text = stringResource(R.string.courses_live_sessions_open),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        // YouTube promo — macOS glass tile
        StaggeredDhyanEntranceBox(index = 1, isVisible = coursesVisible) {
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
        }

        // SAFAR 3.0 course — macOS glass tile
        val courseUrl = "https://www.parmaracademy.in/courses/75-safar-30"
        StaggeredDhyanEntranceBox(index = 2, isVisible = coursesVisible) {
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
}

@Composable
private fun StaggeredDhyanEntranceBox(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else (20 + index * 12).dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 320,
            delayMillis = index * 40,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "dhyanStaggeredOffset",
    )
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 280,
            delayMillis = index * 40,
        ),
        label = "dhyanStaggeredAlpha",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}
