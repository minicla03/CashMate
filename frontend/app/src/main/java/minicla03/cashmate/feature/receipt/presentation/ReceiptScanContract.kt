package minicla03.cashmate.feature.receipt.presentation

import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.model.Transaction
import java.time.LocalDate
import java.util.Currency

data class ReceiptItemDraft(
    val id: String,
    val description: String,
    val quantityInput: String,
    val unitPriceInput: String,
    val grossAmountInput: String,
    val discountAmountInput: String,
    val netAmountInput: String,
    val category: String
)

data class ReceiptAdjustmentDraft(
    val id: String,
    val description: String,
    val amountInput: String,
    val type: ReceiptAdjustmentType
)

sealed interface ReceiptScanState {

    data object Idle : ReceiptScanState

    data object SelectingImage : ReceiptScanState

    data object Uploading : ReceiptScanState

    data object Processing : ReceiptScanState

    data class Review(
        val receiptId: String,
        val originalReceipt: Receipt,
        val merchant: String,
        val date: LocalDate,
        val totalInput: String,
        val subtotalInput: String,
        val totalDiscountInput: String,
        val selectedCategoryId: String?,
        val selectedAccountId: String?,
        val items: List<ReceiptItemDraft>,
        val adjustments: List<ReceiptAdjustmentDraft>,
        val categories: List<Category>,
        val accounts: List<Account>,
        val currency: Currency,
        val hasUnsavedChanges: Boolean = false,
        val fieldErrors: Map<ValidationField, Int> = emptyMap(),
        val invalidItemIds: Set<String> = emptySet(),
        val invalidAdjustmentIds: Set<String> = emptySet(),
        val submitError: UiError? = null,
        val showDiscardDialog: Boolean = false
    ) : ReceiptScanState

    data class Confirming(
        val review: Review
    ) : ReceiptScanState

    data class Success(
        val merchant: String,
        val transaction: Transaction,
        val currency: Currency
    ) : ReceiptScanState

    data class Error(
        val error: UiError,
        val canRetryLastImage: Boolean
    ) : ReceiptScanState
}

fun ReceiptItem.toDraft(): ReceiptItemDraft = ReceiptItemDraft(
    id = id,
    description = description,
    quantityInput = quantity.toString(),
    unitPriceInput = unitPrice.toDecimalInputString(),
    grossAmountInput = grossAmount.toDecimalInputString(),
    discountAmountInput = discountAmount.toDecimalInputString(),
    netAmountInput = netAmount.toDecimalInputString(),
    category = category
)

fun ReceiptAdjustment.toDraft(fallbackId: String): ReceiptAdjustmentDraft = ReceiptAdjustmentDraft(
    id = id.ifBlank { fallbackId },
    description = description,
    amountInput = amount.toDecimalInputString(),
    type = type
)

sealed interface ReceiptScanEvent {
    data object BeginImageSelection : ReceiptScanEvent
    data object ImageSelectionCancelled : ReceiptScanEvent
    data class ImageSelected(val uriString: String) : ReceiptScanEvent
    data object RetryProcessing : ReceiptScanEvent
    data object ChooseAnotherImage : ReceiptScanEvent
    data class EnsureDraftLoaded(val receiptId: String) : ReceiptScanEvent

    data class MerchantChanged(val value: String) : ReceiptScanEvent
    data class DateChanged(val date: LocalDate) : ReceiptScanEvent
    data class TotalChanged(val value: String) : ReceiptScanEvent
    data class SubtotalChanged(val value: String) : ReceiptScanEvent
    data class TotalDiscountChanged(val value: String) : ReceiptScanEvent
    data class CategorySelected(val categoryId: String) : ReceiptScanEvent
    data class AccountSelected(val accountId: String) : ReceiptScanEvent

    data class ItemDescriptionChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemQuantityChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemUnitPriceChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemGrossAmountChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemDiscountAmountChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemNetAmountChanged(val itemId: String, val value: String) : ReceiptScanEvent
    data class ItemCategoryChanged(val itemId: String, val category: String) : ReceiptScanEvent
    data object AddItem : ReceiptScanEvent
    data class RemoveItem(val itemId: String) : ReceiptScanEvent

    data class AdjustmentDescriptionChanged(val adjustmentId: String, val value: String) : ReceiptScanEvent
    data class AdjustmentAmountChanged(val adjustmentId: String, val value: String) : ReceiptScanEvent
    data class AdjustmentTypeChanged(val adjustmentId: String, val type: ReceiptAdjustmentType) : ReceiptScanEvent
    data object AddAdjustment : ReceiptScanEvent
    data class RemoveAdjustment(val adjustmentId: String) : ReceiptScanEvent

    data object ConfirmClick : ReceiptScanEvent
    data object DismissSubmitError : ReceiptScanEvent
    data object CancelClick : ReceiptScanEvent
    data object DismissDiscardDialog : ReceiptScanEvent
    data object ConfirmDiscard : ReceiptScanEvent

    data class ViewTransactionClick(val transactionId: String) : ReceiptScanEvent
    data object DoneClick : ReceiptScanEvent
}

sealed interface ReceiptScanEffect {
    data class NavigateToReview(val receiptId: String) : ReceiptScanEffect
    data class NavigateToTransactionDetail(val transactionId: String) : ReceiptScanEffect
    data object ExitFlow : ReceiptScanEffect
}

