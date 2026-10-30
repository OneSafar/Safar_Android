package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.ui.components.Tab
import com.composeunstyled.UnstyledTabList
import androidx.compose.foundation.gestures.Orientation
import com.composables.ui.components.Tabs
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.timetable.*
import com.safarparmar.app.feature.habits.viewmodel.*
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

internal val TimetableGreen = Color(0xFF36574A)
internal val SessionPastels = listOf(Color(0xFFEDF1DB), Color(0xFFFBE6DF), Color(0xFFE7EFF6), Color(0xFFFFEED2), Color(0xFFF0E8F4))
internal val SessionAccents = listOf(Color(0xFF5D965F), Color(0xFFC9817B), Color(0xFF669AC1), Color(0xFFD79D3F), Color(0xFFA588B8))

@Composable
internal fun HabitTimetableContent(
    state: HabitUiState,
    habitIcons: Map<String, String>,
    habitViewModel: HabitViewModel,
    onAddHabit: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
    onBack: () -> Unit = {},
    onMore: () -> Unit,
    snackbar: SnackbarHostState,
    timetableViewModel: TimetableViewModel = hiltViewModel()
) {
    val sessions by timetableViewModel.sessions.collectAsStateWithLifecycle()
    val owner by timetableViewModel.owner.collectAsStateWithLifecycle()
    // One main checklist; other tools open with a back button, never nested tab strips.
    var section by rememberSaveable { mutableIntStateOf(0) }
    var showAllHabits by rememberSaveable { mutableStateOf(false) }
    var historyParent by rememberSaveable { mutableIntStateOf(0) }
    var habitPage by rememberSaveable { mutableIntStateOf(0) }
    var timetableMode by rememberSaveable { mutableIntStateOf(0) }
    var historyMenu by remember { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val day = LocalDate.parse(selectedDay)
    val displayedHabits by remember(day) { habitViewModel.habitsForDate(day) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val todaySessions = sessions.filter { it.occursOn(day) }.sortedBy { it.startMinute }
    val scope = rememberCoroutineScope()
    LaunchedEffect(timetableViewModel) { timetableViewModel.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(owner) { creating = false; editorId = null }
    LaunchedEffect(state.selectedDate) { selectedDay = state.selectedDate.toString() }
    androidx.activity.compose.BackHandler(enabled = section != 0 && !creating && editorId == null) { section = if (section in listOf(2, 3, 7)) historyParent else 0 }

    if (creating || editorId != null) {
        val existing = sessions.find { it.id == editorId }
        if (creating || existing != null) {
            TimetableSessionEditor(
                initial = existing ?: TimetableSession(date = selectedDay),
                isNew = creating,
                subjects = sessions.map { it.subject }.distinct(),
                onDismiss = { creating = false; editorId = null },
                onSave = { timetableViewModel.save(it); creating = false; editorId = null },
                onDelete = { timetableViewModel.delete(it); editorId = null }
            )
            return
        }
    }
    Column(Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (section == 0) onMore() else section = if (section in listOf(2, 3, 7)) historyParent else 0 },
                modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon)
            ) {
                if (section == 0) Icon(Icons.Rounded.Menu, "Open menu", tint = HabitColors.TextPrimary)
                else Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to Habit Tracker", tint = HabitColors.TextPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(when (section) { 1 -> "Timetable"; 2 -> "Weekly history"; 3 -> "Monthly history"; 4 -> "Habit insights"; 5 -> "All habits"; 7 -> "Daily history"; 6 -> "Habits"; else -> "Habit Tracker" },
                    fontSize = 23.sp, fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary)

            }
            if (section == 6) FilledIconButton(
                onClick = onAddHabit,
                modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)
            ) {
                Icon(Icons.Rounded.Add, "Add habit")
            }
            if (section == 0) Box {
                IconButton(
                    onClick = { historyMenu = true },
                    modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon)
                ) { Icon(Icons.Rounded.MoreVert, "Habit options", tint = HabitColors.TextPrimary) }
                DropdownMenu(expanded = historyMenu, onDismissRequest = { historyMenu = false }, containerColor = HabitColors.Surface) {
                    listOf(5 to "All habits", 2 to "Weekly history", 3 to "Monthly history", 4 to "Insights").forEach { (destination, label) ->
                        DropdownMenuItem(text = { Text(label, color = HabitColors.TextPrimary) }, onClick = { historyParent = 0; section = destination; historyMenu = false })
                    }
                }
            }
        }
        if (section == 0) ReferenceTabs(listOf("Overview", "Timetable", "Habits"), 0, { section = if (it == 2) 6 else it }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (section == 1) ReferenceTabs(listOf("Day", "Week", "Subjects"), timetableMode, { timetableMode = it }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (section == 6) ReferenceTabs(listOf("Habits", "Insights"), habitPage, { habitPage = it }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (section == 5) Button(onClick = onAddHabit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 48.dp).habitPress(scaleDown = HabitMotion.ScaleButton),
            colors = ButtonDefaults.buttonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Habit")
        }
        if (section in 2..4 || (section == 6 && habitPage == 1)) {
            Box(Modifier.weight(1f)) {
                when (section) {
                    2 -> WeeklyView(weekStart = state.weekStart, habits = state.weeklyHabits, onToggleDay = habitViewModel::toggleHabit,
                        onPrevWeek = { habitViewModel.changeWeek(false) }, onNextWeek = { habitViewModel.changeWeek(true) },
                        onEditHabit = onEditHabit, today = state.selectedDate, onCurrent = habitViewModel::currentWeek, onAdd = onAddHabit)
                    3 -> MonthlyView(monthAnchor = state.monthAnchor, habits = state.monthlyHabits, onToggleDay = habitViewModel::toggleHabit,
                        onPrevMonth = { habitViewModel.changeMonth(false) }, onNextMonth = { habitViewModel.changeMonth(true) },
                        onEditHabit = onEditHabit, today = state.selectedDate, onCurrent = habitViewModel::currentMonth, onAdd = onAddHabit)
                    else -> com.safarparmar.app.feature.habits.ui.insights.HabitInsightsContent(onShowSnackbar = { scope.launch { snackbar.showSnackbar(it) } })
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (section != 1) state.syncError?.let { message -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), color = HabitColors.TextSecondary, fontSize = 12.sp)
                        TextButton(onClick = { scope.launch { habitViewModel.refreshBackend() } }) { Text("Retry", color = HabitColors.RoyalPurple) }
                    }
                } }
                if (section == 0) {
                    item {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (LocalHabitDarkTheme.current) HabitColors.Parchment else Color(0xFFEDEDDf)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            SproutArtwork(Modifier.size(72.dp), withSoil = true)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Stay on Track", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = HabitColors.TextPrimary)
                                Text("Small habits. A balanced day.", fontSize = 13.sp, color = HabitColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    item { WeekStrip(day) { selectedDay = it.toString() } }
                    item { ProgressSummary(state.copy(todayHabits = displayedHabits, selectedDate = day), todaySessions, day) }
                    item { SectionHeading("Daily Habits", "+ Add Habit", onAddHabit) }
                    if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = HabitColors.RoyalPurple) }
                    if (displayedHabits.isNotEmpty()) item {
                        HabitContentPanel {
                            displayedHabits.take(3).forEachIndexed { index, habit ->
                                key(habit.habit.id) {
                                    DailyHabitRow(habit.habit, habitIcons[habit.habit.remoteId] ?: "🌱", habit.completed,
                                        { onEditHabit(habit.habit) }, { habitViewModel.toggleHabit(habit.habit.id, day, habit.completed) })
                                    if (index < minOf(displayedHabits.size, 3) - 1) HorizontalDivider(color = HabitColors.Outline.copy(alpha = .4f), modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                        }
                    }
                    if (displayedHabits.size > 3) item { TextButton(onClick = { section = 6 }, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleButton)) { Text("View all ${displayedHabits.size} habits", color = HabitColors.RoyalPurple) } }
                    if (displayedHabits.isEmpty() && !state.isLoading) item { Text("Add a habit to begin your routine.", color = HabitColors.TextSecondary) }
                    item { SectionHeading("Today’s Schedule", "View All") { section = 1 } }
                    if (todaySessions.isEmpty()) item { EmptyTimetable("Plan your day", "Give study, breaks, and routines a time in your day.", "Set Up Timetable") { section = 1; creating = true } }
                    if (todaySessions.isNotEmpty()) item {
                        HabitContentPanel {
                            todaySessions.take(3).forEachIndexed { index, session ->
                                key(session.id) {
                                    SessionRow(session, day, false, { section = 1; editorId = session.id }, { timetableViewModel.toggle(session.id, day) })
                                    if (index < minOf(todaySessions.size, 3) - 1) HorizontalDivider(color = HabitColors.Outline.copy(alpha = .4f), modifier = Modifier.padding(start = 54.dp))
                                }
                            }
                        }
                    }
                } else if (section == 6) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily Habits", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HabitColors.TextPrimary)
                            Text("${state.todayHabits.count { it.completed }}/${state.todayHabits.size} completed", fontSize = 12.sp, color = HabitColors.TextSecondary)
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HabitColors.Surface).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            val visible = if (showAllHabits) state.todayHabits else state.todayHabits.take(6)
                            visible.forEachIndexed { index, habit ->
                                key(habit.habit.id) {
                                    DailyHabitRow(habit.habit, habitIcons[habit.habit.remoteId] ?: "🌱", habit.completed,
                                        { onEditHabit(habit.habit) }, { habitViewModel.toggleTodayHabit(habit.habit.id, state.selectedDate, habit.completed) })
                                    if (index < visible.lastIndex) HorizontalDivider(color = HabitColors.Outline.copy(alpha = .35f), modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                            if (state.todayHabits.isEmpty()) Text("No habits scheduled today.", color = HabitColors.TextSecondary, modifier = Modifier.padding(12.dp))
                            if (state.todayHabits.size > 6) TextButton(onClick = { showAllHabits = !showAllHabits }, modifier = Modifier.align(Alignment.CenterHorizontally).habitPress(scaleDown = HabitMotion.ScaleButton)) {
                                Text(if (showAllHabits) "Show less" else "Show more (${state.todayHabits.size - 6})", color = HabitColors.RoyalPurple)
                            }
                        }
                    }
                    item { Text("Habit History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HabitColors.TextPrimary, modifier = Modifier.padding(top = 16.dp)) }
                    items(listOf(7 to "Daily history", 2 to "Weekly history", 3 to "Monthly history")) { (destination, label) ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HabitColors.Surface).habitClickable(scaleDown = HabitMotion.ScaleCard) { historyParent = 6; section = destination }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = HabitColors.RoyalPurple)
                            Text(label, Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Medium, color = HabitColors.TextPrimary)
                            Icon(Icons.Rounded.ChevronRight, null, tint = HabitColors.TextSecondary)
                        }
                    }
                } else if (section == 7) {
                    item { WeekStrip(day) { selectedDay = it.toString() } }
                    items(displayedHabits, key = { it.habit.id }) { habit ->
                        DailyHabitRow(habit.habit, habitIcons[habit.habit.remoteId] ?: "🌱", habit.completed,
                            { onEditHabit(habit.habit) }, { habitViewModel.toggleHabit(habit.habit.id, day, habit.completed) })
                    }
                    if (displayedHabits.isEmpty()) item { Text("No habits scheduled for this day.", color = HabitColors.TextSecondary) }
                } else if (section == 1) {
                    item { WeekStrip(day) { selectedDay = it.toString() } }
                    if (timetableMode == 0) {
                        if (todaySessions.isEmpty()) item { Text("No timed activities for this day.", color = HabitColors.TextSecondary, modifier = Modifier.padding(vertical = 16.dp)) }
                        if (todaySessions.isNotEmpty()) item {
                            HabitContentPanel {
                                todaySessions.forEach { session -> key(session.id) { SessionRow(session, day, true, { editorId = session.id }, { timetableViewModel.toggle(session.id, day) }) } }
                            }
                        }
                    } else if (timetableMode == 1) {
                        val weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        (0L..6L).forEach { offset ->
                            val date = weekStart.plusDays(offset)
                            val activities = sessions.filter { it.occursOn(date) }.sortedBy { it.startMinute }
                            item { Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), fontWeight = FontWeight.SemiBold, color = HabitColors.TextPrimary) }
                            if (activities.isEmpty()) item { Text("No activities planned", fontSize = 12.sp, color = HabitColors.TextSecondary) }
                            if (activities.isNotEmpty()) item {
                                HabitContentPanel { activities.forEach { session -> key(session.id) { SessionRow(session, date, true, { editorId = session.id }, { timetableViewModel.toggle(session.id, date) }) } } }
                            }
                        }
                    } else {
                        sessions.groupBy { it.subject }.toSortedMap().forEach { (subject, activities) ->
                            item { Text(subject, fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary) }
                            item { HabitContentPanel { activities.forEach { session -> key(session.id) { SessionRow(session, day, false, { editorId = session.id }, null) } } } }
                        }
                        if (sessions.isEmpty()) item { Text("Add an activity to organize your subjects.", color = HabitColors.TextSecondary) }
                    }
                } else if (section == 5) {
                    items(state.weeklyHabits, key = { it.habit.id }) { habit ->
                        DailyHabitRow(habit.habit, habitIcons[habit.habit.remoteId] ?: "🌱", state.todayHabits.firstOrNull { it.habit.id == habit.habit.id }?.completed == true,
                            { onEditHabit(habit.habit) }, null)
                    }
                    if (state.weeklyHabits.isEmpty()) item { Text("Your saved habits will appear here.", color = HabitColors.TextSecondary) }
                }
            }
        }
        if (section == 1) Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 48.dp).habitPress(scaleDown = HabitMotion.ScaleButton),
            shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Timetable Activity")
        }
    }
}

