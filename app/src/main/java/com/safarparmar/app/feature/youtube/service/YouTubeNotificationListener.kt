package com.safarparmar.app.feature.youtube.service

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.safarparmar.app.feature.youtube.policy.VideoIdentity
import com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationListenerService that hooks into MediaSessionManager to receive YouTube's
 * MediaSession metadata, serving as a fallback for fullscreen/PiP video detection
 * when the accessibility UI tree is not reliably parseable (e.g. fullscreen video player).
 *
 * This covers the scenario where the user goes fullscreen and [YouTubeGuardService]'s
 * UI tree parsing becomes unreliable or returns stale data.
 */
@AndroidEntryPoint
class YouTubeNotificationListener : NotificationListenerService() {

    @Inject lateinit var policyEngine: YouTubePolicyEngine

    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var lastTitle: String = ""

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return
            handleMetadataChange(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // If playback has stopped/ended, notify the policy engine
            if (state == null ||
                state.state == PlaybackState.STATE_STOPPED ||
                state.state == PlaybackState.STATE_NONE
            ) {
                listenerScope.launch {
                    policyEngine.onPlaybackStopped()
                }
            }
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val cn = ComponentName(this, YouTubeNotificationListener::class.java)
        try {
            val sessions = mediaSessionManager?.getActiveSessions(cn)
            updateActiveController(sessions)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn)
        } catch (_: SecurityException) {
            // Permission not yet granted; will retry when sessions update
        }
    }

    override fun onListenerDisconnected() {
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        activeController?.unregisterCallback(mediaCallback)
        activeController = null
        listenerScope.cancel()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) { /* not used */ }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { /* not used */ }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun updateActiveController(controllers: List<MediaController>?) {
        // Prefer YouTube controller
        val youtubeController = controllers?.firstOrNull {
            it.packageName == YouTubeGuardService.YOUTUBE_PACKAGE
        }

        if (youtubeController?.sessionToken == activeController?.sessionToken) return

        activeController?.unregisterCallback(mediaCallback)
        activeController = youtubeController
        activeController?.registerCallback(mediaCallback)

        // Immediately process current metadata if available
        activeController?.metadata?.let { handleMetadataChange(it) }
    }

    private fun handleMetadataChange(metadata: MediaMetadata) {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val albumArtist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""

        // Avoid duplicate processing of the same title
        if (title == lastTitle) return
        lastTitle = title

        // YouTube uses ARTIST for channel name in MediaSession
        val channelName = artist.ifBlank { albumArtist }

        val identity = VideoIdentity(
            title = title,
            channelName = channelName,
            // Handle is not available via MediaSession; policy engine will fall back to name lookup
            channelHandle = "",
            isShorts = false, // MediaSession doesn't signal Shorts
            isAd = false,     // Ads don't emit MediaSession metadata
        )

        listenerScope.launch {
            policyEngine.onVideoDetected(identity)
        }
    }
}
