package com.safarparmar.app.feature.kavachanalytics.data

import android.content.Context
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.feature.kavachanalytics.data.local.AppClassificationEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.DailyAppAggregateEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachMetaEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachSessionEntity
import com.safarparmar.app.feature.kavachanalytics.data.remote.AppClassificationBatchRequest
import com.safarparmar.app.feature.kavachanalytics.data.remote.AppClassificationDto
import com.safarparmar.app.feature.kavachanalytics.data.remote.DailyAggregateBatchRequest
import com.safarparmar.app.feature.kavachanalytics.data.remote.DailyAggregateUploadDto
import com.safarparmar.app.feature.kavachanalytics.data.remote.KavachAnalyticsApi
import com.safarparmar.app.feature.kavachanalytics.data.remote.SessionBatchRequest
import com.safarparmar.app.feature.kavachanalytics.data.remote.SessionUploadDto
import com.safarparmar.app.feature.kavachanalytics.data.remote.YoutubeDailyAggregateBatchRequest
import com.safarparmar.app.feature.kavachanalytics.data.remote.YoutubeDailyAggregateUploadDto
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.DailyTrendPoint
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.feature.kavachanalytics.domain.KavachAnalyticsReport
import com.safarparmar.app.feature.kavachanalytics.domain.KavachEventType
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionMode
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionSummary
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first store for Kavach analytics.
 *
 * Detailed foreground transitions never leave the device; only daily per-app
 * aggregates and Kavach session summaries sync. Detailed data is kept for
 * [RETENTION_MONTHS] months, with lifetime headline counters kept separately so
 * pruning never makes a student's all-time totals shrink.
 */
