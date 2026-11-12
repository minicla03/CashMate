package minicla03.cashmate.domain.model

import java.time.LocalDate

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class Transaction(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val date: LocalDate
)

