package minicla03.cashmate.feature.auth.forgotpassword

import minicla03.cashmate.core.ui.UiError

data class ResetPasswordUiState(
    val email: String = "",
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val emailErrorResId: Int? = null,
    val tokenErrorResId: Int? = null,
    val newPasswordErrorResId: Int? = null,
    val confirmPasswordErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: UiError? = null
)

sealed interface ResetPasswordEvent {
    data class EmailChanged(val value: String) : ResetPasswordEvent
    data class TokenChanged(val value: String) : ResetPasswordEvent
    data class NewPasswordChanged(val value: String) : ResetPasswordEvent
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordEvent
    data object SubmitClick : ResetPasswordEvent
    data object ContinueToLoginClick : ResetPasswordEvent
}

sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
}

