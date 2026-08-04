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
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
class VideoPlayerActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var rootLayout: FrameLayout

    companion object {
        private const val EXTRA_EMBED_URL = "extra_embed_url"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"

        fun start(context: Context, embedUrl: String, videoTitle: String = "") {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_EMBED_URL, embedUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
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
                    this@VideoPlayerActivity.webView?.visibility = View.GONE
                    hideSystemBars()
                }

                override fun onHideCustomView() {
                    val cv = customView ?: return
                    rootLayout.removeView(cv)
                    customView = null
                    customViewCallback = null
                    this@VideoPlayerActivity.webView?.visibility = View.VISIBLE
                    hideSystemBars()
                }

                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("VideoPlayerActivity", "JS: ${msg?.message()}")
                    return super.onConsoleMessage(msg)
                }
            }
        }

        webView = wv
        rootLayout.addView(
            wv,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        wv.loadUrl(sanitizedUrl, mapOf("Referer" to "https://safar.parmarssc.in/"))
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
