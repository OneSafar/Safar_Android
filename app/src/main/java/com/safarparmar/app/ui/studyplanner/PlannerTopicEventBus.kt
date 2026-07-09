package com.safarparmar.app.ui.studyplanner

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-scoped signal for study-planner topics that were mutated from *outside*
 * the planner — currently only when an Ekagra focus session linked to a topic
 * finishes and marks that topic done.
 *
 * The Ekagra and Study Planner features live in separate ViewModels that never
 * share state, so without this the planner would only reflect the completion on
 * its next cold load. The planner ViewModel collects this flow and reloads the
 * affected plan when its id matches the one currently on screen.
 *
 * Matches the existing [com.safarparmar.app.ui.premium.PaymentEventBus] pattern:
 * a plain singleton `object` with a buffered [MutableSharedFlow] so an emit that
 * happens while nothing is collecting is not lost.
 */
object PlannerTopicEventBus {
    private val _topicCompletedFromEkagra = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Emits the planId whose topic was just completed by an Ekagra session. */
    val topicCompletedFromEkagra = _topicCompletedFromEkagra.asSharedFlow()

    fun postTopicCompleted(planId: String) {
        if (planId.isNotBlank()) {
            _topicCompletedFromEkagra.tryEmit(planId)
        }
    }
}
