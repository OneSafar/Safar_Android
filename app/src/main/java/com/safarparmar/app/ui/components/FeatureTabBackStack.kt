package com.safarparmar.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Back-stack model for a feature's bottom-nav tabs.
 *
 * Uses the "return-to-start" behaviour recommended for Material bottom navigation:
 * a Back press from ANY non-start tab jumps straight to the start ([rootTab]) in a
 * SINGLE press; a Back press while already on the start tab is not consumed
 * ([goBack] returns false), so the enclosing NavController handles it (→ Home).
 *
 * This intentionally does NOT accumulate a per-tap history. The previous
 * implementation appended the current tab to a list on every switch, so bouncing
 * between tabs (Timer → Duration → History → Timer …) inflated the history and
 * forced one Back press per past tap before the user could leave the feature.
 */
@Stable
class FeatureTabBackStack<T>(val rootTab: T) {
    var currentTab by mutableStateOf(rootTab)
        private set

    /** True when a Back press has something to consume (we're off the start tab). */
    val hasHistory: Boolean
        get() = currentTab != rootTab

    fun select(tab: T) {
        currentTab = tab
    }

    fun replace(tab: T) {
        currentTab = tab
    }

    fun goBack(): Boolean {
        if (currentTab == rootTab) return false
        currentTab = rootTab
        return true
    }
}

@Composable
fun <T> rememberFeatureTabBackStack(
    initialTab: T,
    rootTab: T = initialTab,
): FeatureTabBackStack<T> =
    remember(initialTab, rootTab) {
        FeatureTabBackStack<T>(rootTab).also { it.select(initialTab) }
    }
