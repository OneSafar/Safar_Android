package com.safarparmar.app.feature.kavachanalytics.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A student's category for one app. Seeded from SAFAR's defaults; a user override
 * always wins and is what syncs to their account.
 */
@Entity(tableName = "kavach_app_classification")
data class AppClassificationEntity(
    @PrimaryKey val packageName: String,
    val category: String,
    /** Last known display label — kept so an uninstalled app still has a name. */
    val appLabel: String?,
    val isUserOverride: Boolean,
    val updatedAtMs: Long,
    /** True while a user override still needs uploading. */
    val dirty: Boolean = false,
)

/**
 * One reconstructed foreground visit. These never leave the phone: they are deleted
 * as soon as the day they belong to has been aggregated.
 */
@Entity(
    tableName = "kavach_raw_usage_interval",
    indices = [Index("localDate"), Index(value = ["packageName", "startMs"])],
)
data class RawUsageIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
    val localDate: String,
)

/** One Kavach session, from activation to a recorded outcome. */
@Entity(tableName = "kavach_session", indices = [Index("startedAtMs"), Index("localDate")])
data class KavachSessionEntity(
    @PrimaryKey val clientSessionId: String,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val mode: String,
    /** Null while the session is still open. */
    val outcome: String?,
    val localDate: String,
    val blockedAttempts: Int = 0,
    val quickUnlockCount: Int = 0,
    val quickUnlockSeconds: Int = 0,
    val productiveSeconds: Int = 0,
    val distractingSeconds: Int = 0,
    val neutralSeconds: Int = 0,
    val unclassifiedSeconds: Int = 0,
    val permissionLost: Boolean = false,
    val dataGap: Boolean = false,
    val synced: Boolean = false,
    val updatedAtMs: Long,
)

/**
 * Raw Kavach events, written the instant they happen so a process death cannot lose
 * them. Rolled into [KavachSessionEntity] and [DailyAppAggregateEntity] at
 * finalisation, then pruned with the rest of the detailed data.
 */
@Entity(
    tableName = "kavach_event",
    indices = [Index("clientSessionId"), Index("atMs"), Index("localDate")],
)
data class KavachEventEntity(
    /** Client-generated UUID — retries can never create a duplicate. */
    @PrimaryKey val id: String,
    val clientSessionId: String?,
    val type: String,
    val packageName: String?,
    val atMs: Long,
    val localDate: String,
    /** Quick-unlock duration in seconds (selected on start, actual on end). */
    val durationSeconds: Int = 0,
    val consumed: Boolean = false,
)

/** Per-day, per-app rollup. This is the only usage shape that ever syncs. */
@Entity(
    tableName = "kavach_daily_app_aggregate",
    primaryKeys = ["localDate", "packageName"],
    indices = [Index("localDate"), Index("synced")],
)
data class DailyAppAggregateEntity(
    val localDate: String,
    val packageName: String,
    val appLabel: String?,
    val category: String,
    val allDaySeconds: Int,
    val kavachSeconds: Int,
    val blockedAttempts: Int,
    val quickUnlockCount: Int,
    val updatedAtMs: Long,
    val synced: Boolean = false,
)

/**
 * A stretch of time during which Kavach was actually guarding the student.
 *
 * Separate from [KavachSessionEntity] on purpose. A session is a *timed study
 * sitting* and carries an outcome; protection is simply "was Kavach blocking".
 * Always On produces protection with no session at all, and folding it into
 * sessions would pollute the completed/ended-early split with all-day stretches
 * that have no notion of completing.
 *
 * [endMs] is heart-beaten forward while a window is live, so a process death
 * leaves a window that ends at the last moment we know Kavach was really running
 * rather than one that appears to run forever.
 */
@Entity(
    tableName = "kavach_protection_window",
    indices = [Index("startMs"), Index("localDate"), Index("source")],
)
data class ProtectionWindowEntity(
    @PrimaryKey val id: String,
    val startMs: Long,
    val endMs: Long,
    val source: String,
    val localDate: String,
    /** False once the window has been deliberately ended. */
    val isOpen: Boolean,
)

/** Where a protection window came from. */
object ProtectionSource {
    const val SESSION = "session"
    const val ALWAYS_ON = "always_on"
}

/** Whether a given day's numbers are trustworthy. */
@Entity(tableName = "kavach_day_coverage")
data class DayCoverageEntity(
    @PrimaryKey val localDate: String,
    val status: String,
    val updatedAtMs: Long,
)

/** Key/value scratch: usage-processing watermark, last prune, last sync. */
@Entity(tableName = "kavach_meta")
data class KavachMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** A channel observed in YouTube. Channel names and choices never leave this device. */
@Entity(tableName = "youtube_channel", indices = [Index("lastSeenAtMs")])
data class YoutubeChannelEntity(
    @PrimaryKey val channelKey: String,
    val displayName: String,
    val isProductive: Boolean,
    val lastSeenAtMs: Long,
)

/** Device-only classified YouTube viewing interval. Video titles are never persisted. */
@Entity(tableName = "youtube_viewing_interval", indices = [Index("localDate"), Index("startMs")])
data class YoutubeViewingIntervalEntity(
    @PrimaryKey val id: String,
    val startMs: Long,
    val endMs: Long,
    val localDate: String,
    val channelKey: String?,
    val category: String,
    val isShorts: Boolean,
)

/** Recoverable checkpoint for the interval currently being observed by Accessibility. */
@Entity(tableName = "youtube_open_interval")
data class YoutubeOpenIntervalEntity(
    @PrimaryKey val id: Int = 1,
    val startedAtMs: Long,
    val heartbeatAtMs: Long,
    val channelKey: String?,
    val category: String,
    val isShorts: Boolean,
)

/** Only this derived, channel-free row is eligible for backend synchronization. */
@Entity(tableName = "youtube_daily_aggregate")
data class YoutubeDailyAggregateEntity(
    @PrimaryKey val localDate: String,
    val productiveSeconds: Int,
    val distractingSeconds: Int,
    val shortsSeconds: Int,
    val unidentifiedSeconds: Int,
    val protectedProductiveSeconds: Int,
    val protectedDistractingSeconds: Int,
    val protectedShortsSeconds: Int,
    val protectedUnidentifiedSeconds: Int,
    val coverage: String,
    val updatedAtMs: Long,
    val synced: Boolean = false,
)
