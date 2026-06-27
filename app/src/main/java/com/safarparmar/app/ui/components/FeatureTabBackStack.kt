package com.safarparmar.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class FeatureTabBackStack<T>(initialTab: T) {
    var currentTab by mutableStateOf(initialTab)
        private set

    private val history = mutableStateListOf<T>()

    val hasHistory: Boolean
        get() = history.isNotEmpty()

    fun select(tab: T) {
        if (tab == currentTab) return
        history.add(currentTab)
        currentTab = tab
    }

    fun replace(tab: T) {
        currentTab = tab
        history.clear()
    }

    fun seedRoot(rootTab: T) {
        if (rootTab == currentTab || history.isNotEmpty()) return
        history.add(rootTab)
    }

    fun goBack(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        currentTab = previous
        return true
    }
}

@Composable
fun <T> rememberFeatureTabBackStack(
    initialTab: T,
    rootTab: T = initialTab,
): FeatureTabBackStack<T> =
    remember(initialTab, rootTab) {
        FeatureTabBackStack(initialTab).also { it.seedRoot(rootTab) }
    }
