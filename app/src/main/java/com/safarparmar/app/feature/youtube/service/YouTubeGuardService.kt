package com.safarparmar.app.feature.youtube.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.feature.youtube.detector.ParsedUiSnapshot
import com.safarparmar.app.feature.youtube.detector.ScreenType
import com.safarparmar.app.feature.youtube.detector.YouTubeUiParser
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.feature.youtube.policy.PolicyState
import com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility service that monitors YouTube for content policy enforcement.
 *
 * Watches only [YOUTUBE_PACKAGE], parses the UI tree via [YouTubeUiParser] on window content/state
 * changes, intercepts click events ([AccessibilityEvent.TYPE_VIEW_CLICKED]) on Shorts tabs and suggested
 * videos with zero-latency overlay enforcement, and delegates to [YouTubePolicyEngine] for policy decisions.
 * The overlay is rendered by [YouTubeGuardOverlayManager] using TYPE_ACCESSIBILITY_OVERLAY.
 */
@AndroidEntryPoint
class YouTubeGuardService : AccessibilityService() {

    @Inject lateinit var policyEngine: YouTubePolicyEngine
    @Inject lateinit var overlayManager: YouTubeGuardOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastParsed: ParsedUiSnapshot = ParsedUiSnapshot.EMPTY

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Provide the Service context to the overlay manager for WindowManager token binding
        overlayManager.onServiceConnected(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_CLICKED
            packageNames = arrayOf(YOUTUBE_PACKAGE)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = EVENT_DEBOUNCE_MS
        }

        observePolicyState()
        Log.d(TAG, "YouTubeGuardService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // If an event occurs outside YouTube, hide the overlay and stop verification
        if (event.packageName?.toString() != YOUTUBE_PACKAGE) {
            return
        }

        // Fast-path click interception: consume before layout changes
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val handled = handleViewClicked(event)
            if (handled) return
        }

