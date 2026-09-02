package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRepository
import com.safarparmar.app.feature.kavachanalytics.data.local.AppClassificationEntity
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.DailyTrendPoint
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.feature.kavachanalytics.domain.KavachAnalyticsReport
import com.safarparmar.app.feature.kavachanalytics.domain.KavachCategoryFilter
import com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity
import com.safarparmar.app.feature.kavachanalytics.domain.secondsFor
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safarparmar.app.feature.youtubeinsights.YoutubeInsightsRepository
import com.safarparmar.app.feature.youtubeinsights.YoutubeTotals
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeChannelEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeDailyAggregateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Which measurement the summary and trend show. */
enum class KavachScope(val label: String) {
    ALL_DAY("Full Day"),
    DURING_KAVACH("Kavach Time"),
}

/** One period, resolved from a granularity and how many steps back the student has paged. */
data class KavachPeriod(
    val granularity: KavachGranularity,
    val offset: Int,
    val start: LocalDate,
    val end: LocalDate,
) {
    companion object {
        fun of(granularity: KavachGranularity, offset: Int, today: LocalDate = LocalDate.now()): KavachPeriod {
            val clamped = offset.coerceAtMost(0)
            val (start, end) = when (granularity) {
                KavachGranularity.DAILY -> {
                    val day = today.plusDays(clamped.toLong())
                    day to day
                }
                KavachGranularity.WEEKLY -> {
                    // Weeks run Monday–Sunday, matching how a student reads a timetable.
                    val monday = today.with(java.time.DayOfWeek.MONDAY).plusWeeks(clamped.toLong())
                    monday to monday.plusDays(6)
                }
                KavachGranularity.MONTHLY -> {
                    val first = today.withDayOfMonth(1).plusMonths(clamped.toLong())
                    first to first.plusMonths(1).minusDays(1)
                }
            }
            // Never ask for days that haven't happened yet — an unfinished week must
            // not read as a week where usage collapsed.
            return KavachPeriod(granularity, clamped, start, minOf(end, today))
        }
    }

    /** The same-length period immediately before this one, for the "vs last" delta. */
    fun previous(today: LocalDate = LocalDate.now()): KavachPeriod =
        of(granularity, offset - 1, today)
}

/** One bar in the trend chart. */
data class KavachChartBar(
    val localDate: String,
    val totals: CategoryTotals,
    val coverage: DataCoverage,
    /** False for the surrounding context days in the Daily view. */
    val isFocused: Boolean,
    /** Future placeholders complete the current week/month without pretending they were measured. */
    val isFuture: Boolean = false,
)

/** The single-app view opened by tapping a row. */
data class KavachAppDetail(
    val row: AppUsageRow,
    val isBlockedInKavach: Boolean,
    val dailySeconds: List<Pair<String, Int>>,
)

