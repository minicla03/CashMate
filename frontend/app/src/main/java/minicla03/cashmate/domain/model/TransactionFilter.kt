package minicla03.cashmate.domain.model

import java.time.LocalDate

enum class TransactionSortOrder {
    DATE_DESC,
    DATE_ASC
}

data class TransactionFilter(
    val query: String? = null,
    val categoryId: String? = null,
    val type: TransactionType? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC
)

