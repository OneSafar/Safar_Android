package com.safar.app.ui.butterfly

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates and remembers a [ButterflyTourState] that survives recomposition.
 *
 * Usage:
 * ```kotlin
 * val tourState = rememberButterflyTourState(
 *     ButterflyTourStep("Welcome 🦋", "Let me show you around!", 0.15f, 0.18f),
 *     ButterflyTourStep("Navigation", "Your main menu lives here.", 0.50f, 0.93f),
 *     ButterflyTourStep("Profile", "Tap here to edit your profile.", 0.88f, 0.10f),
 * )
 * ```
 */
@Composable
fun rememberButterflyTourState(vararg steps: ButterflyTourStep): ButterflyTourState {
    return remember { ButterflyTourState(steps.toList()) }
}

@Composable
fun rememberButterflyTourState(steps: List<ButterflyTourStep>): ButterflyTourState {
    return remember { ButterflyTourState(steps) }
}
