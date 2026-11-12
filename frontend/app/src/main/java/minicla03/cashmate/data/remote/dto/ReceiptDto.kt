package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptDraftDto(
    val id: String,
    val merchant: String,
    val date: String,
    val subtotal: Long,
    val totalDiscount: Long = 0L,
    val total: Long,
    val categoryId: String? = null,
    val accountId: String? = null,
    val items: List<ReceiptItemDto> = emptyList(),
    val adjustments: List<ReceiptAdjustmentDto> = emptyList()
)

@Serializable
data class ReceiptItemDto(
    val id: String,
    val description: String,
    val quantity: Int,
    val unitPrice: Long,
    val grossAmount: Long,
    val discountAmount: Long = 0L,
    val netAmount: Long,
    val category: String
)

@Serializable
data class ReceiptAdjustmentDto(
    val id: String = "",
    val description: String,
    val amount: Long,
    val type: String = "DISCOUNT"
)

@Serializable
data class ConfirmReceiptRequest(
    val merchant: String,
    val date: String,
    val subtotal: Long,
    val totalDiscount: Long,
    val total: Long,
    val categoryId: String,
    val accountId: String,
    val items: List<ReceiptItemDto>,
    val adjustments: List<ReceiptAdjustmentDto>
)

@Serializable
data class UpdateReceiptRequest(
    val id: String,
    val merchant: String,
    val date: String,
    val subtotal: Long,
    val totalDiscount: Long,
    val total: Long,
    val categoryId: String? = null,
    val accountId: String? = null,
    val items: List<ReceiptItemDto>,
    val adjustments: List<ReceiptAdjustmentDto>
)

