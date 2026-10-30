package com.safarparmar.app.ui.adaptive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SafarWindowWidthClass {
    Compact,   // < 600dp (standard phone in portrait)
    Medium,    // 600dp..839dp (tablet portrait, foldable unfolded)
    Expanded,  // >= 840dp (tablet landscape, desktop)
}

@Immutable
data class SafarAdaptiveState(
    val widthClass: SafarWindowWidthClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isTablet: Boolean,
    val isLandscape: Boolean,
) {
    /** Window space, not physical device identity, determines whether a rail can fit. */
    val isWideScreen: Boolean
        get() = widthClass != SafarWindowWidthClass.Compact

    val isMediumOrExpanded: Boolean
        get() = widthClass != SafarWindowWidthClass.Compact
}

val LocalSafarAdaptiveState = staticCompositionLocalOf {
    SafarAdaptiveState(
        widthClass = SafarWindowWidthClass.Compact,
        screenWidthDp = 390.dp,
        screenHeightDp = 844.dp,
        isTablet = false,
        isLandscape = false,
    )
}

@Composable
fun rememberSafarAdaptiveState(): SafarAdaptiveState {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = safarWindowWidthClass(widthDp)

    return remember(widthClass, widthDp, heightDp, isTablet, isLandscape) {
        SafarAdaptiveState(
            widthClass = widthClass,
            screenWidthDp = widthDp,
            screenHeightDp = heightDp,
            isTablet = isTablet,
            isLandscape = isLandscape,
        )
    }
}

@Composable
fun ProvideSafarAdaptive(content: @Composable () -> Unit) {
    val state = rememberSafarAdaptiveState()
    CompositionLocalProvider(LocalSafarAdaptiveState provides state) {
        content()
    }
}

/** Pure sizing policy shared by the provider and boundary tests. */
internal fun safarWindowWidthClass(width: Dp): SafarWindowWidthClass = when {
    width < 600.dp -> SafarWindowWidthClass.Compact
    width < 840.dp -> SafarWindowWidthClass.Medium
    else -> SafarWindowWidthClass.Expanded
}

/** Preserve v4's landscape top tabs, including resizable desktop/foldable windows. */
val SafarAdaptiveState.isSpaciousLandscape: Boolean
    get() = isLandscape && screenWidthDp >= 840.dp && screenHeightDp >= 480.dp

val SafarAdaptiveState.usePlannerTopNavigation: Boolean
    get() = isSpaciousLandscape

/** Each pane uses the actual space remaining after navigation, with a height fallback. */
fun SafarAdaptiveState.plannerPanesFit(width: Dp, height: Dp, minimumWidth: Dp = 840.dp): Boolean =
    usePlannerTopNavigation && width >= minimumWidth && height >= 360.dp
