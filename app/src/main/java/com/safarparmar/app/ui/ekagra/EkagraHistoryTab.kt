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
import com.safarparmar.app.ui.glass.SafarGlassCard
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface DateFilter {
    object All : DateFilter
    object Today : DateFilter
    data class Custom(val date: java.time.LocalDate) : DateFilter
}

@Composable
internal fun FocusHistoryTab(
    modifier: Modifier,
    analytics: EkagraAnalyticsStats,
    onSessionClick: (com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val allSessions = remember(analytics.focusSessions) {
        analytics.focusSessions.sortedByDescending { it.endedAt ?: it.startedAt }
    }

    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Ekagra History, 1 = Stopwatch History

    val currentTabSessions = if (selectedSubTab == 0) {
        allSessions.filterNot { it.timerMode.equals("stopwatch", ignoreCase = true) }
    } else {
        allSessions.filter { it.timerMode.equals("stopwatch", ignoreCase = true) }
    }

    val tabAccentColor = if (selectedSubTab == 0) scheme.primary else scheme.secondary

    var dateFilter by remember { mutableStateOf<DateFilter>(DateFilter.All) }

    val filteredSessions = remember(currentTabSessions, dateFilter) {
        val zone = ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        when (val filter = dateFilter) {
            DateFilter.All -> currentTabSessions
            DateFilter.Today -> currentTabSessions.filter {
                parseInstantOrNull(it.endedAt ?: it.startedAt)?.atZone(zone)?.toLocalDate() == today
            }
            is DateFilter.Custom -> {
                currentTabSessions.filter {
                    parseInstantOrNull(it.endedAt ?: it.startedAt)?.atZone(zone)?.toLocalDate() == filter.date
                }
            }
        }
    }

    // Goal-linked sessions live in Goal History now, not here — Ekagra History
    // is only for untitled/free-focus sessions.
    val freeSessions = filteredSessions.filterNot { it.isGoalLinked }
    // Preserve seconds through aggregation so short stopwatch sessions contribute
    // to the total instead of disappearing in minute-level rounding.
    val tabFocusSeconds = freeSessions.sumOf(::exactElapsedSeconds)

    val ink = rememberEkagraInk(onCanvas = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp),
    ) {
        EkagraEyebrow("History", ink.secondaryText)
        Spacer(Modifier.height(6.dp))
        EkagraDisplayTitle(
            if (selectedSubTab == 0) "Your focus sessions" else "Your stopwatch runs",
            ink.primaryText,
        )
        Spacer(Modifier.height(18.dp))

        // Underlined text tabs instead of an M3 TabRow
        EkagraTextTabs(
            items = listOf(0, 1),
            selected = selectedSubTab,
            accent = tabAccentColor,
            ink = ink,
            label = { if (it == 0) "Ekagra" else "Stopwatch" },
            onSelect = { selectedSubTab = it },
        )
        Spacer(Modifier.height(20.dp))

        // Totals — serif numeral above a small caption, split by a hairline
        EkagraHairline(ink.hairline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                    Text(
                        formatElapsedDuration(tabFocusSeconds),
                        fontFamily = EkagraSerif,
                        fontSize   = 26.sp,
                        color      = ink.primaryText,
                    )
                }
                Text(
                    if (selectedSubTab == 0) "Total focus time" else "Total time",
                    fontSize = 11.sp,
                    color    = ink.mutedText,
                )
            }
            Column {
                Text(
                    "${freeSessions.size}",
                    fontFamily = EkagraSerif,
                    fontSize   = 26.sp,
                    color      = tabAccentColor,
                )
                Text("Sessions", fontSize = 11.sp, color = ink.mutedText)
            }
        }
        EkagraHairline(ink.hairline)
        Spacer(Modifier.height(16.dp))

        // Date filters — outline pills
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EkagraPill(
                label    = "All",
                selected = dateFilter == DateFilter.All,
                accent   = tabAccentColor,
                ink      = ink,
                onClick  = { dateFilter = DateFilter.All },
            )
            EkagraPill(
                label    = "Today",
                selected = dateFilter == DateFilter.Today,
                accent   = tabAccentColor,
                ink      = ink,
                onClick  = { dateFilter = DateFilter.Today },
            )

            val customLabel = when (val filter = dateFilter) {
                is DateFilter.Custom -> {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    filter.date.format(formatter)
                }
                else -> "Pick a date"
            }

            EkagraPill(
                label    = customLabel,
                selected = dateFilter is DateFilter.Custom,
                accent   = tabAccentColor,
                ink      = ink,
                onClick  = {
                    val calendar = Calendar.getInstance()
                    if (dateFilter is DateFilter.Custom) {
                        val d = (dateFilter as DateFilter.Custom).date
                        calendar.set(d.year, d.monthValue - 1, d.dayOfMonth)
                    }
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                            dateFilter = DateFilter.Custom(selectedDate)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (currentTabSessions.isEmpty()) {
            // No sessions at all in the database
            EkagraEmptyNote(
                text = if (selectedSubTab == 0) "No ekagra sessions yet." else "No stopwatch sessions yet.",
                ink  = ink,
            )
            return@Column
        }

        if (filteredSessions.isEmpty()) {
            // Sessions exist, but none match the selected filter
            EkagraEmptyNote(text = "Nothing in this range.", ink = ink)
            return@Column
        }

        val rows = if (selectedSubTab == 0) freeSessions else filteredSessions.filterNot { it.isGoalLinked }
        HistorySection(
            sessions    = rows,
            emptyText   = if (selectedSubTab == 0) "No sessions found." else "No stopwatch sessions found.",
            accentColor = tabAccentColor,
            ink         = ink,
            onSessionClick = onSessionClick,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Empty state as a line of quiet text — no card, no oversized icon. */
@Composable
private fun EkagraEmptyNote(text: String, ink: EkagraInk) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
        Text(text, fontSize = 13.sp, color = ink.mutedText)
    }
}

/**
 * Sessions as a quiet list grouped by day. No cards — a hairline under each row
 * and a single dot carrying the completed / ended-early signal.
 */
@Composable
internal fun HistorySection(
    sessions: List<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession>,
    emptyText: String,
    accentColor: Color,
    ink: EkagraInk,
    onSessionClick: (com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) -> Unit = {}
) {
    if (sessions.isEmpty()) {
        EkagraEmptyNote(text = emptyText, ink = ink)
        return
    }

    val zone = ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    // Group by calendar day, newest first; `sessions` already arrives sorted.
    val groups = remember(sessions) {
        sessions.groupBy { session ->
            parseInstantOrNull(session.endedAt ?: session.startedAt)?.atZone(zone)?.toLocalDate()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        groups.forEach { (date, rows) ->
            val heading = when (date) {
                null -> "Undated"
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(
                    java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
                )
            }
            Text(
                heading.uppercase(),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color         = ink.mutedText,
                modifier      = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            rows.forEach { session ->
                FocusSessionRow(
                    session     = session,
                    accentColor = accentColor,
                    ink         = ink,
                    onClick     = { onSessionClick(session) },
                )
                EkagraHairline(ink.hairline.copy(alpha = ink.hairline.alpha * 0.7f))
            }
        }
    }
}

@Composable
internal fun FocusSessionRow(
    session: com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession,
    accentColor: Color,
    ink: EkagraInk,
    onClick: () -> Unit = {}
) {
    val isStopwatch = session.timerMode?.equals("stopwatch", ignoreCase = true) == true
    val elapsedSeconds = exactElapsedSeconds(session)
    // "Completed" means the session ran at least as long as it planned to. A
    // stopwatch has no plan, so it always reads as completed.
    val completed = isStopwatch || elapsedSeconds >= session.durationMinutes * 60L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (completed) accentColor else ink.mutedText),
        )
        Column(Modifier.weight(1f)) {
            Text(
                session.taskText ?: "Unlabeled session",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = ink.primaryText,
                maxLines   = 1,
            )
            Text(
                formatDateTime(session.endedAt ?: session.startedAt),
                fontSize = 11.5.sp,
                color    = ink.mutedText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatElapsedDuration(elapsedSeconds),
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = ink.secondaryText,
            )
            Text(
                when {
                    isStopwatch -> "Stopwatch"
                    completed   -> "Completed"
                    else        -> "Ended early"
                },
                fontSize = 10.5.sp,
                color    = ink.mutedText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}



// ─── Theme & sound dialogs / sheets ───────────────────────────────────────────
