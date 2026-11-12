package minicla03.cashmate.feature.auth.register

import minicla03.cashmate.core.ui.UiError

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameErrorResId: Int? = null,
    val emailErrorResId: Int? = null,
    val passwordErrorResId: Int? = null,
    val confirmPasswordErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val error: UiError? = null
)

sealed interface RegisterEvent {
    data class NameChanged(val value: String) : RegisterEvent
    data class EmailChanged(val value: String) : RegisterEvent
    data class PasswordChanged(val value: String) : RegisterEvent
    data class ConfirmPasswordChanged(val value: String) : RegisterEvent
    data object SubmitClick : RegisterEvent
    data object SignInClick : RegisterEvent
    data object DismissError : RegisterEvent
}

sealed interface RegisterEffect {
    data class NavigateToEmailVerification(val email: String) : RegisterEffect
    data object NavigateBackToLogin : RegisterEffect
}

