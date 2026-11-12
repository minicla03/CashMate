package minicla03.cashmate.domain.model

import java.time.LocalDate

/**
 * Pure domain representation of a scanned receipt draft returned by the backend for user review.
 */
data class Receipt(
    val id: String,
    val merchant: String,
    val date: LocalDate,
    val subtotal: Money,
    val totalDiscount: Money,
    val total: Money,
    val categoryId: String? = null,
    val accountId: String? = null,
    val items: List<ReceiptItem>,
    val adjustments: List<ReceiptAdjustment> = emptyList()
)

data class ReceiptItem(
    val id: String,
    val description: String,
    val quantity: Int,
    val unitPrice: Money,
    val grossAmount: Money,
    val discountAmount: Money,
    val netAmount: Money,
    val category: String
)

enum class ReceiptAdjustmentType {
    DISCOUNT,
    SURCHARGE,
    TAX,
    OTHER
}

data class ReceiptAdjustment(
    val id: String = "",
    val description: String,
    val amount: Money,
    val type: ReceiptAdjustmentType = ReceiptAdjustmentType.DISCOUNT
)

/**
 * Temporary binary image payload passed from presentation to the backend upload boundary.
 * Never persisted locally and omits binary bytes from [toString].
 */
data class ReceiptImage(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceiptImage) return false
        return mimeType == other.mimeType &&
            fileName == other.fileName &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }

    override fun toString(): String =
        "ReceiptImage(fileName=$fileName, mimeType=$mimeType, sizeBytes=${bytes.size})"
}

