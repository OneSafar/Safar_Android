package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.ui.adaptive.isSpaciousLandscape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.alpha
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.safarFrostedPanel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import com.safarparmar.app.ui.tutorial.tutorialTarget
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun EkagraBottomNav(
    selectedTab: EkagraNavTab,
    onSelect: (EkagraNavTab) -> Unit,
    isOnVideo: Boolean,
    isDarkTheme: Boolean,
    selectedTheme: VisualTheme? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val accentColor = scheme.primary
    val ink = rememberEkagraInk(onCanvas = isOnVideo, theme = selectedTheme, isDarkTheme = isDarkTheme)
    val haptic = LocalHapticFeedback.current

    val adaptive = com.safarparmar.app.ui.adaptive.rememberSafarAdaptiveState()
    val floating = adaptive.isSpaciousLandscape
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val dockWidth = if (floating) ekagraTabletChromeWidth(maxWidth) else maxWidth
    val dockShape = RoundedCornerShape(22.dp)

    // Tablet dock and status row use the same width; phones retain their hairline bar.
    Column(
        modifier = Modifier
            .width(dockWidth)
            .navigationBarsPadding()
            .padding(bottom = if (floating) 12.dp else 0.dp)
            .then(if (floating) Modifier.clip(dockShape)
                .background(scheme.surface.copy(alpha = 0.96f))
                .border(1.dp, scheme.outlineVariant, dockShape) else Modifier),
    ) {
        if (!floating) EkagraHairline(ink.hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (floating) 8.dp else 0.dp, end = if (floating) 8.dp else 0.dp,
                    top = if (floating) 8.dp else 12.dp, bottom = if (floating) 8.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val navItem: @Composable RowScope.(EkagraNavTab) -> Unit = { tab ->
                val isSelected = tab == selectedTab
                val tabLabel = stringResource(tab.labelRes)

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else scheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "navIcon_${tab.name}",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "navTextColor_${tab.name}",
                )

                com.composables.ui.components.Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect(tab) },
                    style = com.composables.ui.components.ButtonStyle.Ghost,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues.Zero,
                    modifier = Modifier.weight(1f)
                        .background(if (floating && isSelected) scheme.primaryContainer else Color.Transparent, RoundedCornerShape(16.dp))
                        .then(if (tab == EkagraNavTab.DURATION) Modifier.tutorialTarget("ekagra.settingsEntry") else Modifier),
                ) {
                Column(
                    Modifier.padding(vertical = if (floating) 8.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    EkagraChromeIcon(
                        imageVector = tab.icon,
                        contentDescription = tabLabel,
                        tint = iconColor,
                        baseSizeDp = 20f,
                    )

                    if (!floating) Box(
                        modifier = Modifier
                            .size(EkagraChrome.size(4f))
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.Transparent),
                    )

                    Text(
                        text = tabLabel,
                        fontSize = EkagraChrome.text(11f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                    )
                }
            }
            }
            EkagraNavTab.entries.take(3).forEach { navItem(it) }
            Row(Modifier.weight(2f).tutorialTarget("ekagra.personalizeEntry"), verticalAlignment = Alignment.CenterVertically) {
                EkagraNavTab.entries.drop(3).forEach { navItem(it) }
            }
        }
    }
    }
}

// ─── Duration tab ──────────────────────────────────────────────────────────────
