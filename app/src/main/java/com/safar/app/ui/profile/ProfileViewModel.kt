package com.safar.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: SafarDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = authRepository.getMe()) {
                is Resource.Success -> {
                    val p = r.data
                    _uiState.update { it.copy(isLoading = false, userName = p.name, userEmail = p.email, userAvatar = p.avatar, examType = p.examType ?: "", preparationStage = p.preparationStage ?: "", gender = p.gender ?: "", editName = p.name, editExamType = p.examType ?: "", editStage = p.preparationStage ?: "", editGender = p.gender ?: "") }
                }
                is Resource.Error -> {
                    val name = dataStore.userName.first() ?: ""
                    val avatar = dataStore.userAvatar.first()
                    _uiState.update { it.copy(isLoading = false, userName = name, userAvatar = avatar, editName = name) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ShowLogoutDialog  -> _uiState.update { it.copy(showLogoutDialog = true) }
            is ProfileEvent.DismissLogoutDialog -> _uiState.update { it.copy(showLogoutDialog = false) }
            is ProfileEvent.ClearError        -> _uiState.update { it.copy(error = null) }
            is ProfileEvent.Logout            -> handleLogout()
            is ProfileEvent.SaveProfile       -> saveProfile()
            is ProfileEvent.UpdateName        -> _uiState.update { it.copy(editName = event.name) }
            is ProfileEvent.UpdateExamType    -> _uiState.update { it.copy(editExamType = event.exam) }
            is ProfileEvent.UpdateStage       -> _uiState.update { it.copy(editStage = event.stage) }
            is ProfileEvent.UpdateGender      -> _uiState.update { it.copy(editGender = event.gender) }
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val s = _uiState.value
            when (val r = authRepository.updateProfile(s.editName.ifBlank { null }, s.editExamType.ifBlank { null }, s.editStage.ifBlank { null }, s.editGender.ifBlank { null }, null)) {
                is Resource.Success -> _uiState.update { it.copy(isSaving = false, saveSuccess = true, userName = r.data.name, examType = r.data.examType ?: "", preparationStage = r.data.preparationStage ?: "", gender = r.data.gender ?: "") }
                is Resource.Error   -> _uiState.update { it.copy(isSaving = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showLogoutDialog = false) }
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch { authRepository.logout(); onDone() }
    }
}
