package minicla03.cashmate.domain.model

import java.time.LocalDate

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionSource {
    MANUAL,
    RECEIPT
}

data class Transaction(
    val id: String,
    val amount: Money,
    val type: TransactionType,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val date: LocalDate,
    val source: TransactionSource = TransactionSource.MANUAL,
    val receiptId: String? = null
)

