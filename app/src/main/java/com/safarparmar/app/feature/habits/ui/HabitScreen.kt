package com.safarparmar.app.feature.habits.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    openTimetable: Boolean = false,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val iconViewModel: com.safarparmar.app.feature.habits.viewmodel.HabitIconViewModel = hiltViewModel()
    val habitIcons by iconViewModel.icons.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var openDrawer by remember { mutableStateOf<() -> Unit>({}) }
    var showAddHabit by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    com.safarparmar.app.ui.tutorial.ScreenPracticeEntry(
        com.safarparmar.app.ui.tutorial.TitliFeature.HABITS, isDarkTheme,
        eligible = !state.isLoading && !showAddHabit && editingId == null)

    val selectedHabit = state.weeklyHabits.firstOrNull { it.habit.id == editingId }?.habit
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        launch { viewModel.messages.collect { snackbar.showSnackbar(it) } }
        launch { viewModel.toastMessages.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) } }
    }

    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshToday()
                viewModel.refreshBackend()
                val now = ZonedDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay((Duration.between(now, midnight).toMillis() + 50).coerceIn(50, 15_000))
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

    com.safarparmar.app.feature.habits.ui.HabitAccentTheme(isDarkTheme) {
        SafarDrawerScaffold(
            title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_tracker),
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            showTopBar = false,
            onDrawerControllerReady = { openDrawer = it },
            containerColor = HabitColors.Background
        ) { padding ->
            Box(Modifier.fillMaxSize().background(HabitColors.Background).padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
                HabitTimetableContent(
                    state = state,
                    habitIcons = habitIcons,
                    habitViewModel = viewModel,
                    onAddHabit = { if (!state.isLoading && !state.isMigrating) showAddHabit = true },
                    onEditHabit = { editingId = it.id },
                    onMore = { openDrawer() },
                    openTimetable = openTimetable,
                    snackbar = snackbar
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
                onCreate = { name, days, isEveryDay, reminderTime, emoji, operationId -> viewModel.createHabit(name, days, isEveryDay, reminderTime, emoji, operationId) },
                onCreated = { name, days ->
                    scope.launch { snackbar.showSnackbar(habitCreatedMessage(name, days, LocalDate.now())) }
                }
            )
        }

        if (selectedHabit != null) {
            EditHabitBottomSheet(
                habit = selectedHabit,
                onDismiss = { editingId = null },
                onSave = { habit, emoji -> viewModel.editHabit(habit, emoji) },
                initialEmoji = habitIcons[selectedHabit.remoteId] ?: "🌱",
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
