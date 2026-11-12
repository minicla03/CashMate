package minicla03.cashmate.domain.usecase

import minicla03.cashmate.domain.repository.ReceiptRepository
import javax.inject.Inject

class DiscardReceiptDraftUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(receiptId: String) {
        receiptRepository.discardReceiptDraft(receiptId)
    }
}

