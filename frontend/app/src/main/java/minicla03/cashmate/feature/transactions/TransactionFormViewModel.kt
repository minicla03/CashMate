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
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionSource
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.CreateTransactionUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionReceiptUseCase
import minicla03.cashmate.domain.usecase.GetTransactionUseCase
import minicla03.cashmate.domain.usecase.ReceiptCalculator
import minicla03.cashmate.domain.usecase.TransactionValidator
import minicla03.cashmate.domain.usecase.UpdateTransactionUseCase
import minicla03.cashmate.domain.usecase.UpdateTransactionWithReceiptUseCase
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getTransactionReceiptUseCase: GetTransactionReceiptUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val updateTransactionWithReceiptUseCase: UpdateTransactionWithReceiptUseCase,
    private val transactionValidator: TransactionValidator,
    userPreferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) : ViewModel() {

    private val transactionId: String? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)
    private val localItemSequence = AtomicInteger(1)

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

            TransactionFormEvent.OpenAddReceiptItem -> openAddReceiptItem()

            is TransactionFormEvent.OpenEditReceiptItem -> _uiState.update { current ->
                current.copy(
                    itemEditor = ReceiptItemEditorState(
                        itemId = event.item.id,
                        description = event.item.description,
                        quantityInput = event.item.quantity.toString(),
                        unitPriceInput = event.item.unitPrice.toDecimalInputString(),
                        discountInput = event.item.discountAmount.toDecimalInputString(),
                        category = event.item.category
                    )
                )
            }

            is TransactionFormEvent.ItemEditorDescriptionChanged -> updateItemEditor {
                it.copy(description = event.value, descriptionErrorResId = null)
            }

            is TransactionFormEvent.ItemEditorQuantityChanged -> updateItemEditor {
                it.copy(quantityInput = event.value, quantityErrorResId = null)
            }

            is TransactionFormEvent.ItemEditorUnitPriceChanged -> updateItemEditor {
                it.copy(unitPriceInput = event.value, unitPriceErrorResId = null)
            }

            is TransactionFormEvent.ItemEditorDiscountChanged -> updateItemEditor {
                it.copy(discountInput = event.value, discountErrorResId = null)
            }

            is TransactionFormEvent.ItemEditorCategorySelected -> updateItemEditor {
                it.copy(category = event.categoryName)
            }

            TransactionFormEvent.SubmitItemEditor -> submitItemEditor()

            TransactionFormEvent.DismissItemEditor -> _uiState.update {
                it.copy(itemEditor = null)
            }

            is TransactionFormEvent.RequestRemoveReceiptItem -> _uiState.update {
                it.copy(pendingRemoveItem = event.item)
            }

            TransactionFormEvent.ConfirmRemoveReceiptItem -> confirmRemoveReceiptItem()

            TransactionFormEvent.DismissRemoveReceiptItem -> _uiState.update {
                it.copy(pendingRemoveItem = null)
            }
        }
    }

    private fun updateItemEditor(transform: (ReceiptItemEditorState) -> ReceiptItemEditorState) {
        _uiState.update { current ->
            val editor = current.itemEditor ?: return@update current
            current.copy(itemEditor = transform(editor))
        }
    }

    private fun openAddReceiptItem() {
        _uiState.update { current ->
            val defaultCategory = current.categories
                .firstOrNull { it.id == current.selectedCategoryId }?.name
                ?: current.categories.firstOrNull()?.name
                ?: DEFAULT_ITEM_CATEGORY
            current.copy(
                itemEditor = ReceiptItemEditorState(
                    itemId = null,
                    description = "",
                    quantityInput = "1",
                    unitPriceInput = "",
                    discountInput = "0.00",
                    category = defaultCategory
                )
            )
        }
    }

    private fun submitItemEditor() {
        val current = _uiState.value
        val editor = current.itemEditor ?: return
        val currentReceipt = current.receipt ?: return

        val trimmedDescription = editor.description.trim()
        val parsedQty = editor.quantityInput.trim().toIntOrNull()
        val parsedUnitPrice = MoneyParser.parse(editor.unitPriceInput)
        val parsedDiscount = if (editor.discountInput.isBlank()) {
            Money.ZERO
        } else {
            MoneyParser.parse(editor.discountInput)
        }

        val descriptionError = if (trimmedDescription.isEmpty()) {
            R.string.validation_receipt_item_description_required
        } else {
            null
        }
        val quantityError = if (parsedQty == null || parsedQty <= 0) {
            R.string.validation_receipt_item_quantity_positive
        } else {
            null
        }
        val unitPriceError = if (parsedUnitPrice == null || parsedUnitPrice < Money.ZERO) {
            R.string.validation_receipt_item_price_non_negative
        } else {
            null
        }
        var discountError = if (parsedDiscount == null || parsedDiscount < Money.ZERO) {
            R.string.validation_receipt_item_price_non_negative
        } else {
            null
        }

        if (discountError == null && parsedQty != null && parsedQty > 0 &&
            parsedUnitPrice != null && parsedDiscount != null
        ) {
            val grossMinor = parsedUnitPrice.minorUnits * parsedQty.toLong()
            if (parsedDiscount.minorUnits > grossMinor) {
                discountError = R.string.validation_receipt_item_discount_exceeds_gross
            }
        }

        if (descriptionError != null || quantityError != null ||
            unitPriceError != null || discountError != null
        ) {
            _uiState.update {
                it.copy(
                    itemEditor = editor.copy(
                        descriptionErrorResId = descriptionError,
                        quantityErrorResId = quantityError,
                        unitPriceErrorResId = unitPriceError,
                        discountErrorResId = discountError
                    )
                )
            }
            return
        }

        val itemCategory = editor.category.trim().ifEmpty { DEFAULT_ITEM_CATEGORY }
        val resolvedId = editor.itemId ?: "local-item-${localItemSequence.getAndIncrement()}"
        val updatedItem = ReceiptCalculator.calculateItem(
            id = resolvedId,
            description = trimmedDescription,
            quantity = parsedQty!!,
            unitPrice = parsedUnitPrice!!,
            discountAmount = parsedDiscount!!,
            category = itemCategory
        )

        val updatedItems = if (editor.itemId == null) {
            currentReceipt.items + updatedItem
        } else {
            currentReceipt.items.map { existing ->
                if (existing.id == editor.itemId) updatedItem else existing
            }
        }

        applyUpdatedReceipt(currentReceipt.copy(items = updatedItems))
    }

    private fun confirmRemoveReceiptItem() {
        val current = _uiState.value
        val itemToRemove = current.pendingRemoveItem ?: return
        val currentReceipt = current.receipt ?: return
        val updatedItems = currentReceipt.items.filterNot { it.id == itemToRemove.id }
        applyUpdatedReceipt(
            updatedReceiptBase = currentReceipt.copy(items = updatedItems),
            clearPendingDelete = true
        )
    }

    private fun applyUpdatedReceipt(
        updatedReceiptBase: Receipt,
        clearPendingDelete: Boolean = false
    ) {
        val recalculated = ReceiptCalculator.recalculateTotals(updatedReceiptBase)
        _uiState.update { current ->
            current.copy(
                receipt = recalculated,
                amountInput = recalculated.total.toDecimalInputString(),
                fieldErrors = current.fieldErrors - ValidationField.AMOUNT,
                itemEditor = null,
                pendingRemoveItem = if (clearPendingDelete) null else current.pendingRemoveItem
            )
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
                val hasReceiptOrigin = existingTx != null &&
                    (existingTx.source == TransactionSource.RECEIPT || existingTx.receiptId != null)
                val receiptResult = if (hasReceiptOrigin) {
                    getTransactionReceiptUseCase(existingTx!!.id)
                } else {
                    null
                }
                val loadedReceipt = (receiptResult as? AppResult.Success)?.data
                val receiptUnavailable = hasReceiptOrigin && loadedReceipt == null

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
                            loadError = null,
                            transactionSource = existingTx.source,
                            receiptId = existingTx.receiptId,
                            isReceiptTransaction = hasReceiptOrigin,
                            receipt = loadedReceipt,
                            receiptUnavailable = receiptUnavailable
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

        val currentReceipt = current.receipt
        val parsedAmount = if (currentReceipt != null && currentReceipt.items.isNotEmpty()) {
            currentReceipt.total
        } else {
            MoneyParser.parse(current.amountInput)
        }

        val candidate = Transaction(
            id = transactionId.orEmpty(),
            amount = parsedAmount ?: Money.ZERO,
            type = current.type,
            description = current.description,
            categoryId = current.selectedCategoryId.orEmpty(),
            accountId = current.selectedAccountId.orEmpty(),
            date = current.date,
            source = current.transactionSource,
            receiptId = current.receiptId
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
            _uiState.update {
                it.copy(isSubmitting = true, submitError = null, fieldErrors = emptyMap())
            }
            val result = when {
                transactionId == null -> createTransactionUseCase(candidate)
                currentReceipt != null -> updateTransactionWithReceiptUseCase(candidate, currentReceipt)
                else -> updateTransactionUseCase(candidate)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(TransactionFormEffect.NavigateBackSaved)
                }

                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val mappedErrors = err.toFieldMessageResIds().toMutableMap()
                        mappedErrors[ValidationField.TOTAL]?.let { totalMsg ->
                            mappedErrors.putIfAbsent(ValidationField.AMOUNT, totalMsg)
                        }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                fieldErrors = mappedErrors
                            )
                        }
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
        private const val DEFAULT_ITEM_CATEGORY = "Other"
    }
}
