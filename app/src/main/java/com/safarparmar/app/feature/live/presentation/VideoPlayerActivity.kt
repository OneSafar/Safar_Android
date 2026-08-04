package com.safarparmar.app.feature.live.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.feature.live.data.LiveSocketConnector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A dedicated, plain Android Activity (no Compose) for full-screen video playback.
 *
 * WHY a separate Activity instead of a Compose overlay:
 * ─────────────────────────────────────────────────────
 * Android's WebView renders video through an internal SurfaceView that punches a
 * "transparent hole" in the current Window's surface and composites video frames
 * from a separate hardware layer below it (via SurfaceFlinger).
 *
 * Inside a Compose hierarchy the extra compositing layers (Scaffold, Surface, etc.)
 * interfere with this mechanism — the hole is punched but the video layer isn't
 * visible behind the Compose surfaces.
 *
 * A plain Activity with setContentView(webView) has NO extra compositing layers.
 * The WebView is directly the window's content, so SurfaceView compositing works
 * correctly and video frames are visible.
 */
@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {

    @Inject lateinit var socketManager: MehfilSocketManager
    @Inject lateinit var socketConnector: LiveSocketConnector

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var rootLayout: FrameLayout

    /** Holds the video and the chat pane as siblings; never composited over each other. */
    private lateinit var contentRow: LinearLayout
    private var chatPane: LiveChatPaneView? = null

    private var sessionId: String = ""
    private var sessionStatus: String = ""
    private var currentUserName: String = "Student"
    private var currentUserId: String = ""
    private var cooldownSeconds: Int = DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS
    private var cooldownJob: Job? = null

    companion object {
        private const val EXTRA_EMBED_URL = "extra_embed_url"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_SESSION_STATUS = "extra_session_status"

        /**
         * @param sessionId when non-blank, the live comments pane is shown beside
         *   the video. Students used to have to back out of the player to comment,
         *   which meant they could never do both at once.
         */
        fun start(
            context: Context,
            embedUrl: String,
            videoTitle: String = "",
            sessionId: String? = null,
            sessionStatus: String? = null,
        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_EMBED_URL, embedUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_SESSION_ID, sessionId.orEmpty())
                putExtra(EXTRA_SESSION_STATUS, sessionStatus.orEmpty())
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Window setup ────────────────────────────────────────────────────────
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        customView != null -> {
                            // First Back exits YouTube's own full-screen view.
                            customViewCallback?.onCustomViewHidden()
                        }
                        webView?.canGoBack() == true -> webView?.goBack()
                        else -> finishAfterTransition()
                    }
                }
            },
        )

        // A live session is a 16:9 landscape video, and this screen exists only to
        // play it. Locking a phone to PORTRAIT letterboxed that video into a thin
        // strip with black above and below — so "fullscreen" still meant the
        // student had to rotate or hunt for YouTube's own fullscreen button.
        // Opening in landscape makes the video fill the screen immediately, which
        // is the whole point of this activity.
        //
        // SENSOR_LANDSCAPE (not plain LANDSCAPE) so both landscape orientations
        // work and the phone is not forced to one physical direction. Tablets keep
        // full sensor freedom — their screen is large enough that portrait is still
        // a perfectly good viewing position.
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        val embedUrl = intent.getStringExtra(EXTRA_EMBED_URL) ?: run { finish(); return }
        val sanitizedUrl = buildPlayerEmbedUrl(embedUrl)

        // ── Root layout ─────────────────────────────────────────────────────────
        rootLayout = FrameLayout(this).also { it.setBackgroundColor(Color.BLACK) }
        setContentView(rootLayout)

        // ── WebView ─────────────────────────────────────────────────────────────
        val wv = WebView(this).apply {
            // Do NOT set LAYER_TYPE_HARDWARE — see YouTubePlayerWebView.kt for explanation.
            // Default LAYER_TYPE_NONE lets SurfaceFlinger composite video correctly.

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                textZoom = 100
                mediaPlaybackRequiresUserGesture = false
                loadsImagesAutomatically = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadWithOverviewMode = true
                useWideViewPort = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptCanOpenWindowsAutomatically = true
                // Remove the " wv" marker so YouTube serves the full player.
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; K) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Mobile Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    // Allow YouTube embed navigation inside the WebView
                    if (url.contains("youtube.com/embed/") ||
                        url.contains("youtube-nocookie.com/embed/")
                    ) return false
                    // Block everything else (share links, etc.)
                    return true
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.contains("youtube.com/embed/") ||
                        url.contains("youtube-nocookie.com/embed/")
                    ) return false
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Re-hide bars after page load (some WebView versions restore them)
                    hideSystemBars()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) return
                    // Video going fullscreen: add the custom view on top of the WebView
                    customView = view
                    customViewCallback = callback
                    rootLayout.addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    // Hide the whole row, not just the WebView: YouTube's own
                    // fullscreen should be exactly that, with no chat beside it.
                    contentRow.visibility = View.GONE
                    hideSystemBars()
                }

                override fun onHideCustomView() {
                    val cv = customView ?: return
                    rootLayout.removeView(cv)
                    customView = null
                    customViewCallback = null
                    contentRow.visibility = View.VISIBLE
                    hideSystemBars()
                }

                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("VideoPlayerActivity", "JS: ${msg?.message()}")
                    return super.onConsoleMessage(msg)
                }
            }
        }

        webView = wv

        // Video and chat sit side by side inside contentRow. YouTube's own
        // fullscreen (customView) is still added straight to rootLayout, so it
        // covers both and gives a genuinely uninterrupted picture.
        contentRow = LinearLayout(this).apply {
            orientation = if (isLandscape()) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        rootLayout.addView(
            contentRow,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        sessionStatus = intent.getStringExtra(EXTRA_SESSION_STATUS).orEmpty()

        addVideoAndChat(wv)

        wv.loadUrl(sanitizedUrl, mapOf("Referer" to "https://safar.parmarssc.in/"))

        if (sessionId.isNotBlank()) startLiveChat()
    }

    private fun isLandscape(): Boolean =
        resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    /**
     * Video takes the majority of the space; chat gets a readable column beside it
     * in landscape, or a strip below it in portrait.
     */
    private fun addVideoAndChat(wv: WebView) {
        contentRow.removeAllViews()
        val landscape = isLandscape()
        contentRow.orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        if (sessionId.isBlank()) {
            contentRow.addView(
                wv,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            return
        }

        val pane = chatPane ?: LiveChatPaneView(this).also { created ->
            created.onSend = ::sendComment
            chatPane = created
        }
        (pane.parent as? ViewGroup)?.removeView(pane)
        (wv.parent as? ViewGroup)?.removeView(wv)

        if (landscape) {
            contentRow.addView(wv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.62f))
            contentRow.addView(pane, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.38f))
        } else {
            contentRow.addView(wv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.45f))
            contentRow.addView(pane, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.55f))
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        webView?.let { addVideoAndChat(it) }
    }

    // ── Live chat ────────────────────────────────────────────────────────────────

    private fun startLiveChat() {
        val pane = chatPane ?: return
        pane.setChatOpen(false, "Connecting to live comments…")

        lifecycleScope.launch {
            currentUserName = socketConnector.currentUserName()
            currentUserId = socketConnector.currentUserId()
            when (val result = socketConnector.ensureConnected()) {
                is LiveSocketConnector.Result.SignInRequired ->
                    pane.setChatOpen(false, result.message)
                is LiveSocketConnector.Result.Connecting -> Unit
            }
        }

        // Join as soon as the socket is up, and re-join after any reconnect.
        lifecycleScope.launch {
            socketManager.connected.collect { isConnected ->
                if (isConnected && sessionId.isNotBlank()) {
                    socketManager.emitLiveJoin(sessionId)
                }
            }
        }

        lifecycleScope.launch {
            socketManager.liveMessage.collect { msg ->
                pane.addMessage(
                    author = msg.name,
                    text = msg.text,
                    isMine = msg.userId.isNotBlank() && msg.userId == currentUserId,
                    isHost = msg.isHost,
                )
            }
        }

        lifecycleScope.launch {
            socketManager.liveChatState.collect { state ->
                if (state.sessionId != sessionId) return@collect
                if (state.cooldownSeconds > 0) cooldownSeconds = state.cooldownSeconds
                if (!state.isChatOpen) cooldownJob?.cancel()
                pane.setChatOpen(state.isChatOpen, closedReasonFor(sessionStatus))
            }
        }

        lifecycleScope.launch {
            socketManager.liveViewerCount.collect { viewers ->
                if (viewers.sessionId != sessionId) return@collect
                pane.setViewerCount(viewers.count, isLive = sessionStatus == "live")
            }
        }

        lifecycleScope.launch {
            socketManager.liveStatusChanged.collect { change ->
                if (change.sessionId != sessionId) return@collect
                sessionStatus = change.status
                if (!change.status.equals("live", ignoreCase = true)) {
                    cooldownJob?.cancel()
                    pane.setChatOpen(false, closedReasonFor(change.status))
                }
            }
        }

        lifecycleScope.launch {
            socketManager.liveError.collect { error ->
                when (error.code) {
                    "RATE_LIMITED" -> startCooldown(
                        ((error.retryAfterMs + 999L) / 1000L).toInt().coerceAtLeast(1),
                    )
                    "CHAT_CLOSED" -> pane.setChatOpen(false, closedReasonFor(sessionStatus))
                    else -> android.util.Log.w("VideoPlayerActivity", "live:error ${error.message}")
                }
            }
        }
    }

    private fun sendComment(text: String) {
        if (sessionId.isBlank()) return
        if (!socketManager.isConnected()) {
            lifecycleScope.launch { socketConnector.ensureConnected() }
            return
        }
        socketManager.emitLiveMessage(
            sessionId = sessionId,
            name = currentUserName,
            text = text.take(500),
        )
        // Padded by a second: the server's window opens when it receives the
        // message, so a timer started here would otherwise expire slightly early
        // and the next send would race it.
        startCooldown(cooldownSeconds + 1)
    }

    private fun startCooldown(seconds: Int) {
        val pane = chatPane ?: return
        cooldownJob?.cancel()
        cooldownJob = lifecycleScope.launch {
            var remaining = seconds.coerceAtLeast(1)
            pane.setCooldown(remaining)
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1
                pane.setCooldown(remaining)
            }
        }
    }

    private fun closedReasonFor(status: String): String = when (status.lowercase()) {
        "scheduled" -> "Comments open when the session goes live."
        "ended" -> "This session has ended, so comments are closed."
        "cancelled" -> "This session was cancelled."
        else -> "Comments are turned off for this session."
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onDestroy() {
        cooldownJob?.cancel()
        if (sessionId.isNotBlank()) {
            runCatching { socketManager.emitLiveLeave(sessionId) }
        }
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        webView = null
        super.onDestroy()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Builds a sanitised YouTube embed URL with all required parameters
     * for autoplay and inline playback.
     */
    private fun buildPlayerEmbedUrl(raw: String): String {
        var url = raw.replace("autoplay=0", "autoplay=1")

        val sep = if (url.contains("?")) "&" else "?"
        val extras = buildString {
            if (!url.contains("autoplay=")) append("autoplay=1&")
            if (!url.contains("controls=")) append("controls=1&")
            if (!url.contains("playsinline=")) append("playsinline=1&")
            if (!url.contains("fs=")) append("fs=1&")
            if (!url.contains("rel=")) append("rel=0&")
            if (!url.contains("enablejsapi=")) append("enablejsapi=1&")
        }.trimEnd('&')

        if (extras.isNotEmpty()) url = "$url$sep$extras"
        return url
    }
}
