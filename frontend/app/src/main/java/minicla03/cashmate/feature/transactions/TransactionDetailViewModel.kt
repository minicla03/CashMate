package minicla03.cashmate.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
import minicla03.cashmate.core.common.getOrNull
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.model.TransactionSource
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.DeleteTransactionUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionReceiptUseCase
import minicla03.cashmate.domain.usecase.GetTransactionUseCase
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getTransactionReceiptUseCase: GetTransactionReceiptUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle.get<String>(ARG_TRANSACTION_ID)) {
        "transactionId argument is required"
    }

    private val _uiState = MutableStateFlow(
        TransactionDetailUiState(currency = userPreferencesRepository.preferences.value.currency)
    )
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionDetailEffect>(Channel.BUFFERED)
    val effects: Flow<TransactionDetailEffect> = _effects.receiveAsFlow()

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(currency = prefs.currency) }
            }
            .launchIn(viewModelScope)

        loadTransaction()
    }

    fun onEvent(event: TransactionDetailEvent) {
        when (event) {
            TransactionDetailEvent.Refresh -> loadTransaction()
            TransactionDetailEvent.EditClick -> sendEffect(
                TransactionDetailEffect.NavigateToEdit(transactionId)
            )
            TransactionDetailEvent.DeleteClick -> _uiState.update {
                it.copy(showDeleteDialog = true)
            }
            TransactionDetailEvent.DismissDeleteDialog -> _uiState.update {
                it.copy(showDeleteDialog = false)
            }
            TransactionDetailEvent.ConfirmDelete -> deleteTransaction()
            TransactionDetailEvent.DismissActionError -> _uiState.update {
                it.copy(actionError = null)
            }
        }
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            coroutineScope {
                val txDeferred = async { getTransactionUseCase(transactionId) }
                val categoriesDeferred = async { getCategoriesUseCase() }
                val accountsDeferred = async { getAccountsUseCase() }

                val txResult = txDeferred.await()
                val categories = categoriesDeferred.await().getOrNull().orEmpty()
                val accounts = accountsDeferred.await().getOrNull().orEmpty()

                when (txResult) {
                    is AppResult.Success -> {
                        val tx = txResult.data
                        val hasReceiptOrigin = tx.source == TransactionSource.RECEIPT || tx.receiptId != null
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                transaction = tx,
                                category = categories.firstOrNull { c -> c.id == tx.categoryId },
                                account = accounts.firstOrNull { a -> a.id == tx.accountId },
                                categories = categories,
                                receipt = if (hasReceiptOrigin) it.receipt else null,
                                isReceiptLoading = hasReceiptOrigin,
                                receiptUnavailable = false,
                                error = null
                            )
                        }
                        if (hasReceiptOrigin) {
                            when (val receiptResult = getTransactionReceiptUseCase(tx.id)) {
                                is AppResult.Success -> _uiState.update {
                                    it.copy(
                                        receipt = receiptResult.data,
                                        isReceiptLoading = false,
                                        receiptUnavailable = receiptResult.data == null
                                    )
                                }

                                is AppResult.Error -> _uiState.update {
                                    it.copy(
                                        receipt = null,
                                        isReceiptLoading = false,
                                        receiptUnavailable = true
                                    )
                                }
                            }
                        }
                    }

                    is AppResult.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isReceiptLoading = false,
                            error = txResult.error.toUiError()
                        )
                    }
                }
            }
        }
    }

    private fun deleteTransaction() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true, actionError = null) }
            when (val result = deleteTransactionUseCase(transactionId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    sendEffect(TransactionDetailEffect.NavigateBackAfterDelete)
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        actionError = result.error.toUiError()
                    )
                }
            }
        }
    }

    private fun sendEffect(effect: TransactionDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
    }
}

