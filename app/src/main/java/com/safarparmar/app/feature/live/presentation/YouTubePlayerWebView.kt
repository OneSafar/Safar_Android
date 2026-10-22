package com.safarparmar.app.feature.live.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.safarparmar.app.ui.theme.DarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Inline player; only YouTube's fullscreen button changes orientation. */
@Composable
fun YouTubePlayerWebView(
    embedUrl: String,
    modifier: Modifier = Modifier,
    fullscreenChat: (@Composable () -> Unit)? = null,
    fullscreenTitle: String = "",
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val compositionContext = rememberCompositionContext()
    val currentFullscreenChat by rememberUpdatedState(fullscreenChat)
    val currentFullscreenTitle by rememberUpdatedState(fullscreenTitle)
    val currentDensity by rememberUpdatedState(LocalDensity.current)
    // YouTube requires WebView clients to identify themselves using the app ID.
    val origin = remember(context.packageName) { "https://${context.packageName}" }
    val playerUrl = remember(embedUrl, origin) { buildInlineEmbedUrl(embedUrl, origin) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var fullscreenContainer by remember { mutableStateOf<ComposeView?>(null) }
    var fullscreenBackCallback by remember { mutableStateOf<OnBackPressedCallback?>(null) }
    val loadedUrl = remember { arrayOf("") }
    val previousOrientation = remember { intArrayOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    val previousSystemUi = remember { intArrayOf(View.SYSTEM_UI_FLAG_VISIBLE) }

    fun exitFullscreen() {
        val view = fullscreenView ?: return
        fullscreenBackCallback?.remove()
        fullscreenBackCallback = null
        (view.parent as? ViewGroup)?.removeView(view)
        val callback = fullscreenCallback
        fullscreenView = null
        fullscreenCallback = null
        fullscreenContainer?.let { container ->
            (container.parent as? ViewGroup)?.removeView(container)
            container.disposeComposition()
        }
        fullscreenContainer = null
        activity?.requestedOrientation = previousOrientation[0]
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = previousSystemUi[0]
        callback?.onCustomViewHidden()
        webView?.let { player ->
            player.post {
                if (webView === player) {
                    player.evaluateJavascript(
                        "window.safarSetPlayerFullscreen && window.safarSetPlayerFullscreen(false);", null,
                    )
                    player.syncInlinePlayerViewport()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val player = webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player?.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    player?.evaluateJavascript("if(window.player && player.pauseVideo) player.pauseVideo();", null)
                    player?.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player?.onResume()
        else player?.onPause()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose { exitFullscreen() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            object : WebView(ctx) {
                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    // Use the final native bounds, including the layout after rotation.
                    post { if (webView === this) syncInlinePlayerViewport() }
                }

                override fun onTouchEvent(event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    return super.onTouchEvent(event)
                }
            }.apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                // Keep the normal WebView compositor and its current user agent.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.useWideViewPort = true
                // The HTML sizes the iframe explicitly. Automatic page zoom can
                // retain the landscape scale when the inline WebView returns.
                settings.loadWithOverviewMode = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.textZoom = 100
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.syncInlinePlayerViewport()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        // Subframes stay inside the player. Explicit outbound links open externally.
                        if (request.hasGesture()) {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                        }
                        return true
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        val host = activity
                        val root = host?.window?.decorView as? ViewGroup
                        if (view == null || host == null || root == null || fullscreenView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        previousOrientation[0] = host.requestedOrientation
                        @Suppress("DEPRECATION")
                        previousSystemUi[0] = root.systemUiVisibility
                        fullscreenView = view
                        fullscreenCallback = callback
                        webView?.evaluateJavascript(
                            "window.safarSetPlayerFullscreen && window.safarSetPlayerFullscreen(true);", null,
                        )
                        // Install synchronously: Back must not reach the route while
                        // Compose is still creating the fullscreen window content.
                        (host as? ComponentActivity)?.let { owner ->
                            val backCallback = object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() { exitFullscreen() }
                            }
                            fullscreenBackCallback = backCallback
                            owner.onBackPressedDispatcher.addCallback(owner, backCallback)
                        }
                        val fullscreenDensity = currentDensity
                        // Keep YouTube's native fullscreen surface and chat in separate
                        // columns in the same window. Toggling chat never reloads video.
                        val container = ComposeView(ctx).apply {
                            setParentCompositionContext(compositionContext)
                            setContent {
                                // The app recalculates its density on rotation. Preserve
                                // readable chat text and touch targets in this player window.
                                CompositionLocalProvider(LocalDensity provides fullscreenDensity) {
                                    MaterialTheme(colorScheme = DarkColorScheme) {
                                        FullscreenPlayerLayout(
                                            videoView = view,
                                            title = currentFullscreenTitle,
                                            chatContent = currentFullscreenChat,
                                            onExit = { exitFullscreen() },
                                        )
                                    }
                                }
                            }
                        }
                        fullscreenContainer = container
                        root.addView(container, FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                        ))
                        host.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        @Suppress("DEPRECATION")
                        root.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    }
                    override fun onHideCustomView() { exitFullscreen() }
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("YouTubeWebView", message?.message().orEmpty())
                        return true
                    }
                }
                webView = this
            }
        },
        onRelease = { player ->
            exitFullscreen()
            player.stopLoading()
            player.onPause()
            player.webChromeClient = null
            player.removeAllViews()
            player.destroy()
            webView = null
        },
        update = { player ->
            if (loadedUrl[0] != playerUrl) {
                loadedUrl[0] = playerUrl
                // A viewport-sized HTML page avoids treating an embed document as a desktop page.
                // The base URL supplies the referrer to the iframe and the IFrame API.
                player.loadDataWithBaseURL(origin + "/", inlinePlayerHtml(playerUrl), "text/html", "UTF-8", null)
            }
        },
    )
}