@Singleton
class KavachAnalyticsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: KavachAnalyticsDao,
    private val api: KavachAnalyticsApi,
    private val collector: KavachUsageCollector,
    private val recorder: KavachAnalyticsRecorder,
    private val youtubeInsightsRepository: com.safarparmar.app.feature.youtubeinsights.YoutubeInsightsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val aggregateMutex = Mutex()
    private val syncMutex = Mutex()

    private val zone: ZoneId get() = ZoneId.systemDefault()

    // ── Classification ───────────────────────────────────────────────────────

    /**
     * Writes SAFAR's shipped defaults for any package the student has not already
     * ruled on. User overrides are never touched.
     */
    suspend fun seedDefaultClassifications() = withContext(ioDispatcher) {
        val existing = dao.allClassifications().associateBy { it.packageName }
        val now = System.currentTimeMillis()
        val rows = AppCategoryDefaults.DEFAULTS.mapNotNull { (packageName, category) ->
            val current = existing[packageName]
            when {
                current == null -> AppClassificationEntity(
                    packageName = packageName,
                    category = category.wire,
                    appLabel = context.appLabelOrNull(packageName),
                    isUserOverride = false,
                    updatedAtMs = now,
                    dirty = false,
                )
                // A default may be re-tuned by SAFAR between releases, but only for
                // apps the student has never overridden.
                !current.isUserOverride && current.category != category.wire ->
                    current.copy(category = category.wire, updatedAtMs = now)
                else -> null
            }
        }
        if (rows.isNotEmpty()) dao.upsertClassifications(rows)
    }

    suspend fun classifications(): List<AppClassificationEntity> =
        withContext(ioDispatcher) { dao.allClassifications() }

    suspend fun categoryMap(): Map<String, AppCategory> = withContext(ioDispatcher) {
        dao.allClassifications().associate { it.packageName to AppCategory.fromWire(it.category) }
    }

    /**
     * Records a student's category choice and reclassifies their retained history so
     * every date range stays internally consistent with the new answer.
     */
    suspend fun setCategory(
        packageName: String,
        category: AppCategory,
        appLabel: String? = null,
    ) = withContext(ioDispatcher) {
        if (packageName.isBlank()) return@withContext
        val now = System.currentTimeMillis()
        dao.upsertClassifications(
            listOf(
                AppClassificationEntity(
                    packageName = packageName,
                    category = category.wire,
                    appLabel = appLabel ?: dao.classificationFor(packageName)?.appLabel
                        ?: context.appLabelOrNull(packageName),
                    isUserOverride = true,
                    updatedAtMs = now,
                    dirty = true,
                ),
            ),
        )
        dao.reclassifyAggregates(packageName, category.wire, now)
        recomputeSessionTotalsForRetainedWindow()
    }

    // ── Collection & aggregation ─────────────────────────────────────────────

    /**
     * The one entry point used by both the periodic worker and app startup:
     * collect what the OS has, aggregate every day it touched, prune, and try to
     * sync. Safe to call often — collection is watermark-driven.
     */
    suspend fun refresh(sync: Boolean = true) = withContext(ioDispatcher) {
        youtubeInsightsRepository.flushTracking()
        runCatching { seedDefaultClassifications() }
        // Backstop for a quick unlock whose window lapsed while nothing was watching
        // — the service was killed, or the phone was idle. Without this the event
        // would sit open forever and its duration would never be counted.
        runCatching { recorder.settleExpiredUnlocks() }
        val touched = runCatching { collector.collect() }.getOrDefault(emptySet())
        val today = LocalDate.now(zone).toString()
        (touched + today + dao.datesWithIntervals()).forEach { date ->
            runCatching { aggregateDate(date) }
        }
        runCatching { prune() }
        if (sync) runCatching { syncNow() }
    }

    /**
     * Rebuilds one day's aggregates from its raw intervals and events, then drops the
     * raw intervals once that day is finished — they exist only to be aggregated.
     */
    suspend fun aggregateDate(localDate: String) = withContext(ioDispatcher) {
        aggregateMutex.withLock {
            val intervals = dao.intervalsForDate(localDate).map {
                DatedUsageInterval(it.packageName, it.localDate, it.startMs, it.endMs)
            }
            val events = dao.eventsForDate(localDate)
            if (intervals.isEmpty() && events.isEmpty()) return@withLock

            // A finished day's raw intervals are deleted once aggregated, but its
            // events live on for 12 months. Re-aggregating such a day would rebuild
            // it from events alone and overwrite real measured usage with zeros —
            // silently destroying that day's history. No current caller does this,
            // but the method is public and the failure would be invisible.
            if (intervals.isEmpty() && dao.aggregatesBetween(localDate, localDate).isNotEmpty()) {
                debugLog("aggregateDate($localDate) skipped: intervals pruned, keeping stored totals")
                return@withLock
            }

            val dayStartMs = LocalDate.parse(localDate).atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEndMs = LocalDate.parse(localDate).plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli()

            val sessions = dao.sessionsBetween(
                LocalDate.parse(localDate).minusDays(1).toString(),
                localDate,
            ).filter { it.startedAtMs < dayEndMs && (it.endedAtMs ?: System.currentTimeMillis()) > dayStartMs }

            // "During Kavach" means every stretch Kavach was actually guarding the
            // student — a timed session or Always On. Deriving it from sessions alone
            // is what made an Always On day report zero protected time while Kavach
            // had been blocking all day.
            val windows = dao.protectionWindowsOverlapping(dayStartMs, dayEndMs).map {
                it.startMs..(if (it.isOpen) maxOf(it.endMs, System.currentTimeMillis()) else it.endMs)
            }

            val categories = categoryMap()
            val labels = dao.allClassifications().associate { it.packageName to it.appLabel }
            val counts = DailyEventCounts(
                blockedAttempts = events
                    .filter { it.type == KavachEventType.BLOCKED_ATTEMPT && !it.packageName.isNullOrBlank() }
                    .groupingBy { it.packageName!! }
                    .eachCount(),
                quickUnlocks = events
                    .filter { it.type == KavachEventType.QUICK_UNLOCK_STARTED && !it.packageName.isNullOrBlank() }
                    .groupingBy { it.packageName!! }
                    .eachCount(),
            )

            val rows = KavachDailyAggregator.aggregate(
                localDate = localDate,
                intervals = intervals,
                kavachWindows = windows,
                eventCounts = counts,
                categoryOf = { categories[it] ?: AppCategory.UNCLASSIFIED },
                labelOf = { labels[it] ?: context.appLabelOrNull(it) },
                nowMs = System.currentTimeMillis(),
            )
            dao.replaceAggregatesForDate(localDate, rows)
            if (rows.any { it.packageName == "com.google.android.youtube" }) {
                youtubeInsightsRepository.reconcileUsageStats(localDate)
            }

            // Fill category totals for any session that ended on this day.
            sessions.filter { it.outcome != null }.forEach { session ->
                val sessionStartDate = Instant.ofEpochMilli(session.startedAtMs).atZone(zone).toLocalDate().toString()
                val sessionEndDate = Instant.ofEpochMilli(session.endedAtMs ?: session.startedAtMs)
                    .atZone(zone).toLocalDate().toString()
                val sessionIntervals = eachDate(sessionStartDate, sessionEndDate).flatMap { date ->
                    dao.intervalsForDate(date).map {
                        DatedUsageInterval(it.packageName, it.localDate, it.startMs, it.endMs)
                    }
                }
                val totals = KavachDailyAggregator.sessionCategoryTotals(
                    intervals = sessionIntervals,
                    window = session.startedAtMs..(session.endedAtMs ?: session.startedAtMs),
                    categoryOf = { categories[it] ?: AppCategory.UNCLASSIFIED },
                    youtubeViewing = dao.youtubeIntervalsBetween(sessionStartDate, sessionEndDate),
                )
                dao.upsertSession(
                    session.copy(
                        productiveSeconds = totals[AppCategory.PRODUCTIVE] ?: 0,
                        distractingSeconds = totals[AppCategory.DISTRACTING] ?: 0,
                        neutralSeconds = totals[AppCategory.NEUTRAL] ?: 0,
                        unclassifiedSeconds = totals[AppCategory.UNCLASSIFIED] ?: 0,
                        dataGap = session.dataGap ||
                            DataCoverage.fromWire(dao.coverageFor(localDate)?.status) != DataCoverage.COMPLETE,
                        synced = false,
                        updatedAtMs = System.currentTimeMillis(),
                    ),
                )
            }

            // Keep device-only intervals until the existing retention cleanup.
            // Cross-midnight sessions and later content reconciliation need them.
        }
    }

    private suspend fun recomputeSessionTotalsForRetainedWindow() {
        val start = LocalDate.now(zone).minusMonths(RETENTION_MONTHS).toString()
        dao.datesWithIntervals().filter { it >= start }.forEach { runCatching { aggregateDate(it) } }
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    suspend fun report(startDate: String, endDate: String): KavachAnalyticsReport =
        withContext(ioDispatcher) {
            val aggregates = dao.aggregatesBetween(startDate, endDate)
            val sessions = dao.sessionsBetween(startDate, endDate)
            val events = dao.eventsBetween(startDate, endDate)
            val blockedEvents = events.filter { it.type == KavachEventType.BLOCKED_ATTEMPT }
            val unlockStarts = events.filter { it.type == KavachEventType.QUICK_UNLOCK_STARTED }
            val unlockSeconds = events
                .filter { it.type == KavachEventType.QUICK_UNLOCK_ENDED }
                .sumOf { it.durationSeconds }
            val coverage = dao.coverageBetween(startDate, endDate)
                .associate { it.localDate to DataCoverage.fromWire(it.status) }
            val labels = dao.allClassifications().associate { it.packageName to it.appLabel }
            val youtubeByDate = dao.youtubeAggregatesBetween(startDate, endDate)
                .associateBy { it.localDate }

            var allDay = CategoryTotals()
            var duringKavach = CategoryTotals()
            val byPackage = mutableMapOf<String, AppUsageRow>()
            val byDate = mutableMapOf<String, DailyTrendPoint>()

            val pm = context.packageManager
            val ownPackage = context.packageName
            val launchableCache = mutableMapOf<String, Boolean>()

            aggregates.forEach { row ->
                val pkg = row.packageName
                val isSystemOrExcluded = if (UsageIntervalReconstructor.isExcluded(pkg, ownPackage, emptySet())) {
                    true
                } else {
                    launchableCache.getOrPut(pkg) {
                        pm.getLaunchIntentForPackage(pkg) == null
                    }
                }
                if (isSystemOrExcluded) return@forEach

                val category = AppCategory.fromWire(row.category)
                val youtube = youtubeByDate[row.localDate]
                    ?.takeIf { pkg == YOUTUBE_PACKAGE }
                val allDayContribution = youtube?.let {
                    youtubeCategoryTotals(
                        row.allDaySeconds,
                        it.productiveSeconds,
                        it.distractingSeconds,
                        it.shortsSeconds,
                        it.unidentifiedSeconds,
                        category,
                    )
                }
                val kavachContribution = youtube?.let {
                    youtubeCategoryTotals(
                        row.kavachSeconds,
                        it.protectedProductiveSeconds,
                        it.protectedDistractingSeconds,
                        it.protectedShortsSeconds,
                        it.protectedUnidentifiedSeconds,
                        category,
                    )
                }
                allDay = allDayContribution?.let(allDay::plus)
                    ?: allDay.add(category, row.allDaySeconds)
                duringKavach = kavachContribution?.let(duringKavach::plus)
                    ?: duringKavach.add(category, row.kavachSeconds)

                val existing = byPackage[row.packageName]
                byPackage[row.packageName] = AppUsageRow(
                    packageName = row.packageName,
                    appLabel = row.appLabel
                        ?: labels[row.packageName]
                        ?: context.appLabelOrNull(row.packageName)
                        ?: row.packageName,
                    category = category,
                    allDaySeconds = (existing?.allDaySeconds ?: 0) +
                        (allDayContribution?.totalSeconds ?: row.allDaySeconds),
                    kavachSeconds = (existing?.kavachSeconds ?: 0) +
                        (kavachContribution?.totalSeconds ?: row.kavachSeconds),
                    blockedAttempts = (existing?.blockedAttempts ?: 0) + row.blockedAttempts,
                    quickUnlockCount = (existing?.quickUnlockCount ?: 0) + row.quickUnlockCount,
                    allDayBreakdown = (existing?.allDayBreakdown ?: CategoryTotals()) +
                        (allDayContribution ?: CategoryTotals().add(category, row.allDaySeconds)),
                    kavachBreakdown = (existing?.kavachBreakdown ?: CategoryTotals()) +
                        (kavachContribution ?: CategoryTotals().add(category, row.kavachSeconds)),
                )

                val point = byDate[row.localDate] ?: DailyTrendPoint(
                    localDate = row.localDate,
                    allDay = CategoryTotals(),
                    duringKavach = CategoryTotals(),
                    blockedAttempts = 0,
                    quickUnlockCount = 0,
                    coverage = coverage[row.localDate] ?: DataCoverage.UNAVAILABLE,
                )
                byDate[row.localDate] = point.copy(
                    allDay = allDayContribution?.let(point.allDay::plus)
                        ?: point.allDay.add(category, row.allDaySeconds),
                    duringKavach = kavachContribution?.let(point.duringKavach::plus)
                        ?: point.duringKavach.add(category, row.kavachSeconds),
                    blockedAttempts = point.blockedAttempts + row.blockedAttempts,
                    quickUnlockCount = point.quickUnlockCount + row.quickUnlockCount,
                )
            }

            // Days inside the range with no usage rows still belong on the trend —
            // as a real zero when we measured them, and as "no data" when we didn't.
            eachDate(startDate, endDate).forEach { date ->
                if (byDate[date] == null) {
                    byDate[date] = DailyTrendPoint(
                        localDate = date,
                        allDay = CategoryTotals(),
                        duringKavach = CategoryTotals(),
                        blockedAttempts = 0,
                        quickUnlockCount = 0,
                        coverage = coverage[date] ?: DataCoverage.UNAVAILABLE,
                    )
                }
            }

            val missing = byDate.values
                .filter { it.coverage != DataCoverage.COMPLETE }
                .map { it.localDate }
                .sorted()

            KavachAnalyticsReport(
                rangeStart = startDate,
                rangeEnd = endDate,
                allDay = allDay,
                duringKavach = duringKavach,
                trend = byDate.values.sortedBy { it.localDate },
                apps = byPackage.values.sortedByDescending { it.allDaySeconds },
                sessions = sessions.map { it.toSummary() },
                // Raw events are the immediate source of truth for actions. Daily
                // aggregates can lag until collection runs and must not under-count today.
                blockedAttempts = blockedEvents.size,
                ekagraBlockedAttempts = blockedEvents.count { it.clientSessionId != null },
                alwaysOnBlockedAttempts = blockedEvents.count { it.clientSessionId == null },
                blockedAttemptsByPackage = blockedEvents
                    .mapNotNull { it.packageName }
                    .groupingBy { it }
                    .eachCount(),
                quickUnlockCount = unlockStarts.size,
                // Summed from the unlock-ended events rather than from session rows.
                // A session's own quickUnlockSeconds is rolled up from these same
                // events, so this is not a double count — but unlocks granted under
                // Always On belong to no session, and reading only sessions is what
                // made their duration permanently report as 0m.
                quickUnlockSeconds = unlockSeconds,
                completedSessions = sessions.count { it.outcome == KavachSessionOutcome.COMPLETED.wire },
                endedEarlySessions = sessions.count { it.outcome == KavachSessionOutcome.ENDED_EARLY.wire },
                interruptedSessions = sessions.count { it.outcome == KavachSessionOutcome.INTERRUPTED.wire },
                coverage = when {
                    missing.isEmpty() -> DataCoverage.COMPLETE
                    // Only "nothing could be measured" when every day genuinely had
                    // nothing. Previously any non-complete day counted here, so a
                    // single partially-measured day — which is what the first day
                    // after install always is — reported as unavailable, directly
                    // contradicting the usage listed underneath it.
                    byDate.values.all { it.coverage == DataCoverage.UNAVAILABLE } ->
                        DataCoverage.UNAVAILABLE
                    else -> DataCoverage.PARTIAL
                },
                daysMissingCoverage = missing,
            )
        }

    /** Apps the student uses a lot but has never categorised. */
    suspend fun topUnclassified(startDate: String, endDate: String, limit: Int = 5): List<AppUsageRow> =
        report(startDate, endDate).apps
            .filter { it.category == AppCategory.UNCLASSIFIED && it.allDaySeconds > 0 }
            .take(limit)

    fun hasUsageAccess(): Boolean = FocusShieldPermissionHelper.hasUsageStatsPermission(context)

    // ── Sync ─────────────────────────────────────────────────────────────────

    /**
     * Uploads whatever is unacknowledged. Records stay marked unsynced until the
     * server confirms them, so a failure retries rather than silently dropping data.
     *
     * @return true when everything queued was accepted.
     */
    suspend fun syncNow(): Boolean = withContext(ioDispatcher) {
        syncMutex.withLock {
            var allOk = true
            val timezone = zone.id

            val dirtyClassifications = dao.dirtyClassifications()
            if (dirtyClassifications.isNotEmpty()) {
                val result = safeApiCall {
                    api.uploadAppClassifications(
                        AppClassificationBatchRequest(
                            dirtyClassifications.map {
                                AppClassificationDto(
                                    packageName = it.packageName,
                                    category = it.category,
                                    appLabel = it.appLabel,
                                    updatedAt = Instant.ofEpochMilli(it.updatedAtMs).toString(),
                                )
                            },
                        ),
                    )
                }
                if (result is Resource.Success) {
                    dao.markClassificationsSynced(dirtyClassifications.map { it.packageName })
                } else {
                    allOk = false
                }
            }

            val aggregates = dao.unsyncedAggregates(UPLOAD_BATCH_SIZE)
            if (aggregates.isNotEmpty()) {
                val result = safeApiCall {
                    api.uploadDailyAggregates(
                        DailyAggregateBatchRequest(
                            timezone = timezone,
                            aggregates = aggregates.map { it.toUploadDto() },
                        ),
                    )
                }
                if (result is Resource.Success) {
                    aggregates.forEach {
                        dao.markAggregateSynced(it.localDate, it.packageName, it.updatedAtMs)
                    }
                } else {
                    allOk = false
                }
            }

            val sessions = dao.unsyncedSessions(UPLOAD_BATCH_SIZE).filter { it.outcome != null }
            if (sessions.isNotEmpty()) {
                val result = safeApiCall {
                    api.uploadSessions(
                        SessionBatchRequest(
                            timezone = timezone,
                            sessions = sessions.map { it.toUploadDto() },
                        ),
                    )
                }
                if (result is Resource.Success) {
                    dao.markSessionsSynced(sessions.map { it.clientSessionId })
                } else {
                    allOk = false
                }
            }

            val youtubeRows = dao.unsyncedYoutubeAggregates(UPLOAD_BATCH_SIZE)
            if (youtubeRows.isNotEmpty()) {
                val result = safeApiCall {
                    api.uploadYoutubeDailyAggregates(
                        YoutubeDailyAggregateBatchRequest(
                            timezone = timezone,
                            aggregates = youtubeRows.map {
                                YoutubeDailyAggregateUploadDto(
                                    localDate = it.localDate,
                                    productiveSeconds = it.productiveSeconds,
                                    distractingSeconds = it.distractingSeconds,
                                    shortsSeconds = it.shortsSeconds,
                                    unidentifiedSeconds = it.unidentifiedSeconds,
                                    protectedProductiveSeconds = it.protectedProductiveSeconds,
                                    protectedDistractingSeconds = it.protectedDistractingSeconds,
                                    protectedShortsSeconds = it.protectedShortsSeconds,
                                    protectedUnidentifiedSeconds = it.protectedUnidentifiedSeconds,
                                    coverage = it.coverage,
                                    updatedAt = Instant.ofEpochMilli(it.updatedAtMs).toString(),
                                )
                            },
                        ),
                    )
                }
                if (result is Resource.Success) {
                    youtubeRows.forEach { dao.markYoutubeAggregateSynced(it.localDate, it.updatedAtMs) }
                } else allOk = false
            }

            runCatching { pullClassifications() }
            dao.putMeta(KavachMetaEntity(META_LAST_SYNC_MS, System.currentTimeMillis().toString()))
            debugLog("syncNow ok=$allOk")
            allOk
        }
    }

    /** Merges the account's overrides in, so a second device inherits them. */
    private suspend fun pullClassifications() {
        val result = safeApiCall { api.getAppClassifications() }
        if (result !is Resource.Success) return
        val remote = result.data.classifications.orEmpty()
        if (remote.isEmpty()) return
        val local = dao.allClassifications().associateBy { it.packageName }
        val rows = remote.mapNotNull { dto ->
            val packageName = dto.packageName.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val remoteUpdated = dto.updatedAt?.let {
                runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
            } ?: 0L
            val current = local[packageName]
            // A newer local override always wins; the server is a merge partner, not
            // an authority that can overwrite what the student just chose.
            if (current != null && current.isUserOverride && current.updatedAtMs >= remoteUpdated) {
                return@mapNotNull null
            }
            AppClassificationEntity(
                packageName = packageName,
                category = AppCategory.fromWire(dto.category).wire,
                appLabel = dto.appLabel ?: current?.appLabel ?: context.appLabelOrNull(packageName),
                isUserOverride = true,
                updatedAtMs = remoteUpdated.takeIf { it > 0 } ?: System.currentTimeMillis(),
                dirty = false,
            )
        }
        if (rows.isEmpty()) return
        dao.upsertClassifications(rows)
        rows.forEach { dao.reclassifyAggregates(it.packageName, it.category, it.updatedAtMs) }
    }

    // ── Retention ────────────────────────────────────────────────────────────

    /**
     * Drops detailed records older than [RETENTION_MONTHS] after folding their
     * headline numbers into lifetime counters, so all-time totals never go down.
     */
    suspend fun prune(nowMs: Long = System.currentTimeMillis()) = withContext(ioDispatcher) {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val cutoffDate = today.minusMonths(RETENTION_MONTHS)
        val cutoffKey = cutoffDate.toString()
        val lastPruned = dao.meta(META_LAST_PRUNE_DATE)
        if (lastPruned == cutoffKey) return@withContext

        val expiring = dao.aggregatesBetween("0000-01-01", cutoffKey)
            .filter { it.localDate < cutoffKey }
        val expiringSessions = dao.sessionsBetween("0000-01-01", cutoffKey)
            .filter { it.localDate < cutoffKey }

        if (expiring.isNotEmpty() || expiringSessions.isNotEmpty()) {
            addLifetime(META_LIFETIME_KAVACH_SECONDS, expiring.sumOf { it.kavachSeconds.toLong() })
            addLifetime(META_LIFETIME_ALL_DAY_SECONDS, expiring.sumOf { it.allDaySeconds.toLong() })
            addLifetime(META_LIFETIME_BLOCKED_ATTEMPTS, expiring.sumOf { it.blockedAttempts.toLong() })
            addLifetime(META_LIFETIME_QUICK_UNLOCKS, expiring.sumOf { it.quickUnlockCount.toLong() })
            addLifetime(META_LIFETIME_SESSIONS, expiringSessions.size.toLong())
            addLifetime(
                META_LIFETIME_COMPLETED_SESSIONS,
                expiringSessions.count { it.outcome == KavachSessionOutcome.COMPLETED.wire }.toLong(),
            )
        }

        val cutoffMs = cutoffDate.atStartOfDay(zone).toInstant().toEpochMilli()
        dao.deleteAggregatesBefore(cutoffKey)
        dao.deleteCoverageBefore(cutoffKey)
        dao.deleteSessionsBefore(cutoffMs)
        dao.deleteEventsBefore(cutoffMs)
        dao.deleteProtectionWindowsBefore(cutoffMs)
        // Raw intervals are far shorter-lived than the 12-month window: anything
        // older than a couple of days has already been aggregated.
        val rawCutoff = Instant.ofEpochMilli(nowMs - RAW_INTERVAL_RETENTION_MS).atZone(zone)
            .toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        dao.deleteIntervalsBefore(rawCutoff)
        dao.putMeta(KavachMetaEntity(META_LAST_PRUNE_DATE, cutoffKey))
    }

    suspend fun lifetimeTotals(): Map<String, Long> = withContext(ioDispatcher) {
        LIFETIME_KEYS.associateWith { dao.meta(it)?.toLongOrNull() ?: 0L }
    }

    private suspend fun addLifetime(key: String, delta: Long) {
        if (delta <= 0L) return
        val current = dao.meta(key)?.toLongOrNull() ?: 0L
        dao.putMeta(KavachMetaEntity(key, (current + delta).toString()))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun eachDate(startDate: String, endDate: String): List<String> {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return emptyList()
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return emptyList()
        if (end.isBefore(start)) return emptyList()
        val dates = mutableListOf<String>()
        var cursor = start
        var guard = 0
        while (!cursor.isAfter(end) && guard++ < MAX_RANGE_DAYS) {
            dates += cursor.toString()
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    private fun KavachSessionEntity.toSummary() = KavachSessionSummary(
        clientSessionId = clientSessionId,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        plannedSeconds = plannedSeconds,
        actualSeconds = actualSeconds,
        mode = if (mode == "beast") KavachSessionMode.NORMAL else mode,
        outcome = KavachSessionOutcome.fromWire(outcome),
        blockedAttempts = blockedAttempts,
        quickUnlockCount = quickUnlockCount,
        quickUnlockSeconds = quickUnlockSeconds,
        totals = CategoryTotals(
            productiveSeconds = productiveSeconds,
            distractingSeconds = distractingSeconds,
            neutralSeconds = neutralSeconds,
            unclassifiedSeconds = unclassifiedSeconds,
        ),
        permissionLost = permissionLost,
        dataGap = dataGap,
    )

    private fun DailyAppAggregateEntity.toUploadDto() = DailyAggregateUploadDto(
        localDate = localDate,
        packageName = packageName,
        appLabel = appLabel,
        category = category,
        allDaySeconds = allDaySeconds,
        kavachSeconds = kavachSeconds,
        blockedAttempts = blockedAttempts,
        quickUnlockCount = quickUnlockCount,
        updatedAt = Instant.ofEpochMilli(updatedAtMs).toString(),
    )

    private fun KavachSessionEntity.toUploadDto() = SessionUploadDto(
        clientSessionId = clientSessionId,
        startedAt = Instant.ofEpochMilli(startedAtMs).toString(),
        endedAt = endedAtMs?.let { Instant.ofEpochMilli(it).toString() },
        localDate = localDate,
        plannedSeconds = plannedSeconds,
        actualSeconds = actualSeconds,
        mode = if (mode == "beast") KavachSessionMode.NORMAL else mode,
        outcome = outcome,
        blockedAttempts = blockedAttempts,
        quickUnlockCount = quickUnlockCount,
        quickUnlockSeconds = quickUnlockSeconds,
        productiveSeconds = productiveSeconds,
        distractingSeconds = distractingSeconds,
        neutralSeconds = neutralSeconds,
        unclassifiedSeconds = unclassifiedSeconds,
        permissionLost = permissionLost,
        dataGap = dataGap,
    )

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("KavachAnalytics", message)
    }

    companion object {
        const val RETENTION_MONTHS = 12L
        private const val UPLOAD_BATCH_SIZE = 200
        private const val MAX_RANGE_DAYS = 400
        private const val RAW_INTERVAL_RETENTION_MS = 3L * 24 * 60 * 60 * 1000

        const val META_LAST_SYNC_MS = "last_sync_ms"
        const val META_LAST_PRUNE_DATE = "last_prune_date"
        const val META_LIFETIME_KAVACH_SECONDS = "lifetime_kavach_seconds"
        const val META_LIFETIME_ALL_DAY_SECONDS = "lifetime_all_day_seconds"
        const val META_LIFETIME_BLOCKED_ATTEMPTS = "lifetime_blocked_attempts"
        const val META_LIFETIME_QUICK_UNLOCKS = "lifetime_quick_unlocks"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val META_LIFETIME_SESSIONS = "lifetime_sessions"
        const val META_LIFETIME_COMPLETED_SESSIONS = "lifetime_completed_sessions"

        private val LIFETIME_KEYS = listOf(
            META_LIFETIME_KAVACH_SECONDS,
            META_LIFETIME_ALL_DAY_SECONDS,
            META_LIFETIME_BLOCKED_ATTEMPTS,
            META_LIFETIME_QUICK_UNLOCKS,
            META_LIFETIME_SESSIONS,
            META_LIFETIME_COMPLETED_SESSIONS,
        )
    }
}