data class KavachAnalyticsUiState(
    val isLoading: Boolean = true,
    val granularity: KavachGranularity = KavachGranularity.DAILY,
    val periodOffset: Int = 0,
    val scope: KavachScope = KavachScope.ALL_DAY,
    val categoryFilter: KavachCategoryFilter = KavachCategoryFilter.ALL,
    val startDate: String = "",
    val endDate: String = "",
    val report: KavachAnalyticsReport? = null,
    /** Same-length previous period, used only for the delta. */
    val previousTotalSeconds: Int? = null,
    val classifications: List<AppClassificationEntity> = emptyList(),
    val unclassifiedPrompts: List<AppUsageRow> = emptyList(),
    val hasUsageAccess: Boolean = true,
    val appDetail: KavachAppDetail? = null,
    val youtubeDetailOpen: Boolean = false,
    val youtubeTotals: YoutubeTotals = YoutubeTotals(),
    val youtubeTrend: List<YoutubeDailyAggregateEntity> = emptyList(),
    val youtubeChannels: List<YoutubeChannelEntity> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
) {
    /** Detailed data is kept 12 months; paging stops there. */
    val earliestDate: LocalDate
        get() = LocalDate.now().minusMonths(KavachAnalyticsRepository.RETENTION_MONTHS)

    val canPageBack: Boolean
        get() = runCatching { LocalDate.parse(startDate) }.getOrNull()
            ?.isAfter(earliestDate) ?: false

    val canPageForward: Boolean get() = periodOffset < 0

    val displayStartDate: LocalDate?
        get() = runCatching { LocalDate.parse(startDate) }.getOrNull()

    /** Full calendar boundary used by the chart; data requests remain clamped to today. */
    val displayEndDate: LocalDate?
        get() = displayStartDate?.let { start ->
            when (granularity) {
                KavachGranularity.DAILY -> runCatching { LocalDate.parse(endDate) }.getOrDefault(start)
                KavachGranularity.WEEKLY -> start.plusDays(6)
                KavachGranularity.MONTHLY -> start.withDayOfMonth(start.lengthOfMonth())
            }
        }

    val hasFuturePlaceholders: Boolean
        get() = displayEndDate?.isAfter(LocalDate.now()) == true

    private val totals: CategoryTotals
        get() = report?.let { if (scope == KavachScope.ALL_DAY) it.allDay else it.duringKavach }
            ?: CategoryTotals()

    /** Total for the selected chip — the number shown large at the top. */
    val filteredSeconds: Int get() = totals.secondsFor(categoryFilter)

    /**
     * Percentage change against the previous period, or null when there is nothing
     * to compare against. Deliberately null rather than 0 for a first period.
     */
    val deltaPercent: Int?
        get() {
            val previous = previousTotalSeconds ?: return null
            if (previous <= 0) return null
            return (((filteredSeconds - previous).toDouble() / previous) * 100).toInt()
        }

    val filteredApps: List<AppUsageRow>
        get() {
            val query = searchQuery.trim().lowercase()
            return report?.apps.orEmpty()
                .filter {
                    secondsOf(it) > 0 ||
                        (categoryFilter == KavachCategoryFilter.ALL && it.blockedAttempts > 0)
                }
                .filter { query.isEmpty() || it.appLabel.lowercase().contains(query) }
                .filter { secondsOf(it) > 0 || it.blockedAttempts > 0 }
                // Uncategorised first inside Others: those are the rows whose time is
                // still unattributed, and the ones worth a student's attention.
                .sortedWith(
                    compareBy<AppUsageRow> {
                        categoryFilter == KavachCategoryFilter.OTHERS &&
                            it.category != AppCategory.UNCLASSIFIED
                    }.thenByDescending { secondsOf(it) },
                )
        }

    /** Per-day totals for the chart, with the day the headline describes flagged. */
    val chartBars: List<KavachChartBar>
        get() {
            val source = report?.trend.orEmpty()
            val measured = source.associateBy { it.localDate }
            val dates = when (granularity) {
                KavachGranularity.DAILY -> source.mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
                KavachGranularity.WEEKLY,
                KavachGranularity.MONTHLY -> {
                    val start = displayStartDate ?: return emptyList()
                    val finish = displayEndDate ?: return emptyList()
                    generateSequence(start) { day -> day.plusDays(1).takeIf { !it.isAfter(finish) } }.toList()
                }
            }
            val today = LocalDate.now()
            return dates.map { date ->
                val point = measured[date.toString()]
                KavachChartBar(
                    localDate = date.toString(),
                    totals = point?.let { if (scope == KavachScope.ALL_DAY) it.allDay else it.duringKavach }
                        ?: CategoryTotals(),
                    coverage = point?.coverage ?: DataCoverage.UNAVAILABLE,
                    isFocused = granularity != KavachGranularity.DAILY || date.toString() == endDate,
                    isFuture = date.isAfter(today),
                )
            }
        }

    fun secondsOf(row: AppUsageRow): Int =
        row.secondsFor(categoryFilter, scope == KavachScope.DURING_KAVACH)
}

