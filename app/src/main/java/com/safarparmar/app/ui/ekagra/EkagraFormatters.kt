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

internal fun formatMinutes(min: Int): String = when {
    min <= 0 -> "0m"
    min < 60 -> "${min}m"
    else     -> "${min / 60}h ${min % 60}m".let { if (it.endsWith(" 0m")) it.dropLast(3) else it }
}

internal fun parseInstantOrNull(iso: String?): Instant? =
    iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun isTodayIso(iso: String?): Boolean {
    val zone = ZoneId.systemDefault()
    return parseInstantOrNull(iso)?.atZone(zone)?.toLocalDate() == java.time.LocalDate.now(zone)
}

internal fun formatTime(iso: String?): String {
    val zone     = ZoneId.systemDefault()
    val dateTime = parseInstantOrNull(iso)?.atZone(zone) ?: return "-"
    return java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(dateTime)
}

internal fun formatDateTime(iso: String?): String {
    val zone     = ZoneId.systemDefault()
    val dateTime = parseInstantOrNull(iso)?.atZone(zone) ?: return "-"
    val now = java.time.LocalDate.now(zone)
    val formatter = if (dateTime.toLocalDate() == now) {
        java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    } else {
        java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
    }
    return formatter.format(dateTime)
}

internal fun TimerMode.toApiMode(): String = when (this) {
    TimerMode.FOCUS      -> "Timer"
    TimerMode.POMODORO   -> "Timer" // A pomodoro focus session is just a "Timer" session in the backend
    TimerMode.BREAK      -> "short"
    TimerMode.STOPWATCH  -> "stopwatch"
}