        serviceScope.launch(Dispatchers.Default) {
            val root = rootInActiveWindow
            if (root == null) {
                Log.d(TAG, "rootInActiveWindow is null — skipping event")
                return@launch
            }

            val parsed: ParsedUiSnapshot
            try {
                parsed = YouTubeUiParser.parse(root)
            } catch (e: Exception) {
                Log.e(TAG, "UI parse error", e)
                runCatching { root.recycle() }
                return@launch
            }
            runCatching { root.recycle() }

            // Avoid redundant evaluations for identical parsed UI state
            if (parsed == lastParsed && parsed.screenType != ScreenType.UNKNOWN) return@launch
            lastParsed = parsed

            when (parsed.screenType) {
                ScreenType.SHORTS -> {
                    Log.d(TAG, "Shorts screen detected")
                    policyEngine.onVideoDetected(parsed.toVideoIdentity())
                }
                ScreenType.CHANNEL_PAGE -> {
                    Log.d(TAG, "Channel profile page detected: ${parsed.channelIdentity}")
                    policyEngine.onContentDetected(parsed.toCanonicalContent())
                }
                ScreenType.WATCH -> {
                    Log.d(TAG, "Watch screen detected: title='${parsed.title}' channel='${parsed.channelIdentity}'")
                    policyEngine.onContentDetected(parsed.toCanonicalContent())
                }
                ScreenType.FEED, ScreenType.SEARCH, ScreenType.UNKNOWN -> {
                    if (parsed.channelIdentity.isValid || parsed.title.isNotBlank()) {
                        policyEngine.onContentDetected(parsed.toCanonicalContent())
                    } else {
                        Log.d(TAG, "No video, Shorts, or channel page active — stopping playback policy")
                        policyEngine.onPlaybackStopped()
                        overlayManager.hide()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        overlayManager.hide()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        policyEngine.onPlaybackStopped()
        lastParsed = ParsedUiSnapshot.EMPTY
        overlayManager.onServiceDestroyed()
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        lastParsed = ParsedUiSnapshot.EMPTY
        overlayManager.onServiceDestroyed()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------
    // Click Interception
    // ---------------------------------------------------------------------------

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE)
    internal fun handleViewClicked(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return false
        try {
            if (isShortsClick(source)) {
                Log.d(TAG, "Shorts click intercepted on view: ${source.viewIdResourceName ?: source.contentDescription}")
                overlayManager.showShortsBlocked(
                    onBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
                )
                policyEngine.onShortsDetected()
                return true
            }

            if (isSuggestedVideoClick(source)) {
                Log.d(TAG, "Suggested video click intercepted on view: ${source.viewIdResourceName}")
                overlayManager.showVerifying()
                return true
            }
        } finally {
            runCatching { source.recycle() }
        }
        return false
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE)
    internal fun isShortsClick(node: AccessibilityNodeInfo): Boolean {
        if (matchesShortsClickCriteria(node)) return true

        // Check parent hierarchy (up to 3 levels)
        var currentParent: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (currentParent != null && depth < 3) {
            if (matchesShortsClickCriteria(currentParent)) return true
            val next = currentParent.parent
            currentParent = next
            depth++
        }

        // Check immediate children (1 level)
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            if (matchesShortsClickCriteria(child)) return true
        }

        return false
    }

    private fun matchesShortsClickCriteria(node: AccessibilityNodeInfo): Boolean {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val text = node.text?.toString()?.trim() ?: ""

        if (id.contains("tab_shorts") || id.contains("shorts_tab") ||
            id.contains("reel_tab") || id.contains("reel_shelf") ||
            id.contains("shorts_shelf") || id.contains("reel_item") ||
            id.contains("shorts_card") || id.contains("pivot_shorts") ||
            id.contains("reel_item_renderer")
        ) {
            return true
        }

        if (desc.equals("Shorts", ignoreCase = true) || text.equals("Shorts", ignoreCase = true) ||
            desc.contains("YouTube Shorts", ignoreCase = true) || desc.contains("Shorts tab", ignoreCase = true) ||
            desc.equals("Remix", ignoreCase = true) || text.equals("Remix", ignoreCase = true)
        ) {
            return true
        }

        return false
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE)
    internal fun isSuggestedVideoClick(node: AccessibilityNodeInfo): Boolean {
        if (matchesSuggestedVideoCriteria(node)) return true

        var currentParent: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (currentParent != null && depth < 3) {
            if (matchesSuggestedVideoCriteria(currentParent)) return true
            val next = currentParent.parent
            currentParent = next
            depth++
        }

        return false
    }

    private fun matchesSuggestedVideoCriteria(node: AccessibilityNodeInfo): Boolean {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        return id.contains("compact_video_renderer") ||
            id.contains("watch_next") ||
            id.contains("related_videos") ||
            id.contains("compact_link") ||
            id.contains("compact_movie_renderer") ||
            id.contains("compact_promoted_video_renderer")
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun observePolicyState() {
        policyEngine.state
            .onEach { state -> handlePolicyState(state) }
            .launchIn(serviceScope)
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun handlePolicyState(state: PolicyState) {
        Log.d(TAG, "Policy state → $state")
        when (state) {
            is PolicyState.Allowed -> overlayManager.hide()

            is PolicyState.Verifying -> {
                if (state.identity != null) {
                    overlayManager.showVerifying(
                        channelName = state.identity.channelName,
                        title = state.identity.title,
                    )
                } else {
                    // No active video undergoing verification (e.g. cold start, playback stopped, home/browse screen)
                    overlayManager.hide()
                }
            }

            is PolicyState.Blocked -> overlayManager.showBlocked(
                channelName = state.identity.channelName.ifBlank { state.channelHandle },
                channelHandle = state.channelHandle,
                reason = state.reason,
                onBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            )

            is PolicyState.ShortsBlocked -> overlayManager.showShortsBlocked(
                onBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            )

            is PolicyState.AskUser -> overlayManager.showAskUser(
                channelName = state.identity.channelName.ifBlank { state.channelHandle },
                channelHandle = state.channelHandle,
                onClassify = { handle, policy ->
                    policyEngine.onUserClassified(handle, policy)
                },
                onBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            )
        }
    }

    companion object {
        private const val TAG = "YouTubeGuard"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val EVENT_DEBOUNCE_MS = 300L
    }
}
