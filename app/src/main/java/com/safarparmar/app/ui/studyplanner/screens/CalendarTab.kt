package com.safarparmar.app.ui.studyplanner.screens
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    val locale = Locale.getDefault()
    val monthSlots = remember(visibleMonth) { monthCalendarSlots(visibleMonth) }
    val weeks = remember(monthSlots) { monthSlots.chunked(7) }
    var sheetDay by remember { mutableStateOf<String?>(null) }
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
            item(key = "schedule_navigation") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Calendar") })
                    FilterChip(selected = false, onClick = { actions.openRevisionTopics() }, label = { Text("Revision") })
                }
            }
            item {
                CalendarPlainHeader(plan = plan, isLight = isLight)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous month",
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

                            IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next month",
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
                                                .aspectRatio(1f)
                                                .padding(2.dp),
                                        ) {
                                            if (date != null) {
                                                val dateIso = date.toString()
                                                val dayItems = state.calendar[dateIso].orEmpty()
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
                                                    onClick = { sheetDay = dateIso },
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Flat magazine actions: hairline-separated rows with the colour
                // carried by the label, not a filled button.
                Spacer(Modifier.height(10.dp))
                PlanHairline()
                CalendarActionRow(
                    label = "View revision topics",
                    accent = PlannerFlatColors.PrimaryAccent,
                    onClick = { actions.openRevisionTopics() },
                )
                PlanHairline(alpha = 0.6f)
                CalendarActionRow(
                    label = "View missed topics",
                    accent = Color(0xFFDC2626),
                    onClick = { showUnscheduledTopicsScreen = true },
                )
                PlanHairline()
            }
        }

        if (showUnscheduledTopicsScreen) {
            val refs = remember(plan.subjects) { plan.flattenTopics() }
            val missedTopics = remember(refs, todayK) {
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
private fun CalendarPlainHeader(plan: StudyPlan, isLight: Boolean = false, modifier: Modifier = Modifier) {
    val examDays = daysUntil(plan.examDate)
    val examDate = readableDate(plan.examDate).takeUnless { it == "Not set" }.orEmpty()

    // Flat magazine header: eyebrow, then the plan title with the countdown
    // numeral in serif, and the exam date as a quiet meta line. No card.
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        PlanEyebrow("Target exam")
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = plan.title.ifBlank { "Your Study Journey" },
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            if (examDays != null && examDays >= 0L) {
                Text(
                    text = "$examDays",
                    fontFamily = LoraFontFamily,
                    fontSize = 22.sp,
                    color = PlannerFlatColors.PrimaryAccent,
                    maxLines = 1,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (examDays == 1L) "day left" else "days left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        if (examDate.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = examDate,
                fontSize = 11.5.sp,
                color = PlannerFlatColors.TextMuted,
            )
        }
        Spacer(Modifier.height(18.dp))
        PlanHairline()
    }
}

private enum class CalendarDateStatus(val color: Color) {
    PLANNED(Blue500),
    DONE(Emerald500),
    OVERDUE(Rose500),
    REVISE(Orange500)
}

/** Resolves done/planned/overdue only — "off day" is rendered as its own independent
 *  glyph (see [CalendarDayChip]) so a topic scheduled on an off-day no longer silently
 *  hides the "off" indicator behind a status dot. */
private fun calendarDateStatus(
    dateIso: String,
    items: List<CalendarTopicItem>,
    todayIso: String,
): CalendarDateStatus? {
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val overdue = dateIso < todayIso && items.any { it.status != TopicStatus.DONE }
    return when {
        overdue -> CalendarDateStatus.OVERDUE
        planned > 0 && done == planned -> CalendarDateStatus.DONE
        planned > 0 -> CalendarDateStatus.PLANNED
        else -> null
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
    val todayK = todayKey()
    val dayNum = LocalDate.parse(dateIso).dayOfMonth.toString()
    
    val accentColor = PlannerFlatColors.PrimaryAccent
    
    val dayTextColor = when {
        isToday -> Color.White
        isWeekend -> PlannerFlatColors.TextMuted
        else -> PlannerFlatColors.TextDark
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isToday || selected) 42.dp else 34.dp)
                    .clip(CircleShape)
                    .then(
                        if (isToday) {
                            Modifier.background(accentColor)
                        } else if (selected) {
                            Modifier.border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayNum,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        color = if (isToday) Color.White else dayTextColor,
                    ),
                )
                if (isExamDay) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Exam day",
                        tint = PlannerAccent.Coral,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp),
                    )
                }
            }
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
                ) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { changeDateTarget = null }) { Text("Cancel") }
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
                        val pointsByTopicId = remember(plan.subjects) {
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
                        DayStatBox(value = planned, label = "To Study", color = Blue500, icon = Icons.AutoMirrored.Filled.MenuBook, modifier = Modifier.weight(1f))
                        DayStatBox(value = done, label = "Completed", color = Emerald500, icon = Icons.Rounded.CheckCircle, modifier = Modifier.weight(1f))
                        DayStatBox(value = revisedCount, label = "To Revise", color = Orange500, icon = Icons.Default.TrackChanges, modifier = Modifier.weight(1f))
                        DayStatBox(value = missed, label = "Missed", color = Rose500, icon = Icons.Default.Cancel, modifier = Modifier.weight(1f))
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
    // The day sheet is a pop-up, so it belongs to the glass layer: each tile
    // keeps its own status colour but renders as tinted glass rather than a
    // solid block. Alphas are raised because the tile carries white content.
    Column(
        modifier = modifier
            .glassSurface(
                shape = RoundedCornerShape(12.dp),
                tint = color,
                tintTopAlpha = 0.78f,
                tintBottomAlpha = 0.58f,
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 28.sp
            ),
        )
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                    fontSize = 14.sp,
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
                text = "Change Date",
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
