package minicla03.cashmate.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionsUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionsUiState(currency = userPreferencesRepository.preferences.value.currency)
    )
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionsEffect>(Channel.BUFFERED)
    val effects: Flow<TransactionsEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(currency = prefs.currency) }
            }
            .launchIn(viewModelScope)

        loadTransactions(debounceMs = 0L)
    }

    fun onEvent(event: TransactionsEvent) {
        when (event) {
            TransactionsEvent.Refresh -> loadTransactions(debounceMs = 0L)

            is TransactionsEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                loadTransactions(debounceMs = SEARCH_DEBOUNCE_MS)
            }

            is TransactionsEvent.TypeFilterChanged -> {
                _uiState.update { it.copy(selectedType = event.type) }
                loadTransactions(debounceMs = 0L)
            }

            is TransactionsEvent.CategoryFilterChanged -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
                loadTransactions(debounceMs = 0L)
            }

            is TransactionsEvent.DatePresetSelected -> {
                _uiState.update {
                    it.copy(
                        datePreset = event.preset,
                        customFromDate = if (event.preset == DateRangePreset.CUSTOM) it.customFromDate else null,
                        customToDate = if (event.preset == DateRangePreset.CUSTOM) it.customToDate else null
                    )
                }
                loadTransactions(debounceMs = 0L)
            }

            is TransactionsEvent.CustomDateRangeSelected -> {
                _uiState.update {
                    it.copy(
                        datePreset = if (event.from != null || event.to != null) {
                            DateRangePreset.CUSTOM
                        } else {
                            DateRangePreset.ALL
                        },
                        customFromDate = event.from,
                        customToDate = event.to
                    )
                }
                loadTransactions(debounceMs = 0L)
            }

            TransactionsEvent.ToggleSortOrder -> {
                _uiState.update {
                    val next = when (it.sortOrder) {
                        TransactionSortOrder.DATE_DESC -> TransactionSortOrder.DATE_ASC
                        TransactionSortOrder.DATE_ASC -> TransactionSortOrder.DATE_DESC
                    }
                    it.copy(sortOrder = next)
                }
                loadTransactions(debounceMs = 0L)
            }

            TransactionsEvent.ClearFilters -> {
                _uiState.update {
                    it.copy(
                        searchQuery = "",
                        selectedType = null,
                        selectedCategoryId = null,
                        datePreset = DateRangePreset.ALL,
                        customFromDate = null,
                        customToDate = null
                    )
                }
                loadTransactions(debounceMs = 0L)
            }

            TransactionsEvent.AddTransactionClick -> sendEffect(TransactionsEffect.NavigateToAddTransaction)

            TransactionsEvent.ScanReceiptClick -> sendEffect(TransactionsEffect.NavigateToScanReceipt)

            is TransactionsEvent.TransactionClick -> sendEffect(
                TransactionsEffect.NavigateToTransactionDetail(event.id)
            )
        }
    }

    private fun loadTransactions(debounceMs: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }

            val snapshot = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }

            val categoriesResult = fetchCategoriesIfNeeded(snapshot.categories)
            val categories = when (categoriesResult) {
                is AppResult.Success -> categoriesResult.data
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = categoriesResult.error.toUiError())
                    }
                    return@launch
                }
            }
            val categoriesById = categories.associateBy { it.id }

            val filter = buildFilter(snapshot)
            when (val txResult = getTransactionsUseCase(filter, categoriesById)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        transactions = txResult.data,
                        categories = categories,
                        categoriesById = categoriesById,
                        error = null
                    )
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        categoriesById = categoriesById,
                        error = txResult.error.toUiError()
                    )
                }
            }
        }
    }

    private suspend fun fetchCategoriesIfNeeded(
        current: List<Category>
    ): AppResult<List<Category>> = coroutineScope {
        if (current.isNotEmpty()) {
            async { getCategoriesUseCase() }.await().let { refreshed ->
                when (refreshed) {
                    is AppResult.Success -> refreshed
                    is AppResult.Error -> AppResult.Success(current)
                }
            }
        } else {
            getCategoriesUseCase()
        }
    }

    private fun buildFilter(state: TransactionsUiState): TransactionFilter {
        val today = LocalDate.now(clock)
        val (fromDate, toDate) = when (state.datePreset) {
            DateRangePreset.ALL -> null to null
            DateRangePreset.THIS_MONTH -> {
                val month = YearMonth.from(today)
                month.atDay(1) to month.atEndOfMonth()
            }
            DateRangePreset.LAST_MONTH -> {
                val prevMonth = YearMonth.from(today).minusMonths(1)
                prevMonth.atDay(1) to prevMonth.atEndOfMonth()
            }
            DateRangePreset.LAST_30_DAYS -> today.minusDays(29) to today
            DateRangePreset.CUSTOM -> state.customFromDate to state.customToDate
        }
        return TransactionFilter(
            query = state.searchQuery.takeIf { it.isNotBlank() },
            categoryId = state.selectedCategoryId,
            type = state.selectedType,
            fromDate = fromDate,
            toDate = toDate,
            sortOrder = state.sortOrder
        )
    }

    private fun sendEffect(effect: TransactionsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L
    }
}

