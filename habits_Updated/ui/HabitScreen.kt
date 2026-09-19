package com.safarparmar.app.feature.habits.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.safarparmar.app.feature.habits.viewmodel.HabitTab
import com.safarparmar.app.feature.habits.viewmodel.HabitViewModel
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

@Composable
fun HabitScreen(
    currentRoute: String = Routes.HABIT_TRACKER,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: HabitViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAddHabit by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedHabit = state.weeklyHabits.firstOrNull { it.habit.id == editingId }?.habit
    val snackbar = remember { SnackbarHostState() }
    val pager = rememberPagerState(initialPage = state.selectedTab.ordinal) { HabitTab.entries.size }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    LaunchedEffect(pager.settledPage) {
        viewModel.selectTab(HabitTab.entries[pager.settledPage])
    }
    LaunchedEffect(state.selectedTab) {
        if (pager.currentPage != state.selectedTab.ordinal) pager.scrollToPage(state.selectedTab.ordinal)
    }

    LaunchedEffect(viewModel) {
        launch { viewModel.messages.collect { snackbar.showSnackbar(it) } }
        launch { viewModel.toastMessages.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) } }
    }

    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshToday()
                val now = ZonedDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay((Duration.between(now, midnight).toMillis() + 50).coerceIn(50, 60_000))
            }
        }
    }

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = viewModel.refreshToday()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    CompositionLocalProvider(LocalHabitDarkTheme provides isDarkTheme) {
        SafarDrawerScaffold(
            title = "Habit Tracker",
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            containerColor = HabitColors.Background
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(HabitColors.Background)
                    .padding(padding)
            ) {
                Column(Modifier.fillMaxSize()) {
                    ModernTabs(
                        selected = HabitTab.entries[pager.currentPage],
                        onSelected = { tab -> scope.launch { pager.animateScrollToPage(tab.ordinal) } }
                    )

                    if (state.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = HabitColors.RoyalPurple)
                        }
                    } else {
                        HorizontalPager(
                            state = pager,
                            beyondViewportPageCount = 1,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            when (HabitTab.entries[page]) {
                                HabitTab.TODAY -> TodayView(
                                    date = state.selectedDate,
                                    habits = state.todayHabits,
                                    allHabits = state.weeklyHabits.map { it.habit },
                                    onToggle = { id, done -> viewModel.toggleTodayHabit(id, state.selectedDate, done) },
                                    onEditHabit = { editingId = it.id },
                                    modifier = Modifier.fillMaxSize(),
                                    hasHabits = state.weeklyHabits.isNotEmpty(),
                                    onAdd = { showAddHabit = true }
                                )
                                HabitTab.WEEKLY -> WeeklyView(
                                    weekStart = state.weekStart,
                                    habits = state.weeklyHabits,
                                    onToggleDay = viewModel::toggleHabit,
                                    onPrevWeek = { viewModel.changeWeek(false) },
                                    onNextWeek = { viewModel.changeWeek(true) },
                                    onEditHabit = { editingId = it.id },
                                    modifier = Modifier.fillMaxSize(),
                                    today = state.selectedDate,
                                    onCurrent = viewModel::currentWeek,
                                    onAdd = { showAddHabit = true }
                                )
                                HabitTab.MONTHLY -> MonthlyView(
                                    monthAnchor = state.monthAnchor,
                                    habits = state.monthlyHabits,
                                    onToggleDay = viewModel::toggleHabit,
                                    onPrevMonth = { viewModel.changeMonth(false) },
                                    onNextMonth = { viewModel.changeMonth(true) },
                                    onEditHabit = { editingId = it.id },
                                    modifier = Modifier.fillMaxSize(),
                                    today = state.selectedDate,
                                    onCurrent = viewModel::currentMonth,
                                    onAdd = { showAddHabit = true },
                                    streakHabits = state.streakHabits
                                )
                            }
                        }
                    }
                }

                ModernPrimaryFab(
                    onClick = { showAddHabit = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                )

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 92.dp)
                )
            }
        }

        if (showAddHabit) {
            AddHabitBottomSheet(
                onDismiss = { showAddHabit = false },
                onCreate = { name, days, isEveryDay -> viewModel.createHabit(name, days, isEveryDay) },
                onCreated = { name, days ->
                    scope.launch { snackbar.showSnackbar(habitCreatedMessage(name, days, LocalDate.now())) }
                }
            )
        }

        if (selectedHabit != null) {
            EditHabitBottomSheet(
                habit = selectedHabit,
                onDismiss = { editingId = null },
                onSave = viewModel::editHabit,
                onArchive = { id ->
                    viewModel.archiveHabit(id)
                    editingId = null
                },
                onDelete = { habit ->
                    viewModel.deleteHabit(habit)
                    editingId = null
                }
            )
        }
    }
}
