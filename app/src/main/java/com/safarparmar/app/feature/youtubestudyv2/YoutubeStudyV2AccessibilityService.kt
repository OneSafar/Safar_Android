package com.safarparmar.app.feature.youtubestudyv2

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.feature.youtubeinsights.YoutubeInsightsRepository
import com.safarparmar.app.ui.ekagra.focusshield.KavachBlockOverlay
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

/**
 * Channel decisions follow a video tap or a restored, identified watch page.
 * A confirmed Shorts viewer is always blocked locally and never needs identity
 * resolution or a backend request.
 */
@AndroidEntryPoint
class YoutubeStudyV2AccessibilityService : AccessibilityService() {
    @Inject lateinit var repository: YoutubeStudyV2Repository
    @Inject lateinit var preferences: YoutubeStudyV2Preferences
    @Inject lateinit var youtubeInsightsRepository: YoutubeInsightsRepository
    @Inject lateinit var focusShieldRepository: FocusShieldRepository

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = YoutubeStudyV2Session()
    private var classificationObserver: Job? = null
    private val overlay by lazy { KavachBlockOverlay(this, accessibilityOverlay = true) }
    private var firstStableRead: YoutubeV2Observation? = null
    @Volatile private var lastEvaluatedKey: String? = null
    @Volatile private var evaluationGeneration = 0L
    private var blockOverlayVisible = false
    private var ownerMissingSinceMs: Long? = null
    private var pendingYoutubeClickAtMs: Long? = null
    private var analyticsOpen = false
    private var analyticsChannelId: String? = null
    private var analyticsCategory: String? = null
    private var analyticsShorts = false
    private var quickUnlockWasActive = false
    private var scheduledQuickUnlockUntilMs: Long? = null
    private var lastWatchedChannelId: String? = null
    private var lastWatchedDisplayName: String? = null
    private var lastWatchedExactHandle: String? = null
    private var lastWatchedClassification: YoutubeChannelClassification? = null

    private var lastPipAnalyticsAtMs = 0L

    private val pipMonitor = object : Runnable {
        override fun run() {
            if (preferences.enabled.value) enforceFloatingPlayback()
            handler.postDelayed(this, PIP_CHECK_MS)
        }
    }

    private val quickUnlockExpireRunnable = Runnable {
        scheduledQuickUnlockUntilMs = null
        quickUnlockWasActive = false
        if (!preferences.enabled.value) return@Runnable
        if (enforceFloatingPlayback()) return@Runnable
        if (isYoutubeVisible()) {
            handleQuickUnlockExpired()
        }
    }

