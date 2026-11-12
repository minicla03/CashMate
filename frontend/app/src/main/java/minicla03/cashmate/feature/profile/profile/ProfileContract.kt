package minicla03.cashmate.feature.profile.profile

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: UiError? = null,
    val isLoggingOut: Boolean = false,
    val showLogoutDialog: Boolean = false
)

sealed interface ProfileEvent {
    data object Refresh : ProfileEvent
    data object EditProfileClick : ProfileEvent
    data object ChangePasswordClick : ProfileEvent
    data object PreferencesClick : ProfileEvent
    data object ManageCategoriesClick : ProfileEvent
    data object PrivacyClick : ProfileEvent
    data object ExportDataClick : ProfileEvent
    data object DeleteAccountClick : ProfileEvent
    data object LogoutClick : ProfileEvent
    data object ConfirmLogout : ProfileEvent
    data object DismissLogoutDialog : ProfileEvent
}

sealed interface ProfileEffect {
    data object NavigateToEditProfile : ProfileEffect
    data object NavigateToChangePassword : ProfileEffect
    data object NavigateToPreferences : ProfileEffect
    data object NavigateToCategories : ProfileEffect
    data object NavigateToPrivacy : ProfileEffect
    data object NavigateToExportData : ProfileEffect
    data object NavigateToDeleteAccount : ProfileEffect
}

