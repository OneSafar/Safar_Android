package com.safarparmar.app.feature.youtubestudyv2

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
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

/**
 * Channel decisions are possible only after an explicit normal-video tap.
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
    private val overlay by lazy { KavachBlockOverlay(this, accessibilityOverlay = true) }
    private var firstStableRead: YoutubeV2Observation? = null
    private var lastEvaluatedKey: String? = null
    private var evaluationGeneration = 0L
    private var blockOverlayVisible = false
    private var ownerMissingSinceMs: Long? = null
    private var pendingYoutubeClickAtMs: Long? = null
    private var analyticsOpen = false
    private var analyticsChannelId: String? = null
    private var analyticsCategory: String? = null
    private var analyticsShorts = false
    private var quickUnlockWasActive = false

    private val debounce = Runnable { captureFirstRead() }
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
            val youtubeVisible = isYoutubeVisible()
            val unlockActive = isYoutubeQuickUnlockActive()
            if (quickUnlockWasActive && !unlockActive) {
                quickUnlockWasActive = false
                stopAnalytics()
                lastEvaluatedKey = null
                firstStableRead = null
                scheduleRead(0L)
            } else if (!youtubeVisible) {
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
        if (preferences.enabled.value) {
            preferences.recordAccessibilityHeartbeat()
            YoutubeStudyV2GuardService.start(this)
        }
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
        handler.removeCallbacks(debounce)
        handler.removeCallbacks(heartbeat)
        handler.removeCallbacks(analyticsHeartbeat)
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
        handler.removeCallbacks(debounce)
        handler.postDelayed(debounce, delayMs)
    }

    private fun captureFirstRead() {
        if (!preferences.enabled.value || !isYoutubeVisible()) return stopAnalytics()
        val observation = readObservation()
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
        if (observation.kind == YoutubeV2ContentKind.VIDEO && observation.exactHandle.isNullOrBlank()) {
            val now = SystemClock.elapsedRealtime()
            val missingSince = ownerMissingSinceMs ?: now.also { ownerMissingSinceMs = it }
            // YouTube builds the watch player before its owner row, especially
            // while a pre-roll ad is visible, and the display name can appear
            // before the exact handle. Wait briefly for the exact identity so a
            // new channel can be registered without trusting ambiguous text.
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
            handler.post {
                if (!preferences.enabled.value || !isYoutubeVisible() ||
                    generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey
                ) return@post
                if (measuredCategory != null) {
                    android.util.Log.d("YTCM", "✅ ALLOW — no block for ${observation.exactHandle}")
                    blockOverlayVisible = false
                    overlay.dismiss()
                    recordAnalytics(evaluation.channelId, measuredCategory, false)
                } else {
                    android.util.Log.d("YTCM", "🚫 BLOCK — firing block() for ${observation.exactHandle}")
                    block()
                }
            }
            if (decision == YoutubeV2RuntimeDecision.BLOCK && !unlockActive) {
                android.util.Log.d("YTCM", "📡 Calling registerDiscoveredHandle for handle=${observation.exactHandle} display=${observation.displayName}")
                val discovered = repository.registerDiscoveredHandle(observation.exactHandle, observation.displayName).getOrNull()
                android.util.Log.d("YTCM", "📦 registerDiscoveredHandle returned: $discovered")
                if (discovered != null) {
                    android.util.Log.d("YTCM", "🔔 Firing BlockedNotification for channelId=${discovered.channelId} handle=${discovered.handle} display=${discovered.displayName}")
                    YoutubeStudyV2BlockedNotification.show(this@YoutubeStudyV2AccessibilityService, discovered)
                } else {
                    android.util.Log.e("YTCM", "❌ registerDiscoveredHandle returned null — notification NOT shown for handle=${observation.exactHandle} display=${observation.displayName}")
                }
            }
        }
    }

    private fun block() {
        if (blockOverlayVisible) return
        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        pauseMedia()
        performGlobalAction(GLOBAL_ACTION_BACK)
        blockOverlayVisible = true
        overlay.showContent(
            title = "Channel blocked",
            subtitle = "This channel is not in your Productive list. Need a quick break?",
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
        )
    }

    private fun blockShorts() {
        if (blockOverlayVisible) return
        if (isYoutubeQuickUnlockActive()) {
            android.util.Log.d("YTCM", "⚡ QUICK UNLOCK ACTIVE — allowing Shorts")
            quickUnlockWasActive = true
            recordAnalytics(null, YoutubeInsightsRepository.CATEGORY_DISTRACTING, true)
            return
        }
        stopAnalytics()
        focusShieldRepository.recordBlockedHit(YoutubeStudyV2Parser.YOUTUBE_PACKAGE)
        evaluationGeneration++
        handler.removeCallbacks(debounce)
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        pendingYoutubeClickAtMs = null
        session.onBrowsing()
        pauseMedia()
        navigateToYoutubeHome()
        blockOverlayVisible = true
        overlay.showContent(
            title = "YouTube Shorts blocked",
            subtitle = "Shorts are blocked in Study Mode. Need a quick break?",
            buttonText = "I'll Control Myself.",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
            quickUnlockMinutes = availableQuickUnlockMinutes(),
            blockedPackage = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
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

    private fun isYoutubeVisible(): Boolean =
        getSystemService(android.os.PowerManager::class.java).isInteractive &&
            !getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked &&
            rootInActiveWindow?.packageName?.toString() == YoutubeStudyV2Parser.YOUTUBE_PACKAGE

    /** Prefer YouTube's Home tab; Back is the safe fallback for direct/deep links. */
    private fun navigateToYoutubeHome() {
        val root = rootInActiveWindow
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
        private const val DEBOUNCE_MS = 250L
        private const val STABILITY_GAP_MS = 200L
        private const val CLICK_TRANSITION_MS = 120L
        private const val SHORTS_PROBE_MS = 80L
        private const val OWNER_EVIDENCE_RETRY_MS = 150L
        private const val OWNER_EVIDENCE_WAIT_MS = 3_000L
        private const val GENERIC_CLICK_TRANSITION_WINDOW_MS = 2_500L
        private const val HEARTBEAT_MS = 30_000L
        private const val ANALYTICS_HEARTBEAT_MS = 2_000L
        private const val CLICK_PARENT_DEPTH = 5
        private const val MAX_NODES = 900
    }
}
