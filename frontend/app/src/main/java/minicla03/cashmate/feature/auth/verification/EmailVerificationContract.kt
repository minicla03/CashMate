package minicla03.cashmate.feature.auth.verification

import minicla03.cashmate.core.ui.UiError

data class EmailVerificationUiState(
    val email: String = "",
    val token: String = "",
    val emailErrorResId: Int? = null,
    val tokenErrorResId: Int? = null,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val isVerified: Boolean = false,
    val resendMessageResId: Int? = null,
    val error: UiError? = null
)

sealed interface EmailVerificationEvent {
    data class EmailChanged(val value: String) : EmailVerificationEvent
    data class TokenChanged(val value: String) : EmailVerificationEvent
    data object VerifyClick : EmailVerificationEvent
    data object ResendClick : EmailVerificationEvent
    data object ContinueToLoginClick : EmailVerificationEvent
    data object DismissError : EmailVerificationEvent
}

sealed interface EmailVerificationEffect {
    data object NavigateToLogin : EmailVerificationEffect
}

