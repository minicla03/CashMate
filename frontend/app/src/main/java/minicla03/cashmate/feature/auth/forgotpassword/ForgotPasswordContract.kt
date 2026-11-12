package minicla03.cashmate.feature.auth.forgotpassword

import minicla03.cashmate.core.ui.UiError

data class ForgotPasswordUiState(
    val email: String = "",
    val emailErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val requestSent: Boolean = false,
    val error: UiError? = null
)

sealed interface ForgotPasswordEvent {
    data class EmailChanged(val value: String) : ForgotPasswordEvent
    data object SubmitClick : ForgotPasswordEvent
    data object ProceedToResetClick : ForgotPasswordEvent
}

sealed interface ForgotPasswordEffect {
    data class NavigateToResetPassword(val email: String) : ForgotPasswordEffect
}

