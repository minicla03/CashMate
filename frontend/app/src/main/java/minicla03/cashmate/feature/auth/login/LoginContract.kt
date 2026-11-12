package minicla03.cashmate.feature.auth.login

import minicla03.cashmate.core.ui.UiError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailErrorResId: Int? = null,
    val passwordErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val error: UiError? = null,
    val requiresEmailVerification: Boolean = false
)

sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object SubmitClick : LoginEvent
    data object ForgotPasswordClick : LoginEvent
    data object CreateAccountClick : LoginEvent
    data object VerifyEmailClick : LoginEvent
    data object DismissError : LoginEvent
}

sealed interface LoginEffect {
    data object NavigateToForgotPassword : LoginEffect
    data object NavigateToRegister : LoginEffect
    data class NavigateToEmailVerification(val email: String) : LoginEffect
}

