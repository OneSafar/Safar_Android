package com.safarparmar.app.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.feature.habits.analytics.HabitAnalyticsCalculator
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitPerformance
import com.safarparmar.app.feature.habits.data.HabitPeriodStats
import com.safarparmar.app.feature.habits.data.HabitRepository
import com.safarparmar.app.feature.habits.premium.FeatureAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HabitInsightsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedYear: Int = LocalDate.now().year,
    val selectedHabitId: Long? = null,
    val monthlyStats: HabitPeriodStats? = null,
    val monthlyDailyStats: List<DailyHabitStats> = emptyList(),
    val yearlyDailyStats: List<DailyHabitStats> = emptyList(),
    val habitPerformance: List<HabitPerformance> = emptyList(),
    val availableHabits: List<HabitEntity> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val canNavigateNextMonth: Boolean = false,
    val canNavigateNextYear: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitInsightsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val calculator: HabitAnalyticsCalculator,
    val featureAccessManager: FeatureAccessManager
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val selectedYear = MutableStateFlow(LocalDate.now().year)
    private val selectedHabitId = MutableStateFlow<Long?>(null)

    private val monthlyFlow = selectedMonth.flatMapLatest { ym ->
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        repository.observeHabitsWithCompletionsForRange(start, end)
    }

    private val yearlyFlow = selectedYear.flatMapLatest { yr ->
        val start = LocalDate.of(yr, 1, 1)
        val end = LocalDate.of(yr, 12, 31)
        repository.observeHabitsWithCompletionsForRange(start, end)
    }

    val uiState: StateFlow<HabitInsightsUiState> = combine(
        selectedMonth,
        selectedYear,
        selectedHabitId,
        monthlyFlow,
        yearlyFlow
    ) { month, year, habitId, monthHabits, yearHabits ->
        val today = LocalDate.now()
        val currentYm = YearMonth.now()
        val currentYr = today.year

        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val mStats = calculator.calculatePeriodStats(monthHabits, monthStart, monthEnd, today)
        val mDaily = calculator.calculateDailyStats(monthHabits, monthStart, monthEnd, today)

        val yearStart = LocalDate.of(year, 1, 1)
        val yearEnd = LocalDate.of(year, 12, 31)

        val heatmapHabits = if (habitId == null) {
            yearHabits
        } else {
            yearHabits.filter { it.habit.id == habitId }
        }
        val yDaily = calculator.calculateDailyStats(heatmapHabits, yearStart, yearEnd, today)
        val performance = calculator.calculateHabitPerformance(yearHabits, yearStart, yearEnd, today)

        val overallCurrentStreak = performance.maxOfOrNull { it.currentStreak } ?: 0
        val overallBestStreak = performance.maxOfOrNull { it.bestStreak } ?: 0

        HabitInsightsUiState(
            selectedMonth = month,
            selectedYear = year,
            selectedHabitId = habitId,
            monthlyStats = mStats,
            monthlyDailyStats = mDaily,
            yearlyDailyStats = yDaily,
            habitPerformance = performance,
            availableHabits = yearHabits.map { it.habit }.distinctBy { it.id }.sortedBy { it.order },
            currentStreak = overallCurrentStreak,
            bestStreak = overallBestStreak,
            canNavigateNextMonth = month.isBefore(currentYm),
            canNavigateNextYear = year < currentYr,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HabitInsightsUiState(isLoading = true)
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val current = YearMonth.now()
        if (selectedMonth.value.isBefore(current)) {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
        }
    }

    fun currentMonth() {
        selectedMonth.value = YearMonth.now()
    }

    fun setMonth(yearMonth: YearMonth) {
        val current = YearMonth.now()
        selectedMonth.value = if (yearMonth.isAfter(current)) current else yearMonth
    }

    fun previousYear() {
        selectedYear.value = selectedYear.value - 1
    }

    fun nextYear() {
        val currentYr = LocalDate.now().year
        if (selectedYear.value < currentYr) {
            selectedYear.value = selectedYear.value + 1
        }
    }

    fun currentYear() {
        selectedYear.value = LocalDate.now().year
    }

    fun setYear(year: Int) {
        val currentYr = LocalDate.now().year
        selectedYear.value = if (year > currentYr) currentYr else year
    }

    fun selectHabit(habitId: Long?) {
        selectedHabitId.value = habitId
    }

    suspend fun generateExportCsv(
        exportManager: com.safarparmar.app.feature.habits.export.HabitExportManager,
        rangeOption: com.safarparmar.app.feature.habits.export.ExportDateRangeOption,
        customStart: LocalDate?,
        customEnd: LocalDate?,
        selectedHabitIds: Set<Long>?
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val allHabits = uiState.value.availableHabits
        val earliest = allHabits.minOfOrNull { it.scheduledSince } ?: LocalDate.of(2020, 1, 1)
        val (start, end) = exportManager.resolveDateRange(
            option = rangeOption,
            customStart = customStart,
            customEnd = customEnd,
            today = today,
            earliestHabitDate = earliest
        )
        val habitsWithCompletions = repository.getHabitsWithCompletionsForRange(start, end)
        val performance = calculator.calculateHabitPerformance(habitsWithCompletions, start, end, today)
        exportManager.generateCsv(
            habits = habitsWithCompletions,
            startDate = start,
            endDate = end,
            performanceList = performance,
            selectedHabitIds = selectedHabitIds
        )
    }
}
