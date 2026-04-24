package com.safar.app.ui.butterfly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lightweight state holder – can be hoisted into any ViewModel or used directly
 * via [rememberButterflyTourState].
 */
class ButterflyTourState(
    val steps: List<ButterflyTourStep>,
) {
    /** Whether the overlay is currently visible */
    var isVisible by mutableStateOf(false)
        internal set

    /** Index of the step currently being shown / flying-to */
    var currentStepIndex by mutableIntStateOf(0)
        internal set

    val currentStep: ButterflyTourStep?
        get() = steps.getOrNull(currentStepIndex)

    val isLastStep: Boolean
        get() = currentStepIndex >= steps.lastIndex

    fun start() {
        currentStepIndex = 0
        isVisible = true
    }

    fun next() {
        if (!isLastStep) currentStepIndex++
        else dismiss()
    }

    fun dismiss() {
        isVisible = false
    }

    fun restart() {
        currentStepIndex = 0
        isVisible = true
    }
}
