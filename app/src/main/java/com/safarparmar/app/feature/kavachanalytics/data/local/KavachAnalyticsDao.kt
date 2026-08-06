package com.safarparmar.app.feature.kavachanalytics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KavachAnalyticsDao {

    // ── Classifications ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassifications(rows: List<AppClassificationEntity>)

    @Query("SELECT * FROM kavach_app_classification")
    suspend fun allClassifications(): List<AppClassificationEntity>

    @Query("SELECT * FROM kavach_app_classification ORDER BY appLabel COLLATE NOCASE")
    fun observeClassifications(): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM kavach_app_classification WHERE dirty = 1")
    suspend fun dirtyClassifications(): List<AppClassificationEntity>

    @Query("UPDATE kavach_app_classification SET dirty = 0 WHERE packageName IN (:packages)")
    suspend fun markClassificationsSynced(packages: List<String>)

    @Query("SELECT * FROM kavach_app_classification WHERE packageName = :packageName")
    suspend fun classificationFor(packageName: String): AppClassificationEntity?

    // ── Raw intervals (device-only) ──────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIntervals(rows: List<RawUsageIntervalEntity>)

    @Query("SELECT * FROM kavach_raw_usage_interval WHERE localDate = :localDate ORDER BY startMs")
    suspend fun intervalsForDate(localDate: String): List<RawUsageIntervalEntity>

    @Query("SELECT DISTINCT localDate FROM kavach_raw_usage_interval ORDER BY localDate")
    suspend fun datesWithIntervals(): List<String>

    @Query("DELETE FROM kavach_raw_usage_interval WHERE localDate = :localDate")
    suspend fun deleteIntervalsForDate(localDate: String)

    @Query("DELETE FROM kavach_raw_usage_interval WHERE endMs < :cutoffMs")
    suspend fun deleteIntervalsBefore(cutoffMs: Long)

    // ── Sessions ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: KavachSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<KavachSessionEntity>)

    @Query("SELECT * FROM kavach_session WHERE clientSessionId = :id")
    suspend fun sessionById(id: String): KavachSessionEntity?

    @Query("SELECT * FROM kavach_session WHERE outcome IS NULL ORDER BY startedAtMs DESC")
    suspend fun openSessions(): List<KavachSessionEntity>

    @Query("SELECT * FROM kavach_session WHERE localDate BETWEEN :startDate AND :endDate ORDER BY startedAtMs DESC")
    suspend fun sessionsBetween(startDate: String, endDate: String): List<KavachSessionEntity>

    @Query(
        "SELECT * FROM kavach_session WHERE endedAtMs IS NOT NULL " +
            "AND (:fromMs IS NULL OR endedAtMs >= :fromMs) AND endedAtMs <= :toMs ORDER BY startedAtMs",
    )
    suspend fun sessionsOverlapping(fromMs: Long?, toMs: Long): List<KavachSessionEntity>

    @Query("SELECT * FROM kavach_session WHERE synced = 0 ORDER BY startedAtMs LIMIT :limit")
    suspend fun unsyncedSessions(limit: Int): List<KavachSessionEntity>

    @Query("UPDATE kavach_session SET synced = 1 WHERE clientSessionId IN (:ids)")
    suspend fun markSessionsSynced(ids: List<String>)

    @Query("DELETE FROM kavach_session WHERE startedAtMs < :cutoffMs")
    suspend fun deleteSessionsBefore(cutoffMs: Long)

    // ── Events ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: KavachEventEntity)

    @Query("SELECT * FROM kavach_event WHERE clientSessionId = :sessionId ORDER BY atMs")
    suspend fun eventsForSession(sessionId: String): List<KavachEventEntity>

    @Query("SELECT * FROM kavach_event WHERE localDate = :localDate ORDER BY atMs")
    suspend fun eventsForDate(localDate: String): List<KavachEventEntity>

    /**
     * The newest unconsumed event of [type], regardless of session.
     *
     * A quick unlock is a device-wide grace window — there is only ever one open at
     * a time — and it can be granted with no session running at all under Always On.
     * Looking it up by session is what left those unlocks permanently open and their
     * duration unrecorded.
     */
    @Query("SELECT * FROM kavach_event WHERE type = :type AND consumed = 0 ORDER BY atMs DESC LIMIT 1")
    suspend fun lastOpenEventOfType(type: String): KavachEventEntity?

    @Query("SELECT * FROM kavach_event WHERE localDate BETWEEN :startDate AND :endDate ORDER BY atMs")
    suspend fun eventsBetween(startDate: String, endDate: String): List<KavachEventEntity>

    @Query("UPDATE kavach_event SET consumed = 1 WHERE id = :id")
    suspend fun markEventConsumed(id: String)

    @Query("DELETE FROM kavach_event WHERE atMs < :cutoffMs")
    suspend fun deleteEventsBefore(cutoffMs: Long)

    // ── Daily aggregates ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAggregates(rows: List<DailyAppAggregateEntity>)

    @Query("DELETE FROM kavach_daily_app_aggregate WHERE localDate = :localDate")
    suspend fun deleteAggregatesForDate(localDate: String)

    @Query("SELECT * FROM kavach_daily_app_aggregate WHERE localDate BETWEEN :startDate AND :endDate")
    suspend fun aggregatesBetween(startDate: String, endDate: String): List<DailyAppAggregateEntity>

    @Query("SELECT * FROM kavach_daily_app_aggregate WHERE localDate BETWEEN :startDate AND :endDate")
    fun observeAggregatesBetween(startDate: String, endDate: String): Flow<List<DailyAppAggregateEntity>>

    @Query("SELECT * FROM kavach_daily_app_aggregate WHERE synced = 0 ORDER BY localDate LIMIT :limit")
    suspend fun unsyncedAggregates(limit: Int): List<DailyAppAggregateEntity>

    @Query(
        "UPDATE kavach_daily_app_aggregate SET synced = 1 " +
            "WHERE localDate = :localDate AND packageName = :packageName AND updatedAtMs <= :updatedAtMs",
    )
    suspend fun markAggregateSynced(localDate: String, packageName: String, updatedAtMs: Long)

    @Query("SELECT DISTINCT packageName FROM kavach_daily_app_aggregate")
    suspend fun aggregatedPackages(): List<String>

    /**
     * Reclassifies history in place. Called when the student changes an app's
     * category so every retained date range stays internally consistent.
     */
    @Query("UPDATE kavach_daily_app_aggregate SET category = :category, synced = 0, updatedAtMs = :updatedAtMs WHERE packageName = :packageName")
    suspend fun reclassifyAggregates(packageName: String, category: String, updatedAtMs: Long)

    @Query("DELETE FROM kavach_daily_app_aggregate WHERE localDate < :cutoffDate")
    suspend fun deleteAggregatesBefore(cutoffDate: String)

    // ── Protection windows ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProtectionWindow(window: ProtectionWindowEntity)

    @Query("SELECT * FROM kavach_protection_window WHERE isOpen = 1 AND source = :source ORDER BY startMs DESC LIMIT 1")
    suspend fun openProtectionWindow(source: String): ProtectionWindowEntity?

    @Query("SELECT * FROM kavach_protection_window WHERE isOpen = 1")
    suspend fun allOpenProtectionWindows(): List<ProtectionWindowEntity>

    /** Windows that touch [fromMs]..[toMs] at all — used to compute protected time. */
    @Query("SELECT * FROM kavach_protection_window WHERE endMs >= :fromMs AND startMs <= :toMs")
    suspend fun protectionWindowsOverlapping(fromMs: Long, toMs: Long): List<ProtectionWindowEntity>

    @Query("UPDATE kavach_protection_window SET endMs = :endMs WHERE id = :id")
    suspend fun touchProtectionWindow(id: String, endMs: Long)

    @Query("UPDATE kavach_protection_window SET endMs = :endMs, isOpen = 0 WHERE id = :id")
    suspend fun closeProtectionWindow(id: String, endMs: Long)

    @Query("UPDATE kavach_protection_window SET isOpen = 0 WHERE source = :source AND isOpen = 1")
    suspend fun closeOpenProtectionWindows(source: String)

    @Query("DELETE FROM kavach_protection_window WHERE endMs < :cutoffMs")
    suspend fun deleteProtectionWindowsBefore(cutoffMs: Long)

    // ── Coverage ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoverage(row: DayCoverageEntity)

    @Query("SELECT * FROM kavach_day_coverage WHERE localDate BETWEEN :startDate AND :endDate")
    suspend fun coverageBetween(startDate: String, endDate: String): List<DayCoverageEntity>

    @Query("SELECT * FROM kavach_day_coverage WHERE localDate = :localDate")
    suspend fun coverageFor(localDate: String): DayCoverageEntity?

    @Query("DELETE FROM kavach_day_coverage WHERE localDate < :cutoffDate")
    suspend fun deleteCoverageBefore(cutoffDate: String)

    // ── Meta ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(row: KavachMetaEntity)

    @Query("SELECT value FROM kavach_meta WHERE key = :key")
    suspend fun meta(key: String): String?

    @Transaction
    suspend fun replaceAggregatesForDate(localDate: String, rows: List<DailyAppAggregateEntity>) {
        deleteAggregatesForDate(localDate)
        upsertAggregates(rows)
    }
}
