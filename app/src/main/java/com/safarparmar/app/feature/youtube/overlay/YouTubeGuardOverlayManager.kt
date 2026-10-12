package com.safarparmar.app.feature.youtube.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.feature.youtube.policy.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the TYPE_ACCESSIBILITY_OVERLAY window hosting the Compose guard UI.
 *
 * Must be initialised with the AccessibilityService context via [onServiceConnected]
 * before any show/hide calls — the service context owns the WindowManager token required
 * to add TYPE_ACCESSIBILITY_OVERLAY windows.
 */
@Singleton
class YouTubeGuardOverlayManager @Inject constructor() {

    private companion object {
        const val TAG = "YouTubeGuard"
    }

    private var serviceContext: Context? = null
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val _overlayState = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()
    private var currentOverlayState: OverlayState = OverlayState.Hidden
    private var isAttached = false
    private var lifecycleOwner: SimpleLifecycleOwner? = null

    // ---------------------------------------------------------------------------
    // Lifecycle hooks called from YouTubeGuardService
    // ---------------------------------------------------------------------------

    /** Call from AccessibilityService.onServiceConnected() */
    fun onServiceConnected(context: Context) {
        serviceContext = context
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        Log.d(TAG, "OverlayManager: service context attached")
    }

    /** Call from AccessibilityService.onDestroy() */
    fun onServiceDestroyed() {
        hide()
        serviceContext = null
        windowManager = null
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    fun showVerifying(
        channelName: String = "",
        title: String = "",
    ) {
        updateState(OverlayState.Verifying(channelName, title))
    }

    fun showBlocked(
        channelName: String,
        channelHandle: String,
        reason: BlockReason,
        onBack: () -> Unit = {},
    ) {
        updateState(OverlayState.Blocked(channelName, channelHandle, reason, onBack))
    }

    fun showShortsBlocked(onBack: () -> Unit = {}) {
        updateState(OverlayState.ShortsBlocked(onBack))
    }

    fun showAskUser(
        channelName: String,
        channelHandle: String,
        onClassify: (String, ChannelPolicy) -> Unit,
        onBack: () -> Unit = {},
    ) {
        updateState(OverlayState.AskUser(channelName, channelHandle, onClassify, onBack))
    }

    fun hide() {
        _overlayState.value = OverlayState.Hidden
        currentOverlayState = OverlayState.Hidden
        if (isAttached) {
            try {
                windowManager?.removeView(overlayView)
                Log.d(TAG, "Overlay removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
            overlayView = null
            lifecycleOwner?.stop()
            lifecycleOwner = null
            isAttached = false
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun updateState(newState: OverlayState) {
        Log.d(TAG, "updateState: ${newState::class.simpleName}, isAttached=$isAttached")
        _overlayState.value = newState
        currentOverlayState = newState

        if (newState is OverlayState.Hidden) {
            hide()
            return
        }

        if (isAttached) {
            // Re-render directly into attached view
            overlayView?.setContent {
                YouTubeGuardOverlay(state = newState)
            }
        } else {
            attachOverlay(newState)
        }
    }

    private fun attachOverlay(initialState: OverlayState) {
        val ctx = serviceContext
        val wm = windowManager
        if (ctx == null || wm == null) {
            Log.e(TAG, "Cannot attach overlay — service context not set.")
            return
        }

        val owner = SimpleLifecycleOwner().also {
            it.start()
            lifecycleOwner = it
        }

        val composeView = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                YouTubeGuardOverlay(state = initialState)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        try {
            wm.addView(composeView, params)
            overlayView = composeView
            isAttached = true
            Log.d(TAG, "Overlay attached to WindowManager (state=${initialState::class.simpleName})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay to WindowManager", e)
        }
    }
}

/**
 * Minimal [LifecycleOwner] / [ViewModelStoreOwner] / [SavedStateRegistryOwner] for a
 * [ComposeView] inside a WindowManager overlay (no Activity / Fragment present).
 */
class SimpleLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun start() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
