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
) {
    val scheme = MaterialTheme.colorScheme
    val accentColor = scheme.primary
    val isLight = !isDarkTheme

    // Over video: treat as dark canvas for readable white icons; otherwise follow theme.
    val glassAsLight = isLight && !isOnVideo
    val selectedColor = when {
        isOnVideo -> Color.White
        isDarkTheme -> Color(0xFFF2F2F5)
        else -> SafarGlassPalette.LightTextPrimary
    }
    val unselectedColor = when {
        isOnVideo -> Color.White.copy(alpha = 0.55f)
        isDarkTheme -> Color(0xFFCCCCD8).copy(alpha = 0.55f)
        else -> SafarGlassPalette.LightTextSecondary.copy(alpha = 0.75f)
    }
    val activeDiscBg = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = if (isOnVideo || isDarkTheme) 0.35f else 0.16f),
            accentColor.copy(alpha = if (isOnVideo || isDarkTheme) 0.18f else 0.08f),
        ),
    )
    val activeDiscBorderColor = accentColor.copy(
        alpha = if (isOnVideo || isDarkTheme) 0.40f else 0.22f,
    )

    val navShape = RoundedCornerShape(SafarGlassChromeRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safarFrostedPanel(
                    isLight = glassAsLight,
                    shape = navShape,
                    tintAlpha = if (isOnVideo) 0.14f else null,
                )
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EkagraNavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else unselectedColor,
                    animationSpec = tween(durationMillis = 220),
                    label = "navIconTint_${tab.name}",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else unselectedColor,
                    animationSpec = tween(durationMillis = 220),
                    label = "navTextColor_${tab.name}",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(SafarGlassChromeRadius))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    activeDiscBg
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Transparent),
                                    )
                                },
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 0.6.dp,
                                        color = activeDiscBorderColor,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

// ─── Duration tab ──────────────────────────────────────────────────────────────
