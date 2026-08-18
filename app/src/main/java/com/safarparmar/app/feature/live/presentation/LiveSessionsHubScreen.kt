package com.safarparmar.app.feature.live.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.dhyan.DhyanFlatColors
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * Live Sessions destination opened from Courses. It remains a separate route so
 * notification deep links and session-detail navigation keep working.
 */
@Composable
fun LiveSessionsHubScreen(
    courseId: String = "",
    currentRoute: String = Routes.LIVE_SESSIONS_ROOT,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_live_sessions),
        subtitle = null,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = DhyanFlatColors.Bg,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LiveSessionsScreen(
                courseId = courseId,
                onBack = {},
                onOpenSession = { sessionId -> onNavigate(Routes.liveSession(sessionId)) },
                showTopBar = false,
            )
            if (!premiumStatus.isPremium) {
                LiveSessionsPremiumLockOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onUpgradeClick = { onNavigate(Routes.PREMIUM) },
                )
            }
        }
    }
}

@Composable
fun LiveSessionsPremiumLockOverlay(
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
                    Text(
                        "Upgrade to Safar Premium",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
