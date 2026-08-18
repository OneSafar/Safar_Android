package com.safarparmar.app.feature.youtubeinsights

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.kavachanalytics.data.UsageIntervalReconstructor
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeChannelEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeDailyAggregateEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeOpenIntervalEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeViewingIntervalEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeInsightsRepository @Inject constructor(
    private val dataStore: SafarDataStore,
    private val dao: KavachAnalyticsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val enabled: StateFlow<Boolean> = dataStore.youtubeInsightsEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)
    val shortsScope: StateFlow<String> = dataStore.youtubeShortsBlockScope
        .stateIn(scope, SharingStarted.Eagerly, YoutubeBlockScope.OFF.wire)
    val channelScope: StateFlow<String> = dataStore.youtubeChannelBlockScope
        .stateIn(scope, SharingStarted.Eagerly, YoutubeBlockScope.OFF.wire)

    private data class Open(
        val startedAtMs: Long,
        var heartbeatAtMs: Long,
        val channelKey: String?,
        val category: String,
        val shorts: Boolean,
    )
    private var open: Open? = null
    private var lastPersistedHeartbeatMs = 0L

    suspend fun observe(detection: YoutubeDetection?, nowMs: Long = System.currentTimeMillis()) = mutex.withLock {
        if (!enabled.value || detection == null ||
            detection.kind == YoutubeContentKind.NON_PLAYBACK || detection.kind == YoutubeContentKind.UNKNOWN
        ) {
            closeLocked(nowMs)
            return@withLock
        }

        // Channel discovery is independent from time measurement. YouTube may
        // expose a visible Play control while opening/pausing a video; the content
        // still needs to be saved before shouldBlock() sends the student back.
        val key = detection.channelName?.let { resolveChannelKey(it) }
        if (key != null) {
            val existing = dao.youtubeChannel(key)
            dao.upsertYoutubeChannel(
                YoutubeChannelEntity(
                    channelKey = key,
                    displayName = detection.channelName,
                    isProductive = existing?.isProductive ?: false,
                    lastSeenAtMs = nowMs,
                ),
            )
        }
        if (!detection.isPlaying) {
            closeLocked(nowMs)
            return@withLock
        }
        val category = when {
            detection.kind == YoutubeContentKind.SHORTS -> CATEGORY_SHORTS
            key == null -> CATEGORY_UNIDENTIFIED
            dao.youtubeChannel(key)?.isProductive == true -> CATEGORY_PRODUCTIVE
            else -> CATEGORY_DISTRACTING
        }
        val current = open
        if (current != null && current.channelKey == key && current.category == category &&
            current.shorts == (detection.kind == YoutubeContentKind.SHORTS)
        ) {
            current.heartbeatAtMs = nowMs
            persistOpenLocked(current)
        } else {
            closeLocked(nowMs)
            open = Open(nowMs, nowMs, key, category, detection.kind == YoutubeContentKind.SHORTS)
            persistOpenLocked(open!!, force = true)
        }
    }

    suspend fun stop(nowMs: Long = System.currentTimeMillis()) = mutex.withLock { closeLocked(nowMs) }

    /** Finalises a process-killed interval only up to its last durable heartbeat. */
    suspend fun recoverStaleInterval() = mutex.withLock {
        if (open != null) return@withLock
        val stale = dao.youtubeOpenInterval() ?: return@withLock
        writeIntervalParts(
            startedAtMs = stale.startedAtMs,
            endMs = stale.heartbeatAtMs,
            channelKey = stale.channelKey,
            category = stale.category,
            shorts = stale.isShorts,
        )
        dao.deleteYoutubeOpenInterval()
    }

    suspend fun channels(): List<YoutubeChannelEntity> = dao.youtubeChannels()

    suspend fun channel(channelKey: String): YoutubeChannelEntity? = dao.youtubeChannel(channelKey)

    /** Creates the onboarding suggestions once without overwriting an existing choice. */
    suspend fun seedStarterChannels(nowMs: Long = System.currentTimeMillis()) {
        STARTER_CHANNELS.forEach { name ->
            val key = YoutubeUiParser.normalizeChannel(name)
            if (dao.youtubeChannel(key) == null) {
                dao.upsertYoutubeChannel(YoutubeChannelEntity(key, name, false, nowMs))
            }
        }
        consolidateStarterAliases()
    }

    suspend fun shouldShowBlockedChannelNotification(channelKey: String): Boolean =
        dataStore.markYoutubeChannelNotificationShown(channelKey)

    suspend fun setProductive(channelKey: String, productive: Boolean) {
        dao.setYoutubeChannelProductive(channelKey, productive)
        dao.reclassifyYoutubeIntervals(channelKey, if (productive) CATEGORY_PRODUCTIVE else CATEGORY_DISTRACTING)
        dao.youtubeIntervalDates().forEach { aggregateDate(it) }
    }

    suspend fun isProductive(channelName: String?): Boolean {
        val key = channelName?.let { resolveChannelKey(it) } ?: return false
        return dao.youtubeChannel(key)?.isProductive == true
    }

    /** Maps compact YouTube handles (for example @SAFARPARMAR) to seeded display-name keys. */
    suspend fun channelKey(channelName: String): String = resolveChannelKey(channelName)

    private suspend fun resolveChannelKey(channelName: String): String {
        val normalized = YoutubeUiParser.normalizeChannel(channelName)
        val identity = compactIdentity(normalized)
        val candidates = dao.youtubeChannels().filter { compactIdentity(it.channelKey) == identity }
        return candidates.firstOrNull { it.isProductive }?.channelKey
            ?: candidates.firstOrNull { it.channelKey in STARTER_CHANNEL_KEYS }?.channelKey
            ?: candidates.firstOrNull()?.channelKey
            ?: normalized
    }

    /** Merges @handle variants such as SAFAR_PARMAR into the seeded Safar Parmar row. */
    private suspend fun consolidateStarterAliases() {
        val rows = dao.youtubeChannels()
        STARTER_CHANNELS.forEach { starterName ->
            val targetKey = YoutubeUiParser.normalizeChannel(starterName)
            val aliases = rows.filter {
                it.channelKey != targetKey && compactIdentity(it.channelKey) == compactIdentity(targetKey)
            }
            if (aliases.isEmpty()) return@forEach
            val target = dao.youtubeChannel(targetKey) ?: return@forEach
            dao.upsertYoutubeChannel(
                target.copy(
                    isProductive = target.isProductive || aliases.any { it.isProductive },
                    lastSeenAtMs = maxOf(target.lastSeenAtMs, aliases.maxOf { it.lastSeenAtMs }),
                ),
            )
            aliases.forEach { alias ->
                dao.reassignYoutubeIntervals(alias.channelKey, targetKey)
                dao.reassignYoutubeOpenInterval(alias.channelKey, targetKey)
                dao.deleteYoutubeChannel(alias.channelKey)
            }
        }
    }

    suspend fun shouldBlock(detection: YoutubeDetection, protectedNow: Boolean = com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository.Snapshot.active): Boolean = when {
        detection.kind == YoutubeContentKind.SHORTS ->
            YoutubeBlockScope.fromWire(shortsScope.value).applies(protectedNow)
        detection.kind == YoutubeContentKind.VIDEO && detection.channelName != null &&
            !isProductive(detection.channelName) ->
            YoutubeBlockScope.fromWire(channelScope.value).applies(protectedNow)
        else -> false
    }

    /** True when content-level rules, rather than generic app blocking, own YouTube. */
    fun ownsYoutubeBlocking(): Boolean = enabled.value &&
        (YoutubeBlockScope.fromWire(shortsScope.value) != YoutubeBlockScope.OFF ||
            YoutubeBlockScope.fromWire(channelScope.value) != YoutubeBlockScope.OFF)

    suspend fun aggregateDate(localDate: String) {
        val intervals = dao.youtubeIntervalsBetween(localDate, localDate)
        val day = LocalDate.parse(localDate)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val protection = UsageIntervalReconstructor.mergeWindows(
            dao.protectionWindowsOverlapping(start, end).map { it.startMs..it.endMs },
        )
        fun overlapMs(row: YoutubeViewingIntervalEntity): Long = protection.sumOf { window ->
            (minOf(row.endMs, window.last) - maxOf(row.startMs, window.first)).coerceAtLeast(0L)
        }
        fun seconds(category: String, protected: Boolean): Int = intervals.filter { it.category == category }
            .sumOf { if (protected) overlapMs(it) else it.endMs - it.startMs }
            .div(1000L).toInt().coerceAtLeast(0)
        val now = System.currentTimeMillis()
        dao.upsertYoutubeDailyAggregate(
            YoutubeDailyAggregateEntity(
                localDate = localDate,
                productiveSeconds = seconds(CATEGORY_PRODUCTIVE, false),
                distractingSeconds = seconds(CATEGORY_DISTRACTING, false),
                shortsSeconds = seconds(CATEGORY_SHORTS, false),
                unidentifiedSeconds = seconds(CATEGORY_UNIDENTIFIED, false),
                protectedProductiveSeconds = seconds(CATEGORY_PRODUCTIVE, true),
                protectedDistractingSeconds = seconds(CATEGORY_DISTRACTING, true),
                protectedShortsSeconds = seconds(CATEGORY_SHORTS, true),
                protectedUnidentifiedSeconds = seconds(CATEGORY_UNIDENTIFIED, true),
                coverage = "complete",
                updatedAtMs = now,
                synced = false,
            ),
        )
    }

    /** Assigns any YouTube time Accessibility could not classify to Unidentified. */
    suspend fun reconcileUsageStats(
        localDate: String,
        allDayYoutubeSeconds: Int,
        protectedYoutubeSeconds: Int,
    ) {
        aggregateDate(localDate)
        val row = dao.youtubeAggregatesBetween(localDate, localDate).firstOrNull() ?: return
        val classified = row.productiveSeconds + row.distractingSeconds + row.shortsSeconds
        val protectedClassified = row.protectedProductiveSeconds +
            row.protectedDistractingSeconds + row.protectedShortsSeconds
        dao.upsertYoutubeDailyAggregate(
            row.copy(
                unidentifiedSeconds = maxOf(row.unidentifiedSeconds, allDayYoutubeSeconds - classified),
                protectedUnidentifiedSeconds = maxOf(
                    row.protectedUnidentifiedSeconds,
                    protectedYoutubeSeconds - protectedClassified,
                ),
                coverage = if (allDayYoutubeSeconds > classified + row.unidentifiedSeconds) "partial" else row.coverage,
                updatedAtMs = System.currentTimeMillis(),
                synced = false,
            ),
        )
    }

    suspend fun totals(startDate: String, endDate: String): Pair<YoutubeTotals, List<YoutubeDailyAggregateEntity>> {
        val rows = dao.youtubeAggregatesBetween(startDate, endDate)
        return YoutubeTotals(
            productiveSeconds = rows.sumOf { it.productiveSeconds },
            distractingSeconds = rows.sumOf { it.distractingSeconds },
            shortsSeconds = rows.sumOf { it.shortsSeconds },
            unidentifiedSeconds = rows.sumOf { it.unidentifiedSeconds },
            protectedProductiveSeconds = rows.sumOf { it.protectedProductiveSeconds },
            protectedDistractingSeconds = rows.sumOf { it.protectedDistractingSeconds },
            protectedShortsSeconds = rows.sumOf { it.protectedShortsSeconds },
            protectedUnidentifiedSeconds = rows.sumOf { it.protectedUnidentifiedSeconds },
        ) to rows
    }

    private suspend fun closeLocked(nowMs: Long) {
        val current = open ?: return
        // Never bridge a service/process gap: cap at the last verified heartbeat.
        val end = minOf(nowMs, current.heartbeatAtMs + HEARTBEAT_GRACE_MS)
        writeIntervalParts(current.startedAtMs, end, current.channelKey, current.category, current.shorts)
        dao.deleteYoutubeOpenInterval()
        open = null
        lastPersistedHeartbeatMs = 0L
    }

    private suspend fun persistOpenLocked(current: Open, force: Boolean = false) {
        if (!force && current.heartbeatAtMs - lastPersistedHeartbeatMs < PERSIST_HEARTBEAT_MS) return
        dao.upsertYoutubeOpenInterval(
            YoutubeOpenIntervalEntity(
                startedAtMs = current.startedAtMs,
                heartbeatAtMs = current.heartbeatAtMs,
                channelKey = current.channelKey,
                category = current.category,
                isShorts = current.shorts,
            ),
        )
        lastPersistedHeartbeatMs = current.heartbeatAtMs
    }

    private suspend fun writeIntervalParts(
        startedAtMs: Long,
        endMs: Long,
        channelKey: String?,
        category: String,
        shorts: Boolean,
    ) {
        if (endMs - startedAtMs < MIN_INTERVAL_MS) return
        var cursor = startedAtMs
        val dates = linkedSetOf<String>()
        while (cursor < endMs) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val partEnd = minOf(endMs, boundary)
            if (partEnd - cursor >= MIN_INTERVAL_MS) {
                dao.insertYoutubeInterval(
                    YoutubeViewingIntervalEntity(
                        id = UUID.randomUUID().toString(),
                        startMs = cursor,
                        endMs = partEnd,
                        localDate = date.toString(),
                        channelKey = channelKey,
                        category = category,
                        isShorts = shorts,
                    ),
                )
                dates += date.toString()
            }
            cursor = partEnd
        }
        dates.forEach { aggregateDate(it) }
    }

    companion object {
        val STARTER_CHANNELS = listOf(
            "Physics Wallah",
            "Unacademy",
            "Parmar Academy",
            "Parmar SSC",
            "Safar Parmar",
            "Khan Academy",
        )
        val STARTER_CHANNEL_KEYS = STARTER_CHANNELS.map(YoutubeUiParser::normalizeChannel).toSet()
        const val CATEGORY_PRODUCTIVE = "productive"
        const val CATEGORY_DISTRACTING = "distracting"
        const val CATEGORY_SHORTS = "shorts"
        const val CATEGORY_UNIDENTIFIED = "unidentified"
        private const val MIN_INTERVAL_MS = 1_000L
        private const val HEARTBEAT_GRACE_MS = 5_000L
        private const val PERSIST_HEARTBEAT_MS = 2_000L
        private fun compactIdentity(value: String): String =
            value.lowercase().filter { it.isLetterOrDigit() }
    }
}
