package com.safar.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
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

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode = _isNightMode.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language = _language.asStateFlow()

    init {
        viewModelScope.launch {
            _isDarkTheme.value = dataStore.isDarkTheme.first()
            _isNightMode.value = dataStore.isNightMode.first()
            _language.value    = dataStore.language.first()
        }
    }

    fun toggleDarkTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        viewModelScope.launch { dataStore.setDarkTheme(newValue) }
    }

    fun toggleNightMode() {
        val newValue = !_isNightMode.value
        _isNightMode.value = newValue
        viewModelScope.launch { dataStore.setNightMode(newValue) }
    }

    fun setNightMode(enabled: Boolean) {
        _isNightMode.value = enabled
        viewModelScope.launch { dataStore.setNightMode(enabled) }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        viewModelScope.launch { dataStore.setLanguage(lang) }
    }
}
