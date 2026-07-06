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
    val allSessions = analytics.focusSessions

    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Ekagra history, 1 = stopwatch history

    val currentTabSessions = if (selectedSubTab == 0) {
        allSessions.filter { it.timerMode != "stopwatch" }
    } else {
        allSessions.filter { it.timerMode == "stopwatch" }
    }

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

    val linkedSessions = filteredSessions.filter { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val freeSessions = filteredSessions.filterNot { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val tabFocusMins = filteredSessions.sumOf { it.actualMinutes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Switchable subtabs
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            contentColor = scheme.primary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Ekagra history", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("stopwatch history", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        }

        // Date filters row
        val context = LocalContext.current
        val zone = ZoneId.systemDefault()
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = dateFilter == DateFilter.All,
                onClick = { dateFilter = DateFilter.All },
                label = { Text("All") }
            )
            FilterChip(
                selected = dateFilter == DateFilter.Today,
                onClick = { dateFilter = DateFilter.Today },
                label = { Text("Today") }
            )

            
            val customLabel = when (val filter = dateFilter) {
                is DateFilter.Custom -> {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    filter.date.format(formatter)
                }
                else -> "Select Date"
            }
            
            FilterChip(
                selected = dateFilter is DateFilter.Custom,
                onClick = {
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
                label = { Text(customLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        if (currentTabSessions.isEmpty()) {
            // General empty state when there are absolutely no sessions in the database
            Card(
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                border    = BorderStroke(0.5.dp, scheme.outlineVariant),
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint     = scheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(40.dp))
                    Text(
                        if (selectedSubTab == 0) "No ekagra sessions found." else "No stopwatch sessions found.",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = scheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        if (filteredSessions.isEmpty()) {
            // Empty state when sessions exist, but none match the selected filter
            Card(
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                border    = BorderStroke(0.5.dp, scheme.outlineVariant),
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint     = scheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(40.dp))
                    Text(
                        "No sessions found",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = scheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        Column(Modifier.fillMaxWidth()) {
            Text(
                if (selectedSubTab == 0) "Ekagra history" else "Stopwatch history",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = scheme.onSurface
            )
            Text("All completed sessions", fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }

        // Total minutes card
        Card(
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
            border    = BorderStroke(0.5.dp, scheme.outlineVariant),
            modifier  = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                    Text(formatMinutes(tabFocusMins), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = scheme.primary)
                }
                Text("TOTAL FOCUS TIME",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color         = scheme.onSurfaceVariant)
            }
        }

        if (selectedSubTab == 0) {
            HistorySection(
                title      = "Sessions linked to a goal",
                sessions   = linkedSessions,
                emptyText  = "No linked sessions found.",
                onSessionClick = onSessionClick,
            )
            HistorySection(
                title      = "Sessions Not Linked",
                sessions   = freeSessions,
                emptyText  = "No unlinked sessions found.",
                onSessionClick = onSessionClick,
            )
        } else {
            HistorySection(
                title      = "Stopwatch linked to goal",
                sessions   = linkedSessions,
                emptyText  = "No linked stopwatch sessions found.",
                onSessionClick = onSessionClick,
            )
            HistorySection(
                title      = "Unlinked stopwatch sessions",
                sessions   = freeSessions,
                emptyText  = "No unlinked stopwatch sessions found.",
                onSessionClick = onSessionClick,
            )
        }
    }
}

@Composable
internal fun HistorySection(
    title: String,
    subtitle: String? = null,
    sessions: List<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession>,
    emptyText: String,
    onSessionClick: (com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        // Accent border on history sections using outlineVariant — subtle, not primary
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = scheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            if (sessions.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        // M3: surfaceContainer for inner empty states
                        .background(scheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Text(emptyText, fontSize = 13.sp, color = scheme.onSurfaceVariant)
                }
            } else {
                sessions.forEach { session ->
                    FocusSessionRow(session, onClick = { onSessionClick(session) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun FocusSessionRow(
    session: com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession,
    onClick: () -> Unit = {}
) {
    val scheme    = MaterialTheme.colorScheme
    val isLinked  = !session.associatedGoalId.isNullOrBlank() && session.associatedGoalId?.startsWith("named:") != true

    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        // M3 surfaceContainer — one elevation step above the parent surfaceContainerLow card
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(session.taskText ?: "Unlabeled task",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = scheme.onSurface,
                        maxLines   = 1,
                        modifier   = Modifier.weight(1f, fill = false))
                    if (isLinked) {
                        // M3 tertiaryContainer badge — complementary accent role
                        Text(
                            "Linked",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = scheme.onTertiaryContainer,
                            modifier   = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(scheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text("Planned ${session.durationMinutes}m · Actual ${session.actualMinutes}m",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            // Time — primary colour, consistent with ring and total card
            Text(
                formatDateTime(session.endedAt ?: session.startedAt),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = scheme.primary,
            )
        }
    }
}



// ─── Theme & sound dialogs / sheets ───────────────────────────────────────────
