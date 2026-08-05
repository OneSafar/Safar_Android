package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRepository
import com.safarparmar.app.feature.kavachanalytics.data.local.AppClassificationEntity
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow
import com.safarparmar.app.feature.kavachanalytics.domain.KavachAnalyticsReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** The date windows offered inside the 12-month retention window. */
enum class KavachRange(val label: String) {
    TODAY("Today"),
    LAST_7("7 days"),
    LAST_30("30 days"),
    MONTH("Month"),
    CUSTOM("Custom"),
}

/** Which measurement the summary and trend show. */
enum class KavachScope(val label: String) {
    ALL_DAY("All day"),
    DURING_KAVACH("During Kavach"),
}

data class KavachAnalyticsUiState(
    val isLoading: Boolean = true,
    val range: KavachRange = KavachRange.LAST_7,
    val scope: KavachScope = KavachScope.ALL_DAY,
    val startDate: String = "",
    val endDate: String = "",
    val selectedMonth: String = YearMonth.now().toString(),
    val report: KavachAnalyticsReport? = null,
    val classifications: List<AppClassificationEntity> = emptyList(),
    val unclassifiedPrompts: List<AppUsageRow> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val error: String? = null,
) {
    /** The earliest date analytics can be asked for — detailed data is kept 12 months. */
    val earliestDate: LocalDate
        get() = LocalDate.now().minusMonths(KavachAnalyticsRepository.RETENTION_MONTHS)
}

@HiltViewModel
class KavachAnalyticsViewModel @Inject constructor(
    private val repository: KavachAnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KavachAnalyticsUiState())
    val uiState: StateFlow<KavachAnalyticsUiState> = _uiState.asStateFlow()

    init {
        selectRange(KavachRange.LAST_7)
    }

    fun selectRange(range: KavachRange) {
        val today = LocalDate.now()
        val (start, end) = when (range) {
            KavachRange.TODAY -> today to today
            KavachRange.LAST_7 -> today.minusDays(6) to today
            KavachRange.LAST_30 -> today.minusDays(29) to today
            KavachRange.MONTH -> {
                val month = runCatching { YearMonth.parse(_uiState.value.selectedMonth) }
                    .getOrDefault(YearMonth.from(today))
                month.atDay(1) to minOf(month.atEndOfMonth(), today)
            }
            KavachRange.CUSTOM -> return // custom ranges arrive via selectCustomRange
        }
        _uiState.update { it.copy(range = range) }
        load(start, end)
    }

    fun selectMonth(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
        selectRange(KavachRange.MONTH)
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        _uiState.update { it.copy(range = KavachRange.CUSTOM) }
        load(start, end)
    }

    fun selectScope(scope: KavachScope) {
        _uiState.update { it.copy(scope = scope) }
    }

    fun refresh() {
        val state = _uiState.value
        val start = runCatching { LocalDate.parse(state.startDate) }.getOrNull() ?: return
        val end = runCatching { LocalDate.parse(state.endDate) }.getOrNull() ?: return
        viewModelScope.launch {
            runCatching { repository.refresh() }
            load(start, end)
        }
    }

    fun setCategory(packageName: String, category: AppCategory, appLabel: String? = null) {
        viewModelScope.launch {
            repository.setCategory(packageName, category, appLabel)
            val state = _uiState.value
            val start = runCatching { LocalDate.parse(state.startDate) }.getOrNull() ?: return@launch
            val end = runCatching { LocalDate.parse(state.endDate) }.getOrNull() ?: return@launch
            load(start, end)
        }
    }

    private fun load(startInput: LocalDate, endInput: LocalDate) {
        // Nothing outside the retained window can be answered, so clamp rather than
        // render an empty range that reads as "you did nothing".
        val floor = LocalDate.now().minusMonths(KavachAnalyticsRepository.RETENTION_MONTHS)
        val start = maxOf(startInput, floor)
        val end = maxOf(endInput, start)

        _uiState.update {
            it.copy(
                isLoading = true,
                startDate = start.toString(),
                endDate = end.toString(),
                error = null,
            )
        }

        viewModelScope.launch {
            val result = runCatching {
                repository.report(start.toString(), end.toString())
            }
            val classifications = runCatching { repository.classifications() }.getOrDefault(emptyList())
            _uiState.update { state ->
                val report = result.getOrNull()
                state.copy(
                    isLoading = false,
                    report = report,
                    classifications = classifications,
                    unclassifiedPrompts = report?.apps
                        ?.filter { it.category == AppCategory.UNCLASSIFIED && it.allDaySeconds >= UNCLASSIFIED_PROMPT_SECONDS }
                        ?.take(5)
                        .orEmpty(),
                    hasUsageAccess = repository.hasUsageAccess(),
                    error = result.exceptionOrNull()?.let { "Couldn't load Kavach analytics." },
                )
            }
        }
    }

    private companion object {
        /** Only nag about unknown apps the student actually spends time in. */
        const val UNCLASSIFIED_PROMPT_SECONDS = 5 * 60
    }
}
