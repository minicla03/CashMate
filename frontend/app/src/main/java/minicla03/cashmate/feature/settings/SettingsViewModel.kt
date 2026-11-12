package minicla03.cashmate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = userPreferencesRepository.preferences.value.themeMode,
            selectedCurrency = userPreferencesRepository.preferences.value.currency
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

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
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> userPreferencesRepository.setThemeMode(event.themeMode)
            is SettingsEvent.CurrencySelected -> userPreferencesRepository.setCurrency(event.currency)
            SettingsEvent.ManageCategoriesClick -> viewModelScope.launch {
                _effects.send(SettingsEffect.NavigateToCategories)
            }
        }
    }
}

