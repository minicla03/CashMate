package minicla03.cashmate.feature.receipt.presentation

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
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.ConfirmReceiptUseCase
import minicla03.cashmate.domain.usecase.DiscardReceiptDraftUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetReceiptDraftUseCase
import minicla03.cashmate.domain.usecase.ProcessReceiptUseCase
import minicla03.cashmate.domain.usecase.ReceiptValidator
import java.util.Currency
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val imageReader: ReceiptImageReader,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val confirmReceiptUseCase: ConfirmReceiptUseCase,
    private val getReceiptDraftUseCase: GetReceiptDraftUseCase,
    private val discardReceiptDraftUseCase: DiscardReceiptDraftUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val receiptValidator: ReceiptValidator,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Idle)
    val uiState: StateFlow<ReceiptScanState> = _uiState.asStateFlow()

    private val _effects = Channel<ReceiptScanEffect>(Channel.BUFFERED)
    val effects: Flow<ReceiptScanEffect> = _effects.receiveAsFlow()

    private var currentCurrency: Currency = userPreferencesRepository.preferences.value.currency
    private var lastReceiptImage: ReceiptImage? = null
    private val localIdSequence = AtomicLong(1L)

    init {
        userPreferencesRepository.preferences
            .onEach { prefs ->
                currentCurrency = prefs.currency
                _uiState.update { state ->
                    when (state) {
                        is ReceiptScanState.Review -> state.copy(currency = prefs.currency)
                        is ReceiptScanState.Confirming -> state.copy(
                            review = state.review.copy(currency = prefs.currency)
                        )
                        is ReceiptScanState.Success -> state.copy(currency = prefs.currency)
                        else -> state
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReceiptScanEvent) {
        when (event) {
            ReceiptScanEvent.BeginImageSelection -> {
                if (!isBusy()) {
                    _uiState.value = ReceiptScanState.SelectingImage
                }
            }

            ReceiptScanEvent.ImageSelectionCancelled -> {
                if (_uiState.value is ReceiptScanState.SelectingImage) {
                    _uiState.value = ReceiptScanState.Idle
                }
            }

            is ReceiptScanEvent.ImageSelected -> handleImageSelected(event.uriString)

            ReceiptScanEvent.RetryProcessing -> retryLastImage()

            ReceiptScanEvent.ChooseAnotherImage -> {
                lastReceiptImage = null
                _uiState.value = ReceiptScanState.Idle
            }

            is ReceiptScanEvent.EnsureDraftLoaded -> ensureDraftLoaded(event.receiptId)

            is ReceiptScanEvent.MerchantChanged -> updateReview { review ->
                review.copy(
                    merchant = event.value,
                    fieldErrors = review.fieldErrors - ValidationField.MERCHANT
                )
            }

            is ReceiptScanEvent.DateChanged -> updateReview { review ->
                review.copy(
                    date = event.date,
                    fieldErrors = review.fieldErrors - ValidationField.DATE
                )
            }

            is ReceiptScanEvent.TotalChanged -> updateReview { review ->
                review.copy(
                    totalInput = event.value,
                    fieldErrors = review.fieldErrors - ValidationField.TOTAL
                )
            }

            is ReceiptScanEvent.SubtotalChanged -> updateReview { review ->
                review.copy(
                    subtotalInput = event.value,
                    fieldErrors = review.fieldErrors - ValidationField.TOTAL
                )
            }

            is ReceiptScanEvent.TotalDiscountChanged -> updateReview { review ->
                review.copy(
                    totalDiscountInput = event.value,
                    fieldErrors = review.fieldErrors - ValidationField.TOTAL
                )
            }

            is ReceiptScanEvent.CategorySelected -> updateReview { review ->
                review.copy(
                    selectedCategoryId = event.categoryId,
                    fieldErrors = review.fieldErrors - ValidationField.CATEGORY
                )
            }

            is ReceiptScanEvent.AccountSelected -> updateReview { review ->
                review.copy(
                    selectedAccountId = event.accountId,
                    fieldErrors = review.fieldErrors - ValidationField.ACCOUNT
                )
            }

            is ReceiptScanEvent.ItemDescriptionChanged -> updateItem(event.itemId) {
                it.copy(description = event.value)
            }

            is ReceiptScanEvent.ItemQuantityChanged -> updateItem(event.itemId) {
                it.copy(quantityInput = event.value)
            }

            is ReceiptScanEvent.ItemUnitPriceChanged -> updateItem(event.itemId) {
                it.copy(unitPriceInput = event.value)
            }

            is ReceiptScanEvent.ItemGrossAmountChanged -> updateItem(event.itemId) {
                it.copy(grossAmountInput = event.value)
            }

            is ReceiptScanEvent.ItemDiscountAmountChanged -> updateItem(event.itemId) {
                it.copy(discountAmountInput = event.value)
            }

            is ReceiptScanEvent.ItemNetAmountChanged -> updateItem(event.itemId) {
                it.copy(netAmountInput = event.value)
            }

            is ReceiptScanEvent.ItemCategoryChanged -> updateItem(event.itemId) {
                it.copy(category = event.category)
            }

            ReceiptScanEvent.AddItem -> updateReview { review ->
                val defaultItemCategory = review.categories
                    .firstOrNull { it.id == review.selectedCategoryId }?.name
                    ?: review.categories.firstOrNull()?.name
                    ?: DEFAULT_ITEM_CATEGORY
                val newItem = ReceiptItemDraft(
                    id = "local-item-${localIdSequence.getAndIncrement()}",
                    description = "",
                    quantityInput = "1",
                    unitPriceInput = "0.00",
                    grossAmountInput = "0.00",
                    discountAmountInput = "0.00",
                    netAmountInput = "0.00",
                    category = defaultItemCategory
                )
                review.copy(items = review.items + newItem)
            }

            is ReceiptScanEvent.RemoveItem -> updateReview { review ->
                review.copy(
                    items = review.items.filterNot { it.id == event.itemId },
                    invalidItemIds = review.invalidItemIds - event.itemId
                )
            }

            is ReceiptScanEvent.AdjustmentDescriptionChanged -> updateAdjustment(event.adjustmentId) {
                it.copy(description = event.value)
            }

            is ReceiptScanEvent.AdjustmentAmountChanged -> updateAdjustment(event.adjustmentId) {
                it.copy(amountInput = event.value)
            }

            is ReceiptScanEvent.AdjustmentTypeChanged -> updateAdjustment(event.adjustmentId) {
                it.copy(type = event.type)
            }

            ReceiptScanEvent.AddAdjustment -> updateReview { review ->
                val newAdjustment = ReceiptAdjustmentDraft(
                    id = "local-adj-${localIdSequence.getAndIncrement()}",
                    description = "",
                    amountInput = "0.00",
                    type = ReceiptAdjustmentType.DISCOUNT
                )
                review.copy(adjustments = review.adjustments + newAdjustment)
            }

            is ReceiptScanEvent.RemoveAdjustment -> updateReview { review ->
                review.copy(
                    adjustments = review.adjustments.filterNot { it.id == event.adjustmentId },
                    invalidAdjustmentIds = review.invalidAdjustmentIds - event.adjustmentId
                )
            }

            ReceiptScanEvent.ConfirmClick -> confirmReceipt()

            ReceiptScanEvent.DismissSubmitError -> updateReview { review ->
                review.copy(submitError = null)
            }

            ReceiptScanEvent.CancelClick -> handleCancelClick()

            ReceiptScanEvent.DismissDiscardDialog -> updateReview { review ->
                review.copy(showDiscardDialog = false)
            }

            ReceiptScanEvent.ConfirmDiscard -> discardAndExit()

            is ReceiptScanEvent.ViewTransactionClick -> sendEffect(
                ReceiptScanEffect.NavigateToTransactionDetail(event.transactionId)
            )

            ReceiptScanEvent.DoneClick -> sendEffect(ReceiptScanEffect.ExitFlow)
        }
    }

    private fun isBusy(): Boolean = when (_uiState.value) {
        ReceiptScanState.Uploading,
        ReceiptScanState.Processing,
        is ReceiptScanState.Confirming -> true
        else -> false
    }

    private fun handleImageSelected(uriString: String) {
        if (isBusy()) return

        viewModelScope.launch {
            _uiState.value = ReceiptScanState.Uploading
            when (val imageResult = imageReader.read(uriString)) {
                is AppResult.Error -> {
                    lastReceiptImage = null
                    _uiState.value = ReceiptScanState.Error(
                        error = imageResult.error.toUiError(),
                        canRetryLastImage = false
                    )
                }

                is AppResult.Success -> {
                    val image = imageResult.data
                    lastReceiptImage = image
                    processImage(image)
                }
            }
        }
    }

    private fun retryLastImage() {
        val image = lastReceiptImage
        if (image == null) {
            _uiState.value = ReceiptScanState.Idle
            return
        }
        if (isBusy()) return
        viewModelScope.launch {
            processImage(image)
        }
    }

    private suspend fun processImage(image: ReceiptImage) {
        _uiState.value = ReceiptScanState.Processing
        val (receiptResult, categoriesResult, accountsResult) = coroutineScope {
            val receiptDeferred = async { processReceiptUseCase(image) }
            val categoriesDeferred = async { getCategoriesUseCase() }
            val accountsDeferred = async { getAccountsUseCase() }
            Triple(
                receiptDeferred.await(),
                categoriesDeferred.await(),
                accountsDeferred.await()
            )
        }

        val firstError = listOfNotNull(
            receiptResult as? AppResult.Error,
            categoriesResult as? AppResult.Error,
            accountsResult as? AppResult.Error
        ).firstOrNull()

        if (firstError != null) {
            val uiError = firstError.error.toUiError()
            _uiState.value = ReceiptScanState.Error(
                error = uiError,
                canRetryLastImage = uiError.canRetry
            )
            return
        }

        val receipt = (receiptResult as AppResult.Success).data
        val categories = (categoriesResult as AppResult.Success).data
        val accounts = (accountsResult as AppResult.Success).data

        val reviewState = buildReviewState(receipt, categories, accounts)
        _uiState.value = reviewState
        _effects.send(ReceiptScanEffect.NavigateToReview(receipt.id))
    }

    private fun ensureDraftLoaded(receiptId: String) {
        val current = _uiState.value
        if (current is ReceiptScanState.Review && current.receiptId == receiptId) return
        if (current is ReceiptScanState.Confirming && current.review.receiptId == receiptId) return
        if (current is ReceiptScanState.Success) return

        viewModelScope.launch {
            val draftResult = getReceiptDraftUseCase(receiptId)
            val categoriesResult = getCategoriesUseCase()
            val accountsResult = getAccountsUseCase()

            if (draftResult is AppResult.Success &&
                categoriesResult is AppResult.Success &&
                accountsResult is AppResult.Success
            ) {
                _uiState.value = buildReviewState(
                    receipt = draftResult.data,
                    categories = categoriesResult.data,
                    accounts = accountsResult.data
                )
            } else {
                _uiState.value = ReceiptScanState.Idle
                _effects.send(ReceiptScanEffect.ExitFlow)
            }
        }
    }

    private fun buildReviewState(
        receipt: Receipt,
        categories: List<Category>,
        accounts: List<Account>
    ): ReceiptScanState.Review {
        val defaultCategoryId = resolveDefaultCategoryId(receipt, categories)
        val defaultAccountId = receipt.accountId
            ?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id

        val normalizedReceipt = receipt.copy(
            categoryId = defaultCategoryId,
            accountId = defaultAccountId
        )
        return ReceiptScanState.Review(
            receiptId = normalizedReceipt.id,
            originalReceipt = normalizedReceipt,
            merchant = normalizedReceipt.merchant,
            date = normalizedReceipt.date,
            totalInput = normalizedReceipt.total.toDecimalInputString(),
            subtotalInput = normalizedReceipt.subtotal.toDecimalInputString(),
            totalDiscountInput = normalizedReceipt.totalDiscount.toDecimalInputString(),
            selectedCategoryId = defaultCategoryId,
            selectedAccountId = defaultAccountId,
            items = normalizedReceipt.items.map { it.toDraft() },
            adjustments = normalizedReceipt.adjustments.mapIndexed { index, adj ->
                adj.toDraft(fallbackId = "adj-$index")
            },
            categories = categories,
            accounts = accounts,
            currency = currentCurrency,
            hasUnsavedChanges = false
        )
    }

    private fun resolveDefaultCategoryId(receipt: Receipt, categories: List<Category>): String? {
        ReceiptCategoryResolver.resolve(receipt.categoryId, categories)?.let { return it.id }

        val dominantItemCategory = receipt.items
            .groupingBy { it.category }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        ReceiptCategoryResolver.resolve(dominantItemCategory, categories)?.let { return it.id }

        return categories.firstOrNull { it.id != SALARY_CATEGORY_ID }?.id
            ?: categories.firstOrNull()?.id
    }

    private fun updateReview(transform: (ReceiptScanState.Review) -> ReceiptScanState.Review) {
        val current = _uiState.value as? ReceiptScanState.Review ?: return
        val updated = transform(current)
        _uiState.value = updated.copy(hasUnsavedChanges = computeHasUnsavedChanges(updated))
    }

    private fun updateItem(
        itemId: String,
        transform: (ReceiptItemDraft) -> ReceiptItemDraft
    ) {
        updateReview { review ->
            review.copy(
                items = review.items.map { item ->
                    if (item.id == itemId) transform(item) else item
                },
                invalidItemIds = review.invalidItemIds - itemId,
                fieldErrors = review.fieldErrors -
                    ValidationField.ITEM_DESCRIPTION -
                    ValidationField.ITEM_QUANTITY -
                    ValidationField.ITEM_PRICE
            )
        }
    }

    private fun updateAdjustment(
        adjustmentId: String,
        transform: (ReceiptAdjustmentDraft) -> ReceiptAdjustmentDraft
    ) {
        updateReview { review ->
            review.copy(
                adjustments = review.adjustments.map { adj ->
                    if (adj.id == adjustmentId) transform(adj) else adj
                },
                invalidAdjustmentIds = review.invalidAdjustmentIds - adjustmentId,
                fieldErrors = review.fieldErrors -
                    ValidationField.ADJUSTMENT_DESCRIPTION -
                    ValidationField.ADJUSTMENT_AMOUNT
            )
        }
    }

    private fun computeHasUnsavedChanges(review: ReceiptScanState.Review): Boolean {
        val orig = review.originalReceipt
        if (review.merchant != orig.merchant) return true
        if (review.date != orig.date) return true
        if (review.totalInput != orig.total.toDecimalInputString()) return true
        if (review.subtotalInput != orig.subtotal.toDecimalInputString()) return true
        if (review.totalDiscountInput != orig.totalDiscount.toDecimalInputString()) return true
        if (review.selectedCategoryId != orig.categoryId) return true
        if (review.selectedAccountId != orig.accountId) return true
        if (review.items != orig.items.map { it.toDraft() }) return true
        if (review.adjustments != orig.adjustments.mapIndexed { idx, adj -> adj.toDraft("adj-$idx") }) {
            return true
        }
        return false
    }

    private fun confirmReceipt() {
        val current = _uiState.value as? ReceiptScanState.Review ?: return
        val parsed = buildCandidateAndValidate(current)
        if (parsed.validationErrors.isNotEmpty() || parsed.candidate == null) {
            _uiState.value = current.copy(
                fieldErrors = parsed.validationErrors,
                invalidItemIds = parsed.invalidItemIds,
                invalidAdjustmentIds = parsed.invalidAdjustmentIds
            )
            return
        }

        val candidate = parsed.candidate
        viewModelScope.launch {
            _uiState.value = ReceiptScanState.Confirming(
                current.copy(
                    fieldErrors = emptyMap(),
                    invalidItemIds = emptySet(),
                    invalidAdjustmentIds = emptySet(),
                    submitError = null
                )
            )

            when (val result = confirmReceiptUseCase(candidate)) {
                is AppResult.Success -> {
                    lastReceiptImage = null
                    imageReader.cleanupTemporaryFiles()
                    _uiState.value = ReceiptScanState.Success(
                        merchant = candidate.merchant,
                        transaction = result.data,
                        currency = current.currency
                    )
                }

                is AppResult.Error -> {
                    when (val err = result.error) {
                        is AppError.Validation -> {
                            _uiState.value = current.copy(
                                fieldErrors = err.toFieldMessageResIds(),
                                submitError = null
                            )
                        }
                        else -> {
                            _uiState.value = current.copy(
                                submitError = err.toUiError()
                            )
                        }
                    }
                }
            }
        }
    }

    private data class CandidateValidationResult(
        val candidate: Receipt?,
        val validationErrors: Map<ValidationField, Int>,
        val invalidItemIds: Set<String>,
        val invalidAdjustmentIds: Set<String>
    )

    private fun buildCandidateAndValidate(review: ReceiptScanState.Review): CandidateValidationResult {
        val fieldErrors = mutableMapOf<ValidationField, Int>()
        val invalidItemIds = mutableSetOf<String>()
        val invalidAdjustmentIds = mutableSetOf<String>()

        val parsedTotal = MoneyParser.parse(review.totalInput)
        val parsedSubtotal = MoneyParser.parse(review.subtotalInput)
        val parsedTotalDiscount = MoneyParser.parse(review.totalDiscountInput)

        if (parsedTotal == null || parsedTotal <= Money.ZERO) {
            fieldErrors[ValidationField.TOTAL] = R.string.validation_receipt_total_positive
        }
        if (parsedSubtotal == null || parsedTotalDiscount == null) {
            fieldErrors[ValidationField.TOTAL] = R.string.validation_receipt_total_positive
        }

        val parsedItems = review.items.map { draft ->
            val qty = draft.quantityInput.trim().toIntOrNull()
            val unitPrice = MoneyParser.parse(draft.unitPriceInput)
            val gross = MoneyParser.parse(draft.grossAmountInput)
            val discount = MoneyParser.parse(draft.discountAmountInput)
            val net = MoneyParser.parse(draft.netAmountInput)

            if (draft.description.trim().isEmpty()) {
                fieldErrors[ValidationField.ITEM_DESCRIPTION] =
                    R.string.validation_receipt_item_description_required
                invalidItemIds.add(draft.id)
            }
            if (qty == null || qty <= 0) {
                fieldErrors[ValidationField.ITEM_QUANTITY] =
                    R.string.validation_receipt_item_quantity_positive
                invalidItemIds.add(draft.id)
            }
            if (unitPrice == null || gross == null || discount == null || net == null) {
                fieldErrors[ValidationField.ITEM_PRICE] =
                    R.string.validation_receipt_item_price_non_negative
                invalidItemIds.add(draft.id)
            }

            ReceiptItem(
                id = draft.id,
                description = draft.description,
                quantity = qty ?: 0,
                unitPrice = unitPrice ?: Money.ZERO,
                grossAmount = gross ?: Money.ZERO,
                discountAmount = discount ?: Money.ZERO,
                netAmount = net ?: Money.ZERO,
                category = draft.category
            )
        }

        val parsedAdjustments = review.adjustments.map { draft ->
            val amount = MoneyParser.parse(draft.amountInput)
            if (draft.description.trim().isEmpty()) {
                fieldErrors[ValidationField.ADJUSTMENT_DESCRIPTION] =
                    R.string.validation_receipt_adjustment_description_required
                invalidAdjustmentIds.add(draft.id)
            }
            if (amount == null) {
                fieldErrors[ValidationField.ADJUSTMENT_AMOUNT] =
                    R.string.validation_receipt_adjustment_amount_non_negative
                invalidAdjustmentIds.add(draft.id)
            }
            ReceiptAdjustment(
                id = draft.id,
                description = draft.description,
                amount = amount ?: Money.ZERO,
                type = draft.type
            )
        }

        val candidate = Receipt(
            id = review.receiptId,
            merchant = review.merchant,
            date = review.date,
            subtotal = parsedSubtotal ?: Money.ZERO,
            totalDiscount = parsedTotalDiscount ?: Money.ZERO,
            total = parsedTotal ?: Money.ZERO,
            categoryId = review.selectedCategoryId,
            accountId = review.selectedAccountId,
            items = parsedItems,
            adjustments = parsedAdjustments
        )

        receiptValidator.validate(candidate)?.toFieldMessageResIds()?.forEach { (field, resId) ->
            fieldErrors.putIfAbsent(field, resId)
        }

        return CandidateValidationResult(
            candidate = if (fieldErrors.isEmpty()) candidate else null,
            validationErrors = fieldErrors,
            invalidItemIds = invalidItemIds,
            invalidAdjustmentIds = invalidAdjustmentIds
        )
    }

    private fun handleCancelClick() {
        when (val current = _uiState.value) {
            is ReceiptScanState.Confirming -> Unit
            is ReceiptScanState.Review -> {
                if (current.hasUnsavedChanges) {
                    _uiState.value = current.copy(showDiscardDialog = true)
                } else {
                    discardAndExit()
                }
            }
            else -> {
                lastReceiptImage = null
                imageReader.cleanupTemporaryFiles()
                _uiState.value = ReceiptScanState.Idle
                sendEffect(ReceiptScanEffect.ExitFlow)
            }
        }
    }

    private fun discardAndExit() {
        val receiptId = when (val current = _uiState.value) {
            is ReceiptScanState.Review -> current.receiptId
            is ReceiptScanState.Confirming -> current.review.receiptId
            else -> null
        }
        viewModelScope.launch {
            if (receiptId != null) {
                discardReceiptDraftUseCase(receiptId)
            }
            lastReceiptImage = null
            imageReader.cleanupTemporaryFiles()
            _uiState.value = ReceiptScanState.Idle
            _effects.send(ReceiptScanEffect.ExitFlow)
        }
    }

    private fun sendEffect(effect: ReceiptScanEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    override fun onCleared() {
        lastReceiptImage = null
        imageReader.cleanupTemporaryFiles()
        super.onCleared()
    }

    companion object {
        private const val SALARY_CATEGORY_ID = "cat-salary"
        private const val DEFAULT_ITEM_CATEGORY = "OTHER"
    }
}

