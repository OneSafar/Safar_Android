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

/**
 * Named, in-app-official colors for the Ekagra visual themes — real color names
 * (not made-up labels), each used as [VisualTheme.accent] which drives the whole
 * screen's dynamic M3 color scheme (see `themeColorScheme` in EkagraScreen.kt) and,
 * for gradient themes, the background gradient itself. There used to be a second,
 * separate `bg` color per theme that only ever painted the theme picker's preview
 * swatch and nothing else on the real screen — so the swatch could show a color
 * the user would never actually see applied. That field is gone; the picker now
 * reads the same named color the rest of the screen uses.
 */
object EkagraThemeColor {
    val Cerulean = Color(0xFF1b8ec3)
    val KellyGreen = Color(0xFF1cbc31)
    val ForestGreen = Color(0xFF2e7144)
    val Violet = Color(0xFF7c3aed)
    val Periwinkle = Color(0xFFAAC7FF)
    val Navy = Color(0xFF0A305F)
    val SageGreen = Color(0xFFA9D0B3)
    val HunterGreen = Color(0xFF143723)
    val Peach = Color(0xFFFFB5A0)
    val Mahogany = Color(0xFF561F0F)
    val Thistle = Color(0xFFDDBCE0)
    val Eggplant = Color(0xFF3F2844)
}

data class VisualTheme(
    val name: String,
    val emoji: String,
    val accent: Color,
    val videoUrl: String = "",
    val musicUrl: String = "",
    val gradientColors: List<Color>? = null,
)

val visualThemes = listOf(
    VisualTheme("Serene",    "🌊", EkagraThemeColor.Cerulean,
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_2.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/music_1.mp3"),
    VisualTheme("Nostalgia", "🌿", EkagraThemeColor.KellyGreen,
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_3.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/relaxingtime-sleep-music-vol16-195422.mp3"),
    VisualTheme("Amber",     "🍂", EkagraThemeColor.ForestGreen,
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_4.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/WhatsApp_Audio_2026-02-18_at_10.05.04_AM.mpeg"),
    VisualTheme("Solitude",  "🌙", EkagraThemeColor.Violet,
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_1.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/music_3.mp3"),
    VisualTheme("Focus", "🚀", EkagraThemeColor.Navy,
        gradientColors = listOf(EkagraThemeColor.Periwinkle, EkagraThemeColor.Navy)),
    VisualTheme("Habits", "🌿", EkagraThemeColor.HunterGreen,
        gradientColors = listOf(EkagraThemeColor.SageGreen, EkagraThemeColor.HunterGreen)),
    VisualTheme("Journal", "📝", EkagraThemeColor.Mahogany,
        gradientColors = listOf(EkagraThemeColor.Peach, EkagraThemeColor.Mahogany)),
    VisualTheme("Peace", "🧘", EkagraThemeColor.Eggplant,
        gradientColors = listOf(EkagraThemeColor.Thistle, EkagraThemeColor.Eggplant)),
)

// Removed focusMusicTracks in favor of shared AudioLibrary

// ─── Nav tab & Timer mode enums ────────────────────────────────────────────────

internal enum class EkagraNavTab(val icon: ImageVector, val label: String) {
    TIMER    (Icons.Default.Timer,   "Ekagra"),
    DURATION (Icons.Default.Tune,    "Settings"),
    HISTORY  (Icons.Default.History, "History"),
    MUSIC    (Icons.Default.MusicNote, "Music"),
}

enum class TimerMode(
    @DrawableRes val lightIconRes: Int,
    @DrawableRes val darkIconRes: Int,
    val label: String,
    val showInPill: Boolean = true,
) {
    FOCUS(R.drawable.ic_hourglass_light, R.drawable.ic_hourglass_dark, "Ekagra"),
    BREAK(R.drawable.ic_ekagra_coffee_light, R.drawable.ic_ekagra_coffee_dark, "Break"),
    STOPWATCH(R.drawable.ic_ekagra_timer_light, R.drawable.ic_ekagra_timer_dark, "Stopwatch"),
    POMODORO(R.drawable.ic_ekagra_timer_light, R.drawable.ic_ekagra_timer_dark, "Pomodoro", false)
}

// ─── Root screen ───────────────────────────────────────────────────────────────
