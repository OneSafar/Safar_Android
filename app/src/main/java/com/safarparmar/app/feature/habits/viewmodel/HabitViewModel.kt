package com.safarparmar.app.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.feature.habits.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class HabitTab { TODAY, WEEKLY, MONTHLY, INSIGHTS }

data class HabitPeriod(val anchor: LocalDate, val habits: List<HabitWithCompletions>)

data class HabitUiState(
    val todayHabits: List<HabitWithCompletion> = emptyList(),
    val weeklyHabits: List<HabitWithCompletions> = emptyList(),
    val monthlyHabits: List<HabitWithCompletions> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = weekOf(LocalDate.now()),
    val monthAnchor: LocalDate = LocalDate.now().withDayOfMonth(1),
    val selectedTab: HabitTab = HabitTab.TODAY,
    val isLoading: Boolean = true
)

internal fun weekOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitViewModel @Inject constructor(private val repository: HabitRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(HabitTab.TODAY)
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(weekOf(LocalDate.now()))
    private val monthAnchor = MutableStateFlow(LocalDate.now().withDayOfMonth(1))

    /**
     * Two-channel message bus:
     * - [errors] for fatal / blocking errors shown as snackbars
     * - [toasts] for soft informational messages (retroactive log confirmations)
     */
    private val errors = Channel<String>(Channel.BUFFERED)
    private val toasts = Channel<String>(Channel.BUFFERED)
    val messages = errors.receiveAsFlow()
    val toastMessages = toasts.receiveAsFlow()

    private val todayFlow = selectedDate.flatMapLatest { date ->
        repository.observeForDate(date).map { date to it }
    }
    private val weeklyFlow = weekStart.flatMapLatest { start ->
        repository.observeForRange(start, start.plusDays(6)).map { HabitPeriod(start, it) }
    }
    private val monthlyFlow = monthAnchor.flatMapLatest { start ->
        repository.observeForRange(start, start.withDayOfMonth(start.lengthOfMonth())).map { HabitPeriod(start, it) }
    }
    val uiState: StateFlow<HabitUiState> = combine(todayFlow, weeklyFlow, monthlyFlow, selectedTab) { today, week, month, tab ->
        HabitUiState(
            todayHabits = today.second,
            weeklyHabits = week.habits,
            monthlyHabits = month.habits,
            selectedDate = today.first,
            weekStart = week.anchor,
            monthAnchor = month.anchor,
            selectedTab = tab,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitUiState())

    fun selectTab(tab: HabitTab) { selectedTab.value = tab }

    fun refreshToday(now: LocalDate = LocalDate.now()) {
        val previous = selectedDate.value
        if (previous == now) return
        if (weekStart.value == weekOf(previous)) weekStart.value = weekOf(now)
        if (monthAnchor.value == previous.withDayOfMonth(1)) monthAnchor.value = now.withDayOfMonth(1)
        selectedDate.value = now
    }

    /**
     * BUG-09 FIX: refreshToday is no longer called inside toggleTodayHabit before the
     * stale guard. This eliminates the mid-toggle recomposition race.
     * BUG-01 FIX: stale guard still prevents writing to yesterday after midnight.
     */
    fun toggleTodayHabit(habitId: Long, displayedDate: LocalDate, currentlyDone: Boolean) {
        val now = LocalDate.now()
        if (displayedDate != now) {
            // Stale frame: date rolled over while user was on screen
            refreshToday(now)
            return
        }
        toggleHabit(habitId, now, currentlyDone)
    }

    /**
     * BUG-01 FIX: Uses ToggleResult to show appropriate feedback instead of
     * silently writing or silently blocking.
     */
    fun toggleHabit(habitId: Long, date: LocalDate, currentlyDone: Boolean) {
        viewModelScope.launch {
            try {
                when (repository.toggle(habitId, date, currentlyDone)) {
                    ToggleResult.SUCCESS -> { /* nominal — UI updates via DB flow */ }
                    ToggleResult.RETROACTIVE_DONE -> {
                        val fmt = date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
                        toasts.send("Marked done for $fmt")
                    }
                    ToggleResult.FUTURE_BLOCKED ->
                        toasts.send("Future habits cannot be marked done early.")
                    ToggleResult.NOT_SCHEDULED ->
                        toasts.send("This habit is not scheduled for this day.")
                    ToggleResult.HABIT_NOT_FOUND ->
                        errors.send("Habit not found.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errors.send("Unable to update habit. Please try again.") }
        }
    }

    // UX-05: Expose streak data for a habit on demand
    fun loadStreak(habit: HabitEntity, onResult: (StreakInfo) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(repository.streaks(habit))
            } catch (_: Exception) { /* non-fatal */ }
        }
    }

    suspend fun createHabit(name: String, targetDays: Set<DayOfWeek>, isEveryDay: Boolean) {
        val habits = repository.observeHabits().first()
        repository.createHabit(
            name = name,
            targetDays = targetDays,
            isEveryDay = isEveryDay,
            order = (habits.maxOfOrNull { it.order } ?: -1) + 1
        )
    }

    suspend fun editHabit(habit: HabitEntity) {
        // BUG-03: If targetDays changed, mark scheduledSince = today so historical
        // views can distinguish old vs new schedules in future when we add full history.
        repository.editHabit(habit)
    }

    /** BUG-02: Archive (soft-delete) by default. History is preserved. */
    suspend fun archiveHabit(habitId: Long) = repository.archiveHabit(habitId)

    /** Hard delete — only called when user explicitly wants to erase all history. */
    suspend fun deleteHabit(habit: HabitEntity) = repository.deleteHabit(habit)

    fun changeWeek(forward: Boolean) { weekStart.value = weekStart.value.plusWeeks(if (forward) 1 else -1) }
    fun changeMonth(forward: Boolean) { monthAnchor.value = monthAnchor.value.plusMonths(if (forward) 1 else -1).withDayOfMonth(1) }
    fun currentWeek() { weekStart.value = weekOf(LocalDate.now()) }
    fun currentMonth() { monthAnchor.value = LocalDate.now().withDayOfMonth(1) }
}
