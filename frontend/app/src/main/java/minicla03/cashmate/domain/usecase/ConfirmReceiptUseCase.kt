package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.ReceiptRepository
import javax.inject.Inject

class ConfirmReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val validator: ReceiptValidator
) {
    suspend operator fun invoke(receipt: Receipt): AppResult<Transaction> {
        val sanitized = receipt.copy(
            merchant = receipt.merchant.trim(),
            items = receipt.items.map { item ->
                item.copy(
                    description = item.description.trim(),
                    category = item.category.trim()
                )
            },
            adjustments = receipt.adjustments.map { adjustment ->
                adjustment.copy(description = adjustment.description.trim())
            }
        )
        validator.validate(sanitized)?.let { return AppResult.Error(it) }
        return receiptRepository.confirmReceipt(sanitized)
    }
}

