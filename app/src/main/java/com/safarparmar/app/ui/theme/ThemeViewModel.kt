package com.safarparmar.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    val dataStore: SafarDataStore
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            _isDarkTheme.value = dataStore.isDarkTheme.first()
        }
    }

    fun toggleDarkTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        viewModelScope.launch { dataStore.setDarkTheme(newValue) }
    }

}
