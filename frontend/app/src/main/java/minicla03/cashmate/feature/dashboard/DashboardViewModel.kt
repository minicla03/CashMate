package minicla03.cashmate.feature.dashboard

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
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.GetDashboardUseCase
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardUseCase: GetDashboardUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(currency = userPreferencesRepository.preferences.value.currency)
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(currency = prefs.currency) }
            }
            .launchIn(viewModelScope)

        loadDashboard()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.Refresh -> loadDashboard()
            DashboardEvent.AddTransactionClick -> sendEffect(DashboardEffect.NavigateToAddTransaction)
            DashboardEvent.ScanReceiptClick -> sendEffect(DashboardEffect.NavigateToScanReceipt)
            DashboardEvent.ViewAllTransactionsClick -> sendEffect(DashboardEffect.NavigateToTransactions)
            is DashboardEvent.TransactionClick -> sendEffect(
                DashboardEffect.NavigateToTransactionDetail(event.id)
            )
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.summary == null, error = null) }
            when (val result = getDashboardUseCase()) {
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

    private fun sendEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

