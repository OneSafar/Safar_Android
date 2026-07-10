package com.safarparmar.app.ui.studyplanner.screens
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
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerCalendarStatus
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.components.chapterHierarchyBrush
import com.safarparmar.app.ui.studyplanner.components.subjectHeaderBrush
import com.safarparmar.app.ui.studyplanner.components.subjectMeterBrush
import com.safarparmar.app.ui.studyplanner.components.topicHierarchyBrush
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
import com.safarparmar.app.ui.studyplanner.plan.UnscheduledTopicsScreen
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CalendarPlainHeader(plan = plan)
            }

            item {
                // Elevated/Border Card containing the entire calendar view
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Month navigation
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronLeft,
                                    contentDescription = "Previous month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Text(
                                text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, locale)} ${visibleMonth.year}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            )
                            
                            IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                                    contentDescription = "Next month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days of the week row
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid of day slots
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
                val revisionGradient = Brush.horizontalGradient(colors = listOf(Color(0xFF3E7C8C), Color(0xFF29638A)))
                Button(
                    onClick = {
                        actions.openRevisionTopics()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(revisionGradient, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.CheckCircle, 
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("View Revision Topics", fontWeight = FontWeight.Bold)
                }
            }

            item {
                val unscheduledGradient = Brush.horizontalGradient(colors = listOf(Color(0xFF991B1B), Color(0xFF7F1D1D)))
                Button(
                    onClick = { showUnscheduledTopicsScreen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(unscheduledGradient, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.List, 
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("View Missed Topics", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showUnscheduledTopicsScreen) {
            val refs = remember(plan.subjects) { plan.flattenTopics() }
            val unscheduledTopics = remember(refs, todayK) {
                refs.filter { ref ->
                    val date = ref.topic.plannedDate
                    ref.topic.status != TopicStatus.DONE && (date.isNullOrBlank() || date.take(10) < todayK)
                }
            }
            UnscheduledTopicsScreen(
                plan = plan,
                unscheduledTopics = unscheduledTopics,
                actions = actions,
                onDismiss = { showUnscheduledTopicsScreen = false }
            )
        }
    }
}

@Composable
private fun CalendarPlainHeader(plan: StudyPlan) {
    val examDays = daysUntil(plan.examDate)
    val examDate = readableDate(plan.examDate).takeUnless { it == "Not set" }.orEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "TARGET EXAM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.8.sp
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.title.ifBlank { "Your Study Journey" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (examDate.isNotEmpty()) {
                    Text(
                        text = examDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ExamDaysCountdownBadge(days = examDays)
        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(54.dp),
) {
    val todayK = todayKey()
    val dayNum = LocalDate.parse(dateIso).dayOfMonth.toString()
    
    val highlightColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    
    val dayTextColor = when {
        isToday -> Color.White
        isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onBackground
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
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else if (selected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
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
                            // Single-topic patch — moving one topic's date never rebuilds
                            // or redistributes the rest of the plan, and the server treats
                            // the daily goal as advisory, so this never blocks on capacity.
                            actions.updateTopic(target.topicId, plannedDate = newDate.toString())
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
    ) {
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
    val gradient = Brush.verticalGradient(
        colors = listOf(color.copy(alpha = 0.7f), color)
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showFullName = !showFullName },
        shape = RoundedCornerShape(12.dp),
        color = topicStatus.color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, topicStatus.color.copy(alpha = 0.25f))
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
