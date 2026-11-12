package minicla03.cashmate.feature.transactions

import androidx.annotation.StringRes
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.model.TransactionSource
import minicla03.cashmate.domain.model.TransactionType
import java.time.LocalDate
import java.util.Currency

data class ReceiptItemEditorState(
    val itemId: String? = null,
    val description: String = "",
    val quantityInput: String = "1",
    val unitPriceInput: String = "",
    val discountInput: String = "0.00",
    val category: String = "",
    @StringRes val descriptionErrorResId: Int? = null,
    @StringRes val quantityErrorResId: Int? = null,
    @StringRes val unitPriceErrorResId: Int? = null,
    @StringRes val discountErrorResId: Int? = null
) {
    val isEditMode: Boolean get() = itemId != null
}

data class TransactionFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val description: String = "",
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currency: Currency = Currency.getInstance("EUR"),
    val fieldErrors: Map<ValidationField, Int> = emptyMap(),
    val loadError: UiError? = null,
    val submitError: UiError? = null,
    val transactionSource: TransactionSource = TransactionSource.MANUAL,
    val receiptId: String? = null,
    val isReceiptTransaction: Boolean = false,
    val receipt: Receipt? = null,
    val receiptUnavailable: Boolean = false,
    val itemEditor: ReceiptItemEditorState? = null,
    val pendingRemoveItem: ReceiptItem? = null
)

sealed interface TransactionFormEvent {
    data object RetryLoad : TransactionFormEvent
    data class TypeChanged(val type: TransactionType) : TransactionFormEvent
    data class AmountChanged(val value: String) : TransactionFormEvent
    data class DescriptionChanged(val value: String) : TransactionFormEvent
    data class CategorySelected(val categoryId: String) : TransactionFormEvent
    data class AccountSelected(val accountId: String) : TransactionFormEvent
    data class DateSelected(val date: LocalDate) : TransactionFormEvent
    data object Submit : TransactionFormEvent
    data object DismissSubmitError : TransactionFormEvent

    data object OpenAddReceiptItem : TransactionFormEvent
    data class OpenEditReceiptItem(val item: ReceiptItem) : TransactionFormEvent
    data class ItemEditorDescriptionChanged(val value: String) : TransactionFormEvent
    data class ItemEditorQuantityChanged(val value: String) : TransactionFormEvent
    data class ItemEditorUnitPriceChanged(val value: String) : TransactionFormEvent
    data class ItemEditorDiscountChanged(val value: String) : TransactionFormEvent
    data class ItemEditorCategorySelected(val categoryName: String) : TransactionFormEvent
    data object SubmitItemEditor : TransactionFormEvent
    data object DismissItemEditor : TransactionFormEvent
    data class RequestRemoveReceiptItem(val item: ReceiptItem) : TransactionFormEvent
    data object ConfirmRemoveReceiptItem : TransactionFormEvent
    data object DismissRemoveReceiptItem : TransactionFormEvent
}

sealed interface TransactionFormEffect {
    data object NavigateBackSaved : TransactionFormEffect
}
