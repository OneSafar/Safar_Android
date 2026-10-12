package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.analytics.HabitAnalyticsCalculator
import com.safarparmar.app.feature.habits.data.HabitDao
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitRepository
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import com.safarparmar.app.feature.habits.premium.DefaultFeatureAccessManager
import com.safarparmar.app.feature.habits.viewmodel.HabitInsightsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HabitInsightsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads current month and year stats`() = runTest {
        val today = LocalDate.now()
        val habit = HabitEntity(id = 1L, name = "Reading")
        val habitWithCompletions = HabitWithCompletions(
            habit = habit,
            completionByDate = mapOf(today to true)
        )

        val repository = mockk<HabitRepository>()
        every {
            repository.observeHabitsWithCompletionsForRange(any(), any())
        } returns MutableStateFlow(listOf(habitWithCompletions))

        val calculator = HabitAnalyticsCalculator()
        val featureAccess = DefaultFeatureAccessManager()
        val viewModel = HabitInsightsViewModel(repository, calculator, featureAccess)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(YearMonth.now(), state.selectedMonth)
        assertEquals(today.year, state.selectedYear)
        assertEquals(1, state.availableHabits.size)
        assertEquals("Reading", state.availableHabits[0].name)
        assertFalse(state.canNavigateNextMonth)
        assertFalse(state.canNavigateNextYear)
    }

    @Test
    fun `navigation changes month and year within bounds`() = runTest {
        val repository = mockk<HabitRepository>()
        every {
            repository.observeHabitsWithCompletionsForRange(any(), any())
        } returns MutableStateFlow(emptyList())

        val calculator = HabitAnalyticsCalculator()
        val featureAccess = DefaultFeatureAccessManager()
        val viewModel = HabitInsightsViewModel(repository, calculator, featureAccess)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        // Go to previous month
        viewModel.previousMonth()
        val prevMonth = YearMonth.now().minusMonths(1)
        val stateAfterPrev = viewModel.uiState.first { it.selectedMonth == prevMonth }
        assertEquals(prevMonth, stateAfterPrev.selectedMonth)
        assertTrue(stateAfterPrev.canNavigateNextMonth)

        // Can navigate forward back to current month
        viewModel.nextMonth()
        val stateAfterNext = viewModel.uiState.first { it.selectedMonth == YearMonth.now() }
        assertEquals(YearMonth.now(), stateAfterNext.selectedMonth)
        assertFalse(stateAfterNext.canNavigateNextMonth)

        // Cannot navigate into future month
        viewModel.nextMonth()
        assertEquals(YearMonth.now(), viewModel.uiState.value.selectedMonth)

        // Year navigation
        viewModel.previousYear()
        val prevYear = LocalDate.now().year - 1
        val stateAfterPrevYear = viewModel.uiState.first { it.selectedYear == prevYear }
        assertEquals(prevYear, stateAfterPrevYear.selectedYear)
        assertTrue(stateAfterPrevYear.canNavigateNextYear)

        // Cannot navigate beyond current year
        viewModel.nextYear()
        val stateAfterNextYear = viewModel.uiState.first { it.selectedYear == LocalDate.now().year }
        assertEquals(LocalDate.now().year, stateAfterNextYear.selectedYear)
        assertFalse(stateAfterNextYear.canNavigateNextYear)

        viewModel.nextYear()
        assertEquals(LocalDate.now().year, viewModel.uiState.value.selectedYear)
    }

    @Test
    fun `habit selection updates selectedHabitId`() = runTest {
        val repository = mockk<HabitRepository>()
        every {
            repository.observeHabitsWithCompletionsForRange(any(), any())
        } returns MutableStateFlow(emptyList())

        val calculator = HabitAnalyticsCalculator()
        val featureAccess = DefaultFeatureAccessManager()
        val viewModel = HabitInsightsViewModel(repository, calculator, featureAccess)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        viewModel.selectHabit(42L)
        val state = viewModel.uiState.first { it.selectedHabitId == 42L }
        assertEquals(42L, state.selectedHabitId)

        viewModel.selectHabit(null)
        val stateAll = viewModel.uiState.first { it.selectedHabitId == null }
        assertEquals(null, stateAll.selectedHabitId)
    }

    @Test
    fun `bento metrics compute accurately for all habits and single habit`() = runTest {
        val today = LocalDate.now()
        val habit1 = HabitEntity(id = 1L, name = "Reading", scheduledSince = today.minusDays(10))
        val habit2 = HabitEntity(id = 2L, name = "Workout", scheduledSince = today.minusDays(5))

        val hwc1 = HabitWithCompletions(
            habit = habit1,
            completionByDate = mapOf(
                today.minusDays(2) to true,
                today.minusDays(1) to true,
                today to true
            )
        )
        val hwc2 = HabitWithCompletions(
            habit = habit2,
            completionByDate = mapOf(
                today.minusDays(1) to false,
                today to true
            )
        )

        val repository = mockk<HabitRepository>()
        every {
            repository.observeHabitsWithCompletionsForRange(any(), any())
        } returns MutableStateFlow(listOf(hwc1, hwc2))

        val calculator = HabitAnalyticsCalculator()
        val featureAccess = DefaultFeatureAccessManager()
        val viewModel = HabitInsightsViewModel(repository, calculator, featureAccess)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        val allState = viewModel.uiState.first { !it.isLoading }
        assertEquals(4, allState.totalCompleted)
        assertEquals(5, allState.totalScheduled)
        assertEquals(1, allState.totalMissed)
        assertEquals(0.8f, allState.completionRate, 0.01f)
        assertEquals(today.minusDays(10), allState.startedDate)
        assertEquals(10L, allState.daysSinceStarted)

        // Select habit 2
        viewModel.selectHabit(2L)
        val habit2State = viewModel.uiState.first { it.selectedHabitId == 2L }
        assertEquals(1, habit2State.totalCompleted)
        assertEquals(2, habit2State.totalScheduled)
        assertEquals(1, habit2State.totalMissed)
        assertEquals(0.5f, habit2State.completionRate, 0.01f)
        assertEquals(today.minusDays(5), habit2State.startedDate)
        assertEquals(5L, habit2State.daysSinceStarted)
    }
}
