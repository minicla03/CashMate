package minicla03.cashmate.feature.transactions

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.model.TransactionType
import java.time.LocalDate
import java.util.Currency

enum class DateRangePreset {
    ALL,
    THIS_MONTH,
    LAST_MONTH,
    LAST_30_DAYS,
    CUSTOM
}

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoriesById: Map<String, Category> = emptyMap(),
    val currency: Currency = Currency.getInstance("EUR"),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedCategoryId: String? = null,
    val datePreset: DateRangePreset = DateRangePreset.ALL,
    val customFromDate: LocalDate? = null,
    val customToDate: LocalDate? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val error: UiError? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedType != null ||
            selectedCategoryId != null ||
            datePreset != DateRangePreset.ALL

    val isEmpty: Boolean
        get() = !isLoading && error == null && transactions.isEmpty()
}

sealed interface TransactionsEvent {
    data object Refresh : TransactionsEvent
    data class SearchQueryChanged(val query: String) : TransactionsEvent
    data class TypeFilterChanged(val type: TransactionType?) : TransactionsEvent
    data class CategoryFilterChanged(val categoryId: String?) : TransactionsEvent
    data class DatePresetSelected(val preset: DateRangePreset) : TransactionsEvent
    data class CustomDateRangeSelected(val from: LocalDate?, val to: LocalDate?) : TransactionsEvent
    data object ToggleSortOrder : TransactionsEvent
    data object ClearFilters : TransactionsEvent
    data object AddTransactionClick : TransactionsEvent
    data class TransactionClick(val id: String) : TransactionsEvent
}

sealed interface TransactionsEffect {
    data object NavigateToAddTransaction : TransactionsEffect
    data class NavigateToTransactionDetail(val id: String) : TransactionsEffect
}

