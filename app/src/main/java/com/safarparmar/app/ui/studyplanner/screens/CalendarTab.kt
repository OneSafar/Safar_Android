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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
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
import androidx.compose.ui.graphics.Brush
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
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.components.chapterHierarchyBrush
import com.safarparmar.app.ui.studyplanner.components.subjectDotColor
import com.safarparmar.app.ui.studyplanner.components.subjectHeaderBrush
import com.safarparmar.app.ui.studyplanner.components.subjectMeterBrush
import com.safarparmar.app.ui.studyplanner.components.topicHierarchyBrush
import com.safarparmar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.PlanCardSkeleton
import com.safarparmar.app.ui.components.SafarInlineRefreshIndicator
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanTabScreen
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

    sheetDay?.let { day ->
        SelectedDayLogSheet(
            dateIso = day,
            plan = plan,
            items = state.calendar[day].orEmpty(),
            actions = actions,
            onDismiss = { sheetDay = null },
        )
    }



    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CalendarPlainHeader(plan = plan)
        }

        item {
            // Month navigation with chevron icons
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, locale)} ${visibleMonth.year}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                )
                IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
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
        }

        item {
            // Grid of elevated day slots
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

        item {
            // Legend
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalendarLegendDot(CalendarDateStatus.PLANNED.color, "Planned")
                        CalendarLegendDot(CalendarDateStatus.DONE.color, "Done")
                        CalendarLegendDot(CalendarDateStatus.OVERDUE.color, "Overdue")
                        CalendarLegendDot(CalendarDateStatus.OFF.color, "Off")
                    }
                }
            }
        }


    }
}

@Composable
private fun CalendarPlainHeader(plan: StudyPlan) {
    val examDays = daysUntil(plan.examDate)
    val examDate = readableDate(plan.examDate).takeUnless { it == "Not set" }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = plan.title.ifBlank { "Calendar" },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val caption = plannerExamCountdownCaption(examDays)
        val subtitle = listOf(caption, examDate).filter { it.isNotBlank() }.joinToString("  •  ")
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class CalendarDateStatus(val color: Color) {
    PLANNED(Color(0xFF2563EB)),
    DONE(Color(0xFF7E57D9)),
    OVERDUE(Color(0xFFDC2626)),
    OFF(Color(0xFFF59E0B)),
}

private fun calendarDateStatus(
    dateIso: String,
    items: List<CalendarTopicItem>,
    isOff: Boolean,
    todayIso: String,
): CalendarDateStatus? {
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val overdue = dateIso < todayIso && items.any { it.status != TopicStatus.DONE }
    return when {
        overdue -> CalendarDateStatus.OVERDUE
        planned > 0 && done == planned -> CalendarDateStatus.DONE
        planned > 0 -> CalendarDateStatus.PLANNED
        isOff -> CalendarDateStatus.OFF
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
    dense: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(54.dp),
) {
    val todayK = todayKey()
    val dayNum = LocalDate.parse(dateIso).dayOfMonth.toString()
    val status = calendarDateStatus(dateIso, items, isOff, todayK)
    val highlightColor = when {
        isToday -> CalendarDateStatus.PLANNED.color
        selected -> status?.color ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val highlightAlpha = when {
        isToday -> 1f
        selected -> 0.22f
        else -> 0f
    }
    val dayTextColor = when {
        isToday -> Color.White
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
                    .size(if (isToday || selected) 46.dp else 38.dp)
                    .clip(CircleShape)
                    .background(highlightColor.copy(alpha = highlightAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayNum,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isToday || selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = dayTextColor,
                    ),
                )
            }
            Spacer(Modifier.height(3.dp))
            if (status != null) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(status.color)
                )
            } else {
                Spacer(Modifier.size(7.dp))
            }
        }
    }
}

@Composable
internal fun CalendarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun calendarTopicStatus(
    item: CalendarTopicItem,
    dateIso: String,
    todayIso: String,
): CalendarDateStatus = when {
    dateIso < todayIso && item.status != TopicStatus.DONE -> CalendarDateStatus.OVERDUE
    item.status == TopicStatus.DONE -> CalendarDateStatus.DONE
    else -> CalendarDateStatus.PLANNED
}

private fun calendarTopicStatusLabel(status: CalendarDateStatus): String = when (status) {
    CalendarDateStatus.PLANNED -> "Planned"
    CalendarDateStatus.DONE -> "Done"
    CalendarDateStatus.OVERDUE -> "Overdue"
    CalendarDateStatus.OFF -> "Off"
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
    val planned = items.size
    val done = items.count { it.status == TopicStatus.DONE }
    val missed = if (dateIso < todayK) items.count { it.status != TopicStatus.DONE } else 0
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Day Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(readableDate(dateIso), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayStatBox(value = planned, label = "Planned", modifier = Modifier.weight(1f))
                        DayStatBox(value = done, label = "Done", modifier = Modifier.weight(1f))
                        DayStatBox(value = missed, label = "Missed", modifier = Modifier.weight(1f))
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
private fun DayStatBox(value: Int, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChangeDate),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(subjectDotColor(item.subjectColor)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.topicName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${item.subjectName} · ${item.chapterName}",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (revisionTypeLabel != null) {
                    Text(
                        revisionTypeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = calendarTopicStatusLabel(topicStatus),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
