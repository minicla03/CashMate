package minicla03.cashmate.feature.profile.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.usecase.GetPrivacyInfoUseCase
import minicla03.cashmate.domain.usecase.UpdateConsentUseCase
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val getPrivacyInfoUseCase: GetPrivacyInfoUseCase,
    private val updateConsentUseCase: UpdateConsentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PrivacyEffect>(Channel.BUFFERED)
    val effects: Flow<PrivacyEffect> = _effects.receiveAsFlow()

    init {
        loadPrivacyInfo()
    }

    fun onEvent(event: PrivacyEvent) {
        when (event) {
            PrivacyEvent.Refresh -> loadPrivacyInfo()
            is PrivacyEvent.ConsentToggled -> toggleConsent(event.consentId, event.granted)
            PrivacyEvent.ExportDataClick -> sendEffect(PrivacyEffect.NavigateToExportData)
            PrivacyEvent.DeleteAccountClick -> sendEffect(PrivacyEffect.NavigateToDeleteAccount)
            PrivacyEvent.DismissSnackbar -> _uiState.update {
                it.copy(actionError = null, snackbarMessageResId = null)
            }
        }
    }

    private fun loadPrivacyInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.privacyInfo == null, error = null) }
            when (val result = getPrivacyInfoUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, privacyInfo = result.data, error = null)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (it.privacyInfo == null) result.error.toUiError() else null,
                        actionError = if (it.privacyInfo != null) result.error.toUiError() else null
                    )
                }
            }
        }
    }

    private fun toggleConsent(consentId: String, granted: Boolean) {
        if (_uiState.value.updatingConsentId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(updatingConsentId = consentId, actionError = null) }
            when (val result = updateConsentUseCase(consentId, granted)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        updatingConsentId = null,
                        privacyInfo = state.privacyInfo?.copy(consents = result.data),
                        snackbarMessageResId = R.string.privacy_consent_updated_message
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        updatingConsentId = null,
                        actionError = result.error.toUiError()
                    )
                }
            }
        }
    }

    private fun sendEffect(effect: PrivacyEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