// Hallmark · component: fullscreen live chat · pre-emit critique: P4 H4 E4 S4 R5 V4
@Composable
private fun FullscreenPlayerLayout(
    videoView: View,
    title: String,
    chatContent: (@Composable () -> Unit)?,
    onExit: () -> Unit,
) {
    var showChat by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val chatStateHolder = rememberSaveableStateHolder()
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .safeDrawingPadding().imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            if (chatContent != null) {
                FilledTonalIconToggleButton(
                    checked = showChat,
                    onCheckedChange = {
                        focusManager.clearFocus()
                        showChat = it
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(if (showChat)
                            com.safarparmar.app.R.string.live_hide_comments
                            else com.safarparmar.app.R.string.live_show_comments),
                    )
                }
            }
            IconButton(onClick = {
                focusManager.clearFocus()
                onExit()
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.FullscreenExit,
                    contentDescription = stringResource(com.safarparmar.app.R.string.live_exit_fullscreen),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Leave at least half the window for video, including small landscape windows.
            val chatWidth = (maxWidth * 0.34f).coerceIn(280.dp, 360.dp).coerceAtMost(maxWidth * 0.5f)
            Row(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { videoView },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                if (chatContent != null && showChat) {
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Retain the composer and scroll position while the pane is hidden.
                    Box(modifier = Modifier.width(chatWidth).fillMaxHeight()) {
                        chatStateHolder.SaveableStateProvider("fullscreen-chat") { chatContent() }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun buildInlineEmbedUrl(raw: String, origin: String): String {
    val uri = Uri.parse(raw)
    val overridden = setOf("autoplay", "controls", "fs", "enablejsapi", "origin")
    val builder = uri.buildUpon().clearQuery()
    uri.queryParameterNames.filterNot { it in overridden }.forEach { name ->
        uri.getQueryParameters(name).forEach { value -> builder.appendQueryParameter(name, value) }
    }
    // The native thumbnail tap initializes this visible player. Autoplay may still be
    // blocked by the provider; the standard YouTube play control remains available.
    return builder.appendQueryParameter("autoplay", "1")
        .appendQueryParameter("controls", "1")
        .appendQueryParameter("fs", "1")
        .appendQueryParameter("enablejsapi", "1")
        .appendQueryParameter("origin", origin)
        .build().toString()
}

private fun WebView.syncInlinePlayerViewport() {
    if (width <= 0 || height <= 0 || !isAttachedToWindow) return
    // Web content uses Android's system density, not Safar's Compose density.
    val density = resources.displayMetrics.density
    evaluateJavascript(
        "window.safarResizePlayer && window.safarResizePlayer(${width / density}, ${height / density});",
        null,
    )
}

private fun inlinePlayerHtml(url: String): String = """
    <!doctype html>
    <html><head>
      <!-- Keep page zoom fixed. Scale only the iframe so fullscreen cannot
           leave the inline page at a stale landscape zoom level. -->
      <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1, user-scalable=no">
      <meta name="referrer" content="strict-origin-when-cross-origin">
      <style>
        html,body{margin:0;width:100%;height:100%;background:#000;color:white;overflow:hidden}
        #player{width:100%;height:100%;border:0;display:block;transform-origin:top left}
        #player:fullscreen{width:100%!important;height:100%!important;transform:none!important}
        #player:-webkit-full-screen{width:100%!important;height:100%!important;transform:none!important}
        #error{display:none;padding:20px;font:16px sans-serif;box-sizing:border-box}
      </style>
    </head><body>
      <iframe id="player" title="YouTube video" src="${TextUtils.htmlEncode(url)}"
        allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
      <div id="error" role="alert"></div>
      <script>
        var player;
        var nativeWidth = 0, nativeHeight = 0, nativeFullscreen = false;
        function sizePlayer() {
          // Do not resize YouTube's native fullscreen surface using inline bounds.
          if (nativeFullscreen || document.fullscreenElement || document.webkitFullscreenElement) return;
          var width = nativeWidth || window.innerWidth;
          var height = nativeHeight || window.innerHeight;
          if (width <= 0 || height <= 0) return;
          // Give YouTube's standard controls a consistent logical viewport. A
          // CSS transform fits it to the card without depending on WebView zoom.
          var logicalWidth = Math.max(480, width);
          var scale = width / logicalWidth;
          var iframe = document.getElementById('player');
          document.documentElement.style.height = height + 'px';
          document.body.style.height = height + 'px';
          iframe.style.width = logicalWidth + 'px';
          iframe.style.height = (height / scale) + 'px';
          iframe.style.transform = 'scale(' + scale + ')';
        }
        window.safarResizePlayer = function(width, height) {
          nativeWidth = width;
          nativeHeight = height;
          sizePlayer();
        };
        window.safarSetPlayerFullscreen = function(active) {
          nativeFullscreen = active;
          if (!active) window.requestAnimationFrame(sizePlayer);
        };
        function schedulePlayerSize() { window.requestAnimationFrame(sizePlayer); }
        window.addEventListener('resize', schedulePlayerSize);
        window.addEventListener('load', schedulePlayerSize);
        document.addEventListener('fullscreenchange', schedulePlayerSize);
        document.addEventListener('webkitfullscreenchange', schedulePlayerSize);
        sizePlayer();
        function onYouTubeIframeAPIReady() {
          player = new YT.Player('player', {events: {
            onReady: sizePlayer,
            onError: function(event) {
              console.error('YouTube player error: ' + event.data);
              document.getElementById('player').style.display = 'none';
              var error = document.getElementById('error');
              error.style.display = 'block';
              error.textContent = 'Unable to play this video here (YouTube error ' + event.data + ').';
            }
          }});
        }
      </script>
      <script src="https://www.youtube.com/iframe_api"></script>
    </body></html>
""".trimIndent()
