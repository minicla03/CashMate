package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetTransactionReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(transactionId: String): AppResult<Receipt?> =
        receiptRepository.getReceiptForTransaction(transactionId)
}