    private fun syncQuickUnlockTimer() {
        val unlockActive = isYoutubeQuickUnlockActive()
        if (unlockActive) {
            val graceUntilMs = FocusShieldRepository.ShieldPrefs.getGraceUntilMs(this)
            if (scheduledQuickUnlockUntilMs != graceUntilMs) {
                scheduledQuickUnlockUntilMs = graceUntilMs
                quickUnlockWasActive = true
                handler.removeCallbacks(quickUnlockExpireRunnable)
                val delayMs = (graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
                handler.postDelayed(quickUnlockExpireRunnable, delayMs)
            }
        } else {
            handler.removeCallbacks(quickUnlockExpireRunnable)
            scheduledQuickUnlockUntilMs = null
        }
    }

    private var scheduledReadAtMs = Long.MAX_VALUE
    private val debounce = Runnable {
        scheduledReadAtMs = Long.MAX_VALUE
        captureFirstRead()
    }
    private val heartbeat = object : Runnable {
        override fun run() {
            if (preferences.enabled.value) {
                preferences.recordAccessibilityHeartbeat()
            }
            handler.postDelayed(this, HEARTBEAT_MS)
        }
    }
    private val analyticsHeartbeat = object : Runnable {
        override fun run() {
            handler.postDelayed(this, ANALYTICS_HEARTBEAT_MS)
            if (!preferences.enabled.value) {
                stopAnalytics()
                return
            }
            if (enforceFloatingPlayback()) return
            val youtubeVisible = isYoutubeVisible()
            val unlockActive = isYoutubeQuickUnlockActive()
            val justExpiredMinutes = FocusShieldRepository.ShieldPrefs.consumeQuickUnlockJustExpired(this@YoutubeStudyV2AccessibilityService)
            val quickUnlockJustEnded = (quickUnlockWasActive || scheduledQuickUnlockUntilMs != null || justExpiredMinutes > 0) && !unlockActive

            if (quickUnlockJustEnded) {
                quickUnlockWasActive = false
                scheduledQuickUnlockUntilMs = null
                handler.removeCallbacks(quickUnlockExpireRunnable)
                if (youtubeVisible) {
                    handleQuickUnlockExpired(justExpiredMinutes)
                    return
                } else {
                    stopAnalytics()
                }
            } else if (unlockActive) {
                quickUnlockWasActive = true
                syncQuickUnlockTimer()
            }

            if (!youtubeVisible) {
                stopAnalytics()
            } else if (isKavachYoutubeUnlock()) {
                recordAnalytics(
                    channelId = null,
                    category = YoutubeInsightsRepository.CATEGORY_DISTRACTING,
                    shorts = false,
                )
            } else {
                val observation = readObservation()
                if (!observation.watchScreenConfirmed) {
                    lastEvaluatedKey = null
                    recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_UNIDENTIFIED, false)
                } else if (observation.watchScreenConfirmed &&
                    observation.kind == YoutubeV2ContentKind.SHORTS
                ) {
                    blockShorts()
                } else {
                    if (lastEvaluatedKey != observation.stableKey || !analyticsOpen) {
                        if (!analyticsOpen) recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_UNIDENTIFIED, false)
                        scheduleRead(0L)
                    } else {
                        heartbeatAnalytics()
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        classificationObserver?.cancel()
        classificationObserver = scope.launch(Dispatchers.Main.immediate) {
            repository.classifications.collect { classifications ->
                lastWatchedChannelId?.let { channelId ->
                    lastWatchedClassification = YoutubeChannelClassification.fromWire(
                        classifications.firstOrNull { it.channelId == channelId }?.classification,
                    )
                }
                // Invalidate both cached and in-flight decisions after a saved rule
                // change. The next stable read uses the updated local allowlist.
                evaluationGeneration++
                firstStableRead = null
                stopAnalytics()
                if (preferences.enabled.value && isYoutubeVisible()) scheduleRead(0L)
            }
        }
        if (preferences.enabled.value) {
            preferences.recordAccessibilityHeartbeat()
            YoutubeStudyV2GuardService.start(this)
        }
        handler.removeCallbacks(pipMonitor)
        handler.post(pipMonitor)
        handler.removeCallbacks(heartbeat)
        handler.post(heartbeat)
        handler.removeCallbacks(analyticsHeartbeat)
        handler.post(analyticsHeartbeat)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!preferences.enabled.value) {
            stopAnalytics()
            YoutubeStudyV2GuardService.stop(this)
            return
        }
        // PiP belongs to a separate window even while another app has focus.
        if (enforceFloatingPlayback()) return
        if (event?.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
            stopAnalytics()
            return
        }
        preferences.recordAccessibilityHeartbeat()

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val clickAtMs = SystemClock.elapsedRealtime()
            if (isVideoCardClick(event)) {
                beginVideoTap(clickAtMs)
                scheduleRead(CLICK_TRANSITION_MS)
            } else {
                // YouTube does not consistently label video-card clicks across
                // devices and experiments. Arm the click, then let a confirmed
                // watch page create the session; a feed click remains inert.
                if (session.state == YoutubeStudyV2Session.State.BROWSING) {
                    pendingYoutubeClickAtMs = clickAtMs
                }
                scheduleRead(SHORTS_PROBE_MS)
            }
            return
        }

        // Scroll events are intentionally not subscribed to. Content changes on
        // feeds/previews may schedule a parse, but tap-gating makes them inert.
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> scheduleRead(DEBOUNCE_MS)
        }
    }

