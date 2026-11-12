package minicla03.cashmate.feature.profile.profile

import minicla03.cashmate.core.ui.UiError

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordErrorResId: Int? = null,
    val newPasswordErrorResId: Int? = null,
    val confirmPasswordErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val error: UiError? = null
)

sealed interface ChangePasswordEvent {
    data class CurrentPasswordChanged(val value: String) : ChangePasswordEvent
    data class NewPasswordChanged(val value: String) : ChangePasswordEvent
    data class ConfirmPasswordChanged(val value: String) : ChangePasswordEvent
    data object SubmitClick : ChangePasswordEvent
}

sealed interface ChangePasswordEffect {
    data object PasswordChanged : ChangePasswordEffect
}

