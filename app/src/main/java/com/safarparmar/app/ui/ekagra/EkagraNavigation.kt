package com.safarparmar.app.ui.ekagra

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
) {
    val scheme = MaterialTheme.colorScheme

    val isLight = scheme.background.luminance() > 0.5f
    val containerColor = if (isOnVideo) {
        if (isLight) Color(0xD9FFFFFF) else Color(0xCC0F1115)
    } else {
        scheme.surfaceContainer
    }

    NavigationBar(
        // M3 spec: NavigationBar container = surfaceContainer
        // When we're over the video scrim, use a translucent dark bar
        containerColor = containerColor,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                0.5.dp,
                if (isOnVideo) {
                    if (isLight) scheme.outlineVariant.copy(alpha = 0.5f) else scheme.outlineVariant.copy(alpha = 0.25f)
                } else {
                    scheme.outlineVariant
                },
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ),
    ) {
        EkagraNavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            NavigationBarItem(
                selected = isSelected,
                onClick  = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector     = tab.icon,
                        contentDescription = tab.label,
                        modifier        = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                },
                colors = NavigationBarItemDefaults.colors(
                    // M3 NavigationBarItem tokens
                    selectedIconColor   = scheme.onSecondaryContainer,
                    selectedTextColor   = scheme.primary,
                    indicatorColor      = scheme.secondaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                    unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            )
        }
    }
}

// ─── Duration tab ──────────────────────────────────────────────────────────────
