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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.ekagra.EkagraViewModel
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.isGoalCompleted

sealed interface DateFilter {
    object All : DateFilter
    object Today : DateFilter
    data class Custom(val date: java.time.LocalDate) : DateFilter
}

private const val INITIAL_HISTORY_SESSION_COUNT = 10

/* Hallmark · pre-emit critique: P4 H5 E4 S4 R5 V4 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun FocusHistoryTab(
    modifier: Modifier,
    analytics: EkagraAnalyticsStats,
    selectedTheme: VisualTheme? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val allSessions = remember(analytics.focusSessions) {
        analytics.focusSessions.sortedByDescending { it.endedAt ?: it.startedAt }
    }

    // ViewModel for goal-linking from history
    val ekagraViewModel = hiltViewModel<EkagraViewModel>()
    val ekagraTags by ekagraViewModel.ekagraTags.collectAsStateWithLifecycle(initialValue = DEFAULT_EKAGRA_TAGS)
    val ekagraTagColors by ekagraViewModel.ekagraTagColors.collectAsStateWithLifecycle(initialValue = DEFAULT_TAG_COLORS)
    var activeTagFilter by remember { mutableStateOf<String?>(null) }
    val allGoals by ekagraViewModel.allGoals.collectAsStateWithLifecycle()
    val completedGoalIds = remember(allGoals) {
        allGoals.filter { it.isGoalCompleted() && it.completedViaFocus }.mapTo(mutableSetOf()) { it.id }
    }
    val todayKey = remember { IstDateUtils.todayKey() }
    val linkableGoals = remember(allGoals, todayKey) {
        allGoals.filter { goal ->
            goal.id.isNotBlank() && goal.title.isNotBlank()
                && !goal.completed
                && goal.source != "ekagra"
                && goal.status !in listOf("completed", "done")
                && goal.lifecycleStatus !in listOf("abandoned", "rolled_over", "completed")
                && !goal.nextInstanceCreated
        }
    }
    val todayGoals = remember(linkableGoals, todayKey) {
        linkableGoals.filter { goal ->
            val day = IstDateUtils.getDateKey(goal.scheduledDate)
                ?: IstDateUtils.getDateKey(goal.createdAt)
                ?: IstDateUtils.getDateKey(goal.startedAt)
            day == todayKey && goal.status !in listOf("missed", "expired") && goal.lifecycleStatus != "missed"
        }
    }

    // State for long-press goal linking from history
    var goalLinkingSession by remember { mutableStateOf<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession?>(null) }
    // State for session title renaming from history
    var editingSession by remember { mutableStateOf<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession?>(null) }

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

    val tagStats = remember(filteredSessions) {
        EkagraTagUtils.calculateTagStats(filteredSessions)
    }

    val tagStatsMap = remember(tagStats) {
        tagStats.associateBy { it.tag.lowercase() }
    }

    val allAvailableTags = remember(ekagraTags, tagStats) {
        val list = ekagraTags.toMutableList()
        tagStats.forEach { st ->
            if (list.none { it.equals(st.tag, ignoreCase = true) }) {
                list.add(st.tag)
            }
        }
        list
    }

    val untaggedCount = remember(filteredSessions) {
        filteredSessions.count { session ->
            !session.isGoalLinked && session.associatedGoalId.isNullOrBlank() && EkagraTagUtils.parseTagAndTask(session.taskText).first.isNullOrBlank()
        }
    }

    val displayedSessions = remember(filteredSessions, activeTagFilter) {
        when {
            activeTagFilter == null -> filteredSessions
            activeTagFilter == "__UNTAGGED__" -> {
                filteredSessions.filter { session ->
                    !session.isGoalLinked && session.associatedGoalId.isNullOrBlank() && EkagraTagUtils.parseTagAndTask(session.taskText).first.isNullOrBlank()
                }
            }
            else -> {
                filteredSessions.filter { session ->
                    if (session.isGoalLinked || !session.associatedGoalId.isNullOrBlank()) {
                        false
                    } else {
                        val (tag, _) = EkagraTagUtils.parseTagAndTask(session.taskText)
                        tag.equals(activeTagFilter, ignoreCase = true)
                    }
                }
            }
        }
    }
    var showAllSessions by remember { mutableStateOf(false) }
    LaunchedEffect(selectedSubTab, dateFilter, activeTagFilter) {
        showAllSessions = false
    }
    val visibleSessions = remember(displayedSessions, showAllSessions) {
        if (showAllSessions) displayedSessions else displayedSessions.take(INITIAL_HISTORY_SESSION_COUNT)
    }

    // A linked goal is another view of the same session, not a different save destination.
    val tabFocusSeconds = filteredSessions.sumOf(::exactElapsedSeconds)

    val ink = rememberEkagraInk(onCanvas = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
    ) {
        val saveError by ekagraViewModel.saveError.collectAsStateWithLifecycle()
        saveError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = ekagraViewModel::retryPendingSaves) { Text(stringResource(R.string.ekagra_retry_save)) }
        }

        EkagraEyebrow(stringResource(R.string.ekagra_history_title), ink.secondaryText)
        Spacer(Modifier.height(6.dp))
        EkagraDisplayTitle(
            if (selectedSubTab == 0) "Your focus sessions" else "Your stopwatch runs",
            ink.primaryText,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Saved focus, goal and stopwatch activity.",
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = ink.secondaryText,
        )
        Spacer(Modifier.height(14.dp))

        // Underlined text tabs instead of an M3 TabRow
        EkagraTextTabs(
            items = listOf(0, 1),
            selected = selectedSubTab,
            accent = tabAccentColor,
            ink = ink,
            label = { if (it == 0) "Ekagra" else "Stopwatch" },
            onSelect = { selectedSubTab = it },
        )
        Spacer(Modifier.height(14.dp))

        // Keep the overview together as one visual unit.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = scheme.surfaceVariant.copy(alpha = 0.52f),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatElapsedDuration(tabFocusSeconds),
                    fontFamily = EkagraSerif,
                    fontSize   = 26.sp,
                    color      = ink.primaryText,
                )
                Text(
                    if (selectedSubTab == 0) "Total focus time" else "Total time",
                    fontSize = 11.sp,
                    color    = ink.mutedText,
                )
            }
            Box(Modifier.width(1.dp).height(48.dp).background(ink.hairline))
            Column(Modifier.weight(0.72f).padding(start = 18.dp)) {
                Text(
                    "${filteredSessions.size}",
                    fontFamily = EkagraSerif,
                    fontSize   = 26.sp,
                    color      = tabAccentColor,
                )
                Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_sessions), fontSize = 11.sp, color = ink.mutedText)
            }
        }
        }
        Spacer(Modifier.height(16.dp))

        // Date filters
        Text(
            stringResource(R.string.ekagra_show_label),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = ink.mutedText,
        )
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EkagraPill(
                label    = stringResource(R.string.ekagra_all),
                selected = dateFilter == DateFilter.All,
                accent   = tabAccentColor,
                ink      = ink,
                onClick  = { dateFilter = DateFilter.All },
            )
            EkagraPill(
                label    = stringResource(R.string.ekagra_today),
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
                else -> stringResource(R.string.ekagra_localized_pick_date)
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

        if (filteredSessions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.ekagra_filter_by_tag),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = ink.mutedText,
                )
                if (activeTagFilter != null) {
                    Text(
                        stringResource(R.string.ekagra_clear),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = tabAccentColor,
                        modifier = Modifier.clickable { activeTagFilter = null }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EkagraPill(
                    label    = stringResource(R.string.ekagra_localized_all_count, filteredSessions.size),
                    selected = activeTagFilter == null,
                    accent   = tabAccentColor,
                    ink      = ink,
                    onClick  = { activeTagFilter = null },
                )
                allAvailableTags.forEach { tag ->
                    val stat = tagStatsMap[tag.lowercase()]
                    val isSelected = tag.equals(activeTagFilter, ignoreCase = true)
                    val label = if (stat != null && stat.sessionCount > 0) "$tag (${stat.sessionCount})" else tag
                    val tagColor = EkagraTagUtils.getTagColor(tag, ekagraTagColors)
                    EkagraPill(
                        label    = label,
                        selected = isSelected,
                        accent   = tagColor,
                        ink      = ink,
                        onClick  = {
                            activeTagFilter = if (isSelected) null else tag
                        },
                    )
                }
                if (untaggedCount > 0) {
                    EkagraPill(
                        label    = stringResource(R.string.ekagra_untagged_count, untaggedCount),
                        selected = activeTagFilter == "__UNTAGGED__",
                        accent   = Color(0xFFD97706),
                        ink      = ink,
                        onClick  = {
                            activeTagFilter = if (activeTagFilter == "__UNTAGGED__") null else "__UNTAGGED__"
                        },
                    )
                }
            }

            // Tag insights summary banner
            if (activeTagFilter != null) {
                Spacer(Modifier.height(10.dp))
                val isUntaggedSelected = activeTagFilter == "__UNTAGGED__"
                val bannerColor = if (isUntaggedSelected) Color(0xFFD97706) else EkagraTagUtils.getTagColor(activeTagFilter, ekagraTagColors)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isUntaggedSelected) Color(0xFFFEF3C7).copy(alpha = 0.5f) else bannerColor.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (isUntaggedSelected) Color(0xFFF59E0B).copy(alpha = 0.4f) else bannerColor.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (isUntaggedSelected) {
                            Text(
                                "Untagged Sessions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                "$untaggedCount sessions have no tag. Tap edit on any session to assign a tag.",
                                fontSize = 11.sp,
                                color = ink.mutedText
                            )
                        } else {
                            val activeStat = tagStatsMap[activeTagFilter!!.lowercase()]
                            val totalSecs = activeStat?.totalSeconds ?: 0L
                            val count = activeStat?.sessionCount ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "[$activeTagFilter] Overview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor
                                )
                                Text(
                                    "Total: ${formatElapsedDuration(totalSecs)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink.primaryText
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$count sessions completed",
                                fontSize = 11.sp,
                                color = ink.mutedText
                            )
                        }
                    }
                }
            }
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

        if (displayedSessions.isEmpty()) {
            // Sessions exist, but none match the selected filter
            EkagraEmptyNote(
                text = if (activeTagFilter != null) "No sessions found for tag \"$activeTagFilter\"." else androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_nothing_range),
                ink = ink
            )
            return@Column
        }

        HistorySection(
            sessions      = visibleSessions,
            completedGoalIds = completedGoalIds,
            emptyText     = if (selectedSubTab == 0) "No sessions found." else "No stopwatch sessions found.",
            accentColor   = tabAccentColor,
            ink           = ink,
            tagColors     = ekagraTagColors,
            onLongPress   = { session ->
                if (!session.isGoalLinked) goalLinkingSession = session
            },
            onEditSession = { session ->
                editingSession = session
            },
        )
        if (!showAllSessions && displayedSessions.size > visibleSessions.size) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showAllSessions = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tabAccentColor.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.ekagra_show_more),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tabAccentColor,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${displayedSessions.size - visibleSessions.size} remaining",
                    fontSize = 11.sp,
                    color = ink.mutedText,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Session title edit dialog
    val sessionToEdit = editingSession
    if (sessionToEdit != null) {
        RenameSessionDialog(
            initialTitle = sessionToEdit.taskText ?: "",
            availableTags = ekagraTags,
            tagColors = ekagraTagColors,
            onAddTag = { tag, colorHex -> ekagraViewModel.addTag(tag, colorHex) },
            onDeleteTag = { tag -> ekagraViewModel.removeTag(tag) },
            onSetTagColor = { tag, colorHex -> ekagraViewModel.setTagColor(tag, colorHex) },
            onDismiss = { editingSession = null },
            onConfirm = { newTitle ->
                ekagraViewModel.updateExistingSession(
                    sessionId = sessionToEdit.id,
                    taskTitle = newTitle,
                )
                editingSession = null
            },
        )
    }

    // Long-press androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_link_goal) sheet
    val sessionForLinking = goalLinkingSession
    if (sessionForLinking != null) {
        val actualSecs = exactElapsedSeconds(sessionForLinking).toInt().coerceAtLeast(0)
        PostSaveGoalLinkingSheet(
            savedSessionId       = sessionForLinking.id,
            savedDurationSeconds = actualSecs,
            todayGoals           = todayGoals,
            selectedTheme        = selectedTheme,
            isDarkTheme          = com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme.current == true,
            onDismiss = {
                goalLinkingSession = null
                ekagraViewModel.loadEkagraAnalytics()
            },
            onLinkGoal = { goal, markComplete ->
                ekagraViewModel.linkSavedSessionToGoal(sessionForLinking.id, goal, markComplete)
                goalLinkingSession = null
                ekagraViewModel.loadEkagraAnalytics()
            },
        )
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HistorySection(
    sessions: List<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession>,
    completedGoalIds: Set<String> = emptySet(),
    emptyText: String,
    accentColor: Color,
    ink: EkagraInk,
    tagColors: Map<String, String> = emptyMap(),
    /** Called when the user long-presses a session row. Null = not interactive. */
    onLongPress: ((com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) -> Unit)? = null,
    /** Called when the user taps the edit/rename button on a session row. */
    onEditSession: ((com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) -> Unit)? = null,
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
                modifier      = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp),
            )
            rows.forEach { session ->
                FocusSessionRow(
                    session       = session,
                    completedViaEkagra = session.associatedGoalId in completedGoalIds,
                    accentColor   = accentColor,
                    ink           = ink,
                    tagColors     = tagColors,
                    onLongPress   = if (onLongPress != null && !session.isGoalLinked)
                        { -> onLongPress(session) } else null,
                    onEditSession = if (onEditSession != null && !session.isGoalLinked)
                        { -> onEditSession(session) } else null,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FocusSessionRow(
    session: com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession,
    accentColor: Color,
    ink: EkagraInk,
    completedViaEkagra: Boolean = false,
    tagColors: Map<String, String> = emptyMap(),
    /** Null = not interactive. Saved history is read-only, so a row with no
     *  handler must not show a ripple that implies it can be opened. */
    onClick: (() -> Unit)? = null,
    /** Long-press opens the androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_link_goal) action for unlinked sessions. */
    onLongPress: (() -> Unit)? = null,
    /** Edit icon click triggers rename dialog. */
    onEditSession: (() -> Unit)? = null,
) {
    val isStopwatch = session.timerMode?.equals("stopwatch", ignoreCase = true) == true
    val elapsedSeconds = exactElapsedSeconds(session)
    // "Completed" means the session ran at least as long as it planned to. A
    // stopwatch has no plan, so it always reads as completed.
    val completed = isStopwatch || elapsedSeconds >= session.durationMinutes * 60L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    (onClick != null || onLongPress != null) && !session.pendingSync -> Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongPress?.invoke() },
                    )
                    else -> Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (completed) accentColor else ink.mutedText),
        )
        Column(Modifier.weight(1f)) {
            val (parsedTag, cleanTask) = remember(session.taskText) { EkagraTagUtils.parseTagAndTask(session.taskText) }
            val tagColor = remember(parsedTag, tagColors) {
                parsedTag?.let { EkagraTagUtils.getTagColor(it, tagColors) }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (parsedTag != null && tagColor != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, tagColor.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                            Text(
                                text = parsedTag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = tagColor,
                            )
                        }
                    }
                } else if (!session.isGoalLinked && onEditSession != null && !session.pendingSync) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { onEditSession() }
                    ) {
                        Text(
                            text = "+ Tag",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    (if (parsedTag != null) cleanTask?.ifBlank { null } ?: "Focus Session" else session.taskText) ?: "Unlabeled session",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = ink.primaryText,
                    maxLines   = 1,
                    modifier   = Modifier.weight(1f, fill = false),
                )
                if (onEditSession != null && !session.pendingSync) {
                    IconButton(
                        onClick = onEditSession,
                        modifier = Modifier.size(24.dp).padding(start = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_rename_session),
                            tint = ink.mutedText,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
            if (session.pendingSync) {
                Text(stringResource(R.string.ekagra_saved_waiting_sync), fontSize = 11.sp, color = ink.mutedText)
                val retryModel = hiltViewModel<EkagraViewModel>()
                TextButton(onClick = retryModel::retryPendingSaves) { Text(stringResource(R.string.ekagra_retry_sync)) }
            }
            if (session.isGoalLinked) {
                Text(
                    if (completedViaEkagra) "Goal completed via Ekagra" else "Linked to goal · Goal kept open",
                    fontSize = 11.sp,
                    color = accentColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
                    session.endReason == "interrupted" -> "Interrupted · Saved"
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
}

@Composable
private fun RenameSessionDialog(
    initialTitle: String,
    availableTags: List<String> = emptyList(),
    tagColors: Map<String, String> = emptyMap(),
    onAddTag: ((String, String?) -> Unit)? = null,
    onDeleteTag: ((String) -> Unit)? = null,
    onSetTagColor: ((String, String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parsed = remember(initialTitle) { EkagraTagUtils.parseTagAndTask(initialTitle) }
    var titleInput by remember {
        mutableStateOf(
            parsed.second?.let { if (it.startsWith("Untitled")) "" else it }
                ?: if (initialTitle.startsWith("Untitled")) "" else initialTitle
        )
    }
    var selectedTag by remember { mutableStateOf(parsed.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_rename_session), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_session_name)) },
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.ekagra_enter_session_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (availableTags.isNotEmpty() || onAddTag != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Tag (Optional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        EkagraTagSelector(
                            availableTags = availableTags,
                            selectedTag = selectedTag,
                            onSelectTag = { selectedTag = it },
                            tagColors = tagColors,
                            onAddTag = onAddTag,
                            onDeleteTag = onDeleteTag,
                            onSetTagColor = onSetTagColor,
                            isDark = false,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formatted = EkagraTagUtils.formatTaggedTask(selectedTag, titleInput.trim())
                    onConfirm(formatted)
                },
            ) {
                Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_cancel))
            }
        },
    )
}



// ─── Theme & sound dialogs / sheets ───────────────────────────────────────────
