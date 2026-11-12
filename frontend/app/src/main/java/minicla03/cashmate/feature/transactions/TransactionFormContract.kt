package minicla03.cashmate.feature.transactions

import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.TransactionType
import java.time.LocalDate
import java.util.Currency

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
    val submitError: UiError? = null
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
}

sealed interface TransactionFormEffect {
    data object NavigateBackSaved : TransactionFormEffect
}

