package com.safarparmar.app.feature.live.presentation

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubePlayerWebView(
    embedUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val playerHeight = screenWidth * 9f / 16f

    val sanitizedUrl = buildSanitizedEmbedUrl(embedUrl)

    val loadedUrlRef = remember { arrayOf("") }
    var isFullscreen by remember { mutableStateOf(false) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            if (isFullscreen) {
                fullscreenCallback?.onCustomViewHidden()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    Box(
        modifier = if (isFullscreen) {
            Modifier.fillMaxSize().background(Color.Black)
        } else {
            modifier
        },
    ) {
        AndroidView(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(playerHeight)
            },
            factory = { ctx ->
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                WebView(ctx).apply {
                    // Required for smooth video decoding — without this,
                    // video rendering falls back to software and may appear black.
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

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

                        // ─────────────────────────────────────────────────────────
                        // THE PRIMARY FIX
                        // Android WebView appends " wv" to its user-agent string.
                        // YouTube's embed server detects this marker and silently
                        // renders a black player with no controls — this is their
                        // anti-embed policy for native WebViews.
                        // Overriding with a standard Chrome Mobile UA removes the
                        // "wv" marker and YouTube renders the full player normally.
                        // ─────────────────────────────────────────────────────────
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 10; K) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        @Suppress("DEPRECATION")
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?,
                        ): Boolean {
                            if (url == null) return false
                            if (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/")) {
                                return false // Allow loading inside the WebView
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                            } catch (_: Exception) {}
                            return true // Intercept/block in WebView
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.contains("youtube.com/embed/") || url.contains("youtube-nocookie.com/embed/")) {
                                return false // Allow loading inside the WebView
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                            } catch (_: Exception) {}
                            return true // Intercept/block in WebView
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("YouTubeWebView", "Error: ${error?.description} for URL: ${request?.url}")
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("YouTubeWebView", "Console: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return super.onConsoleMessage(consoleMessage)
                        }

                        override fun onShowCustomView(
                            view: View?,
                            callback: CustomViewCallback?,
                        ) {
                            if (view == null) return
                            fullscreenView = view
                            fullscreenCallback = callback
                            isFullscreen = true

                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            @Suppress("DEPRECATION")
                            activity?.window?.decorView?.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )

                            (activity?.window?.decorView as? ViewGroup)?.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }

                        override fun onHideCustomView() {
                            val view = fullscreenView ?: return
                            (activity?.window?.decorView as? ViewGroup)?.removeView(view)

                            fullscreenView = null
                            fullscreenCallback = null
                            isFullscreen = false

                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            @Suppress("DEPRECATION")
                            activity?.window?.decorView?.systemUiVisibility =
                                View.SYSTEM_UI_FLAG_VISIBLE
                        }
                    }
                }
            },
            update = { webView ->
                if (loadedUrlRef[0] != sanitizedUrl) {
                    loadedUrlRef[0] = sanitizedUrl
                    
                    val headers = mutableMapOf<String, String>()
                    headers["Referer"] = "https://safar.parmarssc.in/"
                    
                    webView.loadUrl(sanitizedUrl, headers)
                }
            },
        )
    }
}

/**
 * Normalises the embed URL to include all parameters that YouTube requires
 * to render the full player UI (controls, fullscreen button, inline playback).
 *
 * autoplay=0   — user must tap to start (avoids audio policy blocks on Android)
 * controls=1   — show the play/pause/seek/fullscreen control bar
 * playsinline=1 — play inside the iframe, not in a separate system player
 * fs=1          — show the fullscreen button inside the YouTube player
 * rel=0         — suppress related-video shelf at the end
 * enablejsapi=1 — allow the page JS to communicate with the player
 */
private fun buildSanitizedEmbedUrl(raw: String): String {
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
