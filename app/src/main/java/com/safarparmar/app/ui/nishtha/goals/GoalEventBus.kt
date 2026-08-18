package com.safarparmar.app.ui.nishtha.goals

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-scoped signal for goals that were completed or mutated outside of Nishtha
 * (e.g. from an Ekagra focus session goal link).
 */
object GoalEventBus {
    private val _goalUpdatedFromEkagra = MutableSharedFlow<String>(extraBufferCapacity = 4)

    val goalUpdatedFromEkagra = _goalUpdatedFromEkagra.asSharedFlow()

    fun postGoalUpdated(goalId: String) {
        if (goalId.isNotBlank()) {
            _goalUpdatedFromEkagra.tryEmit(goalId)
        }
    }
}
