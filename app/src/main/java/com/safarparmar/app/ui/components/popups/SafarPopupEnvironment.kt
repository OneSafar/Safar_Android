package com.safarparmar.app.ui.components.popups

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

/** The app scale survives the platform providers installed by a popup's new ComposeView. */
val LocalSafarPopupDensity = staticCompositionLocalOf<Density?> { null }

internal val LocalPopupSlotStateHolder = staticCompositionLocalOf<SaveableStateHolder?> { null }

/** Re-applies the feature's scale after crossing a native popup or portal boundary. */
@Composable
fun ProvideSafarPopupScale(
    density: Density = LocalSafarPopupDensity.current ?: LocalDensity.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSafarPopupDensity provides density,
        LocalDensity provides density,
        content = content,
    )
}

/** Use inside popup content; it also retains form state when its isolated context changes. */
@Composable
fun SafarPopupContent(content: @Composable () -> Unit) {
    val holder = LocalPopupSlotStateHolder.current
    val slotKey = currentCompositeKeyHash
    ProvideSafarPopupScale {
        // A native Dialog can recreate just its own composition when exact
        // density changes inside the same integer-dpi bucket. Each slot owns
        // a registry whose state is saved before its children unregister.
        if (holder == null) content()
        else holder.SaveableStateProvider(slotKey, content)
    }
}

/** Popup-local context for native pickers; Activity and WebView resources are untouched. */
@Composable
fun rememberSafarPopupContext(): Context {
    val hostContext = LocalContext.current
    val hostConfiguration = LocalConfiguration.current
    // Capture the actual feature environment before creating another window.
    // A feature can intentionally tighten scale below the app theme's default.
    val density = LocalDensity.current
    val resourceScale = popupResourceScale(density.density, density.fontScale)
    return remember(hostContext, hostConfiguration, resourceScale) {
        val configuration = Configuration(hostConfiguration).apply {
            densityDpi = resourceScale.densityDpi
            fontScale = resourceScale.fontScale
        }
        SafarPopupContext(hostContext, configuration)
    }
}

/**
 * Launches an existing dialog or modal sheet from a view with popup-only resources.
 *
 * Android creates another ComposeView for those windows and replaces LocalDensity
 * with Density(view.context). The bridge makes that replacement use the app scale
 * for the library's own padding, controls and sheet chrome as well as its slots.
 * It never changes the Activity's resources or replaces native Back/drag handling.
 *
 * Only window-launching content belongs here. Anchored menus need their real anchor
 * view and should use SafarPopupContent inside the panel instead.
 */
@Composable
fun SafarPopupEnvironment(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val popupContext = rememberSafarPopupContext()
    val parentComposition = rememberCompositionContext()
    val latestContent = rememberUpdatedState(content)
    // Resource density is rounded to an integer dpi. Exact app-density changes
    // within that same dpi bucket must still reach the content provider.
    val latestDensity = rememberUpdatedState(density)
    val popupState = rememberSaveable(saver = PopupSavedState.Saver) { PopupSavedState() }
    val slotStateHolder = rememberSaveableStateHolder()
    val parentRegistry = LocalSaveableStateRegistry.current
    val viewId = rememberSaveable { View.generateViewId() }
    val bridge = remember { PopupBridgeSlot() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateOwner = LocalSavedStateRegistryOwner.current
    val viewModelOwner = LocalViewModelStoreOwner.current
    val hostView = LocalView.current
    val hostRoot = hostView.rootView as? ViewGroup
        ?: error("A popup bridge needs an attached Android view hierarchy")

    DisposableEffect(popupContext, hostRoot, parentComposition, lifecycleOwner, savedStateOwner, viewModelOwner) {
        // Attach outside the Compose layout tree. Even a zero-size AndroidView
        // would count as a Row/Column child and add spacedBy gaps behind a popup.
        bridge.view?.let { previous ->
            previous.disposeComposition()
            (previous.parent as? ViewGroup)?.removeView(previous)
        }
        val registry = popupState.attach { parentRegistry?.canBeSaved(it) ?: true }
        val popupView = ComposeView(popupContext).apply {
            id = viewId
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setViewTreeViewModelStoreOwner(viewModelOwner)
            setParentCompositionContext(parentComposition)
            setContent {
                ProvideSafarPopupScale(latestDensity.value) {
                    CompositionLocalProvider(
                        LocalSaveableStateRegistry provides registry,
                        LocalPopupSlotStateHolder provides slotStateHolder,
                    ) {
                        latestContent.value()
                    }
                }
            }
        }
        bridge.view = popupView
        hostRoot.addView(popupView, ViewGroup.LayoutParams(0, 0))

        onDispose {
            // Native dialog slots unregister their providers while the bridge is
            // disposed. Capture them while both compositions are still alive.
            popupState.detach(registry)
            // Dispose first so saved-state providers settle before a replacement
            // bridge registers the same stable view id and content key.
            popupView.disposeComposition()
            (popupView.parent as? ViewGroup)?.removeView(popupView)
            if (bridge.view === popupView) bridge.view = null
        }
    }
}

internal class PopupSavedState(
    private var restored: Map<String, List<Any?>> = emptyMap(),
) {
    private var active: SaveableStateRegistry? = null

    fun attach(canBeSaved: (Any) -> Boolean): SaveableStateRegistry =
        SaveableStateRegistry(restored, canBeSaved).also { active = it }

    fun detach(registry: SaveableStateRegistry) {
        if (active === registry) {
            restored = registry.performSave()
            active = null
        }
    }

    private fun snapshot(): Map<String, List<Any?>> = active?.performSave() ?: restored

    companion object {
        val Saver = Saver<PopupSavedState, Map<String, List<Any?>>>(
            save = { it.snapshot() },
            restore = { PopupSavedState(it) },
        )
    }
}

private class PopupBridgeSlot {
    var view: ComposeView? = null
}

internal data class PopupResourceScale(val densityDpi: Int, val fontScale: Float)

internal fun popupResourceScale(density: Float, fontScale: Float): PopupResourceScale {
    require(density.isFinite() && density > 0f) { "Popup density must be positive and finite" }
    require(fontScale.isFinite() && fontScale > 0f) { "Popup font scale must be positive and finite" }
    return PopupResourceScale((density * 160f).roundToInt().coerceAtLeast(1), fontScale)
}

/** Retains Activity unwrapping and system services while isolating only resource scaling. */
internal class SafarPopupContext(
    private val hostContext: Context,
    configuration: Configuration,
) : ContextWrapper(hostContext) {
    private val popupResources = hostContext.createConfigurationContext(configuration).resources
    private val popupTheme by lazy {
        popupResources.newTheme().apply { setTo(hostContext.theme) }
    }
    private val popupInflater by lazy { LayoutInflater.from(hostContext).cloneInContext(this) }

    override fun getBaseContext(): Context = hostContext
    override fun getResources(): Resources = popupResources
    override fun getAssets(): AssetManager = popupResources.assets
    override fun getTheme(): Resources.Theme = popupTheme
    override fun getSystemService(name: String): Any? =
        if (name == Context.LAYOUT_INFLATER_SERVICE) popupInflater else hostContext.getSystemService(name)
}
