package minicla03.cashmate.feature.profile.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.GetPreferencesUseCase
import minicla03.cashmate.domain.usecase.UpdatePreferencesUseCase
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PreferencesUiState(
            themeMode = userPreferencesRepository.preferences.value.themeMode,
            selectedCurrency = userPreferencesRepository.preferences.value.currency
        )
    )
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update {
                    it.copy(
                        themeMode = prefs.themeMode,
                        selectedCurrency = prefs.currency
                    )
                }
            }
            .launchIn(viewModelScope)

        loadPreferences()
    }

    fun onEvent(event: PreferencesEvent) {
        when (event) {
            PreferencesEvent.Refresh -> loadPreferences()
            is PreferencesEvent.ThemeSelected -> updatePreferences(
                AppPreferences(
                    themeMode = event.themeMode,
                    currency = _uiState.value.selectedCurrency
                )
            )
            is PreferencesEvent.CurrencySelected -> updatePreferences(
                AppPreferences(
                    themeMode = _uiState.value.themeMode,
                    currency = event.currency
                )
            )
            PreferencesEvent.DismissActionError -> _uiState.update { it.copy(actionError = null) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            when (val result = getPreferencesUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        themeMode = result.data.themeMode,
                        selectedCurrency = result.data.currency,
                        loadError = null
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, loadError = result.error.toUiError())
                }
            }
        }
    }

    private fun updatePreferences(newPrefs: AppPreferences) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionError = null) }
            when (val result = updatePreferencesUseCase(newPrefs)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        themeMode = result.data.themeMode,
                        selectedCurrency = result.data.currency,
                        actionError = null
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isSaving = false, actionError = result.error.toUiError())
                }
            }
        }
    }
}

