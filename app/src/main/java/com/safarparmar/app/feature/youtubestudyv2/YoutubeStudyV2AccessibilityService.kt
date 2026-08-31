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
import com.safarparmar.app.ui.ekagra.focusshield.KavachBlockOverlay
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

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = YoutubeStudyV2Session()
    private val overlay by lazy { KavachBlockOverlay(this, accessibilityOverlay = true) }
    private var firstStableRead: YoutubeV2Observation? = null
    private var lastEvaluatedKey: String? = null
    private var evaluationGeneration = 0L
    private var blockOverlayVisible = false
    private var ownerMissingSinceMs: Long? = null

    private val debounce = Runnable { captureFirstRead() }
    private val heartbeat = object : Runnable {
        override fun run() {
            if (preferences.enabled.value) {
                preferences.recordAccessibilityHeartbeat()
                handler.postDelayed(this, HEARTBEAT_MS)
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!preferences.enabled.value) {
            YoutubeStudyV2GuardService.stop(this)
            return
        }
        if (event?.packageName?.toString() != YoutubeStudyV2Parser.YOUTUBE_PACKAGE) return
        preferences.recordAccessibilityHeartbeat()

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (isVideoCardClick(event)) {
                evaluationGeneration++
                session.onVideoTap(SystemClock.elapsedRealtime())
                firstStableRead = null
                lastEvaluatedKey = null
                ownerMissingSinceMs = null
                blockOverlayVisible = false
                overlay.dismiss()
                scheduleRead(CLICK_TRANSITION_MS)
            } else {
                // Shorts cards/tabs use different labels from normal "play video"
                // cards. Probe quickly, but do not create a normal-video session.
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
        evaluationGeneration++
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        blockOverlayVisible = false
        session.onBrowsing()
        overlay.dismiss()
    }

    private fun scheduleRead(delayMs: Long) {
        handler.removeCallbacks(debounce)
        handler.postDelayed(debounce, delayMs)
    }

    private fun captureFirstRead() {
        if (!preferences.enabled.value) return stopRuntime()
        val observation = readObservation()
        if (observation.kind == YoutubeV2ContentKind.SHORTS && observation.watchScreenConfirmed) {
            blockShorts()
            return
        }
        if (!observation.watchScreenConfirmed) {
            session.acceptStable(observation, SystemClock.elapsedRealtime())
            firstStableRead = null
            if (!blockOverlayVisible) overlay.dismiss()
            if (session.state == YoutubeStudyV2Session.State.VIDEO_TAPPED) scheduleRead(DEBOUNCE_MS)
            else lastEvaluatedKey = null
            return
        }
        if (observation.kind == YoutubeV2ContentKind.VIDEO && !observation.hasOwnerEvidence) {
            val now = SystemClock.elapsedRealtime()
            val missingSince = ownerMissingSinceMs ?: now.also { ownerMissingSinceMs = it }
            // YouTube builds the watch player before its owner row, especially
            // while a pre-roll ad is visible. Advertiser identity is never the
            // video's identity, so wait through the ad. Outside ads, preserve
            // fail-closed behavior after a bounded owner-loading window.
            if (observation.adPlaying || now - missingSince < OWNER_EVIDENCE_WAIT_MS) {
                firstStableRead = null
                scheduleRead(OWNER_EVIDENCE_RETRY_MS)
                return
            }
        } else {
            ownerMissingSinceMs = null
        }
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
        if (session.isAlreadyEvaluated(second.stableKey, lastEvaluatedKey)) return
        lastEvaluatedKey = second.stableKey
        decide(second, generation)
    }

    private fun decide(observation: YoutubeV2Observation, generation: Long) {
        scope.launch {
            val decision = repository.decide(observation.exactHandle, observation.displayName)
            if (generation != evaluationGeneration || observation.stableKey != lastEvaluatedKey) return@launch
            handler.post {
                if (decision == YoutubeV2RuntimeDecision.ALLOW) {
                    blockOverlayVisible = false
                    overlay.dismiss()
                } else {
                    block()
                }
            }
        }
    }

    private fun block() {
        pauseMedia()
        performGlobalAction(GLOBAL_ACTION_BACK)
        blockOverlayVisible = true
        overlay.showContent(
            title = "Channel blocked",
            subtitle = "This channel is not Productive. Add its @handle in SAFAR to allow it.",
            buttonText = "OK",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
        )
    }

    private fun blockShorts() {
        if (blockOverlayVisible) return
        evaluationGeneration++
        handler.removeCallbacks(debounce)
        firstStableRead = null
        lastEvaluatedKey = null
        ownerMissingSinceMs = null
        session.onBrowsing()
        pauseMedia()
        navigateToYoutubeHome()
        blockOverlayVisible = true
        overlay.showContent(
            title = "YouTube Shorts blocked",
            subtitle = "Shorts are blocked in Study Mode.",
            buttonText = "OK",
            onAction = {
                blockOverlayVisible = false
                overlay.dismiss()
            },
        )
    }

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

    companion object {
        private const val DEBOUNCE_MS = 500L
        private const val STABILITY_GAP_MS = 300L
        private const val CLICK_TRANSITION_MS = 500L
        private const val SHORTS_PROBE_MS = 150L
        private const val OWNER_EVIDENCE_RETRY_MS = 250L
        private const val OWNER_EVIDENCE_WAIT_MS = 3_000L
        private const val HEARTBEAT_MS = 30_000L
        private const val CLICK_PARENT_DEPTH = 5
        private const val MAX_NODES = 900
    }
}
