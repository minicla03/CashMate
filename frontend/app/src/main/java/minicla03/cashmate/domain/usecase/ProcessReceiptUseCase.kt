package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.repository.ReceiptRepository
import javax.inject.Inject

class ProcessReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(image: ReceiptImage): AppResult<Receipt> {
        if (image.bytes.isEmpty() || !image.mimeType.startsWith("image/", ignoreCase = true)) {
            return AppResult.Error(AppError.Receipt.UnsupportedImage)
        }
        return receiptRepository.processReceipt(image)
    }
}

