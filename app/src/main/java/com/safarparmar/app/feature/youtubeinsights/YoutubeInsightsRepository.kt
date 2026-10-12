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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
    private val trackingEvents = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        scope.launch {
            for (event in trackingEvents) {
                runCatching { event() }.onFailure {
                    android.util.Log.e("YoutubeAnalytics", "Could not record viewing interval", it)
                }
            }
        }
    }
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val enabled: StateFlow<Boolean> = dataStore.youtubeInsightsEnabled
        .stateIn(scope, SharingStarted.Eagerly, false)

    private data class Open(
        val startedAtMs: Long,
        var heartbeatAtMs: Long,
        val channelKey: String?,
        val category: String,
        val shorts: Boolean,
        val source: String,
    )
    private var open: Open? = null
    private var lastPersistedHeartbeatMs = 0L

    /** One ordered queue for both services; leaving one cannot close the other's interval. */
    fun recordViewing(channelKey: String?, category: String, shorts: Boolean, source: String = SOURCE_STUDY) {
        val atMs = System.currentTimeMillis()
        trackingEvents.trySend { observeViewing(channelKey, category, shorts, atMs, source) }
    }

    fun finishViewing(source: String = SOURCE_STUDY) {
        val atMs = System.currentTimeMillis()
        trackingEvents.trySend { stopViewing(atMs, source) }
    }

    suspend fun flushTracking() {
        val done = CompletableDeferred<Unit>()
        trackingEvents.send {
            try {
                mutex.withLock {
                    val current = open
                    if (current != null) {
                        closeLocked(current.heartbeatAtMs)
                        open = current.copy(startedAtMs = current.heartbeatAtMs)
                        persistOpenLocked(open!!, force = true)
                    }
                }
                done.complete(Unit)
            } catch (error: Exception) {
                done.completeExceptionally(error)
            }
        }
        done.await()
    }

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

    /**
     * Starts or heartbeats the single YouTube activity currently visible. A changed
     * channel/category closes the previous interval first, so time can belong to
     * exactly one Kavach bucket.
     */
    suspend fun observeViewing(
        channelKey: String?,
        category: String,
        shorts: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        source: String = SOURCE_STUDY,
    ) = mutex.withLock {
        val safeCategory = category.takeIf {
            it == CATEGORY_PRODUCTIVE || it == CATEGORY_DISTRACTING ||
                it == CATEGORY_SHORTS || it == CATEGORY_UNIDENTIFIED
        } ?: CATEGORY_UNIDENTIFIED
        val current = open
        if (current?.source == SOURCE_KAVACH && source != SOURCE_KAVACH) return@withLock
        if (current != null && current.channelKey == channelKey &&
            current.category == safeCategory && current.shorts == shorts && current.source == source &&
            nowMs - current.heartbeatAtMs <= HEARTBEAT_GRACE_MS
        ) {
            current.heartbeatAtMs = nowMs
            persistOpenLocked(current)
            return@withLock
        }
        closeLocked(nowMs)
        Open(nowMs, nowMs, channelKey, safeCategory, shorts, source).also {
            open = it
            persistOpenLocked(it, force = true)
        }
    }

    /** Closes measured YouTube activity as soon as YouTube is no longer visible. */
    suspend fun stopViewing(nowMs: Long = System.currentTimeMillis(), source: String = SOURCE_STUDY) = mutex.withLock {
        if (open?.source != source) return@withLock
        closeLocked(nowMs)
    }

    suspend fun channel(channelKey: String): YoutubeChannelEntity? = dao.youtubeChannel(channelKey)

    /** Creates the onboarding suggestions once without overwriting an existing choice. */
    suspend fun seedStarterChannels(nowMs: Long = System.currentTimeMillis()) {
        STARTER_CHANNELS.forEach { name ->
            val key = YoutubeChannelIdentity.normalize(name)
            if (dao.youtubeChannel(key) == null) {
                dao.upsertYoutubeChannel(YoutubeChannelEntity(key, name, false, nowMs))
            }
        }
        consolidateStarterAliases()
    }

    suspend fun hasShownBlockedChannelNotification(channelKey: String): Boolean =
        dataStore.hasYoutubeChannelNotificationShown(channelKey)

    suspend fun markBlockedChannelNotificationShown(channelKey: String) {
        dataStore.markYoutubeChannelNotificationShown(channelKey)
    }

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

    private suspend fun resolveChannelKey(channelName: String): String =
        resolveChannelKey(listOf(channelName))

    private suspend fun resolveChannelKey(channelNames: Collection<String>): String {
        val normalizedNames = channelNames.map(YoutubeChannelIdentity::normalize).distinct()
        val identities = normalizedNames.map(YoutubeChannelIdentity::identityKey).toSet()
        val candidates = dao.youtubeChannels().filter {
            YoutubeChannelIdentity.identityKey(it.channelKey) in identities
        }
        return candidates.firstOrNull { it.isProductive }?.channelKey
            ?: candidates.firstOrNull { it.channelKey in STARTER_CHANNEL_KEYS }?.channelKey
            ?: candidates.firstOrNull()?.channelKey
            ?: normalizedNames.first()
    }

    /** Merges @handle variants such as SAFAR_PARMAR into the seeded Safar Parmar row. */
    private suspend fun consolidateStarterAliases() {
        val rows = dao.youtubeChannels()
        STARTER_CHANNELS.forEach { starterName ->
            val targetKey = YoutubeChannelIdentity.normalize(starterName)
            val aliases = rows.filter {
                it.channelKey != targetKey &&
                    YoutubeChannelIdentity.identityKey(it.channelKey) ==
                    YoutubeChannelIdentity.identityKey(targetKey)
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

    /** Rebuild observed content without inventing a category for off-mode time. */
    suspend fun reconcileUsageStats(localDate: String) = mutex.withLock {
        aggregateDate(localDate)
        // Only observed Study Mode / Quick Unlock intervals override the app's
        // category. Unobserved time is NOT invented as Others: it may be time
        // when Study Mode was off. Browsing is explicitly recorded as Others.
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
        const val SOURCE_STUDY = "study"
        const val SOURCE_KAVACH = "kavach_unlock"
        val STARTER_CHANNELS = listOf(
            "Physics Wallah",
            "Unacademy",
            "Parmar Academy",
            "Parmar SSC",
            "Safar Parmar",
            "Khan Academy",
        )
        val STARTER_CHANNEL_KEYS = STARTER_CHANNELS.map(YoutubeChannelIdentity::normalize).toSet()
        const val CATEGORY_PRODUCTIVE = "productive"
        const val CATEGORY_DISTRACTING = "distracting"
        const val CATEGORY_SHORTS = "shorts"
        const val CATEGORY_UNIDENTIFIED = "unidentified"
        private const val MIN_INTERVAL_MS = 1_000L
        private const val HEARTBEAT_GRACE_MS = 5_000L
        private const val PERSIST_HEARTBEAT_MS = 2_000L
    }
}
