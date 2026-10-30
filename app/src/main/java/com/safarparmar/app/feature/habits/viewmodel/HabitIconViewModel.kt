package com.safarparmar.app.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.feature.habits.data.HabitIconStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HabitIconViewModel @Inject constructor(store: HabitIconStore) : ViewModel() {
    val icons = store.icons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
