package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.domain.model.ReceiptImage

interface ReceiptRemoteDataSource {

    suspend fun processReceipt(image: ReceiptImage): ReceiptDraftDto

    suspend fun confirmReceipt(
        id: String,
        request: ConfirmReceiptRequest
    ): TransactionDto

    suspend fun getReceiptForTransaction(transactionId: String): ReceiptDraftDto?
}

