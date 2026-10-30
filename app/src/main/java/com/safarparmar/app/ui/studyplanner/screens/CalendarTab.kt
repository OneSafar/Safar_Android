package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.ui.platform.LocalLocale
import androidx.compose.material3.BottomSheetDefaults
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.safarparmar.app.ui.theme.Blue500
import com.safarparmar.app.ui.theme.Emerald500
import com.safarparmar.app.ui.theme.Orange500
import com.safarparmar.app.ui.theme.Rose500
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog as AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import com.safarparmar.app.ui.components.popups.SafarDatePickerDialog as DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import com.safarparmar.app.ui.components.popups.SafarDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.ui.components.popups.SafarModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.safarparmar.app.ui.tutorial.tutorialTarget
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import com.safarparmar.app.util.bounceClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.CalendarMap
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.domain.model.studyplanner.PlannerAnalytics
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.effortPoints
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
// ── Liquid Glass design system (Dhyan / Dashboard recipe) ────────────────────
import com.safarparmar.app.ui.studyplanner.components.flatCard
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.studyplanner.components.SafarEnableSheetBackdropBlur
import com.safarparmar.app.ui.studyplanner.components.glassSurface
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.PlannerCalendarStatus
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField

import com.safarparmar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
import com.safarparmar.app.ui.studyplanner.plan.MissedTopicsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@Composable
internal fun CalendarTab(plan: StudyPlan, state: StudyPlannerUiState, actions: PlannerActions) {
    val todayK = todayKey()
    var visibleMonth by rememberSaveable(stateSaver = Saver<YearMonth, String>(save = { it.toString() }, restore = { YearMonth.parse(it) })) { mutableStateOf(YearMonth.now()) }
    val locale = LocalLocale.current.platformLocale
    val monthSlots = remember(visibleMonth) { monthCalendarSlots(visibleMonth) }
    val weeks = remember(monthSlots) { monthSlots.chunked(7) }
    var sheetDay by rememberSaveable { mutableStateOf<String?>(null) }
    var showUnscheduledTopicsScreen by remember { mutableStateOf(false) }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    LaunchedEffect(state.pendingOpenMissedTopics) {
        if (state.pendingOpenMissedTopics) {
            showUnscheduledTopicsScreen = true
            actions.clearPendingOpenMissedTopics()
        }
    }

    sheetDay?.let { day ->
        SelectedDayLogSheet(
            dateIso = day,
            plan = plan,
            items = state.calendar[day].orEmpty(),
            actions = actions,
            onDismiss = { sheetDay = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {


        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarPlainHeader(plan = plan, isLight = isLight, modifier = Modifier.weight(1f))
                    TextButton(onClick = { visibleMonth = YearMonth.now(); sheetDay = todayK }) { Text(stringResource(R.string.planner_warm_today)) }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    PlannerMonthGrid(visibleMonth, weeks, locale, todayK, sheetDay, plan, isLight,
                        dayItems = { state.calendar[it].orEmpty() },
                        onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
                        onNext = { visibleMonth = visibleMonth.plusMonths(1) }, onDay = { sheetDay = it })
                }
            }

            item {
                // Flat magazine actions: hairline-separated rows with the colour
                // carried by the label, not a filled button.
                Spacer(Modifier.height(10.dp))
                PlanHairline()
                CalendarActionRow(
                    label = stringResource(R.string.planner_view_revision_topics),
                    accent = PlannerFlatColors.PrimaryAccent,
                    onClick = { actions.openRevisionTopics() },
                )
                PlanHairline(alpha = 0.6f)
                CalendarActionRow(
                    label = stringResource(R.string.planner_view_missed_topics),
                    accent = Color(0xFFDC2626),
                    onClick = { showUnscheduledTopicsScreen = true },
                )
                PlanHairline()
            }
        }

        if (showUnscheduledTopicsScreen) {
            val refs = remember(PlannerReferenceKey(plan.subjects)) { plan.flattenTopics() }
            val missedTopics = remember(PlannerReferenceKey(refs), todayK) {
                refs.filter { ref -> ref.topic.isMissed(todayK) }
            }
            MissedTopicsScreen(
                plan = plan,
                missedTopics = missedTopics,
                actions = actions,
                onDismiss = { showUnscheduledTopicsScreen = false }
            )
        }
    }
}