/** Shared warm panel used by overview groups, timetable groups, and the editor. */
@Composable
internal fun HabitContentPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(HabitColors.Surface)
        .border(.75.dp, HabitColors.Outline.copy(alpha = .45f), RoundedCornerShape(20.dp))
        .padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

/** Adapted from the Composables UI weighted Tabs example, with reference pill styling and smooth micro-transitions. */
@Composable
internal fun ReferenceTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val accent = HabitColors.RoyalPurple
    val onAccent = HabitColors.OnRoyalPurple
    val ink = HabitColors.TextPrimary
    CompositionLocalProvider(LocalSafarComposablesAccent provides accent) {
        SafarComposablesTheme {
            Tabs(selectedTab = selected, onSelectedTabChange = onSelect, orderedTabs = labels.indices.toList(), modifier = modifier) {
                // The library TabList always draws an active underline. Use its unstyled
                // list primitive so the selected pill is the only persistent indicator.
                UnstyledTabList(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(HabitColors.Parchment)
                        .then(if (labels.size > 4) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) {
                        labels.forEachIndexed { index, label ->
                            val active = selected == index
                            val tabBg by animateColorAsState(
                                targetValue = if (active) accent else Color.Transparent,
                                animationSpec = tween(180),
                                label = "refTabBg"
                            )
                            val tabTextColor by animateColorAsState(
                                targetValue = if (active) onAccent else ink,
                                animationSpec = tween(180),
                                label = "refTabTextColor"
                            )
                            Tab(
                                key = index,
                                activateOnFocus = false,
                                modifier = (if (labels.size > 4) Modifier.widthIn(min = 94.dp) else Modifier.weight(1f))
                                    .heightIn(min = 44.dp)
                                    .habitPress(scaleDown = HabitMotion.ScaleButton)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(tabBg),
                                text = {
                                    com.composables.ui.components.Text(
                                        label,
                                        color = tabTextColor,
                                        fontSize = if (labels.size > 3) 11.sp else 13.sp,
                                        maxLines = 1,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(day: LocalDate, onSelect: (LocalDate) -> Unit) {
    val start = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onSelect(day.minusWeeks(1)) },
                modifier = Modifier.size(40.dp).habitPress(scaleDown = HabitMotion.ScaleIcon)
            ) { Icon(Icons.Rounded.ChevronLeft, "Previous week", tint = HabitColors.TextSecondary) }
            Text(day.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.weight(1f), color = HabitColors.TextSecondary, fontSize = 13.sp)
            TextButton(
                onClick = { onSelect(LocalDate.now()) },
                modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleButton)
            ) { Text("Today", fontSize = 12.sp, color = HabitColors.RoyalPurple) }
            IconButton(
                onClick = { onSelect(day.plusWeeks(1)) },
                modifier = Modifier.size(40.dp).habitPress(scaleDown = HabitMotion.ScaleIcon)
            ) { Icon(Icons.Rounded.ChevronRight, "Next week", tint = HabitColors.TextSecondary) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) { index ->
                val date = start.plusDays(index.toLong())
                val active = date == day
                val dayBg by animateColorAsState(
                    targetValue = if (active) HabitColors.RoyalPurple else Color.Transparent,
                    animationSpec = tween(180),
                    label = "weekStripDayBg"
                )
                val color by animateColorAsState(
                    targetValue = if (active) HabitColors.OnRoyalPurple else HabitColors.TextPrimary,
                    animationSpec = tween(180),
                    label = "weekStripColor"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .habitClickable(scaleDown = HabitMotion.ScaleChip, role = Role.Tab) { onSelect(date) }
                        .clip(RoundedCornerShape(15.dp))
                        .background(dayBg)
                        .semantics { selected = active; contentDescription = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEE")), color = color, fontSize = 11.sp)
                    Text("${date.dayOfMonth}", color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
                    Box(Modifier.padding(top = 6.dp).size(4.dp).background(if (date == LocalDate.now()) color else Color.Transparent, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun ProgressSummary(state: HabitUiState, sessions: List<TimetableSession>, day: LocalDate) {
    val total = state.todayHabits.size + sessions.size
    val done = state.todayHabits.count { it.completed } + sessions.count { it.isDone(day) }
    val green = HabitColors.RoyalPurple
    val track = HabitColors.Outline
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HabitColors.Surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(110.dp).semantics { contentDescription = "$done of $total tasks completed" }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(track, -90f, 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
                if (total > 0 && done > 0) drawArc(green, -90f, 360f * done / total, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            }
            Text("$done/$total", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (day == LocalDate.now()) "Today Progress" else "Day Progress", fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary, fontSize = 16.sp)
            val study = sessions.filter { it.kind == SessionKind.STUDY }
            val other = sessions.filter { it.kind != SessionKind.STUDY }
            listOf(Triple("Study", "${study.count { it.isDone(day) }}/${study.size}", green),
                Triple("Habits", "${state.todayHabits.count { it.completed }}/${state.todayHabits.size}", Color(0xFFE5A13C)),
                Triple("Others", "${other.count { it.isDone(day) }}/${other.size}", Color(0xFF589BCE))).forEach { (label, count, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(color, CircleShape)); Spacer(Modifier.width(8.dp))
                    Text(label, Modifier.weight(1f), fontSize = 13.sp, color = HabitColors.TextPrimary)
                    Text(count, fontSize = 13.sp, color = HabitColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary)
        TextButton(onClick = onClick, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleButton)) {
            Text(action, color = HabitColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun SessionRow(session: TimetableSession, day: LocalDate, timeline: Boolean, onEdit: () -> Unit, onToggle: (() -> Unit)?) {
    val done = session.isDone(day)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (timeline) Text(sessionTime(session.startMinute), modifier = Modifier.width(65.dp), fontSize = 11.sp, color = HabitColors.TextSecondary)
        val dark = LocalHabitDarkTheme.current
        val accent = SessionAccents[session.color.coerceIn(0, 4)]
        Row(Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
            .background(if (dark) accent.copy(alpha = 0.15f) else if (timeline) SessionPastels[session.color.coerceIn(0, 4)] else HabitColors.Surface)
            .habitClickable(scaleDown = HabitMotion.ScaleCard, onClick = onEdit).heightIn(min = if (timeline) 56.dp else 66.dp), verticalAlignment = Alignment.CenterVertically) {
            if (timeline) Box(Modifier.width(3.dp).height(42.dp).background(accent))
            Text(session.kind.symbol, fontSize = 22.sp, modifier = Modifier.padding(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(session.title, color = HabitColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!timeline) Text("${sessionTime(session.startMinute)} – ${sessionTime(session.endMinute)}", color = HabitColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (onToggle != null) CompletionTarget(done, "${session.title}, ${if (done) "completed" else "mark complete"}", onToggle)
            else IconButton(onClick = onEdit, modifier = Modifier.habitPress(scaleDown = HabitMotion.ScaleIcon)) { Icon(Icons.Rounded.ChevronRight, "Edit ${session.title}", tint = HabitColors.TextSecondary) }
        }
    }
}

@Composable
private fun DailyHabitRow(habit: HabitEntity, emoji: String, done: Boolean, onEdit: () -> Unit, onToggle: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).habitClickable(scaleDown = HabitMotion.ScaleCard, onClick = onEdit).heightIn(min = 72.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (LocalHabitDarkTheme.current) HabitColors.Parchment else SessionPastels[((habit.id % 5 + 5) % 5).toInt()]), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 24.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(habit.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HabitColors.TextPrimary)
            Text(habit.reminderTime ?: if (habit.isEveryDay) "Every day" else "${habit.targetDays.size} days a week", color = HabitColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (onToggle != null) CompletionTarget(done, "${habit.name}, ${if (done) "completed" else "mark complete"}", onToggle)
        else Icon(Icons.Rounded.ChevronRight, "Edit habit", tint = HabitColors.TextSecondary)
    }
}

@Composable
private fun CompletionTarget(done: Boolean, description: String, onToggle: () -> Unit) {
    val popScale by rememberHabitCompletionPop(done)
    val targetBg = if (done) HabitColors.CheckDone else Color.Transparent
    val targetBorder = if (done) HabitColors.CheckDone else HabitColors.OutlineStrong
    val bg by animateColorAsState(targetBg, tween(180), label = "completionBg")
    val border by animateColorAsState(targetBorder, tween(180), label = "completionBorder")
    val motion = LocalMotionPolicy.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .habitClickable(
                scaleDown = HabitMotion.ScaleCheckbox,
                role = Role.Checkbox,
                onClick = onToggle
            )
            .semantics {
                contentDescription = description
                toggleableState = if (done) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = popScale
                    scaleY = popScale
                }
                .clip(CircleShape)
                .background(bg)
                .border(1.5.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = done,
                enter = fadeIn(tween(if (motion.animationsEnabled) 120 else 0)) +
                        scaleIn(
                            initialScale = 0.45f,
                            animationSpec = HabitMotion.PressSpecUp
                        ),
                exit = fadeOut(tween(if (motion.animationsEnabled) 90 else 0)) +
                        scaleOut(targetScale = 0.45f)
            ) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint = HabitColors.CheckDoneOn,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun EmptyTimetable(title: String, detail: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(HabitColors.Surface).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SproutArtwork(Modifier.size(48.dp))
        Text(title, color = HabitColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        Text(detail, color = HabitColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.padding(top = 12.dp).heightIn(min = 48.dp).habitPress(scaleDown = HabitMotion.ScaleButton),
            colors = ButtonDefaults.buttonColors(containerColor = HabitColors.RoyalPurple, contentColor = HabitColors.OnRoyalPurple)
        ) { Text(action) }
    }
}

@Composable
internal fun SproutArtwork(modifier: Modifier = Modifier, withSoil: Boolean = false) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (withSoil) {
            drawOval(Color(0xFF86604B), Offset(w * .08f, h * .76f), Size(w * .84f, h * .2f))
            drawOval(Color(0xFFDBB992), Offset(w * .27f, h * .54f), Size(w * .48f, h * .34f))
            drawCircle(Color(0xFF3C3A2C), w * .018f, Offset(w * .42f, h * .71f))
            drawCircle(Color(0xFF3C3A2C), w * .018f, Offset(w * .58f, h * .71f))
        }
        val base = if (withSoil) .62f else .92f
        drawLine(Color(0xFF3D704C), Offset(w * .5f, h * base), Offset(w * .52f, h * .3f), w * .045f, StrokeCap.Round)
        val left = Path().apply { moveTo(w * .51f, h * .46f); cubicTo(w * .18f, h * .51f, w * .12f, h * .14f, w * .12f, h * .14f); cubicTo(w * .44f, h * .12f, w * .55f, h * .3f, w * .51f, h * .46f) }
        val right = Path().apply { moveTo(w * .5f, h * .33f); cubicTo(w * .5f, h * .03f, w * .88f, h * .03f, w * .9f, h * .03f); cubicTo(w * .88f, h * .32f, w * .65f, h * .39f, w * .5f, h * .33f) }
        drawPath(left, Color(0xFF6C9C62)); drawPath(right, Color(0xFF4B8455))
    }
}