    override fun onInterrupt() = stopRuntime()

    override fun onDestroy() {
        stopRuntime()
        scope.cancel()
        super.onDestroy()
    }

    private fun stopRuntime() {
        classificationObserver?.cancel()
        classificationObserver = null
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = Long.MAX_VALUE
        handler.removeCallbacks(heartbeat)
        handler.removeCallbacks(analyticsHeartbeat)
        handler.removeCallbacks(pipMonitor)
        handler.removeCallbacks(quickUnlockExpireRunnable)
        scheduledQuickUnlockUntilMs = null
        evaluationGeneration++
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        pendingYoutubeClickAtMs = null
        blockOverlayVisible = false
        session.onBrowsing()
        overlay.dismiss()
        stopAnalytics()
    }

    private fun scheduleRead(delayMs: Long) {
        val dueAt = SystemClock.elapsedRealtime() + delayMs
        // Frequent player updates must not keep postponing the pending check.
        if (scheduledReadAtMs <= dueAt) return
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = dueAt
        handler.postDelayed(debounce, delayMs)
    }

    private fun captureFirstRead() {
        if (!preferences.enabled.value) return stopAnalytics()
        if (enforceFloatingPlayback()) return
        if (!isYoutubeVisible()) return stopAnalytics()
        val observation = readObservation()
        if (observation.watchScreenConfirmed && lastEvaluatedKey != observation.stableKey) {
            // Autoplay can change the video without a tap. Until the new owner
            // is evaluated, do not carry a productive decision into PiP.
            lastWatchedClassification = null
        }
        if (observation.kind == YoutubeV2ContentKind.SHORTS && observation.watchScreenConfirmed) {
            blockShorts()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val pendingClick = pendingYoutubeClickAtMs
        if (
            observation.kind == YoutubeV2ContentKind.VIDEO &&
            observation.watchScreenConfirmed &&
            session.state == YoutubeStudyV2Session.State.BROWSING &&
            pendingClick != null &&
            now - pendingClick <= GENERIC_CLICK_TRANSITION_WINDOW_MS
        ) {
            beginVideoTap(pendingClick)
        }
        if (!observation.watchScreenConfirmed) {
            if (isKavachYoutubeUnlock()) {
                recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_DISTRACTING, false)
            } else {
                recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_UNIDENTIFIED, false)
            }
            session.acceptStable(observation, now)
            firstStableRead = null
            if (!blockOverlayVisible) overlay.dismiss()
            if (pendingClick != null && now - pendingClick <= GENERIC_CLICK_TRANSITION_WINDOW_MS) {
                scheduleRead(DEBOUNCE_MS)
            } else {
                pendingYoutubeClickAtMs = null
                lastEvaluatedKey = null
            }
            return
        }
        if (observation.kind == YoutubeV2ContentKind.VIDEO && !observation.hasOwnerEvidence) {
            val now = SystemClock.elapsedRealtime()
            val missingSince = ownerMissingSinceMs ?: now.also { ownerMissingSinceMs = it }
            // Wait only while the owner row is absent. A stable display name
            // can already resolve a unique local identity; ambiguous names block.
            if (observation.adPlaying || now - missingSince < OWNER_EVIDENCE_WAIT_MS) {
                firstStableRead = null
                scheduleRead(OWNER_EVIDENCE_RETRY_MS)
                return
            }
        } else {
            ownerMissingSinceMs = null
        }