@Composable
internal fun CalendarPlainHeader(plan: StudyPlan, isLight: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.planner_warm_calendar), style = MaterialTheme.typography.headlineMedium, color = PlannerFlatColors.TextDark)
        Text(plan.title, style = MaterialTheme.typography.bodyMedium, color = PlannerFlatColors.TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

internal enum class CalendarDateStatus(val color: Color) {
    PLANNED(Blue500),
    DONE(Emerald500),
    OVERDUE(Rose500),
    REVISE(Orange500)
}

/** Resolves done/planned/overdue only — "off day" is rendered as its own independent
 *  glyph (see [CalendarDayChip]) so a topic scheduled on an off-day no longer silently
 *  hides the "off" indicator behind a status dot. */
internal fun calendarDateStatus(
    dateIso: String,
    items: List<CalendarTopicItem>,
    todayIso: String,
): CalendarDateStatus? {
    val statuses = items.map { calendarTopicStatus(it, dateIso, todayIso) }
    return when {
        statuses.isEmpty() -> null
        CalendarDateStatus.OVERDUE in statuses -> CalendarDateStatus.OVERDUE
        CalendarDateStatus.REVISE in statuses -> CalendarDateStatus.REVISE
        statuses.all { it == CalendarDateStatus.DONE } -> CalendarDateStatus.DONE
        else -> CalendarDateStatus.PLANNED
    }
}

@Composable
internal fun CalendarDayChip(
    dateIso: String,
    items: List<CalendarTopicItem>,
    selected: Boolean,
    isToday: Boolean,
    isOff: Boolean,
    isExamDay: Boolean = false,
    isWeekend: Boolean = false,
    dense: Boolean = false,
    isLight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(54.dp),
) {
    val scheme = MaterialTheme.colorScheme
    val status = calendarDateStatus(dateIso, items, todayKey())
    val statusColor = when (status) {
        CalendarDateStatus.DONE -> scheme.secondary
        CalendarDateStatus.OVERDUE -> scheme.error
        CalendarDateStatus.REVISE -> scheme.tertiary
        else -> scheme.primary
    }
    val day = LocalDate.parse(dateIso).dayOfMonth.toString()
    val workload = stringResource(R.string.planner_warm_due_load, items.size)
    val rest = stringResource(R.string.planner_warm_rest)
    val exam = stringResource(R.string.planner_exam_day)
    val label = listOfNotNull(readableDate(dateIso), workload, rest.takeIf { isOff }, exam.takeIf { isExamDay }).joinToString(", ")
    Column(modifier.clip(RoundedCornerShape(14.dp))
        .background(if (isToday) scheme.primary else if (selected) scheme.primaryContainer else Color.Transparent)
        .then(if (selected && !isToday) Modifier.border(1.dp, scheme.primary, RoundedCornerShape(14.dp)) else Modifier)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label; this.selected = selected }
        .padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(day, fontSize = 16.sp, fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) scheme.onPrimary else scheme.onSurface)
            if (isOff || isExamDay) Text(if (isExamDay) "★" else "☾", fontSize = 11.sp,
                color = if (isToday) scheme.onPrimary else scheme.onSurfaceVariant)
        }
        if (items.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(4.dp).clip(CircleShape).background(if (isToday) scheme.onPrimary else statusColor))
            Text("${items.size}", fontSize = 11.sp, color = if (isToday) scheme.onPrimary else scheme.onSurfaceVariant)
        }

    }
}

private fun calendarTopicStatus(
    item: CalendarTopicItem,
    dateIso: String,
    todayIso: String,
): CalendarDateStatus = when {
    item.status == TopicStatus.REVISION_NEEDED -> CalendarDateStatus.REVISE
    dateIso < todayIso && item.status != TopicStatus.DONE -> CalendarDateStatus.OVERDUE
    item.status == TopicStatus.DONE -> CalendarDateStatus.DONE
    else -> CalendarDateStatus.PLANNED
}

