package com.safarparmar.app.ui.ekagra

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
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
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

    // ── A hairline, an icon, a dot ───────────────────────────────────────────
    // The redesign drops the floating frosted panel and the filled icon discs
    // in favour of a rule and plain per-tab icon + label, with one small accent
    // dot marking the active tab. The dot and icon both use the live theme
    // accent, so the bar still recolours per theme.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        EkagraHairline(ink.hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EkagraNavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else ink.secondaryText,
                    animationSpec = tween(durationMillis = 220),
                    label = "navIcon_${tab.name}",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) ink.primaryText else ink.secondaryText,
                    animationSpec = tween(durationMillis = 220),
                    label = "navTextColor_${tab.name}",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    EkagraChromeIcon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = iconColor,
                        baseSizeDp = 20f,
                    )

                    Box(
                        modifier = Modifier
                            .size(EkagraChrome.size(4f))
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.Transparent),
                    )

                    Text(
                        text = tab.label,
                        fontSize = EkagraChrome.text(11f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                    )
                }
            }
        }
    }
}

// ─── Duration tab ──────────────────────────────────────────────────────────────