@HiltViewModel
class KavachAnalyticsViewModel @Inject constructor(
    private val repository: KavachAnalyticsRepository,
    private val focusShieldRepository: FocusShieldRepository,
    private val youtubeRepository: YoutubeInsightsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KavachAnalyticsUiState())
    val uiState: StateFlow<KavachAnalyticsUiState> = _uiState.asStateFlow()

    init {
        reload()
        refresh()
    }

    fun selectGranularity(granularity: KavachGranularity) {
        _uiState.update { it.copy(granularity = granularity, periodOffset = 0) }
        reload()
    }

    /** Steps one period back (-1) or forward (+1). */
    fun page(direction: Int) {
        val state = _uiState.value
        if (direction > 0 && !state.canPageForward) return
        if (direction < 0 && !state.canPageBack) return
        _uiState.update { it.copy(periodOffset = (it.periodOffset + direction).coerceAtMost(0)) }
        reload()
    }

    fun selectScope(scope: KavachScope) = _uiState.update { it.copy(scope = scope) }

    fun selectCategory(filter: KavachCategoryFilter) =
        _uiState.update { it.copy(categoryFilter = filter) }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.refresh() }
            reload()
        }
    }

    // ── App detail ───────────────────────────────────────────────────────────

    fun openAppDetail(row: AppUsageRow) {
        val state = _uiState.value
        val perDay = state.report?.trend.orEmpty().map { it.localDate to 0 }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    appDetail = KavachAppDetail(
                        row = row,
                        isBlockedInKavach = row.packageName in focusShieldRepository.blockedPackages.value,
                        dailySeconds = perDay,
                    ),
                )
            }
        }
    }

    fun closeAppDetail() = _uiState.update { it.copy(appDetail = null) }
    fun closeYoutubeDetail() = _uiState.update { it.copy(youtubeDetailOpen = false) }

    fun setYoutubeChannelProductive(channelKey: String, productive: Boolean) {
        viewModelScope.launch {
            youtubeRepository.setProductive(channelKey, productive)
            val state = _uiState.value
            val (totals, trend) = youtubeRepository.totals(state.startDate, state.endDate)
            _uiState.update {
                it.copy(
                    youtubeTotals = totals,
                    youtubeTrend = trend,
                    youtubeChannels = youtubeRepository.channels(),
                )
            }
        }
    }

    /**
     * Adds or removes this app from the set Kavach blocks. The reference app can
     * only offer "add a limit" here; Kavach already blocks apps, so the analytics
     * row and the block list are the same decision.
     */
    fun toggleBlocked(packageName: String) {
        val current = focusShieldRepository.blockedPackages.value.toMutableSet()
        val nowBlocked = if (packageName in current) {
            current.remove(packageName); false
        } else {
            current.add(packageName); true
        }
        focusShieldRepository.setBlockedPackages(current)
        _uiState.update { state ->
            state.copy(
                appDetail = state.appDetail
                    ?.takeIf { it.row.packageName == packageName }
                    ?.copy(isBlockedInKavach = nowBlocked)
                    ?: state.appDetail,
            )
        }
    }

    fun setCategory(packageName: String, category: AppCategory, appLabel: String? = null) {
        viewModelScope.launch {
            repository.setCategory(packageName, category, appLabel)
            reload()
            _uiState.update { state ->
                state.copy(
                    appDetail = state.appDetail?.takeIf { it.row.packageName == packageName }
                        ?.let { it.copy(row = it.row.copy(category = category)) }
                        ?: state.appDetail,
                )
            }
        }
    }

    // ── Loading ──────────────────────────────────────────────────────────────

    private fun reload() {
        val state = _uiState.value
        val period = KavachPeriod.of(state.granularity, state.periodOffset)
        val floor = state.earliestDate
        val start = maxOf(period.start, floor)
        val end = maxOf(period.end, start)

        _uiState.update {
            it.copy(
                isLoading = true,
                startDate = start.toString(),
                endDate = end.toString(),
                error = null,
            )
        }

        viewModelScope.launch {
            val result = runCatching { repository.report(start.toString(), end.toString()) }

            val previous = period.previous().let { prev ->
                if (prev.end.isBefore(floor)) null
                else runCatching {
                    repository.report(
                        maxOf(prev.start, floor).toString(),
                        maxOf(prev.end, floor).toString(),
                    )
                }.getOrNull()
            }
            val classifications = runCatching { repository.classifications() }.getOrDefault(emptyList())
            val youtube = runCatching { youtubeRepository.totals(start.toString(), end.toString()) }.getOrNull()

            _uiState.update { current ->
                val report = result.getOrNull()
                val previousTotals = previous?.let {
                    if (current.scope == KavachScope.ALL_DAY) it.allDay else it.duringKavach
                }
                current.copy(
                    isLoading = false,
                    report = report,
                    previousTotalSeconds = previousTotals?.secondsFor(current.categoryFilter),
                    classifications = classifications,
                    unclassifiedPrompts = report?.apps
                        ?.filter {
                            it.category == AppCategory.UNCLASSIFIED &&
                                it.allDaySeconds >= UNCLASSIFIED_PROMPT_SECONDS
                        }
                        ?.take(5)
                        .orEmpty(),
                    hasUsageAccess = repository.hasUsageAccess(),
                    youtubeTotals = youtube?.first ?: YoutubeTotals(),
                    youtubeTrend = youtube?.second.orEmpty(),
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

/** Today's headline numbers for the Ekagra summary pills. */
data class KavachTodaySummary(
    val usageSeconds: Int,
    val kavachSeconds: Int,
    val coverage: DataCoverage,
) {
    /**
     * A day we could not measure must read as "—", never as "0m". A pill is the
     * most convincing place to accidentally tell a student they used nothing.
     */
    val isMeasured: Boolean get() = coverage != DataCoverage.UNAVAILABLE
}