private fun calendarRevisionTypeLabel(item: CalendarTopicItem): String? {
    if (item.status != TopicStatus.REVISION_NEEDED) return null
    val count = item.revisionReminderDates.orEmpty().size
    val type = item.revisionScheduleType?.lowercase()
    return when {
        type == "spaced" -> "Spaced revision • $count session${if (count == 1) "" else "s"}"
        type == "custom" -> "Custom revision • one time"
        count > 1 -> "Spaced revision • $count sessions"
        count == 1 -> "Custom revision • one time"
        else -> "Revision scheduled"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SelectedDayLogSheet(
    dateIso: String,
    plan: StudyPlan,
    items: List<CalendarTopicItem>,
    actions: PlannerActions,
    onDismiss: () -> Unit,
) {
    val todayK = todayKey()
    val planned = items.count { calendarTopicStatus(it, dateIso, todayK) == CalendarDateStatus.PLANNED }
    val done = items.count { calendarTopicStatus(it, dateIso, todayK) == CalendarDateStatus.DONE }
    val missed = items.count { calendarTopicStatus(it, dateIso, todayK) == CalendarDateStatus.OVERDUE }
    val revisedCount = items.count { calendarTopicStatus(it, dateIso, todayK) == CalendarDateStatus.REVISE }
    var changeDateTarget by remember { mutableStateOf<CalendarTopicItem?>(null) }

    changeDateTarget?.let { target ->
        val initialMillis = remember(target.topicId) {
            (parsePlannerDate(dateIso) ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { changeDateTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (target.status == TopicStatus.REVISION_NEEDED &&
                                target.revisionReminderDates.orEmpty().any { it.take(10) == dateIso }
                            ) {
                                // Revision cards come from revisionReminderDates. Changing
                                // plannedDate alone leaves the visible revision on its old day.
                                actions.changeRevisionDate(
                                    topicId = target.topicId,
                                    oldDate = dateIso,
                                    newDate = newDate.toString(),
                                )
                            } else {
                                // Moving one normal topic pins only that topic. It does not
                                // rebuild or redistribute the weighted study plan.
                                actions.updateTopic(target.topicId, plannedDate = newDate.toString())
                            }
                        }
                        changeDateTarget = null
                    },
                ) { Text(stringResource(R.string.common_move)) }
            },
            dismissButton = {
                TextButton(onClick = { changeDateTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = PlannerFlatColors.BgCream,
    ) {
        SafarEnableSheetBackdropBlur()
        // Everything — header, badges, AND the topic rows — lives in one single
        // LazyColumn. Nesting a scrollable LazyColumn inside a Modifier.verticalScroll
        // Column (the old layout) creates two competing vertical-scroll containers;
        // Compose can't always tell whether a drag should scroll the inner list or
        // drag the sheet itself, so scrolling through topics could accidentally
        // swipe the whole sheet closed. A single flat LazyColumn removes the
        // ambiguity — there is only one scrollable, so the sheet's own
        // nested-scroll-to-dismiss only kicks in once that one list is at its top.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
                .heightIn(max = 480.dp)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val dateTitle = readableDate(dateIso).split(",").firstOrNull() ?: readableDate(dateIso)
                    Text(
                        "Your targets for $dateTitle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(readableDate(dateIso), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    // Day load in effort points vs the daily budget (goal × 2):
                    // "Light day" under budget, "Full day" at/over. Big topics
                    // count as more, so 2 big topics can already fill a 3-topic day.
                    run {
                        val pointsByTopicId = remember(PlannerReferenceKey(plan.subjects)) {
                            plan.flattenTopics().associate { it.topic.id to it.topic.effortPoints(it.chapter).toFloat() }
                        }
                        val pendingPoints = items
                            .filter { calendarTopicStatus(it, dateIso, todayK) != CalendarDateStatus.DONE }
                            .sumOf { (pointsByTopicId[it.topicId] ?: 2f).toDouble() }
                        val budget = (plan.dailyGoal ?: 3).coerceAtLeast(1) * 2.0
                        if (items.isNotEmpty()) {
                            val label = when {
                                pendingPoints <= 0.0 -> "All done for this day"
                                pendingPoints < budget -> "Light day — room for more"
                                else -> "Full day — big topics count as more"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayStatBox(value = planned, label = stringResource(R.string.planner_to_study), color = Blue500, icon = Icons.AutoMirrored.Filled.MenuBook, modifier = Modifier.weight(1f))
                        DayStatBox(value = done, label = stringResource(R.string.common_completed), color = Emerald500, icon = Icons.Rounded.CheckCircle, modifier = Modifier.weight(1f))
                        DayStatBox(value = revisedCount, label = stringResource(R.string.planner_to_revise), color = Orange500, icon = Icons.Default.TrackChanges, modifier = Modifier.weight(1f))
                        DayStatBox(value = missed, label = stringResource(R.string.planner_missed), color = Rose500, icon = Icons.Default.Cancel, modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider()
                    if (items.isEmpty()) {
                        Text(
                            "No topics planned for this day.",
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Compact one-line-per-topic rows: with a full daily goal (e.g. 15
            // topics/day) the old tall cards (name + subject line + a wrapping row
            // of action chips each) made this sheet scroll for several screens.
            // Status is shown as a read-only badge — marking done/revision happens
            // from the topic's own card, not here — and "Change date" replaces the
            // old swap-only flow with a real date picker that can move a topic to
            // any date directly.
            items(items, key = { it.topicId }) { item ->
                CompactDayTopicRow(
                    item = item,
                    dateIso = dateIso,
                    onChangeDate = { changeDateTarget = item },
                )
            }
        }
    }
}

@Composable
private fun DayStatBox(
    value: Int,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
            Text("$value", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CompactDayTopicRow(
    item: CalendarTopicItem,
    dateIso: String,
    onChangeDate: () -> Unit,
) {
    val todayK = todayKey()
    val topicStatus = calendarTopicStatus(item, dateIso, todayK)
    val revisionTypeLabel = calendarRevisionTypeLabel(item)
    val scheme = MaterialTheme.colorScheme
    
    var showFullName by remember { mutableStateOf(false) }

    // Pop-up layer → glass. Each row keeps its status colour as a light tint.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(shape = RoundedCornerShape(12.dp), tint = topicStatus.color)
            .clickable { showFullName = !showFullName },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(topicStatus.color),
            )
            Spacer(Modifier.width(12.dp))
            CalendarStatusBadge(topicStatus)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.topicName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = scheme.onSurface,
                    maxLines = if (showFullName) Int.MAX_VALUE else 1,
                    overflow = if (showFullName) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                Text(
                    "${item.subjectName} · ${item.chapterName}",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = if (showFullName) Int.MAX_VALUE else 1,
                    overflow = if (showFullName) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                if (revisionTypeLabel != null) {
                    Text(
                        revisionTypeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ChangeDatePill(onClick = onChangeDate)
        }
    }
}

/** Ring-with-dot for "to study"/"to revise" (still pending), a filled checkmark for
 *  done, a filled X for missed — promoted from a plain 10dp status dot to a legible
 *  circular badge. */
@Composable
private fun CalendarStatusBadge(status: CalendarDateStatus) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            CalendarDateStatus.DONE -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = status.color, modifier = Modifier.size(18.dp))
            CalendarDateStatus.OVERDUE -> Icon(Icons.Default.Cancel, contentDescription = null, tint = status.color, modifier = Modifier.size(18.dp))
            else -> Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(status.color))
        }
    }
}

/** "Change Date" action, styled as a light pill rather than plain text — a consistent
 *  blue affordance across every card regardless of that card's own status tint, so it
 *  always reads as the tappable action rather than more status color. */
@Composable
private fun ChangeDatePill(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Blue500.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Blue500.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.planner_change_date),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue500,
                ),
            )
        }
    }
}

/** A flat calendar action: accent label on the left, chevron on the right. */
@Composable
private fun CalendarActionRow(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PlannerFlatColors.BorderSoft,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The live calendar and tutorial share the same month controls and day cells. */
@Composable
internal fun PlannerMonthGrid(
    visibleMonth: java.time.YearMonth, weeks: List<List<LocalDate?>>, locale: java.util.Locale,
    todayK: String, sheetDay: String?, plan: StudyPlan, isLight: Boolean,
    dayItems: (String) -> List<CalendarTopicItem>, onPrevious: () -> Unit, onNext: () -> Unit,
    onDay: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().flatCard(RoundedCornerShape(24.dp)).padding(10.dp).tutorialTarget("planner.calendar"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onPrevious() }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.planner_previous_month),
                    tint = PlannerFlatColors.TextDark,
                    modifier = Modifier.size(28.dp),
                )
            }

            Text(
                text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, locale)} ${visibleMonth.year}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark,
                ),
            )

            IconButton(onClick = { onNext() }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.planner_next_month),
                    tint = PlannerFlatColors.TextDark,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            weeks.forEach { week ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    week.forEach { date ->
                        Box(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .padding(2.dp),
                        ) {
                            if (date != null) {
                                val dateIso = date.toString()
                                val dayItems = dayItems(dateIso)
                                CalendarDayChip(
                                    dateIso = dateIso,
                                    items = dayItems,
                                    selected = sheetDay == dateIso,
                                    isToday = dateIso == todayK,
                                    isOff = jsDayOfWeek(date) in plan.offDays.toSet(),
                                    isExamDay = dateIso == plan.examDate?.take(10),
                                    isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY,
                                    dense = true,
                                    isLight = isLight,
                                    onClick = { onDay(dateIso) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("● " + stringResource(R.string.planner_warm_planned), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("● " + stringResource(R.string.planner_warm_done), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("☾ " + stringResource(R.string.planner_warm_rest), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
