package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.compositionLocalOf
import java.time.LocalDate

internal val LocalHabitPendingCells = compositionLocalOf<Set<Pair<Long, LocalDate>>> { emptySet() }
