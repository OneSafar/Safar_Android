package com.safarparmar.app.feature.kavachanalytics.domain

/**
 * How an app's screen time is counted in Kavach analytics.
 *
 * An app SAFAR has never seen is [UNCLASSIFIED] — never silently "distracting". The
 * student is prompted to categorise the ones they actually spend time in.
 */
enum class AppCategory {
    PRODUCTIVE,
    DISTRACTING,
    NEUTRAL,
    UNCLASSIFIED;

    val wire: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): AppCategory = when (value?.trim()?.lowercase()) {
            "productive" -> PRODUCTIVE
            "distracting" -> DISTRACTING
            "neutral" -> NEUTRAL
            else -> UNCLASSIFIED
        }
    }
}

/**
 * How a Kavach session finished.
 *
 * Deliberately never called "failed": a pause, a break, a revoked permission or a
 * crash is not a student failure. The user-facing label for [ENDED_EARLY] is
 * "Ended early".
 */
enum class KavachSessionOutcome {
    /** The timer reached its normal completion condition. */
    COMPLETED,

    /** The student explicitly ended the session before it completed. */
    ENDED_EARLY,

    /** A persisted active session was recovered after process/device failure. */
    INTERRUPTED;

    val wire: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): KavachSessionOutcome? = when (value?.trim()?.lowercase()) {
            "completed" -> COMPLETED
            "ended_early" -> ENDED_EARLY
            "interrupted" -> INTERRUPTED
            else -> null
        }
    }
}

/**
 * Whether a day's usage numbers can be trusted. A day with no Usage Access is
 * [UNAVAILABLE] and must never be rendered as "0 minutes".
 */
enum class DataCoverage {
    COMPLETE,
    PARTIAL,
    UNAVAILABLE;

    val wire: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): DataCoverage = when (value?.trim()?.lowercase()) {
            "complete" -> COMPLETE
            "partial" -> PARTIAL
            else -> UNAVAILABLE
        }
    }
}

/**
 * The buckets a student actually sees.
 *
 * Neutral and unclassified are one "Others" bucket on screen: the distinction is
 * SAFAR's bookkeeping, not something a student asked for. They stay separate in
 * the data so uncategorised time can still be surfaced and prompted about — see
 * [AppCategory.UNCLASSIFIED] — but the chips stay down to four.
 */
enum class KavachCategoryFilter(val label: String) {
    ALL("All apps"),
    DISTRACTING("Distracting"),
    PRODUCTIVE("Productive"),
    OTHERS("Others");

    fun matches(category: AppCategory): Boolean = when (this) {
        ALL -> true
        DISTRACTING -> category == AppCategory.DISTRACTING
        PRODUCTIVE -> category == AppCategory.PRODUCTIVE
        OTHERS -> category == AppCategory.NEUTRAL || category == AppCategory.UNCLASSIFIED
    }
}

/** Seconds in [totals] belonging to [filter]. */
fun CategoryTotals.secondsFor(filter: KavachCategoryFilter): Int = when (filter) {
    KavachCategoryFilter.ALL -> totalSeconds
    KavachCategoryFilter.DISTRACTING -> distractingSeconds
    KavachCategoryFilter.PRODUCTIVE -> productiveSeconds
    KavachCategoryFilter.OTHERS -> neutralSeconds + unclassifiedSeconds
}

/** How the date range is stepped through. */
enum class KavachGranularity(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
}

/** Kavach event types persisted immediately and rolled up at aggregation time. */
object KavachEventType {
    const val SESSION_STARTED = "session_started"
    const val BLOCKED_ATTEMPT = "blocked_attempt"
    const val QUICK_UNLOCK_STARTED = "quick_unlock_started"
    const val QUICK_UNLOCK_ENDED = "quick_unlock_ended"
    const val SESSION_COMPLETED = "session_completed"
    const val SESSION_ENDED_EARLY = "session_ended_early"
    const val SESSION_INTERRUPTED = "session_interrupted"
}

/** Kavach blocking profile a session ran under. */
object KavachSessionMode {
    const val NORMAL = "normal"
    const val BEAST = "beast"
}

// ── Read models used by the UI ───────────────────────────────────────────────

data class AppUsageRow(
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val allDaySeconds: Int,
    val kavachSeconds: Int,
    val blockedAttempts: Int,
    val quickUnlockCount: Int,
)

data class CategoryTotals(
    val productiveSeconds: Int = 0,
    val distractingSeconds: Int = 0,
    val neutralSeconds: Int = 0,
    val unclassifiedSeconds: Int = 0,
) {
    val totalSeconds: Int
        get() = productiveSeconds + distractingSeconds + neutralSeconds + unclassifiedSeconds

    operator fun plus(other: CategoryTotals) = CategoryTotals(
        productiveSeconds = productiveSeconds + other.productiveSeconds,
        distractingSeconds = distractingSeconds + other.distractingSeconds,
        neutralSeconds = neutralSeconds + other.neutralSeconds,
        unclassifiedSeconds = unclassifiedSeconds + other.unclassifiedSeconds,
    )

    fun add(category: AppCategory, seconds: Int): CategoryTotals = when (category) {
        AppCategory.PRODUCTIVE -> copy(productiveSeconds = productiveSeconds + seconds)
        AppCategory.DISTRACTING -> copy(distractingSeconds = distractingSeconds + seconds)
        AppCategory.NEUTRAL -> copy(neutralSeconds = neutralSeconds + seconds)
        AppCategory.UNCLASSIFIED -> copy(unclassifiedSeconds = unclassifiedSeconds + seconds)
    }
}

data class DailyTrendPoint(
    val localDate: String,
    val allDay: CategoryTotals,
    val duringKavach: CategoryTotals,
    val blockedAttempts: Int,
    val quickUnlockCount: Int,
    val coverage: DataCoverage,
)

data class KavachSessionSummary(
    val clientSessionId: String,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val mode: String,
    val outcome: KavachSessionOutcome?,
    val blockedAttempts: Int,
    val quickUnlockCount: Int,
    val quickUnlockSeconds: Int,
    val totals: CategoryTotals,
    val permissionLost: Boolean,
    val dataGap: Boolean,
)

data class KavachAnalyticsReport(
    val rangeStart: String,
    val rangeEnd: String,
    val allDay: CategoryTotals = CategoryTotals(),
    val duringKavach: CategoryTotals = CategoryTotals(),
    val trend: List<DailyTrendPoint> = emptyList(),
    val apps: List<AppUsageRow> = emptyList(),
    val sessions: List<KavachSessionSummary> = emptyList(),
    val blockedAttempts: Int = 0,
    val blockedAttemptsByPackage: Map<String, Int> = emptyMap(),
    val quickUnlockCount: Int = 0,
    val quickUnlockSeconds: Int = 0,
    val completedSessions: Int = 0,
    val endedEarlySessions: Int = 0,
    val interruptedSessions: Int = 0,
    val coverage: DataCoverage = DataCoverage.COMPLETE,
    val daysMissingCoverage: List<String> = emptyList(),
)
