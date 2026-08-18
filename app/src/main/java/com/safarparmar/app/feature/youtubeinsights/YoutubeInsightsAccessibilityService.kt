package com.safarparmar.app.feature.youtubeinsights

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.Context
import android.media.AudioManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldEntryPoint
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import android.widget.Toast
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reads only YouTube's visible accessibility tree; raw nodes and titles are never persisted. */
class YoutubeInsightsAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val decisionMutex = Mutex()
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, FocusShieldEntryPoint::class.java)
    }
    private val repository get() = entryPoint.youtubeInsightsRepository()
    private var lastProcessedElapsed = 0L
    private var activeBlockKey: String? = null
    private var lastMediaPauseElapsed = 0L
    private var lastEscapeElapsed = 0L
    private var foregroundPackage: String? = null
    private var usageEventWatermarkMs = 0L
    private var pendingClickedChannel: String? = null
    private var pendingClickedChannelAt = 0L
    private var pendingVideoTapAt = 0L
    private var userOpenedVideo = false
    private var wasBrowsingYoutube = false
    /** Monotonic token prevents an older IO decision from restoring a stale overlay. */
    @Volatile private var decisionGeneration = 0L

    private val watchdog = object : Runnable {
        override fun run() {
            processActiveWindow()
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onServiceConnected() {
        handler.removeCallbacks(watchdog)
        handler.post(watchdog)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != YoutubeUiParser.YOUTUBE_PACKAGE) return
        val now = SystemClock.elapsedRealtime()
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val cardDescription = clickedNodeDescriptions(event).firstOrNull(YoutubeUiParser::isClickedVideoCard)
            val clickedChannel = YoutubeUiParser.channelFromClickedVideo(cardDescription)
            debugLog("click card=${cardDescription != null} channel=${clickedChannel ?: "unknown"}")
            clickedChannel?.let {
                pendingClickedChannel = it
                pendingClickedChannelAt = now
            }
            if (cardDescription != null) {
                pendingVideoTapAt = now
                // The click is reported while the old feed hierarchy can still be
                // active. Do not mistake its autoplay preview for the opened video
                // and press Back out of YouTube; inspect after navigation starts.
                handler.postDelayed(::processActiveWindow, CLICK_TRANSITION_DELAY_MS)
                return
            }
        }
        if (now - lastProcessedElapsed < EVENT_DEBOUNCE_MS) return
        lastProcessedElapsed = now
        processActiveWindow()
    }

    override fun onInterrupt() {
        handler.removeCallbacks(watchdog)
        decisionGeneration += 1
        scope.launch { repository.stop() }
        activeBlockKey = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        decisionGeneration += 1
        scope.launch {
            repository.stop()
            scope.cancel()
        }
        activeBlockKey = null
        super.onDestroy()
    }

    private fun processActiveWindow() {
        val generation = ++decisionGeneration
        if (!repository.enabled.value) {
            clearBlockState()
            scope.launch {
                decisionMutex.withLock {
                    if (generation == decisionGeneration) repository.stop()
                }
            }
            return
        }
        if (currentForegroundPackage() != YoutubeUiParser.YOUTUBE_PACKAGE) {
            clearPlaybackSession()
            clearBlockState()
            scope.launch {
                decisionMutex.withLock {
                    if (generation == decisionGeneration) repository.stop()
                }
            }
            return
        }
        val root = rootInActiveWindow
        if (root?.packageName?.toString() != YoutubeUiParser.YOUTUBE_PACKAGE) {
            clearBlockState()
            scope.launch {
                decisionMutex.withLock {
                    if (generation == decisionGeneration) repository.stop()
                }
            }
            return
        }
        var detection = YoutubeUiParser.parse(YoutubeUiSnapshot(readNodes(root), YoutubeUiParser.YOUTUBE_PACKAGE))
        val now = SystemClock.elapsedRealtime()
        val recentVideoTap = pendingVideoTapAt > 0L && now - pendingVideoTapAt <= PENDING_VIDEO_TAP_MAX_AGE_MS
        val pendingChannel = pendingClickedChannel?.takeIf {
            now - pendingClickedChannelAt <= PENDING_CHANNEL_MAX_AGE_MS
        }
        debugLog(
            "raw kind=${detection.kind} channel=${detection.channelName ?: "unknown"} " +
                "playing=${detection.isPlaying} recentTap=$recentVideoTap browsed=$wasBrowsingYoutube " +
                "opened=$userOpenedVideo",
        )
        if (detection.kind == YoutubeContentKind.VIDEO) {
            // Some YouTube/OEM combinations emit TYPE_VIEW_CLICKED with a null or
            // leaf-only source. A browse -> full-watch transition is equivalent
            // evidence of an intentional open. Inline autoplay never leaves the
            // browse snapshot, so it cannot arm blocking through this path.
            if (recentVideoTap || wasBrowsingYoutube) {
                userOpenedVideo = true
                pendingVideoTapAt = 0L
            }
            wasBrowsingYoutube = false
            // Player-like nodes also exist for silent Home/search thumbnail previews
            // and the collapsed mini-player. A normal video is eligible for analytics
            // and blocking only after a real video-card click was observed.
            if (!userOpenedVideo) {
                detection = YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)
            } else {
                if (detection.channelName == null && pendingChannel != null) {
                    detection = detection.copy(channelName = pendingChannel)
                }
                if (detection.channelName != null) clearPendingChannel()
            }
        } else if (detection.kind == YoutubeContentKind.NON_PLAYBACK && !recentVideoTap) {
            userOpenedVideo = false
            clearPendingChannel()
            wasBrowsingYoutube = true
        }
        scope.launch {
            decisionMutex.withLock {
                if (generation != decisionGeneration) return@withLock
                repository.observe(detection)
                val protectedNow = FocusShieldRepository.Snapshot.active
                val shouldBlock = repository.shouldBlock(detection, protectedNow)
                debugLog(
                    "decision kind=${detection.kind} channel=${detection.channelName ?: "unknown"} " +
                        "scope=${repository.channelScope.value} protected=$protectedNow block=$shouldBlock",
                )
                if (generation != decisionGeneration) return@withLock
                if (shouldBlock) {
                    val shorts = detection.kind == YoutubeContentKind.SHORTS
                    val blockKey = if (shorts) "shorts" else
                        "channel:${detection.channelName?.let(YoutubeUiParser::normalizeChannel)}"

                    val isNewAttempt = activeBlockKey != blockKey
                    if (isNewAttempt) {
                        activeBlockKey = blockKey
                        runCatching {
                            entryPoint.focusShieldRepository().recordBlockedHit(YoutubeUiParser.YOUTUBE_PACKAGE)
                        }
                        if (!shorts) {
                            val channelName = detection.channelName
                            val channelKey = channelName?.let { repository.channelKey(it) }
                            if (channelName != null && channelKey != null &&
                                repository.shouldShowBlockedChannelNotification(channelKey)
                            ) {
                                runCatching {
                                    YoutubeChannelNotifications.showBlocked(this@YoutubeInsightsAccessibilityService, channelKey, channelName)
                                }
                            }
                        }
                    }
                    escapeBlockedContent(shorts, showMessage = isNewAttempt)
                } else {
                    clearBlockState()
                }
            }
        }
    }

    private fun clearBlockState() {
        activeBlockKey = null
        lastMediaPauseElapsed = 0L
        lastEscapeElapsed = 0L
    }

    private fun clearPendingChannel() {
        pendingClickedChannel = null
        pendingClickedChannelAt = 0L
    }

    private fun clearPlaybackSession() {
        clearPendingChannel()
        pendingVideoTapAt = 0L
        userOpenedVideo = false
        wasBrowsingYoutube = false
    }

    /** Includes parent labels because YouTube often reports the clicked child icon. */
    private fun clickedNodeDescriptions(event: AccessibilityEvent): List<CharSequence> {
        val descriptions = mutableListOf<CharSequence>()
        event.contentDescription?.let(descriptions::add)
        if (event.text.isNotEmpty()) descriptions += event.text.joinToString(" ")
        var node = event.source
        var depth = 0
        while (node != null && depth++ < CLICKED_NODE_PARENT_DEPTH) {
            node.contentDescription?.let(descriptions::add)
            node.text?.let(descriptions::add)
            node = node.parent
        }
        return descriptions
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(DEBUG_TAG, message)
    }

    /**
     * A touch-blocking overlay is not a content blocker: YouTube continues video,
     * audio and PiP underneath it. Pause the player and leave the blocked surface
     * instead. Back is retried only if YouTube is still verified foreground.
     */
    private fun escapeBlockedContent(shorts: Boolean, showMessage: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastEscapeElapsed < ESCAPE_RETRY_MS) return
        lastEscapeElapsed = now
        pauseYoutubeMedia()
        handler.post {
            pauseVisiblePlayerNode()
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (showMessage) {
                Toast.makeText(
                    this,
                    if (shorts) "YouTube Shorts blocked by SAFAR" else "Distracting channel blocked by SAFAR",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun pauseYoutubeMedia() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMediaPauseElapsed < MEDIA_PAUSE_REPEAT_MS) return
        lastMediaPauseElapsed = now
        val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }

    /** Prefer YouTube's explicit Pause accessibility action when its media session ignores MEDIA_PAUSE. */
    private fun pauseVisiblePlayerNode() {
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != YoutubeUiParser.YOUTUBE_PACKAGE) return
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var playerFallback: AccessibilityNodeInfo? = null
        var visited = 0
        while (queue.isNotEmpty() && visited++ < MAX_NODES) {
            val node = queue.removeFirst()
            val description = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
            val id = node.viewIdResourceName.orEmpty().lowercase()
            val explicitPause = description == "pause" || description == "रोकें" ||
                (id.contains("play_pause") && description.contains("pause"))
            if (explicitPause && node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            if (playerFallback == null && node.isClickable &&
                (id.contains("reel_watch") || id.contains("shorts_player"))
            ) playerFallback = node
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        // Shorts commonly expose only the player surface; clicking it pauses.
        playerFallback?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Accessibility events are restricted to YouTube, so they cannot tell us that
     * SAFAR/launcher became foreground. UsageEvents provides that missing transition
     * and correctly treats a YouTube PiP window as background content.
     */
    private fun currentForegroundPackage(): String? {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return rootInActiveWindow?.packageName?.toString()
        val now = System.currentTimeMillis()
        val from = if (usageEventWatermarkMs == 0L) now - FOREGROUND_INITIAL_LOOKBACK_MS
        else usageEventWatermarkMs - FOREGROUND_OVERLAP_MS
        val events = runCatching { manager.queryEvents(from, now) }.getOrNull()
        if (events != null) {
            val event = UsageEvents.Event()
            var latestAt = Long.MIN_VALUE
            var latestPackage: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val resumed = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
                if (resumed && event.timeStamp >= latestAt && !event.packageName.isNullOrBlank()) {
                    latestAt = event.timeStamp
                    latestPackage = event.packageName
                }
            }
            if (latestPackage != null) foregroundPackage = latestPackage
        }
        usageEventWatermarkMs = now
        return foregroundPackage ?: rootInActiveWindow?.packageName?.toString()
    }

    private fun readNodes(root: AccessibilityNodeInfo): List<YoutubeUiNode> {
        val result = ArrayList<YoutubeUiNode>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && result.size < MAX_NODES) {
            val node = queue.removeFirst()
            result += YoutubeUiNode(
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                className = node.className?.toString(),
                selected = node.isSelected,
                clickable = node.isClickable,
            )
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return result
    }

    companion object {
        private const val EVENT_DEBOUNCE_MS = 75L
        private const val WATCHDOG_MS = 900L
        // After tapping a video card the transition to the watch screen can take
        // ~200–800 ms on mid-range devices.  Wait long enough for the first player
        // frame before evaluating so we never mis-classify the departing feed.
        private const val CLICK_TRANSITION_DELAY_MS = 250L
        // On slow connections the watch-screen player hierarchy (watch_player node)
        // can take several seconds to appear.  Keep the clicked channel alive long
        // enough to be applied when the player finally renders.
        private const val PENDING_CHANNEL_MAX_AGE_MS = 7_000L
        private const val PENDING_VIDEO_TAP_MAX_AGE_MS = 7_000L
        private const val CLICKED_NODE_PARENT_DEPTH = 4
        private const val DEBUG_TAG = "SafarYoutubeStudy"
        private const val MAX_NODES = 700
        private const val MEDIA_PAUSE_REPEAT_MS = 1_500L
        private const val ESCAPE_RETRY_MS = 900L
        private const val FOREGROUND_INITIAL_LOOKBACK_MS = 60_000L
        private const val FOREGROUND_OVERLAP_MS = 500L
    }
}
