package minicla03.cashmate.feature.profile.deleteaccount

import minicla03.cashmate.core.ui.UiError

data class DeleteAccountUiState(
    val confirmationText: String = "",
    val password: String = "",
    val acknowledgedConsequences: Boolean = false,
    val confirmationErrorResId: Int? = null,
    val showFinalConfirmationDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val error: UiError? = null
)

sealed interface DeleteAccountEvent {
    data class ConfirmationTextChanged(val value: String) : DeleteAccountEvent
    data class PasswordChanged(val value: String) : DeleteAccountEvent
    data class AcknowledgedChanged(val acknowledged: Boolean) : DeleteAccountEvent
    data object RequestDeleteClick : DeleteAccountEvent
    data object ConfirmDeleteClick : DeleteAccountEvent
    data object DismissDialog : DeleteAccountEvent
}

