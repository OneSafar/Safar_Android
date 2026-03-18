package com.safar.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val dataStore: SafarDataStore
) : ViewModel() {

    val isDarkTheme = dataStore.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isNightMode = dataStore.isNightMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleDarkTheme() {
        viewModelScope.launch {
            dataStore.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun setNightMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setNightMode(enabled)
        }
    }
}
