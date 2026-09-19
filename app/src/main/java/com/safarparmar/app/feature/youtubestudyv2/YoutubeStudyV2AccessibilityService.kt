package com.safarparmar.app.feature.youtubestudyv2

import com.safarparmar.app.R

import android.accessibilityservice.GestureDescription
import android.graphics.Path
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
import com.safarparmar.app.BuildConfig
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
import kotlinx.coroutines.withContext
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
    private var returningHome = false
    private val blockDismissal = YoutubeBlockDismissal()
    private var verifiedVideoTitle: String? = null
    private val fullscreenTransition = YoutubeFullscreenTransition()
    private var fullscreenMissingSinceMs: Long? = null
    private var fullscreenHudRevealAtMs = 0L
    private var fullscreenHudRevealAttempts = 0
    private var fullscreenHudRevealInFlight = false
    private var fullscreenSidePanelDismissAtMs = 0L
    private var fullscreenSidePanelDismissAttempts = 0
    private var fullscreenSidePanelDismissInFlight = false
    private var ignoreHudRevealClickUntilMs = 0L
    private var pausedForFullscreenIdentity = false
    private var homeNavigationGeneration = 0L
    private var pendingEvaluationKey: String? = null
    private var evaluationJob: Job? = null
    private var savingAllowlistEntry = false
    private var confirmation: Runnable? = null
    private var confirmationKey: String? = null
    private var lastLoggedKey: String? = null
    private var lastYoutubeTapMs = 0L

    private fun trace(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("YTV2_DEBUG",
            "t=${SystemClock.elapsedRealtime()} tapAgeMs=${if (lastYoutubeTapMs > 0) SystemClock.elapsedRealtime() - lastYoutubeTapMs else -1} $message")
    }
    private var blockOverlayVisible = false
    private var ownerMissingSinceMs: Long? = null
    private var pendingYoutubeClickAtMs: Long? = null
    private var analyticsOpen = false
    private var analyticsChannelId: String? = null
    private var analyticsCategory: String? = null
    private var analyticsShorts = false
    private var lastWatchedChannelId: String? = null
    private var lastWatchedDisplayName: String? = null
    private var lastWatchedExactHandle: String? = null
    private var lastWatchedClassification: YoutubeChannelClassification? = null
    private var classificationsByChannelId: Map<String, YoutubeChannelClassification> = emptyMap()

    private var lastPipAnalyticsAtMs = 0L
    private val launcherPackageName: String? by lazy {
        packageManager.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME),
            0,
        )?.activityInfo?.packageName
    }

    private val pipMonitor = object : Runnable {
        override fun run() {
            if (blockOverlayVisible && !shouldKeepYoutubeBlockOverlay(
                    screenAvailable = screenIsInteractive(),
                    youtubeForeground = isYoutubeOverlayHostVisible(),
                )
            ) {
                dismissBlockOverlaySilently()
            } else if (preferences.enabled.value) {
                // Window lookup is cheap. The in-app mini-player needs a full
                // node-tree traversal and is handled from parsed observations.
                enforceYoutubePip()
            }
            handler.postDelayed(this, PIP_CHECK_MS)
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
            if (returningHome || blockOverlayVisible) return
            if (!isYoutubeVisible()) {
                stopAnalytics()
                return
            }
            // YouTube occasionally changes the watch page without sending a
            // useful accessibility event. Process the watchdog snapshot now;
            // scheduling another debounced read added several seconds on some
            // devices when content-change events kept replacing that callback.
            captureFirstRead()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        classificationObserver?.cancel()
        classificationObserver = scope.launch(Dispatchers.Main.immediate) {
            repository.classifications.collect { classifications ->
                classificationsByChannelId = classifications.associate { entity ->
                    entity.channelId to YoutubeChannelClassification.fromWire(entity.classification)
                }
                lastWatchedChannelId?.let { channelId ->
                    // Keep the already verified playback decision if Room emits
                    // a transient list that does not contain this entry yet.
                    classificationsByChannelId[channelId]?.let { lastWatchedClassification = it }
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
        if (event?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE &&
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            lastYoutubeTapMs = SystemClock.elapsedRealtime()
            trace("CLICK id=${event.source?.viewIdResourceName} label=${event.contentDescription ?: event.source?.contentDescription}")
            if (isShortsClick(event)) {
                // A new Shorts entry supersedes an unfinished return or channel lookup.
                evaluationGeneration++
                evaluationJob?.cancel()
                pendingEvaluationKey = null
                homeNavigationGeneration++
                returningHome = false
                blockOverlayVisible = false
                overlay.dismiss()
                beginShortsTap()
                return
            }
        }
        if (returningHome || blockOverlayVisible) return
        // PiP belongs to a separate window even while another app has focus.
        // Avoid a full mini-player tree scan on every accessibility event.
        if (enforceYoutubePip()) return
        if (event?.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
            stopAnalytics()
            return
        }
        preferences.recordAccessibilityHeartbeat()

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val clickAtMs = SystemClock.elapsedRealtime()
            if (clickAtMs <= ignoreHudRevealClickUntilMs) {
                scheduleRead(HUD_READ_DELAY_MS)
                return
            }
            val sourceId = event.source?.viewIdResourceName.orEmpty().lowercase()
            val sourceLabel = (event.source?.contentDescription ?: event.contentDescription)?.toString().orEmpty().lowercase()
            if (sourceId.contains("fullscreen") || sourceId.contains("full_screen") ||
                sourceLabel in setOf("full screen", "fullscreen", "enter full screen", "exit full screen")) {
                // Capture the owner before YouTube replaces the portrait metadata.
                readObservation()
                scheduleRead(CLICK_TRANSITION_MS)
                return
            }
            if (isShortsClick(event)) {
                beginShortsTap()
            } else if (isVideoCardClick(event) ||
                isFullscreenPlaybackChangeControl(sourceId, sourceLabel)
            ) {
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
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (isShortsEvent(event)) {
                    scheduleRead(0L)
                } else {
                    scheduleRead(DEBOUNCE_MS)
                }
            }
        }
    }

    override fun onInterrupt() = stopRuntime()

    override fun onDestroy() {
        stopRuntime()
        scope.cancel()
        super.onDestroy()
    }

    private fun stopRuntime() {
        homeNavigationGeneration++
        evaluationJob?.cancel()
        confirmation?.let(handler::removeCallbacks)
        confirmationKey = null
        returningHome = false
        classificationObserver?.cancel()
        classificationObserver = null
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = Long.MAX_VALUE
        handler.removeCallbacks(heartbeat)
        handler.removeCallbacks(analyticsHeartbeat)
        handler.removeCallbacks(pipMonitor)
        evaluationGeneration++
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        pendingYoutubeClickAtMs = null
        fullscreenMissingSinceMs = null
        fullscreenHudRevealAttempts = 0
        fullscreenHudRevealInFlight = false
        fullscreenSidePanelDismissAtMs = 0L
        fullscreenSidePanelDismissAttempts = 0
        fullscreenSidePanelDismissInFlight = false
        pausedForFullscreenIdentity = false
        savingAllowlistEntry = false
        blockOverlayVisible = false
        session.onBrowsing()
        overlay.dismiss()
        stopAnalytics()
    }

    private fun scheduleRead(delayMs: Long) {
        val dueAt = SystemClock.elapsedRealtime() + delayMs
        // If a read is already scheduled sooner or equal, don't postpone it.
        // But if delayMs is 0L (immediate probe), always execute promptly!
        if (delayMs > 0L && scheduledReadAtMs <= dueAt) return
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = dueAt
        if (delayMs <= 0L) {
            handler.post(debounce)
        } else {
            handler.postDelayed(debounce, delayMs)
        }
    }

    private fun captureFirstRead() {
        if (returningHome || blockOverlayVisible) return
        if (!preferences.enabled.value) return stopAnalytics()
        if (!isYoutubeVisible()) return stopAnalytics()
        // Android PiP is discoverable without walking YouTube's node tree.
        if (enforceYoutubePip()) return
        val observation = readObservation()
        if (observation.kind == YoutubeV2ContentKind.SHORTS) {
            blockShorts()
            return
        }
        if (observation.kind == YoutubeV2ContentKind.MINI_PLAYER) {
            enforceYoutubeMiniPlayer()
            return
        }
        if (observation.fullscreen && !observation.hasOwnerEvidence) {
            if (canRetainFullscreenPermission(verifiedVideoTitle, observation.title, lastWatchedClassification)) {
                heartbeatAnalytics()
                return
            }
            val now = SystemClock.elapsedRealtime()
            val missingSince = fullscreenMissingSinceMs ?: now.also { fullscreenMissingSinceMs = it }
            // Rotation may briefly rebuild the player hierarchy. If metadata
            // remains absent, reveal YouTube's HUD so its accessibility nodes
            // become available without exiting fullscreen.
            if (now - missingSince < FULLSCREEN_METADATA_GRACE_MS) {
                scheduleRead(150L)
                return
            }
            if (!pausedForFullscreenIdentity) {
                pauseMedia()
                pausedForFullscreenIdentity = true
            }
            if (observation.fullscreenSidePanel != YoutubeFullscreenSidePanel.NONE &&
                fullscreenSidePanelDismissAttempts < MAX_FULLSCREEN_SIDE_PANEL_DISMISSES &&
                !fullscreenSidePanelDismissInFlight &&
                now - fullscreenSidePanelDismissAtMs >= FULLSCREEN_SIDE_PANEL_DISMISS_COOLDOWN_MS
            ) {
                dismissFullscreenSidePanel(observation.fullscreenSidePanel)
                return
            }
            if (fullscreenHudRevealAttempts < MAX_FULLSCREEN_HUD_REVEALS &&
                !fullscreenHudRevealInFlight &&
                now - fullscreenHudRevealAtMs >= FULLSCREEN_HUD_REVEAL_COOLDOWN_MS
            ) {
                revealFullscreenHud()
                return
            }
            if (now - missingSince >= FULLSCREEN_METADATA_TIMEOUT_MS &&
                fullscreenHudRevealAttempts >= MAX_FULLSCREEN_HUD_REVEALS &&
                !fullscreenHudRevealInFlight
            ) {
                lastWatchedClassification = null
                verifiedVideoTitle = null
                block("This channel")
                return
            }
            scheduleRead(200L)
            return
        }
        fullscreenMissingSinceMs = null
        fullscreenHudRevealAttempts = 0
        fullscreenHudRevealInFlight = false
        fullscreenSidePanelDismissAtMs = 0L
        fullscreenSidePanelDismissAttempts = 0
        fullscreenSidePanelDismissInFlight = false
        if (observation.watchScreenConfirmed && lastEvaluatedKey != observation.stableKey) {
            // Autoplay can change the video without a tap. Until the new owner
            // is evaluated, do not carry a productive decision into PiP.
            lastWatchedClassification = null
        }
        if (observation.kind == YoutubeV2ContentKind.SHORTS) {
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
            recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_UNIDENTIFIED, false)
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
        if (observation.kind == YoutubeV2ContentKind.VIDEO && observation.adPlaying) {
            // An ad can replace the title/owner nodes while a decision for the
            // underlying video is waiting for confirmation. Cancel that work so
            // advertiser metadata can never produce a channel-block sheet.
            confirmation?.let(handler::removeCallbacks)
            confirmation = null
            confirmationKey = null
            evaluationGeneration++
            evaluationJob?.cancel()
            evaluationJob = null
            pendingEvaluationKey = null
            lastEvaluatedKey = null
            ownerMissingSinceMs = null
            firstStableRead = null
            scheduleRead(OWNER_EVIDENCE_RETRY_MS)
            return
        }
        if (observation.kind == YoutubeV2ContentKind.VIDEO && !observation.hasOwnerEvidence) {
            val now = SystemClock.elapsedRealtime()
            val missingSince = ownerMissingSinceMs ?: now.also { ownerMissingSinceMs = it }
            // Wait briefly for the owner row before reporting unresolved identity.
            if (now - missingSince < OWNER_EVIDENCE_WAIT_MS) {
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
        val candidateKey = "$generation:${observation.stableKey}"
        if (confirmationKey == candidateKey) return
        confirmation?.let(handler::removeCallbacks)
        confirmationKey = candidateKey
        confirmation = Runnable {
            confirmationKey = null
            confirmation = null
            captureConfirmation(observation, generation)
        }.also { handler.postDelayed(it, STABILITY_GAP_MS) }
    }

    private fun captureConfirmation(first: YoutubeV2Observation, generation: Long) {
        if (generation != evaluationGeneration || !preferences.enabled.value || blockOverlayVisible || returningHome) return
        val second = readObservation()
        if (second.stableKey != first.stableKey || !second.watchScreenConfirmed) {
            firstStableRead = null
            scheduleRead(DEBOUNCE_MS)
            return
        }
        if (session.isAlreadyEvaluated(second.stableKey, lastEvaluatedKey)) {
            if (!analyticsOpen && lastWatchedClassification == YoutubeChannelClassification.PRODUCTIVE) {
                recordAnalytics(lastWatchedChannelId, YoutubeInsightsRepository.CATEGORY_PRODUCTIVE, false)
            }
            return
        }
        if (pendingEvaluationKey == "$generation:${second.stableKey}") return
        lastEvaluatedKey = second.stableKey
        decide(second, generation)
    }

    private fun decide(observation: YoutubeV2Observation, generation: Long) {
        blockDismissal.onVideoEntered()
        val requestKey = "$generation:${observation.stableKey}"
        pendingEvaluationKey = requestKey
        val handle = observation.exactHandle
        val displayName = observation.displayName ?: handle ?: "This channel"
        android.util.Log.d("YTV2_DEBUG", "allowlist check: title='${observation.title}' handle='$handle' display='$displayName'")
        evaluationJob?.cancel()
        evaluationJob = scope.launch {
            val evaluationStartedAtMs = SystemClock.elapsedRealtime()
            var applied = false
            try {
                val evaluation = repository.evaluate(handle, observation.displayName)
                trace(
                    "EVALUATION result=${evaluation.classification} durationMs=" +
                        (SystemClock.elapsedRealtime() - evaluationStartedAtMs),
                )
                withContext(Dispatchers.Main) {
                    if (!preferences.enabled.value || !isYoutubeVisible() || returningHome ||
                        generation != evaluationGeneration || readObservation().stableKey != observation.stableKey) return@withContext
                    applied = true
                    lastWatchedChannelId = evaluation.channelId
                    lastWatchedDisplayName = displayName
                    lastWatchedExactHandle = handle
                    lastWatchedClassification = evaluation.classification
                    verifiedVideoTitle = observation.title.takeIf { evaluation.decision == YoutubeV2RuntimeDecision.ALLOW }
                    lastEvaluatedKey = observation.stableKey
                    if (evaluation.decision == YoutubeV2RuntimeDecision.ALLOW) {
                        blockOverlayVisible = false
                        overlay.dismiss()
                        if (pausedForFullscreenIdentity) resumeMedia()
                        pausedForFullscreenIdentity = false
                        recordAnalytics(evaluation.channelId, YoutubeInsightsRepository.CATEGORY_PRODUCTIVE, false)
                    } else {
                        pausedForFullscreenIdentity = false
                        block(
                            displayName = displayName,
                            allowReference = handle ?: observation.displayName,
                            expectedVideoKey = observation.stableKey,
                        )
                    }
                }
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (pendingEvaluationKey == requestKey) {
                        pendingEvaluationKey = null
                        if (!applied && lastEvaluatedKey == observation.stableKey) {
                            lastEvaluatedKey = null
                            trace("EVALUATION discarded; scheduling fresh read")
                            if (preferences.enabled.value && !returningHome && !blockOverlayVisible) scheduleRead(0L)
                        }
                    }
                }
            }
        }
    }

    private fun dismissToHome() {
        trace("ACTION return_home")
        if (!blockOverlayVisible) return
        blockOverlayVisible = false
        blockDismissal.onReturnHome()
        navigateToYoutubeHome()
    }

    private fun dismissBlockOverlaySilently() {
        if (!blockOverlayVisible && !overlay.isShowing) return
        blockOverlayVisible = false
        overlay.dismiss()
    }

    private fun block(
        displayName: String? = null,
        allowReference: String? = null,
        expectedVideoKey: String? = null,
    ) {
        if (returningHome) return
        stopAnalytics()
        pauseMedia()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        blockOverlayVisible = true
        trace("SHEET not_allowlisted")
        val identifiedReference = allowReference?.takeIf { it.isNotBlank() }
        overlay.showContent(
            title = getString(if (identifiedReference == null) R.string.youtube_focus_channel_unidentified else R.string.youtube_focus_channel_blocked),
            subtitle = if (identifiedReference == null) {
                getString(R.string.youtube_focus_channel_unidentified_body)
            } else {
                getString(R.string.youtube_focus_channel_not_allowed_body, displayName ?: getString(R.string.youtube_focus_this_channel))
            },
            buttonText = getString(R.string.youtube_focus_return_home),
            onAction = { overlay.dismiss() },
            classificationOptions = identifiedReference?.let { reference ->
                listOf(
                    KavachBlockOverlay.ClassificationOption(
                        label = getString(R.string.youtube_focus_add_productive),
                        backgroundColor = Color.argb(210, 46, 90, 39),
                        strokeColor = Color.parseColor("#65A30D"),
                        textColor = Color.WHITE,
                        onSelected = { addBlockedChannelToAllowlist(reference, displayName, expectedVideoKey) },
                        dismissOnSelect = false,
                    ),
                )
            }.orEmpty(),
            onDismiss = { dismissToHome() },
        )
    }

    private fun addBlockedChannelToAllowlist(
        reference: String,
        displayName: String?,
        expectedVideoKey: String?,
    ) {
        if (savingAllowlistEntry) return
        savingAllowlistEntry = true
        scope.launch {
            val result = repository.resolveAndAllow(reference)
            withContext(Dispatchers.Main) {
                savingAllowlistEntry = false
                if (result.isFailure) {
                    android.widget.Toast.makeText(
                        this@YoutubeStudyV2AccessibilityService,
                        result.exceptionOrNull()?.message ?: "Could not add this channel. Please try again.",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                    return@withContext
                }
                if (!blockOverlayVisible || returningHome) return@withContext
                val current = readObservation()
                val channel = result.getOrThrow().channel
                lastWatchedChannelId = channel.channelId
                lastWatchedDisplayName = displayName ?: channel.displayName
                lastWatchedExactHandle = reference.takeIf { it.startsWith('@') }
                lastWatchedClassification = YoutubeChannelClassification.PRODUCTIVE
                blockOverlayVisible = false
                overlay.dismiss()
                if (isYoutubeVisible() && expectedVideoKey != null && current.stableKey == expectedVideoKey) {
                    verifiedVideoTitle = current.title
                    resumeMedia()
                    recordAnalytics(channel.channelId, YoutubeInsightsRepository.CATEGORY_PRODUCTIVE, false)
                    lastEvaluatedKey = current.stableKey
                } else {
                    lastEvaluatedKey = null
                    scheduleRead(0L)
                }
            }
        }
    }

    private fun resumeMedia() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    private fun revealFullscreenHud() {
        val root = youtubeWindowRoot() ?: return scheduleRead(200L)
        val bounds = Rect().also(root::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return scheduleRead(200L)
        val now = SystemClock.elapsedRealtime()
        fullscreenHudRevealAtMs = now
        fullscreenHudRevealAttempts++
        fullscreenHudRevealInFlight = true
        ignoreHudRevealClickUntilMs = now + HUD_REVEAL_CLICK_GUARD_MS
        val path = Path().apply {
            moveTo(
                bounds.left + bounds.width() * 0.25f,
                bounds.top + bounds.height() * 0.35f,
            )
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 45L))
            .build()
        trace("ACTION reveal_fullscreen_hud attempt=$fullscreenHudRevealAttempts")
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                fullscreenHudRevealInFlight = false
                scheduleRead(HUD_READ_DELAY_MS)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                fullscreenHudRevealInFlight = false
                scheduleRead(FULLSCREEN_HUD_REVEAL_COOLDOWN_MS)
            }
        }, null)
        if (!accepted) {
            fullscreenHudRevealInFlight = false
            scheduleRead(FULLSCREEN_HUD_REVEAL_COOLDOWN_MS)
        }
    }

    private fun dismissFullscreenSidePanel(panel: YoutubeFullscreenSidePanel) {
        val root = youtubeWindowRoot() ?: return scheduleRead(FULLSCREEN_SIDE_PANEL_RETRY_MS)
        val bounds = Rect().also(root::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return scheduleRead(FULLSCREEN_SIDE_PANEL_RETRY_MS)
        val now = SystemClock.elapsedRealtime()
        fullscreenSidePanelDismissAtMs = now
        fullscreenSidePanelDismissAttempts++
        fullscreenSidePanelDismissInFlight = true

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        while (queue.isNotEmpty() && inspected++ < MAX_NODES) {
            val node = queue.removeFirst()
            val nodeBounds = Rect().also(node::getBoundsInScreen)
            val id = node.viewIdResourceName.orEmpty().lowercase()
            val label = (node.contentDescription ?: node.text)?.toString()?.trim()?.lowercase().orEmpty()
            val onPanelHeader = nodeBounds.centerX() >= bounds.left + bounds.width() * FULLSCREEN_SIDE_PANEL_LEFT_RATIO &&
                nodeBounds.centerY() <= bounds.top + bounds.height() * FULLSCREEN_SIDE_PANEL_HEADER_RATIO
            val semanticClose = id.contains("close") &&
                (id.contains("chat") || id.contains("panel") || id.contains("companion") || id.contains("ad")) ||
                label in FULLSCREEN_SIDE_PANEL_CLOSE_LABELS
            if (node.isVisibleToUser && node.isClickable && onPanelHeader && semanticClose &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                trace("ACTION dismiss_fullscreen_side_panel panel=$panel method=node")
                fullscreenSidePanelDismissInFlight = false
                fullscreenMissingSinceMs = SystemClock.elapsedRealtime()
                handler.postDelayed({ scheduleRead(0L) }, FULLSCREEN_SIDE_PANEL_SETTLE_MS)
                return
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }

        // Some Compose variants draw the X without an actionable semantic node.
        // The panel marker proves this is the panel's top-right close target.
        val path = Path().apply {
            moveTo(
                bounds.left + bounds.width() * FULLSCREEN_SIDE_PANEL_CLOSE_X_RATIO,
                bounds.top + bounds.height() * FULLSCREEN_SIDE_PANEL_CLOSE_Y_RATIO,
            )
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 45L))
            .build()
        trace("ACTION dismiss_fullscreen_side_panel panel=$panel method=gesture")
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                fullscreenSidePanelDismissInFlight = false
                fullscreenMissingSinceMs = SystemClock.elapsedRealtime()
                scheduleRead(FULLSCREEN_SIDE_PANEL_SETTLE_MS)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                fullscreenSidePanelDismissInFlight = false
                scheduleRead(FULLSCREEN_SIDE_PANEL_RETRY_MS)
            }
        }, null)
        if (!accepted) {
            fullscreenSidePanelDismissInFlight = false
            scheduleRead(FULLSCREEN_SIDE_PANEL_RETRY_MS)
        }
    }

    private fun beginShortsTap() {
        // Wait for YouTube's click transition; Back on the old feed can exit the app.
        scheduleRead(CLICK_TRANSITION_MS)
    }

    private fun blockShorts() {
        if (blockOverlayVisible || overlay.isShowing) return
        if (isShortsQuickUnlockActive()) {
            lastWatchedChannelId = null
            lastWatchedDisplayName = "Shorts"
            lastWatchedClassification = YoutubeChannelClassification.DISTRACTING
            recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_DISTRACTING, true)
            return
        }
        stopAnalytics()
        pauseMedia()
        lastWatchedChannelId = null
        lastWatchedDisplayName = "Shorts"
        lastWatchedClassification = YoutubeChannelClassification.DISTRACTING
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)

        // 1. Display the bottom sheet immediately that YouTube Shorts is blocked
        showShortsBlockedSheet()

        // 2. Close Shorts and reload the home screen in the background
        closeShortsAndReloadHome()
    }

    private fun isShortsQuickUnlockActive(): Boolean =
        FocusShieldRepository.ShieldPrefs.isInGracePeriodForPackage(this, YoutubeStudyV2Parser.YOUTUBE_PACKAGE) &&
            FocusShieldRepository.ShieldPrefs.quickUnlockOrigin(this) ==
                FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY

    private fun showShortsBlockedSheet() {
        if (!preferences.enabled.value || isShortsQuickUnlockActive()) return
        blockOverlayVisible = true
        trace("SHEET shorts")
        overlay.showContent(
            title = getString(R.string.youtube_focus_shorts_blocked),
            subtitle = getString(R.string.youtube_focus_shorts_blocked_body),
            buttonText = getString(R.string.youtube_focus_return_home),
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
                navigateToYoutubeHome()
            },
            quickUnlockMinutes = if (FocusShieldRepository.ShieldPrefs.isActive(this) &&
                FocusShieldRepository.ShieldPrefs.isStrict(this)) emptyList() else listOf(5, 10, 15, 20),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            onQuickUnlock = { minutes ->
                blockOverlayVisible = false
                overlay.dismiss()
                handler.postDelayed({
                    if (preferences.enabled.value && !isShortsQuickUnlockActive()) scheduleRead(0L)
                }, minutes * 60_000L + 100L)
            },
            onDismiss = { blockOverlayVisible = false },
        )
    }

    private fun closeShortsAndReloadHome() {
        // Immediately pause media to cut off Shorts audio
        pauseMedia()
        // Send Back to immediately dismiss the Shorts viewer container
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Switch to Home tab and reload Home feed in the background
        handler.postDelayed({
            if (!preferences.enabled.value || !screenIsInteractive()) return@postDelayed
            val root = youtubeWindowRoot() ?: rootInActiveWindow
            if (root != null && root.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                val queue = ArrayDeque<AccessibilityNodeInfo>()
                queue.add(root)
                while (queue.isNotEmpty() && nodes.size < MAX_NODES) {
                    val node = queue.removeFirst()
                    if (node.isVisibleToUser) nodes.add(node)
                    for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
                }
                val homeNode = nodes.firstOrNull { node ->
                    val id = node.viewIdResourceName.orEmpty().lowercase()
                    val label = (node.contentDescription ?: node.text)?.toString()?.trim()?.lowercase().orEmpty()
                    node.isClickable && (id.contains("pivot_home") || label == "home" || label == "होम")
                }
                if (homeNode != null) {
                    // Clicking Home selects Home tab; clicking it when already on Home reloads/refreshes Home feed!
                    homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    handler.postDelayed({
                        homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }, 200L)
                    return@postDelayed
                }
            }
            // Fallback: direct Home intent to ensure YouTube is on the Home route
            runCatching {
                startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.youtube.com/"),
                    ).setPackage(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }, 150L)
    }

    private fun recordAnalytics(channelId: String?, category: String, shorts: Boolean) {
        analyticsOpen = true
        analyticsChannelId = channelId
        analyticsCategory = category
        analyticsShorts = shorts
        youtubeInsightsRepository.recordViewing(channelId, category, shorts)
    }

    private fun heartbeatAnalytics() {
        if (!analyticsOpen) return
        val category = analyticsCategory ?: return
        youtubeInsightsRepository.recordViewing(analyticsChannelId, category, analyticsShorts)
    }

    private fun stopAnalytics() {
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

    /**
     * TYPE_ACCESSIBILITY_OVERLAY becomes rootInActiveWindow while the sheet is
     * visible. Read the highest real application window underneath it instead.
     */
    private fun isYoutubeOverlayHostVisible(): Boolean {
        val activePackage = rootInActiveWindow?.packageName?.toString()
        if (activePackage == YoutubeStudyV2Parser.YOUTUBE_PACKAGE) return true
        val foregroundApplication = windows.asSequence()
            .filter { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    !window.isInPictureInPictureMode &&
                    window.root?.packageName != null
            }
            .maxByOrNull(AccessibilityWindowInfo::getLayer)
            ?.root?.packageName?.toString()
        return foregroundApplication == YoutubeStudyV2Parser.YOUTUBE_PACKAGE ||
            (youtubePipWindow() != null && foregroundApplication == launcherPackageName)
    }

    private fun youtubeWindowRoot(): AccessibilityNodeInfo? {
        val active = rootInActiveWindow
        if (active?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE) return active
        return windows.firstOrNull { window ->
            window.root?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE
        }?.root
    }

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
        val classification = floatingPlaybackClassification()
        if (!(analyticsShorts && isShortsQuickUnlockActive()) && shouldBlockYoutubePip(false, classification)) {
            stopAnalytics()
            pauseMedia()
            if (blockDismissal.suppressFloatingSheet) return true
            val miniChannelId = lastWatchedChannelId
            val miniDisplayName = lastWatchedDisplayName ?: "This channel"
            if (!miniChannelId.isNullOrBlank() || classification != YoutubeChannelClassification.PRODUCTIVE) {
                block(
                    displayName = miniDisplayName,
                    allowReference = lastWatchedExactHandle ?: lastWatchedDisplayName,
                )
            }
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
        val now = SystemClock.elapsedRealtime()
        if (!analyticsOpen || now - lastPipAnalyticsAtMs >= ANALYTICS_HEARTBEAT_MS) {
            lastPipAnalyticsAtMs = now
            val classification = floatingPlaybackClassification()
            recordAnalytics(
                lastWatchedChannelId,
                if (classification != YoutubeChannelClassification.PRODUCTIVE)
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
        val classification = floatingPlaybackClassification()
        if (!(analyticsShorts && isShortsQuickUnlockActive()) && shouldBlockYoutubePip(false, classification)) {
            stopAnalytics()
            pauseMedia()
            if (blockDismissal.suppressFloatingSheet) return true
            val pipChannelId = lastWatchedChannelId
            val pipDisplayName = lastWatchedDisplayName ?: "This channel"
            if (!pipChannelId.isNullOrBlank() || classification != YoutubeChannelClassification.PRODUCTIVE) {
                block(
                    displayName = pipDisplayName,
                    allowReference = lastWatchedExactHandle ?: lastWatchedDisplayName,
                )
            }
        } else {
            recordFloatingPlayback()
        }
        return true
    }

    private fun floatingPlaybackClassification(): YoutubeChannelClassification? =
        lastWatchedChannelId?.let(classificationsByChannelId::get) ?: lastWatchedClassification

    private fun isYoutubeVisible(): Boolean =
        screenIsInteractive() &&
            (rootInActiveWindow?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE ||
                youtubePipWindow() != null)

    /** Navigate only inside YouTube, re-reading after every action. */
    private fun navigateToYoutubeHome(refresh: Boolean = false, onHomeReady: (() -> Unit)? = null) {
        if (returningHome) return
        returningHome = true
        fullscreenTransition.clear()
        fullscreenMissingSinceMs = null
        fullscreenHudRevealAttempts = 0
        fullscreenHudRevealInFlight = false
        fullscreenSidePanelDismissAtMs = 0L
        fullscreenSidePanelDismissAttempts = 0
        fullscreenSidePanelDismissInFlight = false
        pausedForFullscreenIdentity = false
        val navigation = ++homeNavigationGeneration
        evaluationGeneration++
        handler.removeCallbacks(debounce)
        scheduledReadAtMs = Long.MAX_VALUE
        pendingYoutubeClickAtMs = null
        firstStableRead = null
        session.onBrowsing()
        stopAnalytics()
        pauseMedia()
        val policy = YoutubeHomeReturnPolicy()
        var scrolls = 0
        fun finish(homeReady: Boolean = false) {
            if (navigation != homeNavigationGeneration || !returningHome) return
            returningHome = false
            lastEvaluatedKey = null
            if (homeReady && isYoutubeVisible() && !readObservation().watchScreenConfirmed) onHomeReady?.invoke()
        }
        fun step() {
            if (navigation != homeNavigationGeneration) return
            // Returning Home can briefly move the blocked player into a PiP window.
            youtubePipWindow()?.root?.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
            val root = youtubeWindowRoot() ?: rootInActiveWindow
            if (!preferences.enabled.value || !screenIsInteractive() ||
                root?.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
                finish()
                return
            }
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty() && nodes.size < MAX_NODES) {
                val node = queue.removeFirst()
                if (node.isVisibleToUser) nodes.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
            val home = nodes.firstOrNull { node ->
                val id = node.viewIdResourceName.orEmpty()
                val label = (node.contentDescription ?: node.text)?.toString()?.trim()?.lowercase()
                node.isClickable && (id.contains("pivot_home") || label == "home" || label == "होम")
            }
            val observation = readObservation()
            val action = policy.next(true, observation.watchScreenConfirmed, home != null, home?.isSelected == true)
            when (action) {
                YoutubeHomeReturnPolicy.Action.ABORT -> {
                    finish()
                    android.widget.Toast.makeText(this, getString(R.string.youtube_focus_tap_home_to_finish), android.widget.Toast.LENGTH_SHORT).show()
                    return
                }
                YoutubeHomeReturnPolicy.Action.SELECT_HOME -> home?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                YoutubeHomeReturnPolicy.Action.HOME_READY -> {
                    if (!refresh) {
                        // YouTube usually attaches its in-app mini-player shortly
                        // after Home becomes selected. Wait briefly for that late
                        // node and close it before completing Return to home.
                        closeMiniPlayerAfterHome(navigation) {
                            finish(homeReady = true)
                        }
                        return
                    } else {
                        // Reach the top of the Home feed before pull-to-refresh.
                        val scroller = nodes.firstOrNull { it.isScrollable && it.actionList.any { action ->
                            action.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                        } }
                        if (scrolls++ < 5 && scroller?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true) {
                            handler.postDelayed({ step() }, 250L)
                            return
                        }
                        val metrics = resources.displayMetrics
                        val path = Path().apply {
                            moveTo(metrics.widthPixels * 0.5f, metrics.heightPixels * 0.25f)
                            lineTo(metrics.widthPixels * 0.5f, metrics.heightPixels * 0.7f)
                        }
                        val gesture = GestureDescription.Builder()
                            .addStroke(GestureDescription.StrokeDescription(path, 0, 400)).build()
                        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                handler.postDelayed({ if (navigation == homeNavigationGeneration) finish(homeReady = true) }, 500L)
                            }
                            override fun onCancelled(gestureDescription: GestureDescription?) { finish(homeReady = true) }
                        }, handler)
                        if (!accepted) finish(homeReady = true)
                        return
                    }
                }
                YoutubeHomeReturnPolicy.Action.OPEN_HOME -> {
                    // A direct-linked player may have no Home tab. Open YouTube's
                    // own Home route; system Back could finish that activity.
                    runCatching {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com/"))
                            .setPackage(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                YoutubeHomeReturnPolicy.Action.WAIT -> Unit
            }
            handler.postDelayed({ step() }, 250L)
        }
        // Allow an accessibility overlay to be removed before reading YouTube.
        handler.postDelayed({ step() }, 150L)
    }

    private fun closeMiniPlayerAfterHome(
        navigation: Long,
        attempt: Int = 0,
        onFinished: () -> Unit,
    ) {
        if (navigation != homeNavigationGeneration || !returningHome) return
        val root = youtubeWindowRoot()
        if (root != null && closeVisibleMiniPlayer(root)) {
            trace("ACTION close_miniplayer_after_home attempt=${attempt + 1}")
            handler.postDelayed(onFinished, MINI_PLAYER_CLOSE_SETTLE_MS)
            return
        }
        if (attempt + 1 >= MINI_PLAYER_CLOSE_MAX_ATTEMPTS) {
            trace("ACTION close_miniplayer_after_home unavailable")
            onFinished()
            return
        }
        handler.postDelayed(
            { closeMiniPlayerAfterHome(navigation, attempt + 1, onFinished) },
            MINI_PLAYER_CLOSE_RETRY_MS,
        )
    }

    private fun closeVisibleMiniPlayer(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val containers = mutableListOf<AccessibilityNodeInfo>()
        queue.add(root)
        var inspected = 0
        while (queue.isNotEmpty() && inspected++ < MAX_NODES) {
            val node = queue.removeFirst()
            if (node.isVisibleToUser) {
                if (node.isClickable && YoutubeStudyV2Parser.isMiniPlayerCloseControl(
                        node.viewIdResourceName,
                        (node.contentDescription ?: node.text)?.toString(),
                    ) && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) return true
                if (YoutubeStudyV2Parser.isMiniPlayerId(node.viewIdResourceName)) containers += node
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return containers.any(::closeMiniPlayer)
    }

    private fun readObservation(): YoutubeV2Observation {
        val observationStartedNs = SystemClock.elapsedRealtimeNanos()
        val root = youtubeWindowRoot() ?: rootInActiveWindow
            ?: return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        if (root.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) {
            return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        }
        val metrics = resources.displayMetrics
        // Service display metrics can remain portrait during YouTube rotation.
        val windowBounds = Rect().also(root::getBoundsInScreen)
        val nodes = readNodes(root)
        val parseStartedNs = SystemClock.elapsedRealtimeNanos()
        val observation = YoutubeStudyV2Parser.parse(
            YoutubeV2Snapshot(
                packageName = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
                density = metrics.density,
                screenWidth = windowBounds.width().takeIf { it > 0 } ?: metrics.widthPixels,
                screenHeight = windowBounds.height().takeIf { it > 0 } ?: metrics.heightPixels,
                nodes = nodes,
            ),
        )
        if (BuildConfig.DEBUG && lastLoggedKey != observation.stableKey) {
            val finishedNs = SystemClock.elapsedRealtimeNanos()
            val treeReadMs = (parseStartedNs - observationStartedNs) / 1_000_000.0
            val parseMs = (finishedNs - parseStartedNs) / 1_000_000.0
            lastLoggedKey = observation.stableKey
            trace(
                "OBSERVATION fullscreen=${observation.fullscreen} ad=${observation.adPlaying} " +
                    "sidePanel=${observation.fullscreenSidePanel} " +
                    "nodes=${nodes.size} treeMs=${"%.1f".format(treeReadMs)} parseMs=${"%.1f".format(parseMs)}",
            )
            android.util.Log.d(
                "YTV2_DUMP",
                "--> PARSED: kind=${observation.kind} watchConfirmed=${observation.watchScreenConfirmed} " +
                    "title='${observation.title}' handle='${observation.exactHandle}' " +
                    "display='${observation.displayName}' channelId='${observation.exactChannelId}'",
            )
        }
        return fullscreenTransition.observe(observation, SystemClock.elapsedRealtime())
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
        val sourceLabel = (event.source?.contentDescription ?: event.contentDescription)?.toString()
        if (isCurrentVideoTransportControl(event.source?.viewIdResourceName, sourceLabel)) return false
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
        return isVideoCardAccessibilityLabel(value)
    }

    private fun isShortsClick(event: AccessibilityEvent): Boolean {
        val source = event.source
        val sourceId = source?.viewIdResourceName.orEmpty().lowercase()
        if (isShortsRelatedId(sourceId)) return true

        val labels = mutableListOf<CharSequence>()
        event.contentDescription?.let(labels::add)
        if (event.text.isNotEmpty()) labels += event.text.joinToString(" ")

        var node = source
        repeat(CLICK_PARENT_DEPTH) {
            node ?: return@repeat
            val id = node?.viewIdResourceName.orEmpty().lowercase()
            if (isShortsRelatedId(id)) return true
            node?.contentDescription?.let(labels::add)
            node?.text?.let(labels::add)
            node = node?.parent
        }

        return labels.any(::isShortsLabel)
    }

    private fun isShortsRelatedId(id: String): Boolean {
        if (id.isEmpty()) return false
        return id.contains("pivot_shorts") ||
            id.contains("reel_") ||
            id.contains("shorts_") ||
            id.contains("short_shelf") ||
            id.contains("reel_shelf") ||
            id.contains("reel_recycler")
    }

    private fun isShortsLabel(value: CharSequence?): Boolean {
        val label = value?.toString()?.trim()?.lowercase().orEmpty()
        if (label.isEmpty()) return false
        if (label == "shorts" || label == "शॉर्ट्स" || label == "short" || label == "शॉर्ट") return true
        if (label.startsWith("play short") || label.contains("short video") || label.startsWith("shorts -") ||
            label.contains("short •") || label.contains("short ·")
        ) return true
        return false
    }

    private fun isShortsEvent(event: AccessibilityEvent): Boolean {
        val source = event.source
        val id = source?.viewIdResourceName.orEmpty().lowercase()
        if (isShortsRelatedId(id)) return true
        val desc = event.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
        val text = event.text.joinToString(" ").lowercase()
        return desc == "shorts player" || desc == "शॉर्ट्स प्लेयर" ||
            desc.contains("short") || text.contains("short") ||
            desc.contains("शॉर्ट") || text.contains("शॉर्ट") ||
            desc.contains("dislike this short") || desc.contains("like this short") ||
            desc.contains("remix this short") || desc.contains("share this short")
    }

    private fun stopMediaPlayback() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }

    private fun pauseMedia() = stopMediaPlayback()

    private fun beginVideoTap(clickAtMs: Long) {
        evaluationJob?.cancel()
        pendingEvaluationKey = null
        confirmation?.let(handler::removeCallbacks)
        confirmationKey = null
        fullscreenTransition.clear()
        fullscreenMissingSinceMs = null
        fullscreenHudRevealAttempts = 0
        fullscreenHudRevealInFlight = false
        fullscreenSidePanelDismissAtMs = 0L
        fullscreenSidePanelDismissAttempts = 0
        fullscreenSidePanelDismissInFlight = false
        pausedForFullscreenIdentity = false
        blockDismissal.onVideoEntered()
        verifiedVideoTitle = null
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
        private const val MINI_PLAYER_CLOSE_RETRY_MS = 150L
        private const val MINI_PLAYER_CLOSE_SETTLE_MS = 100L
        private const val MINI_PLAYER_CLOSE_MAX_ATTEMPTS = 8
        private const val OWNER_EVIDENCE_RETRY_MS = 150L
        private const val OWNER_EVIDENCE_WAIT_MS = 3_500L
        private const val FULLSCREEN_METADATA_GRACE_MS = 500L
        private const val FULLSCREEN_METADATA_TIMEOUT_MS = 4_000L
        private const val FULLSCREEN_HUD_REVEAL_COOLDOWN_MS = 900L
        private const val HUD_REVEAL_CLICK_GUARD_MS = 700L
        private const val HUD_READ_DELAY_MS = 180L
        private const val MAX_FULLSCREEN_HUD_REVEALS = 2
        private const val FULLSCREEN_SIDE_PANEL_DISMISS_COOLDOWN_MS = 700L
        private const val FULLSCREEN_SIDE_PANEL_RETRY_MS = 250L
        private const val FULLSCREEN_SIDE_PANEL_SETTLE_MS = 220L
        private const val MAX_FULLSCREEN_SIDE_PANEL_DISMISSES = 2
        private const val FULLSCREEN_SIDE_PANEL_LEFT_RATIO = 0.52f
        private const val FULLSCREEN_SIDE_PANEL_HEADER_RATIO = 0.24f
        private const val FULLSCREEN_SIDE_PANEL_CLOSE_X_RATIO = 0.968f
        private const val FULLSCREEN_SIDE_PANEL_CLOSE_Y_RATIO = 0.075f
        private val FULLSCREEN_SIDE_PANEL_CLOSE_LABELS = setOf(
            "close", "dismiss", "close live chat", "close chat", "close ad", "close panel",
        )
        private const val GENERIC_CLICK_TRANSITION_WINDOW_MS = 2_500L
        private const val HEARTBEAT_MS = 30_000L
        // Also acts as a recovery watchdog when a YouTube build omits a
        // window-content event during an instant fullscreen transition.
        private const val ANALYTICS_HEARTBEAT_MS = 1_000L
        private const val CLICK_PARENT_DEPTH = 5
        private const val MAX_NODES = 900
    }
}
