package com.safarparmar.app.feature.kavachanalytics.data

import android.content.Context
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachEventEntity
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachSessionEntity
import com.safarparmar.app.feature.kavachanalytics.domain.KavachEventType
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionMode
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes Kavach lifecycle events the instant they happen.
 *
 * Everything lands in Room before anything is rolled up, so a process death between
 * "session started" and "session completed" still leaves a recoverable record — and
 * that recovery is reported as *interrupted*, never as a student failure.
 */
@Singleton
class KavachAnalyticsRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: KavachAnalyticsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    /** Fire-and-forget wrapper for the service/UI call sites, which are not suspending. */
    private fun record(block: suspend () -> Unit) {
        scope.launch { runCatching { block() }.onFailure { debugLog("record failed: $it") } }
    }

    // ── Session lifecycle ────────────────────────────────────────────────────

    fun sessionStarted(strictMode: Boolean, plannedSeconds: Int) = record {
        startSession(strictMode, plannedSeconds)
    }

    suspend fun startSession(
        strictMode: Boolean,
        plannedSeconds: Int,
        nowMs: Long = System.currentTimeMillis(),
    ): String = withContext(ioDispatcher) {
        mutex.withLock {
            val open = dao.openSessions().firstOrNull()
            if (open != null) {
                // Kavach deactivates during a Normal-Mode break and re-activates when
                // focus resumes. That is one session to the student, so keep it.
                if (plannedSeconds > open.plannedSeconds) {
                    dao.upsertSession(open.copy(plannedSeconds = plannedSeconds, updatedAtMs = nowMs))
                }
                return@withLock open.clientSessionId
            }

            val id = "kavach-${UUID.randomUUID()}"
            dao.upsertSession(
                KavachSessionEntity(
                    clientSessionId = id,
                    startedAtMs = nowMs,
                    endedAtMs = null,
                    plannedSeconds = plannedSeconds.coerceAtLeast(0),
                    actualSeconds = 0,
                    mode = if (strictMode) KavachSessionMode.BEAST else KavachSessionMode.NORMAL,
                    outcome = null,
                    localDate = localDate(nowMs),
                    updatedAtMs = nowMs,
                ),
            )
            insertEvent(KavachEventType.SESSION_STARTED, id, null, nowMs)
            debugLog("session started $id")
            id
        }
    }

    fun sessionCompleted(actualSeconds: Int) =
        record { endSession(KavachSessionOutcome.COMPLETED, actualSeconds) }

    fun sessionEndedEarly(actualSeconds: Int) =
        record { endSession(KavachSessionOutcome.ENDED_EARLY, actualSeconds) }

    fun sessionInterrupted(actualSeconds: Int) =
        record { endSession(KavachSessionOutcome.INTERRUPTED, actualSeconds) }

    /**
     * Finalises the open session, folding its events into the summary row.
     * A no-op when no session is open, so duplicate end calls cannot double-count.
     */
    suspend fun endSession(
        outcome: KavachSessionOutcome,
        actualSeconds: Int,
        nowMs: Long = System.currentTimeMillis(),
    ): String? = withContext(ioDispatcher) {
        mutex.withLock {
            val open = dao.openSessions().firstOrNull() ?: return@withLock null
            closeOpenUnlocks(open.clientSessionId, nowMs)
            val finalised = rollUp(open, outcome, actualSeconds, nowMs)
            dao.upsertSession(finalised)
            insertEvent(outcome.toEventType(), open.clientSessionId, null, nowMs)
            debugLog("session ${open.clientSessionId} -> ${outcome.wire}")
            open.clientSessionId
        }
    }

    /**
     * Finalises sessions left open by a process or device failure. Called on startup:
     * an active session that never reached a normal end is *interrupted*.
     */
    suspend fun recoverStaleSessions(nowMs: Long = System.currentTimeMillis()): Int =
        withContext(ioDispatcher) {
            mutex.withLock {
                val stale = dao.openSessions()
                stale.forEach { session ->
                    closeOpenUnlocks(session.clientSessionId, nowMs)
                    val elapsed = ((nowMs - session.startedAtMs) / 1000L).toInt().coerceAtLeast(0)
                    val actual = if (session.plannedSeconds > 0) {
                        elapsed.coerceAtMost(session.plannedSeconds)
                    } else {
                        elapsed
                    }
                    dao.upsertSession(
                        rollUp(session, KavachSessionOutcome.INTERRUPTED, actual, nowMs)
                            .copy(dataGap = true),
                    )
                    insertEvent(KavachEventType.SESSION_INTERRUPTED, session.clientSessionId, null, nowMs)
                }
                if (stale.isNotEmpty()) debugLog("recovered ${stale.size} interrupted session(s)")
                stale.size
            }
        }

    /** Kavach could not actually block this session — flag it, don't hide it. */
    fun permissionLost() = record {
        mutex.withLock {
            val open = dao.openSessions().firstOrNull() ?: return@withLock
            dao.upsertSession(open.copy(permissionLost = true, updatedAtMs = System.currentTimeMillis()))
        }
    }

    // ── In-session events ────────────────────────────────────────────────────

    /**
     * One attempt per foreground visit. The caller (TimerService) already debounces
     * repeated polls and overlay relaunches for the same visit, so this is recorded
     * exactly where that first hit is counted.
     */
    fun blockedAttempt(packageName: String) = record {
        if (packageName.isBlank()) return@record
        val sessionId = dao.openSessions().firstOrNull()?.clientSessionId
        insertEvent(KavachEventType.BLOCKED_ATTEMPT, sessionId, packageName, System.currentTimeMillis())
    }

    fun quickUnlockStarted(packageName: String, selectedSeconds: Int) = record {
        val nowMs = System.currentTimeMillis()
        val sessionId = dao.openSessions().firstOrNull()?.clientSessionId
        if (sessionId != null) closeOpenUnlocks(sessionId, nowMs)
        insertEvent(
            type = KavachEventType.QUICK_UNLOCK_STARTED,
            sessionId = sessionId,
            packageName = packageName.takeIf { it.isNotBlank() },
            atMs = nowMs,
            durationSeconds = selectedSeconds.coerceAtLeast(0),
        )
    }

    /** Ends any unlock whose window has expired. Safe to call often. */
    fun settleExpiredUnlocks() = record {
        val nowMs = System.currentTimeMillis()
        val sessionId = dao.openSessions().firstOrNull()?.clientSessionId ?: return@record
        val open = dao.lastOpenEvent(sessionId, KavachEventType.QUICK_UNLOCK_STARTED) ?: return@record
        if (nowMs >= open.atMs + open.durationSeconds * 1000L) {
            closeOpenUnlocks(sessionId, nowMs)
        }
    }

    /**
     * Closes an in-flight quick unlock with the duration actually elapsed — the
     * selected window is a ceiling, not what the student really spent.
     */
    private suspend fun closeOpenUnlocks(sessionId: String, nowMs: Long) {
        val open = dao.lastOpenEvent(sessionId, KavachEventType.QUICK_UNLOCK_STARTED) ?: return
        val cap = open.atMs + open.durationSeconds * 1000L
        val endedAt = minOf(nowMs, cap)
        val actualSeconds = ((endedAt - open.atMs) / 1000L).toInt().coerceAtLeast(0)
        dao.markEventConsumed(open.id)
        insertEvent(
            type = KavachEventType.QUICK_UNLOCK_ENDED,
            sessionId = sessionId,
            packageName = open.packageName,
            atMs = endedAt,
            durationSeconds = actualSeconds,
            consumed = true,
        )
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private suspend fun rollUp(
        session: KavachSessionEntity,
        outcome: KavachSessionOutcome,
        actualSeconds: Int,
        nowMs: Long,
    ): KavachSessionEntity {
        val events = dao.eventsForSession(session.clientSessionId)
        val blocked = events.count { it.type == KavachEventType.BLOCKED_ATTEMPT }
        val unlockStarts = events.count { it.type == KavachEventType.QUICK_UNLOCK_STARTED }
        val unlockSeconds = events
            .filter { it.type == KavachEventType.QUICK_UNLOCK_ENDED }
            .sumOf { it.durationSeconds }
        val elapsed = ((nowMs - session.startedAtMs) / 1000L).toInt().coerceAtLeast(0)
        return session.copy(
            endedAtMs = nowMs,
            actualSeconds = actualSeconds.takeIf { it > 0 } ?: elapsed,
            outcome = outcome.wire,
            blockedAttempts = blocked,
            quickUnlockCount = unlockStarts,
            quickUnlockSeconds = unlockSeconds,
            synced = false,
            updatedAtMs = nowMs,
        )
    }

    private suspend fun insertEvent(
        type: String,
        sessionId: String?,
        packageName: String?,
        atMs: Long,
        durationSeconds: Int = 0,
        consumed: Boolean = false,
    ) {
        dao.insertEvent(
            KavachEventEntity(
                id = UUID.randomUUID().toString(),
                clientSessionId = sessionId,
                type = type,
                packageName = packageName,
                atMs = atMs,
                localDate = localDate(atMs),
                durationSeconds = durationSeconds,
                consumed = consumed,
            ),
        )
    }

    private fun localDate(atMs: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(atMs), ZoneId.systemDefault()).toString()

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("KavachAnalytics", message)
    }

    companion object {
        /** For TimerService and other non-Hilt call sites. */
        fun from(context: Context): KavachAnalyticsRecorder =
            EntryPointAccessors
                .fromApplication(context.applicationContext, KavachAnalyticsEntryPoint::class.java)
                .kavachAnalyticsRecorder()
    }
}

private fun KavachSessionOutcome.toEventType(): String = when (this) {
    KavachSessionOutcome.COMPLETED -> KavachEventType.SESSION_COMPLETED
    KavachSessionOutcome.ENDED_EARLY -> KavachEventType.SESSION_ENDED_EARLY
    KavachSessionOutcome.INTERRUPTED -> KavachEventType.SESSION_INTERRUPTED
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KavachAnalyticsEntryPoint {
    fun kavachAnalyticsRecorder(): KavachAnalyticsRecorder
}
