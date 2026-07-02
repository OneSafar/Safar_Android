package com.safarparmar.app.ui.dhyan

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.rememberFeatureTabBackStack
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.feature.live.presentation.LiveSessionsScreen
import com.safarparmar.app.util.YoutubeUrls
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign

enum class CoursesHubTab(val label: String) {
    COURSES("Courses"),
    LIVE_CLASSES("Live Classes"),
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

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        SafarDrawerScaffold(
            title = "Courses",
            subtitle = "SAFAR",
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        CoursesHubTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { tabBackStack.select(tab) },
                                text = { Text(tab.label) },
                            )
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            CoursesHubTab.COURSES -> CoursesTabContent()
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
                                            onUpgradeClick = { onNavigate(Routes.PREMIUM) }
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

@Composable
private fun CoursesTabContent() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(YoutubeUrls.SAFAR_CHANNEL_URL)))
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF4D4D), Color(0xFFE60000))
                            )
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
                    Text(
                        "Visit SAFAR on YouTube",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Meditation guidance, mindful practices, and new videos from SAFAR.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Visit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        // SAFAR 30-Day Meditation Course Card
        val courseUrl = "https://www.parmaracademy.in/courses/75-safar-30"
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(courseUrl)))
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.safar_3_0_meditation),
                        contentDescription = "SAFAR 3.0 Meditation",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "SAFAR 3.0 Meditation Course",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Every Morning Join Parmar Sir for refreshing Yoga sessions, Guided meditation and Mind Calming Practices.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primary.copy(0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Status",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Available",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Buy now →",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClassesPremiumLockOverlay(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.background
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.20f to bg.copy(alpha = 0.85f),
            0.40f to bg,
        )
    )
    Box(
        modifier = modifier
            .background(gradient)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpgradeClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.25f),
                                scheme.primary.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        ),
                        androidx.compose.foundation.shape.CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(scheme.primary.copy(alpha = 0.6f), scheme.secondary.copy(alpha = 0.3f))
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Safar Premium feature",
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = "Safar Premium Feature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onBackground,
            )
            Text(
                text = "Upgrade to unlock Live Classes, interactive meditation sessions, and real-time guidance.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
            Button(
                onClick = onUpgradeClick,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Upgrade to Safar Premium", fontWeight = FontWeight.Bold)
            }
        }
    }
}
