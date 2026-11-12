package minicla03.cashmate.feature.profile.privacy

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.PrivacyInfo

data class PrivacyUiState(
    val isLoading: Boolean = true,
    val privacyInfo: PrivacyInfo? = null,
    val updatingConsentId: String? = null,
    val error: UiError? = null,
    val actionError: UiError? = null,
    val snackbarMessageResId: Int? = null
)

sealed interface PrivacyEvent {
    data object Refresh : PrivacyEvent
    data class ConsentToggled(val consentId: String, val granted: Boolean) : PrivacyEvent
    data object ExportDataClick : PrivacyEvent
    data object DeleteAccountClick : PrivacyEvent
    data object DismissSnackbar : PrivacyEvent
}

sealed interface PrivacyEffect {
    data object NavigateToExportData : PrivacyEffect
    data object NavigateToDeleteAccount : PrivacyEffect
}

