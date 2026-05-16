package com.safar.app.ui.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.ekagra.focusshield.FocusShieldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LaunchUsageQuestionnaireUiState(
    val selectedReasons: Set<Int> = emptySet()
)

@HiltViewModel
class LaunchUsageQuestionnaireViewModel @Inject constructor(
    private val dataStore: SafarDataStore,
    private val focusShieldRepository: FocusShieldRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchUsageQuestionnaireUiState())
    val uiState: StateFlow<LaunchUsageQuestionnaireUiState> = _uiState.asStateFlow()

    fun toggleReason(index: Int) {
        _uiState.update {
            val current = it.selectedReasons
            val next = if (index in current) current - index else current + index
            it.copy(selectedReasons = next)
        }
    }

    fun setFocusShieldEnabled(enabled: Boolean) {
        focusShieldRepository.setEnabled(enabled)
    }

    fun markQuestionnaireFinished(mode: String, onDone: () -> Unit) {
        viewModelScope.launch {
            dataStore.setAppUsageMode(mode)
            dataStore.setLaunchUsageQuestionnaireCompleted(true)
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
