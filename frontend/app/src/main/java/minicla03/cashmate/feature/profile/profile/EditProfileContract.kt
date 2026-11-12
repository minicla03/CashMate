package minicla03.cashmate.feature.profile.profile

import minicla03.cashmate.core.ui.UiError

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val nameErrorResId: Int? = null,
    val emailErrorResId: Int? = null,
    val isSubmitting: Boolean = false,
    val loadError: UiError? = null,
    val submitError: UiError? = null
)

sealed interface EditProfileEvent {
    data class NameChanged(val value: String) : EditProfileEvent
    data class EmailChanged(val value: String) : EditProfileEvent
    data object SaveClick : EditProfileEvent
    data object RetryLoad : EditProfileEvent
}

sealed interface EditProfileEffect {
    data object ProfileUpdated : EditProfileEffect
}

