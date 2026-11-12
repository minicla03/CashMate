package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val amount: Long,
    val type: String,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val date: String,
    val source: String = "MANUAL",
    val receiptId: String? = null,
    val receipt: ReceiptDraftDto? = null
)

@Serializable
data class CreateTransactionRequest(
    val amount: Long,
    val type: String,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val date: String
)

@Serializable
data class UpdateTransactionRequest(
    val amount: Long,
    val type: String,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val date: String,
    val receipt: UpdateReceiptRequest? = null
)

data class TransactionFilterDto(
    val from: String? = null,
    val to: String? = null,
    val categoryId: String? = null,
    val type: String? = null
)

