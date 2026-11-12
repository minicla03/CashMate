package minicla03.cashmate.feature.statistics

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
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.GetStatisticsUseCase
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatisticsUiState(currency = userPreferencesRepository.preferences.value.currency)
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(currency = prefs.currency) }
            }
            .launchIn(viewModelScope)

        loadStatistics()
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            StatisticsEvent.Refresh -> loadStatistics()
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.summary == null, error = null) }
            when (val result = getStatisticsUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = result.data,
                        error = null
                    )
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.toUiError()
                    )
                }
            }
        }
    }
}

