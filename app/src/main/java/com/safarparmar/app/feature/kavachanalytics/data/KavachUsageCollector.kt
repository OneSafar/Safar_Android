package com.safarparmar.app.feature.kavachanalytics.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.feature.kavachanalytics.data.local.DayCoverageEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachMetaEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.RawUsageIntervalEntity
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
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
 * Incrementally reads Android's `UsageEvents` stream and stores the reconstructed
 * foreground intervals on the device.
 *
 * Runs from a WorkManager job and again whenever SAFAR opens, so a window the OS
 * never let us run in is reconciled on the next launch rather than lost. Nothing
 * here ever leaves the phone: raw intervals are deleted once their day has been
 * aggregated.
 */
@Singleton
class KavachUsageCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: KavachAnalyticsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val collectMutex = Mutex()

    private val homePackages: Set<String> by lazy { resolveHomePackages() }

    /**
     * Collects everything since the watermark.
     *
     * @return the local dates whose raw intervals changed, so the caller can
     *   re-aggregate exactly those days.
     */
    suspend fun collect(nowMs: Long = System.currentTimeMillis()): Set<String> =
        withContext(ioDispatcher) {
            collectMutex.withLock { collectLocked(nowMs) }
        }

    private suspend fun collectLocked(nowMs: Long): Set<String> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone)

        if (!FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
            // Never write zeros for a day we simply could not measure.
            markCoverage(today.toString(), DataCoverage.UNAVAILABLE, nowMs)
            return emptySet()
        }

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (manager == null) {
            markCoverage(today.toString(), DataCoverage.UNAVAILABLE, nowMs)
            return emptySet()
        }

        val storedWatermark = dao.meta(META_WATERMARK_MS)?.toLongOrNull()
        // The platform only retains a few days of events; never scan further back
        // than that or we produce partial days that look like real drops in usage.
        val floor = nowMs - MAX_LOOKBACK_MS
        val windowStart = (storedWatermark ?: (nowMs - INITIAL_LOOKBACK_MS)).coerceAtLeast(floor)
        if (nowMs - windowStart < MIN_WINDOW_MS) return emptySet()

        // Either we've never collected before (so the earlier part of today was never
        // measured) or the watermark is older than the platform retains. Both mean the
        // days this window spans are partial, and must not be reported as complete.
        val gapDetected = storedWatermark == null || storedWatermark < floor

        val transitions = runCatching { readTransitions(manager, windowStart, nowMs) }
            .getOrElse {
                debugLog("UsageEvents query failed: ${it.javaClass.simpleName}")
                markCoverage(today.toString(), DataCoverage.PARTIAL, nowMs)
                return emptySet()
            }

        val ownPackage = context.packageName
        val pm = context.packageManager
        val launchableCache = mutableMapOf<String, Boolean>()

        val intervals = UsageIntervalReconstructor.reconstruct(
            transitions = transitions,
            windowStartMs = windowStart,
            windowEndMs = nowMs,
            isExcluded = { pkg ->
                if (UsageIntervalReconstructor.isExcluded(pkg, ownPackage, homePackages)) {
                    true
                } else {
                    launchableCache.getOrPut(pkg) {
                        pm.getLaunchIntentForPackage(pkg) == null
                    }
                }
            },
        )

        val dated = UsageIntervalReconstructor.splitAll(intervals, zone)
            .filter { it.durationMs >= MIN_INTERVAL_MS }

        if (dated.isNotEmpty()) {
            dao.insertIntervals(
                dated.map {
                    RawUsageIntervalEntity(
                        packageName = it.packageName,
                        startMs = it.startMs,
                        endMs = it.endMs,
                        localDate = it.localDate,
                    )
                },
            )
        }

        dao.putMeta(KavachMetaEntity(META_WATERMARK_MS, nowMs.toString()))

        val touchedDates = dated.map { it.localDate }.toMutableSet()
        // Every day the window spanned gets a coverage record, including days with
        // no usage at all — that is a real "you used nothing", not missing data.
        datesSpanned(windowStart, nowMs, zone).forEach { date ->
            touchedDates += date
            val status = if (gapDetected) DataCoverage.PARTIAL else DataCoverage.COMPLETE
            markCoverageIfNotWorse(date, status, nowMs)
        }
        return touchedDates
    }

    /** Marks the current day as un-measurable, e.g. Usage Access was just revoked. */
    suspend fun markUnavailableNow(nowMs: Long = System.currentTimeMillis()) =
        withContext(ioDispatcher) {
            val date = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault())
            markCoverage(date.toString(), DataCoverage.UNAVAILABLE, nowMs)
        }

    private fun readTransitions(
        manager: UsageStatsManager,
        fromMs: Long,
        toMs: Long,
    ): List<ForegroundTransition> {
        val events = manager.queryEvents(fromMs, toMs)
        val event = UsageEvents.Event()
        val transitions = mutableListOf<ForegroundTransition>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName.isBlank()) continue
            val foreground = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            val background = event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED))
            if (!foreground && !background) continue
            transitions += ForegroundTransition(
                packageName = packageName,
                timestampMs = event.timeStamp,
                movedToForeground = foreground,
            )
        }
        return transitions
    }

    private suspend fun markCoverage(localDate: String, status: DataCoverage, nowMs: Long) {
        dao.upsertCoverage(DayCoverageEntity(localDate, status.wire, nowMs))
    }

    /**
     * Coverage only ever degrades within a day: once a stretch of a day was missed,
     * a later successful window must not upgrade it back to "complete".
     */
    private suspend fun markCoverageIfNotWorse(localDate: String, status: DataCoverage, nowMs: Long) {
        val existing = DataCoverage.fromWire(dao.coverageFor(localDate)?.status)
        val existingRank = coverageRank(existing)
        val incomingRank = coverageRank(status)
        val effective = if (dao.coverageFor(localDate) == null || incomingRank < existingRank) {
            status
        } else {
            existing
        }
        markCoverage(localDate, effective, nowMs)
    }

    private fun coverageRank(status: DataCoverage): Int = when (status) {
        DataCoverage.UNAVAILABLE -> 0
        DataCoverage.PARTIAL -> 1
        DataCoverage.COMPLETE -> 2
    }

    private fun datesSpanned(fromMs: Long, toMs: Long, zone: ZoneId): List<String> {
        val start = LocalDate.ofInstant(Instant.ofEpochMilli(fromMs), zone)
        val end = LocalDate.ofInstant(Instant.ofEpochMilli(toMs), zone)
        val dates = mutableListOf<String>()
        var cursor = start
        var guard = 0
        while (!cursor.isAfter(end) && guard++ < 40) {
            dates += cursor.toString()
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    private fun resolveHomePackages(): Set<String> {
        val pm = context.packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val queried = runCatching {
            pm.queryIntentActivities(homeIntent, 0).mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())
        val resolved = runCatching {
            pm.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        }.getOrNull()
        return (queried + listOfNotNull(resolved)).toSet()
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "KavachUsage"
        const val META_WATERMARK_MS = "usage_watermark_ms"

        /** Android keeps roughly a week of raw events; stay inside that. */
        private const val MAX_LOOKBACK_MS = 5L * 24 * 60 * 60 * 1000
        private const val INITIAL_LOOKBACK_MS = 12L * 60 * 60 * 1000
        private const val MIN_WINDOW_MS = 30_000L

        /**
         * Visits shorter than this are polling noise (an app briefly resumed behind
         * the block screen), not screen time the student would recognise.
         */
        private const val MIN_INTERVAL_MS = 1_000L
    }
}

/** Convenience for resolving a package's display label, with uninstalls handled. */
internal fun Context.appLabelOrNull(packageName: String): String? = runCatching {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
}.getOrElse {
    if (it is PackageManager.NameNotFoundException) null else null
}
