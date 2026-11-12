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
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.common.MoneyParser
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.toFieldMessageResIds
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.CreateTransactionUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionUseCase
import minicla03.cashmate.domain.usecase.TransactionValidator
import minicla03.cashmate.domain.usecase.UpdateTransactionUseCase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val transactionValidator: TransactionValidator,
    userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : ViewModel() {

    private val transactionId: String? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(
            isEditMode = transactionId != null,
            date = LocalDate.now(clock),
            currency = userPreferencesRepository.preferences.value.currency
        )
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionFormEffect>(Channel.BUFFERED)
    val effects: Flow<TransactionFormEffect> = _effects.receiveAsFlow()

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                _uiState.update { it.copy(currency = prefs.currency) }
            }
            .launchIn(viewModelScope)

        loadInitialData()
    }

    fun onEvent(event: TransactionFormEvent) {
        when (event) {
            TransactionFormEvent.RetryLoad -> loadInitialData()

            is TransactionFormEvent.TypeChanged -> _uiState.update {
                it.copy(type = event.type)
            }

            is TransactionFormEvent.AmountChanged -> _uiState.update {
                it.copy(
                    amountInput = event.value,
                    fieldErrors = it.fieldErrors - ValidationField.AMOUNT
                )
            }

            is TransactionFormEvent.DescriptionChanged -> _uiState.update {
                it.copy(
                    description = event.value,
                    fieldErrors = it.fieldErrors - ValidationField.DESCRIPTION
                )
            }

            is TransactionFormEvent.CategorySelected -> _uiState.update {
                it.copy(
                    selectedCategoryId = event.categoryId,
                    fieldErrors = it.fieldErrors - ValidationField.CATEGORY
                )
            }

            is TransactionFormEvent.AccountSelected -> _uiState.update {
                it.copy(
                    selectedAccountId = event.accountId,
                    fieldErrors = it.fieldErrors - ValidationField.ACCOUNT
                )
            }

            is TransactionFormEvent.DateSelected -> _uiState.update {
                it.copy(
                    date = event.date,
                    fieldErrors = it.fieldErrors - ValidationField.DATE
                )
            }

            TransactionFormEvent.Submit -> submitForm()

            TransactionFormEvent.DismissSubmitError -> _uiState.update {
                it.copy(submitError = null)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            coroutineScope {
                val categoriesDeferred = async { getCategoriesUseCase() }
                val accountsDeferred = async { getAccountsUseCase() }
                val txDeferred = transactionId?.let { id -> async { getTransactionUseCase(id) } }

                val categoriesResult = categoriesDeferred.await()
                val accountsResult = accountsDeferred.await()
                val txResult = txDeferred?.await()

                val firstError = listOfNotNull(
                    categoriesResult as? AppResult.Error,
                    accountsResult as? AppResult.Error,
                    txResult as? AppResult.Error
                ).firstOrNull()

                if (firstError != null) {
                    _uiState.update {
                        it.copy(isLoading = false, loadError = firstError.error.toUiError())
                    }
                    return@coroutineScope
                }

                val categories = (categoriesResult as AppResult.Success).data
                val accounts = (accountsResult as AppResult.Success).data
                val existingTx = (txResult as? AppResult.Success)?.data

                _uiState.update { current ->
                    if (existingTx != null) {
                        current.copy(
                            isLoading = false,
                            type = existingTx.type,
                            amountInput = existingTx.amount.toDecimalInputString(),
                            description = existingTx.description,
                            selectedCategoryId = existingTx.categoryId,
                            selectedAccountId = existingTx.accountId,
                            date = existingTx.date,
                            categories = categories,
                            accounts = accounts,
                            loadError = null
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            selectedCategoryId = current.selectedCategoryId
                                ?: categories.firstOrNull { c -> c.id != SALARY_CATEGORY_ID }?.id
                                ?: categories.firstOrNull()?.id,
                            selectedAccountId = current.selectedAccountId ?: accounts.firstOrNull()?.id,
                            categories = categories,
                            accounts = accounts,
                            loadError = null
                        )
                    }
                }
            }
        }
    }

    private fun submitForm() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val parsedAmount = MoneyParser.parse(current.amountInput)
        val candidate = Transaction(
            id = transactionId.orEmpty(),
            amount = parsedAmount ?: Money.ZERO,
            type = current.type,
            description = current.description,
            categoryId = current.selectedCategoryId.orEmpty(),
            accountId = current.selectedAccountId.orEmpty(),
            date = current.date
        )

        val validationErrors = transactionValidator.validate(candidate)
            ?.toFieldMessageResIds()
            ?.toMutableMap()
            ?: mutableMapOf()

        if (parsedAmount == null || parsedAmount <= Money.ZERO) {
            validationErrors[ValidationField.AMOUNT] = R.string.validation_amount_positive
        }

        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = validationErrors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null, fieldErrors = emptyMap()) }
            val result = if (transactionId == null) {
                createTransactionUseCase(candidate)
            } else {
                updateTransactionUseCase(candidate)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(TransactionFormEffect.NavigateBackSaved)
                }

                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            fieldErrors = err.toFieldMessageResIds()
                        )
                    }

                    else -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = err.toUiError()
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
        private const val SALARY_CATEGORY_ID = "cat-salary"
    }
}

