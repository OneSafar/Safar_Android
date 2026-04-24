package com.safar.app.ui.tour

import androidx.compose.runtime.*
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.butterfly.ButterflyOverlay
import com.safar.app.ui.butterfly.ButterflyTourState
import com.safar.app.ui.butterfly.ButterflyTourStep
import com.safar.app.ui.butterfly.rememberButterflyTourState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Generic reusable tour wrapper.
 *
 * Place at the end of any screen's root Box so the overlay covers all content.
 * The "Would you like a tour?" dialog is shown once per app install (keyed by DataStore).
 * After the user answers (yes or no) it won't appear again. The butterfly tour can be
 * dismissed at any step via the "close all" button in the tooltip.
 *
 * Pass [onTourStateReady] to receive the [ButterflyTourState] — call state.start() from
 * a Guide icon in the top bar to let the user re-trigger the tour at any time.
 *
 * Usage:
 * ```kotlin
 * var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }
 * Box(Modifier.fillMaxSize()) {
 *     // ... screen content ...
 *     TourManager(
 *         dataStore = dataStore,
 *         steps = mySteps,
 *         onTourStateReady = { tourState = it },
 *     )
 * }
 * // In topBarActions:
 * IconButton(onClick = { tourState?.start() }) { Icon(Icons.Default.HelpOutline, null) }
 * ```
 */
@Composable
fun TourManager(
    dataStore: SafarDataStore,
    steps: List<ButterflyTourStep>,
    askOnFirstVisit: Boolean = true,
    onTourStateReady: ((ButterflyTourState) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var showAskDialog by remember { mutableStateOf(false) }
    val tourState = rememberButterflyTourState(steps)

    // Surface the state to the caller immediately so the guide icon can trigger it
    LaunchedEffect(tourState) {
        onTourStateReady?.invoke(tourState)
    }

    LaunchedEffect(Unit) {
        if (askOnFirstVisit) {
            val done = dataStore.isTourDone.first()
            if (!done) showAskDialog = true
        }
    }

    if (showAskDialog) {
        TourAskDialog(
            onYes = {
                showAskDialog = false
                tourState.start()
                scope.launch { dataStore.setTourDone(true) }
            },
            onNo = {
                showAskDialog = false
                scope.launch { dataStore.setTourDone(true) }
            },
        )
    }

    ButterflyOverlay(state = tourState)
}
