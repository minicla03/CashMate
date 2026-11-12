package minicla03.cashmate.feature.transactions

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import java.util.Currency

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val transaction: Transaction? = null,
    val category: Category? = null,
    val account: Account? = null,
    val categories: List<Category> = emptyList(),
    val receipt: Receipt? = null,
    val isReceiptLoading: Boolean = false,
    val receiptUnavailable: Boolean = false,
    val currency: Currency = Currency.getInstance("EUR"),
    val error: UiError? = null,
    val actionError: UiError? = null
)

sealed interface TransactionDetailEvent {
    data object Refresh : TransactionDetailEvent
    data object EditClick : TransactionDetailEvent
    data object DeleteClick : TransactionDetailEvent
    data object DismissDeleteDialog : TransactionDetailEvent
    data object ConfirmDelete : TransactionDetailEvent
    data object DismissActionError : TransactionDetailEvent
}

sealed interface TransactionDetailEffect {
    data class NavigateToEdit(val transactionId: String) : TransactionDetailEffect
    data object NavigateBackAfterDelete : TransactionDetailEffect
}