        // Fast-path evaluation: owner evidence is confirmed
        firstStableRead = observation
        val generation = evaluationGeneration
        handler.postDelayed({ captureConfirmation(observation, generation) }, STABILITY_GAP_MS)
    }

    private fun captureConfirmation(first: YoutubeV2Observation, generation: Long) {
        if (generation != evaluationGeneration || !preferences.enabled.value) return
        val second = readObservation()
        if (second.stableKey != first.stableKey || !second.watchScreenConfirmed) {
            firstStableRead = null
            scheduleRead(DEBOUNCE_MS)
            return
        }
        if (!session.acceptStable(second, SystemClock.elapsedRealtime())) return
        if (session.isAlreadyEvaluated(second.stableKey, lastEvaluatedKey, analyticsOpen)) return
        lastEvaluatedKey = second.stableKey
        decide(second, generation)
    }

    private fun decide(observation: YoutubeV2Observation, generation: Long) {
        android.util.Log.d("YTCM", "🔍 decide() called — handle=${observation.exactHandle} display=${observation.displayName} gen=$generation evalGen=$evaluationGeneration stableKey=${observation.stableKey} lastKey=$lastEvaluatedKey")
        scope.launch {
            val evaluation = repository.evaluate(observation.exactHandle, observation.displayName)
            val decision = evaluation.decision
            val unlockActive = isYoutubeQuickUnlockActive()
            val measuredCategory = when {
                isKavachYoutubeUnlock() -> YoutubeInsightsRepository.CATEGORY_DISTRACTING
                decision == YoutubeV2RuntimeDecision.ALLOW -> YoutubeInsightsRepository.CATEGORY_PRODUCTIVE
                unlockActive -> YoutubeInsightsRepository.CATEGORY_DISTRACTING
                else -> null
            }
            android.util.Log.d("YTCM", "⚖️ decision=$decision for handle=${observation.exactHandle} | gen match=${generation == evaluationGeneration} key match=${observation.stableKey == lastEvaluatedKey}")
            if (generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey) {
                android.util.Log.d("YTCM", "⚠️ Stale evaluation — dropping. gen=$generation evalGen=$evaluationGeneration stableKey=${observation.stableKey} lastKey=$lastEvaluatedKey")
                return@launch
            }
            // Channel is blocked or allowed
            if (measuredCategory == null && evaluation.channelId == null) {
                // Discovery supplies the classification controls, not permission
                // to keep playing. Stop media while that optional lookup runs.
                handler.post {
                    if (preferences.enabled.value && isYoutubeVisible() &&
                        generation == evaluationGeneration && observation.stableKey == lastEvaluatedKey
                    ) pauseMedia()
                }
            }
            val discovered = if (!unlockActive && evaluation.channelId == null) {
                repository.registerDiscoveredHandle(observation.exactHandle, observation.displayName).getOrNull()
            } else null
            if (generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey) return@launch
            val targetChannelId = discovered?.channelId ?: evaluation.channelId
            val displayName = discovered?.displayName ?: observation.displayName ?: "This channel"
            val isUnclassifiedOrOthers = evaluation.classification == YoutubeChannelClassification.OTHERS

            lastWatchedChannelId = targetChannelId
            lastWatchedDisplayName = displayName
            lastWatchedExactHandle = observation.exactHandle
            lastWatchedClassification = if (decision == YoutubeV2RuntimeDecision.ALLOW) {
                YoutubeChannelClassification.PRODUCTIVE
            } else {
                evaluation.classification
            }

            if (measuredCategory != null) {
                if (unlockActive) {
                    syncQuickUnlockTimer()
                }
                handler.post {
                    if (!preferences.enabled.value || !isYoutubeVisible() ||
                        generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey
                    ) return@post
                    android.util.Log.d("YTCM", "✅ ALLOW — no block for ${observation.exactHandle}")
                    blockOverlayVisible = false
                    overlay.dismiss()
                    recordAnalytics(evaluation.channelId, measuredCategory, false)
                }
                return@launch
            }

            handler.post {
                if (!preferences.enabled.value || !isYoutubeVisible() ||
                    generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey
                ) return@post
                android.util.Log.d("YTCM", "🚫 BLOCK — firing block for $displayName (isOthers=$isUnclassifiedOrOthers)")
                if (isUnclassifiedOrOthers && targetChannelId != null) {
                    blockWithClassification(targetChannelId, displayName)
                } else {
                    block(displayName)
                }
            }
        }
    }

    private fun blockWithClassification(channelId: String, displayName: String) {
        if (blockOverlayVisible || overlay.isShowing) return
        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        closePlayingVideo()
        blockOverlayVisible = true

        val chips = listOf(
            KavachBlockOverlay.ClassificationOption(
                label = "Productive",
                backgroundColor = Color.argb(180, 46, 90, 39),
                strokeColor = Color.parseColor("#4D7C0F"),
                textColor = Color.WHITE,
                onSelected = {
                    blockOverlayVisible = false
                    overlay.dismiss()
                    scope.launch {
                        repository.setClassification(channelId, YoutubeChannelClassification.PRODUCTIVE)
                    }
                },
            ),
            KavachBlockOverlay.ClassificationOption(
                label = "Distracting",
                backgroundColor = Color.argb(180, 136, 19, 55),
                strokeColor = Color.parseColor("#FB7185"),
                textColor = Color.WHITE,
                onSelected = {
                    blockOverlayVisible = false
                    overlay.dismiss()
                    scope.launch {
                        repository.setClassification(channelId, YoutubeChannelClassification.DISTRACTING)
                    }
                },
            ),
        )

        overlay.showContent(
            title = "Channel blocked",
            subtitle = "$displayName has been blocked. What do you want to do with it?",
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            classificationOptions = chips,
            onQuickUnlock = { syncQuickUnlockTimer() },
            onDismiss = { blockOverlayVisible = false },
        )
    }

    private fun block(displayName: String? = null) {
        if (blockOverlayVisible || overlay.isShowing) return
        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        closePlayingVideo()
        blockOverlayVisible = true
        val subtitle = if (!displayName.isNullOrBlank()) {
            "$displayName is marked as Distracting. Need a quick break?"
        } else {
            "This channel is not in your Productive list. Need a quick break?"
        }
        overlay.showContent(
            title = "Channel blocked",
            subtitle = subtitle,
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            onQuickUnlock = { syncQuickUnlockTimer() },
            onDismiss = { blockOverlayVisible = false },
        )
    }

    private fun blockShorts() {
        if (blockOverlayVisible || overlay.isShowing) return
        if (isYoutubeQuickUnlockActive()) {
            android.util.Log.d("YTCM", "⚡ QUICK UNLOCK ACTIVE — allowing Shorts")
            quickUnlockWasActive = true
            syncQuickUnlockTimer()
            lastWatchedChannelId = null
            lastWatchedDisplayName = "Shorts"
            lastWatchedExactHandle = null
            lastWatchedClassification = YoutubeChannelClassification.DISTRACTING
            recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_DISTRACTING, true)
            return
        }
        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        evaluationGeneration++
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = Long.MAX_VALUE
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        pendingYoutubeClickAtMs = null
        session.onBrowsing()
        closePlayingVideo()
        blockOverlayVisible = true
        overlay.showContent(
            title = "YouTube Shorts blocked",
            subtitle = "Shorts are blocked in YouTube Focus. Need a quick break?",
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            onQuickUnlock = { syncQuickUnlockTimer() },
            onDismiss = { blockOverlayVisible = false },
        )
    }

    private fun closePlayingVideo() {
        if (enforceFloatingPlayback()) return
        if (!isYoutubeVisible()) return
        pauseMedia()
        navigateToYoutubeHome()
        // Never send a delayed global Back: the student may already be working
        // in another app, and Back can close that app or its current screen.
    }

    private fun handleQuickUnlockExpired(passedExpiredMinutes: Int = 0) {
        if (overlay.isShowing) return
        handler.removeCallbacks(quickUnlockExpireRunnable)
        scheduledQuickUnlockUntilMs = null
        quickUnlockWasActive = false

        // If user is currently studying a PRODUCTIVE channel, let them continue without interruption.
        if (lastWatchedClassification == YoutubeChannelClassification.PRODUCTIVE && !analyticsShorts) {
            stopAnalytics()
            return
        }

        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        evaluationGeneration++
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = Long.MAX_VALUE
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        pendingYoutubeClickAtMs = null
        session.onBrowsing()

        // 1. Immediately pause media & close video/shorts
        closePlayingVideo()
        blockOverlayVisible = true

        // 2. Compute expired minutes
        val consumedMins = FocusShieldRepository.ShieldPrefs.consumeQuickUnlockJustExpired(this)
        val expiredMinutes = when {
            passedExpiredMinutes > 0 -> passedExpiredMinutes
            consumedMins > 0 -> consumedMins
            else -> 5
        }

        val targetChannelId = lastWatchedChannelId
        val displayName = lastWatchedDisplayName ?: "This channel"
        val isUnclassifiedOrOthers = lastWatchedClassification == YoutubeChannelClassification.OTHERS

        val subtitle = if (analyticsShorts) {
            "Your $expiredMinutes-minute break ended. Shorts are blocked in YouTube Focus."
        } else if (!lastWatchedDisplayName.isNullOrBlank()) {
            "Your $expiredMinutes-minute break ended. $displayName is blocked in YouTube Focus."
        } else {
            "Your $expiredMinutes-minute break ended. You have been watching for over $expiredMinutes minutes."
        }

        val classificationChips = if (isUnclassifiedOrOthers && !targetChannelId.isNullOrBlank()) {
            listOf(
                KavachBlockOverlay.ClassificationOption(
                    label = "Productive",
                    backgroundColor = Color.argb(180, 46, 90, 39),
                    strokeColor = Color.parseColor("#4D7C0F"),
                    textColor = Color.WHITE,
                    onSelected = {
                        blockOverlayVisible = false
                        overlay.dismiss()
                        scope.launch {
                            repository.setClassification(targetChannelId, YoutubeChannelClassification.PRODUCTIVE)
                        }
                    },
                ),
                KavachBlockOverlay.ClassificationOption(
                    label = "Distracting",
                    backgroundColor = Color.argb(180, 136, 19, 55),
                    strokeColor = Color.parseColor("#FB7185"),
                    textColor = Color.WHITE,
                    onSelected = {
                        blockOverlayVisible = false
                        overlay.dismiss()
                        scope.launch {
                            repository.setClassification(targetChannelId, YoutubeChannelClassification.DISTRACTING)
                        }
                    },
                ),
            )
        } else emptyList()

        overlay.showContent(
            title = "Quick Unlock Expired",
            subtitle = subtitle,
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            classificationOptions = classificationChips,
            onQuickUnlock = { syncQuickUnlockTimer() },
            onDismiss = { blockOverlayVisible = false },
        )
    }

    private fun isYoutubeQuickUnlockActive(): Boolean =
        FocusShieldRepository.ShieldPrefs.isInGracePeriodForPackage(
            this,
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
        )

    private fun isKavachYoutubeUnlock(): Boolean =
        isYoutubeQuickUnlockActive() &&
            FocusShieldRepository.ShieldPrefs.quickUnlockOrigin(this) ==
            FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH

    private fun availableQuickUnlockMinutes(): List<Int> =
        if (FocusShieldRepository.ShieldPrefs.isActive(this) &&
            FocusShieldRepository.ShieldPrefs.isStrict(this)
        ) emptyList() else listOf(5, 10, 15, 20)

    private fun recordAnalytics(channelId: String?, category: String, shorts: Boolean) {
        analyticsOpen = true
        analyticsChannelId = channelId
        analyticsCategory = category
        analyticsShorts = shorts
        quickUnlockWasActive = isYoutubeQuickUnlockActive()
        youtubeInsightsRepository.recordViewing(channelId, category, shorts)
    }

    private fun heartbeatAnalytics() {
        if (!analyticsOpen) return
        val category = analyticsCategory ?: return
        youtubeInsightsRepository.recordViewing(analyticsChannelId, category, analyticsShorts)
    }

    private fun stopAnalytics() {
        lastEvaluatedKey = null
        if (!analyticsOpen) return
        analyticsOpen = false
        analyticsChannelId = null
        analyticsCategory = null
        analyticsShorts = false
        youtubeInsightsRepository.finishViewing()
    }

    private fun screenIsInteractive(): Boolean =
        getSystemService(android.os.PowerManager::class.java).isInteractive &&
            !getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked

    private fun youtubePipWindow(): AccessibilityWindowInfo? =
        windows.firstOrNull { window ->
            window.isInPictureInPictureMode &&
                window.root?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE
        }

    /** Both Android PiP and YouTube's in-app mini-player can outlive the watch page. */
    private fun enforceFloatingPlayback(): Boolean = enforceYoutubePip() || enforceYoutubeMiniPlayer()

    private fun enforceYoutubeMiniPlayer(): Boolean {
        if (!screenIsInteractive()) return false
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val miniNodes = mutableListOf<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        while (queue.isNotEmpty() && inspected++ < MAX_NODES) {
            val node = queue.removeFirst()
            if (node.isVisibleToUser && YoutubeStudyV2Parser.isMiniPlayerId(node.viewIdResourceName)) {
                miniNodes.add(node)
                // The close-control search owns this subtree. Do not collect
                // its descendants again and rescan them on every heartbeat.
                continue
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        if (miniNodes.isEmpty()) return false
        if (shouldBlockYoutubePip(isYoutubeQuickUnlockActive(), lastWatchedClassification)) {
            stopAnalytics()
            pauseMedia()
            // Some builds expose only individually named controls, with no
            // named container. Keep looking past play/expand for the close control.
            miniNodes.firstOrNull { closeMiniPlayer(it) }
        } else {
            recordFloatingPlayback()
        }
        return true
    }

    private fun closeMiniPlayer(container: AccessibilityNodeInfo): Boolean {
        // Search only inside the mini-player; never click a feed item's close
        // button or send Back/Home to whatever app the student is working in.
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(container)
        var inspected = 0
        while (queue.isNotEmpty() && inspected++ < MAX_NODES) {
            val node = queue.removeFirst()
            if (node.isVisibleToUser && node.isClickable &&
                YoutubeStudyV2Parser.isMiniPlayerCloseControl(
                    node.viewIdResourceName,
                    (node.contentDescription ?: node.text)?.toString(),
                ) && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) return true
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return container.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
    }

    private fun recordFloatingPlayback() {
        syncQuickUnlockTimer()
        val now = SystemClock.elapsedRealtime()
        if (!analyticsOpen || now - lastPipAnalyticsAtMs >= ANALYTICS_HEARTBEAT_MS) {
            lastPipAnalyticsAtMs = now
            recordAnalytics(
                lastWatchedChannelId,
                if (isKavachYoutubeUnlock() || lastWatchedClassification != YoutubeChannelClassification.PRODUCTIVE)
                    YoutubeInsightsRepository.CATEGORY_DISTRACTING
                else YoutubeInsightsRepository.CATEGORY_PRODUCTIVE,
                analyticsShorts,
            )
        }
    }

    /** Returns true while PiP owns playback, so the normal watch parser stays idle. */
    private fun enforceYoutubePip(): Boolean {
        if (!screenIsInteractive()) return false
        val pip = youtubePipWindow() ?: return false
        if (shouldBlockYoutubePip(isYoutubeQuickUnlockActive(), lastWatchedClassification)) {
            stopAnalytics()
            pauseMedia()
            // Android exposes PiP dismissal on the window root. This targets
            // YouTube only; global Back/Home would act on the foreground app.
            pip.root?.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
        } else {
            recordFloatingPlayback()
        }
        return true
    }

    private fun isYoutubeVisible(): Boolean =
        screenIsInteractive() &&
            (rootInActiveWindow?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE ||
                youtubePipWindow() != null)

    /** Prefer YouTube's Home tab; Back is the safe fallback for direct/deep links. */
    private fun navigateToYoutubeHome() {
        val root = rootInActiveWindow
        if (root?.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) return
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        root?.let(queue::add)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val id = node.viewIdResourceName.orEmpty().lowercase()
            val label = (node.contentDescription ?: node.text)?.toString()?.trim()?.lowercase().orEmpty()
            if (node.isVisibleToUser && node.isClickable &&
                (id.contains("pivot_home") || label == "home" || label == "होम") &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) return
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun readObservation(): YoutubeV2Observation {
        val root = rootInActiveWindow
            ?: return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        if (root.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
            return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        }
        val metrics = resources.displayMetrics
        return YoutubeStudyV2Parser.parse(
            YoutubeV2Snapshot(
                packageName = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
                density = metrics.density,
                screenWidth = metrics.widthPixels,
                screenHeight = metrics.heightPixels,
                nodes = readNodes(root),
            ),
        )
    }

    private fun readNodes(root: AccessibilityNodeInfo): List<YoutubeV2Node> {
        val result = ArrayList<YoutubeV2Node>()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int?>>()
        queue.add(root to null)
        while (queue.isNotEmpty() && result.size < MAX_NODES) {
            val (node, parentIndex) = queue.removeFirst()
            val bounds = Rect().also(node::getBoundsInScreen)
            val nodeIndex = result.size
            result += YoutubeV2Node(
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                className = node.className?.toString(),
                visibleToUser = node.isVisibleToUser,
                clickable = node.isClickable,
                selected = node.isSelected,
                parentIndex = parentIndex,
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
            )
            for (index in 0 until node.childCount) node.getChild(index)?.let { queue.addLast(it to nodeIndex) }
        }
        return result
    }

    private fun isVideoCardClick(event: AccessibilityEvent): Boolean {
        val labels = mutableListOf<CharSequence>()
        event.contentDescription?.let(labels::add)
        if (event.text.isNotEmpty()) labels += event.text.joinToString(" ")
        var node = event.source
        repeat(CLICK_PARENT_DEPTH) {
            node ?: return@repeat
            node?.contentDescription?.let(labels::add)
            node?.text?.let(labels::add)
            node = node?.parent
        }
        return labels.any(::isClickedVideoCardLabel)
    }

    private fun isClickedVideoCardLabel(value: CharSequence?): Boolean {
        val label = value?.toString()?.lowercase().orEmpty()
        return label.contains("play video") ||
            label.contains("वीडियो चलाएं") ||
            label.contains("वीडियो चलाएँ")
    }

    private fun pauseMedia() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }

    private fun beginVideoTap(clickAtMs: Long) {
        // An earlier productive video cannot authorize a new, unidentified PiP.
        lastWatchedChannelId = null
        lastWatchedDisplayName = null
        lastWatchedExactHandle = null
        lastWatchedClassification = null
        recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_UNIDENTIFIED, false)
        evaluationGeneration++
        session.onVideoTap(clickAtMs)
        pendingYoutubeClickAtMs = null
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        blockOverlayVisible = false
        overlay.dismiss()
    }

    companion object {
        private const val PIP_CHECK_MS = 250L
        private const val DEBOUNCE_MS = 120L
        private const val STABILITY_GAP_MS = 120L
        private const val CLICK_TRANSITION_MS = 120L
        private const val SHORTS_PROBE_MS = 80L
        private const val OWNER_EVIDENCE_RETRY_MS = 150L
        private const val OWNER_EVIDENCE_WAIT_MS = 1_200L
        private const val GENERIC_CLICK_TRANSITION_WINDOW_MS = 2_500L
        private const val HEARTBEAT_MS = 30_000L
        private const val ANALYTICS_HEARTBEAT_MS = 2_000L
        private const val CLICK_PARENT_DEPTH = 5
        private const val MAX_NODES = 900
    }
}
