package com.safar.app.ui.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatar: String? = null,
    val examType: String = "",
    val preparationStage: String = "",
    val gender: String = "",
    val editName: String = "",
    val editExamType: String = "",
    val editStage: String = "",
    val editGender: String = "",
    val saveSuccess: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null
)

sealed class ProfileEvent {
    object ShowLogoutDialog : ProfileEvent()
    object DismissLogoutDialog : ProfileEvent()
    object Logout : ProfileEvent()
    object ClearError : ProfileEvent()
    object SaveProfile : ProfileEvent()
    data class UpdateName(val name: String) : ProfileEvent()
    data class UpdateExamType(val exam: String) : ProfileEvent()
    data class UpdateStage(val stage: String) : ProfileEvent()
    data class UpdateGender(val gender: String) : ProfileEvent()
}
